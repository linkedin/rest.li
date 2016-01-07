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

package com.linkedin.multipart.integ;


import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.data.ByteString;
import com.linkedin.multipart.MultiPartMIMEDataSourceWriter;
import com.linkedin.multipart.MultiPartMIMEInputStream;
import com.linkedin.multipart.MultiPartMIMEReader;
import com.linkedin.multipart.MultiPartMIMEReaderCallback;
import com.linkedin.multipart.MultiPartMIMEStreamRequestFactory;
import com.linkedin.multipart.MultiPartMIMEStreamResponseFactory;
import com.linkedin.multipart.MultiPartMIMEWriter;
import com.linkedin.multipart.exceptions.MultiPartIllegalFormatException;
import com.linkedin.multipart.utils.MIMETestUtils;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.StreamRequestHandler;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.server.HttpServer;
import com.linkedin.r2.transport.http.server.HttpServerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.multipart.utils.MIMETestUtils.*;


/**
 * Represents a test where we write an envelope containing a {@link com.linkedin.multipart.MultiPartMIMEReader.SinglePartMIMEReader},
 * a {@link com.linkedin.multipart.MultiPartMIMEReader} and a {@link com.linkedin.multipart.MultiPartMIMEInputStream}.
 *
 * @author Karim Vidhani
 */
public class TestMIMEChainingMultipleSources
{
  private static final int PORT_SERVER_A = 8450;
  private static final int PORT_SERVER_B = 8451;
  private static final URI SERVER_A_URI = URI.create("/serverA");
  private static final URI SERVER_B_URI = URI.create("/serverB");
  private static final int TEST_TIMEOUT = 30000;
  private TransportClientFactory _clientFactory;
  private HttpServer _serverA;
  private HttpServer _serverB;
  private Client _client;
  private Client _server_A_client;
  private CountDownLatch _latch;
  private ServerAMultiPartCallback _serverAMultiPartCallback;
  private int _chunkSize;
  private ScheduledExecutorService _scheduledExecutorService;

  @BeforeClass
  public void threadPoolSetup()
  {
    _scheduledExecutorService = Executors.newScheduledThreadPool(30);
  }

  @AfterClass
  public void threadPoolTearDown()
  {
    _scheduledExecutorService.shutdownNow();
  }

  @BeforeMethod
  public void setup() throws IOException
  {
    _latch = new CountDownLatch(2);
    _clientFactory = new HttpClientFactory();
    _client = new TransportClientAdapter(_clientFactory.getClient(Collections.<String, String>emptyMap()));
    _server_A_client = new TransportClientAdapter(_clientFactory.getClient(Collections.<String, String>emptyMap()));

    final HttpServerFactory httpServerFactory = new HttpServerFactory();

    final ServerARequestHandler serverARequestHandler = new ServerARequestHandler();
    final TransportDispatcher serverATransportDispatcher =
        new TransportDispatcherBuilder().addStreamHandler(SERVER_A_URI, serverARequestHandler).build();

    final ServerBRequestHandler serverBRequestHandler = new ServerBRequestHandler();
    final TransportDispatcher serverBTransportDispatcher =
        new TransportDispatcherBuilder().addStreamHandler(SERVER_B_URI, serverBRequestHandler).build();

    _serverA = httpServerFactory.createServer(PORT_SERVER_A, serverATransportDispatcher, true);
    _serverB = httpServerFactory.createServer(PORT_SERVER_B, serverBTransportDispatcher, true);
    _serverA.start();
    _serverB.start();
  }

  @AfterMethod
  public void tearDown() throws Exception
  {
    final FutureCallback<None> clientShutdownCallback = new FutureCallback<None>();
    _client.shutdown(clientShutdownCallback);
    clientShutdownCallback.get();

    final FutureCallback<None> server1ClientShutdownCallback = new FutureCallback<None>();
    _server_A_client.shutdown(server1ClientShutdownCallback);
    server1ClientShutdownCallback.get();

    final FutureCallback<None> factoryShutdownCallback = new FutureCallback<None>();
    _clientFactory.shutdown(factoryShutdownCallback);
    factoryShutdownCallback.get();

    _serverA.stop();
    _serverA.waitForStop();
    _serverB.stop();
    _serverB.waitForStop();
  }

  @DataProvider(name = "chunkSizes")
  public Object[][] chunkSizes() throws Exception
  {
    return new Object[][]
        {
            {1}, {R2Constants.DEFAULT_DATA_CHUNK_SIZE}
        };
  }

  private class ServerARequestHandler implements StreamRequestHandler
  {
    ServerARequestHandler()
    {
    }

    @Override
    public void handleRequest(final StreamRequest request, RequestContext requestContext,
                              final Callback<StreamResponse> callback)
    {
      try
      {
        //1. Send a request to server B.
        //2. Get a MIME response back.
        //3. Tack on a local input stream (BODY_5).
        //4. Send the original incoming reader + local input stream + first part from the incoming response.
        //5. Drain the remaining parts from the response.
        //6. Count down the latch.

        final EntityStream emptyBody = EntityStreams.newEntityStream(new ByteStringWriter(ByteString.empty()));
        final StreamRequest simplePost =
            new StreamRequestBuilder(Bootstrap.createHttpsURI(PORT_SERVER_B, SERVER_B_URI)).setMethod("POST").build(emptyBody);

        //In this callback, when we get the response from Server B, we will create the compound writer
        Callback<StreamResponse> responseCallback = generateServerAResponseCallback(request, callback);

        //Send the request to Server A
        _client.streamRequest(simplePost, responseCallback);
      }
      catch (MultiPartIllegalFormatException illegalMimeFormatException)
      {
        RestException restException = new RestException(RestStatus.responseForError(400, illegalMimeFormatException));
        callback.onError(restException);
      }
    }
  }

  private Callback<StreamResponse> generateServerAResponseCallback(final StreamRequest incomingRequest,
                                                                   final Callback<StreamResponse> incomingRequestCallback)
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
        final MultiPartMIMEReader reader = MultiPartMIMEReader.createAndAcquireStream(result);
        _serverAMultiPartCallback = new ServerAMultiPartCallback(incomingRequest, incomingRequestCallback);
        reader.registerReaderCallback(_serverAMultiPartCallback);
      }
    };
  }

  private class ServerAMultiPartCallback implements MultiPartMIMEReaderCallback
  {
    final Callback<StreamResponse> _incomingRequestCallback;
    final StreamRequest _incomingRequest;
    boolean _firstPartConsumed = false;
    final List<MIMETestUtils.SinglePartMIMEFullReaderCallback> _singlePartMIMEReaderCallbacks =
        new ArrayList<MIMETestUtils.SinglePartMIMEFullReaderCallback>();

    ServerAMultiPartCallback(final StreamRequest incomingRequest, final Callback<StreamResponse> callback)
    {
      _incomingRequest = incomingRequest;
      _incomingRequestCallback = callback;
    }

    public List<SinglePartMIMEFullReaderCallback> getSinglePartMIMEReaderCallbacks()
    {
      return _singlePartMIMEReaderCallbacks;
    }

    @Override
    public void onNewPart(MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader)
    {
      if (!_firstPartConsumed)
      {
        _firstPartConsumed = true;

        final MultiPartMIMEReader incomingRequestReader = MultiPartMIMEReader.createAndAcquireStream(_incomingRequest);

        final MultiPartMIMEInputStream localInputStream =
            new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(BODY_5.getPartData().copyBytes()),
                _scheduledExecutorService, BODY_5.getPartHeaders()).withWriteChunkSize(_chunkSize).build();

        final MultiPartMIMEWriter writer =
            new MultiPartMIMEWriter.Builder().appendDataSource(singlePartMIMEReader).appendDataSource(localInputStream)
                .appendDataSourceIterator(incomingRequestReader).build();

        final StreamResponse streamResponse =
            MultiPartMIMEStreamResponseFactory
                .generateMultiPartMIMEStreamResponse("mixed", writer, Collections.<String, String>emptyMap());
        _incomingRequestCallback.onSuccess(streamResponse);
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

  private class ServerBRequestHandler implements StreamRequestHandler
  {
    ServerBRequestHandler()
    {
    }

    @Override
    public void handleRequest(StreamRequest request, RequestContext requestContext,
        final Callback<StreamResponse> callback)
    {
      try
      {
        final MultiPartMIMEInputStream body1DataSource =
            new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(BODY_1.getPartData().copyBytes()),
                _scheduledExecutorService, BODY_1.getPartHeaders()).withWriteChunkSize(_chunkSize).build();

        final MultiPartMIMEInputStream body2DataSource =
            new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(BODY_2.getPartData().copyBytes()),
                _scheduledExecutorService, BODY_2.getPartHeaders()).withWriteChunkSize(_chunkSize).build();

        final MultiPartMIMEInputStream body3DataSource =
            new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(BODY_3.getPartData().copyBytes()),
                _scheduledExecutorService, BODY_3.getPartHeaders()).withWriteChunkSize(_chunkSize).build();

        final MultiPartMIMEInputStream body4DataSource =
            new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(BODY_4.getPartData().copyBytes()),
                _scheduledExecutorService, BODY_4.getPartHeaders()).withWriteChunkSize(_chunkSize).build();

        final List<MultiPartMIMEDataSourceWriter> dataSources = new ArrayList<MultiPartMIMEDataSourceWriter>();
        dataSources.add(body1DataSource);
        dataSources.add(body2DataSource);
        dataSources.add(body3DataSource);
        dataSources.add(body4DataSource);

        final MultiPartMIMEWriter writer = new MultiPartMIMEWriter.Builder().appendDataSources(dataSources).build();

        final StreamResponse streamResponse =
            MultiPartMIMEStreamResponseFactory.generateMultiPartMIMEStreamResponse("mixed", writer, Collections.<String, String>emptyMap());
        callback.onSuccess(streamResponse);
      }
      catch (MultiPartIllegalFormatException illegalMimeFormatException)
      {
        RestException restException = new RestException(RestStatus.responseForError(400, illegalMimeFormatException));
        callback.onError(restException);
      }
    }
  }

  //Test breakdown:
  //1. Main thread sends mime request
  //2. Server A sends a simple POST request to Server B
  //3. Server B sends back a mime response to Server A
  //4. Server A takes the original incoming request from the main thread + a local input
  //stream + the first part from the incoming mime response from Server B.
  //5. Main thread then gets all of this and stores it.
  //6. Server A then drains and stores the rest of the parts from Server B's response.
  @Test(dataProvider = "chunkSizes")
  public void testSinglePartDataSource(final int chunkSize) throws Exception
  {
    _chunkSize = chunkSize;

    final List<MultiPartMIMEDataSourceWriter> dataSources = generateInputStreamDataSources(chunkSize, _scheduledExecutorService);

    final MultiPartMIMEWriter writer = new MultiPartMIMEWriter.Builder().appendDataSources(dataSources).build();

    final StreamRequest streamRequest =
        MultiPartMIMEStreamRequestFactory
            .generateMultiPartMIMEStreamRequest(Bootstrap.createHttpsURI(PORT_SERVER_A, SERVER_A_URI), "mixed", writer,
                                                Collections.<String, String>emptyMap());

    ClientMultiPartReceiver clientReceiver = new ClientMultiPartReceiver();
    Callback<StreamResponse> callback = generateSuccessChainCallback(clientReceiver);

    //Send the request to Server A
    _client.streamRequest(streamRequest, callback);

    _latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS);

    //Verify client
    List<MIMETestUtils.SinglePartMIMEFullReaderCallback> clientSinglePartCallbacks = clientReceiver.getSinglePartMIMEReaderCallbacks();
    Assert.assertEquals(clientReceiver.getSinglePartMIMEReaderCallbacks().size(), 6);
    Assert.assertEquals(clientSinglePartCallbacks.get(0).getFinishedData(), BODY_1.getPartData());
    Assert.assertEquals(clientSinglePartCallbacks.get(0).getHeaders(), BODY_1.getPartHeaders());
    Assert.assertEquals(clientSinglePartCallbacks.get(1).getFinishedData(), BODY_5.getPartData());
    Assert.assertEquals(clientSinglePartCallbacks.get(1).getHeaders(), BODY_5.getPartHeaders());
    Assert.assertEquals(clientSinglePartCallbacks.get(2).getFinishedData(), BODY_A.getPartData());
    Assert.assertEquals(clientSinglePartCallbacks.get(2).getHeaders(), BODY_A.getPartHeaders());
    Assert.assertEquals(clientSinglePartCallbacks.get(3).getFinishedData(), BODY_B.getPartData());
    Assert.assertEquals(clientSinglePartCallbacks.get(3).getHeaders(), BODY_B.getPartHeaders());
    Assert.assertEquals(clientSinglePartCallbacks.get(4).getFinishedData(), BODY_C.getPartData());
    Assert.assertEquals(clientSinglePartCallbacks.get(4).getHeaders(), BODY_C.getPartHeaders());
    Assert.assertEquals(clientSinglePartCallbacks.get(5).getFinishedData(), BODY_D.getPartData());
    Assert.assertEquals(clientSinglePartCallbacks.get(5).getHeaders(), BODY_D.getPartHeaders());

    //Verify Server A
    List<MIMETestUtils.SinglePartMIMEFullReaderCallback> serverASinglePartCallbacks = _serverAMultiPartCallback.getSinglePartMIMEReaderCallbacks();
    Assert.assertEquals(serverASinglePartCallbacks.size(), 3);
    Assert.assertEquals(serverASinglePartCallbacks.get(0).getFinishedData(), BODY_2.getPartData());
    Assert.assertEquals(serverASinglePartCallbacks.get(0).getHeaders(), BODY_2.getPartHeaders());
    Assert.assertEquals(serverASinglePartCallbacks.get(1).getFinishedData(), BODY_3.getPartData());
    Assert.assertEquals(serverASinglePartCallbacks.get(1).getHeaders(), BODY_3.getPartHeaders());
    Assert.assertEquals(serverASinglePartCallbacks.get(2).getFinishedData(), BODY_4.getPartData());
    Assert.assertEquals(serverASinglePartCallbacks.get(2).getHeaders(), BODY_4.getPartHeaders());
  }

  private Callback<StreamResponse> generateSuccessChainCallback(final ClientMultiPartReceiver receiver)
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
        final MultiPartMIMEReader reader = MultiPartMIMEReader.createAndAcquireStream(result);
        reader.registerReaderCallback(receiver);
      }
    };
  }

  //Client callback:
  //Note that we can't use the top level Multipart mime reader callback from MimeTestUtils here because we need
  //count down the latch upon finishing.
  private class ClientMultiPartReceiver implements MultiPartMIMEReaderCallback
  {
    final List<MIMETestUtils.SinglePartMIMEFullReaderCallback> _singlePartMIMEReaderCallbacks =
        new ArrayList<MIMETestUtils.SinglePartMIMEFullReaderCallback>();

    ClientMultiPartReceiver()
    {
    }

    public List<SinglePartMIMEFullReaderCallback> getSinglePartMIMEReaderCallbacks()
    {
      return _singlePartMIMEReaderCallbacks;
    }

    @Override
    public void onNewPart(MultiPartMIMEReader.SinglePartMIMEReader singleParMIMEReader)
    {
      MIMETestUtils.SinglePartMIMEFullReaderCallback singlePartMIMEReaderCallback =
          new MIMETestUtils.SinglePartMIMEFullReaderCallback(singleParMIMEReader);
      singleParMIMEReader.registerReaderCallback(singlePartMIMEReaderCallback);
      _singlePartMIMEReaderCallbacks.add(singlePartMIMEReaderCallback);
      singleParMIMEReader.requestPartData();
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