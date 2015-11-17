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

import com.linkedin.r2.filter.CompressionConfig;
import com.linkedin.r2.message.MessageHeaders;
import com.linkedin.r2.transport.http.common.HttpConstants;


/**
 * This class contains helper methods for Rest and Stream ServerCompressionFilter.
 */
public final class ServerCompressionHelper
{
  private final CompressionConfig _defaultResponseCompressionConfig;

  public ServerCompressionHelper(int defaultThreshold)
  {
    this(new CompressionConfig(defaultThreshold));
  }

  public ServerCompressionHelper(CompressionConfig defaultResponseCompressionConfig)
  {
    _defaultResponseCompressionConfig = defaultResponseCompressionConfig;
  }

  public int getResponseCompressionThreshold(MessageHeaders message) throws CompressionException
  {
    String responseCompressionThreshold = message.getHeader(HttpConstants.HEADER_RESPONSE_COMPRESSION_THRESHOLD);
    // If Response-Compression-Threshold header is present, use that value. If not, use the value in _defaultResponseCompressionConfig.
    if (responseCompressionThreshold != null)
    {
      try
      {
        return Integer.parseInt(responseCompressionThreshold);
      }
      catch (NumberFormatException e)
      {
        throw new CompressionException(CompressionConstants.INVALID_THRESHOLD + responseCompressionThreshold);
      }
    }
    return _defaultResponseCompressionConfig.getCompressionThreshold();
  }
}
