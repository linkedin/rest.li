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

package com.linkedin.restli.client.multiplexer;


import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;


/**
 * Client side abstraction for a multiplexed response.
 *
 * @author Dmitriy Yefremov
 */
public class MultiplexedResponse
{
  private final int _status;
  private final Map<String, String> _headers;

  public MultiplexedResponse(int status, Map<String, String> headers)
  {
    _status = status;
    // see com.linkedin.restli.internal.client.ResponseImpl.ResponseImpl()
    TreeMap<String, String> headersTreeMap = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    headersTreeMap.putAll(headers);
    _headers = Collections.unmodifiableMap(headersTreeMap);
  }

  public int getStatus()
  {
    return _status;
  }

  public Map<String, String> getHeaders()
  {
    return _headers;
  }

  public String getHeader(String name)
  {
    return _headers.get(name);
  }
}
