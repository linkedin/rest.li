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


import com.linkedin.data.ByteString;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.message.stream.entitystream.Writer;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.server.ReactiveDataWriter;
import com.linkedin.restli.server.RestLiResponseDataException;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.unstructuredData.UnstructuredDataCollectionResourceReactiveTemplate;

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
  public ReactiveDataWriter get(String key)
  {
    /*
     * In these examples, the writer is simply backed by a static data source. However, in real-life use cases, the writer
     * is typically backed by a "reactive"-style data source that supports fetching the data partially such as FileInputStream
     */
    switch (key)
    {
      case "good":
        ByteStringWriter goodWriter = new ByteStringWriter(ByteString.copy(UNSTRUCTURED_DATA_BYTES));
        ReactiveDataWriter goodReactiveWriter = new ReactiveDataWriter(goodWriter);
        goodReactiveWriter.setContentType(MIME_TYPE);
        return goodReactiveWriter;
      case "goodMultiWrites":
        MultiByteStringWriter multiWriter = new MultiByteStringWriter(ByteString.copy(UNSTRUCTURED_DATA_BYTES));
        ReactiveDataWriter reactiveWriter = new ReactiveDataWriter(multiWriter);
        reactiveWriter.setContentType(MIME_TYPE);
        return reactiveWriter;
      case "goodInline":
        ByteStringWriter goodInlineWriter = new ByteStringWriter(ByteString.copy(UNSTRUCTURED_DATA_BYTES));
        getContext().setResponseHeader(HEADER_CONTENT_DISPOSITION, CONTENT_DISPOSITION_VALUE);
        ReactiveDataWriter goodReactiveInlineWriter = new ReactiveDataWriter(goodInlineWriter);
        goodReactiveInlineWriter.setContentType(MIME_TYPE);
        return goodReactiveInlineWriter;
      case "missingContentType":
        ByteStringWriter fineWriter = new ByteStringWriter(ByteString.copy(UNSTRUCTURED_DATA_BYTES));
        ReactiveDataWriter missingContentTypeWriter = new ReactiveDataWriter(fineWriter);
        return missingContentTypeWriter;
      case "bad":
        BadWriter badWriter = new BadWriter();
        ReactiveDataWriter badReactiveWriter = new ReactiveDataWriter(badWriter);
        badReactiveWriter.setContentType(MIME_TYPE);
        return badReactiveWriter;
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
  private class BadWriter implements Writer
  {
    private WriteHandle _wh;

    @Override
    public void onInit(WriteHandle wh)
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