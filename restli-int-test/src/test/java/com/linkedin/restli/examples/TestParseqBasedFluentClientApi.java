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
import com.linkedin.restli.common.CreateIdEntityStatus;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.IdEntityResponse;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.UpdateEntityStatus;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.CreateGreeting;
import com.linkedin.restli.examples.greetings.client.Greetings;
import com.linkedin.restli.examples.greetings.client.PartialUpdateGreeting;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
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
  public void testGetRequest() throws InterruptedException, ExecutionException
  {
    Greetings greetings = new Greetings(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CountDownLatch latch = new CountDownLatch(1);
    CompletionStage<Greeting> result = greetings.get(1L);
    result.whenComplete((r, t) -> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<Greeting> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    Assert.assertFalse(future.isCompletedExceptionally());
    Greeting greeting = future.get();
    Assert.assertTrue(greeting.hasId());
    Assert.assertEquals((Long) 1L, greeting.getId());
  }

  @Test
  public void testGetRequestFailure() throws InterruptedException
  {
    Greetings greetings = new Greetings(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CountDownLatch latch = new CountDownLatch(1);
    CompletionStage<Greeting> result = greetings.get(-1L);
    result.whenComplete((r, t) -> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS), "Request timed out.");
    CompletableFuture<Greeting> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    Assert.assertTrue(future.isCompletedExceptionally());
  }


  @Test
  public void testBatchGetRequest() throws InterruptedException, ExecutionException
  {
    Greetings greetings = new Greetings(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CountDownLatch latch = new CountDownLatch(1);
    Set<Long> ids = Sets.newHashSet(Arrays.asList(1L, 2L, 3L));
    CompletionStage<Map<Long, EntityResponse<Greeting>>> result = greetings.batchGet(ids);
    result.whenComplete((r, t)-> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<Map<Long, EntityResponse<Greeting>>> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    Map<Long, EntityResponse<Greeting>> resultMap = future.get();
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
  public void testBatchGetRequestWithPartialErrors() throws InterruptedException, ExecutionException
  {
    Greetings greetings = new Greetings(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CountDownLatch latch = new CountDownLatch(1);
    Set<Long> ids = Sets.newHashSet(Arrays.asList(-1L, -2L, 1L, 2L, 3L));
    CompletionStage<Map<Long, EntityResponse<Greeting>>> result = greetings.batchGet(ids);
    result.whenComplete((r, t)-> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<Map<Long, EntityResponse<Greeting>>> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    Map<Long, EntityResponse<Greeting>> resultMap = future.get();
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
  public void testCreate() throws InterruptedException, ExecutionException
  {
    Greetings greetings = new Greetings(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    CountDownLatch latch = new CountDownLatch(1);
    CompletionStage<Long> result = greetings.create(getGreeting(), false);
    result.whenComplete((r, t)-> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<Long> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    Assert.assertNotNull(future.get());
  }

  @Test
  public void testCreateNullId() throws InterruptedException, ExecutionException
  {
    Greetings greetings = new Greetings(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CountDownLatch latch = new CountDownLatch(1);
    CompletionStage<Long> result = greetings.create(getGreeting(), true);
    result.whenComplete((r, t)-> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<Long> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    Assert.assertNull(future.get());
  }

  @Test
  public void testCreateReturnEntity() throws InterruptedException, ExecutionException
  {
    CreateGreeting greetings = new CreateGreeting(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CountDownLatch latch = new CountDownLatch(1);
    String msg = Double.toString(Math.random());
    CompletionStage<IdEntityResponse<Long, Greeting>> result = greetings.createAndGet(getGreeting(msg));
    result.whenComplete((r, t)-> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<IdEntityResponse<Long, Greeting>> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    Assert.assertNotNull(future.get());
    Assert.assertEquals(msg, future.get().getEntity().getMessage());
  }

  @Test
  public void testCreateReturnEntityDisabled() throws InterruptedException, ExecutionException
  {
    CreateGreeting greetings = new CreateGreeting(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CountDownLatch latch = new CountDownLatch(1);
    CompletionStage<Long> result = greetings.create(getGreeting());
    result.whenComplete((r, t)-> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<Long> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    Assert.assertNotNull(future.get());
  }

  @Test
  public void testBatchCreate() throws InterruptedException, ExecutionException
  {
    Greetings greetings = new Greetings(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CountDownLatch latch = new CountDownLatch(1);
    CompletionStage<List<CreateIdStatus<Long>>> result = greetings.batchCreate(
        Arrays.asList(getGreeting(), getGreeting()));
    result.whenComplete((r, t)-> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<List<CreateIdStatus<Long>>> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    List<CreateIdStatus<Long>> ids = future.get();
    Assert.assertEquals(ids.size(), 2);
  }

  @Test
  public void testBatchCreateReturnEntity() throws InterruptedException, ExecutionException
  {
    CreateGreeting greetings = new CreateGreeting(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CountDownLatch latch = new CountDownLatch(1);
    String msg1 = Double.toString(Math.random());
    String msg2 = Double.toString(Math.random());
    CompletionStage<List<CreateIdEntityStatus<Long, Greeting>>>
        result = greetings.batchCreateAndGet(Arrays.asList(getGreeting(msg1), getGreeting(msg2)));
    result.whenComplete((r, t)-> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<List<CreateIdEntityStatus<Long, Greeting>>> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    List<CreateIdEntityStatus<Long, Greeting>> entities = future.get();
    Assert.assertEquals(entities.size(), 2);
    Assert.assertNotNull(entities.get(0).getEntity());
    Assert.assertEquals(msg1, entities.get(0).getEntity().getMessage());
    Assert.assertNotNull(entities.get(1).getEntity());
    Assert.assertEquals(msg2, entities.get(1).getEntity().getMessage());
  }

  @Test
  public void testBatchCreateReturnEntityDisabled() throws InterruptedException, ExecutionException
  {
    CreateGreeting greetings = new CreateGreeting(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CountDownLatch latch = new CountDownLatch(1);
    CompletionStage<List<CreateIdStatus<Long>>>
        result = greetings.batchCreate(Arrays.asList(getGreeting(), getGreeting()));
    result.whenComplete((r, t)-> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<List<CreateIdStatus<Long>>> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    List<CreateIdStatus<Long>> ids = future.get();
    Assert.assertEquals(ids.size(), 2);
  }

  @Test
  public void testCreateAndThenBatchDelete() throws InterruptedException, ExecutionException
  {
    Greetings greetings = new Greetings(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    CountDownLatch latch = new CountDownLatch(1);
    // Create entities first so we don't delete those used by other tests.
    CompletionStage<List<CreateIdStatus<Long>>>
        createResult = greetings.batchCreate(Arrays.asList(getGreeting(), getGreeting()));

    CompletionStage<Map<Long, UpdateStatus>>
        result = createResult.thenCompose(ids -> greetings.batchDelete(
            Sets.newHashSet(ids.stream().map(CreateIdStatus::getKey).collect(Collectors.toList()))));
    result.whenComplete((r, t)-> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<Map<Long, UpdateStatus>> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    Map<Long, UpdateStatus> ids = future.get();
    Assert.assertEquals(ids.size(), 2);
    for (UpdateStatus status : ids.values())
    {
      Assert.assertEquals(status.getStatus().intValue(), 204);
    }
  }

  @Test
  public void testCreateAndThenBatchDeleteWithFailures() throws InterruptedException, ExecutionException
  {
    Greetings greetings = new Greetings(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    CountDownLatch latch = new CountDownLatch(1);
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
    result.whenComplete((r, t)-> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<Map<Long, UpdateStatus>> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    Map<Long, UpdateStatus> ids = future.get();
    Assert.assertEquals(ids.size(), 3);
    Assert.assertEquals(ids.remove(-1L).getStatus().intValue(), 404);
    for (UpdateStatus status : ids.values())
    {
      Assert.assertEquals(status.getStatus().intValue(), 204);
    }
  }

  @Test
  public void testCreateAndThenDelete() throws InterruptedException
  {
    Greetings greetings = new Greetings(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    CountDownLatch latch = new CountDownLatch(1);
    // Create entities first so we don't delete those used by other tests.
    CompletionStage<Long> createResult = greetings.create(getGreeting(), false);

    CompletionStage<Void> result = createResult.thenCompose(greetings::delete);
    result.whenComplete((r, t)-> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<Void> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
  }

  @Test
  public void testDeleteFailure() throws InterruptedException
  {
    Greetings greetings = new Greetings(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    CountDownLatch latch = new CountDownLatch(1);
    CompletionStage<Void> result = greetings.delete(-1L);
    result.whenComplete((r, t)-> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<Void> future = result.toCompletableFuture();
    Assert.assertTrue(future.isCompletedExceptionally());
    try
    {
      future.get();
      Assert.fail("Expected failure");
    } catch (ExecutionException e)
    {
      Assert.assertEquals(((RestLiResponseException) e.getCause()).getStatus(), 404);
    }
  }

  @Test
  public void testGetAndThenPartialUpdate() throws InterruptedException, ExecutionException
  {
    Greetings greetings = new Greetings(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CountDownLatch latch = new CountDownLatch(1);
    CompletionStage<Greeting> result = greetings.get(1L);
    CompletionStage<Void> updateResult = result.thenCompose(greeting ->
    {
      try {
        Greeting update = greeting.copy();
        greeting.setMessage("Partial update message");
        return greetings.partialUpdate(1L, PatchGenerator.diff(greeting, update));
      } catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }
    });
    updateResult.whenComplete((r, t) -> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<Void> future = updateResult.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    Assert.assertFalse(future.isCompletedExceptionally());
  }

  @Test
  public void testPartialUpdateAndGet() throws InterruptedException, ExecutionException
  {
    PartialUpdateGreeting greetings = new PartialUpdateGreeting(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CountDownLatch latch = new CountDownLatch(1);
    Greeting original = getGreeting();
    String message = "Edited message: fluent api test partialUpdateAndGet";
    Greeting update = getGreeting(message);
    CompletionStage<Greeting> result = greetings.partialUpdateAndGet(1L, PatchGenerator.diff(original, update));
    result.whenComplete((r, t) -> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<Greeting> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    Assert.assertEquals(future.get().getMessage(), message);
  }

  @Test
  public void testPartialUpdateError() throws InterruptedException, ExecutionException
  {
    PartialUpdateGreeting greetings = new PartialUpdateGreeting(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CountDownLatch latch = new CountDownLatch(1);
    Greeting original = getGreeting();
    String message = "Edited message: fluent api test partialUpdateAndGet";
    Greeting update = getGreeting(message);
    CompletionStage<Greeting> result = greetings.partialUpdateAndGet(-1L, PatchGenerator.diff(original, update));
    result.whenComplete((r, t) -> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<Greeting> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    Assert.assertTrue(future.isCompletedExceptionally());
    try
    {
      future.get();
      Assert.fail("Expected failure");
    } catch (ExecutionException e)
    {
      Assert.assertEquals(((RestLiResponseException) e.getCause()).getStatus(), 404);
    }
  }

  @Test
  public void testBatchPartialUpdate() throws InterruptedException, ExecutionException
  {
    PartialUpdateGreeting greetings = new PartialUpdateGreeting(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CountDownLatch latch = new CountDownLatch(1);
    Map<Long, PatchRequest<Greeting>> inputs = new HashMap<>();
    Greeting original = getGreeting();
    String message = "Edited message: fluent api test partialUpdateAndGet";
    Greeting update = getGreeting(message);
    inputs.put(1L, PatchGenerator.diff(original, update));
    inputs.put(2L, PatchGenerator.diff(original, update));
    CompletionStage<Map<Long, UpdateStatus>> result = greetings.batchPartialUpdate(inputs);
    result.whenComplete((r, t) -> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<Map<Long, UpdateStatus>> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    Assert.assertEquals(future.get().get(1L).getStatus().intValue(), 200);
    Assert.assertEquals(future.get().get(2L).getStatus().intValue(), 200);
  }

  @Test
  public void testBatchPartialUpdateWithErrors() throws InterruptedException, ExecutionException
  {
    PartialUpdateGreeting greetings = new PartialUpdateGreeting(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CountDownLatch latch = new CountDownLatch(1);
    Map<Long, PatchRequest<Greeting>> inputs = new HashMap<>();
    Greeting original = getGreeting();
    String message = "Edited message: fluent api test partialUpdateAndGet";
    Greeting update = getGreeting(message);
    inputs.put(1L, PatchGenerator.diff(original, update));
    inputs.put(-2L, PatchGenerator.diff(original, update));
    CompletionStage<Map<Long, UpdateStatus>> result = greetings.batchPartialUpdate(inputs);
    result.whenComplete((r, t) -> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<Map<Long, UpdateStatus>> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    Assert.assertEquals(future.get().get(1L).getStatus().intValue(), 200);
    Assert.assertEquals(future.get().get(-2L).getStatus().intValue(), 404);
  }

  @Test
  public void testBatchPartialUpdateAndGet() throws InterruptedException, ExecutionException
  {
    PartialUpdateGreeting greetings = new PartialUpdateGreeting(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CountDownLatch latch = new CountDownLatch(1);
    Map<Long, PatchRequest<Greeting>> inputs = new HashMap<>();
    Greeting original = getGreeting();
    String message = "Edited message: fluent api test partialUpdateAndGet";
    Greeting update = getGreeting(message);
    inputs.put(1L, PatchGenerator.diff(original, update));
    inputs.put(2L, PatchGenerator.diff(original, update));
    CompletionStage<Map<Long, UpdateEntityStatus<Greeting>>> result = greetings.batchPartialUpdateAndGet(inputs);
    result.whenComplete((r, t) -> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<Map<Long, UpdateEntityStatus<Greeting>>> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    Assert.assertEquals(future.get().get(1L).getEntity().getId().longValue(), 1L);
    Assert.assertEquals(future.get().get(1L).getEntity().getMessage(), message);
    Assert.assertEquals(future.get().get(2L).getEntity().getId().longValue(), 2L);
    Assert.assertEquals(future.get().get(2L).getEntity().getMessage(), message);
  }

  @Test
  public void testBatchPartialUpdateAndGetWithErrors() throws InterruptedException, ExecutionException
  {
    PartialUpdateGreeting greetings = new PartialUpdateGreeting(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CountDownLatch latch = new CountDownLatch(1);
    Map<Long, PatchRequest<Greeting>> inputs = new HashMap<>();
    Greeting original = getGreeting();
    String message = "Edited message: fluent api test partialUpdateAndGet";
    Greeting update = getGreeting(message);
    inputs.put(1L, PatchGenerator.diff(original, update));
    inputs.put(-2L, PatchGenerator.diff(original, update));
    CompletionStage<Map<Long, UpdateEntityStatus<Greeting>>> result = greetings.batchPartialUpdateAndGet(inputs);
    result.whenComplete((r, t) -> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<Map<Long, UpdateEntityStatus<Greeting>>> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    Assert.assertEquals(future.get().get(1L).getStatus().intValue(), 200);
    Assert.assertEquals(future.get().get(1L).getEntity().getId().longValue(), 1L);
    Assert.assertEquals(future.get().get(1L).getEntity().getMessage(), message);
    Assert.assertEquals(future.get().get(-2L).getStatus().intValue(), 404);
    Assert.assertFalse(future.get().get(-2L).hasEntity());
  }

  @Test
  public void testBatchUpdate() throws InterruptedException, ExecutionException
  {
    Greetings greetings = new Greetings(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    Map<Long, Greeting> inputs = new HashMap<>();
    CountDownLatch latch = new CountDownLatch(1);
    inputs.put(5L, getGreeting());
    inputs.put(6L, getGreeting());
    CompletionStage<Map<Long, UpdateStatus>> result = greetings.batchUpdate(inputs);
    result.whenComplete((r, t)-> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<Map<Long, UpdateStatus>> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    Map<Long, UpdateStatus> ids = future.get();
    Assert.assertEquals(ids.size(), 2);
    Assert.assertEquals(ids.get(5L).getStatus().intValue(), 204);
    Assert.assertEquals(ids.get(6L).getStatus().intValue(), 204);
  }

  @Test
  public void testBatchUpdateWithErrors() throws InterruptedException, ExecutionException
  {
    Greetings greetings = new Greetings(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    Map<Long, Greeting> inputs = new HashMap<>();
    CountDownLatch latch = new CountDownLatch(1);
    inputs.put(5L, getGreeting());
    inputs.put(-6L, getGreeting());
    CompletionStage<Map<Long, UpdateStatus>> result = greetings.batchUpdate(inputs);
    result.whenComplete((r, t)-> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<Map<Long, UpdateStatus>> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    Map<Long, UpdateStatus> ids = future.get();
    Assert.assertEquals(ids.size(), 2);
    Assert.assertEquals(ids.get(5L).getStatus().intValue(), 204);
    Assert.assertEquals(ids.get(-6L).getStatus().intValue(), 404);
  }

  @Test
  public void testUpdate() throws InterruptedException, ExecutionException
  {
    Greetings greetings = new Greetings(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    CountDownLatch latch = new CountDownLatch(1);
    CompletionStage<Void> result = greetings.update(7L, getGreeting());
    result.whenComplete((r, t)-> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<Void> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    Assert.assertFalse(future.isCompletedExceptionally());
  }

  @Test
  public void testUpdateFailure() throws InterruptedException, ExecutionException
  {
    Greetings greetings = new Greetings(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    CountDownLatch latch = new CountDownLatch(1);
    CompletionStage<Void> result = greetings.update(-7L, getGreeting());
    result.whenComplete((r, t)-> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<Void> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    Assert.assertTrue(future.isCompletedExceptionally());
    try
    {
      future.get();
      Assert.fail("Expected failure");
    } catch (ExecutionException e)
    {
      Assert.assertEquals(((RestLiResponseException) e.getCause()).getStatus(), 404);
    }
  }

  @Test
  public void testGetAll() throws InterruptedException, ExecutionException
  {
    Greetings greetings = new Greetings(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CountDownLatch latch = new CountDownLatch(1);
    // Create some greetings with "GetAll" in message so they will be returned by getAll test method..
    CompletionStage<List<CreateIdStatus<Long>>> createResult = greetings.batchCreate(
        Arrays.asList(getGreeting("GetAll"), getGreeting("GetAll")));

    CompletionStage<List<Greeting>> result = createResult.thenCompose(ids -> greetings.getAll());
    result.whenComplete((r, t) -> latch.countDown());
    Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS),
        "Request timed out");
    CompletableFuture<List<Greeting>> future = result.toCompletableFuture();
    Assert.assertTrue(future.isDone());
    Assert.assertFalse(future.isCompletedExceptionally());
    List<Greeting> greetingList = future.get();
    Assert.assertTrue(greetingList.size() >= 2);
    for (Greeting greeting :greetingList)
    {
      Assert.assertTrue(greeting.getMessage().contains("GetAll"));
    }
  }

  private Greeting getGreeting()
  {
    return getGreeting(MESSAGE);
  }

  private Greeting getGreeting(String message)
  {
    return new Greeting().setId(100L).setMessage(message).setTone(Tone.FRIENDLY);
  }
}
