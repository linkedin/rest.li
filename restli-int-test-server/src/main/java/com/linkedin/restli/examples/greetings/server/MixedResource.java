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
import com.linkedin.parseq.promise.Promise;
import com.linkedin.parseq.promise.Promises;
import com.linkedin.parseq.promise.SettablePromise;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.CallbackParam;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod.Create;
import com.linkedin.restli.server.annotations.RestMethod.Delete;
import com.linkedin.restli.server.annotations.RestMethod.Get;
import com.linkedin.restli.server.annotations.RestMethod.Update;
import com.linkedin.restli.server.resources.KeyValueResource;
import com.linkedin.restli.server.resources.ResourceContextHolder;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This resource demonstrates mixing of various method signatures: synchronous, callback,
 * promise
 *
 * @author jnwang
 */
@RestLiCollection(name = "mixed", namespace = "com.linkedin.restli.examples.greetings.client")
public class MixedResource extends ResourceContextHolder implements
    KeyValueResource<Long, Greeting>
{
  private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private final static int                      DELAY     = 200;

  @Get
  public Greeting get(Long key)
  {
    return new Greeting().setMessage(key.toString());
  }

  @Create
  public void create(Greeting entity, @CallbackParam final Callback<CreateResponse> callback)
  {
    final Runnable requestHandler = new Runnable()
    {
      public void run()
      {
        callback.onSuccess(new CreateResponse(HttpStatus.S_200_OK));
      }
    };
    scheduler.schedule(requestHandler, DELAY, TimeUnit.MILLISECONDS);
  }

  @Update
  public Promise<UpdateResponse> update(Long key, Greeting entity)
  {
    final SettablePromise<UpdateResponse> result = Promises.settable();
    Runnable requestHandler = new Runnable()
    {
      public void run()
      {
        result.done(new UpdateResponse(HttpStatus.S_200_OK));
      }
    };
    scheduler.schedule(requestHandler, DELAY, TimeUnit.MILLISECONDS);
    return result;
  }

  @Delete
  public Promise<UpdateResponse> delete(Long key)
  {
    return Promises.value(new UpdateResponse(HttpStatus.S_200_OK));
  }

  @Finder("search")
  public Promise<List<Greeting>> search(@QueryParam("what") final String s)
  {
    final SettablePromise<List<Greeting>> result = Promises.settable();
    Runnable requestHandler = new Runnable()
    {
      public void run()
      {
        result.done(Arrays.asList(new Greeting().setMessage(s)));
      }
    };
    scheduler.schedule(requestHandler, DELAY, TimeUnit.MILLISECONDS);
    return result;
  }

  @Action(name = "theAction")
  public void theAction(@CallbackParam Callback<String> callback)
  {
    callback.onSuccess("theResult");
  }
}
