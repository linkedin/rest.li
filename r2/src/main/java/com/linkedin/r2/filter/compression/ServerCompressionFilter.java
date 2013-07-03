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

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.r2.filter.Filter;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.message.RequestContext;
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

  private static final String UNKNOWN_ENCODING = "Unknown client encoding. ";
  private static final String UNSUPPORTED_ENCODING = "Client uses unsupported encoding: ";

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
      String requestCompression = req.getHeader(HttpConstants.CONTENT_ENCODING);

      if (requestCompression != null)
      {
        //This must be a specific compression type other than *
        EncodingType encoding = EncodingType.get(requestCompression.trim().toLowerCase());
        if (encoding == null || encoding == EncodingType.ANY)
        {
          //NOTE: this is going to be thrown up, but this isn't quite the right type of exception
          //Will change to proper type when rest.li supports centralized filter exception handling
          throw new RuntimeException(UNSUPPORTED_ENCODING
                                          + requestCompression);
        }

        //Process the correct compression types only
        if (encoding.hasCompressor())
        {
          byte[] decompressedContent = encoding.getCompressor().inflate(req.getEntity().asInputStream());
          req = req.builder().setEntity(decompressedContent).build();
        }
      }

      //Get client support for compression and flag compress if need be
      String responseCompression = req.getHeader(HttpConstants.ACCEPT_ENCODING);
      if (responseCompression == null)
      {
        responseCompression = ""; //Only permit identity
      }

      requestContext.putLocalAttr(HttpConstants.ACCEPT_ENCODING, responseCompression);
    }
    catch (CompressionException e)
    {
      LOG.error(e.getMessage(), e.getCause());
    }

    nextFilter.onRequest(req, requestContext, wireAttrs);
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
          throw new CompressionException(UNKNOWN_ENCODING);
        }

        List<AcceptEncoding> parsedEncodings = AcceptEncoding.parseAcceptEncodingHeader(responseCompression);
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
