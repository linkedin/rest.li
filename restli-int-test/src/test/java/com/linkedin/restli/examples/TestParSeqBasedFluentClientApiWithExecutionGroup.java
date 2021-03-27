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
import com.linkedin.parseq.batching.BatchingSupport;
import com.linkedin.restli.client.ExecutionGroup;
import com.linkedin.restli.client.ParSeqRestliClient;
import com.linkedin.restli.client.ParSeqRestliClientBuilder;
import com.linkedin.restli.client.ParSeqRestliClientConfig;
import com.linkedin.restli.client.ParSeqRestliClientConfigBuilder;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.GreetingsFluentClient;
import com.linkedin.restli.server.validation.RestLiValidationFilter;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
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

  @BeforeClass
  void setUp() throws Exception
  {
    super.init(Arrays.asList(new RestLiValidationFilter()));
    ParSeqRestliClientConfig config = new ParSeqRestliClientConfigBuilder()
        .addBatchingEnabled("withBatching.*/*.*", true)
        .addMaxBatchSize("withBatching.*/*.*", 2)
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

  @Test public void testBatchOn() throws Exception
  {
    GreetingsFluentClient greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    new ExecutionGroup(_parSeqUnitTestHelper.getEngine()).batchOn(
        () -> {
          try {
            greetings.get(1L).toCompletableFuture();
            greetings.get(1L).toCompletableFuture();
            greetings.get(1L).toCompletableFuture();
          } catch (Exception e)
          {
            throw new RuntimeException();
          }
        },
        greetings
    );
  }

  @Test public void testUsingExecutionGroupInstance() throws Exception
  {
  }
}
