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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.r2.filter.Filter;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.transport.http.common.HttpConstants;

/**
 *
 * Filter class for server to negotiate acceptable compression formats from clients
 * and compresses the response with the relevant headers accordingly.
 * @author erli
 *
 */
public class ServerCompressionFilter implements Filter, RestFilter
{
  private static final Logger LOG = LoggerFactory.getLogger(ServerCompressionFilter.class);

  private final Set<EncodingType> _supportedEncoding;

  /**
   * Instantiates an empty compression filter that does no compression.
   */
  public ServerCompressionFilter()
  {
    this(new EncodingType[0]);
  }

  /** Takes a comma delimited string containing standard
   * HTTP encoding headers and instantiates server compression
   * support for the said encoding types.
   * @param acceptedFilters
   */
  public ServerCompressionFilter(String acceptedFilters)
  {
    this(AcceptEncoding.parseAcceptEncoding(acceptedFilters));
  }

  /** Instantiates a compression filter
   * that supports the compression methods in the given set in argument.
   * @param supportedEncoding
   */
  public ServerCompressionFilter(EncodingType[] supportedEncoding)
  {
    _supportedEncoding = new HashSet<EncodingType>(Arrays.asList(supportedEncoding));
    _supportedEncoding.add(EncodingType.IDENTITY);
    _supportedEncoding.add(EncodingType.ANY);
  }

  /**
   * Handles compression tasks for incoming requests
   */
  @Override
  public void onRestRequest(RestRequest req, RequestContext requestContext,
                            Map<String, String> wireAttrs,
                            NextFilter<RestRequest, RestResponse> nextFilter)
  {
    try
    {
      //Check if the request is compressed, if so, decompress
      String requestContentEncoding = req.getHeader(HttpConstants.CONTENT_ENCODING);

      if (requestContentEncoding != null)
      {
        //This must be a specific compression type other than *
        EncodingType encoding;
        try
        {
          encoding = EncodingType.get(requestContentEncoding.trim().toLowerCase());
        }
        catch (IllegalArgumentException ex)
        {
          throw new CompressionException(CompressionConstants.UNSUPPORTED_ENCODING
              + requestContentEncoding);
        }
        if (encoding == EncodingType.ANY)
        {
          throw new CompressionException(CompressionConstants.REQUEST_ANY_ERROR
              + requestContentEncoding);
        }

        //Process the correct compression types only
        if (encoding.hasCompressor())
        {
          byte[] decompressedContent = encoding.getCompressor().inflate(req.getEntity().asInputStream());
          Map<String, String> headers = new HashMap<String, String>(req.getHeaders());
          headers.remove(HttpConstants.CONTENT_ENCODING);
          headers.put(HttpConstants.CONTENT_LENGTH, Integer.toString(decompressedContent.length));
          req = req.builder().setEntity(decompressedContent).setHeaders(headers).build();
        }
      }

      //Get client support for compression and flag compress if need be
      String responseCompression = req.getHeader(HttpConstants.ACCEPT_ENCODING);
      if (responseCompression == null)
      {
        responseCompression = ""; //Only permit identity
      }

      requestContext.putLocalAttr(HttpConstants.ACCEPT_ENCODING, responseCompression);
      nextFilter.onRequest(req, requestContext, wireAttrs);
    }
    catch (CompressionException e)
    {
      //If we can't decompress the client's request, we can't do much more with it
      LOG.error(e.getMessage(), e.getCause());
      RestResponse restResponse = new RestResponseBuilder().setStatus(HttpConstants.UNSUPPORTED_MEDIA_TYPE).build();
      nextFilter.onError(new RestException(restResponse, e), requestContext, wireAttrs);
    }
  }

  /**
   * Optionally compresses outgoing response
   * */
  @Override
  public void onRestResponse(RestResponse res, RequestContext requestContext,
                             Map<String, String> wireAttrs,
                             NextFilter<RestRequest, RestResponse> nextFilter)
  {
    try
    {
      if (res.getEntity().length() > 0)
      {
        String responseCompression = (String) requestContext.getLocalAttr(HttpConstants.ACCEPT_ENCODING);
        if (responseCompression == null)
        {
          throw new CompressionException(CompressionConstants.UNKNOWN_ENCODING);
        }

        List<AcceptEncoding> parsedEncodings = AcceptEncoding.parseAcceptEncodingHeader(responseCompression, _supportedEncoding);
        EncodingType selectedEncoding = AcceptEncoding.chooseBest(parsedEncodings);

        //Check if there exists an acceptable encoding
        if (selectedEncoding != null)
        {
          //NOTE: this is sort of problematic and is mirrored in 3 other places.
          //ByteBuffer from res.getEntity() is read only, and it's awkward for
          //compressor to return a sensible value for identity
          if (selectedEncoding.hasCompressor())
          {
            Compressor compressor = selectedEncoding.getCompressor();
            byte[] compressed = compressor.deflate(res.getEntity().asInputStream());

            if (compressed.length < res.getEntity().length())
            {
              RestResponseBuilder resCompress = res.builder();
              resCompress.addHeaderValue(HttpConstants.CONTENT_ENCODING, compressor.getContentEncodingName());
              resCompress.setEntity(compressed);
              res = resCompress.build();
            }
          }
        }
        else
        {
          //Not acceptable encoding status
          res = res.builder().setStatus(HttpConstants.NOT_ACCEPTABLE).setEntity(new byte[0]).build();
        }
      }
    }
    catch (CompressionException e)
    {
      LOG.error(e.getMessage(), e.getCause());
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
