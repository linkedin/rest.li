/**
 * $Id: $
 */

package com.linkedin.restli.examples.greetings.server;

import com.linkedin.restli.examples.custom.types.CustomLong;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.typeref.api.CustomLongRef;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Moira Tagle
 * @version $Revision: $
 */

@RestLiCollection(name="customTypes2",
                  namespace = "com.linkedin.restli.examples.greetings.client",
                  keyTyperefClass = CustomLongRef.class)
public class CustomTypesResource2 extends CollectionResourceTemplate<CustomLong, Greeting>
{

  @Override
  public Greeting get(CustomLong lo)
  {
    return new Greeting().setId(lo.toLong());
  }

  @Override
  public Map<CustomLong, Greeting> batchGet(Set<CustomLong> ids)
  {
    Map<CustomLong, Greeting> result = new HashMap<CustomLong, Greeting>(ids.size());

    for (CustomLong id: ids)
    {
      result.put(id, new Greeting().setId(id.toLong()));
    }

    return result;
  }

}
