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


import com.linkedin.common.callback.Callback;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.Tasks;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.parseq.promise.Promises;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.CallbackParam;
import com.linkedin.restli.server.annotations.RestLiActions;

import java.util.concurrent.Callable;


/**
 * @author erli
 */
@RestLiActions(name = "asyncErrors", namespace = "com.linkedin.restli.examples.greetings.client")
public class AsyncErrorResource
{
  @Action(name = "promise")
  public Promise<Greeting> promise(@ActionParam(value = "id") final String key)
  {
    if (key.equals("returnNonService"))
    {
      // Non-RestLiServiceException passed to callback
      return Promises.error(new RestException(new RestResponseBuilder().setStatus(401).build()));
    }

    // RestLiServiceException passed to callback
    if (key.equals("returnService"))
    {
      return Promises.error(new RestLiServiceException(HttpStatus.S_401_UNAUTHORIZED));
    }

    //RestLiServiceException thrown
    if (key.equals("throwService"))
    {
      throw new RestLiServiceException(HttpStatus.S_401_UNAUTHORIZED);
    }

    if (key.equals("throwNonService"))
    {
      // Non-RestLiServiceException thrown
      throw new IllegalStateException();
    }

    return Promises.value(new Greeting());
  }

  @Action(name = "task")
  public Task<Greeting> task(@ActionParam(value = "id") final String key)
  {
    // Non-RestLiServiceException passed to callback
    if (key.equals("returnNonService"))
    {
      return Task.callable("", new Callable<Greeting>()
      {
        @Override
        public Greeting call() throws Exception
        {
          throw new RestException(new RestResponseBuilder().setStatus(401).build());
        }
      });
    }

    // RestLiServiceException passed to callback
    if (key.equals("returnService"))
    {
      return Task.callable("", new Callable<Greeting>() {
        @Override
        public Greeting call() throws Exception
        {
          throw new RestLiServiceException(HttpStatus.S_401_UNAUTHORIZED);
        }
      });
    }

    //RestLiServiceException thrown
    if (key.equals("throwService"))
    {
      throw new RestLiServiceException(HttpStatus.S_401_UNAUTHORIZED);
    }

    if (key.equals("throwNonService"))
    {
      // Non-RestLiServiceException thrown
      throw new IllegalStateException();
    }

    return Task.callable("", new Callable<Greeting>()
    {
      @Override
      public Greeting call() throws Exception
      {
        return new Greeting();
      }
    });
  }

  @Action(name = "callback")
  public void callback(@ActionParam(value = "id") final String key, @CallbackParam final Callback<Greeting> callback)
  {
    // Non-RestLiServiceException passed to callback
    if (key.equals("returnNonService"))
    {
      callback.onError(new RestException(new RestResponseBuilder().setStatus(401).build()));
    }

    // RestLiServiceException passed to callback
    if (key.equals("returnService"))
    {
      callback.onError(new RestLiServiceException(HttpStatus.S_401_UNAUTHORIZED));
    }

    //RestLiServiceException thrown
    if (key.equals("throwService"))
    {
      throw new RestLiServiceException(HttpStatus.S_401_UNAUTHORIZED);
    }

    if (key.equals("throwNonService"))
    {
      // Non-RestLiServiceException thrown
      throw new IllegalStateException();
    }

    callback.onSuccess(new Greeting());
  }
}
