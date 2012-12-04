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
