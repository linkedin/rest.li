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

public class PatchRequest<T> extends RecordTemplate
{
  private static final String PATCH="patch";

  /**
   * Initialize an empty PatchRequest.
   */
  public PatchRequest()
  {
    super(new DataMap(), null);
  }

  /**
   * Initialize a PatchRequest based off of the given DataMap.
   *
   * @param dataMap a DataMap
   */
  public PatchRequest(DataMap dataMap)
  {
    super(dataMap, null);
  }

  /**
   * Initialize and return a PatchRequest off of the given patch document.
   *
   * @param patchDocument a DataMap representing a patch
   * @param <T> the type of the object that the patchRequest will patch
   * @return a PatchRequest
   */
  public static <T> PatchRequest<T> createFromPatchDocument(DataMap patchDocument)
  {
    PatchRequest<T> result = new PatchRequest<T>();
    result.data().put(PATCH, patchDocument);
    return result;
  }

  /**
   * @return the patch document
   */
  public DataMap getPatchDocument()
  {
    return (DataMap)data().get(PATCH);
  }

}
