/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.multipart;


import com.linkedin.data.ByteString;
import com.linkedin.multipart.exceptions.SinglePartFinishedException;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.mail.internet.MimeMultipart;

import org.testng.Assert;
import org.testng.annotations.Test;

import static com.linkedin.multipart.utils.MIMETestUtils.LARGE_DATA_SOURCE;


/**
 * Test to verify that the {@link com.linkedin.multipart.MultiPartMIMEReader} can properly handle onError()
 * sent by R2.
 *
 * @author Karim Vidhani
 */
public class TestMIMEReaderR2Error extends AbstractMIMEUnitTest
{
  MultiPartMIMEReader _reader;
  MultiPartMIMEReaderCallbackImpl _currentMultiPartMIMEReaderCallback;

  //This test will verify that, in the middle of normal processing, we are able to handle R2
  //errors gracefully. We simulate a pause in the middle of normal processing by counting down the latch
  //in the callbacks in the middle of the 2nd part.
  @Test
  public void testMidProcessingR2Error() throws Exception
  {
    MimeMultipart multiPartMimeBody = new MimeMultipart();

    multiPartMimeBody.addBodyPart(LARGE_DATA_SOURCE);
    multiPartMimeBody.addBodyPart(LARGE_DATA_SOURCE);
    multiPartMimeBody.addBodyPart(LARGE_DATA_SOURCE);
    multiPartMimeBody.addBodyPart(LARGE_DATA_SOURCE);
    multiPartMimeBody.addBodyPart(LARGE_DATA_SOURCE);
    multiPartMimeBody.addBodyPart(LARGE_DATA_SOURCE);

    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    multiPartMimeBody.writeTo(byteArrayOutputStream);
    final ByteString requestPayload = ByteString.copy(byteArrayOutputStream.toByteArray());

    CountDownLatch countDownLatch =
        executeRequestPartialReadWithException(requestPayload, 1, multiPartMimeBody.getContentType());

    countDownLatch.await(_testTimeout, TimeUnit.MILLISECONDS);

    //When this returns, its partially complete
    //In this point in time let us simulate an R2 error
    _reader.getR2MultiPartMIMEReader().onError(new NullPointerException());

    Assert.assertTrue(_currentMultiPartMIMEReaderCallback._streamError instanceof NullPointerException);
    try
    {
      _currentMultiPartMIMEReaderCallback._singlePartMIMEReaderCallbacks.get(0)._singlePartMIMEReader.requestPartData();
      Assert.fail();
    }
    catch (SinglePartFinishedException singlePartFinishedException)
    {
      //pass
    }

    Assert.assertEquals(_currentMultiPartMIMEReaderCallback._singlePartMIMEReaderCallbacks.size(), 2);
    Assert.assertNull(_currentMultiPartMIMEReaderCallback._singlePartMIMEReaderCallbacks.get(0)._streamError);
    Assert.assertTrue(_currentMultiPartMIMEReaderCallback._singlePartMIMEReaderCallbacks.get(1)._streamError instanceof NullPointerException);

    try
    {
      _currentMultiPartMIMEReaderCallback._singlePartMIMEReaderCallbacks.get(1)._singlePartMIMEReader.requestPartData();
      Assert.fail();
    }
    catch (SinglePartFinishedException singlePartFinishedException)
    {
      //pass
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////
  private CountDownLatch executeRequestPartialReadWithException(final ByteString requestPayload, final int chunkSize,
                                                                final String contentTypeHeader) throws Exception
  {
    mockR2AndWrite(requestPayload, chunkSize, contentTypeHeader);
    final CountDownLatch latch = new CountDownLatch(1);

    _reader = MultiPartMIMEReader.createAndAcquireStream(_streamRequest);
    _currentMultiPartMIMEReaderCallback = new MultiPartMIMEReaderCallbackImpl(latch);
    _reader.registerReaderCallback(_currentMultiPartMIMEReaderCallback);

    return latch;
  }

  private class SinglePartMIMEReaderCallbackImpl implements SinglePartMIMEReaderCallback
  {
    final MultiPartMIMEReader.SinglePartMIMEReader _singlePartMIMEReader;
    Throwable _streamError = null;
    final CountDownLatch _countDownLatch;
    final boolean _partiallyRead;

    SinglePartMIMEReaderCallbackImpl(final MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader,
                                     final CountDownLatch countDownLatch, final boolean partiallyRead)
    {
      _singlePartMIMEReader = singlePartMIMEReader;
      _countDownLatch = countDownLatch;
      _partiallyRead = partiallyRead;
    }

    @Override
    public void onPartDataAvailable(ByteString partData)
    {
      if (!_partiallyRead)
      {
        _singlePartMIMEReader.requestPartData();
      }
      else
      {
        _countDownLatch.countDown();
      }
    }

    @Override
    public void onFinished()
    {
    }

    //Delegate to the top level for now for these two
    @Override
    public void onAbandoned()
    {
      Assert.fail();
    }

    @Override
    public void onStreamError(Throwable throwable)
    {
      _streamError = throwable;
    }
  }

  private class MultiPartMIMEReaderCallbackImpl implements MultiPartMIMEReaderCallback
  {
    final List<SinglePartMIMEReaderCallbackImpl> _singlePartMIMEReaderCallbacks = new ArrayList<SinglePartMIMEReaderCallbackImpl>();
    Throwable _streamError = null;
    final CountDownLatch _latch;

    MultiPartMIMEReaderCallbackImpl(final CountDownLatch latch)
    {
      _latch = latch;
    }

    @Override
    public void onNewPart(MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader)
    {
      //Only partially read the 2nd part.
      SinglePartMIMEReaderCallbackImpl singlePartMIMEReaderCallback = null;
      if (_singlePartMIMEReaderCallbacks.size() < 1)
      {
        singlePartMIMEReaderCallback = new SinglePartMIMEReaderCallbackImpl(singlePartMIMEReader, _latch, false);
      }
      else
      {
        singlePartMIMEReaderCallback = new SinglePartMIMEReaderCallbackImpl(singlePartMIMEReader, _latch, true);
      }

      singlePartMIMEReader.registerReaderCallback(singlePartMIMEReaderCallback);
      _singlePartMIMEReaderCallbacks.add(singlePartMIMEReaderCallback);
      singlePartMIMEReader.requestPartData();
    }

    @Override
    public void onFinished()
    {
      Assert.fail();
    }

    @Override
    public void onAbandoned()
    {
      Assert.fail();
    }

    @Override
    public void onStreamError(Throwable throwable)
    {
      _streamError = throwable;
    }
  }
}