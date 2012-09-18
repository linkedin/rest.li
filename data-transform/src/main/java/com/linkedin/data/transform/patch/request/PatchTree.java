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
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.transform.Escaper;
import com.linkedin.util.ArgumentUtil;

import java.util.List;

/**
 * @author Josh Walker
 * @version $Revision: $
 *
 * PatchTree represents a set of mutations that can be applied to a DataMap. A PatchTree consists
 * of key/value pairs, where the key is either the name of a class implementing {@link PatchOperation},
 * in which case the value represents the arguments for the operation, or the key may be the name
 * of a field, in which case the value represents a PatchTree to be recursively applied to the
 * value of that field.
 *
 * Internally, a PatchTree is represented as a DataMap.
 *
 * The semantics of applying a PatchTree to a DataMap are
 * documented in {@link com.linkedin.data.transform.DataMapProcessor}
 */

public class PatchTree
{
  /**
   * Initialize a new {@link PatchTree}.
   */
  public PatchTree()
  {
    _representation = new DataMap();
  }

  /**
   * Initialize a new {@link PatchTree}.
   *
   * @param rep {@link DataMap} representation of a PatchTree
   */
  public PatchTree(DataMap rep)
  {
    ArgumentUtil.notNull(rep, "rep");
    _representation = rep;
  }

  /**
   * Adds the operation op to the key path.
   *
   * @param path the path to a field in the object
   * @param op the operation to apply to the path
   */
  public void addOperation(PathSpec path, PatchOperation op)
  {
    List<String> segments = path.getPathComponents();

    DataMap map = _representation;
    for (int ii = 0; ii<segments.size() - 1; ++ii)
    {
      String segment = Escaper.escapePathSegment(segments.get(ii));
      Object o = map.get(segment);
      if (o == null)
      {
        DataMap childMap = new DataMap();
        map.put(segment, childMap);
        map = childMap;
      }
      else
      {
        map = (DataMap)o;
      }
    }
    String lastSegment = Escaper.escapePathSegment(segments.get(segments.size()-1));
    op.store(map, lastSegment);
  }

  /**
   * @return the underlying DataMap of the PatchTree
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

  private DataMap _representation;
}
