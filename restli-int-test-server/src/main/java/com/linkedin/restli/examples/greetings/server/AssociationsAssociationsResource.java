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

import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.server.annotations.Key;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.resources.AssociationResourceTemplate;


/**
 * Association resource under a parent association resource
 */
@RestLiAssociation(name = "associationsAssociations", parent = AssociationsResource.class,
    namespace = "com.linkedin.restli.examples.greetings.client",
    assocKeys = {
    @Key(name = "anotherSrc", type = String.class),
        @Key(name = "anotherDest", type = String.class)})
public class AssociationsAssociationsResource extends AssociationResourceTemplate<Message>
{

}
