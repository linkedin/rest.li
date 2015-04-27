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

package com.linkedin.restli.examples;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.Tasks;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.restli.client.ParSeqRestClient;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.examples.greetings.client.ActionsBuilders;
import com.linkedin.restli.examples.greetings.client.ActionsRequestBuilders;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Integration test with actual usage of {@link ParSeqRestClient}.
 *
 * @author jnwang
 */
public class TestParSeqRestClient extends RestLiIntegrationTest
{
  private Engine                        _engine;
  private ScheduledExecutorService      _scheduler;
  private ParSeqRestClient              _restClient;

  @BeforeClass
  public void setUp() throws Exception
  {
    super.init();
    final int numCores = Runtime.getRuntime().availableProcessors();
    _scheduler = Executors.newScheduledThreadPool(numCores + 1);
    _engine = new EngineBuilder()
        .setTaskExecutor(_scheduler)
        .setTimerScheduler(_scheduler)
        .build();
    _restClient = new ParSeqRestClient(getClient());
  }

  @AfterClass
  public void tearDown() throws Exception
  {
    _engine.shutdown();
    _scheduler.shutdown();

    super.shutdown();
  }

  /**
   * Request that should succeed, using promise
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testPromise(RootBuilderWrapper<?, ?> builders) throws InterruptedException
  {
    final Request<String> req =
        builders.<String>action("Parseq").setActionParam("A", 5).setActionParam("B", "yay").setActionParam("C", false).build();
    final Promise<Response<String>> promise = _restClient.sendRequest(req);
    promise.await();
    Assert.assertFalse(promise.isFailed());
    final Response<String> response = promise.get();
    Assert.assertEquals("101 YAY false", response.getEntity());
  }

  /**
   * Request that should succeed, using task
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testTask(RootBuilderWrapper<?, ?> builders) throws InterruptedException
  {
    final Request<String> req =
        builders.<String>action("Parseq").setActionParam("A", 5).setActionParam("B", "yay").setActionParam("C", false).build();
    final Task<Response<String>> task = _restClient.createTask(req);

    _engine.run(task);
    task.await();

    Assert.assertFalse(task.isFailed());
    final Response<String> response = task.get();
    Assert.assertEquals("101 YAY false", response.getEntity());
  }

  /**
   * Multiple requests that should succeed, using task
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testMultipleTask(RootBuilderWrapper<?, ?> builders) throws InterruptedException
  {
    final Request<String> req1 =
        builders.<String>action("Parseq").setActionParam("A", 5).setActionParam("B", "yay").setActionParam("C", false).build();
    final Task<Response<String>> task1 = _restClient.createTask(req1);
    final Request<String> req2 =
        builders.<String>action("Parseq").setActionParam("A", 6).setActionParam("B", "wohoo").setActionParam("C", true).build();
    final Task<Response<String>> task2 = _restClient.createTask(req2);
    final Request<String> req3 =
        builders.<String>action("Parseq").setActionParam("A", 7).setActionParam("B", "rawr").setActionParam("C", false).build();
    final Task<Response<String>> task3 = _restClient.createTask(req3);

    final Task<?> master = Tasks.par(task1, task2, task3);
    _engine.run(master);
    master.await();

    Assert.assertFalse(master.isFailed());
    Assert.assertEquals("101 YAY false", task1.get().getEntity());
    Assert.assertEquals("110 WOHOO true", task2.get().getEntity());
    Assert.assertEquals("111 RAWR false", task3.get().getEntity());
  }

  /**
   * Request that should fail, using promise
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testFailPromise(RootBuilderWrapper<?, ?> builders) throws InterruptedException
  {
    final Request<Void> req = builders.<Void>action("FailPromiseCall").build();
    final Promise<Response<Void>> promise = _restClient.sendRequest(req);
    promise.await();
    Assert.assertTrue(promise.isFailed());
    final Throwable t = promise.getError();
    Assert.assertTrue(t instanceof RestLiResponseException);
  }

  /**
   * Request that should fail, using task
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testFailTask(RootBuilderWrapper<?, ?> builders) throws InterruptedException
  {
    final Request<Void> req = builders.<Void>action("FailPromiseCall").build();
    final Task<Response<Void>> task = _restClient.createTask(req);

    _engine.run(task);
    task.await();

    Assert.assertTrue(task.isFailed());
    final Throwable t = task.getError();
    Assert.assertTrue(t instanceof RestLiResponseException);
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  private static Object[][] requestBuilderDataProvider()
  {
    return new Object[][]
      {
        { new RootBuilderWrapper<Object, RecordTemplate>(new ActionsBuilders()) },
        { new RootBuilderWrapper<Object, RecordTemplate>(new ActionsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
        { new RootBuilderWrapper<Object, RecordTemplate>(new ActionsRequestBuilders()) },
        { new RootBuilderWrapper<Object, RecordTemplate>(new ActionsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }
      };
  }
}
