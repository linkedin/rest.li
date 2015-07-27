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


import com.linkedin.common.callback.Callback;
import com.linkedin.multipart.utils.MIMETestUtils;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.annotations.Test;

import static com.linkedin.multipart.utils.MIMETestUtils.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Represents a test where we alternate between chaining and consuming a
 * {@link com.linkedin.multipart.MultiPartMIMEReader.SinglePartMIMEReader}.
 *
 * @author Karim Vidhani
 */
public class TestMIMEChainingAlternate extends AbstractMIMEUnitTest
{
  //This test has the server alternate between consuming a part and sending a part as a data source
  //to a writer.
  //Since we have four parts, the server will consume the 2nd and 4th and send out the 1st and 3rd.
  //To make the test easier we will have two callbacks to send to the server to indicate
  //the presence of each data source.
  //This violates the typical client/server http pattern, but accomplishes the purpose of this test
  //and it makes it easier to write.
  @Test(dataProvider = "chunkSizes")
  public void testAlternateSinglePartDataSource(final int chunkSize) throws Exception
  {
    final List<MultiPartMIMEDataSourceWriter> dataSources = generateInputStreamDataSources(chunkSize, _scheduledExecutorService);

    final MultiPartMIMEWriter writer = new MultiPartMIMEWriter.Builder().appendDataSources(dataSources).build();

    final StreamRequest streamRequest = mock(StreamRequest.class);
    when(streamRequest.getEntityStream()).thenReturn(writer.getEntityStream());
    final String contentTypeHeader = "multipart/mixed; boundary=" + writer.getBoundary();
    when(streamRequest.getHeader(MultiPartMIMEUtils.CONTENT_TYPE_HEADER)).thenReturn(contentTypeHeader);

    //Client side preparation to read the part back on the callback.
    //We have two callbacks here since we will get two responses.
    //Note the chunks size will carry over since the client is controlling how much data he gets back
    //based on the chunk size he writes.
    MIMETestUtils.MultiPartMIMEFullReaderCallback clientReceiverA = new MIMETestUtils.MultiPartMIMEFullReaderCallback();
    MIMETestUtils.MultiPartMIMEFullReaderCallback clientReceiverB = new MIMETestUtils.MultiPartMIMEFullReaderCallback();
    Callback<StreamResponse> callbackA = generateSuccessChainCallback(clientReceiverA);
    Callback<StreamResponse> callbackB = generateSuccessChainCallback(clientReceiverB);

    //Server side start
    MultiPartMIMEReader reader = MultiPartMIMEReader.createAndAcquireStream(streamRequest);
    final CountDownLatch latch = new CountDownLatch(1);
    ServerMultiPartMIMEAlternatorCallback
        serverSender = new ServerMultiPartMIMEAlternatorCallback(latch, callbackA, callbackB);
    reader.registerReaderCallback(serverSender);

    latch.await(_testTimeout, TimeUnit.MILLISECONDS);

    //Verify client
    Assert.assertEquals(clientReceiverA.getSinglePartMIMEReaderCallbacks().size(), 1);
    Assert.assertEquals(clientReceiverA.getSinglePartMIMEReaderCallbacks().get(0).getFinishedData(), BODY_A.getPartData());
    Assert.assertEquals(clientReceiverA.getSinglePartMIMEReaderCallbacks().get(0).getHeaders(), BODY_A.getPartHeaders());

    Assert.assertEquals(clientReceiverB.getSinglePartMIMEReaderCallbacks().size(), 1);
    Assert.assertEquals(clientReceiverB.getSinglePartMIMEReaderCallbacks().get(0).getFinishedData(), BODY_C.getPartData());
    Assert.assertEquals(clientReceiverB.getSinglePartMIMEReaderCallbacks().get(0).getHeaders(), BODY_C.getPartHeaders());

    //Verify server
    Assert.assertEquals(serverSender.getSinglePartMIMEReaderCallbacks().size(), 2);
    Assert.assertEquals(serverSender.getSinglePartMIMEReaderCallbacks().get(0).getFinishedData(), BODY_B.getPartData());
    Assert.assertEquals(serverSender.getSinglePartMIMEReaderCallbacks().get(0).getHeaders(), BODY_B.getPartHeaders());
    Assert.assertEquals(serverSender.getSinglePartMIMEReaderCallbacks().get(1).getFinishedData(), BODY_D.getPartData());
    Assert.assertEquals(serverSender.getSinglePartMIMEReaderCallbacks().get(1).getHeaders(), BODY_D.getPartHeaders());
  }

  private Callback<StreamResponse> generateSuccessChainCallback(final MIMETestUtils.MultiPartMIMEFullReaderCallback receiver)
  {
    return new Callback<StreamResponse>()
    {
      @Override
      public void onError(Throwable e)
      {
        Assert.fail();
      }

      @Override
      public void onSuccess(StreamResponse result)
      {
        MultiPartMIMEReader reader = MultiPartMIMEReader.createAndAcquireStream(result);
        reader.registerReaderCallback(receiver);
      }
    };
  }

  private static class ServerMultiPartMIMEAlternatorCallback implements MultiPartMIMEReaderCallback
  {
    final CountDownLatch _latch;
    final Callback<StreamResponse> _callbackA;
    final Callback<StreamResponse> _callbackB;
    final List<SinglePartMIMEFullReaderCallback> _singlePartMIMEReaderCallbacks = new ArrayList<SinglePartMIMEFullReaderCallback>();
    int _currentPart = 0;

    ServerMultiPartMIMEAlternatorCallback(final CountDownLatch latch, final Callback<StreamResponse> callbackA,
                                          final Callback<StreamResponse> callbackB)
    {
      _latch = latch;
      _callbackA = callbackA;
      _callbackB = callbackB;
    }

    public List<SinglePartMIMEFullReaderCallback> getSinglePartMIMEReaderCallbacks()
    {
      return _singlePartMIMEReaderCallbacks;
    }

    @Override
    public void onNewPart(MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader)
    {
      _currentPart++;
      if (_currentPart == 1)
      {
        final MultiPartMIMEWriter writer =
            new MultiPartMIMEWriter.Builder().appendDataSource(singlePartMIMEReader).build();

        final StreamResponse streamResponse = mock(StreamResponse.class);
        when(streamResponse.getEntityStream()).thenReturn(writer.getEntityStream());
        final String contentTypeHeader = "multipart/mixed; boundary=" + writer.getBoundary();
        when(streamResponse.getHeader(MultiPartMIMEUtils.CONTENT_TYPE_HEADER)).thenReturn(contentTypeHeader);
        _callbackA.onSuccess(streamResponse);
      }
      else if (_currentPart == 3)
      {
        final MultiPartMIMEWriter writer = new MultiPartMIMEWriter.Builder().appendDataSource(singlePartMIMEReader).build();

        final StreamResponse streamResponse = mock(StreamResponse.class);
        when(streamResponse.getEntityStream()).thenReturn(writer.getEntityStream());
        final String contentTypeHeader = "multipart/mixed; boundary=" + writer.getBoundary();
        when(streamResponse.getHeader(MultiPartMIMEUtils.CONTENT_TYPE_HEADER)).thenReturn(contentTypeHeader);
        _callbackB.onSuccess(streamResponse);
      }
      else
      {
        //Consume 2 and 4
        SinglePartMIMEFullReaderCallback singlePartMIMEReaderCallback = new SinglePartMIMEFullReaderCallback(singlePartMIMEReader);
        singlePartMIMEReader.registerReaderCallback(singlePartMIMEReaderCallback);
        _singlePartMIMEReaderCallbacks.add(singlePartMIMEReaderCallback);
        singlePartMIMEReader.requestPartData();
      }
    }

    @Override
    public void onFinished()
    {
      //Now we can assert everywhere
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