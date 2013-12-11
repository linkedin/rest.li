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

package com.linkedin.restli.examples.greetings.server;


import com.linkedin.restli.examples.custom.types.CustomLong;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.typeref.api.CustomLongRef;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.annotations.RestLiCollection;

import com.linkedin.restli.server.resources.CollectionResourceTemplate;


/**
 * @author Moira Tagle
 * @version $Revision: $
 */

@RestLiCollection(name="customTypes4",
                  namespace = "com.linkedin.restli.examples.greetings.client",
                  keyTyperefClass = CustomLongRef.class,
                  parent = CustomTypesResource2.class)
public class CustomTypesResource4 extends CollectionResourceTemplate<CustomLong, Greeting>
{

  @Override
  public Greeting get(CustomLong lo)
  {
    CustomLong customTypes2Id = (CustomLong)getContext().getPathKeys().get("customTypes2Id");
    return new Greeting().setId(customTypes2Id.toLong() * lo.toLong());
  }

}
