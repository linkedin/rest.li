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
 * Unit tests that mock R2 for testing the {@link com.linkedin.multipart.MultiPartMIMEReader}
 *
 * @author Karim Vidhani
 */
public class TestMIMEReader extends AbstractMIMEUnitTest
{
  private MultiPartMIMEReader _reader;

  @DataProvider(name = "eachSingleBodyDataSource")
  public Object[][] eachSingleBodyDataSource() throws Exception
  {
    return new Object[][]
        {
            {1, SMALL_DATA_SOURCE},
            {R2Constants.DEFAULT_DATA_CHUNK_SIZE, SMALL_DATA_SOURCE},
            {1, LARGE_DATA_SOURCE},
            {R2Constants.DEFAULT_DATA_CHUNK_SIZE, LARGE_DATA_SOURCE},
            {1, HEADER_LESS_BODY},
            {R2Constants.DEFAULT_DATA_CHUNK_SIZE, HEADER_LESS_BODY},
            {1, BODY_LESS_BODY},
            {R2Constants.DEFAULT_DATA_CHUNK_SIZE, BODY_LESS_BODY},
            {1, BYTES_BODY},
            {R2Constants.DEFAULT_DATA_CHUNK_SIZE, BYTES_BODY},
            {1, PURELY_EMPTY_BODY},
            {R2Constants.DEFAULT_DATA_CHUNK_SIZE, PURELY_EMPTY_BODY}
        };
  }

  @Test(dataProvider = "eachSingleBodyDataSource")
  public void testEachSingleBodyDataSource(final int chunkSize, final MimeBodyPart bodyPart) throws Exception
  {
    MimeMultipart multiPartMimeBody = new MimeMultipart();

    //Add your body parts
    multiPartMimeBody.addBodyPart(bodyPart);
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    multiPartMimeBody.writeTo(byteArrayOutputStream);
    final ByteString requestPayload = ByteString.copy(byteArrayOutputStream.toByteArray());
    executeRequestAndAssert(trimTrailingCRLF(requestPayload), chunkSize, multiPartMimeBody);
  }

  @Test(dataProvider = "eachSingleBodyDataSource")
  public void testEachSingleBodyDataSourceMultipleTimes(final int chunkSize, final MimeBodyPart bodyPart)
      throws Exception
  {
    MimeMultipart multiPartMimeBody = new MimeMultipart();

    //Add your body parts
    multiPartMimeBody.addBodyPart(bodyPart);
    multiPartMimeBody.addBodyPart(bodyPart);
    multiPartMimeBody.addBodyPart(bodyPart);
    multiPartMimeBody.addBodyPart(bodyPart);
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    multiPartMimeBody.writeTo(byteArrayOutputStream);
    final ByteString requestPayload = ByteString.copy(byteArrayOutputStream.toByteArray());
    executeRequestAndAssert(trimTrailingCRLF(requestPayload), chunkSize, multiPartMimeBody);
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  @DataProvider(name = "multipleNormalBodiesDataSource")
  public Object[][] multipleNormalBodiesDataSource() throws Exception
  {
    final List<MimeBodyPart> bodyPartList = new ArrayList<MimeBodyPart>();
    bodyPartList.add(LARGE_DATA_SOURCE);
    bodyPartList.add(SMALL_DATA_SOURCE);
    bodyPartList.add(BODY_LESS_BODY);
    bodyPartList.add(LARGE_DATA_SOURCE);
    bodyPartList.add(SMALL_DATA_SOURCE);
    bodyPartList.add(BODY_LESS_BODY);

    //For this particular data source, we will use a variety of chunk sizes to cover all edge cases.
    //This is particularly useful due to the way we decompose ByteStrings when creating data
    //for our clients. Such chunk sizes allow us to make sure that our decomposing logic works as intended.
    //We use chunk sizes based off prime numbers for good distribution.
    final List<Integer> primeNumberList = generatePrimeNumbers(100);

    //Make space at the end for the R2 default chunk size.
    final Object[][] multipleChunkPayloads = new Object[primeNumberList.size() + 1][];

    for (int i = 0; i < primeNumberList.size(); i++)
    {
      multipleChunkPayloads[i] = new Object[2];
      multipleChunkPayloads[i][0] = primeNumberList.get(i);
      multipleChunkPayloads[i][1] = bodyPartList;
    }

    final int lastIndex = primeNumberList.size();

    multipleChunkPayloads[lastIndex] = new Object[2];
    multipleChunkPayloads[lastIndex][0] = R2Constants.DEFAULT_DATA_CHUNK_SIZE;
    multipleChunkPayloads[lastIndex][1] = bodyPartList;

    return multipleChunkPayloads;
  }

  @Test(dataProvider = "multipleNormalBodiesDataSource")
  public void testMultipleNormalBodiesDataSource(final int chunkSize, final List<MimeBodyPart> bodyPartList)
      throws Exception
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
    executeRequestAndAssert(trimTrailingCRLF(requestPayload), chunkSize, multiPartMimeBody);
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  @DataProvider(name = "multipleAbnormalBodies")
  public Object[][] multipleAbnormalBodies() throws Exception
  {
    final List<MimeBodyPart> bodyPartList = new ArrayList<MimeBodyPart>();
    bodyPartList.add(HEADER_LESS_BODY);
    bodyPartList.add(BODY_LESS_BODY);
    bodyPartList.add(PURELY_EMPTY_BODY);

    return new Object[][]
        {
            {1, bodyPartList}, {R2Constants.DEFAULT_DATA_CHUNK_SIZE, bodyPartList}
        };
  }

  @Test(dataProvider = "multipleAbnormalBodies")
  public void testMultipleAbnormalBodies(final int chunkSize, final List<MimeBodyPart> bodyPartList) throws Exception
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
    executeRequestAndAssert(trimTrailingCRLF(requestPayload), chunkSize, multiPartMimeBody);
  }

  ///////////////////////////////////////////////////////////////////////////////////////

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
  public void testAllTypesOfBodiesDataSource(final int chunkSize, final List<MimeBodyPart> bodyPartList)
      throws Exception
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
    executeRequestAndAssert(trimTrailingCRLF(requestPayload), chunkSize, multiPartMimeBody);
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  @DataProvider(name = "preambleEpilogueDataSource")
  public Object[][] preambleEpilogueDataSource() throws Exception
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
            {1, bodyPartList, null, null},
            {R2Constants.DEFAULT_DATA_CHUNK_SIZE, bodyPartList, null, null},
            {1, bodyPartList, "Some preamble", "Some epilogue"},
            {R2Constants.DEFAULT_DATA_CHUNK_SIZE, bodyPartList, "Some preamble", "Some epilogue"},
            {1, bodyPartList, "Some preamble", null},
            {R2Constants.DEFAULT_DATA_CHUNK_SIZE, bodyPartList, "Some preamble", null},
            {1, bodyPartList, null, "Some epilogue"},
            {R2Constants.DEFAULT_DATA_CHUNK_SIZE, bodyPartList, null, "Some epilogue"}
        };
  }

  //Just test the preamble and epilogue here
  @Test(dataProvider = "preambleEpilogueDataSource")
  public void testPreambleAndEpilogue(final int chunkSize, final List<MimeBodyPart> bodyPartList, final String preamble,
      final String epilogue) throws Exception
  {
    MimeMultipart multiPartMimeBody = new MimeMultipart();

    //Add your body parts
    for (final MimeBodyPart bodyPart : bodyPartList)
    {
      multiPartMimeBody.addBodyPart(bodyPart);
    }

    if (preamble != null)
    {
      multiPartMimeBody.setPreamble(preamble);
    }

    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    multiPartMimeBody.writeTo(byteArrayOutputStream);

    final ByteString requestPayload;
    if (epilogue != null)
    {
      //Javax mail does not support epilogue so we add it ourselves (other then the CRLF following the final
      //boundary).
      byteArrayOutputStream.write(epilogue.getBytes());
      requestPayload = ByteString.copy(byteArrayOutputStream.toByteArray());
    }
    else
    {
      //Our test desired no epilogue.
      //Remove the CRLF introduced by javax mail at the end. We won't want a fake epilogue.
      requestPayload = trimTrailingCRLF(ByteString.copy(byteArrayOutputStream.toByteArray()));
    }

    executeRequestAndAssert(requestPayload, chunkSize, multiPartMimeBody);
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  //Special test to make sure we don't stack overflow.
  //Have tons of small parts that are all read in at once due to the huge chunk size.
  @Test
  public void testStackOverflow() throws Exception
  {
    MimeMultipart multiPartMimeBody = new MimeMultipart();
    _testTimeout = 600000;

    //Add many tiny bodies. Since everything comes into memory on the first chunk, we will interact exclusively with the
    //client and not R2. We want to make sure that us calling them, and them calling us back, and us calling them over and over
    //does not lead to a stack overflow.
    for (int i = 0; i < 5000; i++)
    {
      multiPartMimeBody.addBodyPart(TINY_DATA_SOURCE);
    }

    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    multiPartMimeBody.writeTo(byteArrayOutputStream);
    final ByteString requestPayload = ByteString.copy(byteArrayOutputStream.toByteArray());
    executeRequestAndAssert(trimTrailingCRLF(requestPayload), Integer.MAX_VALUE, multiPartMimeBody);
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  //This test will verify, that once we are successfully finished, that if R2 gives us onError() we don't let the client know.
  @Test(dataProvider = "allTypesOfBodiesDataSource")
  public void alreadyFinishedPreventErrorClient(final int chunkSize, final List<MimeBodyPart> bodyPartList) throws Exception
  {
    testAllTypesOfBodiesDataSource(chunkSize, bodyPartList);

    //The asserts in the callback will make sure that we don't call onStreamError() on the callbacks.
    //Also we have already verified that _rh.cancel() did not occur.
    _reader.getR2MultiPartMIMEReader().onError(new NullPointerException());
  }

  ///////////////////////////////////////////////////////////////////////////////////////
  private void executeRequestAndAssert(final ByteString payload, final int chunkSize, final MimeMultipart mimeMultipart)
      throws Exception
  {
    mockR2AndWrite(payload, chunkSize, mimeMultipart.getContentType());
    final CountDownLatch latch = new CountDownLatch(1);

    //We simulate _client.streamRequest(request, callback);
    _reader = MultiPartMIMEReader.createAndAcquireStream(_streamRequest);
    MultiPartMIMEReaderCallbackImpl testMultiPartMIMEReaderCallback = new MultiPartMIMEReaderCallbackImpl(latch);
    _reader.registerReaderCallback(testMultiPartMIMEReaderCallback);

    latch.await(_testTimeout, TimeUnit.MILLISECONDS);

    //Verify this is unusable.
    try
    {
      _reader.abandonAllParts();
      Assert.fail();
    }
    catch (MultiPartReaderFinishedException multiPartReaderFinishedException)
    {
      //pass
    }

    List<SinglePartMIMEReaderCallbackImpl> singlePartMIMEReaderCallbacks = testMultiPartMIMEReaderCallback._singlePartMIMEReaderCallbacks;
    Assert.assertEquals(singlePartMIMEReaderCallbacks.size(), mimeMultipart.getCount());
    for (int i = 0; i < singlePartMIMEReaderCallbacks.size(); i++)
    {
      //Actual
      final SinglePartMIMEReaderCallbackImpl currentCallback = singlePartMIMEReaderCallbacks.get(i);
      //Expected
      final BodyPart currentExpectedPart = mimeMultipart.getBodyPart(i);

      //Construct expected headers and verify they match
      final Map<String, String> expectedHeaders = new HashMap<String, String>();
      @SuppressWarnings("unchecked")
      final Enumeration<Header> allHeaders = currentExpectedPart.getAllHeaders();
      while (allHeaders.hasMoreElements())
      {
        final Header header = allHeaders.nextElement();
        expectedHeaders.put(header.getName(), header.getValue());
      }
      Assert.assertEquals(currentCallback._headers, expectedHeaders);

      Assert.assertNotNull(currentCallback._finishedData);
      //Verify the body matches
      if (currentExpectedPart.getContent() instanceof byte[])
      {
        Assert.assertEquals(currentCallback._finishedData.copyBytes(), currentExpectedPart.getContent());
      }
      else
      {
        //Default is String
        Assert.assertEquals(new String(currentCallback._finishedData.copyBytes()), currentExpectedPart.getContent());
      }
    }

    Assert.assertTrue(_reader.haveAllPartsFinished());

    //Mock verifies
    verify(_streamRequest, times(1)).getEntityStream();
    verify(_streamRequest, times(1)).getHeader(HEADER_CONTENT_TYPE);
    verify(_entityStream, times(1)).setReader(isA(MultiPartMIMEReader.R2MultiPartMIMEReader.class));
    final int expectedRequests = (int) Math.ceil((double) payload.length() / chunkSize);
    //One more expected request because we have to make the last call to get called onDone().
    verify(_readHandle, times(expectedRequests + 1)).request(1);
    verifyNoMoreInteractions(_streamRequest);
    verifyNoMoreInteractions(_entityStream);
    verifyNoMoreInteractions(_readHandle);
  }

  private static class SinglePartMIMEReaderCallbackImpl implements SinglePartMIMEReaderCallback
  {
    final MultiPartMIMEReaderCallback _topLevelCallback;
    final MultiPartMIMEReader.SinglePartMIMEReader _singlePartMIMEReader;
    final ByteArrayOutputStream _byteArrayOutputStream = new ByteArrayOutputStream();
    Map<String, String> _headers;
    ByteString _finishedData = null;

    SinglePartMIMEReaderCallbackImpl(final MultiPartMIMEReaderCallback topLevelCallback,
                                     final MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader)
    {
      _topLevelCallback = topLevelCallback;
      _singlePartMIMEReader = singlePartMIMEReader;
      _headers = singlePartMIMEReader.dataSourceHeaders();
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
        onStreamError(ioException);
      }
      _singlePartMIMEReader.requestPartData();
    }

    @Override
    public void onFinished()
    {
      //Verify that upon finishing that this is reader is no longer usable.
      try
      {
        _singlePartMIMEReader.abandonPart();
        Assert.fail();
      }
      catch (SinglePartFinishedException singlePartFinishedException)
      {
      }

      _finishedData = ByteString.copy(_byteArrayOutputStream.toByteArray());
    }

    //Delegate to the top level for now for these two
    @Override
    public void onAbandoned()
    {
      //This will end up failing the test.
      _topLevelCallback.onAbandoned();
    }

    @Override
    public void onStreamError(Throwable throwable)
    {
      Assert.fail();
    }
  }

  private static class MultiPartMIMEReaderCallbackImpl implements MultiPartMIMEReaderCallback
  {
    final CountDownLatch _latch;
    final List<SinglePartMIMEReaderCallbackImpl> _singlePartMIMEReaderCallbacks = new ArrayList<SinglePartMIMEReaderCallbackImpl>();

    MultiPartMIMEReaderCallbackImpl(final CountDownLatch latch)
    {
      _latch = latch;
    }

    @Override
    public void onNewPart(MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader)
    {
      SinglePartMIMEReaderCallbackImpl singlePartMIMEReaderCallback = new SinglePartMIMEReaderCallbackImpl(this,
                                                                                                           singlePartMIMEReader);
      singlePartMIMEReader.registerReaderCallback(singlePartMIMEReaderCallback);
      _singlePartMIMEReaderCallbacks.add(singlePartMIMEReaderCallback);
      singlePartMIMEReader.requestPartData();
    }

    @Override
    public void onFinished()
    {
      _latch.countDown();
    }

    @Override
    public void onAbandoned()
    {
      Assert.fail();
    }

    @Override
    public void onStreamError(Throwable throwable)
    {
      Assert.fail();
    }
  }
}