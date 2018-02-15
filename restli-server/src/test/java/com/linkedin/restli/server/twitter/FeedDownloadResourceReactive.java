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

package com.linkedin.restli.server.twitter;


import com.linkedin.common.callback.Callback;
import com.linkedin.data.ByteString;
import com.linkedin.data.ByteChunkWriter;
import com.linkedin.java.util.concurrent.Flow;
import com.linkedin.entitystream.adapter.FlowAdapters;
import com.linkedin.restli.server.UnstructuredDataReactiveResult;
import com.linkedin.restli.server.annotations.CallbackParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.unstructuredData.UnstructuredDataCollectionResourceReactiveTemplate;


/**
 * Resource that serve feed downloads via reactive streaming.
 */
@RestLiCollection(name = "reactiveFeedDownloads", keyName = "feedId")
public class FeedDownloadResourceReactive extends UnstructuredDataCollectionResourceReactiveTemplate<Long>
{
  public static final String CONTENT_TYPE = "text/plain";
  public static final String CONTENT = "hello world";

  @Override
  public void get(Long key, @CallbackParam Callback<UnstructuredDataReactiveResult> callback)
  {
    ByteChunkWriter writer = new ByteChunkWriter(CONTENT, 2);
    Flow.Publisher<ByteString> publisher = FlowAdapters.toPublisher(writer);
    callback.onSuccess(new UnstructuredDataReactiveResult(publisher, CONTENT_TYPE));
  }
}
