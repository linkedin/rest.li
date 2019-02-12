package com.linkedin.restli.server.twitter;

import com.linkedin.common.callback.Callback;
import com.linkedin.data.ByteString;
import com.linkedin.entitystream.EntityStreams;
import com.linkedin.entitystream.SingletonWriter;
import com.linkedin.entitystream.Writer;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.server.UnstructuredDataReactiveResult;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.CallbackParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.unstructuredData.UnstructuredDataCollectionResourceReactiveTemplate;

@RestLiCollection(name = "reactiveGreetingCollectionUnstructuredData", namespace = "com.linkedin.restli.server.twitter")
public class UnstructuredDataReactiveCollectionResource extends UnstructuredDataCollectionResourceReactiveTemplate<String>
{
  public static String MIME_TYPE = "text/csv";
  public static byte[] UNSTRUCTURED_DATA_BYTES = "hello world".getBytes();

  @Override
  public void get(String key, @CallbackParam Callback<UnstructuredDataReactiveResult> callback)
  {
    Writer<ByteString> writer = new SingletonWriter<>(ByteString.copy(UNSTRUCTURED_DATA_BYTES));
    UnstructuredDataReactiveResult result = new UnstructuredDataReactiveResult(EntityStreams.newEntityStream(writer), MIME_TYPE);
    callback.onSuccess(result);
  }

  @Override
  public void delete(String key, @CallbackParam Callback<UpdateResponse> callback)
  {
    callback.onSuccess(new UpdateResponse(HttpStatus.S_200_OK));
  }
}