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
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class PatchRequest<T> extends RecordTemplate
{
  public static final String PATCH = "patch";
  private static final String SCHEMA_STRING = "{\n" +
      "  \"type\" : \"record\",\n" +
      "  \"name\" : \"PatchRequest\",\n" +
      "  \"namespace\" : \"com.linkedin.restli.common\",\n" +
      "  \"fields\" : []\n" +
      "}";
  private static final RecordDataSchema SCHEMA = (RecordDataSchema) DataTemplateUtil.parseSchema(SCHEMA_STRING);

  /**
   * Initialize an empty PatchRequest.
   */
  public PatchRequest()
  {
    super(new DataMap(), SCHEMA);
  }

  /**
   * Initialize a PatchRequest based off of the given DataMap.
   *
   * @param dataMap a DataMap
   */
  public PatchRequest(DataMap dataMap)
  {
    super(dataMap, SCHEMA);
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
    PatchRequest<T> result = new PatchRequest<>();
    result.data().put(PATCH, patchDocument);
    return result;
  }

  /**
   * Initialize and return an empty PatchRequest.
   *
   * @param <T> the type of the object that the patchRequest will patch
   * @return an empty PatchRequest
   */
  public static <T> PatchRequest<T> createFromEmptyPatchDocument()
  {
    return createFromPatchDocument(new DataMap());
  }

  /**
   * @return the patch document
   */
  public DataMap getPatchDocument()
  {
    return (DataMap)data().get(PATCH);
  }

}
