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

package com.linkedin.restli.server.resources.fixtures;

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.server.TestRecord;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;

/**
 * @author dellamag
 */
@RestLiCollection(name="someResource2",
                  keyName="key")
public class SomeResource2 extends CollectionResourceTemplate<String,TestRecord>
{
  // no deps
}
