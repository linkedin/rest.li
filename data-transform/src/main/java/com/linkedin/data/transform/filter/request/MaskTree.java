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

package com.linkedin.data.transform.filter.request;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.transform.DataComplexProcessor;
import com.linkedin.data.transform.DataProcessingException;
import com.linkedin.data.transform.Escaper;
import com.linkedin.data.transform.filter.MaskComposition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Josh Walker
 * @version $Revision: $
 *
 * MaskTree represents a structural mask that can be applied to a DataMap to select a subset of the
 * DataMap's fields.  A MaskTree consists of key/value pairs, where the key is the name of the field
 * to be operated upon, and the value is either a PositiveMaskOp, a NegativeMaskOp, or a MaskTree.
 * Internally, a MaskTree is represented as a DataMap, with PositiveMaskOp represented as the Integer 1,
 * and NegativeMaskOp represented as the Integer 0.
 *
 * The semantics of applying a MaskTree to a DataMap are documented in {@link com.linkedin.data.transform.DataComplexProcessor}
 */

public class MaskTree
{
  /**
   * Initialize a new {@link MaskTree}.
   */
  public MaskTree()
  {
    _representation = new DataMap();
  }

  /**
   * Initialize a new {@link MaskTree}.
   *
   * @param rep a DataMap representation of the MaskTree
   */
  public MaskTree(DataMap rep)
  {
    _representation = rep;
  }

  /**
   * Add an operation to this {@link MaskTree}, given a path indicating the field to which the operation
   * applies and a {@link MaskOperation} representing the operation to be applied.
   * @param path the path of the field to which the operation applies
   * @param op the MaskOperation to be performed on the field
   */
  public void addOperation(PathSpec path, MaskOperation op)
  {
    List<String> segments = path.getPathComponents();


    final DataMap fieldMask = new DataMap();
    DataMap map = fieldMask;  //map variable contains DataMap, into which current segment will be put
    for (int ii = 0; ii<segments.size()-1; ++ii)
    {
      String segment = Escaper.escapePathSegment(segments.get(ii));
      DataMap childMap = new DataMap();
      map.put(segment, childMap);
      map = childMap;
    }
    String lastSegment = Escaper.escapePathSegment(segments.get(segments.size()-1));
    map.put(lastSegment, op.getRepresentation());

    //compose existing tree with mask for specific field
    try
    {
      new DataComplexProcessor(new MaskComposition(), fieldMask, _representation).run(false);
    }
    catch (DataProcessingException e)
    {
      throw new IllegalStateException("error while building mask tree", e);
    }
  }

  /**
   * Obtain a flattened represented of this {@link MaskTree}, represented as a Map from field path to
   * {@link MaskOperation}.
   * @return Map representing flattened MaskTree.
   */
  public Map<PathSpec, MaskOperation> getOperations()
  {
    Map<PathSpec, MaskOperation> result = new HashMap<PathSpec, MaskOperation>();
    getOperationsImpl(_representation, PathSpec.emptyPath(), result);
    return result;
  }

  /**
   * Returning the underlying representation of this {@link MaskTree}.
   * @return the {@link DataMap} representing this MaskTree
   */
  public DataMap getDataMap()
  {
    return _representation;
  }

  private void getOperationsImpl(DataMap data, PathSpec path, Map<PathSpec, MaskOperation> result)
  {
    for (Map.Entry<String, Object> entry : data.entrySet())
    {
      String segment = Escaper.unescapePathSegment(entry.getKey());
      PathSpec subpath = new PathSpec(path.getPathComponents(), segment);
      Object value = entry.getValue();
      if (value instanceof Integer)
      {
        if (value.equals(MaskOperation.NEGATIVE_MASK_OP.getRepresentation()))
        {
          result.put(subpath, MaskOperation.NEGATIVE_MASK_OP);
        }
        else if (value.equals(MaskOperation.POSITIVE_MASK_OP.getRepresentation()))
        {
          result.put(subpath, MaskOperation.POSITIVE_MASK_OP);
        }
        else
        {
          throw new IllegalStateException("invalid mask tree");
        }
      }
      else if (value.getClass() == DataMap.class)
      {
        getOperationsImpl((DataMap) value, subpath, result);
      }
      else
      {
        throw new IllegalStateException("invalid mask tree");
      }
    }
  }

  @Override
  public String toString()
  {
    return _representation.toString();
  }

  private DataMap _representation;
}
