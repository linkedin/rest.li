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

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.transform.DataComplexProcessor;
import com.linkedin.data.transform.DataProcessingException;
import com.linkedin.data.transform.patch.Patch;
import com.linkedin.restli.common.PatchRequest;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class PatchApplier
{
  public static <T extends RecordTemplate> void applyPatch(T original,
                                                           PatchRequest<T> patch) throws DataProcessingException
  {
    DataComplexProcessor processor =
        new DataComplexProcessor(new Patch(), patch.getPatchDocument(), original.data());
    processor.run(false);
  }
}
