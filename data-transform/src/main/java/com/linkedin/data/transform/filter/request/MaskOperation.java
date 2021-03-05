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

package com.linkedin.data.transform.filter.request;

import com.linkedin.data.schema.FieldMask;


/**
 * @author Josh Walker
 * @version $Revision: $
 */
public enum MaskOperation
{
  POSITIVE_MASK_OP(FieldMask.POSITIVE_MASK),
  NEGATIVE_MASK_OP(FieldMask.NEGATIVE_MASK);

  MaskOperation(int value)
  {
    _representation = value;
  }

  /**
   * return the representation of this MaskOperation
   * @return an Integer, either 0 or 1
   */
  public Integer getRepresentation()
  {
    return _representation;
  }

  private final Integer _representation;
}
