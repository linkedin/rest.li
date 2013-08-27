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

package com.linkedin.restli.common;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;

/**
 * A Create response status.  May optionally contain error details.
 *
 * The errors field is primarily for batch requests.  For error responses to individual create requests,
 * the response will only contains an error response and will not contain create status.
 *
 * @author Josh Walker
 * @version $Revision: $
 */

public class CreateStatus extends RecordTemplate
{
  private static final String _STATUS = "status";
  private static final String _ID = "id";
  private static final String _ERROR = "error";

  /**
   * Initialize an empty CreateStatus.
   */
  public CreateStatus()
  {
    super(new DataMap(), null);
  }

  /**
   * Initialize a CreateStatus off of a given DataMap.
   *
   * @param data a DataMap
   */
  public CreateStatus(DataMap data)
  {
    super(data, null);
  }

  /**
   * @return the http status of the CreateStatus
   */
  public Integer getStatus()
  {
    return data().getInteger(_STATUS);
  }

  /**
   * Set the http status of the CreateStatus.
   *
   * @param status the http status
   */
  public void setStatus(Integer status)
  {
    data().put(_STATUS, status);
  }

  /**
   * @return the Id of the created item
   */
  public String getId()
  {
    return data().getString(_ID);
  }

  /**
   * Set the Id of the created item.
   *
   * @param id the Id
   */
  public void setId(String id)
  {
    data().put(_ID, id);
  }

  /**
   * Checks if the create status contains a error.
   *
   * This is primarily for batch requests.  For error responses to individual create requests,
   * the response only contains an error response and will not contain create status.
   *
   * @return true if the create status contains an error.
   */
  public boolean hasError()
  {
    return data().containsKey(_ERROR);
  }

  /**
   * Gets the error for for the create status, if any.
   *
   * This is primarily for batch requests.  For error responses to individual create requests,
   * the response will only contains an error response and will not contain create status.
   *
   * @return the error if it exists, otherwise null.
   */
  public ErrorResponse getError()
  {
    return data().containsKey(_ERROR) ? new ErrorResponse(data().getDataMap(_ERROR)) : null;
  }

  /**
   * Sets the error response of the created item.
   *
   * This is primarily for batch requests.  For error responses to individual create requests,
   * the response will only contains an error response and will not contain create status.
   *
   * @param error the error.
   */
  public void setError(ErrorResponse error)
  {
    data().put(_ERROR, error.data());
  }

}
