package com.linkedin.restli.examples.greetings.server;


import com.linkedin.data.transform.filter.request.MaskOperation;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.server.ProjectionMode;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.annotations.Optional;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.examples.greetings.api.Greeting;


/**
 * Resource where all get operations are implemented to explicitly examine the projection
 * sent by the client and then manually apply the projection.
 */
@RestLiCollection(name = "manualProjections",
                  namespace = "com.linkedin.restli.examples.greetings.client")
public class ManualProjectionsResource extends CollectionResourceTemplate<Long, Greeting>
{
  @RestMethod.Get
  public Greeting get(Long key, @QueryParam("ignoreProjection") @Optional("false") boolean ignoreProjection)
  {
    ResourceContext context = getContext();

    Greeting greeting = new Greeting();

    context.setProjectionMode(ProjectionMode.MANUAL);
    MaskTree mask = context.getProjectionMask();
    if(mask != null && ignoreProjection == false)
    {
      if(mask.getOperations().get(Greeting.fields().message()) == MaskOperation.POSITIVE_MASK_OP)
      {
        greeting.setMessage("Projected message!");
      }

      if(mask.getOperations().get(Greeting.fields().tone()) == MaskOperation.POSITIVE_MASK_OP)
      {
        greeting.setTone(Tone.FRIENDLY);
      }

      if(mask.getOperations().get(Greeting.fields().id()) == MaskOperation.POSITIVE_MASK_OP)
      {
        greeting.setId(key);
      }
    }
    else
    {
      greeting.setMessage("Full greeting.");
      greeting.setTone(Tone.FRIENDLY);
      greeting.setId(key);
    }

    return greeting;
  }
}
