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

package com.linkedin.restli.examples.greetings.server;

import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.examples.custom.types.CustomLong;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.typeref.api.CustomLongRef;
import com.linkedin.restli.examples.typeref.api.DateRef;
import com.linkedin.restli.server.annotations.AssocKey;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Key;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.resources.AssociationResourceTemplate;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author Moira Tagle
 * @version $Revision: $
 */

@RestLiAssociation(
        name = "customTypes3",
        namespace = "com.linkedin.restli.examples.greetings.client",
        assocKeys = {
                @Key(name = "longId", type = CustomLong.class, typeref = CustomLongRef.class),
                @Key(name = "dateId", type = Date.class, typeref = DateRef.class)
        }
)
public class CustomTypesResource3 extends AssociationResourceTemplate<Greeting>
{

  @Override
  public Greeting get(CompoundKey key)
  {
    CustomLong longId = (CustomLong)key.getPart("longId");
    Date dateId = (Date)key.getPart("dateId");

    return new Greeting().setId(longId.toLong() + dateId.getTime());
  }

  @Finder("dateOnly")
  public List<Greeting> dateOnly(@AssocKey(value="dateId", typeref=DateRef.class) Date dateId)
  {
    return Collections.emptyList();
  }

}
