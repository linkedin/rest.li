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


import com.linkedin.data.transform.DataProcessingException;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.parseq.promise.Promises;
import com.linkedin.parseq.promise.SettablePromise;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.TwoPartKey;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchResult;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.annotations.TestMethod;
import com.linkedin.restli.server.resources.KeyValueResource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@RestLiCollection(name="annotatedComplexKeys", namespace = "com.linkedin.restli.examples.greetings.client", keyName="annotatedComplexKeyId")
public class AnnotatedComplexKeysResource
    implements KeyValueResource<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message>
{
  private static ComplexKeysDataProvider _dataProvider = new ComplexKeysDataProvider();
  private static final ScheduledExecutorService _scheduler = Executors.newScheduledThreadPool(1);

  private static final int DELAY = 100;

  @RestMethod.Get
  public Promise<Message> get(final ComplexResourceKey<TwoPartKey, TwoPartKey> key)
  {
    final SettablePromise<Message> result = Promises.settable();
    final Runnable requestHandler = new Runnable() {
      public void run()
      {
        result.done(_dataProvider.get(key));
      }
    };

    _scheduler.schedule(requestHandler, DELAY, TimeUnit.MILLISECONDS);

    return result;
  }

  @RestMethod.Create
  public Promise<CreateResponse> create(final Message message)
  {
    final SettablePromise<CreateResponse> result = Promises.settable();
    final Runnable requestHandler = new Runnable() {
      public void run()
      {
        ComplexResourceKey<TwoPartKey, TwoPartKey> key = _dataProvider.create(message);
        result.done(new CreateResponse(key));
      }
    };

    _scheduler.schedule(requestHandler, DELAY, TimeUnit.MILLISECONDS);

    return result;
  }

  @RestMethod.PartialUpdate
  public Promise<UpdateResponse> update(final ComplexResourceKey<TwoPartKey, TwoPartKey> key,
                               final PatchRequest<Message> patch)
  {
    final SettablePromise<UpdateResponse> result = Promises.settable();
    final Runnable requestHandler = new Runnable() {
      public void run()
      {
        try
        {
          _dataProvider.partialUpdate(key, patch);
          result.done(new UpdateResponse(HttpStatus.S_204_NO_CONTENT));
        }
        catch (DataProcessingException e)
        {
          result.done(new UpdateResponse(HttpStatus.S_400_BAD_REQUEST));
        }
      }
    };

    _scheduler.schedule(requestHandler, DELAY, TimeUnit.MILLISECONDS);

    return result;
  }

  /**
   * Example javadoc
   */
  @TestMethod(doc = "For integration tests only.")
  @Finder("prefix")
  public Promise<List<Message>> prefix(@QueryParam("prefix")final String prefix)
  {
    final SettablePromise<List<Message>> result = Promises.settable();
    final Runnable requestHandler = new Runnable() {
      public void run()
      {
        result.done(_dataProvider.findByPrefix(prefix));
      }
    };

    _scheduler.schedule(requestHandler, DELAY, TimeUnit.MILLISECONDS);

    return result;
  }

  @RestMethod.BatchGet
  public Promise<BatchResult<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message>> batchGet(
      final Set<ComplexResourceKey<TwoPartKey, TwoPartKey>> ids)
  {
    final SettablePromise<BatchResult<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message>> result = Promises.settable();
    final Runnable requestHandler = new Runnable() {
      public void run()
      {
        result.done(_dataProvider.batchGet(ids));
      }
    };

    _scheduler.schedule(requestHandler, DELAY, TimeUnit.MILLISECONDS);

    return result;
  }

  @RestMethod.BatchUpdate
  public BatchUpdateResult<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchUpdate(
      final BatchUpdateRequest<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> entities)
  {
    return _dataProvider.batchUpdate(entities);
  }

  @RestMethod.BatchPartialUpdate
  public BatchUpdateResult<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchUpdate(
      final BatchPatchRequest<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> patches)
  {
    return _dataProvider.batchUpdate(patches);
  }

  @RestMethod.BatchDelete
  public BatchUpdateResult<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> batchDelete(
      final BatchDeleteRequest<ComplexResourceKey<TwoPartKey, TwoPartKey>, Message> ids)
  {
    return _dataProvider.batchDelete(ids);
  }
}