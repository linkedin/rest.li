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


import com.linkedin.common.callback.Callback;
import com.linkedin.data.ByteString;
import com.linkedin.java.util.concurrent.Flow;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.Writer;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.streaming.FlowBridge;
import com.linkedin.restli.server.UnstructuredDataReactiveResult;
import com.linkedin.restli.server.annotations.CallbackParam;
import com.linkedin.restli.server.annotations.Key;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.resources.unstructuredData.UnstructuredDataAssociationResourceReactiveTemplate;

import static com.linkedin.restli.examples.greetings.server.GreetingUnstructuredDataUtils.MIME_TYPE;
import static com.linkedin.restli.examples.greetings.server.GreetingUnstructuredDataUtils.UNSTRUCTURED_DATA_BYTES;


/**
 * This resource models an association resource that reactively streams unstructured data response.
 *
 * For more comprehensive examples, look at {@link GreetingUnstructuredDataCollectionResourceReactive}
 */
@RestLiAssociation(
  name = "reactiveGreetingAssociationUnstructuredData",
  namespace = "com.linkedin.restli.examples.greetings.client",
  assocKeys = {
    @Key(name = "src", type = String.class),
    @Key(name = "dest", type = String.class)
  }
)
public class GreetingUnstructuredDataAssociationResourceReactive extends UnstructuredDataAssociationResourceReactiveTemplate
{
  @Override
  public void get(CompoundKey key, @CallbackParam Callback<UnstructuredDataReactiveResult> callback)
  {
    Writer writer = new ByteStringWriter(ByteString.copy(UNSTRUCTURED_DATA_BYTES));
    Flow.Publisher<ByteString> publisher = FlowBridge.toPublisher(writer);
    callback.onSuccess(new UnstructuredDataReactiveResult(publisher, MIME_TYPE));
  }
}