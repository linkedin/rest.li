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


import com.linkedin.data.DataMap;
import com.linkedin.data.transform.DataProcessingException;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.RestLiSimpleResource;
import com.linkedin.restli.server.resources.SimpleResourceTemplate;
import com.linkedin.restli.server.util.PatchApplier;
import java.util.HashMap;
import java.util.Map;


/**
 * This resource represents a simple sub-resource.
 */
@RestLiSimpleResource(name="subsubgreeting", namespace="com.linkedin.restli.examples.greetings.client",
                    parent = CollectionUnderSimpleResource.class)
public class SimpleResourceUnderCollectionResource extends SimpleResourceTemplate<Greeting>
{
  private static final Tone DEFAULT_TONE = Tone.FRIENDLY;

  private static Map<Long, Tone> TONES = new HashMap<Long, Tone>();

  static
  {
    TONES.put(1L, DEFAULT_TONE);
  }

  /**
   * Gets the greeting.
   */
  @Override
  public Greeting get()
  {
    Long key = this.getContext().getPathKeys().get("subgreetingsId");

    return TONES.containsKey(key) ? new Greeting().setId(key * 10)
                                                  .setMessage("Subsubgreeting")
                                                  .setTone(TONES.get(key)) :
                                    null;
  }

  /**
   * Updates the greeting.
   */
  @Override
  public UpdateResponse update(Greeting greeting)
  {
    Long key = this.getContext().getPathKeys().get("subgreetingsId");
    TONES.put(key, greeting.getTone());
    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }

  /**
   * Updates the greeting.
   */
  @Override
  public UpdateResponse update(PatchRequest<Greeting> patchRequest)
  {
    Long key = this.getContext().getPathKeys().get("subgreetingsId");
    if (TONES.containsKey(key))
    {
      try
      {
        Greeting patched = new Greeting();
        PatchApplier.applyPatch(patched, patchRequest);
        TONES.put(key, patched.getTone());
        return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
      }
      catch(DataProcessingException e)
      {
        return new UpdateResponse((HttpStatus.S_400_BAD_REQUEST));
      }
    }
    else
    {
      return new UpdateResponse(HttpStatus.S_404_NOT_FOUND);
    }
  }

  /**
   * Deletes the greeting.
   */
  @Override
  public UpdateResponse delete()
  {
    Long key = this.getContext().getPathKeys().get("subgreetingsId");
    TONES.remove(key);
    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }

  /**
   * An example action on the greeting.
   */
  @Action(name="exampleAction")
  public int exampleAction(@ActionParam("param1") int param1)
  {
    return param1 * 10;
  }

  /**
   * An example action throwing an exception.
   */
  @Action(name = "exceptionTest")
  public void exceptionTest()
  {
    throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, "Test Exception");
  }
}
