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

package com.linkedin.restli.internal.server.methods.response;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.HttpStatus;

/**
 * @author Josh Walker
 * @version $Revision: $
 */
public class PartialRestResponse
{
  private final HttpStatus _status;
  private final DataMap    _data;

  /**
   * @param record response data. The status is set to 200.
   */
  public PartialRestResponse(final RecordTemplate record)
  {
    this(HttpStatus.S_200_OK, record);
  }

  /**
   * @param status http response status
   */
  public PartialRestResponse(final HttpStatus status)
  {
    this(status, null);
  }

  /**
   * @param status http response status
   * @param record response data
   */
  public PartialRestResponse(final HttpStatus status, final RecordTemplate record)
  {
    if (record != null)
    {
      _data = record.data();
    }
    else
    {
      _data = null;
    }

    _status = status;
  }

  /**
   * @return true if response contains data, null otherwise.
   */
  public boolean hasData()
  {
    return _data != null;
  }

  public DataMap getDataMap()
  {
    return _data;
  }

  public HttpStatus getStatus()
  {
    return _status;
  }
}
