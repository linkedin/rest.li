/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.restli.internal.server.response;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.server.RestLiServiceException;


/**
 * Contains response data for {@link ResourceMethod#CREATE}.
 *
 * @author gye
 */
public class CreateResponseEnvelope extends RecordResponseEnvelope
{
  private final boolean _isGetAfterCreate;

  /**
   * This constructor has a configuration boolean for whether or not this is a CREATE + GET (i.e. this constructor
   * creates a BatchCreateResponse that contains the newly created data) as opposed to a normal CREATE. true = CREATE +
   * GET, false = CREATE.
   * @param response Newly created response.
   * @param isGetAfterCreate Boolean flag denoting whether or not this is a CREATE + GET.
   */
  CreateResponseEnvelope(HttpStatus status, RecordTemplate response, boolean isGetAfterCreate)
  {
    super(status, response);
    _isGetAfterCreate = isGetAfterCreate;
  }

  CreateResponseEnvelope(RestLiServiceException exception, boolean isGetAfterCreate)
  {
    super(exception);
    _isGetAfterCreate = isGetAfterCreate;
  }

  @Override
  public ResourceMethod getResourceMethod()
  {
    return ResourceMethod.CREATE;
  }

  /**
   * Returns whether or not this CREATE response also contains the newly created data (i.e. a GET after a CREATE)
   * Users can use getRecord() to retrieve the newly created data if this is a CREATE + GET. Otherwise, the user can
   * only use getRecord() to get the ID of the newly created data.
   *
   * @return boolean as to whether or not this response contains the newly created data.
   */
  public boolean isGetAfterCreate()
  {
    return _isGetAfterCreate;
  }
}
