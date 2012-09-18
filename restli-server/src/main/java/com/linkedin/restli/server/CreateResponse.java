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

package com.linkedin.restli.server;

import com.linkedin.restli.common.HttpStatus;

/**
 * @author dellamag
 */
public class CreateResponse
{
  private final Object _id;
  private final HttpStatus _status;

  /**
   * @param id the newly created resource id
   */
  public CreateResponse(final Object id)
  {
    _id = id;
    _status = HttpStatus.S_201_CREATED;
  }

  /**
   * @param id the newly created resource id
   * @param status HTTP response status
   */
  public CreateResponse(final Object id, final HttpStatus status)
  {
    super();
    _id = id;
    _status = status;
  }

  /**
   * @param status HTTP response status
   */
  public CreateResponse(final HttpStatus status)
  {
    super();
    _id = null;
    _status = status;
  }

  /**
   * @return true if the response has id, false otherwise
   */
  public boolean hasId()
  {
    return _id != null;
  }

  public Object getId()
  {
    return _id;
  }

  public HttpStatus getStatus()
  {
    return _status;
  }
}
