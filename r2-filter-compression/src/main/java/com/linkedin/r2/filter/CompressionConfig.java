/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.r2.filter;


import com.linkedin.r2.filter.compression.ClientCompressionFilter;

/**
 * By setting this config in {@link ClientCompressionFilter}, the client can set the default compression threshold
 * for requests and/or responses.
 * Requests/responses with body size larger than this threshold will be compressed.
 * To compress all requests/responses, threshold must be 0.
 * To not compress any requests/responses, threshold must be {@link Integer#MAX_VALUE}.
 *
 * This default behavior can be overridden by {@link CompressionOption} in the request context.
 *
 * @author Soojung Ha
 */
public class CompressionConfig
{
  private final int _compressionThreshold;

  public CompressionConfig(int compressionThreshold)
  {
    if (compressionThreshold < 0)
    {
      throw new IllegalArgumentException("compressionThreshold should not be negative.");
    }
    _compressionThreshold = compressionThreshold;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
    {
      return true;
    }
    if (o == null || getClass() != o.getClass())
    {
      return false;
    }

    CompressionConfig that = (CompressionConfig) o;

    if (_compressionThreshold != that._compressionThreshold)
    {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode()
  {
    return _compressionThreshold;
  }

  @Override
  public String toString()
  {
    return "CompressionConfig{" +
        "_compressionThreshold=" + _compressionThreshold +
        '}';
  }

  public int getCompressionThreshold()
  {
    return _compressionThreshold;
  }
}
