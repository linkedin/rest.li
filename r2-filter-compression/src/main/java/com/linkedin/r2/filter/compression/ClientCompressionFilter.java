/*
   Copyright (c) 2013 LinkedIn Corp.

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


import com.linkedin.data.ByteString;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.filter.CompressionConfig;
import com.linkedin.r2.filter.CompressionOption;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.http.common.HttpConstants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client filter for compression
 */
public class ClientCompressionFilter implements RestFilter
{
  private static final Logger LOG = LoggerFactory.getLogger(ClientCompressionFilter.class);

  private final EncodingType _requestContentEncoding;
  private final CompressionConfig _responseCompressionConfig;
  private final String _acceptEncodingHeader;
  private final ClientCompressionHelper _helper;


  /**
   * Instantiates a client compression filter.
   *
   * @param requestContentEncoding the encoding that should be used to compress requests.
   * @param requestCompressionConfig config for determining when to compress requests.
   * @param acceptedEncodings encodings accepted by the client, used to generate Accept-Encoding header.
   * @param responseCompressionConfig config for determining when to ask the server to compress responses.
   *                                  This overrides the config in the server.
   * @param responseCompressionOperations the set of operations for which response compression will be turned on.
   */
  public ClientCompressionFilter(EncodingType requestContentEncoding,
                                 CompressionConfig requestCompressionConfig,
                                 EncodingType[] acceptedEncodings,
                                 CompressionConfig responseCompressionConfig,
                                 List<String> responseCompressionOperations)
  {
    if (requestContentEncoding == null)
    {
      throw new IllegalArgumentException(CompressionConstants.NULL_COMPRESSOR_ERROR);
    }
    else if (requestContentEncoding.equals(EncodingType.ANY))
    {
      throw new IllegalArgumentException(CompressionConstants.REQUEST_ANY_ERROR
          + requestContentEncoding.getHttpName());
    }

    if (acceptedEncodings == null)
    {
      acceptedEncodings = new EncodingType[0];
    }
    //Sanity check
    for (EncodingType type : acceptedEncodings)
    {
      if (type == null)
      {
        throw new IllegalArgumentException(CompressionConstants.NULL_ENCODING_ERROR);
      }
    }

    if (requestCompressionConfig == null)
    {
      throw new IllegalArgumentException(CompressionConstants.NULL_CONFIG_ERROR);
    }
    // Null response compression config is allowed. This means that the default threshold on the server will be used.

    _requestContentEncoding = requestContentEncoding;
    _acceptEncodingHeader = buildAcceptEncodingHeader(acceptedEncodings);
    _responseCompressionConfig = responseCompressionConfig;
    _helper = new ClientCompressionHelper(requestCompressionConfig, responseCompressionOperations);
  }

  /**
   * Same as previous constructor, but with comma delimited strings for requestContentEncoding and acceptedEncodings.
   */
  public ClientCompressionFilter(String requestContentEncoding,
                                 CompressionConfig requestCompressionConfig,
                                 String acceptedEncodings,
                                 CompressionConfig responseCompressionConfig,
                                 List<String> responseCompressionOperations)
  {
    this(requestContentEncoding.trim().isEmpty() ? EncodingType.IDENTITY : EncodingType.get(requestContentEncoding.trim().toLowerCase()),
        requestCompressionConfig,
        AcceptEncoding.parseAcceptEncoding(acceptedEncodings),
        responseCompressionConfig,
        responseCompressionOperations);
  }


  /**
   * Builds the accept encoding header as a string
   * @return string representation of the Accept-Encoding value for this client
   */
  /* package private */ static String buildAcceptEncodingHeader(EncodingType[] acceptedEncodings)
  {
    //Essentially, we want to assign nonzero quality values to all those specified;
    float delta = 1.0f/(acceptedEncodings.length + 1);
    float currentQuality = 1.0f;

    //Special case so we don't end with an unnecessary delimiter
    StringBuilder acceptEncodingValue = new StringBuilder();
    for (int i = 0; i < acceptedEncodings.length; i++)
    {
      EncodingType t = acceptedEncodings[i];

      if (i > 0)
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
   * Builds HTTP headers related to response compression and creates a RestRequest with those headers added. If the
   * request already has a {@link HttpConstants#ACCEPT_ENCODING} set, then it returns the input request as is.
   *
   * @param responseCompressionOverride compression force on/off override from the request context.
   * @param req current request.
   * @return request with response compression headers.
   */
  public RestRequest addResponseCompressionHeaders(CompressionOption responseCompressionOverride, RestRequest req)
  {
    // If the client manually set an accept encoding header, don't override and short circuit.
    if (req.getHeader(HttpConstants.ACCEPT_ENCODING) != null)
    {
      return req;
    }

    RestRequestBuilder builder = req.builder();
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
    return builder.build();
  }

  /**
   * Optionally compresses outgoing REST requests
   * */
  @Override
  public void onRestRequest(RestRequest req, RequestContext requestContext,
                            Map<String, String> wireAttrs,
                            NextFilter<RestRequest, RestResponse> nextFilter)
  {
    try
    {
      if (_requestContentEncoding.hasCompressor())
      {
        if (_helper.shouldCompressRequest(req.getEntity().length(),
            (CompressionOption) requestContext.getLocalAttr(R2Constants.REQUEST_COMPRESSION_OVERRIDE)
        ))
        {
          Compressor compressor = _requestContentEncoding.getCompressor();
          ByteString compressed = compressor.deflate(req.getEntity());

          if (compressed.length() < req.getEntity().length())
          {
            req = req.builder().setEntity(compressed).setHeader(HttpConstants.CONTENT_ENCODING,
                compressor.getContentEncodingName()).build();
          }
        }
      }

      String operation = (String) requestContext.getLocalAttr(R2Constants.OPERATION);
      if (!_acceptEncodingHeader.isEmpty() && _helper.shouldCompressResponseForOperation(operation))
      {
        CompressionOption responseCompressionOverride =
            (CompressionOption) requestContext.getLocalAttr(R2Constants.RESPONSE_COMPRESSION_OVERRIDE);
        req = addResponseCompressionHeaders(responseCompressionOverride, req);
      }
    }
    catch (CompressionException e)
    {
      LOG.error(e.getMessage(), e.getCause());
    }

    //Specify the actual compression algorithm used
    nextFilter.onRequest(req, requestContext, wireAttrs);
  }

  /**
   *  Decompresses server response
   */
  @Override
  public void onRestResponse(RestResponse res, RequestContext requestContext,
                             Map<String, String> wireAttrs,
                             NextFilter<RestRequest, RestResponse> nextFilter)
  {
    Boolean decompressionOff = (Boolean) requestContext.getLocalAttr(R2Constants.RESPONSE_DECOMPRESSION_OFF);
    if (decompressionOff == null || !decompressionOff)
    {
      try
      {
        //Check for header encoding
        String compressionHeader = res.getHeader(HttpConstants.CONTENT_ENCODING);

        //Compress if necessary
        if (compressionHeader != null && res.getEntity().length() > 0)
        {
          EncodingType encoding = null;
          try
          {
            encoding = EncodingType.get(compressionHeader.trim().toLowerCase());
          }
          catch (IllegalArgumentException e)
          {
            throw new CompressionException(CompressionConstants.SERVER_ENCODING_ERROR + compressionHeader);
          }
          if (!encoding.hasCompressor())
          {
            throw new CompressionException(CompressionConstants.SERVER_ENCODING_ERROR + compressionHeader);
          }
          ByteString inflated = encoding.getCompressor().inflate(res.getEntity());
          Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
          headers.putAll(res.getHeaders());
          headers.remove(HttpConstants.CONTENT_ENCODING);
          headers.put(HttpConstants.CONTENT_LENGTH, Integer.toString(inflated.length()));
          res = res.builder().setEntity(inflated).setHeaders(headers).build();
        }
      }
      catch (CompressionException e)
      {
        nextFilter.onError(e, requestContext, wireAttrs);
        return;
      }
    }

    nextFilter.onResponse(res, requestContext, wireAttrs);
  }

  @Override
  public void onRestError(Throwable ex, RequestContext requestContext,
                          Map<String, String> wireAttrs,
                          NextFilter<RestRequest, RestResponse> nextFilter)
  {
    nextFilter.onError(ex, requestContext, wireAttrs);
  }

}
