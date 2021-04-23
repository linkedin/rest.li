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
import com.linkedin.parseq.trace.TraceUtil;
import com.linkedin.restli.client.ExecutionGroup;
import com.linkedin.restli.client.ParSeqBasedCompletionStage;
import com.linkedin.restli.client.ParSeqRestliClient;
import com.linkedin.restli.client.ParSeqRestliClientBuilder;
import com.linkedin.restli.client.ParSeqRestliClientConfig;
import com.linkedin.restli.client.ParSeqRestliClientConfigBuilder;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.GreetingsFluentClient;
import com.linkedin.restli.server.validation.RestLiValidationFilter;
import com.linkedin.util.clock.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class TestParSeqBasedFluentClientApiWithExecutionGroup extends RestLiIntegrationTest
{
  public static final String MESSAGE = "Create a new greeting";
  ParSeqUnitTestHelper _parSeqUnitTestHelper;
  ParSeqRestliClient _parSeqRestliClient;

  @BeforeClass
  void setUp() throws Exception
  {
    super.init(Arrays.asList(new RestLiValidationFilter()));
    ParSeqRestliClientConfig config = new ParSeqRestliClientConfigBuilder()
        .addBatchingEnabled("*.*/*.*", Boolean.TRUE)
        .addMaxBatchSize("*.*/*.*", 3)
        .build();
    BatchingSupport batchingSupport = new BatchingSupport();
    _parSeqUnitTestHelper = new ParSeqUnitTestHelper(engineBuilder -> {
      engineBuilder.setPlanDeactivationListener(batchingSupport);
    });
    _parSeqUnitTestHelper.setUp();
    _parSeqRestliClient = new ParSeqRestliClientBuilder()
        .setBatchingSupport(batchingSupport) // RestClient Registered Strategy
        .setClient(getClient())
        .setConfig(config)
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

  /**
    This test is to test if the batch method has been triggered.

    Note: Currently ParSeqRestClient only support "batch" for gets.
  */
  @Test public void testBatchGet() throws Exception
  {
    // Test 3+1 = 4 calls using the "runBatchOnClient" method in the fluent Client
    GreetingsFluentClient greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    final List<CompletionStage<Greeting>> results = new ArrayList<>(4);
    greetings.runBatchOnClient(
        () -> {
          try {
            results.add(greetings.get(1L));
            results.add(greetings.get(1L));
            results.add(greetings.get(1L));
            results.add(greetings.get(1L));
          } catch (Exception e)
          {
            throw new RuntimeException();
          }
        }
    );
    results.get(0).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    assert(results.get(0) instanceof ParSeqBasedCompletionStage);
    Task<Greeting> t = ((ParSeqBasedCompletionStage<Greeting>) results.get(0)).getTask();
    System.out.println(TraceUtil.getJsonTrace(t.getTrace()));
    Assert.assertTrue(hasTask("greetings batch_get(reqs: 1, ids: 3)",t.getTrace()));

    // Test 3+1 = 4 calls generating a new ExecutionGroup and explicitly call the methods with ExecutionGroup
    results.clear();
    ExecutionGroup eg = greetings.generateExecutionGroup();
    results.add(greetings.get(1L, eg));
    results.add(greetings.get(1L, eg));
    results.add(greetings.get(1L, eg));
    results.add(greetings.get(1L, eg));
    assert(results.get(0) instanceof ParSeqBasedCompletionStage);
    t = ((ParSeqBasedCompletionStage<Greeting>) results.get(0)).getTask();
    Assert.assertFalse(t.isDone());
    Assert.assertFalse(hasTask("greetings batch_get(reqs: 1, ids: 3)",t.getTrace()));
    eg.execute();
    for (CompletionStage<Greeting> stage : results)
    {
      stage.toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    }
    Assert.assertTrue(hasTask("greetings batch_get(reqs: 1, ids: 3)",t.getTrace()));

    // Test 2( less than 3 ) calls using client's batchOn
    results.clear();
    greetings.runBatchOnClient(
        () -> {
          try {
            results.add(greetings.get(1L));
            results.add(greetings.get(1L));
          } catch (Exception e)
          {
            throw new RuntimeException();
          }
        }
    );
    results.get(0).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    assert(results.get(0) instanceof ParSeqBasedCompletionStage);
    t = ((ParSeqBasedCompletionStage<Greeting>) results.get(0)).getTask();
    Assert.assertTrue(hasTask("greetings batch_get(reqs: 1, ids: 2)",t.getTrace()));

    // Test 2( less than 3 ) calls execution group
    results.clear();
    eg = greetings.generateExecutionGroup();
    results.add(greetings.get(1L, eg));
    results.add(greetings.get(1L, eg));
    assert(results.get(0) instanceof ParSeqBasedCompletionStage);
    t = ((ParSeqBasedCompletionStage<Greeting>) results.get(0)).getTask();
    Assert.assertFalse(t.isDone());
    Assert.assertFalse(hasTask("greetings batch_get(reqs: 1, ids: 2)",t.getTrace()));
    eg.execute();
    for (CompletionStage<Greeting> stage : results)
    {
      stage.toCompletableFuture().get(5000, TimeUnit.MILLISECONDS);
    }
    Assert.assertTrue(hasTask("greetings batch_get(reqs: 1, ids: 2)",t.getTrace()));


    results.clear();
    try
    {
      greetings.runBatchOnClient(
          () -> {
            try {
              results.add(greetings.get(1L));
              results.add(greetings.get(1L));
              results.get(0).toCompletableFuture().get(1000, TimeUnit.MILLISECONDS);
            } catch (Exception e)
            {
              throw new RuntimeException(e);
            }
          }
      );
      Assert.fail("Should fail due: the CompletionStage cannot be read ");
    }
    catch (Exception e)
    {
      Assert.assertTrue(e.getCause() instanceof  TimeoutException);
    }
  }

  protected boolean hasTask(final String name, final Trace trace) {
    return trace.getTraceMap().values().stream().anyMatch(shallowTrace -> shallowTrace.getName().equals(name));
  }
}