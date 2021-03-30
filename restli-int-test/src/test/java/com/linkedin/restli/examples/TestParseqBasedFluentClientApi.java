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

import com.linkedin.parseq.ParSeqUnitTestHelper;
import com.linkedin.restli.client.ParSeqRestliClient;
import com.linkedin.restli.client.ParSeqRestliClientBuilder;
import com.linkedin.restli.client.ParSeqRestliClientConfigBuilder;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.client.util.PatchGenerator;
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
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.api.TwoPartKey;
import com.linkedin.restli.examples.greetings.client.AssociationsAssociationsSubFluentClient;
import com.linkedin.restli.examples.greetings.client.AssociationsFluentClient;
import com.linkedin.restli.examples.greetings.client.AssociationsSubFluentClient;
import com.linkedin.restli.examples.greetings.client.AssociationsAssociationsFluentClient;
import com.linkedin.restli.examples.greetings.client.ComplexKeysSubFluentClient;
import com.linkedin.restli.examples.greetings.client.CreateGreetingFluentClient;
import com.linkedin.restli.examples.greetings.client.GreetingFluentClient;
import com.linkedin.restli.examples.greetings.client.GreetingsFluentClient;
import com.linkedin.restli.examples.greetings.client.GreetingsOfgreetingsOfgreetingsOfgreetingFluentClient;
import com.linkedin.restli.examples.greetings.client.PartialUpdateGreetingFluentClient;
import com.linkedin.restli.examples.greetings.client.SubgreetingsFluentClient;
import com.linkedin.restli.examples.greetings.client.SubsubgreetingFluentClient;
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
    GreetingsFluentClient greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CompletionStage<Greeting> result = greetings.get(1L);
    CompletableFuture<Greeting> future = result.toCompletableFuture();
    Greeting greeting = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertTrue(greeting.hasId());
    Assert.assertEquals((Long) 1L, greeting.getId());
  }

  @Test
  public void testGetRequestFailure() throws Exception
  {
    GreetingsFluentClient greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

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
    GreetingsFluentClient greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

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
    GreetingsFluentClient greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

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
    GreetingsFluentClient greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    CompletionStage<Long> result = greetings.create(getGreeting(), false);
    CompletableFuture<Long> future = result.toCompletableFuture();
    Assert.assertNotNull(future.get(5000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testCreateNullId() throws Exception
  {
    GreetingsFluentClient greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CompletionStage<Long> result = greetings.create(getGreeting(), true);
    CompletableFuture<Long> future = result.toCompletableFuture();
    Assert.assertNull(future.get(5000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testCreateReturnEntity() throws Exception
  {
    CreateGreetingFluentClient greetings = new CreateGreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    String msg = Double.toString(Math.random());
    CompletionStage<IdEntityResponse<Long, Greeting>> result = greetings.createAndGet(getGreeting(msg));
    CompletableFuture<IdEntityResponse<Long, Greeting>> future = result.toCompletableFuture();
    Assert.assertNotNull(future.get(5000, TimeUnit.MILLISECONDS));
    Assert.assertEquals(msg, future.get().getEntity().getMessage());
  }

  @Test
  public void testCreateReturnEntityDisabled() throws Exception
  {
    CreateGreetingFluentClient greetings = new CreateGreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CompletionStage<Long> result = greetings.create(getGreeting());
    CompletableFuture<Long> future = result.toCompletableFuture();
    Assert.assertNotNull(future.get(5000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testBatchCreate() throws Exception
  {
    GreetingsFluentClient greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CompletionStage<List<CreateIdStatus<Long>>> result = greetings.batchCreate(
        Arrays.asList(getGreeting(), getGreeting()));
    CompletableFuture<List<CreateIdStatus<Long>>> future = result.toCompletableFuture();
    List<CreateIdStatus<Long>> ids = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(ids.size(), 2);
  }

  @Test
  public void testBatchCreateReturnEntity() throws Exception
  {
    CreateGreetingFluentClient greetings = new CreateGreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

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
    CreateGreetingFluentClient greetings = new CreateGreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CompletionStage<List<CreateIdStatus<Long>>>
        result = greetings.batchCreate(Arrays.asList(getGreeting(), getGreeting()));
    CompletableFuture<List<CreateIdStatus<Long>>> future = result.toCompletableFuture();
    List<CreateIdStatus<Long>> ids = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(ids.size(), 2);
  }

  @Test
  public void testCreateAndThenBatchDelete() throws Exception
  {
    GreetingsFluentClient greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
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
    GreetingsFluentClient greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
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
    GreetingsFluentClient greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    // Create entities first so we don't delete those used by other tests.
    CompletionStage<Long> createResult = greetings.create(getGreeting(), false);

    CompletionStage<Void> result = createResult.thenCompose(greetings::delete);
    CompletableFuture<Void> future = result.toCompletableFuture();
    future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertFalse(future.isCompletedExceptionally());
  }

  @Test
  public void testDeleteFailure() throws Exception
  {
    GreetingsFluentClient greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
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
    GreetingsFluentClient greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    // Create a new greeting to use for partial update test.
    CompletionStage<Long> createResult = greetings.create(getGreeting(), false);
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
    PartialUpdateGreetingFluentClient greetings = new PartialUpdateGreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
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
    PartialUpdateGreetingFluentClient greetings = new PartialUpdateGreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

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
    PartialUpdateGreetingFluentClient greetings = new PartialUpdateGreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

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
    PartialUpdateGreetingFluentClient greetings = new PartialUpdateGreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

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
    PartialUpdateGreetingFluentClient greetings = new PartialUpdateGreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

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
    PartialUpdateGreetingFluentClient greetings = new PartialUpdateGreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

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
    GreetingsFluentClient greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
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
    GreetingsFluentClient greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    final Map<Long, Greeting> inputs = new HashMap<>();
    inputs.put(-6L, getGreeting());
    CompletionStage<Long> createResult = greetings.create(getGreeting(), false);

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
    GreetingsFluentClient greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    CompletionStage<Long> createResult = greetings.create(getGreeting(), false);
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
    GreetingsFluentClient greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
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
    GreetingsFluentClient greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    // Create some greetings with "GetAll" in message so they will be returned by getAll test method..
    CompletionStage<List<CreateIdStatus<Long>>> createResult = greetings.batchCreate(
        Arrays.asList(getGreeting("GetAll"), getGreeting("GetAll")));

    CompletionStage<List<Greeting>> result = createResult.thenCompose(ids -> greetings.getAll());
    CompletableFuture<List<Greeting>> future = result.toCompletableFuture();
    List<Greeting> greetingList = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertTrue(greetingList.size() >= 2);
    for (Greeting greeting :greetingList)
    {
      Assert.assertTrue(greeting.getMessage().contains("GetAll"));
    }
  }

  // ----- Test with Simple Resources ------

  @Test
  public void testSimpleResourceUpdate() throws Exception
  {
    final String message = "Update test";
    GreetingFluentClient greeting = new GreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    greeting.update(getGreeting(message)).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Greeting greetingEntity = greeting.get().toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(greetingEntity.getMessage(), message);
  }

  @Test
  public void testSimpleResourceDeleteMethodAndGetMethod() throws Exception
  {
    GreetingFluentClient greeting = new GreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
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

  @Test
  public void testAssociateResourceGet() throws Exception
  {
    CompoundKey key = AssociationResourceHelpers.URL_COMPOUND_KEY;
    AssociationsFluentClient associations =
        new AssociationsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    Message response = associations.get(key).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Assert.assertTrue(response.hasId());
  }

  @Test
  public void testAssociateResourceBatchGet() throws Exception
  {
    AssociationsFluentClient associations =
        new AssociationsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    Map<CompoundKey, EntityResponse<Message>> entityResponse =
        associations.batchGet(AssociationResourceHelpers.DB.keySet())
            .toCompletableFuture()
            .get(5000, TimeUnit.MILLISECONDS);
    for (CompoundKey id : AssociationResourceHelpers.DB.keySet())
    {
      Assert.assertTrue(entityResponse.containsKey(id));
      EntityResponse<Message> single = entityResponse.get(id);
      Assert.assertEquals(single.getEntity(), AssociationResourceHelpers.DB.get(id));
    }
  }

  @Test
  public void testAssociateResourceBatchUpdate() throws Exception
  {
    AssociationsFluentClient associations =
        new AssociationsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    Map<CompoundKey, UpdateStatus> ids =
        associations.batchUpdate(AssociationResourceHelpers.DB).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);

    Assert.assertEquals(ids.size(), 2);
    for (CompoundKey id : ids.keySet())
    {
      Assert.assertEquals(ids.get(id).getStatus().intValue(), 204);
    }
  }

  @Test
  public void testAssociateResourceBatchPartialUpdate() throws Exception
  {
    AssociationsFluentClient associations =
        new AssociationsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    Map<CompoundKey, PatchRequest<Message>> patches = new HashMap<CompoundKey, PatchRequest<Message>>();
    patches.put(AssociationResourceHelpers.URL_COMPOUND_KEY, new PatchRequest<Message>());
    patches.put(AssociationResourceHelpers.SIMPLE_COMPOUND_KEY, new PatchRequest<Message>());

    Map<CompoundKey, UpdateStatus> ids =
        associations.batchPartialUpdate(patches).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);

    for (CompoundKey id : ids.keySet())
    {
      Assert.assertEquals(ids.get(id).getStatus().intValue(), 204);
    }
  }

  // ----- Test with Sub Resources ------
  // These tests is to verify subresources methods in FluentClient subresources
  // works as expected as non-subresources.

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
    SubsubgreetingFluentClient subsubClient = new SubsubgreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    Greeting greeting = subsubClient.withSubgreetingsId(1L).get().toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(greeting.getId().longValue(), 10L);

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
    GreetingsOfgreetingsOfgreetingsOfgreetingFluentClient gggs =
        new GreetingsOfgreetingsOfgreetingsOfgreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    gggs.withSubgreetingsId(100L);
    gggs.withGreetingsOfgreetingsOfgreetingId(1000L);
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
    AssociationsSubFluentClient subFluentClient = new AssociationsSubFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    Message response = subFluentClient.withAssociationsId(key1).get(subKey).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
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
    AssociationsAssociationsSubFluentClient subFluentClient = new AssociationsAssociationsSubFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    Message response = subFluentClient.withAssociationsId(key1)
        .withAssociationsAssociationsId(key2)
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
  @Test public void testSubResource_complexKey() throws Exception
  {
    TwoPartKey key = new TwoPartKey();
    key.setMajor("a");
    key.setMinor("b");
    TwoPartKey param = new TwoPartKey();
    param.setMajor("c");
    param.setMinor("d");
    ComplexResourceKey<TwoPartKey, TwoPartKey> complexKey = new ComplexResourceKey<TwoPartKey, TwoPartKey>(key, param);
    ComplexKeysSubFluentClient subFluentClient = new ComplexKeysSubFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    subFluentClient.withKeys(complexKey);
    TwoPartKey response = subFluentClient.get("stringKey").toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(response.getMajor(), "aANDc");
    Assert.assertEquals(response.getMinor(), "bANDd");
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
