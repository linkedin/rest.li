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


import com.linkedin.data.transform.DataProcessingException;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.RestLiSimpleResource;
import com.linkedin.restli.server.resources.SimpleResourceTemplate;
import com.linkedin.restli.server.util.PatchApplier;


/**
 * This resource represents a simple root resource.
 */
@RestLiSimpleResource(name="greeting", namespace="com.linkedin.restli.examples.greetings.client")
public class RootSimpleResource extends SimpleResourceTemplate<Greeting>
{
  private static Greeting GREETING = new Greeting().setId(12345)
                                                    .setMessage("Root Greeting")
                                                    .setTone(Tone.FRIENDLY);
  /**
   * Gets the greeting.
   */
  @Override
  public Greeting get()
  {
    return GREETING;
  }

  /**
   * Updates the greeting.
   */
  @Override
  public UpdateResponse update(Greeting greeting)
  {
    GREETING = greeting;
    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }

  /**
   * Updates the greeting.
   */
  @Override
  public UpdateResponse update(PatchRequest<Greeting> patchRequest)
  {
    try
    {
      PatchApplier.applyPatch(GREETING, patchRequest);
    }
    catch(DataProcessingException e)
    {
      return new UpdateResponse(HttpStatus.S_400_BAD_REQUEST);
    }

    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }

  /**
   * Deletes the greeting.
   */
  @Override
  public UpdateResponse delete()
  {
    GREETING = null;
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
   * An example action on the greeting which is explicitly set to entity-level.
   */
  @Action(name="exampleActionThatIsExplicitlyEntityLevel", resourceLevel=ResourceLevel.ENTITY)
  public int exampleActionThatIsExplicitlyEntityLevel(@ActionParam("param1") int param1)
  {
    return param1 * 11;
  }

  /**
   * An example action on the greeting which is explicitly set to any-level.
   */
  @Action(name="exampleActionThatIsExplicitlyAnyLevel", resourceLevel=ResourceLevel.ANY)
  public int exampleActionThatIsExplicitlyAnyLevel(@ActionParam("param1") int param1)
  {
    return param1 * 12;
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
