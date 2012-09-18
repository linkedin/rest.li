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

package com.linkedin.restli.client.util;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.transform.patch.request.PatchCreator;
import com.linkedin.data.transform.patch.request.PatchTree;
import com.linkedin.restli.common.PatchRequest;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class PatchGenerator
{
  private static final DataMap _emptyDataMap = new DataMap();

  public static <T extends RecordTemplate> PatchRequest<T> diff(T original, T revised)
  {
    PatchTree patch = PatchCreator.diff(original, revised);
    return PatchRequest.createFromPatchDocument(patch.getDataMap());
  }

  public static <T extends RecordTemplate> PatchRequest<T> diffEmpty(T revised)
  {
    PatchTree patch = PatchCreator.diff(_emptyDataMap, revised.data());
    return PatchRequest.createFromPatchDocument(patch.getDataMap());
  }
}
