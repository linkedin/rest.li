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

/**
 * $Id: $
 */

package com.linkedin.restli.client;

import java.net.URI;
import java.util.Map;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public interface Response<T>
{

  int getStatus();

  T getEntity();

  String getHeader(String name);

  Map<String, String> getHeaders();

  /**
   * @return a serialized form of the ID. This ID is stored in the header of the {@link Response}.
   *
   * @deprecated
   * This should only used in responses from non-batch creates.
   * This information can be found in a strongly typed format from {@link #getEntity()}
   *
   * If you are using old request builders (named ...Builders), cast the {@link com.linkedin.restli.common.EmptyRecord}
   * you receive from {@link #getEntity()} to {@link com.linkedin.restli.client.response.CreateResponse}&lt;YourKeyType&gt;.
   * You can then call {@link com.linkedin.restli.client.response.CreateResponse#getId()}
   *
   * If you are using new request builders (named ...RequestBuilders), {@link #getEntity()}
   * will return a {@link com.linkedin.restli.common.IdResponse}.
   * You can call {@link com.linkedin.restli.common.IdResponse#getId()} to get the key.
   */
  @Deprecated
  String getId();

  URI getLocation();

  RestLiResponseException getError();

  boolean hasError();
}
