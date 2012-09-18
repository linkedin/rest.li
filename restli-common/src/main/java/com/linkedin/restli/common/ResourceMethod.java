/*
   Copyright (c) 2012 LinkedIn Corp.

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

package com.linkedin.restli.common;


/**
 * REST resource methods.
 */
public enum ResourceMethod
{
  GET             (HttpMethod.GET),
  BATCH_GET       (HttpMethod.GET),
  FINDER          (HttpMethod.GET),
  CREATE          (HttpMethod.POST),
  BATCH_CREATE    (HttpMethod.POST),
  PARTIAL_UPDATE  (HttpMethod.POST),
  UPDATE          (HttpMethod.PUT),
  BATCH_UPDATE    (HttpMethod.PUT),
  DELETE          (HttpMethod.DELETE),
  ACTION          (HttpMethod.POST),
  BATCH_PARTIAL_UPDATE (HttpMethod.POST),
  BATCH_DELETE    (HttpMethod.DELETE);

  ResourceMethod(HttpMethod httpMethod)
  {
    _httpMethod = httpMethod;
  }

  private HttpMethod _httpMethod;

  /**
   * @return the HttpMethod associated with this ResourceMethod
   */
  public HttpMethod getHttpMethod()
  {
    return _httpMethod;
  }

  @Override
  public String toString()
  {
    return super.toString().toLowerCase();
  }

  /**
   * Convert to ResourceMethod enum from String.
   *
   * @param name String of a ResourceMethod enum
   * @return a ResourceMethod
   */
  public static ResourceMethod fromString(String name)
  {
    return valueOf(name.toUpperCase());
  }
}
