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
import com.linkedin.data.template.RecordTemplate;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class UpdateStatus extends RecordTemplate
{
  private static final String STATUS = "status";

  /**
   * Initialize an empty UpdateStatus.
   */
  public UpdateStatus()
  {
    super(new DataMap(), null);
  }

  /**
   * Initialize an UpdateStatus based on the given DataMap.
   *
   * @param data a DataMap
   */
  public UpdateStatus(DataMap data)
  {
    super(data, null);
  }

  /**
   * @return the http status of the updateStatus
   */
  public Integer getStatus()
  {
    return data().getInteger(STATUS);
  }

  /**
   * Set the http status of the UpdateStatus.
   *
   * @param status the http status
   */
  public void setStatus(Integer status)
  {
    data().put(STATUS, status);
  }
}
