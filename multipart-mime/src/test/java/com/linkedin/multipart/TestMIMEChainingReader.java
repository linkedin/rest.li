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
 * Test passing a {@link com.linkedin.multipart.MultiPartMIMEReader} as a data source
 * to the {@link com.linkedin.multipart.MultiPartMIMEWriter}.
 *
 * @author Karim Vidhani
 */
public class TestMIMEChainingReader extends AbstractMIMEUnitTest
{
  //Verifies that a multi part mime reader can be used as a data source to the writer.
  //To make the test easier to write, we simply chain back to the client in the form of simulating a response.
  @Test(dataProvider = "chunkSizes")
  public void testMimeReaderDataSource(final int chunkSize) throws Exception
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
    final CountDownLatch latch = new CountDownLatch(1);
    ClientMultiPartMIMEReaderReceiverCallback _clientReceiver = new ClientMultiPartMIMEReaderReceiverCallback(latch);
    Callback<StreamResponse> callback = generateSuccessChainCallback(_clientReceiver);

    //Server side start
    MultiPartMIMEReader reader = MultiPartMIMEReader.createAndAcquireStream(streamRequest);
    ServerMultiPartMIMEChainReaderWriterCallback
        _serverSender = new ServerMultiPartMIMEChainReaderWriterCallback(callback, reader);
    reader.registerReaderCallback(_serverSender);

    latch.await(_testTimeout, TimeUnit.MILLISECONDS);

    //Verify client. No need to verify the server.
    List<MIMETestUtils.SinglePartMIMEFullReaderCallback> singlePartMIMEReaderCallbacks = _clientReceiver.getSinglePartMIMEReaderCallbacks();

    Assert.assertEquals(singlePartMIMEReaderCallbacks.size(), 4);
    Assert.assertEquals(singlePartMIMEReaderCallbacks.get(0).getFinishedData(), BODY_A.getPartData());
    Assert.assertEquals(singlePartMIMEReaderCallbacks.get(0).getHeaders(), BODY_A.getPartHeaders());
    Assert.assertEquals(singlePartMIMEReaderCallbacks.get(1).getFinishedData(), BODY_B.getPartData());
    Assert.assertEquals(singlePartMIMEReaderCallbacks.get(1).getHeaders(), BODY_B.getPartHeaders());
    Assert.assertEquals(singlePartMIMEReaderCallbacks.get(2).getFinishedData(), BODY_C.getPartData());
    Assert.assertEquals(singlePartMIMEReaderCallbacks.get(2).getHeaders(), BODY_C.getPartHeaders());
    Assert.assertEquals(singlePartMIMEReaderCallbacks.get(3).getFinishedData(), BODY_D.getPartData());
    Assert.assertEquals(singlePartMIMEReaderCallbacks.get(3).getHeaders(), BODY_D.getPartHeaders());
  }

  private Callback<StreamResponse> generateSuccessChainCallback(final ClientMultiPartMIMEReaderReceiverCallback receiver)
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

  //Client callbacks:
  private static class ClientMultiPartMIMEReaderReceiverCallback implements MultiPartMIMEReaderCallback
  {
    final List<MIMETestUtils.SinglePartMIMEFullReaderCallback> _singlePartMIMEReaderCallbacks =
        new ArrayList<MIMETestUtils.SinglePartMIMEFullReaderCallback>();
    final CountDownLatch _latch;

    ClientMultiPartMIMEReaderReceiverCallback(final CountDownLatch latch)
    {
      _latch = latch;
    }

    public List<SinglePartMIMEFullReaderCallback> getSinglePartMIMEReaderCallbacks()
    {
      return _singlePartMIMEReaderCallbacks;
    }

    @Override
    public void onNewPart(MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader)
    {
      MIMETestUtils.SinglePartMIMEFullReaderCallback singlePartMIMEReaderCallback =
          new MIMETestUtils.SinglePartMIMEFullReaderCallback(singlePartMIMEReader);
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

  //Server callback. Only the top level needed:
  private static class ServerMultiPartMIMEChainReaderWriterCallback implements MultiPartMIMEReaderCallback
  {
    final Callback<StreamResponse> _callback;
    final MultiPartMIMEReader _reader;

    ServerMultiPartMIMEChainReaderWriterCallback(final Callback<StreamResponse> callback,
                                                 final MultiPartMIMEReader reader)
    {
      _callback = callback;
      _reader = reader;
    }

    @Override
    public void onNewPart(MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader)
    {
      final MultiPartMIMEWriter writer = new MultiPartMIMEWriter.Builder().appendDataSourceIterator(_reader).build();
      final StreamResponse streamResponse = mock(StreamResponse.class);
      when(streamResponse.getEntityStream()).thenReturn(writer.getEntityStream());
      final String contentTypeHeader = "multipart/mixed; boundary=" + writer.getBoundary();
      when(streamResponse.getHeader(MultiPartMIMEUtils.CONTENT_TYPE_HEADER)).thenReturn(contentTypeHeader);
      _callback.onSuccess(streamResponse);
    }

    @Override
    public void onFinished()
    {
      //Based on the current implementation, this will not be called.
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