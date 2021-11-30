/*
   Copyright (c) 2014 LinkedIn Corp.

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
import com.linkedin.data.template.StringArray;
import com.linkedin.parseq.BaseTask;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.parseq.promise.Promises;
import com.linkedin.parseq.promise.SettablePromise;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.examples.greetings.api.Empty;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.SearchMetadata;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.server.ActionResult;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchResult;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.CallbackParam;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.PagingContextParam;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Tests to observe restli's resilience for resource methods returning null. We are simply reusing
 * the Greetings model here for our own null-generating purposes.
 *
 * @author Karim Vidhani
 */
@RestLiCollection(name = "nullGreeting", namespace = "com.linkedin.restli.examples.greetings.client")
public class NullGreetingsResourceImpl extends CollectionResourceTemplate<Long, Greeting>
{
  private static final String[] GREETINGS =
      {"Good morning!", "Guten Morgen!", "Buenos dias!", "Bon jour!", "Buon Giorno!"};
  private static final Tone[] TONES = {Tone.FRIENDLY, Tone.SINCERE, Tone.INSULTING};
  private static final int INITIAL_SIZE = 20;
  private static final String[] INITIAL_MESSAGES = new String[INITIAL_SIZE];
  private static final Tone[] INITIAL_TONES = new Tone[INITIAL_SIZE];
  private static Long ID_SEQ = 0l;
  private static final Map<Long, Greeting> DB = new HashMap<>();
  private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);
  private static final int DELAY = 100;

  static
  {
    // generate some "random" initial data
    for (int i = 0; i < INITIAL_SIZE; i++)
    {
      INITIAL_MESSAGES[i] = GREETINGS[i % GREETINGS.length];
    }
    for (int i = 0; i < INITIAL_SIZE; i++)
    {
      INITIAL_TONES[i] = TONES[i % TONES.length];
    }

    for (int i = 0; i < INITIAL_SIZE; i++)
    {
      Greeting g =
          new Greeting().setId(ID_SEQ++).setMessage(INITIAL_MESSAGES[i]).setTone(INITIAL_TONES[i]);
      DB.put(g.getId(), g);
    }
  }

  public NullGreetingsResourceImpl()
  {
  }

  @RestMethod.Create
  public CreateResponse create(Greeting entity)
  {
    //Based off of the message in the greeting, we send back various types of nulls
    if (entity.getMessage().equalsIgnoreCase("nullCreateResponse"))
    {
      //Return a null CreateResponse
      return null;
    }
    else
    {
      //Return a valid CreateResponse but with a null HttpStatus
      final HttpStatus nullStatus = null;
      return new CreateResponse(nullStatus);
    }
    //Note, we don't need a test for returning a null entityID
  }

  @Finder("searchReturnNullList")
  public List<Greeting> searchReturnNullList(@PagingContextParam PagingContext ctx, @QueryParam("tone") Tone tone)
  {
    if (tone == Tone.INSULTING)
    {
      //return a null list
      return null;
    }
    else
    {
      //return a list with a null element in it
      final List<Greeting> greetings = new ArrayList<>();
      greetings.add(null);
      greetings.add(DB.get(1));
      return greetings;
    }
  }

  @Finder("searchReturnNullCollectionList")
  public CollectionResult<Greeting, SearchMetadata> searchReturnNullCollectionList(@PagingContextParam PagingContext ctx,
      @QueryParam("tone") Tone tone)
  {
    if (tone == Tone.INSULTING)
    {
      //return a null CollectionResult
      return null;
    }
    else if (tone == Tone.SINCERE)
    {
      //return a CollectionResult with a null list
      return new CollectionResult<>(null);
    }
    else
    {
      //return a CollectionResult with a list that has a null element in it
      final List<Greeting> greetings = new ArrayList<>();
      greetings.add(null);
      greetings.add(DB.get(1));
      return new CollectionResult<>(greetings);
    }
  }

  @RestMethod.Get
  public Greeting get(Long key)
  {
    return null;
  }

  @RestMethod.GetAll
  public CollectionResult<Greeting, Empty> getAllCollectionResult(@PagingContextParam PagingContext ctx)
  {
    return null;
  }

  @RestMethod.BatchGet
  public BatchResult<Long, Greeting> batchGetBatchResult(Set<Long> ids)
  {
    final Map<Long, Greeting> greetingMap = new HashMap<>();
    greetingMap.put(0l, DB.get(0l));

    if (ids.contains(1l))
    {
      //Return null BatchResult
      return null;
    }
    else if (ids.contains(2l))
    {
      //Return BatchResult with null maps
      return new BatchResult<>(null, null, null);
    }
    else if (ids.contains(3l))
    {
      //Return a BatchResult with a null key in the status map.
      final Map<Long, HttpStatus> statusMap = new HashMap<>();
      statusMap.put(null, null);
      return new BatchResult<>(greetingMap, statusMap, null);
    }
    else if (ids.contains(4l))
    {
      //Return a BatchResult that has a map with a null key.
      greetingMap.put(null, null);
      return new BatchResult<>(greetingMap, null, null);
    }
    else
    {
     /*
      * Return a BatchResult with java.util.concurrent.ConcurrentHashMaps.
      * This test is in place because certain map implementations, such as ConcurrentHashMap, can throw an NPE when
      * calling contains(null). We want to verify that checking for the existence of nulls in maps returned by
      * Rest.li resource methods do not cause such NPEs.
      * This is one of the few cases in this file where an error will not be generated by Rest.li.
      */
      final Map<Long, Greeting> concurrentGreetingMap = new ConcurrentHashMap<>(greetingMap);
      return new BatchResult<>(concurrentGreetingMap,
          new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }
  }

  @RestMethod.Update
  public UpdateResponse update(Long key, Greeting entity)
  {
    if (key == 1l)
    {
      //Return null UpdateResponse
      return null;
    }
    else
    {
      //Return an UpdateResponse with a null HttpStatus
      return new UpdateResponse(null);
    }
  }

  @RestMethod.BatchCreate
  public BatchCreateResult<Long, Greeting> batchCreate(BatchCreateRequest<Long, Greeting> entities)
  {
    List<CreateResponse> responses = new ArrayList<>(1);
    if (entities.getInput().size() == 0)
    {
      //Return null
      return null;
    }
    else if (entities.getInput().size() == 1)
    {
      //Return a new BatchCreateResult with a null list
      return new BatchCreateResult<>(null);
    }
    else
    {
      //Return a new BatchCreateResult with a response list that has a null inside of it
      responses.add(new CreateResponse(1l));
      responses.add(null);
      return new BatchCreateResult<>(responses);
    }
  }

  @RestMethod.BatchUpdate
  public BatchUpdateResult<Long, Greeting> batchUpdate(BatchUpdateRequest<Long, Greeting> entities)
  {
    final Map<Long, UpdateResponse> responseMap = new HashMap<>();
    responseMap.put(3l, new UpdateResponse(HttpStatus.S_201_CREATED));

    final Map<Long, RestLiServiceException> errorsMap = new HashMap<>();
    errorsMap.put(8l, new RestLiServiceException(HttpStatus.S_202_ACCEPTED));

    if (entities.getData().containsKey(1l))
    {
      //Return a null BatchUpdateResult
      return null;
    }
    else if (entities.getData().containsKey(2l))
    {
      //Return a BatchUpdateResult with a null results Map
      return new BatchUpdateResult<>(null);
    }
    else if (entities.getData().containsKey(3l))
    {
      //Return a BatchUpdateResult with a null errors Map
      return new BatchUpdateResult<>(responseMap, null);
    }
    else if (entities.getData().containsKey(4l))
    {
      //Return a BatchUpdateResult with a errors Map that has a null key in it
      errorsMap.put(null, new RestLiServiceException(HttpStatus.S_202_ACCEPTED));
      return new BatchUpdateResult<>(responseMap, errorsMap);
    }
    else if (entities.getData().containsKey(5l))
    {
      //Return a BatchUpdateResult with a errors Map that has a null value in it
      errorsMap.put(9l, null);
      return new BatchUpdateResult<>(responseMap, errorsMap);
    }
    else if (entities.getData().containsKey(6l))
    {
      //Return a BatchUpdateResult with a map that has a null key in it
      responseMap.put(null, new UpdateResponse(HttpStatus.S_201_CREATED));
      return new BatchUpdateResult<>(responseMap);
    }
    else
    {

      /*
       * Return a BatchUpdateResult with java.util.concurrent.ConcurrentHashMap(s).
       * This test is in place because certain map implementations, such as ConcurrentHashMap, can throw an NPE when
       * calling contains(null). We want to verify that checking for the existence of nulls in maps returned by
       * Rest.li resource methods do not cause such NPEs.
       * This is one of the few cases in this file where an error will not be generated by Rest.li.
       */
      final Map<Long, UpdateResponse> concurrentResponseMap = new ConcurrentHashMap<>(responseMap);
      return new BatchUpdateResult<>(concurrentResponseMap, new ConcurrentHashMap<>());
    }
  }

  @RestMethod.BatchPartialUpdate
  public BatchUpdateResult<Long, Greeting> batchUpdate(BatchPatchRequest<Long, Greeting> entityUpdates)
  {
    final Map<Long, UpdateResponse> responseMap = new HashMap<>();
    responseMap.put(3l, new UpdateResponse(HttpStatus.S_201_CREATED));

    if (entityUpdates.getData().containsKey(1l))
    {
      //Return a null BatchUpdateResult
      return null;
    }
    else if (entityUpdates.getData().containsKey(2l))
    {
      //Return a BatchUpdateResult with a null results Map
      return new BatchUpdateResult<>(null);
    }
    else if (entityUpdates.getData().containsKey(3l))
    {
      //Return a BatchUpdateResult with a null errors Map
      return new BatchUpdateResult<>(responseMap, null);
    }
    else
    {
      //Return a BatchUpdateResult with a map that has a null key in it
      responseMap.put(null, new UpdateResponse(HttpStatus.S_201_CREATED));
      return new BatchUpdateResult<>(responseMap);
    }
  }

  @RestMethod.BatchDelete
  public BatchUpdateResult<Long, Greeting> batchDelete(BatchDeleteRequest<Long, Greeting> deleteRequest)
  {
    return null;
  }

  @RestMethod.Delete
  public UpdateResponse delete(Long key)
  {
    return null;
  }

  @Action(name = "returnNullStringArray")
  public StringArray returnNullStringArray()
  {
    return null;
  }

  @Action(name = "returnStringArrayWithNullElement")
  public StringArray returnStringArrayWithNullElement()
  {
    //Return a StringArray with a null element
    return new StringArray("abc", null, "def");
  }

  @Action(name = "returnNullActionResult")
  public ActionResult<Integer> returnNull()
  {
    return null;
  }

  @Action(name = "returnActionResultWithNullValue")
  public ActionResult<Integer> returnActionResultWithNullValue()
  {
    //Return an ActionResult with a null Value
    final Integer nullInteger = null;
    return new ActionResult<>(nullInteger);
  }

  @Action(name = "returnActionResultWithNullStatus")
  public ActionResult<Integer> returnActionResultWithNullStatus()
  {
    //Return an ActionResult with a null HttpStatus
    return new ActionResult<>(3, null);
  }

  @Finder("finderCallbackNullList")
  public void finderCallbackNull(@PagingContextParam final PagingContext a, @QueryParam("tone") final Tone b,
      @CallbackParam final Callback<List<Greeting>> callback)
  {
    final Runnable requestHandler = new Runnable()
    {
      public void run()
      {
        try
        {
          //Depending on the tone, we return a null list or a list with a null element
          callback.onSuccess(searchReturnNullList(a, b));
        }
        catch (final Throwable throwable)
        {
          callback.onError(throwable);
        }
      }
    };
    SCHEDULER.schedule(requestHandler, DELAY, TimeUnit.MILLISECONDS);
  }

  @Finder("finderPromiseNullList")
  public Promise<List<Greeting>> finderPromiseNullList(@PagingContextParam final PagingContext a, @QueryParam("tone") final Tone b)
  {
    final SettablePromise<List<Greeting>> result = Promises.settable();
    final Runnable requestHandler = new Runnable()
    {
      public void run()
      {
        try
        {
          //Depending on the tone, we return a null list or a list with a null element
          result.done(searchReturnNullList(a, b));
        }
        catch (final Throwable throwable)
        {
          result.fail(throwable);
        }
      }
    };
    SCHEDULER.schedule(requestHandler, DELAY, TimeUnit.MILLISECONDS);
    return result;
  }

  @Finder("finderTaskNullList")
  public Task<List<Greeting>> finderTaskNullList(@PagingContextParam final PagingContext a, @QueryParam("tone") final Tone b)
  {
    return new BaseTask<List<Greeting>>()
    {
      protected Promise<List<Greeting>> run(final com.linkedin.parseq.Context context) throws Exception
      {
        //Depending on the tone, we return a null list or a list with a null element
        return Promises.value(searchReturnNullList(a, b));
      }
    };
  }
}
