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
package com.linkedin.restli.server.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.transform.DataMapProcessor;
import com.linkedin.data.transform.filter.Filter;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.data.transform.patch.PatchConstants;
import com.linkedin.restli.common.PatchRequest;

/**
 * This class provides methods useful for manipulation on patches.
 *
 * @author jodzga
 */
public class PatchHelper
{

  private static final DataMap EMPTY_DATAMAP = new DataMap();
  static
  {
    EMPTY_DATAMAP.makeReadOnly();
  }

  /**
   * Returns new PatchRequest instance, which represent subset of modifications of PatchRequest
   * passed as a parameter: resulting PatchRequest will modify only fields specified by a given projection.
   * @param patch original patch
   * @param projection projection to be applied on patch
   * @return new instance of patch containing only modifications to fields specified by projection
   */
  public static <T extends RecordTemplate> PatchRequest<T> applyProjection(PatchRequest<T> patch, MaskTree projection)
  {
    try
    {
      /**
       * Implementation consists of 3 steps:
       * 1) move data conveyed by patch out of meta-commands (expose method)
       * 2) apply projection on it
       * 2) trim original patch with result of projection
       * In order to limit amount of generated garbage operations are
       * performed in-place
       */
      DataMap forProjecting = patch.getPatchDocument().copy();
      expose(forProjecting);
      DataMap projected = project(forProjecting, projection);
      DataMap forTrimming = patch.getPatchDocument().copy();
      trim(forTrimming, projected);
      return PatchRequest.createFromPatchDocument(forTrimming);
    }
    catch (CloneNotSupportedException e)
    {
      throw new IllegalArgumentException("Patch must be cloneable in order to apply projection to it", e);
    }
  }

  /**
   * This method trims doc (1st arg) according to projected (2nd arg),
   * which may have been modified by projection.
   * This method works in-place, meaning that the doc may be mutated.
   */
  private static void trim(DataMap doc, DataMap projected)
  {
    DataMap toAddDoc = new DataMap();
    Set<String> fields = doc.keySet();
    List<String> toRemoveDoc = new ArrayList<String>(fields.size());
    for (String  f : fields)
    {
      Object v = doc.get(f);
      if (f.equals(PatchConstants.DELETE_COMMAND))
      {
        DataList deletedFields = (DataList)v;
        DataList filteredDeleteFields = new DataList();
        for (Object patchDeleteField : deletedFields)
        {
          if (projected.containsKey(patchDeleteField))
          {
            filteredDeleteFields.add(patchDeleteField);
          }
        }
        toRemoveDoc.add(f);
        if (!filteredDeleteFields.isEmpty())
        {
          toAddDoc.put(PatchConstants.DELETE_COMMAND, filteredDeleteFields);
        }
      }
      else if (f.equals(PatchConstants.SET_COMMAND))
      {
        DataMap setFields = (DataMap)v;
        Set<String> setFieldNames = setFields.keySet();
        List<String> toRemove = new LinkedList<String>();
        DataMap filteredSetFields = new DataMap();
        for (String setFieldName: setFieldNames)
        {
          if (projected.containsKey(setFieldName))
          {
            filteredSetFields.put(setFieldName, projected.get(setFieldName));
          }
          toRemove.add(setFieldName);
        }
        for (String fieldToRemove: toRemove)
        {
          setFields.remove(fieldToRemove);
          if (filteredSetFields.containsKey(fieldToRemove))
          {
            setFields.put(fieldToRemove, filteredSetFields.get(fieldToRemove));
          }
        }
        if (setFields.isEmpty()) {
          toRemoveDoc.add(f);
        }
      }
      else if (v instanceof DataMap)
      {
        if (projected.containsKey(f))
        {
          trim((DataMap)v, (DataMap)projected.get(f));
        }
        else
        {
          toRemoveDoc.add(f);
        }
      }
    }
    //apply changes to doc
    for (String f : toRemoveDoc)
    {
      doc.remove(f);
    }
    for (String f : toAddDoc.keySet())
    {
      doc.put(f, toAddDoc.get(f));
    }
  }

  /**
   * Applies projection, works in-place, meaning that the forProjection may be mutated.
   */
  private static DataMap project(DataMap forProjection, MaskTree projection)
  {
    if (projection == null)
    {
      return forProjection;
    }
    //Special-case: when present, an empty filter should not return any fields.
    else if (projection.getDataMap().isEmpty())
    {
      return EMPTY_DATAMAP;
    }
    try
    {
      new DataMapProcessor(new Filter(), projection.getDataMap(), forProjection).run(false);
      return forProjection;
    }
    catch (Exception e)
    {
      throw new RuntimeException("Error projecting fields", e);
    }
  }

  /**
   * This method 'exposes' changes conveyed in the patch's
   * meta commands to the main document. Contents of $set
   * commands are moved to the node which contains $set command.
   * Names of removed fields from $delete commands are moved
   * to the nod which contains $delete command.
   * The effect is that patch will resemble structurally
   * document which it is supposed to modify. This allows
   * application of projection to such patch to discover which
   * changes relate to fields specified by that projection.
   * Examples:
   *  $delete: ['x', 'y', 'z'] => x: true, y: true, z: true
   *  $set: {x: 10, y: {z: 'yeey'}, t: [10]} => x: 10, y: {z: 'yeey'}, t: [10]
   *
   * This method works in-place, meaning that the doc may be mutated.
   */
  private static void expose(DataMap doc)
  {
    Set<String> fields = doc.keySet();
    DataMap toAdd = new DataMap();
    for (String  f : fields)
    {
      Object v = doc.get(f);
      if (f.equals(PatchConstants.DELETE_COMMAND))
      {
        for (Object removedFields : (DataList) v)
        {
          toAdd.put((String)removedFields, true);
        }
      }
      else if (f.equals(PatchConstants.SET_COMMAND))
      {
        toAdd.putAll((DataMap)v);
      }
      else if (v instanceof DataMap)
      {
        expose((DataMap)v);
      }
    }
    doc.putAll(toAdd);
  }
}
