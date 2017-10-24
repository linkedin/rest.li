/*
 * Copyright (c) 2017 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linkedin.restli.examples.greetings.server;


import com.linkedin.parseq.Task;
import com.linkedin.restli.server.UnstructuredDataWriter;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.UnstructuredDataWriterParam;
import com.linkedin.restli.server.resources.unstructuredData.UnstructuredDataCollectionResourceTaskTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


@RestLiCollection(name = "greetingCollectionUnstructuredDataTask", namespace = "com.linkedin.restli.examples.greetings.client")
public class GreetingUnstructuredDataCollectionResourceTask extends UnstructuredDataCollectionResourceTaskTemplate<String>
{
  private static final ScheduledExecutorService _scheduler = Executors.newScheduledThreadPool(1);

  @Override
  public Task<Void> get(String key, final @UnstructuredDataWriterParam UnstructuredDataWriter writer)
  {
    return Task.blocking("fetchBytes", () ->
    {
      GreetingUnstructuredDataUtils.respondGoodUnstructuredData(writer);
      return null;
    }, _scheduler);
  }
}