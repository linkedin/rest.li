/*
   Copyright (c) 2021 LinkedIn Corp.

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

package com.linkedin.data.schema;

import com.linkedin.data.DataMap;


/**
 * Represents a projection mask using a DataMap. The DataMap structure is same as the data being projected, with values
 * storing the masks. Positive masks are stored as 1 and negative masks as 0.
 */
public class FieldMask
{
  public static final int POSITIVE_MASK = 1;
  public static final int NEGATIVE_MASK = 0;

  /**
   * Initialize a new {@link FieldMask}.
   */
  public FieldMask()
  {
    _representation = new DataMap();
  }

  /**
   * Initialize a new {@link FieldMask} using the given initial capacity for the map.
   */
  public FieldMask(int capacity)
  {
    _representation = new DataMap(capacity);
  }

  /**
   * Initialize a new {@link FieldMask}.
   *
   * @param rep a DataMap representation of the MaskTree
   */
  public FieldMask(DataMap rep)
  {
    _representation = rep;
  }

  /**
   * Returning the underlying representation of this {@link FieldMask}.
   * @return the {@link DataMap} representing this MaskTree
   */
  public DataMap getDataMap()
  {
    return _representation;
  }

  @Override
  public String toString()
  {
    return _representation.toString();
  }

  protected final DataMap _representation;
}
