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


import com.linkedin.r2.message.RequestContext;

/**
 * This enumeration is used by the client to force compression on or off for each request,
 * when included as a local attribute of the {@link RequestContext} with the key as {@link R2Constants#REQUEST_COMPRESSION_OVERRIDE}.
 *
 * @author Soojung Ha
 */
public enum CompressionOption
{
  /**
   * Compress the request.
   */
  FORCE_ON,

  /**
   * Do not compress the request.
   */
  FORCE_OFF
}