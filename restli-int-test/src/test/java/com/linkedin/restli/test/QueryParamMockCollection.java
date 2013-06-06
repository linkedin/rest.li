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

package com.linkedin.restli.test;

import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.KeyValueResource;

/**
* @author Josh Walker
* @version $Revision: $
*/
@RestLiCollection(name = QueryParamMockCollection.RESOURCE_NAME)
public class QueryParamMockCollection implements KeyValueResource<String, EmptyRecord>
{
  public static volatile String _lastQueryParamValue = null;
  public static final String VALUE_KEY = "v";
  public static final String RESOURCE_NAME = "testQueryParam";

  @RestMethod.Get
  public EmptyRecord get(String key, @QueryParam("v") String value)
  {
    _lastQueryParamValue = value;
    EmptyRecord result = new EmptyRecord();
    result.data().put(VALUE_KEY, value);
    return result;
  }
}
