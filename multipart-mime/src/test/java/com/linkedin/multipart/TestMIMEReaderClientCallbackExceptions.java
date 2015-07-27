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
import com.linkedin.multipart.exceptions.MultiPartReaderFinishedException;
import com.linkedin.multipart.exceptions.SinglePartFinishedException;
import com.linkedin.r2.filter.R2Constants;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.multipart.utils.MIMETestUtils.BODY_LESS_BODY;
import static com.linkedin.multipart.utils.MIMETestUtils.BYTES_BODY;
import static com.linkedin.multipart.utils.MIMETestUtils.HEADER_LESS_BODY;
import static com.linkedin.multipart.utils.MIMETestUtils.LARGE_DATA_SOURCE;
import static com.linkedin.multipart.utils.MIMETestUtils.PURELY_EMPTY_BODY;
import static com.linkedin.multipart.utils.MIMETestUtils.SMALL_DATA_SOURCE;


/**
 * Tests for making sure that the {@link com.linkedin.multipart.MultiPartMIMEReader} is resilient in the face of
 * exceptions thrown by invoking client callbacks.
 *
 * @author Karim Vidhani
 */
public class TestMIMEReaderClientCallbackExceptions extends AbstractMIMEUnitTest
{
  MultiPartMIMEReader _reader;
  MultiPartMIMEExceptionReaderCallbackImpl _currentMultiPartMIMEReaderCallback;

  //MultiPartMIMEReader callback invocations throwing exceptions:
  //These tests all verify the resilience of the multipart mime reader when multipart mime reader client callbacks throw runtime exceptions
  @DataProvider(name = "allTypesOfBodiesDataSource")
  public Object[][] allTypesOfBodiesDataSource() throws Exception
  {
    final List<MimeBodyPart> bodyPartList = new ArrayList<MimeBodyPart>();
    bodyPartList.add(SMALL_DATA_SOURCE);
    bodyPartList.add(LARGE_DATA_SOURCE);
    bodyPartList.add(HEADER_LESS_BODY);
    bodyPartList.add(BODY_LESS_BODY);
    bodyPartList.add(BYTES_BODY);
    bodyPartList.add(PURELY_EMPTY_BODY);

    return new Object[][]
        {
            {1, bodyPartList},
            {R2Constants.DEFAULT_DATA_CHUNK_SIZE, bodyPartList}
        };
  }

  @Test(dataProvider = "allTypesOfBodiesDataSource")
  public void testMultiPartMIMEReaderCallbackExceptionOnNewPart(final int chunkSize,
                                                                final List<MimeBodyPart> bodyPartList) throws Exception
  {
    MimeMultipart multiPartMimeBody = new MimeMultipart();

    //Add your body parts
    for (final MimeBodyPart bodyPart : bodyPartList)
    {
      multiPartMimeBody.addBodyPart(bodyPart);
    }

    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    multiPartMimeBody.writeTo(byteArrayOutputStream);
    final ByteString requestPayload = ByteString.copy(byteArrayOutputStream.toByteArray());

    final CountDownLatch countDownLatch =
        executeRequestPartialReadWithException(requestPayload, chunkSize, multiPartMimeBody.getContentType(),
                                               MultiPartMIMEThrowOnFlag.THROW_ON_NEW_PART,
                                               SinglePartMIMEThrowOnFlag.NO_THROW);

    countDownLatch.await(_testTimeout, TimeUnit.MILLISECONDS);

    Assert.assertTrue(_currentMultiPartMIMEReaderCallback.getStreamError() instanceof IllegalMonitorStateException);
    Assert.assertEquals(_currentMultiPartMIMEReaderCallback.getSinglePartMIMEReaderCallbacks().size(), 0);

    try
    {
      _currentMultiPartMIMEReaderCallback.getReader().abandonAllParts();
      Assert.fail();
    }
    catch (MultiPartReaderFinishedException multiPartReaderFinishedException)
    {
      //pass
    }
  }

  @Test(dataProvider = "allTypesOfBodiesDataSource")
  public void testMultiPartMIMEReaderCallbackExceptionOnFinished(final int chunkSize,
                                                                 final List<MimeBodyPart> bodyPartList) throws Exception
  {
    MimeMultipart multiPartMimeBody = new MimeMultipart();

    //Add your body parts
    for (final MimeBodyPart bodyPart : bodyPartList)
    {
      multiPartMimeBody.addBodyPart(bodyPart);
    }

    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    multiPartMimeBody.writeTo(byteArrayOutputStream);
    final ByteString requestPayload = ByteString.copy(byteArrayOutputStream.toByteArray());

    final CountDownLatch countDownLatch =
        executeRequestPartialReadWithException(requestPayload, chunkSize, multiPartMimeBody.getContentType(),
                                               MultiPartMIMEThrowOnFlag.THROW_ON_FINISHED,
                                               SinglePartMIMEThrowOnFlag.NO_THROW);

    countDownLatch.await(_testTimeout, TimeUnit.MILLISECONDS);

    Assert.assertTrue(_currentMultiPartMIMEReaderCallback.getStreamError() instanceof IllegalMonitorStateException);

    //Verify this is unusable.
    try
    {
      _currentMultiPartMIMEReaderCallback.getReader().abandonAllParts();
      Assert.fail();
    }
    catch (MultiPartReaderFinishedException multiPartReaderFinishedException)
    {
      //pass
    }

    Assert.assertEquals(_currentMultiPartMIMEReaderCallback.getSinglePartMIMEReaderCallbacks().size(), 6);
    //None of the single part callbacks should have recieved the error since they were all done before the top
    //callback threw
    for (int i = 0; i < _currentMultiPartMIMEReaderCallback.getSinglePartMIMEReaderCallbacks().size(); i++)
    {
      Assert.assertNull(_currentMultiPartMIMEReaderCallback.getSinglePartMIMEReaderCallbacks().get(i).getStreamError());
      //Verify this is unusable.
      try
      {
        _currentMultiPartMIMEReaderCallback.getSinglePartMIMEReaderCallbacks().get(i).getSinglePartMIMEReader().requestPartData();
        Assert.fail();
      }
      catch (SinglePartFinishedException singlePartFinishedException)
      {
        //pass
      }
    }
  }

  @Test(dataProvider = "allTypesOfBodiesDataSource")
  public void testMultiPartMIMEReaderCallbackExceptionOnAbandoned(final int chunkSize,
                                                                  final List<MimeBodyPart> bodyPartList) throws Exception
  {
    MimeMultipart multiPartMimeBody = new MimeMultipart();

    //Add your body parts
    for (final MimeBodyPart bodyPart : bodyPartList)
    {
      multiPartMimeBody.addBodyPart(bodyPart);
    }

    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    multiPartMimeBody.writeTo(byteArrayOutputStream);
    final ByteString requestPayload = ByteString.copy(byteArrayOutputStream.toByteArray());

    final CountDownLatch countDownLatch =
        executeRequestPartialReadWithException(requestPayload, chunkSize, multiPartMimeBody.getContentType(),
                                               MultiPartMIMEThrowOnFlag.THROW_ON_ABANDONED,
                                               SinglePartMIMEThrowOnFlag.NO_THROW);

    countDownLatch.await(_testTimeout, TimeUnit.MILLISECONDS);

    Assert.assertTrue(_currentMultiPartMIMEReaderCallback.getStreamError() instanceof IllegalMonitorStateException);
    Assert.assertEquals(_currentMultiPartMIMEReaderCallback.getSinglePartMIMEReaderCallbacks().size(), 0);

    //Verify this is unusable.
    try
    {
      _currentMultiPartMIMEReaderCallback.getReader().abandonAllParts();
      Assert.fail();
    }
    catch (MultiPartReaderFinishedException multiPartReaderFinishedException)
    {
      //pass
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////
  //SinglePartMIMEReader callback invocations throwing exceptions:
  //These tests all verify the resilience of the single part mime reader when single part mime reader client callbacks throw runtime exceptions
  @Test(dataProvider = "allTypesOfBodiesDataSource")
  public void testSinglePartMIMEReaderCallbackExceptionOnPartDataAvailable(final int chunkSize,
                                                                           final List<MimeBodyPart> bodyPartList) throws Exception
  {
    MimeMultipart multiPartMimeBody = new MimeMultipart();

    //Add your body parts
    for (final MimeBodyPart bodyPart : bodyPartList)
    {
      multiPartMimeBody.addBodyPart(bodyPart);
    }

    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    multiPartMimeBody.writeTo(byteArrayOutputStream);
    final ByteString requestPayload = ByteString.copy(byteArrayOutputStream.toByteArray());

    final CountDownLatch countDownLatch = executeRequestPartialReadWithException(requestPayload, chunkSize,
                                                                                 multiPartMimeBody.getContentType(),
                                                                                 MultiPartMIMEThrowOnFlag.NO_THROW,
                                                                                 SinglePartMIMEThrowOnFlag.THROW_ON_PART_DATA_AVAILABLE);

    countDownLatch.await(_testTimeout, TimeUnit.MILLISECONDS);

    Assert.assertTrue(_currentMultiPartMIMEReaderCallback.getStreamError() instanceof IllegalMonitorStateException);
    //Verify this is unusable.
    try
    {
      _currentMultiPartMIMEReaderCallback.getReader().abandonAllParts();
      Assert.fail();
    }
    catch (MultiPartReaderFinishedException multiPartReaderFinishedException)
    {
      //pass
    }

    Assert.assertEquals(_currentMultiPartMIMEReaderCallback.getSinglePartMIMEReaderCallbacks().size(), 1);
    Assert.assertTrue(_currentMultiPartMIMEReaderCallback.getSinglePartMIMEReaderCallbacks().get(0).getStreamError() instanceof IllegalMonitorStateException);
    try
    {
      _currentMultiPartMIMEReaderCallback.getSinglePartMIMEReaderCallbacks().get(0).getSinglePartMIMEReader().requestPartData();
      Assert.fail();
    }
    catch (SinglePartFinishedException singlePartFinishedException)
    {
      //pass
    }
  }

  @Test(dataProvider = "allTypesOfBodiesDataSource")
  public void testSinglePartMIMEReaderCallbackExceptionOnFinished(final int chunkSize,
                                                                  final List<MimeBodyPart> bodyPartList) throws Exception
  {
    MimeMultipart multiPartMimeBody = new MimeMultipart();

    //Add your body parts
    for (final MimeBodyPart bodyPart : bodyPartList)
    {
      multiPartMimeBody.addBodyPart(bodyPart);
    }

    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    multiPartMimeBody.writeTo(byteArrayOutputStream);
    final ByteString requestPayload = ByteString.copy(byteArrayOutputStream.toByteArray());

    final CountDownLatch countDownLatch =
        executeRequestPartialReadWithException(requestPayload, chunkSize, multiPartMimeBody.getContentType(),
                                               MultiPartMIMEThrowOnFlag.NO_THROW,
                                               SinglePartMIMEThrowOnFlag.THROW_ON_FINISHED);

    countDownLatch.await(_testTimeout, TimeUnit.MILLISECONDS);

    Assert.assertTrue(_currentMultiPartMIMEReaderCallback.getStreamError() instanceof IllegalMonitorStateException);
    //Verify this is unusable.
    try
    {
      _currentMultiPartMIMEReaderCallback.getReader().abandonAllParts();
      Assert.fail();
    }
    catch (MultiPartReaderFinishedException multiPartReaderFinishedException)
    {
      //pass
    }

    Assert.assertEquals(_currentMultiPartMIMEReaderCallback.getSinglePartMIMEReaderCallbacks().size(), 1);
    Assert.assertTrue(_currentMultiPartMIMEReaderCallback.getSinglePartMIMEReaderCallbacks().get(0).getStreamError() instanceof IllegalMonitorStateException);
    try
    {
      _currentMultiPartMIMEReaderCallback.getSinglePartMIMEReaderCallbacks().get(0).getSinglePartMIMEReader().requestPartData();
      Assert.fail();
    }
    catch (SinglePartFinishedException singlePartFinishedException)
    {
      //pass
    }
  }

  @Test(dataProvider = "allTypesOfBodiesDataSource")
  public void testSinglePartMIMEReaderCallbackExceptionOnAbandoned(final int chunkSize,
                                                                   final List<MimeBodyPart> bodyPartList) throws Exception
  {
    MimeMultipart multiPartMimeBody = new MimeMultipart();

    //Add your body parts
    for (final MimeBodyPart bodyPart : bodyPartList)
    {
      multiPartMimeBody.addBodyPart(bodyPart);
    }

    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    multiPartMimeBody.writeTo(byteArrayOutputStream);
    final ByteString requestPayload = ByteString.copy(byteArrayOutputStream.toByteArray());

    final CountDownLatch countDownLatch =
        executeRequestPartialReadWithException(requestPayload, chunkSize, multiPartMimeBody.getContentType(),
                                               MultiPartMIMEThrowOnFlag.NO_THROW,
                                               SinglePartMIMEThrowOnFlag.THROW_ON_ABANDONED);

    countDownLatch.await(_testTimeout, TimeUnit.MILLISECONDS);

    Assert.assertTrue(_currentMultiPartMIMEReaderCallback.getStreamError() instanceof IllegalMonitorStateException);
    //Verify these are unusable.
    try
    {
      _currentMultiPartMIMEReaderCallback.getReader().abandonAllParts();
      Assert.fail();
    }
    catch (MultiPartReaderFinishedException multiPartReaderFinishedException)
    {
      //pass
    }
    Assert.assertEquals(_currentMultiPartMIMEReaderCallback.getSinglePartMIMEReaderCallbacks().size(), 1);
    Assert.assertTrue(_currentMultiPartMIMEReaderCallback.getSinglePartMIMEReaderCallbacks().get(0).getStreamError() instanceof IllegalMonitorStateException);
    try
    {
      _currentMultiPartMIMEReaderCallback.getSinglePartMIMEReaderCallbacks().get(0).getSinglePartMIMEReader().requestPartData();
      Assert.fail();
    }
    catch (SinglePartFinishedException singlePartFinishedException)
    {
      //pass
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////
  private CountDownLatch executeRequestPartialReadWithException(final ByteString requestPayload,
                                                                final int chunkSize,
                                                                final String contentTypeHeader,
                                                                final MultiPartMIMEThrowOnFlag multiPartThrowOnFlag,
                                                                final SinglePartMIMEThrowOnFlag singlePartThrowOnFlag) throws Exception
  {
    mockR2AndWrite(requestPayload, chunkSize, contentTypeHeader);
    final CountDownLatch latch = new CountDownLatch(1);

    _reader = MultiPartMIMEReader.createAndAcquireStream(_streamRequest);
    _currentMultiPartMIMEReaderCallback = new MultiPartMIMEExceptionReaderCallbackImpl(latch, _reader,
                                                                                       multiPartThrowOnFlag,
                                                                                       singlePartThrowOnFlag);
    _reader.registerReaderCallback(_currentMultiPartMIMEReaderCallback);

    return latch;
  }

  private enum SinglePartMIMEThrowOnFlag
  {
    THROW_ON_PART_DATA_AVAILABLE,
    THROW_ON_FINISHED,
    THROW_ON_ABANDONED,
    NO_THROW;
  }

  private static class SinglePartMIMEExceptionReaderCallbackImpl implements SinglePartMIMEReaderCallback
  {
    final MultiPartMIMEReader.SinglePartMIMEReader _singlePartMIMEReader;
    Throwable _streamError = null;
    final CountDownLatch _countDownLatch;
    final SinglePartMIMEThrowOnFlag _singlePartMIMEThrowOnFlag;

    SinglePartMIMEExceptionReaderCallbackImpl(final MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader,
                                              final CountDownLatch countDownLatch,
                                              final SinglePartMIMEThrowOnFlag singlePartMIMEThrowOnFlag)
    {
      _singlePartMIMEReader = singlePartMIMEReader;
      _countDownLatch = countDownLatch;
      _singlePartMIMEThrowOnFlag = singlePartMIMEThrowOnFlag;
    }

    public MultiPartMIMEReader.SinglePartMIMEReader getSinglePartMIMEReader()
    {
      return _singlePartMIMEReader;
    }

    public Throwable getStreamError()
    {
      return _streamError;
    }

    @Override
    public void onPartDataAvailable(ByteString partData)
    {
      if (_singlePartMIMEThrowOnFlag == SinglePartMIMEThrowOnFlag.THROW_ON_PART_DATA_AVAILABLE)
      {
        throw new IllegalMonitorStateException();
      }
      else if (_singlePartMIMEThrowOnFlag == SinglePartMIMEThrowOnFlag.THROW_ON_ABANDONED)
      {
        _singlePartMIMEReader.abandonPart();
        return;
      }
      else
      {
        _singlePartMIMEReader.requestPartData();
      }
    }

    @Override
    public void onFinished()
    {
      if (_singlePartMIMEThrowOnFlag == SinglePartMIMEThrowOnFlag.THROW_ON_FINISHED)
      {
        throw new IllegalMonitorStateException();
      }
    }

    @Override
    public void onAbandoned()
    {
      //We only reached here due to the presence of throwOnAbandoned == true
      throw new IllegalMonitorStateException();
    }

    @Override
    public void onStreamError(Throwable throwable)
    {
      _streamError = throwable;
    }
  }

  private enum MultiPartMIMEThrowOnFlag
  {
    THROW_ON_NEW_PART,
    THROW_ON_FINISHED,
    THROW_ON_ABANDONED,
    NO_THROW;
  }

  private static class MultiPartMIMEExceptionReaderCallbackImpl implements MultiPartMIMEReaderCallback
  {
    final List<SinglePartMIMEExceptionReaderCallbackImpl> _singlePartMIMEReaderCallbacks = new ArrayList<SinglePartMIMEExceptionReaderCallbackImpl>();
    Throwable _streamError = null;
    final CountDownLatch _latch;
    final MultiPartMIMEReader _reader;
    final MultiPartMIMEThrowOnFlag _multiPartMIMEThrowOnFlag;
    final SinglePartMIMEThrowOnFlag _singlePartMIMEThrowOnFlag;

    MultiPartMIMEExceptionReaderCallbackImpl(final CountDownLatch latch,
                                             final MultiPartMIMEReader reader,
                                             final MultiPartMIMEThrowOnFlag multiPartMIMEThrowOnFlag,
                                             final SinglePartMIMEThrowOnFlag singlePartMIMEThrowOnFlag)
    {
      _latch = latch;
      _reader = reader;
      _multiPartMIMEThrowOnFlag = multiPartMIMEThrowOnFlag;
      _singlePartMIMEThrowOnFlag = singlePartMIMEThrowOnFlag;
    }

    public List<SinglePartMIMEExceptionReaderCallbackImpl> getSinglePartMIMEReaderCallbacks()
    {
      return _singlePartMIMEReaderCallbacks;
    }

    public Throwable getStreamError()
    {
      return _streamError;
    }

    public MultiPartMIMEReader getReader()
    {
      return _reader;
    }

    @Override
    public void onNewPart(MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader)
    {
      if (_multiPartMIMEThrowOnFlag == MultiPartMIMEThrowOnFlag.THROW_ON_NEW_PART)
      {
        throw new IllegalMonitorStateException();
      }

      if (_multiPartMIMEThrowOnFlag == MultiPartMIMEThrowOnFlag.THROW_ON_ABANDONED)
      {
        _reader.abandonAllParts();
        return;
      }

      SinglePartMIMEExceptionReaderCallbackImpl singlePartMIMEReaderCallback =
          new SinglePartMIMEExceptionReaderCallbackImpl(singlePartMIMEReader, _latch, _singlePartMIMEThrowOnFlag);
      singlePartMIMEReader.registerReaderCallback(singlePartMIMEReaderCallback);
      _singlePartMIMEReaderCallbacks.add(singlePartMIMEReaderCallback);

      singlePartMIMEReader.requestPartData();
    }

    @Override
    public void onFinished()
    {
      if (_multiPartMIMEThrowOnFlag == MultiPartMIMEThrowOnFlag.THROW_ON_FINISHED)
      {
        throw new IllegalMonitorStateException();
      }
    }

    @Override
    public void onAbandoned()
    {
      //We only reached here due to the presence of throwOnAbandoned == true
      throw new IllegalMonitorStateException();
    }

    @Override
    public void onStreamError(Throwable throwable)
    {
      _streamError = throwable;
      _latch.countDown();
    }
  }
}