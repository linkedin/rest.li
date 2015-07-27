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

package com.linkedin.multipart.utils;


import com.linkedin.data.ByteString;

import java.util.Map;


/**
 * Represents in an-memory multipart mime data source used for testing.
 *
 * @author Karim Vidhani
 */
public final class MIMEDataPart
{
  private final ByteString _partData;
  private final Map<String, String> _headers;

  public MIMEDataPart(final ByteString partData, final Map<String, String> headers)
  {
    if (partData == null)
    {
      _partData = ByteString.empty();
    }
    else
    {
      _partData = partData;
    }
    _headers = headers;
  }

  public ByteString getPartData()
  {
    return _partData;
  }

  public Map<String, String> getPartHeaders()
  {
    return _headers;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
    {
      return true;
    }

    if (!(o instanceof MIMEDataPart))
    {
      return false;
    }

    final MIMEDataPart that = (MIMEDataPart) o;

    if (!_headers.equals(that.getPartHeaders()))
    {
      return false;
    }

    if (!_partData.equals(that.getPartData()))
    {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode()
  {
    int result = _partData != null ? _partData.hashCode() : 0;
    result = 31 * result + (_headers != null ? _headers.hashCode() : 0);
    return result;
  }
}