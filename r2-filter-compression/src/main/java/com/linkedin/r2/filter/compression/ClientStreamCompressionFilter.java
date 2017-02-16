/*
   Copyright (c) 2015 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.linkedin.r2.filter.compression;


import com.linkedin.common.callback.Callback;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.filter.CompressionConfig;
import com.linkedin.r2.filter.CompressionOption;
import com.linkedin.r2.filter.compression.streaming.AcceptEncoding;
import com.linkedin.r2.filter.compression.streaming.StreamEncodingType;
import com.linkedin.r2.message.stream.entitystream.CompositeWriter;
import com.linkedin.r2.filter.compression.streaming.PartialReader;
import com.linkedin.r2.filter.compression.streaming.StreamingCompressor;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.stream.StreamException;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.transport.http.common.HttpConstants;

import java.util.List;
import java.util.Map;

import java.util.TreeMap;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client filter for compression
 *
 * @author Ang Xu
 */
public class ClientStreamCompressionFilter implements StreamFilter
{
  private static final Logger LOG = LoggerFactory.getLogger(ClientStreamCompressionFilter.class);

  private final StreamEncodingType _requestContentEncoding;
  private final CompressionConfig _requestCompressionConfig;
  private final CompressionConfig _responseCompressionConfig;

  /**
   * Encodings accepted by the client, used to generate Accept-Encoding header.
   */
  private final StreamEncodingType[] _acceptedEncodings;
  private final String _acceptEncodingHeader;
  private final ClientCompressionHelper _helper;

  private final Executor _executor;


  /**
   * Instantiates a client compression filter.
   *
   * @param requestContentEncoding the encoding that should be used to compress requests.
   * @param requestCompressionConfig config for determining when to compress requests.
   * @param acceptedEncodings encodings accepted by the client, used to generate Accept-Encoding header.
   * @param responseCompressionOperations the set of operations for which response compression will be turned on.
   */
  public ClientStreamCompressionFilter(StreamEncodingType requestContentEncoding,
                                       CompressionConfig requestCompressionConfig,
                                       StreamEncodingType[] acceptedEncodings,
                                       CompressionConfig responseCompressionConfig,
                                       List<String> responseCompressionOperations,
                                       Executor executor)
  {
    if (requestContentEncoding == null)
    {
      throw new IllegalArgumentException(CompressionConstants.NULL_COMPRESSOR_ERROR);
    }

    if (acceptedEncodings == null)
    {
      acceptedEncodings = new StreamEncodingType[0];
    }

    //Sanity check
    for (StreamEncodingType type : acceptedEncodings)
    {
      if (type == null)
      {
        throw new IllegalArgumentException(CompressionConstants.NULL_COMPRESSOR_ERROR);
      }
    }

    if (requestContentEncoding.equals(StreamEncodingType.ANY))
    {
      throw new IllegalArgumentException(CompressionConstants.REQUEST_ANY_ERROR
          + requestContentEncoding.getHttpName());
    }

    _requestContentEncoding = requestContentEncoding;
    _requestCompressionConfig = requestCompressionConfig;
    _acceptedEncodings = acceptedEncodings;
    _responseCompressionConfig = responseCompressionConfig;

    _acceptEncodingHeader = buildAcceptEncodingHeader();
    _helper = new ClientCompressionHelper(requestCompressionConfig, responseCompressionOperations);
    _executor = executor;
  }

  /**
   * Same as previous constructor, but with comma delimited strings for requestContentEncoding and acceptedEncodings.
   */
  public ClientStreamCompressionFilter(String requestContentEncoding,
                                       CompressionConfig requestCompressionConfig,
                                       String acceptedEncodings,
                                       CompressionConfig responseCompressionConfig,
                                       List<String> responseCompressionOperations,
                                       Executor executor)
  {
    this(requestContentEncoding.trim().isEmpty() ? StreamEncodingType.IDENTITY : StreamEncodingType.get(requestContentEncoding.trim().toLowerCase()),
        requestCompressionConfig,
        AcceptEncoding.parseAcceptEncoding(acceptedEncodings),
        responseCompressionConfig,
        responseCompressionOperations,
        executor);
  }

  /**
   * Builds the accept encoding header as a string
   * @return string representation of the Accept-Encoding value for this client
   */
  public String buildAcceptEncodingHeader()
  {
    //Essentially, we want to assign nonzero quality values to all those specified;
    float delta = 1.0f/(_acceptedEncodings.length+1);
    float currentQuality = 1.0f;

    //Special case so we don't end with an unnecessary delimiter
    StringBuilder acceptEncodingValue = new StringBuilder();
    for(int i=0; i < _acceptedEncodings.length; i++)
    {
      StreamEncodingType t = _acceptedEncodings[i];

      if(i > 0)
      {
        acceptEncodingValue.append(CompressionConstants.ENCODING_DELIMITER);
      }
      acceptEncodingValue.append(t.getHttpName());
      acceptEncodingValue.append(CompressionConstants.QUALITY_DELIMITER);
      acceptEncodingValue.append(CompressionConstants.QUALITY_PREFIX);
      acceptEncodingValue.append(String.format("%.2f", currentQuality));
      currentQuality = currentQuality - delta;
    }

    return acceptEncodingValue.toString();
  }

  /**
   * Optionally compresses outgoing Stream requests
   * */
  public void onStreamRequest(StreamRequest req, final RequestContext requestContext, final Map<String, String> wireAttrs,
                              final NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    //Set accepted encoding for compressed response
    String operation = (String) requestContext.getLocalAttr(R2Constants.OPERATION);
    if (!_acceptEncodingHeader.isEmpty() && _helper.shouldCompressResponseForOperation(operation))
    {
      CompressionOption responseCompressionOverride =
          (CompressionOption) requestContext.getLocalAttr(R2Constants.RESPONSE_COMPRESSION_OVERRIDE);
      req = addResponseCompressionHeaders(responseCompressionOverride, req);
    }

    if (_requestContentEncoding != StreamEncodingType.IDENTITY)
    {
      final StreamRequest request = req;
      final StreamingCompressor compressor = _requestContentEncoding.getCompressor(_executor);
      CompressionOption option = (CompressionOption) requestContext.getLocalAttr(R2Constants.REQUEST_COMPRESSION_OVERRIDE);
      if (option == null || option != CompressionOption.FORCE_OFF)
      {
        final int threshold = option == CompressionOption.FORCE_ON ? 0 : _requestCompressionConfig.getCompressionThreshold();
        PartialReader reader = new PartialReader(threshold, new Callback<EntityStream[]>()
        {
          @Override
          public void onError(Throwable ex)
          {
            nextFilter.onError(ex, requestContext, wireAttrs);
          }

          @Override
          public void onSuccess(EntityStream[] result)
          {
            if (result.length == 1)
            {
              StreamRequest uncompressedRequest = request.builder().build(result[0]);
              nextFilter.onRequest(uncompressedRequest, requestContext, wireAttrs);
            }
            else
            {
              StreamRequestBuilder builder = request.builder();
              EntityStream compressedStream = compressor.deflate(EntityStreams.newEntityStream(new CompositeWriter(result)));
              Map<String, String> headers = stripHeaders(builder.getHeaders(), HttpConstants.CONTENT_LENGTH);
              StreamRequest compressedRequest = builder.setHeaders(headers)
                  .setHeader(HttpConstants.CONTENT_ENCODING, compressor.getContentEncodingName())
                  .build(compressedStream);
              nextFilter.onRequest(compressedRequest, requestContext, wireAttrs);
            }
          }
        });
        req.getEntityStream().setReader(reader);
        return;
      }
    }

    nextFilter.onRequest(req, requestContext, wireAttrs);
  }

  /**
   *  Decompresses server response
   */
  @Override
  public void onStreamResponse(StreamResponse res, RequestContext requestContext, Map<String, String> wireAttrs,
                               NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    Boolean decompressionOff = (Boolean) requestContext.getLocalAttr(R2Constants.RESPONSE_DECOMPRESSION_OFF);
    if (decompressionOff == null || !decompressionOff)
    {
      //Check for header encoding
      String compressionHeader = res.getHeader(HttpConstants.CONTENT_ENCODING);
      //decompress if necessary
      if (compressionHeader != null)
      {
        final StreamEncodingType encoding = StreamEncodingType.get(compressionHeader.trim().toLowerCase());
        if (encoding == null)
        {
          nextFilter.onError(new IllegalArgumentException("Server returned unrecognized content encoding: " +
              compressionHeader), requestContext, wireAttrs);
          return;
        }

        final StreamingCompressor compressor = encoding.getCompressor(_executor);
        EntityStream uncompressedStream = compressor.inflate(res.getEntityStream());
        StreamResponseBuilder builder = res.builder();
        Map<String, String> headers =
            stripHeaders(builder.getHeaders(), HttpConstants.CONTENT_ENCODING, HttpConstants.CONTENT_LENGTH);
        res = builder.setHeaders(headers).build(uncompressedStream);
      }
    }

    nextFilter.onResponse(res, requestContext, wireAttrs);
  }

  @Override
  public void onStreamError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs,
                            NextFilter<StreamRequest, StreamResponse> nextFilter)
  {
    if (ex instanceof StreamException)
    {
      Boolean decompressionOff = (Boolean) requestContext.getLocalAttr(R2Constants.RESPONSE_DECOMPRESSION_OFF);
      if (decompressionOff == null || !decompressionOff)
      {
        StreamException se = (StreamException) ex;

        StreamResponse response = se.getResponse();
        //Check for header encoding
        String compressionHeader = response.getHeader(HttpConstants.CONTENT_ENCODING);

        //decompress if necessary
        if (compressionHeader != null)
        {
          StreamEncodingType encoding = StreamEncodingType.get(compressionHeader.trim().toLowerCase());
          if (encoding != null)
          {
            final StreamingCompressor compressor = encoding.getCompressor(_executor);
            EntityStream uncompressedStream = compressor.inflate(response.getEntityStream());

            StreamResponseBuilder builder = response.builder();
            Map<String, String> headers =
                stripHeaders(builder.getHeaders(), HttpConstants.CONTENT_ENCODING, HttpConstants.CONTENT_LENGTH);
            response = builder.setHeaders(headers).build(uncompressedStream);
            ex = new StreamException(response);
          }
        }
      }
    }
    nextFilter.onError(ex, requestContext, wireAttrs);
  }

  private Map<String, String> stripHeaders(Map<String, String> headerMap, String...headers)
  {
    Map<String, String> newMap = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    newMap.putAll(headerMap);
    for (String header : headers)
    {
      newMap.remove(header);
    }
    return newMap;
  }

  /**
   * Builds HTTP headers related to response compression and creates a RestRequest with those headers added.
   *
   * @param responseCompressionOverride compression force on/off override from the request context.
   * @param req current request.
   * @return request with response compression headers.
   */
  public StreamRequest addResponseCompressionHeaders(CompressionOption responseCompressionOverride, StreamRequest req)
  {
    StreamRequestBuilder builder = req.builder();
    if (responseCompressionOverride == null)
    {
      builder.addHeaderValue(HttpConstants.ACCEPT_ENCODING, _acceptEncodingHeader);
      if (_responseCompressionConfig != null)
      {
        builder.addHeaderValue(HttpConstants.HEADER_RESPONSE_COMPRESSION_THRESHOLD,
            Integer.toString(_responseCompressionConfig.getCompressionThreshold()));
      }
    }
    else if (responseCompressionOverride == CompressionOption.FORCE_ON)
    {
      builder.addHeaderValue(HttpConstants.ACCEPT_ENCODING, _acceptEncodingHeader)
          .addHeaderValue(HttpConstants.HEADER_RESPONSE_COMPRESSION_THRESHOLD, Integer.toString(0));
    }
    return builder.build(req.getEntityStream());
  }
}
