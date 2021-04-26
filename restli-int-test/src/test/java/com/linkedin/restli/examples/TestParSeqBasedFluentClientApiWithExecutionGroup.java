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
import com.linkedin.parseq.Task;
import com.linkedin.parseq.batching.BatchingSupport;
import com.linkedin.parseq.trace.Trace;
import com.linkedin.restli.client.ExecutionGroup;
import com.linkedin.restli.client.ParSeqBasedCompletionStage;
import com.linkedin.restli.client.ParSeqRestliClient;
import com.linkedin.restli.client.ParSeqRestliClientBuilder;
import com.linkedin.restli.client.ParSeqRestliClientConfig;
import com.linkedin.restli.client.ParSeqRestliClientConfigBuilder;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.GreetingsFluentClient;
import com.linkedin.restli.server.validation.RestLiValidationFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class TestParSeqBasedFluentClientApiWithExecutionGroup extends RestLiIntegrationTest
{
  public static final String MESSAGE = "Create a new greeting";
  ParSeqUnitTestHelper _parSeqUnitTestHelper;
  ParSeqRestliClient _parSeqRestliClient;
  private final int TEST_BATCH_SIZE = 3;
  private final int OVER_BATCH_SIZE_CALLS = TEST_BATCH_SIZE + 1;
  private final int UNDER_BATCH_SIZE_CALLS = TEST_BATCH_SIZE - 1;

  @BeforeClass
  void setUp() throws Exception
  {
    super.init(Arrays.asList(new RestLiValidationFilter()));
    ParSeqRestliClientConfig config = new ParSeqRestliClientConfigBuilder().addBatchingEnabled("*.*/*.*", Boolean.TRUE)
        .addMaxBatchSize("*.*/*.*", TEST_BATCH_SIZE)
        .build();
    BatchingSupport batchingSupport = new BatchingSupport();
    _parSeqUnitTestHelper = new ParSeqUnitTestHelper(engineBuilder ->
    {
      engineBuilder.setPlanDeactivationListener(batchingSupport);
    });
    _parSeqUnitTestHelper.setUp();
    _parSeqRestliClient =
        new ParSeqRestliClientBuilder().setBatchingSupport(batchingSupport) // RestClient Registered Strategy
            .setClient(getClient()).setConfig(config).build();
  }

  @AfterClass
  void tearDown() throws Exception
  {
    if (_parSeqUnitTestHelper != null)
    {
      _parSeqUnitTestHelper.tearDown();
    } else
    {
      throw new RuntimeException(
          "Tried to shut down Engine but it either has not even been created or has " + "already been shut down");
    }
    super.shutdown();
  }

  /**
   This test is to test if the batch method has been triggered.

   Note: Currently ParSeqRestClient only support "batch" for gets.
   */
  @Test
  public void testBatchGet() throws Exception
  {
    // Test 3+1 = 4 calls using the "runBatchOnClient" method in the fluent Client
    GreetingsFluentClient greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    final List<CompletionStage<Greeting>> results = new ArrayList<>(TEST_BATCH_SIZE + 1);
    greetings.runBatchOnClient(() ->
    {
      try
      {
        for (int i = 0; i < OVER_BATCH_SIZE_CALLS; i++)
        {
          results.add(greetings.get((long) (i + 1)));
        }
      } catch (Exception e)
      {
        throw new RuntimeException();
      }
    });
    for (CompletionStage<Greeting> stage : results)
    {
      stage.toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    }
    assert (results.get(0) instanceof ParSeqBasedCompletionStage);
    Task<Greeting> t = ((ParSeqBasedCompletionStage<Greeting>) results.get(0)).getTask();
    Assert.assertTrue(hasTask(String.format("greetings batch_get(reqs: %s, ids: %s)", TEST_BATCH_SIZE, TEST_BATCH_SIZE),
        t.getTrace()));
    Assert.assertEquals(findTaskOccurrenceInTrace("greetings batch_get", t.getTrace()),
        OVER_BATCH_SIZE_CALLS / TEST_BATCH_SIZE);
    Assert.assertEquals(findTaskOccurrenceInTrace("greetings get", t.getTrace()), OVER_BATCH_SIZE_CALLS + 1);

    // Test 3+1 = 4 calls generating a new ExecutionGroup and explicitly call the methods with ExecutionGroup
    results.clear();
    ExecutionGroup eg = greetings.generateExecutionGroup();
    for (int i = 0; i < OVER_BATCH_SIZE_CALLS; i++)
    {
      results.add(greetings.get((long) (i + 1), eg));
    }
    assert (results.get(0) instanceof ParSeqBasedCompletionStage);
    t = ((ParSeqBasedCompletionStage<Greeting>) results.get(0)).getTask();
    Assert.assertFalse(t.isDone());
    Assert.assertFalse(
        hasTask(String.format("greetings batch_get(reqs: %s, ids: %s)", TEST_BATCH_SIZE, TEST_BATCH_SIZE),
            t.getTrace()));
    eg.execute();
    for (CompletionStage<Greeting> stage : results)
    {
      stage.toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    }
    Assert.assertTrue(hasTask(String.format("greetings batch_get(reqs: %s, ids: %s)", TEST_BATCH_SIZE, TEST_BATCH_SIZE),
        t.getTrace()));
    Assert.assertEquals(findTaskOccurrenceInTrace("greetings batch_get", t.getTrace()),
        OVER_BATCH_SIZE_CALLS / TEST_BATCH_SIZE);
    Assert.assertEquals(findTaskOccurrenceInTrace("greetings get", t.getTrace()), OVER_BATCH_SIZE_CALLS + 1);

    // Test 2( less than 3 ) calls using client's batchOn
    results.clear();
    greetings.runBatchOnClient(() ->
    {
      try
      {
        for (int i = 0; i < UNDER_BATCH_SIZE_CALLS; i++)
        {
          results.add(greetings.get((long) (i + 1)));
        }
      } catch (Exception e)
      {
        throw new RuntimeException();
      }
    });
    for (CompletionStage<Greeting> stage : results)
    {
      stage.toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    }
    assert (results.get(0) instanceof ParSeqBasedCompletionStage);
    t = ((ParSeqBasedCompletionStage<Greeting>) results.get(0)).getTask();
    Assert.assertTrue(
        hasTask(String.format("greetings batch_get(reqs: %s, ids: %s)", TEST_BATCH_SIZE - 1, TEST_BATCH_SIZE - 1),
            t.getTrace()));

    // Test 2( less than 3 ) calls execution group
    results.clear();
    eg = greetings.generateExecutionGroup();
    for (int i = 0; i < UNDER_BATCH_SIZE_CALLS; i++)
    {
      results.add(greetings.get((long) (i + 1), eg));
    }
    assert (results.get(0) instanceof ParSeqBasedCompletionStage);
    t = ((ParSeqBasedCompletionStage<Greeting>) results.get(0)).getTask();
    Assert.assertFalse(t.isDone());
    Assert.assertFalse(
        hasTask(String.format("greetings batch_get(reqs: %s, ids: %s)", TEST_BATCH_SIZE - 1, TEST_BATCH_SIZE - 1),
            t.getTrace()));
    eg.execute();
    for (CompletionStage<Greeting> stage : results)
    {
      stage.toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    }
    Assert.assertTrue(
        hasTask(String.format("greetings batch_get(reqs: %s, ids: %s)", TEST_BATCH_SIZE - 1, TEST_BATCH_SIZE - 1),
            t.getTrace()));

    // Testing read the completion Stage within the runnable: Not allowed; should fail
    results.clear();
    try
    {
      greetings.runBatchOnClient(() ->
      {
        try
        {
          results.add(greetings.get(1L));
          results.get(0).toCompletableFuture().get(500, TimeUnit.MILLISECONDS);
        } catch (Exception e)
        {
          throw new RuntimeException(e);
        }
      });
      Assert.fail("Should fail: the CompletionStage cannot be read ");
    } catch (Exception e)
    {
      Assert.assertTrue(e.getCause() instanceof TimeoutException);
    }
  }

  protected boolean hasTask(final String name, final Trace trace)
  {
    return trace.getTraceMap().values().stream().anyMatch(shallowTrace -> shallowTrace.getName().equals(name));
  }

  protected int findTaskOccurrenceInTrace(final String name, final Trace trace)
  {
    return (int) trace.getTraceMap()
        .values()
        .stream()
        .filter(shallowTrace -> shallowTrace.getName().contains(name))
        .count();
  }
}