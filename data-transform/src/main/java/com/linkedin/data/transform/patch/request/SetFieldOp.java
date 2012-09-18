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

package com.linkedin.data.transform.patch.request;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.transform.patch.PatchConstants;


/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class SetFieldOp implements PatchOperation
{
  public static final String OP_NAME = PatchConstants.SET_COMMAND;

  /**
   * Initialize a {@link PatchOperation}.
   *
   * @param value the value to set the field to
   */
  public SetFieldOp(Object value)
  {
    if (value instanceof DataTemplate)
    {
      _operand = ((DataTemplate) value).data();
    }
    else
    {
      _operand = value;
    }
  }

  @Override
  public void store(DataMap parentMap, String key)
  {
    DataMap setMap = (DataMap)parentMap.get(OP_NAME);
    if (setMap == null)
    {
      setMap = new DataMap();
      parentMap.put(OP_NAME, setMap);
    }

    setMap.put(key, _operand);
  }

  private final Object _operand;
}
