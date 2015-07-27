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

import static com.linkedin.multipart.utils.MIMETestUtils.SinglePartMIMEFullReaderCallback;
import static com.linkedin.multipart.utils.MIMETestUtils.BODY_A;
import static com.linkedin.multipart.utils.MIMETestUtils.BODY_B;
import static com.linkedin.multipart.utils.MIMETestUtils.BODY_C;
import static com.linkedin.multipart.utils.MIMETestUtils.BODY_D;
import static com.linkedin.multipart.utils.MIMETestUtils.generateInputStreamDataSources;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Tests sending a {@link com.linkedin.multipart.MultiPartMIMEReader.SinglePartMIMEReader} as a
 * data source to a {@link com.linkedin.multipart.MultiPartMIMEWriter}
 *
 * @author Karim Vidhani
 */
public class TestMIMEChainingSinglePart extends AbstractMIMEUnitTest
{
  //Verifies that a single part mime reader can be used as a data source to the writer.
  //To make the test easier to write, we simply chain back to the client in the form of simulating a response.
  @Test(dataProvider = "chunkSizes")
  public void testSinglePartDataSource(final int chunkSize) throws Exception
  {
    final List<MultiPartMIMEDataSourceWriter> dataSources = generateInputStreamDataSources(chunkSize, _scheduledExecutorService);

    final MultiPartMIMEWriter writer = new MultiPartMIMEWriter.Builder().appendDataSources(dataSources).build();

    final StreamRequest streamRequest = mock(StreamRequest.class);
    when(streamRequest.getEntityStream()).thenReturn(writer.getEntityStream());
    final String contentTypeHeader = "multipart/mixed; boundary=" + writer.getBoundary();
    when(streamRequest.getHeader(MultiPartMIMEUtils.CONTENT_TYPE_HEADER)).thenReturn(contentTypeHeader);

    //Client side preparation to read the part back on the callback
    //Note the chunks size will carry over since the client is controlling how much data he gets back
    //based on the chunk size he writes.
    MIMETestUtils.MultiPartMIMEFullReaderCallback clientReceiver = new MIMETestUtils.MultiPartMIMEFullReaderCallback();
    Callback<StreamResponse> callback = generateSuccessChainCallback(clientReceiver);

    //Server side start
    MultiPartMIMEReader reader = MultiPartMIMEReader.createAndAcquireStream(streamRequest);
    final CountDownLatch latch = new CountDownLatch(1);
    ServerMultiPartMIMEReaderSinglePartSenderCallback
        serverSender = new ServerMultiPartMIMEReaderSinglePartSenderCallback(latch, callback);
    reader.registerReaderCallback(serverSender);

    latch.await(_testTimeout, TimeUnit.MILLISECONDS);

    //Verify client
    Assert.assertEquals(clientReceiver.getSinglePartMIMEReaderCallbacks().size(), 1);
    Assert.assertEquals(clientReceiver.getSinglePartMIMEReaderCallbacks().get(0).getFinishedData(), BODY_A.getPartData());
    Assert.assertEquals(clientReceiver.getSinglePartMIMEReaderCallbacks().get(0).getHeaders(), BODY_A.getPartHeaders());

    //Verify server
    List<MIMETestUtils.SinglePartMIMEFullReaderCallback> singlePartMIMEReaderCallbacks = serverSender.getSinglePartMIMEReaderCallbacks();
    Assert.assertEquals(singlePartMIMEReaderCallbacks.size(), 3);
    Assert.assertEquals(singlePartMIMEReaderCallbacks.get(0).getFinishedData(), BODY_B.getPartData());
    Assert.assertEquals(singlePartMIMEReaderCallbacks.get(0).getHeaders(), BODY_B.getPartHeaders());
    Assert.assertEquals(singlePartMIMEReaderCallbacks.get(1).getFinishedData(), BODY_C.getPartData());
    Assert.assertEquals(singlePartMIMEReaderCallbacks.get(1).getHeaders(), BODY_C.getPartHeaders());
    Assert.assertEquals(singlePartMIMEReaderCallbacks.get(2).getFinishedData(), BODY_D.getPartData());
    Assert.assertEquals(singlePartMIMEReaderCallbacks.get(2).getHeaders(), BODY_D.getPartHeaders());
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

  //Server callback:
  private static class ServerMultiPartMIMEReaderSinglePartSenderCallback implements MultiPartMIMEReaderCallback
  {
    final CountDownLatch _latch;
    boolean _firstPartEchoed = false;
    final Callback<StreamResponse> _callback;
    final List<MIMETestUtils.SinglePartMIMEFullReaderCallback> _singlePartMIMEReaderCallbacks =
        new ArrayList<MIMETestUtils.SinglePartMIMEFullReaderCallback>();

    ServerMultiPartMIMEReaderSinglePartSenderCallback(final CountDownLatch latch,
                                                      final Callback<StreamResponse> callback)
    {
      _latch = latch;
      _callback = callback;
    }

    public List<SinglePartMIMEFullReaderCallback> getSinglePartMIMEReaderCallbacks()
    {
      return _singlePartMIMEReaderCallbacks;
    }

    @Override
    public void onNewPart(MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader)
    {
      if (!_firstPartEchoed)
      {
        _firstPartEchoed = true;
        final MultiPartMIMEWriter writer = new MultiPartMIMEWriter.Builder().appendDataSource(singlePartMIMEReader).build();

        final StreamResponse streamResponse = mock(StreamResponse.class);
        when(streamResponse.getEntityStream()).thenReturn(writer.getEntityStream());
        final String contentTypeHeader = "multipart/mixed; boundary=" + writer.getBoundary();
        when(streamResponse.getHeader(MultiPartMIMEUtils.CONTENT_TYPE_HEADER)).thenReturn(contentTypeHeader);
        _callback.onSuccess(streamResponse);
      }
      else
      {
        MIMETestUtils.SinglePartMIMEFullReaderCallback singlePartMIMEReaderCallback =
            new MIMETestUtils.SinglePartMIMEFullReaderCallback(singlePartMIMEReader);
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