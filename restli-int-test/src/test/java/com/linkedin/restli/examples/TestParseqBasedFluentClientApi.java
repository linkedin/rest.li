/*
   Copyright (c) 2021 LinkedIn Corp.

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

package com.linkedin.restli.examples;

import com.linkedin.data.template.BooleanArray;
import com.linkedin.data.template.StringMap;
import com.linkedin.parseq.ParSeqUnitTestHelper;
import com.linkedin.restli.client.ParSeqRestliClient;
import com.linkedin.restli.client.ParSeqRestliClientBuilder;
import com.linkedin.restli.client.ParSeqRestliClientConfigBuilder;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.client.util.PatchGenerator;
import com.linkedin.restli.common.BatchCollectionResponse;
import com.linkedin.restli.common.BatchFinderCriteriaResult;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.CreateIdEntityStatus;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.IdEntityResponse;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.UpdateEntityStatus;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.examples.custom.types.CustomLong;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.GreetingCriteria;
import com.linkedin.restli.examples.greetings.api.GreetingCriteriaArray;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.MessageCriteria;
import com.linkedin.restli.examples.greetings.api.MessageCriteriaArray;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.api.TwoPartKey;
import com.linkedin.restli.examples.greetings.client.Actions;
import com.linkedin.restli.examples.greetings.client.ActionsFluentClient;
import com.linkedin.restli.examples.greetings.client.Associations;
import com.linkedin.restli.examples.greetings.client.AssociationsAssociationsFluentClient;
import com.linkedin.restli.examples.greetings.client.AssociationsAssociationsSubFluentClient;
import com.linkedin.restli.examples.greetings.client.AssociationsFluentClient;
import com.linkedin.restli.examples.greetings.client.AssociationsSubFluentClient;
import com.linkedin.restli.examples.greetings.client.Batchfinders;
import com.linkedin.restli.examples.greetings.client.ComplexKeys;
import com.linkedin.restli.examples.greetings.client.BatchGreetingFluentClient;
import com.linkedin.restli.examples.greetings.client.BatchfindersFluentClient;
import com.linkedin.restli.examples.greetings.client.ComplexKeysFluentClient;
import com.linkedin.restli.examples.greetings.client.ComplexKeysSubFluentClient;
import com.linkedin.restli.examples.greetings.client.CreateGreeting;
import com.linkedin.restli.examples.greetings.client.CreateGreetingFluentClient;
import com.linkedin.restli.examples.greetings.client.CustomTypes2;
import com.linkedin.restli.examples.greetings.client.CustomTypes2FluentClient;
import com.linkedin.restli.examples.greetings.client.GreetingFluentClient;
import com.linkedin.restli.examples.greetings.client.Greetings;
import com.linkedin.restli.examples.greetings.client.GreetingsFluentClient;
import com.linkedin.restli.examples.greetings.client.PartialUpdateGreeting;
import com.linkedin.restli.examples.greetings.client.PartialUpdateGreetingFluentClient;
import com.linkedin.restli.examples.greetings.client.SubgreetingsFluentClient;
import com.linkedin.restli.examples.groups.api.TransferOwnershipRequest;
import com.linkedin.restli.examples.groups.client.Groups;
import com.linkedin.restli.examples.groups.client.GroupsFluentClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.collections.Sets;


public class TestParseqBasedFluentClientApi extends RestLiIntegrationTest
{
  public static final String MESSAGE = "Create a new greeting";
  ParSeqUnitTestHelper _parSeqUnitTestHelper;
  ParSeqRestliClient _parSeqRestliClient;

  @BeforeClass
  void setUp() throws Exception
  {
    super.init();
    _parSeqUnitTestHelper = new ParSeqUnitTestHelper();
    _parSeqUnitTestHelper.setUp();
    _parSeqRestliClient = new ParSeqRestliClientBuilder()
        .setClient(getClient())
        .setConfig(new ParSeqRestliClientConfigBuilder().build())
        .build();
  }

  @AfterClass
  void tearDown() throws Exception
  {
    if (_parSeqUnitTestHelper != null)
    {
      _parSeqUnitTestHelper.tearDown();
    }
    else
    {
      throw new RuntimeException("Tried to shut down Engine but it either has not even been created or has "
          + "already been shut down");
    }
    super.shutdown();
  }

  @Test
  public void testGetRequest() throws Exception
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CompletionStage<Greeting> result = greetings.get(1L);
    CompletableFuture<Greeting> future = result.toCompletableFuture();
    Greeting greeting = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertTrue(greeting.hasId());
    Assert.assertEquals((Long) 1L, greeting.getId());
  }

  @Test
  public void testGetRequestFailure() throws Exception
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CompletionStage<Greeting> result = greetings.get(-1L);
    CompletableFuture<Greeting> future = result.toCompletableFuture();
    try
    {
      future.get(5000, TimeUnit.MILLISECONDS);
      Assert.fail("Expected failure");
    } catch (ExecutionException e)
    {
      Assert.assertEquals(((RestLiResponseException) e.getCause()).getStatus(), 404);
    }
  }


  @Test
  public void testBatchGetRequest() throws Exception
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    Set<Long> ids = Sets.newHashSet(Arrays.asList(1L, 2L, 3L));
    CompletionStage<Map<Long, EntityResponse<Greeting>>> result = greetings.batchGet(ids);
    CompletableFuture<Map<Long, EntityResponse<Greeting>>> future = result.toCompletableFuture();
    Map<Long, EntityResponse<Greeting>> resultMap = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(resultMap.size(), ids.size());
    for (Long id : ids)
    {
      EntityResponse<Greeting> g = resultMap.get(id);
      Assert.assertNotNull(g);
      Assert.assertTrue(g.hasEntry());
      Assert.assertEquals(id, g.getEntity().getId());
    }
  }

  @Test
  public void testBatchGetRequestWithPartialErrors() throws Exception
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    Set<Long> ids = Sets.newHashSet(Arrays.asList(-1L, -2L, 1L, 2L, 3L));
    CompletionStage<Map<Long, EntityResponse<Greeting>>> result = greetings.batchGet(ids);
    CompletableFuture<Map<Long, EntityResponse<Greeting>>> future = result.toCompletableFuture();
    Map<Long, EntityResponse<Greeting>> resultMap = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(resultMap.size(), ids.size());
    for (Long id : ids)
    {
      EntityResponse<Greeting> g = resultMap.get(id);
      Assert.assertNotNull(g);
      if (id > 0)
      {
        Assert.assertTrue(g.hasEntry());
        Assert.assertEquals(id, g.getEntity().getId());
      }
      else
      {
        Assert.assertTrue(g.hasError());
      }
    }
  }

  @Test
  public void testCreate() throws Exception
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    CompletionStage<Long> result = greetings.create(getGreeting());
    CompletableFuture<Long> future = result.toCompletableFuture();
    Assert.assertNotNull(future.get(5000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testCreateNullId() throws Exception
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CompletionStage<Long> result = greetings.create(getGreeting(), params -> params.setIsNullId(true));
    CompletableFuture<Long> future = result.toCompletableFuture();
    Assert.assertNull(future.get(5000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testCreateReturnEntity() throws Exception
  {
    CreateGreeting greetings = new CreateGreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    String msg = Double.toString(Math.random());
    CompletionStage<IdEntityResponse<Long, Greeting>> result = greetings.createAndGet(getGreeting(msg));
    CompletableFuture<IdEntityResponse<Long, Greeting>> future = result.toCompletableFuture();
    Assert.assertNotNull(future.get(5000, TimeUnit.MILLISECONDS));
    Assert.assertEquals(msg, future.get().getEntity().getMessage());
  }

  @Test
  public void testCreateReturnEntityDisabled() throws Exception
  {
    CreateGreeting greetings = new CreateGreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CompletionStage<Long> result = greetings.create(getGreeting());
    CompletableFuture<Long> future = result.toCompletableFuture();
    Assert.assertNotNull(future.get(5000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testBatchCreate() throws Exception
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CompletionStage<List<CreateIdStatus<Long>>> result = greetings.batchCreate(
        Arrays.asList(getGreeting(), getGreeting()));
    CompletableFuture<List<CreateIdStatus<Long>>> future = result.toCompletableFuture();
    List<CreateIdStatus<Long>> ids = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(ids.size(), 2);
  }

  @Test
  public void testBatchCreateReturnEntity() throws Exception
  {
    CreateGreeting greetings = new CreateGreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    String msg1 = Double.toString(Math.random());
    String msg2 = Double.toString(Math.random());
    CompletionStage<List<CreateIdEntityStatus<Long, Greeting>>>
        result = greetings.batchCreateAndGet(Arrays.asList(getGreeting(msg1), getGreeting(msg2)));
    CompletableFuture<List<CreateIdEntityStatus<Long, Greeting>>> future = result.toCompletableFuture();
    List<CreateIdEntityStatus<Long, Greeting>> entities = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(entities.size(), 2);
    Assert.assertNotNull(entities.get(0).getEntity());
    Assert.assertEquals(msg1, entities.get(0).getEntity().getMessage());
    Assert.assertNotNull(entities.get(1).getEntity());
    Assert.assertEquals(msg2, entities.get(1).getEntity().getMessage());
  }

  @Test
  public void testBatchCreateReturnEntityDisabled() throws Exception
  {
    CreateGreeting greetings = new CreateGreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CompletionStage<List<CreateIdStatus<Long>>>
        result = greetings.batchCreate(Arrays.asList(getGreeting(), getGreeting()));
    CompletableFuture<List<CreateIdStatus<Long>>> future = result.toCompletableFuture();
    List<CreateIdStatus<Long>> ids = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(ids.size(), 2);
  }

  @Test
  public void testCreateAndThenBatchDelete() throws Exception
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    // Create entities first so we don't delete those used by other tests.
    CompletionStage<List<CreateIdStatus<Long>>>
        createResult = greetings.batchCreate(Arrays.asList(getGreeting(), getGreeting()));

    CompletionStage<Map<Long, UpdateStatus>>
        result = createResult.thenCompose(ids -> greetings.batchDelete(
            Sets.newHashSet(ids.stream().map(CreateIdStatus::getKey).collect(Collectors.toList()))));
    CompletableFuture<Map<Long, UpdateStatus>> future = result.toCompletableFuture();
    Map<Long, UpdateStatus> ids = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(ids.size(), 2);
    for (UpdateStatus status : ids.values())
    {
      Assert.assertEquals(status.getStatus().intValue(), 204);
    }
  }

  @Test
  public void testCreateAndThenBatchDeleteWithFailures() throws Exception
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    // Create entities first so we don't delete those used by other tests.
    CompletionStage<List<CreateIdStatus<Long>>>
        createResult = greetings.batchCreate(Arrays.asList(getGreeting(), getGreeting()));

    CompletionStage<Map<Long, UpdateStatus>>
        result = createResult.thenCompose(ids ->
    {
      Set<Long> deleteIds = Sets.newHashSet(
          ids.stream().map(CreateIdStatus::getKey).collect(Collectors.toList()));
      deleteIds.add(-1L);
      return greetings.batchDelete(deleteIds);
    });
    CompletableFuture<Map<Long, UpdateStatus>> future = result.toCompletableFuture();
    Map<Long, UpdateStatus> ids = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(ids.size(), 3);
    Assert.assertEquals(ids.remove(-1L).getStatus().intValue(), 404);
    for (UpdateStatus status : ids.values())
    {
      Assert.assertEquals(status.getStatus().intValue(), 204);
    }
  }

  @Test
  public void testCreateAndThenDelete() throws Exception
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    // Create entities first so we don't delete those used by other tests.
    CompletionStage<Long> createResult = greetings.create(getGreeting());

    CompletionStage<Void> result = createResult.thenCompose(greetings::delete);
    CompletableFuture<Void> future = result.toCompletableFuture();
    future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertFalse(future.isCompletedExceptionally());
  }

  @Test
  public void testDeleteFailure() throws Exception
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    CompletionStage<Void> result = greetings.delete(-1L);
    CompletableFuture<Void> future = result.toCompletableFuture();
    try
    {
      future.get(5000, TimeUnit.MILLISECONDS);
      Assert.fail("Expected failure");
    } catch (ExecutionException e)
    {
      Assert.assertEquals(((RestLiResponseException) e.getCause()).getStatus(), 404);
    }
  }

  @Test
  public void testPartialUpdateAfterCreateAndGet() throws Exception
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    // Create a new greeting to use for partial update test.
    CompletionStage<Long> createResult = greetings.create(getGreeting());
    CompletionStage<Void> updateResult = createResult.thenCompose(greetings::get).thenCompose(greeting ->
    {
      try
      {
        Greeting update = greeting.copy();
        greeting.setMessage("Partial update message");
        return greetings.partialUpdate(1L, PatchGenerator.diff(greeting, update));
      } catch (CloneNotSupportedException e)
      {
        throw new RuntimeException(e);
      }
    });
    CompletableFuture<Void> future = updateResult.toCompletableFuture();
    future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertFalse(future.isCompletedExceptionally());
  }

  @Test
  public void testPartialUpdateAndGet() throws Exception
  {
    PartialUpdateGreeting greetings = new PartialUpdateGreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    Greeting original = getGreeting();
    String message = "Edited message: fluent api test partialUpdateAndGet";
    Greeting update = getGreeting(message);
    CompletionStage<Greeting> result = greetings.partialUpdateAndGet(21L, PatchGenerator.diff(original, update));
    CompletableFuture<Greeting> future = result.toCompletableFuture();
    Assert.assertEquals(future.get(5000, TimeUnit.MILLISECONDS).getMessage(), message);
  }

  @Test
  public void testPartialUpdateError() throws Exception
  {
    PartialUpdateGreeting greetings = new PartialUpdateGreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    Greeting original = getGreeting();
    String message = "Edited message: fluent api test partialUpdateAndGet";
    Greeting update = getGreeting(message);
    CompletionStage<Greeting> result = greetings.partialUpdateAndGet(-1L, PatchGenerator.diff(original, update));
    CompletableFuture<Greeting> future = result.toCompletableFuture();
    try
    {
      future.get(5000, TimeUnit.MILLISECONDS);
      Assert.fail("Expected failure");
    } catch (ExecutionException e)
    {
      Assert.assertEquals(((RestLiResponseException) e.getCause()).getStatus(), 404);
    }
  }

  @Test
  public void testBatchPartialUpdate() throws Exception
  {
    PartialUpdateGreeting greetings = new PartialUpdateGreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    Map<Long, PatchRequest<Greeting>> inputs = new HashMap<>();
    Greeting original = getGreeting();
    String message = "Edited message: fluent api test partialUpdateAndGet";
    Greeting update = getGreeting(message);
    inputs.put(21L, PatchGenerator.diff(original, update));
    inputs.put(22L, PatchGenerator.diff(original, update));
    CompletionStage<Map<Long, UpdateStatus>> result = greetings.batchPartialUpdate(inputs);
    CompletableFuture<Map<Long, UpdateStatus>> future = result.toCompletableFuture();
    Assert.assertNotNull(future.get(5000, TimeUnit.MILLISECONDS));
    Assert.assertEquals(future.get().get(21L).getStatus().intValue(), 200);
    Assert.assertEquals(future.get().get(22L).getStatus().intValue(), 200);
  }

  @Test
  public void testBatchPartialUpdateWithErrors() throws Exception
  {
    PartialUpdateGreeting greetings = new PartialUpdateGreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    Map<Long, PatchRequest<Greeting>> inputs = new HashMap<>();
    Greeting original = getGreeting();
    String message = "Edited message: fluent api test partialUpdateAndGet";
    Greeting update = getGreeting(message);
    inputs.put(21L, PatchGenerator.diff(original, update));
    inputs.put(-2L, PatchGenerator.diff(original, update));
    CompletionStage<Map<Long, UpdateStatus>> result = greetings.batchPartialUpdate(inputs);
    CompletableFuture<Map<Long, UpdateStatus>> future = result.toCompletableFuture();
    Assert.assertNotNull(future.get(5000, TimeUnit.MILLISECONDS));
    Assert.assertEquals(future.get().get(21L).getStatus().intValue(), 200);
    Assert.assertEquals(future.get().get(-2L).getStatus().intValue(), 404);
  }

  @Test
  public void testBatchPartialUpdateAndGet() throws Exception
  {
    PartialUpdateGreeting greetings = new PartialUpdateGreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    Map<Long, PatchRequest<Greeting>> inputs = new HashMap<>();
    Greeting original = getGreeting();
    String message = "Edited message: fluent api test partialUpdateAndGet";
    Greeting update = getGreeting(message);
    inputs.put(21L, PatchGenerator.diff(original, update));
    inputs.put(22L, PatchGenerator.diff(original, update));
    CompletionStage<Map<Long, UpdateEntityStatus<Greeting>>> result = greetings.batchPartialUpdateAndGet(inputs);
    CompletableFuture<Map<Long, UpdateEntityStatus<Greeting>>> future = result.toCompletableFuture();
    Assert.assertNotNull(future.get(5000, TimeUnit.MILLISECONDS));
    Assert.assertEquals(future.get().get(21L).getEntity().getId().longValue(), 21L);
    Assert.assertEquals(future.get().get(21L).getEntity().getMessage(), message);
    Assert.assertEquals(future.get().get(22L).getEntity().getId().longValue(), 22L);
    Assert.assertEquals(future.get().get(22L).getEntity().getMessage(), message);
  }

  @Test
  public void testBatchPartialUpdateAndGetWithErrors() throws Exception
  {
    PartialUpdateGreeting greetings = new PartialUpdateGreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    Map<Long, PatchRequest<Greeting>> inputs = new HashMap<>();
    Greeting original = getGreeting();
    String message = "Edited message: fluent api test partialUpdateAndGet";
    Greeting update = getGreeting(message);
    inputs.put(21L, PatchGenerator.diff(original, update));
    inputs.put(-2L, PatchGenerator.diff(original, update));
    CompletionStage<Map<Long, UpdateEntityStatus<Greeting>>> result = greetings.batchPartialUpdateAndGet(inputs);
    CompletableFuture<Map<Long, UpdateEntityStatus<Greeting>>> future = result.toCompletableFuture();
    Assert.assertNotNull(future.get(5000, TimeUnit.MILLISECONDS));
    Assert.assertEquals(future.get().get(21L).getStatus().intValue(), 200);
    Assert.assertEquals(future.get().get(21L).getEntity().getId().longValue(), 21L);
    Assert.assertEquals(future.get().get(21L).getEntity().getMessage(), message);
    Assert.assertEquals(future.get().get(-2L).getStatus().intValue(), 404);
    Assert.assertFalse(future.get().get(-2L).hasEntity());
  }

  @Test
  public void testBatchUpdate() throws Exception
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    CompletionStage<List<CreateIdStatus<Long>>>
        createResult = greetings.batchCreate(Arrays.asList(getGreeting(), getGreeting()));

    final Map<Long, Greeting> inputs = new HashMap<>();
    CompletionStage<Map<Long, UpdateStatus>> result = createResult.thenCompose(idStatuses ->
    {
      for (CreateIdStatus<Long> idStatus : idStatuses)
      {
        inputs.put(idStatus.getKey(), getGreeting("Batch update test"));
      }
      return greetings.batchUpdate(inputs);
    });
    CompletableFuture<Map<Long, UpdateStatus>> future = result.toCompletableFuture();
    Map<Long, UpdateStatus> ids = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(ids.size(), 2);
    for (Long id : inputs.keySet())
    {
      Assert.assertEquals(ids.get(id).getStatus().intValue(), 204);
    }
  }

  @Test
  public void testBatchUpdateWithErrors() throws Exception
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    final Map<Long, Greeting> inputs = new HashMap<>();
    inputs.put(-6L, getGreeting());
    CompletionStage<Long> createResult = greetings.create(getGreeting());

    CompletionStage<Map<Long, UpdateStatus>> result = createResult.thenCompose(id ->
    {
      inputs.put(id, getGreeting("Batch update test"));
      return greetings.batchUpdate(inputs);
    });
    CompletableFuture<Map<Long, UpdateStatus>> future = result.toCompletableFuture();
    Map<Long, UpdateStatus> ids = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(ids.size(), 2);
    Assert.assertEquals(ids.get(createResult.toCompletableFuture().get()).getStatus().intValue(), 204);
    Assert.assertEquals(ids.get(-6L).getStatus().intValue(), 404);
  }

  @Test
  public void testUpdate() throws Exception
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    CompletionStage<Long> createResult = greetings.create(getGreeting());
    final String message = "Update test";
    CompletionStage<Void> updateResult = createResult.thenCompose(id -> greetings.update(id, getGreeting(message)));
    CompletionStage<Greeting> getResult = createResult.thenCombine(updateResult, (id, v) -> id).thenCompose(greetings::get);

    CompletableFuture<Greeting> future = getResult.toCompletableFuture();
    future.get(5000, TimeUnit.MILLISECONDS);
    // Get the greeting to test updated value
    Assert.assertEquals(future.get().getMessage(), message);
  }

  @Test
  public void testUpdateFailure() throws Exception
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    CompletionStage<Void> result = greetings.update(-7L, getGreeting());
    CompletableFuture<Void> future = result.toCompletableFuture();
    try
    {
      future.get(5000, TimeUnit.MILLISECONDS);
      Assert.fail("Expected failure");
    } catch (ExecutionException e)
    {
      Assert.assertEquals(((RestLiResponseException) e.getCause()).getStatus(), 404);
    }
  }

  @Test
  public void testGetAll() throws Exception
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    // Create some greetings with "GetAll" in message so they will be returned by getAll test method..
    CompletionStage<List<CreateIdStatus<Long>>> createResult = greetings.batchCreate(
        Arrays.asList(getGreeting("GetAll"), getGreeting("GetAll")));

    CompletionStage<CollectionResponse<Greeting>> result = createResult.thenCompose(ids -> greetings.getAll());
    CompletableFuture<CollectionResponse<Greeting>> future = result.toCompletableFuture();
    List<Greeting> greetingList = future.get(5000, TimeUnit.MILLISECONDS).getElements();
    Assert.assertTrue(greetingList.size() >= 2);
    for (Greeting greeting :greetingList)
    {
      Assert.assertTrue(greeting.getMessage().contains("GetAll"));
    }
  }

  @Test
  public void testFinder() throws Exception
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CompletionStage<CollectionResponse<Greeting>> result = greetings.findBySearch(params -> params.setTone(Tone.FRIENDLY));
    CompletableFuture<CollectionResponse<Greeting>> future = result.toCompletableFuture();
    List<Greeting> greetingList = future.get(5000, TimeUnit.MILLISECONDS).getElements();
    Assert.assertTrue(greetingList.size() > 0);
    for (Greeting greeting :greetingList)
    {
      Assert.assertEquals(greeting.getTone(), Tone.FRIENDLY);
    }
  }

  @Test
  public void testBatchFinder() throws Exception
  {
    Batchfinders batchfinders = new BatchfindersFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    GreetingCriteria c1 = new GreetingCriteria().setId(1L).setTone(Tone.SINCERE);
    GreetingCriteria c2 = new GreetingCriteria().setId(2L).setTone(Tone.FRIENDLY);
    CompletionStage<BatchCollectionResponse<Greeting>> result = batchfinders.findBySearchGreetings(
        Arrays.asList(c1, c2), "hello world");
    CompletableFuture<BatchCollectionResponse<Greeting>> future = result.toCompletableFuture();
    List<BatchFinderCriteriaResult<Greeting>> batchResult = future.get(5000, TimeUnit.MILLISECONDS).getResults();

    List<Greeting> greetings1 = batchResult.get(0).getElements();
    Assert.assertTrue(greetings1.get(0).hasTone());
    Assert.assertEquals(greetings1.get(0).getTone(), Tone.SINCERE);

    List<Greeting> greetings2 = batchResult.get(1).getElements();
    Assert.assertTrue(greetings2.get(0).hasId());
    Assert.assertEquals(greetings2.get(0).getTone(), Tone.FRIENDLY);
  }

  // ----- Test with Simple Resources ------
  @Test
  public void testSimpleResourceUpdate() throws Exception
  {
    final String message = "Update test";
    com.linkedin.restli.examples.greetings.client.Greeting greeting = new GreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    greeting.update(getGreeting(message)).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Greeting greetingEntity = greeting.get().toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(greetingEntity.getMessage(), message);
  }

  @Test
  public void testSimpleResourceDeleteMethodAndGetMethod() throws Exception
  {
    com.linkedin.restli.examples.greetings.client.Greeting greeting = new GreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    greeting.delete().toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    try
    {
      Greeting greetingEntity = greeting.get().toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
      Assert.fail("should fail since entity delete");
    }
    catch (Exception e)
    {

    }
    final String message = "Test Get";
    greeting.update(getGreeting(message)).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Greeting greetingEntity = greeting.get().toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Assert.assertTrue(greetingEntity.hasId());
    Assert.assertEquals(greetingEntity.getMessage(), message);

  }

  // ----- Test with Assocations Resources ------

  private CompoundKey getAssociateResourceUrlKey(Associations client)
  {
    return client.generateAssociationsCompoundKey(StringTestKeys.URL, StringTestKeys.URL2);
  }

  private CompoundKey getAssociateResourceSimpleKey(Associations client)
  {
    return client.generateAssociationsCompoundKey(StringTestKeys.SIMPLEKEY, StringTestKeys.SIMPLEKEY2);
  }

  private Map<CompoundKey, Message> getAssociateResourceMockDB(Associations client)
  {
    HashMap<CompoundKey, Message> mapDB = new HashMap<CompoundKey, Message>();
    CompoundKey urlKey = getAssociateResourceUrlKey(client);
    CompoundKey simpleKey = getAssociateResourceSimpleKey(client);
    mapDB.put(urlKey, new Message().setId(urlKey.getPartAsString("src") + " " + urlKey.getPartAsString("dest"))
        .setMessage("I need some %20")
        .setTone(Tone.SINCERE));
    mapDB.put(getAssociateResourceSimpleKey(client),
        new Message().setId(simpleKey.getPartAsString("src") + " " + simpleKey.getPartAsString("dest"))
        .setMessage("src1-dest1")
        .setTone(Tone.SINCERE));
    return mapDB;
  }

  @Test
  public void testAssociateResourceGet() throws Exception
  {
    Associations associations =
        new AssociationsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    Message response = associations.get(getAssociateResourceUrlKey(associations))
        .toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Assert.assertTrue(response.hasId());
  }

  @Test
  public void testAssociateResourceBatchGet() throws Exception
  {
    Associations associations =
        new AssociationsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    Map<CompoundKey, EntityResponse<Message>> entityResponse =
        associations.batchGet(getAssociateResourceMockDB(associations).keySet())
            .toCompletableFuture()
            .get(5000, TimeUnit.MILLISECONDS);
    for (CompoundKey id : getAssociateResourceMockDB(associations).keySet())
    {
      Assert.assertTrue(entityResponse.containsKey(id));
      EntityResponse<Message> single = entityResponse.get(id);
      Assert.assertEquals(single.getEntity(), getAssociateResourceMockDB(associations).get(id));
    }
  }

  @Test
  public void testAssociateResourceBatchUpdate() throws Exception
  {
    Associations associations =
        new AssociationsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    Map<CompoundKey, UpdateStatus> ids =
        associations.batchUpdate(getAssociateResourceMockDB(associations))
            .toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);

    Assert.assertEquals(ids.size(), 2);
    for (CompoundKey id : ids.keySet())
    {
      Assert.assertEquals(ids.get(id).getStatus().intValue(), 204);
    }
  }

  @Test
  public void testAssociateResourceBatchPartialUpdate() throws Exception
  {
    Associations associations =
        new AssociationsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    Map<CompoundKey, PatchRequest<Message>> patches = new HashMap<CompoundKey, PatchRequest<Message>>();
    patches.put(getAssociateResourceUrlKey(associations), new PatchRequest<Message>());
    patches.put(getAssociateResourceSimpleKey(associations), new PatchRequest<Message>());

    Map<CompoundKey, UpdateStatus> ids =
        associations.batchPartialUpdate(patches).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);

    for (CompoundKey id : ids.keySet())
    {
      Assert.assertEquals(ids.get(id).getStatus().intValue(), 204);
    }
  }

  @Test
  public void testAssociationFinderUsingAssocKey() throws Exception
  {
    AssociationsFluentClient associations =
        new AssociationsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CollectionResponse<Message> messages =
        associations.findByAssocKeyFinder(AssociationResourceHelpers.URL_COMPOUND_KEY.getPartAsString("src")).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Assert.assertTrue(messages.getElements().size() > 0);
    for (Message message : messages.getElements())
    {
      Assert.assertEquals(message.getId(), AssociationResourceHelpers.URL_MESSAGE.getId());
    }
  }

  @Test
  public void testAssociationBatchFinderUsingAssocKey() throws Exception
  {
    AssociationsFluentClient associations =
        new AssociationsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    MessageCriteriaArray criteriaArray = new MessageCriteriaArray();
    criteriaArray.add(new MessageCriteria().setTone(Tone.FRIENDLY));
    criteriaArray.add(new MessageCriteria().setTone(Tone.INSULTING));
    BatchCollectionResponse<Message> messages =
        associations.findBySearchMessages(AssociationResourceHelpers.URL_COMPOUND_KEY.getPartAsString("src"),
            criteriaArray).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);

    Assert.assertEquals(messages.getResults().size(), 2);
    BatchFinderCriteriaResult<Message> friendly = messages.getResults().get(0);
    Assert.assertFalse(friendly.isError());
    for (Message message : friendly.getElements())
    {
      Assert.assertEquals(message.getTone(), Tone.FRIENDLY);
    }

    BatchFinderCriteriaResult<Message> insulting = messages.getResults().get(1);
    Assert.assertTrue(insulting.isError());
    Assert.assertEquals( (int) insulting.getError().getStatus(), 404);
  }

  // ----- Test with Sub Resources ------
  // These tests is to verify subresources methods in FluentClient subresources
  // works as expected as non-subresources.

  @Test public void testSubResource_getSubClientFromParent() throws Exception
  {
    com.linkedin.restli.examples.greetings.client.Greeting
        greetingClient = new GreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    Greeting greeting = greetingClient.subgreetingsOf()
        .subsubgreetingOf(1L)
        .get()
        .toCompletableFuture()
        .get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(greeting.getId().longValue(), 10L); // value should be (pathKey * 10)
  }

  /**
   * Test {@link com.linkedin.restli.examples.greetings.server.CollectionUnderSimpleResource}
   * A complete set of request tests were tested in {@link TestSimpleResourceHierarchy}
   */
  @Test public void testSubResource_noPathKey() throws Exception
  {
     SubgreetingsFluentClient
         subgreetingsFluentClient = new SubgreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
     CompletableFuture<Greeting> future = subgreetingsFluentClient.get(1L).toCompletableFuture();
     Greeting greeting = future.get(5000, TimeUnit.MILLISECONDS);
     Assert.assertTrue(greeting.hasId());
     Assert.assertEquals((Long) 1L, greeting.getId());

    List<Long> ids = Arrays.asList(1L, 2L, 3L, 4L);
    Map<Long, EntityResponse<Greeting>> response =
        subgreetingsFluentClient.batchGet(new HashSet<>(ids)).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(response.size(), ids.size());

    // Update
    String updatedMessage = "updated";
    greeting.setMessage(updatedMessage);
    CompletionStage<Void> updateStage = subgreetingsFluentClient.update(1L, greeting).thenRun(() -> {
      try{
        Assert.assertEquals(
        subgreetingsFluentClient.get(1L).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS),
            greeting
        );
      }
      catch (Exception e)
      {
        Assert.fail("Unexpected error");
      }
    });
    updateStage.toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Assert.assertFalse(updateStage.toCompletableFuture().isCompletedExceptionally());

    // Partial update
    Greeting update = greeting.copy();
    String partialUpdateMessage = "Partial update message";
    update.setMessage(partialUpdateMessage);
    CompletionStage<Void> partialUpdateResult = subgreetingsFluentClient.partialUpdate(1L, PatchGenerator.diff(greeting, update));
    partialUpdateResult.toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(subgreetingsFluentClient.get(1L).toCompletableFuture().get(500, TimeUnit.MILLISECONDS).getMessage(), partialUpdateMessage);
    Assert.assertFalse(partialUpdateResult.toCompletableFuture().isCompletedExceptionally());


    // create
    String msg = Double.toString(Math.random());
    CompletionStage<Long> result = subgreetingsFluentClient.create(getGreeting(msg));
    CompletableFuture<Long> createFuture = result.toCompletableFuture();
    Long createdId = createFuture.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertTrue(subgreetingsFluentClient.get(createdId).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS).getMessage().equals(msg));

    // batch create
    String msg1 = Double.toString(Math.random());
    String msg2 = Double.toString(Math.random());
    CompletionStage<List<CreateIdStatus<Long>>>
        batchCreateStage = subgreetingsFluentClient.batchCreate(Arrays.asList(getGreeting(msg1), getGreeting(msg2)));
    CompletableFuture<List<CreateIdStatus<Long>>> batchCreateFuture = batchCreateStage.toCompletableFuture();
    List<CreateIdStatus<Long>> createdList = batchCreateFuture.get(5000, TimeUnit.MILLISECONDS);
    CompletionStage<Map<Long, EntityResponse<Greeting>>> batchGetStage =
        subgreetingsFluentClient.batchGet(createdList.stream().map(CreateIdStatus::getKey).collect(Collectors.toSet()));

    Map<Long, EntityResponse<Greeting>> entities = batchGetStage.toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(entities.size(), 2);
  }

  /**
   * Test {@link com.linkedin.restli.examples.greetings.server.SimpleResourceUnderCollectionResource}
   * A complete set of request tests were tested in {@link TestSimpleResourceHierarchy}
   */
  @Test public void testSubResource_oneLayerPathKey() throws Exception
  {
    // Get
    com.linkedin.restli.examples.greetings.client.Greeting.Subgreetings.Subsubgreeting
        subsubClient = new GreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine()).subgreetingsOf()
        .subsubgreetingOf(1L);
    Greeting greeting = subsubClient.get().toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(greeting.getId().longValue(), 10L); // value should be (pathKey * 10)

    // Update
    Greeting updateGreeting = new Greeting();
    updateGreeting.setMessage("Message1");
    updateGreeting.setTone(Tone.INSULTING);
    updateGreeting.setId(1L);
    subsubClient.update(updateGreeting).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(subsubClient.get().toCompletableFuture().get(5000, TimeUnit.MILLISECONDS).getTone(), Tone.INSULTING);

    // Partial Update
    Greeting partialUpdateGreeting = new Greeting();
    partialUpdateGreeting.setMessage("Message1");
    partialUpdateGreeting.setTone(Tone.SINCERE);
    partialUpdateGreeting.setId(1L);
    PatchRequest<Greeting> patch = PatchGenerator.diffEmpty(partialUpdateGreeting);
    subsubClient.partialUpdate(patch).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(subsubClient.get().toCompletableFuture().get(5000, TimeUnit.MILLISECONDS).getTone(), Tone.SINCERE);

    // Delete
    subsubClient.delete().toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    try
    {
      subsubClient.get().toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
      Assert.fail("Sub resource call without path key should fail");
    }
    catch (Exception e)
    {
      Assert.assertTrue(e.getCause() instanceof RestLiResponseException);
      Assert.assertEquals(((RestLiResponseException)e.getCause()).getStatus(), HttpStatus.S_404_NOT_FOUND.getCode());
    }

  }

  /**
   * Test {@link com.linkedin.restli.examples.greetings.server.CollectionOfCollectionOfCollectionOfSimpleResource}
   */
  @Test public void testSubResource_twoLayersPathKeys() throws Exception
  {
    com.linkedin.restli.examples.greetings.client.Greeting.Subgreetings.GreetingsOfgreetingsOfgreeting.GreetingsOfgreetingsOfgreetingsOfgreeting
        gggs =
        new GreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine())
            .subgreetingsOf()
            .greetingsOfgreetingsOfgreetingOf(100L).greetingsOfgreetingsOfgreetingsOfgreetingOf(1000L);

    Greeting response = gggs.get(10L).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Assert.assertTrue(response.getId() == 1110);
  }

  /**
   * Test {@link com.linkedin.restli.examples.greetings.server.AssociationsSubResource}
   *
   * A complete set of request tests were tested in {@link TestAssociationsResource}
   */
  @Test public void testSubResource_associationKey() throws Exception
  {
    //AssociationsSub
    String src = "src";
    String dest = "dest";
    String subKey = "subKey";
    CompoundKey key1 = new AssociationsFluentClient.Key().setSrc(src).setDest(dest);
    AssociationsSubFluentClient
        subFluentClient = new AssociationsSubFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    Message response = subFluentClient.withAssociationsId(key1).get(subKey).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(response.getId(), "src");
    Assert.assertEquals(response.getMessage(), "dest");

    // Repeat using sub client generated from parent
    Associations.AssociationsSub
        subFluentClient2 = new AssociationsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine()).associationsSubOf(key1);
    response = subFluentClient2.get(subKey).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(response.getId(), "src");
    Assert.assertEquals(response.getMessage(), "dest");
  }

  /**
   * Test {@link com.linkedin.restli.examples.greetings.server.AssociationsAssociationsSubResource}
  */
  @Test public void testSubResource_twoLayerAssociationPathKey() throws Exception
  {
    //AssociationsAssociations
    String src = "src";
    String anotherSrc = "anotherSrc";
    String dest = "dest";
    String anotherDest = "anotherDest";
    String subKey = "subKey";
    CompoundKey key1 = new AssociationsFluentClient.Key().setSrc(src).setDest(dest);
    CompoundKey key2 =
        new AssociationsAssociationsFluentClient.Key()
            .setAnotherSrc(anotherSrc)
            .setAnotherDest(anotherDest);
    AssociationsAssociationsSubFluentClient
        subFluentClient = new AssociationsAssociationsSubFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    Message response = subFluentClient.withAssociationsId(key1)
        .withAssociationsAssociationsId(key2)
        .get(subKey)
        .toCompletableFuture()
        .get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(response.getId(), src+anotherSrc+subKey);
    Assert.assertEquals(response.getMessage(), dest+anotherDest);

    // Repeat using sub client generated from parent
    Associations.AssociationsAssociations.AssociationsAssociationsSub
        subFluentClient2 = new AssociationsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine())
        .associationsAssociationsOf(key1).associationsAssociationsSubOf(key2);

    response = subFluentClient2
        .get(subKey)
        .toCompletableFuture()
        .get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(response.getId(), src+anotherSrc+subKey);
    Assert.assertEquals(response.getMessage(), dest+anotherDest);
  }

  /**
   * Test {@link com.linkedin.restli.server.resources.ComplexKeyResource}
   *
   * A complete set of request tests were tested in {@link TestComplexKeysResource}
   */
  @Test public void testSubResource_complexKeyParentResource() throws Exception
  {
    TwoPartKey key = new TwoPartKey();
    key.setMajor("a");
    key.setMinor("b");
    TwoPartKey param = new TwoPartKey();
    param.setMajor("c");
    param.setMinor("d");
    ComplexResourceKey<TwoPartKey, TwoPartKey> complexKey = new ComplexResourceKey<TwoPartKey, TwoPartKey>(key, param);
    ComplexKeysSubFluentClient
        subFluentClient = new ComplexKeysSubFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    subFluentClient.withKeys(complexKey);
    TwoPartKey response = subFluentClient.get("stringKey").toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(response.getMajor(), "aANDc");
    Assert.assertEquals(response.getMinor(), "bANDd");

    // Repeat using sub client generated from parent

    ComplexKeys.ComplexKeysSub
        subFluentClient2 = new ComplexKeysFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine()).complexKeysSubOf(complexKey);
    response = subFluentClient2.get("stringKey").toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(response.getMajor(), "aANDc");
    Assert.assertEquals(response.getMinor(), "bANDd");

  }

  // ----- Tests with actions ------
  @Test public void testCollectionEntityActionWithReturn() throws Exception
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    Greeting newGreeting;
    newGreeting =  greetings.updateTone(1L,
          param -> param.setNewTone(Tone.SINCERE).setDelOld(false))
        .toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Assert.assertNotNull(newGreeting);
    Assert.assertEquals(newGreeting.getId().longValue(), 1L);
    Assert.assertEquals(newGreeting.getTone(), Tone.SINCERE);

    newGreeting =  greetings.updateTone(1L, param -> param.setNewTone(Tone.INSULTING)).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Assert.assertNotNull(newGreeting);
    Assert.assertEquals(newGreeting.getId().longValue(), 1L);
    Assert.assertEquals(newGreeting.getTone(), Tone.INSULTING);
  }

  @Test public void testCollectionEntityActionWithNoReturn() throws Exception
  {
    // TestGroupsResource
    String testEmail = "test@test.com";
    TransferOwnershipRequest ownershipRequest = new TransferOwnershipRequest();
    ownershipRequest.setNewOwnerContactEmail(testEmail);
    int testId = 9999;
    ownershipRequest.setNewOwnerMemberID(testId);
    Groups groupsFluentClient = new GroupsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    CompletableFuture<Void> response =
        groupsFluentClient.transferOwnership(1, param -> param.setRequest(ownershipRequest))
            .toCompletableFuture();
    response.get(5000, TimeUnit.MILLISECONDS);
    assert(!response.isCompletedExceptionally());
  }

  @Test public void testCollectionActionWithReturn() throws Exception
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    Assert.assertTrue(greetings.purge().toCompletableFuture().get(5000, TimeUnit.MILLISECONDS) == 100);
  }

  @Test public void testCollectionActionWithNoReturn() throws Exception
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    CompletableFuture<Void> stage = greetings.anotherAction(param -> param.setBitfield(new BooleanArray())
        .setRequest(new TransferOwnershipRequest())
        .setSomeString("")
        .setStringMap(new StringMap())).toCompletableFuture();
    Assert.assertNull(stage.get(5000, TimeUnit.MILLISECONDS));
    assert(!stage.isCompletedExceptionally());
  }

  @Test(expectedExceptions = {RestLiResponseException.class})
  public void testCollectionActionWithException() throws Throwable
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    CompletableFuture<Void> stage = greetings.exceptionTest().toCompletableFuture();
    try
    {
      stage.get(5000, TimeUnit.MILLISECONDS);
      Assert.fail("expected exception");
    }
    catch (Exception e)
    {
      assert(stage.isCompletedExceptionally());
      throw e.getCause();
    }
  }

  @Test public void testActionSetActionWithReturn() throws Exception
  {
    Actions actionsFluentClient = new ActionsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    Assert.assertTrue(actionsFluentClient.ultimateAnswer().toCompletableFuture().get(5000, TimeUnit.MILLISECONDS) == 42);
  }

  @Test public void testActionSetActionWithNoReturn() throws Exception
  {
    Actions actionsFluentClient = new ActionsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    Assert.assertNull(actionsFluentClient.returnVoid().toCompletableFuture().get(5000, TimeUnit.MILLISECONDS));

  }

  @Test public void testActionSetActionWithTypeRef() throws Exception
  {
    // This end point use typeref for both ActionParam and Action methods' return value
    Actions actionsFluentClient = new ActionsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    Assert.assertEquals(actionsFluentClient.customTypeRef(new CustomLong(500L)).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS), new CustomLong(500L));
  }

  // ----- Test TypeRef cases ------

  @Test public void testTypeRef_keyTypeRef() throws Exception
  {
    CustomTypes2 customTypes2FluentClient = new CustomTypes2FluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    Assert.assertEquals(customTypes2FluentClient.get(new CustomLong(1L))
        .toCompletableFuture()
        .get(5000, TimeUnit.MILLISECONDS)
        .getId()
        .longValue(), 1L);
  }

  // ----- Testing ComplexKeysResource ------
  private static ComplexResourceKey<TwoPartKey, TwoPartKey> getComplexKey(String major, String minor)
  {
    return new ComplexResourceKey<TwoPartKey, TwoPartKey>(
        new TwoPartKey().setMajor(major).setMinor(minor),
        new TwoPartKey());
  }

  private static List<ComplexResourceKey<TwoPartKey, TwoPartKey>> getBatchComplexKeys()
  {
    List<ComplexResourceKey<TwoPartKey, TwoPartKey>> ids =
      new ArrayList<ComplexResourceKey<TwoPartKey, TwoPartKey>>();
    ComplexResourceKey<TwoPartKey, TwoPartKey> key1 = getComplexKey(StringTestKeys.SIMPLEKEY, StringTestKeys.SIMPLEKEY2);
    ComplexResourceKey<TwoPartKey, TwoPartKey> key2 = getComplexKey(StringTestKeys.URL, StringTestKeys.URL2);
    ComplexResourceKey<TwoPartKey, TwoPartKey> key3 = getComplexKey(StringTestKeys.ERROR, StringTestKeys.ERROR);
    ids.add(key1);
    ids.add(key2);
    ids.add(key3);

    return ids;
  }

  // ComplexKeysResource: Test "Get", "create", "batchGet"
  @Test public void testComplexKey_get() throws Exception
  {
    ComplexKeys complexKeyClient = new ComplexKeysFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    ComplexResourceKey<TwoPartKey, TwoPartKey> testKeys = getComplexKey(
            StringTestKeys.SIMPLEKEY, StringTestKeys.SIMPLEKEY2);

    Message result = complexKeyClient.get(testKeys).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);


    Assert.assertTrue(result.hasId());
    Assert.assertEquals(result.getMessage(), StringTestKeys.SIMPLEKEY + " " + StringTestKeys.SIMPLEKEY2);
  }

  @Test public void testComplexKey_batchGet() throws Exception
  {
    List<ComplexResourceKey<TwoPartKey, TwoPartKey>> ids = getBatchComplexKeys();
    ComplexKeys complexKeyClient = new ComplexKeysFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    Map<ComplexResourceKey<TwoPartKey, TwoPartKey>, EntityResponse<Message>> resultMap =
        complexKeyClient.batchGet(new HashSet<>(ids)).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);

    Assert.assertEquals(resultMap.size(), 3);
    Assert.assertNotNull(resultMap.get(ids.get(0)).getEntity());
    Assert.assertNotNull(resultMap.get(ids.get(1)).getEntity());
    Assert.assertNotNull(resultMap.get(ids.get(2)).getError());
  }

  @Test public void testComplexKey_createThenGet() throws Exception
  {
      final String messageText = "newMessage";
      Message message = new Message();
      message.setMessage(messageText);

      ComplexKeys complexKeyClient = new ComplexKeysFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    ComplexResourceKey<TwoPartKey, TwoPartKey> result =
        complexKeyClient.create(message).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);

      Assert.assertEquals(result, getComplexKey(messageText, messageText));
      Assert.assertEquals(complexKeyClient.get(result).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS).getMessage(), messageText);
  }

  @Test public void testComplexKey_batchUpdate() throws Exception
  {
    ComplexKeys complexKeyClient = new ComplexKeysFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    final String messageText = StringTestKeys.SIMPLEKEY + " " + StringTestKeys.SIMPLEKEY2;
    final Message message = new Message();
    message.setId(StringTestKeys.SIMPLEKEY + " " + StringTestKeys.SIMPLEKEY2);
    message.setMessage(messageText);
    message.setTone(Tone.INSULTING);
    final String messageText2 = StringTestKeys.URL + " " + StringTestKeys.URL2;
    final Message message2 = new Message();
    message2.setId(StringTestKeys.URL + " " + StringTestKeys.URL2);
    message2.setMessage(messageText2);
    message2.setTone(Tone.INSULTING);

    final Map<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> inputs = new HashMap<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message>();
    ComplexResourceKey<TwoPartKey, TwoPartKey> key1 = getComplexKey(StringTestKeys.SIMPLEKEY, StringTestKeys.SIMPLEKEY2);
    ComplexResourceKey<TwoPartKey, TwoPartKey> key2 = getComplexKey(StringTestKeys.URL, StringTestKeys.URL2);
    inputs.put(key1, message);
    inputs.put(key2, message2);
    complexKeyClient.batchUpdate(inputs).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Map<ComplexResourceKey<TwoPartKey, TwoPartKey>, EntityResponse<Message>> result =
        complexKeyClient.batchGet(new HashSet<>(Arrays.asList(key1, key2)))
            .toCompletableFuture()
            .get(5000, TimeUnit.MILLISECONDS);

    Assert.assertNotNull(result.get(key1));
    Assert.assertNotNull(result.get(key2));
    Assert.assertEquals(result.get(key1).getEntity().getTone(), Tone.INSULTING);
    Assert.assertEquals(result.get(key2).getEntity().getTone(), Tone.INSULTING);
  }

  @Test public void testComplexKey_partialUpdate() throws Exception
  {
    ComplexKeys complexKeyClient = new ComplexKeysFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    Message message = new Message();
    message.setTone(Tone.FRIENDLY);

    PatchRequest<Message> patch = PatchGenerator.diffEmpty(message);

    final Map<ComplexResourceKey<TwoPartKey, TwoPartKey>, PatchRequest<Message>> inputs =
        new HashMap<ComplexResourceKey<TwoPartKey, TwoPartKey>, PatchRequest<Message>>();
    ComplexResourceKey<TwoPartKey, TwoPartKey> key1 = getComplexKey(StringTestKeys.SIMPLEKEY, StringTestKeys.SIMPLEKEY2);
    ComplexResourceKey<TwoPartKey, TwoPartKey> key2 = getComplexKey(StringTestKeys.URL, StringTestKeys.URL2);
    inputs.put(key1, patch);
    inputs.put(key2, patch);

    Map<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus> result =
        complexKeyClient.batchPartialUpdate(inputs).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);

    // Update return valid result
    Assert.assertEquals(result.get(key1).getStatus().intValue(), 204);
    Assert.assertEquals(result.get(key2).getStatus().intValue(), 204);

    Map<ComplexResourceKey<TwoPartKey, TwoPartKey>, EntityResponse<Message>> getResult =
        complexKeyClient.batchGet(new HashSet<>(Arrays.asList(key1, key2)))
            .toCompletableFuture()
            .get(5000, TimeUnit.MILLISECONDS);

    Assert.assertEquals(getResult.get(key1).getEntity().getTone(), Tone.FRIENDLY);
    Assert.assertEquals(getResult.get(key2).getEntity().getTone(), Tone.FRIENDLY);
  }

  @Test public void testComplexKey_batchDelete() throws Exception
  {
    String messageText = "m1";
    Message message = new Message();
    message.setMessage(messageText);

    ComplexKeys complexKeyClient = new ComplexKeysFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    ComplexResourceKey<TwoPartKey, TwoPartKey> createResponse =
        complexKeyClient.create(message).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);

    String messageText2 = "m2";
    message.setMessage(messageText2);

    createResponse = complexKeyClient.create(message).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);

    ComplexResourceKey<TwoPartKey, TwoPartKey> key1 = getComplexKey(messageText, messageText);
    ComplexResourceKey<TwoPartKey, TwoPartKey> key2 = getComplexKey(messageText2, messageText2);

    ArrayList<ComplexResourceKey<TwoPartKey, TwoPartKey>> ids = new ArrayList<ComplexResourceKey<TwoPartKey, TwoPartKey>>();
    ids.add(key1);
    ids.add(key2);
    Map<ComplexResourceKey<TwoPartKey, TwoPartKey>, UpdateStatus> deleteResponse =
        complexKeyClient.batchDelete(new HashSet<>(ids)).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);

    Assert.assertEquals(deleteResponse.size(), ids.size());
    Assert.assertEquals(deleteResponse.get(key1).getStatus().intValue(), 204);
    Assert.assertEquals(deleteResponse.get(key2).getStatus().intValue(), 204);
    Map<ComplexResourceKey<TwoPartKey, TwoPartKey>, EntityResponse<Message>> getResponse =
        complexKeyClient.batchGet(new HashSet<>(ids)).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(getResponse.get(key1).getError().getStatus().intValue(), 404);
    Assert.assertEquals(getResponse.get(key2).getError().getStatus().intValue(), 404);

  }

  @Test public void testComplexKey_entityAction() throws Exception
  {
    ComplexResourceKey<TwoPartKey, TwoPartKey> key = getComplexKey("major", "minor");
    ComplexKeys complexKeyClient = new ComplexKeysFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    Integer entity = complexKeyClient.entityAction(key).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(entity.longValue(), 1L);
  }

  // ----- utils used for testing ------

  private Greeting getGreeting()
  {
    return getGreeting(MESSAGE);
  }

  private Greeting getGreeting(String message)
  {
    return new Greeting().setId(100L).setMessage(message).setTone(Tone.FRIENDLY);
  }
}
