/*
   Copyright (c) 2021 LinkedIn Corp.

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

package com.linkedin.restli.examples.greetings.server;

import com.linkedin.restli.examples.greetings.api.ValidateEmptyUnion;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;


/**
 * Resource for testing Rest.li empty union data validation.
 * @author Brian Pin
 */
@RestLiCollection(name = "emptyUnion", namespace = "com.linkedin.restli.examples.greetings.client")
public class ValidateEmptyUnionResource extends CollectionResourceTemplate<Long, ValidateEmptyUnion>
{
  // write some resource method to provide the data of the ValidateEmptyUnion record
  @Override
  public ValidateEmptyUnion get(Long keyId)
  {
    ValidateEmptyUnion union = new ValidateEmptyUnion();
    ValidateEmptyUnion.Foo foo = new ValidateEmptyUnion.Foo();
    union.setFoo(foo);
    return union;
  }
}
