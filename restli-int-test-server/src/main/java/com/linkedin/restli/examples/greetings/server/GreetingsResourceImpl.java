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


import com.linkedin.data.template.BooleanArray;
import com.linkedin.data.template.StringMap;
import com.linkedin.data.transform.DataProcessingException;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.examples.greetings.api.Empty;
import com.linkedin.restli.examples.greetings.api.EmptyArray;
import com.linkedin.restli.examples.greetings.api.EmptyMap;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.SearchMetadata;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.api.ToneFacet;
import com.linkedin.restli.examples.greetings.api.ToneFacetArray;
import com.linkedin.restli.examples.groups.api.TransferOwnershipRequest;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchResult;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.CollectionResult.PageIncrement;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Optional;
import com.linkedin.restli.server.annotations.PagingContextParam;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.BaseResource;
import com.linkedin.restli.server.resources.KeyValueResource;
import com.linkedin.restli.server.util.PatchApplier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Base class to various interfaces of a richer "Hello world" example, demonstrating a
 * full array of methods, finders and actions. To circumvent inheritance diamonds, this
 * class is intended to be used as a class member. Annotations are kept for reference.
 *
 * @author dellamag
 */
// package private
class GreetingsResourceImpl implements KeyValueResource<Long,Greeting>
{
  private static final String[] GREETINGS =
      { "Good morning!", "Guten Morgen!", "Buenos dias!", "Bon jour!", "Buon Giorno!" };
  private static final Tone[] TONES = { Tone.FRIENDLY, Tone.SINCERE, Tone.INSULTING };

  private static final Tone DEFAULT_TONE = Tone.INSULTING;

  private static final int INITIAL_SIZE = 20;
  private static final String[] INITIAL_MESSAGES = new String[INITIAL_SIZE];
  private static final Tone[] INITIAL_TONES = new Tone[INITIAL_SIZE];
  static {
    // generate some "random" initial data
    for (int i = 0; i < INITIAL_SIZE; i++)
      INITIAL_MESSAGES[i] = GREETINGS[i % GREETINGS.length];
    for (int i = 0; i < INITIAL_SIZE; i++)
      INITIAL_TONES[i] = TONES[i % TONES.length];
  }

  private final AtomicLong _idSeq = new AtomicLong();
  private final Map<Long, Greeting> _db = Collections.synchronizedMap(new LinkedHashMap<Long, Greeting>());
  private final String _resourceName;

  public GreetingsResourceImpl(String resourceName)
  {
    for (int i = 0; i < INITIAL_SIZE; i++)
    {
      Greeting g =
          new Greeting().setId(_idSeq.incrementAndGet())
                        .setMessage(INITIAL_MESSAGES[i])
                        .setTone(INITIAL_TONES[i]);
      _db.put(g.getId(), g);
    }
    _resourceName = resourceName;
  }

  // These CRUD annotations are MANDATORY for the code generator because we want to generate
  // implementations which do not use the templates, e.g. Task
  @RestMethod.Create
  public CreateResponse create(Greeting entity, @QueryParam("isNullId") @Optional("false") boolean isNullId)
  {
    entity.setId(_idSeq.incrementAndGet());
    _db.put(entity.getId(), entity);
    if (isNullId)
    {
      return new CreateResponse(null, HttpStatus.S_201_CREATED);
    }
    return new CreateResponse(entity.getId());
  }

  @RestMethod.BatchGet
  public Map<Long, Greeting> batchGet(Set<Long> ids)
  {
    Map<Long, Greeting> batch = new HashMap<Long, Greeting>();
    Map<Long, RestLiServiceException> errors = new HashMap<Long, RestLiServiceException>();
    for (long id : ids)
    {
      Greeting g = _db.get(id);
      if (g != null)
      {
        batch.put(id, g);
      }
      else
      {
        errors.put(id, new RestLiServiceException(HttpStatus.S_404_NOT_FOUND));
      }
    }

    return new BatchResult<Long, Greeting>(batch, errors);
  }

  @RestMethod.BatchUpdate
  public BatchUpdateResult<Long, Greeting> batchUpdate(BatchUpdateRequest<Long, Greeting> entities)
  {
    Map<Long, UpdateResponse> responseMap = new HashMap<Long, UpdateResponse>();
    for (Map.Entry<Long, Greeting> entry : entities.getData().entrySet())
    {
      responseMap.put(entry.getKey(), update(entry.getKey(), entry.getValue()));
    }
    return new BatchUpdateResult<Long, Greeting>(responseMap);
  }

  @RestMethod.BatchPartialUpdate
  public BatchUpdateResult<Long, Greeting> batchUpdate(BatchPatchRequest<Long, Greeting> entityUpdates)
  {
    Map<Long, UpdateResponse> responseMap = new HashMap<Long, UpdateResponse>();
    for (Map.Entry<Long, PatchRequest<Greeting>> entry : entityUpdates.getData().entrySet())
    {
      responseMap.put(entry.getKey(), update(entry.getKey(), entry.getValue()));
    }
    return new BatchUpdateResult<Long, Greeting>(responseMap);
  }

  @RestMethod.BatchCreate
  public BatchCreateResult<Long, Greeting> batchCreate(BatchCreateRequest<Long, Greeting> entities)
  {
    List<CreateResponse> responses = new ArrayList<CreateResponse>(entities.getInput().size());

    for (Greeting g : entities.getInput())
    {
      responses.add(create(g, false));
    }
    return new BatchCreateResult<Long, Greeting>(responses);
  }

  @RestMethod.BatchDelete
  public BatchUpdateResult<Long, Greeting> batchDelete(BatchDeleteRequest<Long, Greeting> deleteRequest)
  {
    Map<Long, UpdateResponse> responseMap = new HashMap<Long, UpdateResponse>();
    for (Long id : deleteRequest.getKeys())
    {
      responseMap.put(id, delete(id));
    }
    return new BatchUpdateResult<Long, Greeting>(responseMap);
  }

  @RestMethod.Get
  public Greeting get(Long key)
  {
    return _db.get(key);
  }

  @RestMethod.Delete
  public UpdateResponse delete(Long key)
  {
    boolean removed = _db.remove(key) != null;

    return new UpdateResponse(removed ? HttpStatus.S_204_NO_CONTENT : HttpStatus.S_404_NOT_FOUND);
  }

  @RestMethod.PartialUpdate
  public UpdateResponse update(Long key, PatchRequest<Greeting> patch)
  {
    Greeting g = _db.get(key);
    if (g == null)
    {
      return new UpdateResponse(HttpStatus.S_404_NOT_FOUND);
    }

    try
    {
      PatchApplier.applyPatch(g, patch);
    }
    catch (DataProcessingException e)
    {
      return new UpdateResponse(HttpStatus.S_400_BAD_REQUEST);
    }

    _db.put(key, g);

    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }

  @RestMethod.Update
  public UpdateResponse update(Long key, Greeting entity)
  {
    Greeting g = _db.get(key);
    if (g == null)
    {
      return new UpdateResponse(HttpStatus.S_404_NOT_FOUND);
    }

    _db.put(key, entity);

    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }

  @RestMethod.GetAll
  public List<Greeting> getAll(@PagingContextParam PagingContext ctx)
  {
    // Deterministic behaviour of getAll to make it easier to test as part of the integration test suite
    // Just return those greetings that have "GetAll" present in their message
    List<Greeting> greetings = new ArrayList<Greeting>();
    for (Greeting greeting: _db.values())
    {
      if (greeting.getMessage().contains("GetAll"))
      {
        greetings.add(greeting);
      }
    }
    return greetings;
  }

  @Finder("searchWithDefault")
  public List<Greeting> searchWithDefault(@PagingContextParam PagingContext ctx,
                                          @QueryParam("tone") @Optional("FRIENDLY") Tone tone)
  {
    return search(ctx, tone);
  }

  @Finder("search")
  public List<Greeting> search(@PagingContextParam PagingContext ctx, @QueryParam("tone") @Optional Tone tone)
  {
    List<Greeting> greetings = new ArrayList<Greeting>();
    int idx = 0;
    int start = ctx.getStart();
    int stop = start + ctx.getCount();
    for (Greeting g : _db.values())
    {
      if (idx++ >= ctx.getStart())
      {
        if (tone == null || g.getTone().equals(tone))
        {
          greetings.add(g);
        }

        if (idx == stop)
        {
          break;
        }
      }
    }
    return greetings;
  }

  @Finder("searchWithPostFilter")
  public CollectionResult<Greeting, Empty> searchWithPostFilter(@PagingContextParam PagingContext ctx)
  {
    List<Greeting> greetings = new ArrayList<Greeting>();
    int idx = 0;
    int start = ctx.getStart();
    int stop = start + ctx.getCount();
    for (Greeting g : _db.values())
    {
      if (idx++ >= ctx.getStart())
      {
        greetings.add(g);
        if (idx == stop)
        {
          break;
        }
      }
    }

    if(greetings.size() > 0) greetings.remove(0); // for testing, using a post-filter that just removes the first element

    int total = _db.values().size();
    // but we keep the numElements returned as the full count despite the fact that with the filter removed 1
    // this is to keep paging consistent even in the presence of a post filter.
    return new CollectionResult<Greeting, Empty>(greetings, total, null, PageIncrement.FIXED);
  }

  @Finder("searchWithTones")
  public List<Greeting> searchWithTones(@PagingContextParam PagingContext ctx, @QueryParam("tones") @Optional Tone[] tones)
  {
    Set<Tone> toneSet = new HashSet<Tone>(Arrays.asList(tones));
    List<Greeting> greetings = new ArrayList<Greeting>();
    int idx = 0;
    int start = ctx.getStart();
    int stop = start + ctx.getCount();
    for (Greeting g : _db.values())
    {
      if (idx++ >= ctx.getStart())
      {
        if (tones == null || toneSet.contains(g.getTone()))
        {
          greetings.add(g);
        }

        if (idx == stop)
        {
          break;
        }
      }
    }
    return greetings;
  }

  @Finder("searchWithFacets")
  public CollectionResult<Greeting, SearchMetadata> searchWithFacets(@PagingContextParam PagingContext ctx, @QueryParam("tone") @Optional Tone tone)
  {
    List<Greeting> greetings = search(ctx, tone);

    Map<Tone, Integer> toneCounts = new HashMap<Tone, Integer>();
    for (Greeting g : greetings)
    {
      if (!toneCounts.containsKey(g.getTone()))
      {
        toneCounts.put(g.getTone(), 0);
      }
      toneCounts.put(g.getTone(), toneCounts.get(g.getTone()) + 1);
    }

    SearchMetadata metadata = new SearchMetadata();
    metadata.setFacets(new ToneFacetArray());
    for(Map.Entry<Tone, Integer> entry : toneCounts.entrySet())
    {
      ToneFacet f = new ToneFacet();
      f.setTone(entry.getKey());
      f.setCount(entry.getValue());
      metadata.getFacets().add(f);
    }

    return new CollectionResult<Greeting, SearchMetadata>(greetings, null, metadata);
  }

  // test if @{link EmptyArray} is generated from ArrayOfEmptys.pdsc
  // test if @{link EmptyMap} is generated from MapOfEmptys.pdsc
  @Finder("empty")
  public List<Greeting> emptyFinder(@QueryParam("array") EmptyArray array,
                                    @QueryParam("map") EmptyMap map)
  {
    return Collections.emptyList();
  }

  private Greeting createGreeting()
  {
    return
      new Greeting().setId(_idSeq.incrementAndGet())
                    .setMessage("This is a newly created greeting")
                    .setTone(DEFAULT_TONE);
  }

  // Some action examples
  // These are not necessarily related to the greetings domain, but are here to give an idea as to how action work


  /**
   * Pretend to delete all greetings
   * @return the number of greetings purged, always 100
   */
  @Action(name = "purge")
  public int purge()
  {
    return 100;
  }

  // complex types
  @Action(name = "anotherAction")
  public void anotherAction(@ActionParam("bitfield") BooleanArray bitfield,
                            @ActionParam("request") TransferOwnershipRequest transferReq,
                            @ActionParam("someString") String someString,
                            @ActionParam("stringMap") StringMap stringMap)
  {
  }

  // optional/default
  @Action(name = "someAction", resourceLevel = ResourceLevel.ENTITY)
  public Greeting someAction(@ActionParam("a") @Optional("1") int a,
                             @ActionParam("b") @Optional("default") String b,
                             @ActionParam("c") @Optional TransferOwnershipRequest c,
                             @ActionParam("d") TransferOwnershipRequest d,
                             @ActionParam("e") Integer e)
  {
    return createGreeting();
  }

  /**
   * a more concrete example of custom action<br>
   * resource level determines the granularity of the action<br>
   * mismatching the resource level in the request throws exception and will respond HTTP
   * 400
   *
   * @param resource
   *          Instance of the resource class. This is not part of the action method and is
   *          needed because this implementation is not an actual resource.
   */
  @Action(name = "updateTone", resourceLevel = ResourceLevel.ENTITY)
  // The base resource parameter gets special handling in the generator. It is set to the actual
  // resource class instance, and is not part of the generated REST method.
  public Greeting updateTone(BaseResource resource,
                             @ActionParam("newTone") @Optional Tone newTone,
                             @ActionParam("delOld") @Optional("false") Boolean delOld)
  {
    // the way to get entity key in action
    Long key = resource.getContext().getPathKeys().get(_resourceName + "Id");
    Greeting g = _db.get(key);
    if (g == null)
    {
      // HTTP 404
      return g;
    }

    // delete existing Greeting and assign new key
    if (delOld)
    {
      _db.remove(key);
      key = _idSeq.incrementAndGet();
      g.setId(key);
    }

    Tone t;
    // newTone is an optional parameter
    // omitting it in request results a null value
    if (newTone == null)
    {
      t = DEFAULT_TONE;
    }
    else
    {
      t = newTone;
    }
    g.setTone(t);
    _db.put(key, g);

    return g;
  }

  @Action(name = "exceptionTest")
  public void exceptionTest()
  {
    throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, "Test Exception");
  }
}
