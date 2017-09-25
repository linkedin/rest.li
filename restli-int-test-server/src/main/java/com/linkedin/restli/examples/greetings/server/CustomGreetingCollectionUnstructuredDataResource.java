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


import com.linkedin.restli.server.UnstructuredDataWriter;
import com.linkedin.restli.server.annotations.UnstructuredDataWriterParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.ResourceContextHolder;
import com.linkedin.restli.server.resources.unstructuredData.KeyUnstructuredDataResource;

import static com.linkedin.restli.examples.greetings.server.GreetingUnstructuredDataUtils.respondGoodUnstructuredData;


/**
 * This resource models a (very simple) custom collection resource that produces unstructured data as results.
 */
@RestLiCollection(name = "customGreetingCollectionUnstructuredData", namespace = "com.linkedin.restli.examples.greetings.client")
public class CustomGreetingCollectionUnstructuredDataResource extends ResourceContextHolder implements KeyUnstructuredDataResource<String>
{
  @RestMethod.Get
  public void get(String key, @UnstructuredDataWriterParam UnstructuredDataWriter writer)
  {
    respondGoodUnstructuredData(writer);
  }
}