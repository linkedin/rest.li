/*
   Copyright (c) 2016 LinkedIn Corp.

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


import com.linkedin.common.callback.Callback;
import com.linkedin.data.ByteString;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.attachments.RestLiAttachmentDataSourceWriter;
import com.linkedin.restli.common.attachments.RestLiAttachmentReader;
import com.linkedin.restli.common.attachments.RestLiAttachmentReaderCallback;
import com.linkedin.restli.common.attachments.SingleRestLiAttachmentReaderCallback;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.RestLiResponseAttachments;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.CallbackParam;
import com.linkedin.restli.server.annotations.RestLiAttachmentsParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceAsyncTemplate;

import java.io.ByteArrayOutputStream;


/**
 * @author Karim Vidhani
 */
@RestLiCollection(name = "streamingGreetings", namespace = "com.linkedin.restli.examples.greetings.client")
public class StreamingGreetings extends CollectionResourceAsyncTemplate<Long, Greeting>
{
  private static byte[] greetingBytes = "BeginningBytes".getBytes();

  public StreamingGreetings()
  {
  }

  @Override
  public void get(Long key, @CallbackParam Callback<Greeting> callback)
  {
    if (getContext().responseAttachmentsSupported())
    {
      final GreetingWriter greetingWriter = new GreetingWriter(ByteString.copy(greetingBytes));
      final RestLiResponseAttachments streamingAttachments =
          new RestLiResponseAttachments.Builder().appendSingleAttachment(greetingWriter).build();
      getContext().setResponseAttachments(streamingAttachments);
      final String headerValue = getContext().getRequestHeaders().get("getHeader");
      getContext().setResponseHeader("getHeader", headerValue);
      callback.onSuccess(new Greeting().setMessage("Your greeting has an attachment since you were kind and "
                                                   + "decided you wanted to read it!").setId(key));
    }
    callback.onError(new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, "You must be able to receive attachments!"));
  }

  public void create(Greeting entity, @CallbackParam Callback<CreateResponse> callback,
                     @RestLiAttachmentsParam RestLiAttachmentReader attachmentReader)
  {
    if (attachmentReader != null)
    {
      final String headerValue = getContext().getRequestHeaders().get("createHeader");
      getContext().setResponseHeader("createHeader", headerValue);
      attachmentReader.registerAttachmentReaderCallback(new GreetingBlobReaderCallback(callback));
      return;
    }
    callback.onError(new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, "You must supply some attachments!"));
  }

  //The delete and update resource methods here are simply to show that although not typical, it is possible to return
  //attachments from DELETE, UPDATE, PARTIAL_UPDATE, BATCH_DELETE, BATCH_UPDATE, and BATCH_PARTIAL_UPDATE. For the sake of
  //brevity DELETE and UPDATE are used as examples.
  @Override
  public void delete(Long key, @CallbackParam Callback<UpdateResponse> callback)
  {
    respondWithResponseAttachment(callback);
  }

  @Override
  public void update(Long key, Greeting entity, @CallbackParam Callback<UpdateResponse> callback)
  {
    respondWithResponseAttachment(callback);
  }

  private void respondWithResponseAttachment(final Callback<UpdateResponse> callback)
  {
    if (getContext().responseAttachmentsSupported())
    {
      //Echo the bytes back from the header
      final String headerValue = getContext().getRequestHeaders().get("getHeader");
      final GreetingWriter greetingWriter = new GreetingWriter(ByteString.copy(headerValue.getBytes()));
      final RestLiResponseAttachments streamingAttachments =
          new RestLiResponseAttachments.Builder().appendSingleAttachment(greetingWriter).build();
      getContext().setResponseAttachments(streamingAttachments);
      callback.onSuccess(new UpdateResponse(HttpStatus.S_200_OK));
    }
    callback.onError(new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, "You must be able to receive attachments!"));
  }

  @Action(name = "actionNoAttachmentsAllowed")
  public int actionNoAttachmentsAllowed()
  {
    return 100;
  }

  @Action(name = "actionAttachmentsAllowedButDisliked")
  public boolean actionAttachmentsAllowedButDisliked(final @RestLiAttachmentsParam RestLiAttachmentReader attachmentReader)
  {
    //Verify that null was passed in by returning true;
    if (attachmentReader == null)
    {
      return true;
    }
    else
    {
      return false;
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  //For writing the response attachment
  private static class GreetingWriter extends ByteStringWriter implements RestLiAttachmentDataSourceWriter
  {
    private GreetingWriter(final ByteString content)
    {
      super(content);
    }

    @Override
    public String getAttachmentID()
    {
      return "12345";
    }
  }

  //For reading in the request attachment
  private static class GreetingBlobReaderCallback implements RestLiAttachmentReaderCallback
  {
    private final Callback<CreateResponse> _createResponseCallback;

    private GreetingBlobReaderCallback(final Callback<CreateResponse> createResponseCallback)
    {
      _createResponseCallback = createResponseCallback;
    }

    @Override
    public void onNewAttachment(RestLiAttachmentReader.SingleRestLiAttachmentReader singleRestLiAttachmentReader)
    {
      final SingleGreetingBlobReaderCallback singleGreetingBlobReaderCallback = new SingleGreetingBlobReaderCallback(this,
                                                                                             singleRestLiAttachmentReader);
      singleRestLiAttachmentReader.registerCallback(singleGreetingBlobReaderCallback);
      singleRestLiAttachmentReader.requestAttachmentData();
    }

    @Override
    public void onFinished()
    {
      _createResponseCallback.onSuccess(new CreateResponse(150));
    }

    @Override
    public void onDrainComplete()
    {
      _createResponseCallback.onError(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR));
    }

    @Override
    public void onStreamError(Throwable throwable)
    {
      _createResponseCallback.onError(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR));
    }
  }

  private static class SingleGreetingBlobReaderCallback implements SingleRestLiAttachmentReaderCallback
  {
    private final RestLiAttachmentReaderCallback _topLevelCallback;
    private final RestLiAttachmentReader.SingleRestLiAttachmentReader _singleRestLiAttachmentReader;
    private final ByteArrayOutputStream _byteArrayOutputStream = new ByteArrayOutputStream();

    public SingleGreetingBlobReaderCallback(RestLiAttachmentReaderCallback topLevelCallback,
                                            RestLiAttachmentReader.SingleRestLiAttachmentReader singleRestLiAttachmentReader)
    {
      _topLevelCallback = topLevelCallback;
      _singleRestLiAttachmentReader = singleRestLiAttachmentReader;
    }

    @Override
    public void onAttachmentDataAvailable(ByteString attachmentData)
    {
      try
      {
        _byteArrayOutputStream.write(attachmentData.copyBytes());
        _singleRestLiAttachmentReader.requestAttachmentData();
      }
      catch (Exception exception)
      {
        _topLevelCallback.onStreamError(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR));
      }
    }

    @Override
    public void onFinished()
    {
      greetingBytes = _byteArrayOutputStream.toByteArray();
    }

    @Override
    public void onDrainComplete()
    {
      _topLevelCallback.onStreamError(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR));
    }

    @Override
    public void onAttachmentError(Throwable throwable)
    {
      //No need to do anything since the top level callback will get invoked with an error anyway
    }
  }
}
