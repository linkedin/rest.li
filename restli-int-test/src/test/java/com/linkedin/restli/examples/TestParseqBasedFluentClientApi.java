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
import com.linkedin.restli.common.CreateIdEntityStatus;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.IdEntityResponse;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.CreateGreeting;
import com.linkedin.restli.examples.greetings.client.Greetings;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
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

  private Greeting getGreeting()
  {
    return getGreeting(MESSAGE);
  }

  private Greeting getGreeting(String message)
  {
    return new Greeting().setId(100L).setMessage(message).setTone(Tone.FRIENDLY);
  }
}
