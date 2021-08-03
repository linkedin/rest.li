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
import com.linkedin.r2.filter.R2Constants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.multipart.utils.MIMETestUtils.*;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.times;


/**
 * Unit tests that mock out R2 and test the draining behavior of {@link com.linkedin.multipart.MultiPartMIMEReader}.
 *
 * @author Karim Vidhani
 */
public class TestMIMEReaderDrain extends AbstractMIMEUnitTest
{
  MultiPartMIMEDrainReaderCallbackImpl _currentMultiPartMIMEReaderCallback;
  MimeMultipart _currentMimeMultipartBody;

  //This test will perform a drain without registering a callback. It functions different then other drain tests
  //located in this class in terms of setup, assertions and verifies.
  @Test
  public void testDrainAllWithoutCallbackRegistered() throws Exception
  {
    mockR2AndWrite(ByteString.copy("Some multipart mime payload. It doesn't need to be pretty".getBytes()),
                   1, "multipart/mixed; boundary=----abcdefghijk");

    MultiPartMIMEReader reader = MultiPartMIMEReader.createAndAcquireStream(_streamRequest);

    try
    {
      reader.drainAllParts(); //The first should succeed.
      reader.drainAllParts(); //The second should fail.
      Assert.fail();
    }
    catch (MultiPartReaderFinishedException multiPartReaderFinishedException)
    {
    }

    Assert.assertTrue(reader.haveAllPartsFinished());

    //mock verifies
    verify(_readHandle, times(1)).cancel();
    verify(_streamRequest, times(1)).getEntityStream();
    verify(_streamRequest, times(1)).getHeader(HEADER_CONTENT_TYPE);
    verify(_entityStream, times(1)).setReader(isA(MultiPartMIMEReader.R2MultiPartMIMEReader.class));

    verifyNoMoreInteractions(_streamRequest);
    verifyNoMoreInteractions(_entityStream);
    verifyNoMoreInteractions(_readHandle);
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  @DataProvider(name = "allTypesOfBodiesDataSource")
  public Object[][] allTypesOfBodiesDataSource() throws Exception
  {
    final List<MimeBodyPart> bodyPartList = new ArrayList<>();
    bodyPartList.add(SMALL_DATA_SOURCE);
    bodyPartList.add(LARGE_DATA_SOURCE);
    bodyPartList.add(HEADER_LESS_BODY);
    bodyPartList.add(BODY_LESS_BODY);
    bodyPartList.add(BYTES_BODY);
    bodyPartList.add(PURELY_EMPTY_BODY);

    bodyPartList.add(PURELY_EMPTY_BODY);
    bodyPartList.add(BYTES_BODY);
    bodyPartList.add(BODY_LESS_BODY);
    bodyPartList.add(HEADER_LESS_BODY);
    bodyPartList.add(LARGE_DATA_SOURCE);
    bodyPartList.add(SMALL_DATA_SOURCE);

    return new Object[][]
        {
            {1, bodyPartList}, {R2Constants.DEFAULT_DATA_CHUNK_SIZE, bodyPartList}
        };
  }

  @Test(dataProvider = "allTypesOfBodiesDataSource")
  public void testSingleAllNoCallback(final int chunkSize, final List<MimeBodyPart> bodyPartList) throws Exception
  {
    executeRequestWithDrainStrategy(chunkSize, bodyPartList, SINGLE_ALL_NO_CALLBACK, "onFinished");

    //Single part drains all individually but doesn't use a callback:
    List<SinglePartMIMEDrainReaderCallbackImpl> singlePartMIMEReaderCallbacks =
        _currentMultiPartMIMEReaderCallback.getSinglePartMIMEReaderCallbacks();
    Assert.assertEquals(singlePartMIMEReaderCallbacks.size(), 0);
  }

  @Test(dataProvider = "allTypesOfBodiesDataSource")
  public void testDrainAllWithCallbackRegistered(final int chunkSize, final List<MimeBodyPart> bodyPartList) throws Exception
  {
    executeRequestWithDrainStrategy(chunkSize, bodyPartList, TOP_ALL_WITH_CALLBACK, "onDrainComplete");

    //Top level drains all after registering a callback and being invoked for the first time on onNewPart().
    List<SinglePartMIMEDrainReaderCallbackImpl> singlePartMIMEReaderCallbacks =
        _currentMultiPartMIMEReaderCallback.getSinglePartMIMEReaderCallbacks();
    Assert.assertEquals(singlePartMIMEReaderCallbacks.size(), 0);
  }

  @Test(dataProvider = "allTypesOfBodiesDataSource")
  public void testSinglePartialTopRemaining(final int chunkSize, final List<MimeBodyPart> bodyPartList) throws Exception
  {
    //Execute the request, verify the correct header came back to ensure the server took the proper drain actions
    //and return the payload so we can assert deeper.
    executeRequestWithDrainStrategy(chunkSize, bodyPartList, SINGLE_PARTIAL_TOP_REMAINING, "onDrainComplete");

    //Single part drains the first 6 then the top level drains all of remaining
    List<SinglePartMIMEDrainReaderCallbackImpl> singlePartMIMEReaderCallbacks =
        _currentMultiPartMIMEReaderCallback.getSinglePartMIMEReaderCallbacks();

    Assert.assertEquals(singlePartMIMEReaderCallbacks.size(), 6);

    for (int i = 0; i < singlePartMIMEReaderCallbacks.size(); i++)
    {
      //Actual 
      final SinglePartMIMEDrainReaderCallbackImpl currentCallback = singlePartMIMEReaderCallbacks.get(i);
      //Expected 
      final BodyPart currentExpectedPart = _currentMimeMultipartBody.getBodyPart(i);

      //Construct expected headers and verify they match 
      final Map<String, String> expectedHeaders = new HashMap<>();
      @SuppressWarnings("unchecked")
      final Enumeration<Header> allHeaders = currentExpectedPart.getAllHeaders();

      while (allHeaders.hasMoreElements())
      {
        final Header header = allHeaders.nextElement();
        expectedHeaders.put(header.getName(), header.getValue());
      }

      Assert.assertEquals(currentCallback.getHeaders(), expectedHeaders);
      //Verify that the bodies are empty
      Assert.assertNull(currentCallback.getFinishedData());
    }
  }

  @Test(dataProvider = "allTypesOfBodiesDataSource")
  public void testSingleAlternateTopRemaining(final int chunkSize, final List<MimeBodyPart> bodyPartList)
      throws Exception
  {
    //Execute the request, verify the correct header came back to ensure the server took the proper drain actions
    //and return the payload so we can assert deeper.
    executeRequestWithDrainStrategy(chunkSize, bodyPartList, SINGLE_ALTERNATE_TOP_REMAINING, "onDrainComplete");

    //Single part alternates between consumption and draining the first 6 parts, then top level drains all of remaining.
    //This means that parts 0, 2, 4 will be consumed and parts 1, 3, 5 will be drained.
    List<SinglePartMIMEDrainReaderCallbackImpl> singlePartMIMEReaderCallbacks =
        _currentMultiPartMIMEReaderCallback.getSinglePartMIMEReaderCallbacks();

    Assert.assertEquals(singlePartMIMEReaderCallbacks.size(), 6);

    //First the consumed
    for (int i = 0; i < singlePartMIMEReaderCallbacks.size(); i = i + 2)
    {
      //Actual 
      final SinglePartMIMEDrainReaderCallbackImpl currentCallback = singlePartMIMEReaderCallbacks.get(i);
      //Expected 
      final BodyPart currentExpectedPart = _currentMimeMultipartBody.getBodyPart(i);

      //Construct expected headers and verify they match 
      final Map<String, String> expectedHeaders = new HashMap<>();
      @SuppressWarnings("unchecked")
      final Enumeration<Header> allHeaders = currentExpectedPart.getAllHeaders();

      while (allHeaders.hasMoreElements())
      {
        final Header header = allHeaders.nextElement();
        expectedHeaders.put(header.getName(), header.getValue());
      }

      Assert.assertEquals(currentCallback.getHeaders(), expectedHeaders);

      //Verify the body matches
      if (currentExpectedPart.getContent() instanceof byte[])
      {
        Assert.assertEquals(currentCallback.getFinishedData().copyBytes(), currentExpectedPart.getContent());
      }
      else
      {
        //Default is String
        Assert.assertEquals(new String(currentCallback.getFinishedData().copyBytes()), currentExpectedPart.getContent());
      }
    }

    //Then the drained
    for (int i = 1; i < singlePartMIMEReaderCallbacks.size(); i = i + 2)
    {
      //Actual 
      final SinglePartMIMEDrainReaderCallbackImpl currentCallback = singlePartMIMEReaderCallbacks.get(i);
      //Expected 
      final BodyPart currentExpectedPart = _currentMimeMultipartBody.getBodyPart(i);

      //Construct expected headers and verify they match 
      final Map<String, String> expectedHeaders = new HashMap<>();
      @SuppressWarnings("unchecked")
      final Enumeration<Header> allHeaders = currentExpectedPart.getAllHeaders();

      while (allHeaders.hasMoreElements())
      {
        final Header header = allHeaders.nextElement();
        expectedHeaders.put(header.getName(), header.getValue());
      }

      Assert.assertEquals(currentCallback.getHeaders(), expectedHeaders);
      //Verify that the bodies are empty
      Assert.assertNull(currentCallback.getFinishedData(), null);
    }
  }

  @Test(dataProvider = "allTypesOfBodiesDataSource")
  public void testSingleAll(final int chunkSize, final List<MimeBodyPart> bodyPartList) throws Exception
  {
    //Execute the request, verify the correct header came back to ensure the server took the proper drain actions
    //and return the payload so we can assert deeper.
    executeRequestWithDrainStrategy(chunkSize, bodyPartList, SINGLE_ALL, "onFinished");

    //Single part drains all, one by one
    List<SinglePartMIMEDrainReaderCallbackImpl> singlePartMIMEReaderCallbacks =
        _currentMultiPartMIMEReaderCallback.getSinglePartMIMEReaderCallbacks();

    Assert.assertEquals(singlePartMIMEReaderCallbacks.size(), 12);

    //Verify everything was drained
    for (int i = 0; i < singlePartMIMEReaderCallbacks.size(); i++)
    {
      //Actual 
      final SinglePartMIMEDrainReaderCallbackImpl currentCallback = singlePartMIMEReaderCallbacks.get(i);
      //Expected 
      final BodyPart currentExpectedPart = _currentMimeMultipartBody.getBodyPart(i);

      //Construct expected headers and verify they match 
      final Map<String, String> expectedHeaders = new HashMap<>();
      @SuppressWarnings("unchecked")
      final Enumeration<Header> allHeaders = currentExpectedPart.getAllHeaders();

      while (allHeaders.hasMoreElements())
      {
        final Header header = allHeaders.nextElement();
        expectedHeaders.put(header.getName(), header.getValue());
      }

      Assert.assertEquals(currentCallback.getHeaders(), expectedHeaders);
      //Verify that the bodies are empty
      Assert.assertNull(currentCallback.getFinishedData());
    }
  }

  @Test(dataProvider = "allTypesOfBodiesDataSource")
  public void testSingleAlternate(final int chunkSize, final List<MimeBodyPart> bodyPartList) throws Exception
  {
    //Execute the request, verify the correct header came back to ensure the server took the proper drain actions
    //and return the payload so we can assert deeper.
    executeRequestWithDrainStrategy(chunkSize, bodyPartList, SINGLE_ALTERNATE, "onFinished");

    //Single part alternates between consumption and draining for all 12 parts.
    //This means that parts 0, 2, 4, etc.. will be consumed and parts 1, 3, 5, etc... will be drained.
    List<SinglePartMIMEDrainReaderCallbackImpl> singlePartMIMEReaderCallbacks =
        _currentMultiPartMIMEReaderCallback.getSinglePartMIMEReaderCallbacks();

    Assert.assertEquals(singlePartMIMEReaderCallbacks.size(), 12);

    //First the consumed
    for (int i = 0; i < singlePartMIMEReaderCallbacks.size(); i = i + 2)
    {
      //Actual 
      final SinglePartMIMEDrainReaderCallbackImpl currentCallback = singlePartMIMEReaderCallbacks.get(i);
      //Expected 
      final BodyPart currentExpectedPart = _currentMimeMultipartBody.getBodyPart(i);

      //Construct expected headers and verify they match 
      final Map<String, String> expectedHeaders = new HashMap<>();
      @SuppressWarnings("unchecked")
      final Enumeration<Header> allHeaders = currentExpectedPart.getAllHeaders();

      while (allHeaders.hasMoreElements())
      {
        final Header header = allHeaders.nextElement();
        expectedHeaders.put(header.getName(), header.getValue());
      }

      Assert.assertEquals(currentCallback.getHeaders(), expectedHeaders);

      //Verify the body matches
      if (currentExpectedPart.getContent() instanceof byte[])
      {
        Assert.assertEquals(currentCallback.getFinishedData().copyBytes(), currentExpectedPart.getContent());
      }
      else
      {
        //Default is String
        Assert.assertEquals(new String(currentCallback.getFinishedData().copyBytes()), currentExpectedPart.getContent());
      }
    }

    //Then the drained
    for (int i = 1; i < singlePartMIMEReaderCallbacks.size(); i = i + 2)
    {
      //Actual 
      final SinglePartMIMEDrainReaderCallbackImpl currentCallback = singlePartMIMEReaderCallbacks.get(i);
      //Expected 
      final BodyPart currentExpectedPart = _currentMimeMultipartBody.getBodyPart(i);

      //Construct expected headers and verify they match 
      final Map<String, String> expectedHeaders = new HashMap<>();
      @SuppressWarnings("unchecked")
      final Enumeration<Header> allHeaders = currentExpectedPart.getAllHeaders();

      while (allHeaders.hasMoreElements())
      {
        final Header header = allHeaders.nextElement();
        expectedHeaders.put(header.getName(), header.getValue());
      }

      Assert.assertEquals(currentCallback.getHeaders(), expectedHeaders);
      //Verify that the bodies are empty
      Assert.assertNull(currentCallback.getFinishedData());
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  private void executeRequestWithDrainStrategy(final int chunkSize, final List<MimeBodyPart> bodyPartList,
                                               final String drainStrategy, final String serverHeaderPrefix) throws Exception
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
    _currentMimeMultipartBody = multiPartMimeBody;

    mockR2AndWrite(requestPayload, chunkSize, multiPartMimeBody.getContentType());

    final CountDownLatch latch = new CountDownLatch(1);
    MultiPartMIMEReader reader = MultiPartMIMEReader.createAndAcquireStream(_streamRequest);
    _currentMultiPartMIMEReaderCallback = new MultiPartMIMEDrainReaderCallbackImpl(latch, drainStrategy, reader);
    reader.registerReaderCallback(_currentMultiPartMIMEReaderCallback);

    latch.await(_testTimeout, TimeUnit.MILLISECONDS);

    Assert.assertEquals(_currentMultiPartMIMEReaderCallback.getResponseHeaders().get(DRAIN_HEADER), serverHeaderPrefix + drainStrategy);

    try
    {
      reader.drainAllParts();
      Assert.fail();
    }
    catch (MultiPartReaderFinishedException multiPartReaderFinishedException)
    {
    }

    Assert.assertTrue(reader.haveAllPartsFinished());

    //mock verifies
    verify(_streamRequest, times(1)).getEntityStream();
    verify(_streamRequest, times(1)).getHeader(HEADER_CONTENT_TYPE);
    verify(_entityStream, times(1)).setReader(isA(MultiPartMIMEReader.R2MultiPartMIMEReader.class));
    final int expectedRequests = (int) Math.ceil((double) requestPayload.length() / chunkSize);
    //One more expected request because we have to make the last call to get called onDone().
    verify(_readHandle, times(expectedRequests + 1)).request(1);
    verifyNoMoreInteractions(_streamRequest);
    verifyNoMoreInteractions(_entityStream);
    verifyNoMoreInteractions(_readHandle);
  }

  private static class SinglePartMIMEDrainReaderCallbackImpl implements SinglePartMIMEReaderCallback
  {
    final MultiPartMIMEReader.SinglePartMIMEReader _singlePartMIMEReader;
    final ByteArrayOutputStream _byteArrayOutputStream = new ByteArrayOutputStream();
    Map<String, String> _headers;
    ByteString _finishedData = null;
    static int partCounter = 0;

    SinglePartMIMEDrainReaderCallbackImpl(final MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader)
    {
      _singlePartMIMEReader = singlePartMIMEReader;
      _headers = singlePartMIMEReader.dataSourceHeaders();
    }

    public Map<String, String> getHeaders()
    {
      return _headers;
    }

    public ByteString getFinishedData()
    {
      return _finishedData;
    }

    @Override
    public void onPartDataAvailable(ByteString partData)
    {
      try
      {
        _byteArrayOutputStream.write(partData.copyBytes());
      }
      catch (IOException ioException)
      {
        Assert.fail();
      }
      _singlePartMIMEReader.requestPartData();
    }

    @Override
    public void onFinished()
    {
      partCounter++;
      _finishedData = ByteString.copy(_byteArrayOutputStream.toByteArray());
    }

    //Delegate to the top level for now for these two
    @Override
    public void onDrainComplete()
    {
      partCounter++;
    }

    @Override
    public void onStreamError(Throwable throwable)
    {
      //MultiPartMIMEReader will end up calling onStreamError(e) on our top level callback
      //which will fail the test
    }
  }

  private static class MultiPartMIMEDrainReaderCallbackImpl implements MultiPartMIMEReaderCallback
  {
    final CountDownLatch _latch;
    final String _drainValue;
    final MultiPartMIMEReader _reader;
    final Map<String, String> _responseHeaders = new HashMap<>();
    final List<SinglePartMIMEDrainReaderCallbackImpl> _singlePartMIMEReaderCallbacks = new ArrayList<>();

    MultiPartMIMEDrainReaderCallbackImpl(final CountDownLatch latch, final String drainValue,
                                         final MultiPartMIMEReader reader)
    {
      _latch = latch;
      _drainValue = drainValue;
      _reader = reader;
    }

    public List<SinglePartMIMEDrainReaderCallbackImpl> getSinglePartMIMEReaderCallbacks()
    {
      return _singlePartMIMEReaderCallbacks;
    }

    public Map<String, String> getResponseHeaders()
    {
      return _responseHeaders;
    }

    @Override
    public void onNewPart(MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader)
    {
      if (_drainValue.equalsIgnoreCase(SINGLE_ALL_NO_CALLBACK))
      {
        singlePartMIMEReader.drainPart();
        return;
      }

      if (_drainValue.equalsIgnoreCase(TOP_ALL_WITH_CALLBACK))
      {
        _reader.drainAllParts();
        return;
      }

      if (_drainValue.equalsIgnoreCase(SINGLE_PARTIAL_TOP_REMAINING) && _singlePartMIMEReaderCallbacks.size() == 6)
      {
        _reader.drainAllParts();
        return;
      }

      if (_drainValue.equalsIgnoreCase(SINGLE_ALTERNATE_TOP_REMAINING) && _singlePartMIMEReaderCallbacks.size() == 6)
      {
        _reader.drainAllParts();
        return;
      }

      //Now we know we have to either consume or drain individually using a registered callback, so we
      //register with the SinglePartReader and take appropriate action based on the drain strategy:
      SinglePartMIMEDrainReaderCallbackImpl singlePartMIMEReaderCallback =
          new SinglePartMIMEDrainReaderCallbackImpl(singlePartMIMEReader);
      singlePartMIMEReader.registerReaderCallback(singlePartMIMEReaderCallback);
      _singlePartMIMEReaderCallbacks.add(singlePartMIMEReaderCallback);

      if (_drainValue.equalsIgnoreCase(SINGLE_ALL) || _drainValue.equalsIgnoreCase(SINGLE_PARTIAL_TOP_REMAINING))
      {
        singlePartMIMEReader.drainPart();
        return;
      }

      if (_drainValue.equalsIgnoreCase(SINGLE_ALTERNATE) || _drainValue.equalsIgnoreCase(SINGLE_ALTERNATE_TOP_REMAINING))
      {
        if (SinglePartMIMEDrainReaderCallbackImpl.partCounter % 2 == 1)
        {
          singlePartMIMEReader.drainPart();
        }
        else
        {
          singlePartMIMEReader.requestPartData();
        }
      }
    }

    @Override
    public void onFinished()
    {
      //Happens for SINGLE_ALL_NO_CALLBACK, SINGLE_ALL and SINGLE_ALTERNATE
      _responseHeaders.put(DRAIN_HEADER, "onFinished" + _drainValue);
      _latch.countDown();
    }

    @Override
    public void onDrainComplete()
    {
      //Happens for TOP_ALL, SINGLE_PARTIAL_TOP_REMAINING and SINGLE_ALTERNATE_TOP_REMAINING
      _responseHeaders.put(DRAIN_HEADER, "onDrainComplete" + _drainValue);
      _latch.countDown();
    }

    @Override
    public void onStreamError(Throwable throwable)
    {
      Assert.fail();
    }
  }
}
