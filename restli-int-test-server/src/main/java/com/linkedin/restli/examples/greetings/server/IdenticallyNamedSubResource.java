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

import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;


/**
 * A Subresource whose unqualified name is identical to its parent
 *
 * N.B. The only reason a namespace is specified on this resource is to avoid clashing when the
 * client builders are generated.
 *
 * @author Keren Jin
 */
@RestLiCollection(parent = NoNamespaceResource.class,
                  name=NoNamespaceResource.RESOURCE_NAME,
                  namespace = "com.linkedin.restli.examples")
public class IdenticallyNamedSubResource extends CollectionResourceTemplate<Long, Greeting>
{
}
