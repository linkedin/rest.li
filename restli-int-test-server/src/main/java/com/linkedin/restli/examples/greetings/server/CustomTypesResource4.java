/**
 * $Id: $
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
