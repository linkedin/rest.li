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

import org.testng.annotations.Test;

import org.junit.Assert;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;


/**
 * Tests for {@link RestLiAttachmentReader}.
 */
public class TestRestLiAttachmentReader
{
  @Test
  public void testRestLiAttachmentReader()
  {
    //Create a mock MultiPartMIMEReader and pass to the RestLiAttachmentReader. Verify that API calls are propagated accordingly.
    final MultiPartMIMEReader multiPartMIMEReader = mock(MultiPartMIMEReader.class);
    final RestLiAttachmentReader attachmentReader = new RestLiAttachmentReader(multiPartMIMEReader);
    attachmentReader.drainAllAttachments();
    attachmentReader.haveAllAttachmentsFinished();

    final RestLiAttachmentReaderCallback dummyCallback = new RestLiAttachmentReaderCallback()
    {
      //None of these should be called.
      @Override
      public void onNewAttachment(RestLiAttachmentReader.SingleRestLiAttachmentReader singleRestLiAttachmentReader)
      {
        Assert.fail();
      }

      @Override
      public void onFinished()
      {
        Assert.fail();
      }

      @Override
      public void onDrainComplete()
      {
        Assert.fail();
      }

      @Override
      public void onStreamError(Throwable throwable)
      {
        Assert.fail();
      }
    };

    attachmentReader.registerAttachmentReaderCallback(dummyCallback);

    //Verify the calls above made it correctly to the layer below
    verify(multiPartMIMEReader, times(1)).drainAllParts();
    verify(multiPartMIMEReader, times(1)).haveAllPartsFinished();
    verify(multiPartMIMEReader, times(1)).registerReaderCallback(isA(MultiPartMIMEReaderCallback.class));
    verifyNoMoreInteractions(multiPartMIMEReader);
  }

  @Test
  public void testSingleRestLiAttachmentReader()
  {
    //Create a mock MultiPartMIMEReader.SinglePartMIMEReader and pass to the RestLiAttachmentReader.SingleRestLiAttachmentReader.
    //Verify that API calls are propagated accordingly.
    final RestLiAttachmentReader attachmentReader = mock(RestLiAttachmentReader.class);
    final MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader = mock(MultiPartMIMEReader.SinglePartMIMEReader.class);
    final RestLiAttachmentReader.SingleRestLiAttachmentReader singleRestLiAttachmentReader =
        attachmentReader.new SingleRestLiAttachmentReader(singlePartMIMEReader, "foo");

    singleRestLiAttachmentReader.requestAttachmentData();
    singleRestLiAttachmentReader.drainAttachment();

    final SingleRestLiAttachmentReaderCallback dummyCallback = new SingleRestLiAttachmentReaderCallback()
    {
      @Override
      public void onAttachmentDataAvailable(ByteString attachmentData)
      {
        Assert.fail();
      }

      @Override
      public void onFinished()
      {
        Assert.fail();
      }

      @Override
      public void onDrainComplete()
      {
        Assert.fail();
      }

      @Override
      public void onAttachmentError(Throwable throwable)
      {
        Assert.fail();
      }
    };
    singleRestLiAttachmentReader.registerCallback(dummyCallback);


    Assert.assertEquals(singleRestLiAttachmentReader.getAttachmentID(), "foo");

    //Verify the calls above made it correctly to the layer below.
    verify(singlePartMIMEReader, times(1)).requestPartData();
    verify(singlePartMIMEReader, times(1)).drainPart();
    verify(singlePartMIMEReader, times(1)).registerReaderCallback(isA(SinglePartMIMEReaderCallback.class));
    verifyNoMoreInteractions(singlePartMIMEReader);
  }
}