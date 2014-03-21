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


import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.filter.Filter;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.http.common.HttpConstants;
import com.linkedin.r2.util.ConfigValueExtractor;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client filter for compression
 * */
public class ClientCompressionFilter implements Filter, RestFilter
{
  private static final Logger LOG = LoggerFactory.getLogger(ClientCompressionFilter.class);

  private final EncodingType _requestCompression;
  private final EncodingType[] _acceptCompression;
  private final String _acceptEncodingHeader;

  /**
   * The set of methods for which response compression will be turned on
   */
  private Set<String> _responseCompressionMethods;

  private boolean _compressAllResponses;

  /**
   * The set of families for which response compression will be turned on.
   */
  private Set<String> _responseCompressionFamilies;

  /**
   * Turns on response compression for all operations
   */
  private static final String COMPRESS_ALL_RESPONSES_INDICATOR = "*";

  private static final String OPERATION_SEPARATOR = ",";
  private static final String FAMILIY_SEPARATOR = ":";
  private static final String COMPRESS_ALL_IN_FAMILY = FAMILIY_SEPARATOR + COMPRESS_ALL_RESPONSES_INDICATOR;


  /**
   * Instantiates a client compression filter
   * @param requestCompression Specifies which compression encoding
   * was used to compress requests
   * @param acceptCompression Specifies which compression encodings
   * are accepted by the client
   */
  public ClientCompressionFilter(EncodingType requestCompression,
                                 EncodingType[] acceptCompression,
                                 List<String> responseCompressionOperations)
  {
    if (requestCompression == null)
    {
      throw new IllegalArgumentException(CompressionConstants.NULL_COMPRESSOR_ERROR);
    }

    if (acceptCompression == null)
    {
      acceptCompression = new EncodingType[0];
    }

    //Sanity check
    for(EncodingType type : acceptCompression)
    {
      if (type == null)
      {
        throw new IllegalArgumentException(CompressionConstants.NULL_COMPRESSOR_ERROR);
      }
    }

    if (requestCompression.equals(EncodingType.ANY))
    {
      throw new IllegalArgumentException(CompressionConstants.REQUEST_ANY_ERROR
                                         + requestCompression.getHttpName());
    }

    _requestCompression = requestCompression;
    _acceptCompression = acceptCompression;

    _acceptEncodingHeader = buildAcceptEncodingHeader();
    _responseCompressionMethods = new HashSet<String>();
    _responseCompressionFamilies = new HashSet<String>();
    buildResponseCompressionMethodsAndFamiliesSet(responseCompressionOperations);
    // Prevent Set lookup if we are compressing responses for all operations
    _compressAllResponses = _responseCompressionMethods.contains(COMPRESS_ALL_RESPONSES_INDICATOR);
  }

  /**
   * Same as previous constructor, but with comma delimited strings as args.
   * @param requestCompression
   * @param acceptCompression
   */
  public ClientCompressionFilter(String requestCompression, String acceptCompression, List<String> responseCompressionOperations)
  {
    this(requestCompression.trim().isEmpty() ? EncodingType.IDENTITY : EncodingType.get(requestCompression.trim().toLowerCase()),
        AcceptEncoding.parseAcceptEncoding(acceptCompression), responseCompressionOperations);
  }

  @Deprecated
  public ClientCompressionFilter(String requestCompression, String acceptCompression, String responseCompressionOperations)
  {
    this(requestCompression,
         acceptCompression,
         ConfigValueExtractor.buildList(responseCompressionOperations, OPERATION_SEPARATOR));
  }

  /**
   * Converts a comma separated list of operations into a set of Strings representing the operations for which we want
   * response compression to be turned on
   * @param responseCompressionOperations
   */
  private void buildResponseCompressionMethodsAndFamiliesSet(List<String> responseCompressionOperations)
  {
    for (String operation: responseCompressionOperations)
    {
      // family operations are represented in the config as "familyName:*"
      if (operation.endsWith(COMPRESS_ALL_IN_FAMILY))
      {
        String[] parts = operation.split(FAMILIY_SEPARATOR);
        if (parts == null || parts.length != 2)
        {
          LOG.warn("Illegal compression operation family " + operation + " specified");
          return;
        }
        _responseCompressionFamilies.add(parts[0].trim());
      }
      else
      {
        _responseCompressionMethods.add(operation);
      }
    }
  }

  /**
   * Builds the accept encoding header as a string
   * @return string representation of the Accept-Encoding value for this client
   */
  public String buildAcceptEncodingHeader()
  {
    //Essentially, we want to assign nonzero quality values to all those specified;
    float delta = 1.0f/(_acceptCompression.length+1);
    float currentQuality = 1.0f;

    //Special case so we don't end with an unnecessary delimiter
    StringBuilder acceptEncodingValue = new StringBuilder();
    for(int i=0; i < _acceptCompression.length; i++)
    {
      EncodingType t = _acceptCompression[i];

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
   * Optionally compresses outgoing REST requests
   * */
  @Override
  public void onRestRequest(RestRequest req, RequestContext requestContext,
                            Map<String, String> wireAttrs,
                            NextFilter<RestRequest, RestResponse> nextFilter)
  {
    try
    {
      //If request can be compressed, compress
      if (_requestCompression.hasCompressor())
      {
        Compressor compressor = _requestCompression.getCompressor();
        byte[] compressed = compressor.deflate(req.getEntity().asInputStream());

        if (compressed.length < req.getEntity().length())
        {
          req = req.builder().setEntity(compressed).setHeader(HttpConstants.CONTENT_ENCODING,
                                                              compressor.getContentEncodingName()).build();
        }
      }

      //Set accepted encoding for compressed response
      String operation = (String)requestContext.getLocalAttr(R2Constants.OPERATION);
      if (_acceptCompression.length > 0 && shouldCompress(operation))
      {
        req = req.builder().addHeaderValue(HttpConstants.ACCEPT_ENCODING, _acceptEncodingHeader).build();
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
   * Check if the operation should be compressed
   * @param operation
   */
  private boolean shouldCompress(String operation)
  {
    return _compressAllResponses ||
           _responseCompressionMethods.contains(operation) ||
           isMemberOfCompressionFamily(operation);
  }

  /**
   * Checks if the operation is a member of a family for which we have turned response compression on.
   * @param operation
   */
  private boolean isMemberOfCompressionFamily(String operation)
  {
    if (operation.contains(FAMILIY_SEPARATOR))
    {
      String[] parts = operation.split(FAMILIY_SEPARATOR);
      if (parts == null || parts.length != 2)
      {
        return false;
      }
      String family = parts[0];
      return _responseCompressionFamilies.contains(family);
    }
    return false;
  }

  /**
   *  Decompresses server response
   */
  @Override
  public void onRestResponse(RestResponse res, RequestContext requestContext,
                             Map<String, String> wireAttrs,
                             NextFilter<RestRequest, RestResponse> nextFilter)
  {
    try
    {
      //Check for header encoding
      String compressionHeader = res.getHeader(HttpConstants.CONTENT_ENCODING);

      //Compress if necessary
      if (compressionHeader != null && res.getEntity().length() > 0)
      {
        EncodingType encoding = EncodingType.get(compressionHeader.trim().toLowerCase());
        if (encoding == null || !encoding.hasCompressor())
        {
          throw new CompressionException(CompressionConstants.SERVER_ENCODING_ERROR + compressionHeader);
        }

        if (encoding.hasCompressor())
        {
          byte[] inflated = encoding.getCompressor().inflate(res.getEntity().asInputStream());
          res = res.builder().setEntity(inflated).build();
        }
      }
    }
    catch (CompressionException e)
    {
      //NOTE: this is going to be thrown up, but this isn't quite the right type of exception
      //Will change to proper type when rest.li supports centralized filter exception handling
      throw new RuntimeException(CompressionConstants.SERVER_ENCODING_ERROR, e);
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
