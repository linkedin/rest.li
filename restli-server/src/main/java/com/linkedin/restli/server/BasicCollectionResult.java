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

package com.linkedin.restli.server;

import java.util.List;

import com.linkedin.data.template.RecordTemplate;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class BasicCollectionResult<T extends RecordTemplate> extends CollectionResult<T, RecordTemplate>
{
  public BasicCollectionResult(final List<T> elements)
  {
    super(elements, null, null);
  }

  public BasicCollectionResult(final List<T> elements, final Integer total)
  {
    super(elements, total, null);
  }
}
