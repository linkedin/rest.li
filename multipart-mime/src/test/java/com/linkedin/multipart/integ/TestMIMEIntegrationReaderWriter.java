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
import com.linkedin.data.ByteString;
import com.linkedin.multipart.MultiPartMIMEDataSourceWriter;
import com.linkedin.multipart.MultiPartMIMEInputStream;
import com.linkedin.multipart.MultiPartMIMEReader;
import com.linkedin.multipart.MultiPartMIMEReaderCallback;
import com.linkedin.multipart.MultiPartMIMEStreamRequestFactory;
import com.linkedin.multipart.MultiPartMIMEWriter;
import com.linkedin.multipart.SinglePartMIMEReaderCallback;
import com.linkedin.multipart.exceptions.MultiPartIllegalFormatException;
import com.linkedin.multipart.utils.MIMEDataPart;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.transport.common.StreamRequestHandler;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.client.HttpClientFactory;

import com.google.common.collect.ImmutableList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Represents integration tests that work between the {@link com.linkedin.multipart.MultiPartMIMEWriter} and
 * the {@link com.linkedin.multipart.MultiPartMIMEReader}
 *
 * @author Karim Vidhani
 */
public class TestMIMEIntegrationReaderWriter extends AbstractMIMEIntegrationStreamTest
{
  private static ScheduledExecutorService scheduledExecutorService;

  private static final URI SERVER_URI = URI.create("/pegasusMimeServer");
  private MIMEServerRequestHandler _mimeServerRequestHandler;

  byte[] _normalBodyData;
  Map<String, String> _normalBodyHeaders;

  byte[] _headerLessBodyData;

  Map<String, String> _bodyLessHeaders;

  MIMEDataPart _normalBody;
  MIMEDataPart _headerLessBody;
  MIMEDataPart _bodyLessBody;
  MIMEDataPart _purelyEmptyBody;

  @BeforeSuite
  public void dataSourceSetup()
  {
    scheduledExecutorService = Executors.newScheduledThreadPool(10);

    _normalBodyData = "some normal body that is relatively small".getBytes();
    _normalBodyHeaders = new HashMap<String, String>();
    _normalBodyHeaders.put("simpleheader", "simplevalue");

    //Second body has no headers
    _headerLessBodyData = "a body without headers".getBytes();

    //Third body has only headers
    _bodyLessHeaders = new HashMap<String, String>();
    _normalBodyHeaders.put("header1", "value1");
    _normalBodyHeaders.put("header2", "value2");
    _normalBodyHeaders.put("header3", "value3");

    _normalBody = new MIMEDataPart(ByteString.copy(_normalBodyData), _normalBodyHeaders);

    _headerLessBody = new MIMEDataPart(ByteString.copy(_headerLessBodyData), Collections.<String, String>emptyMap());

    _bodyLessBody = new MIMEDataPart(ByteString.empty(), _bodyLessHeaders);

    _purelyEmptyBody = new MIMEDataPart(ByteString.empty(), Collections.<String, String>emptyMap());
  }

  @AfterSuite
  public void shutDown()
  {
    scheduledExecutorService.shutdownNow();
  }

  @Override
  protected TransportDispatcher getTransportDispatcher()
  {
    _mimeServerRequestHandler = new MIMEServerRequestHandler();
    return new TransportDispatcherBuilder().addStreamHandler(SERVER_URI, _mimeServerRequestHandler).build();
  }

  @Override
  protected Map<String, String> getClientProperties()
  {
    Map<String, String> clientProperties = new HashMap<String, String>();
    clientProperties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, "9000000");
    return clientProperties;
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  @DataProvider(name = "eachSingleBodyDataSource")
  public Object[][] eachSingleBodyDataSource() throws Exception
  {
    return new Object[][]
        {
            {1, _normalBody},
            {10, _normalBody},
            {R2Constants.DEFAULT_DATA_CHUNK_SIZE, _normalBody},
            {1, _headerLessBody},
            {10, _headerLessBody},
            {R2Constants.DEFAULT_DATA_CHUNK_SIZE, _headerLessBody},
            {1, _bodyLessBody},
            {10, _bodyLessBody},
            {R2Constants.DEFAULT_DATA_CHUNK_SIZE, _bodyLessBody},
            {1, _purelyEmptyBody},
            {10, _purelyEmptyBody},
            {R2Constants.DEFAULT_DATA_CHUNK_SIZE, _purelyEmptyBody}
        };
  }

  @Test(dataProvider = "eachSingleBodyDataSource")
  public void testEachSingleBodyDataSource(final int chunkSize, final MIMEDataPart bodyPart) throws Exception
  {
    final MultiPartMIMEInputStream inputStreamDataSource =
        new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(bodyPart.getPartData().copyBytes()),
                                             scheduledExecutorService, bodyPart.getPartHeaders()).withWriteChunkSize(chunkSize).build();

    final MultiPartMIMEWriter writer = new MultiPartMIMEWriter.Builder("some preamble", "").appendDataSource(inputStreamDataSource).build();

    executeRequestAndAssert(writer, ImmutableList.of(bodyPart));
  }

  @Test(dataProvider = "eachSingleBodyDataSource")
  public void testEachSingleBodyDataSourceMultipleTimes(final int chunkSize, final MIMEDataPart bodyPart)
      throws Exception
  {
    final List<MultiPartMIMEDataSourceWriter> dataSources = new ArrayList<MultiPartMIMEDataSourceWriter>();
    for (int i = 0; i < 4; i++)
    {
      final MultiPartMIMEInputStream inputStreamDataSource =
          new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(bodyPart.getPartData().copyBytes()),
                                               scheduledExecutorService, bodyPart.getPartHeaders()).withWriteChunkSize(chunkSize).build();
      dataSources.add(inputStreamDataSource);
    }

    final MultiPartMIMEWriter writer = new MultiPartMIMEWriter.Builder("some preamble", "").appendDataSources(dataSources).build();

    executeRequestAndAssert(writer, ImmutableList.of(bodyPart, bodyPart, bodyPart, bodyPart));
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  @DataProvider(name = "chunkSizes")
  public Object[][] chunkSizes() throws Exception
  {
    return new Object[][]
        {
            {1}, {R2Constants.DEFAULT_DATA_CHUNK_SIZE}
        };
  }

  @Test(dataProvider = "chunkSizes")
  public void testMultipleBodies(final int chunkSize) throws Exception
  {
    final MultiPartMIMEInputStream normalBodyInputStream =
        new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(_normalBody.getPartData().copyBytes()),
                                             scheduledExecutorService, _normalBody.getPartHeaders()).withWriteChunkSize(chunkSize).build();

    final MultiPartMIMEInputStream headerLessBodyInputStream =
        new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(_headerLessBody.getPartData().copyBytes()),
                                             scheduledExecutorService, _headerLessBody.getPartHeaders()).withWriteChunkSize(chunkSize).build();

    //Copying over empty ByteString, but let's keep things consistent.
    final MultiPartMIMEInputStream bodyLessBodyInputStream =
        new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(_bodyLessBody.getPartData().copyBytes()),
                                             scheduledExecutorService, _bodyLessBody.getPartHeaders()).withWriteChunkSize(chunkSize).build();

    final MultiPartMIMEInputStream purelyEmptyBodyInputStream =
        new MultiPartMIMEInputStream.Builder(new ByteArrayInputStream(_purelyEmptyBody.getPartData().copyBytes()),
                                             scheduledExecutorService, _purelyEmptyBody.getPartHeaders()).withWriteChunkSize(chunkSize).build();

    final MultiPartMIMEWriter writer =
        new MultiPartMIMEWriter.Builder("some preamble", "").appendDataSource(normalBodyInputStream)
            .appendDataSource(headerLessBodyInputStream).appendDataSource(bodyLessBodyInputStream)
            .appendDataSource(purelyEmptyBodyInputStream).build();

    executeRequestAndAssert(writer, ImmutableList.of(_normalBody, _headerLessBody, _bodyLessBody, _purelyEmptyBody));
  }

  @Test
  public void testEmptyEnvelope() throws Exception
  {
    //Javax mail does not support this, hence we can only test this using our writer
    final MultiPartMIMEWriter writer = new MultiPartMIMEWriter.Builder("some preamble", "").build();

    executeRequestAndAssert(writer, Collections.<MIMEDataPart>emptyList());
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  //This method will execute a StreamRequest using a payload comprised of the provided MultiPartMIMEWriter. The request
  //makes its way to a server which will read all the parts one by one and store them in a list. Subsequently,
  //an assertion is performed to make sure that each part was read and saved properly on the server.
  private void executeRequestAndAssert(final MultiPartMIMEWriter requestWriter, final List<MIMEDataPart> expectedParts)
      throws Exception
  {
    final StreamRequest streamRequest =
        MultiPartMIMEStreamRequestFactory
            .generateMultiPartMIMEStreamRequest(Bootstrap.createHttpURI(PORT, SERVER_URI), "mixed", requestWriter,
                                                Collections.<String, String>emptyMap());

    final AtomicInteger status = new AtomicInteger(-1);
    final CountDownLatch latch = new CountDownLatch(1);
    Callback<StreamResponse> callback = expectSuccessCallback(latch, status, new HashMap<String, String>());
    _client.streamRequest(streamRequest, callback);
    latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS);
    Assert.assertEquals(status.get(), RestStatus.OK);

    List<SinglePartMIMEReaderCallbackImpl> singlePartMIMEReaderCallbacks =
        _mimeServerRequestHandler.getTestMultiPartMIMEReaderCallback().getSinglePartMIMEReaderCallbacks();
        _mimeServerRequestHandler.getTestMultiPartMIMEReaderCallback().getSinglePartMIMEReaderCallbacks();
    Assert.assertEquals(singlePartMIMEReaderCallbacks.size(), expectedParts.size());
    for (int i = 0; i < singlePartMIMEReaderCallbacks.size(); i++)
    {
      //Actual
      final SinglePartMIMEReaderCallbackImpl currentCallback = singlePartMIMEReaderCallbacks.get(i);
      //Expected
      final MIMEDataPart currentExpectedPart = expectedParts.get(i);

      Assert.assertEquals(currentCallback.getHeaders(), currentExpectedPart.getPartHeaders());
      Assert.assertEquals(currentCallback.getFinishedData(), currentExpectedPart.getPartData());
    }
  }

  private static class SinglePartMIMEReaderCallbackImpl implements SinglePartMIMEReaderCallback
  {
    final MultiPartMIMEReaderCallback _topLevelCallback;
    final MultiPartMIMEReader.SinglePartMIMEReader _singlePartMIMEReader;
    final ByteArrayOutputStream _byteArrayOutputStream = new ByteArrayOutputStream();
    Map<String, String> _headers;
    ByteString _finishedData = null;
    static int partCounter = 0;

    SinglePartMIMEReaderCallbackImpl(final MultiPartMIMEReaderCallback topLevelCallback,
                                     final MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader)
    {
      _topLevelCallback = topLevelCallback;
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
        onStreamError(ioException);
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
    public void onAbandoned()
    {
      //This will end up failing the test.
      _topLevelCallback.onAbandoned();
    }

    @Override
    public void onStreamError(Throwable throwable)
    {
      //MultiPartMIMEReader will end up calling onStreamError(e) on our top level callback
      //which will fail the test
    }
  }

  private static class MultiPartMIMEReaderCallbackImpl implements MultiPartMIMEReaderCallback
  {
    final Callback<StreamResponse> _r2callback;
    final List<SinglePartMIMEReaderCallbackImpl> _singlePartMIMEReaderCallbacks = new ArrayList<SinglePartMIMEReaderCallbackImpl>();

    MultiPartMIMEReaderCallbackImpl(final Callback<StreamResponse> r2callback)
    {
      _r2callback = r2callback;
    }

    public List<SinglePartMIMEReaderCallbackImpl> getSinglePartMIMEReaderCallbacks()
    {
      return _singlePartMIMEReaderCallbacks;
    }

    @Override
    public void onNewPart(MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader)
    {
      SinglePartMIMEReaderCallbackImpl singlePartMIMEReaderCallback = new SinglePartMIMEReaderCallbackImpl(this, singlePartMIMEReader);
      singlePartMIMEReader.registerReaderCallback(singlePartMIMEReaderCallback);
      _singlePartMIMEReaderCallbacks.add(singlePartMIMEReaderCallback);
      singlePartMIMEReader.requestPartData();
    }

    @Override
    public void onFinished()
    {
      RestResponse response = RestStatus.responseForStatus(RestStatus.OK, "");
      _r2callback.onSuccess(Messages.toStreamResponse(response));
    }

    @Override
    public void onAbandoned()
    {
      RestException restException = new RestException(RestStatus.responseForStatus(406, "Not Acceptable"));
      _r2callback.onError(restException);
    }

    @Override
    public void onStreamError(Throwable throwable)
    {
      RestException restException = new RestException(RestStatus.responseForError(400, throwable));
      _r2callback.onError(restException);
    }
  }

  private static class MIMEServerRequestHandler implements StreamRequestHandler
  {
    private MultiPartMIMEReaderCallbackImpl _testMultiPartMIMEReaderCallback;

    MIMEServerRequestHandler()
    {
    }

    public MultiPartMIMEReaderCallbackImpl getTestMultiPartMIMEReaderCallback()
    {
      return _testMultiPartMIMEReaderCallback;
    }

    @Override
    public void handleRequest(StreamRequest request, RequestContext requestContext,
        final Callback<StreamResponse> callback)
    {
      try
      {
        MultiPartMIMEReader reader = MultiPartMIMEReader.createAndAcquireStream(request);
        _testMultiPartMIMEReaderCallback = new MultiPartMIMEReaderCallbackImpl(callback);
        reader.registerReaderCallback(_testMultiPartMIMEReaderCallback);
      }
      catch (MultiPartIllegalFormatException illegalMimeFormatException)
      {
        RestException restException = new RestException(RestStatus.responseForError(400, illegalMimeFormatException));
        callback.onError(restException);
      }
    }
  }
}