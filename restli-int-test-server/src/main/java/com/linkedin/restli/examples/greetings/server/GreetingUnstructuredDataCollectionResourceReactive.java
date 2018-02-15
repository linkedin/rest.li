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
import com.linkedin.data.ByteChunkWriter;
import com.linkedin.java.util.concurrent.Flow;
import com.linkedin.entitystream.SingletonWriter;
import com.linkedin.entitystream.WriteHandle;
import com.linkedin.entitystream.Writer;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.entitystream.adapter.FlowAdapters;
import com.linkedin.restli.server.UnstructuredDataReactiveResult;
import com.linkedin.restli.server.RestLiResponseDataException;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.annotations.CallbackParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.unstructuredData.UnstructuredDataCollectionResourceReactiveTemplate;

import javax.naming.NoPermissionException;

import static com.linkedin.restli.common.RestConstants.HEADER_CONTENT_DISPOSITION;
import static com.linkedin.restli.examples.greetings.server.GreetingUnstructuredDataUtils.CONTENT_DISPOSITION_VALUE;
import static com.linkedin.restli.examples.greetings.server.GreetingUnstructuredDataUtils.MIME_TYPE;
import static com.linkedin.restli.examples.greetings.server.GreetingUnstructuredDataUtils.UNSTRUCTURED_DATA_BYTES;


/**
 * This resource models a collection resource that reactively streams unstructured data response
 */
@RestLiCollection(name = "reactiveGreetingCollectionUnstructuredData", namespace = "com.linkedin.restli.examples.greetings.client")
public class GreetingUnstructuredDataCollectionResourceReactive extends UnstructuredDataCollectionResourceReactiveTemplate<String>
{
  @Override
  public void get(String key, @CallbackParam Callback<UnstructuredDataReactiveResult> callback)
  {
    if (key.equals("callbackError"))
    {
      callback.onError(new NoPermissionException("missing access permission"));
      return;
    }

    Writer<ByteString> writer = chooseGreetingWriter(key);
    Flow.Publisher<ByteString> publisher = FlowAdapters.toPublisher(writer);

    String contentType;
    if (key.equals("goodNullContentType"))
    {
      contentType = null;
    }
    else
    {
      contentType = MIME_TYPE;
    }
    UnstructuredDataReactiveResult result = new UnstructuredDataReactiveResult(publisher, contentType);
    callback.onSuccess(result);
  }

  /**
   * Choose a writer based on the test key
   */
  private Writer<ByteString> chooseGreetingWriter(String key)
  {
    switch (key)
    {
      case "good":
      case "goodNullContentType":
        return new SingletonWriter<>(ByteString.copy(UNSTRUCTURED_DATA_BYTES));
      case "goodMultiWrites":
        return new ByteChunkWriter(UNSTRUCTURED_DATA_BYTES, 2);
      case "goodInline":
        getContext().setResponseHeader(HEADER_CONTENT_DISPOSITION, CONTENT_DISPOSITION_VALUE);
        return new SingletonWriter<>(ByteString.copy(UNSTRUCTURED_DATA_BYTES));
      case "bad":
        return new BadWriter();
      case "exception":
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, "internal service exception");
      default:
        throw new RestLiServiceException(HttpStatus.S_503_SERVICE_UNAVAILABLE,
                                         "unexpected unstructured data key, something wrong with the test.");
    }
  }

  /**
   * A writer that fail to read data from source.
   */
  private class BadWriter implements Writer<ByteString>
  {
    private WriteHandle<? super ByteString> _wh;

    @Override
    public void onInit(WriteHandle<? super ByteString> wh)
    {
      _wh = wh;
    }

    @Override
    public void onWritePossible()
    {
      _wh.error(new RestLiResponseDataException("Failed to read data"));
    }

    @Override
    public void onAbort(Throwable ex)
    {
    }
  }
}