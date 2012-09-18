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
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.transform.Escaper;

import java.util.Map;

/**
 * @author Josh Walker
 * @version $Revision: $
 *
 * PatchCreator is used to create a {@link PatchTree} by comparing ("diff-ing") DataMaps.
 *
 * See {@link PatchCreator#diff(com.linkedin.data.DataMap, com.linkedin.data.DataMap)} for a
 * detailed description of the semantics.
 */

public class PatchCreator
{
  /**
   * See {@link PatchCreator#diff(com.linkedin.data.DataMap, com.linkedin.data.DataMap)}
   *
   * @param original The original object for the diff
   * @param modified the modified object for the diff
   * @param <T> The subclass of RecordTemplate for original and modified.
   * @return a PatchTree as described above.
   */
  public static <T extends RecordTemplate> PatchTree diff(T original, T modified)
  {
    return diff(original.data(), modified.data());
  }

  /**
   * Create a PatchTree by by diff-ing the DataMap representations of two objects extending RecordTemplate.
   * The resulting PatchTree describes a set of modifications such that applying the modifications to the
   * original object will make it equal to the modified object.
   *
   * The PatchTree will contain a RemoveFieldOp for each field in original which does not exist in modified,
   * and a SetFieldOp for each field in modified which does not exist in original.  For fields which exist
   * in both original and modified, if the values are equal, no operation will be generated in the PatchTree.
   * If the field exists in both original and modified, and has different value, if the type in both objects
   * is a DataMap, the values will be recursively diff-ed.  If the type is not DataMap in both objects,
   * the PatchTree will contain a SetFieldOp.
   *
   * @param original The original object for the diff
   * @param modified the modified object for the diff
   * @return a PatchTree as described above.
  *
  */
  public static PatchTree diff(DataMap original, DataMap modified)
  {
    DataMap result = diffMaps(original, modified);
    if (result == null)
    {
      return new PatchTree();
    }
    return new PatchTree(result);
  }

  private static DataMap diffMaps(DataMap original, DataMap modified)
  {
    DataMap result = null;
    for (String key : original.keySet())
    {
      if (! modified.containsKey(key))
      {
        result = result==null ? new DataMap() : result;
        PatchOpFactory.REMOVE_FIELD_OP.store(result, key);
      }
      else
      {
        Object oValue = original.get(key);
        Object mValue = modified.get(key);
        if ((mValue.getClass() == DataMap.class) && (oValue.getClass() == DataMap.class))
        {
          DataMap subDiff = diffMaps((DataMap)oValue, (DataMap)mValue);
          if (subDiff != null)
          {
            result = result==null ? new DataMap() : result;
            result.put(Escaper.escapePathSegment(key), subDiff);
          }
        }
        else if (! oValue.equals(mValue))
        {
          result = result==null ? new DataMap() : result;
          PatchOpFactory.setFieldOp(mValue).store(result, key);
        }
      }
    }
    for (Map.Entry<String, Object> entry : modified.entrySet())
    {
      if (! original.containsKey(entry.getKey()))
      {
        result = result==null ? new DataMap() : result;
        PatchOpFactory.setFieldOp(entry.getValue()).store(result, entry.getKey());
      }
    }
    return result;
  }

}
