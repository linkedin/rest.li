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

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.Tasks;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.ActionRequest;
import com.linkedin.restli.client.ParSeqRestClient;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.examples.greetings.client.ActionsBuilders;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Integration test with actual usage of {@link ParSeqRestClient}.
 *
 * @author jnwang
 */
public class TestParSeqRestClient extends RestLiIntegrationTest
{
  private static final Client           CLIENT           =
                                                             new TransportClientAdapter(new HttpClientFactory().getClient(Collections.<String, String> emptyMap()));
  private static final String           URI_PREFIX       = "http://localhost:1338/";
  private static final ParSeqRestClient REST_CLIENT      =
                                                             new ParSeqRestClient(new RestClient(CLIENT,
                                                                                                 URI_PREFIX));
  private static final ActionsBuilders  ACTIONS_BUILDERS = new ActionsBuilders();

  private Engine                        _engine;
  private ScheduledExecutorService      _scheduler;

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
  @Test
  public void testPromise() throws InterruptedException
  {
    final ActionRequest<String> req =
        ACTIONS_BUILDERS.actionParseq().paramA(5).paramB("yay").paramC(false).build();
    final Promise<Response<String>> promise = REST_CLIENT.sendRequest(req);
    promise.await();
    Assert.assertFalse(promise.isFailed());
    final Response<String> response = promise.get();
    Assert.assertEquals("101 YAY false", response.getEntity());
  }

  /**
   * Request that should succeed, using task
   */
  @Test
  public void testTask() throws InterruptedException
  {
    final ActionRequest<String> req =
        ACTIONS_BUILDERS.actionParseq().paramA(5).paramB("yay").paramC(false).build();
    final Task<Response<String>> task = REST_CLIENT.createTask(req);

    _engine.run(task);
    task.await();

    Assert.assertFalse(task.isFailed());
    final Response<String> response = task.get();
    Assert.assertEquals("101 YAY false", response.getEntity());
  }

  /**
   * Multiple requests that should succeed, using task
   */
  @Test
  public void testMultipleTask() throws InterruptedException
  {
    final ActionRequest<String> req1 =
        ACTIONS_BUILDERS.actionParseq().paramA(5).paramB("yay").paramC(false).build();
    final Task<Response<String>> task1 = REST_CLIENT.createTask(req1);
    final ActionRequest<String> req2 =
        ACTIONS_BUILDERS.actionParseq().paramA(6).paramB("wohoo").paramC(true).build();
    final Task<Response<String>> task2 = REST_CLIENT.createTask(req2);
    final ActionRequest<String> req3 =
        ACTIONS_BUILDERS.actionParseq().paramA(7).paramB("rawr").paramC(false).build();
    final Task<Response<String>> task3 = REST_CLIENT.createTask(req3);

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
  @Test
  public void testFailPromise() throws InterruptedException
  {
    final ActionRequest<Void> req = ACTIONS_BUILDERS.actionFailPromiseCall().build();
    final Promise<Response<Void>> promise = REST_CLIENT.sendRequest(req);
    promise.await();
    Assert.assertTrue(promise.isFailed());
    final Throwable t = promise.getError();
    Assert.assertTrue(t instanceof RestLiResponseException);
  }

  /**
   * Request that should fail, using task
   */
  @Test
  public void testFailTask() throws InterruptedException
  {
    final ActionRequest<Void> req = ACTIONS_BUILDERS.actionFailPromiseCall().build();
    final Task<Response<Void>> task = REST_CLIENT.createTask(req);

    _engine.run(task);
    task.await();

    Assert.assertTrue(task.isFailed());
    final Throwable t = task.getError();
    Assert.assertTrue(t instanceof RestLiResponseException);
  }
}
