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

package com.linkedin.restli.common.attachments;


import com.linkedin.data.ByteString;
import com.linkedin.multipart.MultiPartMIMEReader;
import com.linkedin.multipart.MultiPartMIMEReaderCallback;
import com.linkedin.multipart.SinglePartMIMEReaderCallback;
import com.linkedin.multipart.exceptions.GeneralMultiPartMIMEReaderStreamException;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.restli.common.RestConstants;


/**
 * Allows users to asynchronously walk through all attachments from an incoming request on the server side, or an
 * incoming response on the client side.
 *
 * Usage of this reader always begins with a registration using
 * {@link RestLiAttachmentReader#registerAttachmentReaderCallback(com.linkedin.restli.common.attachments.RestLiAttachmentReaderCallback)}.
 *
 * @author Karim Vidhani
 */
public class RestLiAttachmentReader implements RestLiDataSourceIterator
{
  private final MultiPartMIMEReader _multiPartMIMEReader;

  /**
   * Constructs a RestLiAttachmentReader by wrapping a {@link com.linkedin.multipart.MultiPartMIMEReader}.
   *
   * NOTE: This should not be instantiated directly by consumers of rest.li.
   *
   * @param multiPartMIMEReader the {@link com.linkedin.multipart.MultiPartMIMEReader} to wrap.
   */
  public RestLiAttachmentReader(final MultiPartMIMEReader multiPartMIMEReader)
  {
    _multiPartMIMEReader = multiPartMIMEReader;
  }

  /**
   * Determines if there are any more attachments to read. If the last attachment is in the process of being read,
   * this will return false.
   *
   * @return true if there are more attachments to read, or false if all attachments have been consumed.
   */
  public boolean haveAllAttachmentsFinished()
  {
    return _multiPartMIMEReader.haveAllPartsFinished();
  }

  /**
   * <p>Reads through and drains the current new attachment (if applicable) and additionally all remaining attachments.
   *
   * <p>This API can be used in only the following scenarios:
   *
   * <p>1. Without registering a {@link com.linkedin.restli.common.attachments.RestLiAttachmentReaderCallback}.
   * Draining will begin and since no callback is registered, there will be no notification when it is completed.
   *
   * <p>2. After registration using a {@link com.linkedin.restli.common.attachments.RestLiAttachmentReaderCallback}
   * AND after an invocation on
   * {@link RestLiAttachmentReaderCallback#onNewAttachment(com.linkedin.restli.common.attachments.RestLiAttachmentReader.SingleRestLiAttachmentReader)}.
   * Draining will begin and when it is complete, a call will be made to {@link RestLiAttachmentReaderCallback#onDrainComplete()}.
   *
   * <p>If this is called after registration and before an invocation on
   * {@link RestLiAttachmentReaderCallback#onNewAttachment(com.linkedin.restli.common.attachments.RestLiAttachmentReader.SingleRestLiAttachmentReader)},
   * then a {@link com.linkedin.restli.common.attachments.RestLiAttachmentReaderException} will be thrown.
   *
   * <p>If this is used after registration, then this can ONLY be called if there is no attachment being actively read, meaning that
   * the current {@link com.linkedin.restli.common.attachments.RestLiAttachmentReader.SingleRestLiAttachmentReader} has not been initialized
   * with a {@link com.linkedin.restli.common.attachments.SingleRestLiAttachmentReaderCallback}. If this is violated, a
   * {@link com.linkedin.restli.common.attachments.RestLiAttachmentReaderException} will be thrown.
   *
   * <p>If the stream is finished, subsequent calls will throw {@link com.linkedin.restli.common.attachments.RestLiAttachmentReaderException}.
   *
   * <p>Since this is async and request queueing is not allowed, repetitive calls will result in
   * {@link com.linkedin.restli.common.attachments.RestLiAttachmentReaderException}.
   */
  public void drainAllAttachments()
  {
    try
    {
      //It should be noted here that we use a little clever workaround here to achieve point 1 mentioned above in the
      //Javadocs. Meaning that we allow clients to drain all parts without registering a callback. The caveat
      //is however that if the application developer chooses not to register a callback, then the previous callback
      //(TopLevelReaderCallback in RestLiServer or TopLevelReaderCallback in RestResponseDecoder) is bound to the underlying
      //MultiPartMIMEReader at this point in time.
      //Therefore technically there is a callback bound to the MultiPartMIMEReader, but the application developer
      //is not aware of this. Therefore if they do not register a new callback and then attempt to drainAllAttachments(),
      //the TopLevelReaderCallback will get the notification that draining has completed. In such a case no
      //subsequent action is needed by the TopLevelReaderCallback.
      _multiPartMIMEReader.drainAllParts();
    }
    catch (GeneralMultiPartMIMEReaderStreamException readerException)
    {
      throw new RestLiAttachmentReaderException(readerException);
    }
  }

  /**
   * <p>Register to read using this RestLiAttachmentReader. Upon registration, at some point in the future, an invocation will be
   * made on {@link RestLiAttachmentReaderCallback#onNewAttachment(com.linkedin.restli.common.attachments.RestLiAttachmentReader.SingleRestLiAttachmentReader)}.
   * From this point forward users may start consuming attachment data.
   *
   * <p>This can ONLY be called if there is no attachment being actively
   * read meaning that the current {@link com.linkedin.restli.common.attachments.RestLiAttachmentReader.SingleRestLiAttachmentReader}
   * has not had a callback registered with it. Violation of this will throw a {@link com.linkedin.restli.common.attachments.RestLiAttachmentReaderException}.
   *
   * <p>This can even be set if no attachments in the stream have actually been consumed, i.e after the very first invocation of
   * {@link RestLiAttachmentReaderCallback#onNewAttachment(com.linkedin.restli.common.attachments.RestLiAttachmentReader.SingleRestLiAttachmentReader)}.
   *
   * <p>Essentially users can register a new callback at the very beginning (to initiate the process) or users can register a new callback
   * any time when invoked on onNewAttachment(). When invoked on onNewAttachment() users can also register as many callbacks as they
   * like any number of times they like. Every time a callback is registered, an invocation will be made on onNewAttachment() on that
   * callback.
   *
   * @param restLiAttachmentReaderCallback the callback to register with.
   */
  public void registerAttachmentReaderCallback(final RestLiAttachmentReaderCallback restLiAttachmentReaderCallback)
  {
    try
    {
      _multiPartMIMEReader.registerReaderCallback(new MultiPartMIMEReaderCallback()
      {
        @Override
        public void onNewPart(MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader)
        {
          //If there is no Content-ID in the response then we bail early
          final String contentID = singlePartMIMEReader.dataSourceHeaders().get(RestConstants.HEADER_CONTENT_ID);
          if (contentID == null)
          {
            onStreamError(new RemoteInvocationException("Illegally formed multipart mime envelope. RestLi attachment" +
                    " is missing the ContentID!"));
          }
          restLiAttachmentReaderCallback.onNewAttachment(new SingleRestLiAttachmentReader(singlePartMIMEReader, contentID));
        }

        @Override
        public void onFinished()
        {
          restLiAttachmentReaderCallback.onFinished();
        }

        @Override
        public void onDrainComplete()
        {
          restLiAttachmentReaderCallback.onDrainComplete();
        }

        @Override
        public void onStreamError(Throwable throwable)
        {
          restLiAttachmentReaderCallback.onStreamError(throwable);
        }
      });
    }
    catch (GeneralMultiPartMIMEReaderStreamException readerException)
    {
      throw new RestLiAttachmentReaderException(readerException);
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////
  //Chaining interface implementation. These should not be used directly by external consumers.

  /**
   * Please do not use. This is for internal use only.
   *
   * Invoked when all the potential data sources that this RestLiDataSourceIterator represents need to be abandoned
   * since they will not be given a chance to produce data.
   */
  @Override
  public void abandonAllDataSources()
  {
    _multiPartMIMEReader.abandonAllDataSources();
  }

  /**
   * Please do not use. This is for internal use only.
   *
   * Invoked as the first step to walk through all potential data sources represented by this RestLiDataSourceIterator.
   *
   * @param callback the callback that will be invoked as data sources become available for consumption.
   */
  @Override
  public void registerDataSourceReaderCallback(final RestLiDataSourceIteratorCallback callback)
  {
    registerAttachmentReaderCallback(new RestLiAttachmentReaderCallback()
    {
      @Override
      public void onNewAttachment(SingleRestLiAttachmentReader singleRestLiAttachmentReader)
      {
        callback.onNewDataSourceWriter(singleRestLiAttachmentReader);
      }

      @Override
      public void onFinished()
      {
        callback.onFinished();
      }

      @Override
      public void onDrainComplete()
      {
        callback.onAbandonComplete();
      }

      @Override
      public void onStreamError(Throwable throwable)
      {
        callback.onStreamError(throwable);
      }
    });
  }

  /**
   * Allows users to asynchronously walk through all the data in an individual attachment. Instances of this
   * can only be constructed by a {@link com.linkedin.restli.common.attachments.RestLiAttachmentReader}.
   *
   * Note that this SingleRestLiAttachmentReader may also be used as a data source (as an attachment itself)
   * in an outgoing request. This can happen due to the {@link RestLiAttachmentDataSourceWriter} interface.
   * In such an event, this SinglePartRestLiAttachmentReader has been taken over and cannot subsequently be read from.
   */
  public final class SingleRestLiAttachmentReader implements RestLiAttachmentDataSourceWriter
  {
    private final MultiPartMIMEReader.SinglePartMIMEReader _singlePartMIMEReader;
    private final String _attachmentID;

    /**
     * Package private constructor for testing. This creates a SingleRestLiAttachmentReader by wrapping a
     * {@link com.linkedin.multipart.MultiPartMIMEReader.SinglePartMIMEReader}.
     *
     * @param singlePartMIMEReader the {@link com.linkedin.multipart.MultiPartMIMEReader.SinglePartMIMEReader} to wrap.
     * @param attachmentID the value of the {@link RestConstants#HEADER_CONTENT_ID} from the headers of the
     *                     {@link com.linkedin.multipart.MultiPartMIMEReader.SinglePartMIMEReader}.
     */
    SingleRestLiAttachmentReader(final MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader,
                                 final String attachmentID)
    {
      _singlePartMIMEReader = singlePartMIMEReader;
      _attachmentID = attachmentID;
    }

    /**
     * Denotes the unique identifier for this attachment.
     *
     * @return the {@link java.lang.String} representing this attachment.
     */
    @Override
    public String getAttachmentID()
    {
      return _attachmentID;
    }

    /**
     * Reads bytes from this attachment and notifies the registered callback on
     * {@link SingleRestLiAttachmentReaderCallback#onAttachmentDataAvailable(com.linkedin.data.ByteString)}.
     *
     * Usage of this API requires registration using a {@link SingleRestLiAttachmentReaderCallback}.
     * Failure to do so will throw a {@link com.linkedin.restli.common.attachments.RestLiAttachmentReaderException}.
     *
     * If this attachment is fully consumed, meaning {@link SingleRestLiAttachmentReaderCallback#onFinished()}
     * has been called, then any subsequent calls to requestAttachmentData() will throw
     * {@link com.linkedin.restli.common.attachments.RestLiAttachmentReaderException}.
     *
     * Since this is async and request queueing is not allowed, repetitive calls will result in
     * {@link com.linkedin.restli.common.attachments.RestLiAttachmentReaderException}.
     *
     * If this reader is done, either through an error or a proper finish. Calls to requestAttachmentData() will throw
     * {@link com.linkedin.restli.common.attachments.RestLiAttachmentReaderException}.
     */
    public void requestAttachmentData()
    {
      try
      {
        _singlePartMIMEReader.requestPartData();
      }
      catch (GeneralMultiPartMIMEReaderStreamException readerException)
      {
        throw new RestLiAttachmentReaderException(readerException);
      }
    }

    /**
     * Drains all bytes from this attachment and then notifies the registered callback (if present) on
     * {@link SingleRestLiAttachmentReaderCallback#onDrainComplete()}.
     *
     * Usage of this API does NOT require registration using a {@link SingleRestLiAttachmentReaderCallback}.
     * If there is no callback registration then there is no notification provided upon completion of draining
     * this attachment.
     *
     * If this attachment is fully consumed, meaning {@link SingleRestLiAttachmentReaderCallback#onFinished()}
     * has been called, then any subsequent calls to drainPart() will throw
     * {@link com.linkedin.restli.common.attachments.RestLiAttachmentReaderException}.
     *
     * Since this is async and request queueing is not allowed, repetitive calls will result in
     * {@link com.linkedin.restli.common.attachments.RestLiAttachmentReaderException}.
     *
     * If this reader is done, either through an error or a proper finish. Calls to drainAttachment() will throw
     * {@link com.linkedin.restli.common.attachments.RestLiAttachmentReaderException}.
     */
    public void drainAttachment()
    {
      try
      {
        _singlePartMIMEReader.drainPart();
      }
      catch (GeneralMultiPartMIMEReaderStreamException readerException)
      {
        throw new RestLiAttachmentReaderException(readerException);
      }
    }

    /**
     * This call registers a callback and commits to reading this attachment. This can only happen once per life of each
     * SinglePartRestLiAttachmentReader. Subsequent attempts to modify this will throw
     * {@link com.linkedin.restli.common.attachments.RestLiAttachmentReaderException}.
     *
     * When this SingleRestLiAttachmentReader is used as a data source in an out going request or in a response to a client
     * (via the {@link RestLiAttachmentDataSourceWriter} interface), then this SingleRestLiAttachmentReader can no longer
     * be consumed (as it will have a new callback registered with it).
     *
     * @param callback the callback to be invoked on in order to read attachment data.
     */
    public void registerCallback(final SingleRestLiAttachmentReaderCallback callback)
    {
      try
      {
        _singlePartMIMEReader.registerReaderCallback(new SinglePartMIMEReaderCallback()
        {
          @Override
          public void onPartDataAvailable(ByteString partData)
          {
            callback.onAttachmentDataAvailable(partData);
          }

          @Override
          public void onFinished()
          {
            callback.onFinished();
          }

          @Override
          public void onDrainComplete()
          {
            callback.onDrainComplete();
          }

          @Override
          public void onStreamError(Throwable throwable)
          {
            callback.onAttachmentError(throwable);
          }
        });
      }
      catch (GeneralMultiPartMIMEReaderStreamException readerException)
      {
        throw new RestLiAttachmentReaderException(readerException);
      }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    //Chaining interface implementation. These should not be used directly by external consumers.

    /**
     * Please do not use. This is for internal use only.
     */
    @Override
    public void onInit(WriteHandle wh)
    {
      _singlePartMIMEReader.onInit(wh);
    }

    /**
     * Please do not use. This is for internal use only.
     */
    @Override
    public void onWritePossible()
    {
      _singlePartMIMEReader.onWritePossible();
    }

    /**
     * Please do not use. This is for internal use only.
     */
    @Override
    public void onAbort(Throwable e)
    {
      _singlePartMIMEReader.onAbort(e);
    }
  }
}