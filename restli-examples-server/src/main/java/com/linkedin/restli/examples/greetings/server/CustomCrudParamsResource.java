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

import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.groups.api.GroupMembershipParam;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Optional;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.KeyValueResource;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

@RestLiCollection(name = "greetingsAuth",
                  namespace = "com.linkedin.restli.examples.greetings.client")
public class CustomCrudParamsResource implements KeyValueResource<Long, Greeting>
{
  private static GreetingsResourceImpl _impl = new GreetingsResourceImpl("greetingsAuth");

  @RestMethod.Get
  public Greeting getGreeting(Long idx, @Optional @QueryParam("auth") String auth,
                              @Optional @QueryParam("testComplex") GroupMembershipParam complexParam)
  {
    validateAuth(auth);
    Greeting g = _impl.get(idx);
    return g;
  }

  @RestMethod.Create
  public CreateResponse createGreeting(Greeting entity, @Optional @QueryParam("auth") String auth)
  {
    validateAuth(auth);
    return _impl.create(entity);
  }

  @RestMethod.Delete
  public UpdateResponse deleteGreeting(Long key,  @Optional @QueryParam("auth") String auth)
  {
    validateAuth(auth);
    return _impl.delete(key);
  }

  @RestMethod.PartialUpdate
  public UpdateResponse updateGreeting(Long key, PatchRequest<Greeting> patch, @Optional @QueryParam("auth") String auth)
  {
    validateAuth(auth);
    return _impl.update(key, patch);
  }

  @RestMethod.Update
  public UpdateResponse updateGreeting(Long key, Greeting entity, @Optional @QueryParam("auth") String auth)
  {
    validateAuth(auth);
    return _impl.update(key, entity);
  }

  void validateAuth(String auth)
  {
    if (auth==null || ! auth.equals("PLEASE"))
    {
      throw new RestLiServiceException(HttpStatus.S_401_UNAUTHORIZED, "Invalid auth token");
    }
  }

}
