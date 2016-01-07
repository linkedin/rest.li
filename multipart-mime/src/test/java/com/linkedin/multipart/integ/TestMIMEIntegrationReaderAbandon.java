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
import com.linkedin.multipart.MultiPartMIMEReader;
import com.linkedin.multipart.MultiPartMIMEReaderCallback;
import com.linkedin.multipart.SinglePartMIMEReaderCallback;
import com.linkedin.multipart.exceptions.MultiPartIllegalFormatException;
import com.linkedin.multipart.utils.VariableByteStringWriter;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.transport.common.StreamRequestHandler;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.client.HttpClientFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.multipart.utils.MIMETestUtils.BODY_LESS_BODY;
import static com.linkedin.multipart.utils.MIMETestUtils.BYTES_BODY;
import static com.linkedin.multipart.utils.MIMETestUtils.HEADER_CONTENT_TYPE;
import static com.linkedin.multipart.utils.MIMETestUtils.HEADER_LESS_BODY;
import static com.linkedin.multipart.utils.MIMETestUtils.LARGE_DATA_SOURCE;
import static com.linkedin.multipart.utils.MIMETestUtils.PURELY_EMPTY_BODY;
import static com.linkedin.multipart.utils.MIMETestUtils.SINGLE_ALL;
import static com.linkedin.multipart.utils.MIMETestUtils.SINGLE_ALL_NO_CALLBACK;
import static com.linkedin.multipart.utils.MIMETestUtils.SINGLE_ALTERNATE;
import static com.linkedin.multipart.utils.MIMETestUtils.SINGLE_ALTERNATE_TOP_REMAINING;
import static com.linkedin.multipart.utils.MIMETestUtils.SINGLE_PARTIAL_TOP_REMAINING;
import static com.linkedin.multipart.utils.MIMETestUtils.SMALL_DATA_SOURCE;
import static com.linkedin.multipart.utils.MIMETestUtils.TOP_ALL_NO_CALLBACK;
import static com.linkedin.multipart.utils.MIMETestUtils.TOP_ALL_WITH_CALLBACK;


/**
 * A series of integration tests that write multipart mime envelopes using Javax mail, and then use
 * {@link com.linkedin.multipart.MultiPartMIMEReader} on the server side to read and subsequently
 * abandon the data using different strategies.
 *
 * @author Karim Vidhani
 */
public class TestMIMEIntegrationReaderAbandon extends AbstractMIMEIntegrationStreamTest
{
  private static final URI SERVER_URI = URI.create("/pegasusAbandonServer");
  private MimeServerRequestAbandonHandler _mimeServerRequestAbandonHandler;
  private static final String ABANDON_HEADER = "AbandonMe";

  @Override
  protected TransportDispatcher getTransportDispatcher()
  {
    _mimeServerRequestAbandonHandler = new MimeServerRequestAbandonHandler();
    return new TransportDispatcherBuilder().addStreamHandler(SERVER_URI, _mimeServerRequestAbandonHandler).build();
  }

  @Override
  protected Map<String, String> getClientProperties()
  {
    Map<String, String> clientProperties = new HashMap<String, String>();
    clientProperties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, "9000000");
    return clientProperties;
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

    bodyPartList.add(PURELY_EMPTY_BODY);
    bodyPartList.add(BYTES_BODY);
    bodyPartList.add(BODY_LESS_BODY);
    bodyPartList.add(HEADER_LESS_BODY);
    bodyPartList.add(LARGE_DATA_SOURCE);
    bodyPartList.add(SMALL_DATA_SOURCE);

    return new Object[][]
        {
            {1, bodyPartList},
            {R2Constants.DEFAULT_DATA_CHUNK_SIZE, bodyPartList}
        };
  }

  ///////////////////////////////////////////////////////////////////////////////////////
  @Test(dataProvider = "allTypesOfBodiesDataSource")
  public void testSingleAllNoCallback(final int chunkSize, final List<MimeBodyPart> bodyPartList) throws Exception
  {
    executeRequestWithAbandonStrategy(chunkSize, bodyPartList, SINGLE_ALL_NO_CALLBACK, "onFinished");

    //Single part abandons all individually but doesn't use a callback:
    List<SinglePartMIMEAbandonReaderCallbackImpl> singlePartMIMEReaderCallbacks =
        _mimeServerRequestAbandonHandler.getTestMultiPartMIMEReaderCallback().getSinglePartMIMEReaderCallbacks();
    Assert.assertEquals(singlePartMIMEReaderCallbacks.size(), 0);
  }

  @Test(dataProvider = "allTypesOfBodiesDataSource")
  public void testAbandonAllWithCallbackRegistered(final int chunkSize, final List<MimeBodyPart> bodyPartList) throws Exception
  {
    executeRequestWithAbandonStrategy(chunkSize, bodyPartList, TOP_ALL_WITH_CALLBACK, "onAbandoned");

    //Top level abandons all after registering a callback and being invoked for the first time on onNewPart().
    List<SinglePartMIMEAbandonReaderCallbackImpl> singlePartMIMEReaderCallbacks =
        _mimeServerRequestAbandonHandler.getTestMultiPartMIMEReaderCallback().getSinglePartMIMEReaderCallbacks();
    Assert.assertEquals(singlePartMIMEReaderCallbacks.size(), 0);
  }

  //todo this test is failing - working with Zhenkai on this
  @Test(enabled = false, dataProvider = "allTypesOfBodiesDataSource")
  public void testAbandonAllWithoutCallbackRegistered(final int chunkSize, final List<MimeBodyPart> bodyPartList) throws Exception
  {
    executeRequestWithAbandonStrategy(chunkSize, bodyPartList, TOP_ALL_NO_CALLBACK, "onAbandoned");

    //Top level abandons all without registering a top level callback.
    Assert.assertNull(_mimeServerRequestAbandonHandler.getTestMultiPartMIMEReaderCallback()); //No callback created
  }

  @Test(dataProvider = "allTypesOfBodiesDataSource")
  public void testSinglePartialTopRemaining(final int chunkSize, final List<MimeBodyPart> bodyPartList) throws Exception
  {
    //Execute the request, verify the correct header came back to ensure the server took the proper abandon actions
    //and return the payload so we can assert deeper.
    MimeMultipart mimeMultipart = executeRequestWithAbandonStrategy(chunkSize, bodyPartList, SINGLE_PARTIAL_TOP_REMAINING, "onAbandoned");

    //Single part abandons the first 6 then the top level abandons all of remaining
    List<SinglePartMIMEAbandonReaderCallbackImpl> singlePartMIMEReaderCallbacks =
        _mimeServerRequestAbandonHandler.getTestMultiPartMIMEReaderCallback().getSinglePartMIMEReaderCallbacks();

    Assert.assertEquals(singlePartMIMEReaderCallbacks.size(), 6);

    for (int i = 0; i < singlePartMIMEReaderCallbacks.size(); i++)
    {
      //Actual 
      final SinglePartMIMEAbandonReaderCallbackImpl currentCallback = singlePartMIMEReaderCallbacks.get(i);
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
      //Verify that the bodies are empty
      Assert.assertEquals(currentCallback._finishedData, ByteString.empty());
    }
  }

  @Test(dataProvider = "allTypesOfBodiesDataSource")
  public void testSingleAlternateTopRemaining(final int chunkSize, final List<MimeBodyPart> bodyPartList)
      throws Exception
  {
    //Execute the request, verify the correct header came back to ensure the server took the proper abandon actions
    //and return the payload so we can assert deeper.
    MimeMultipart mimeMultipart = executeRequestWithAbandonStrategy(chunkSize, bodyPartList, SINGLE_ALTERNATE_TOP_REMAINING, "onAbandoned");

    //Single part alternates between consumption and abandoning the first 6 parts, then top level abandons all of remaining.
    //This means that parts 0, 2, 4 will be consumed and parts 1, 3, 5 will be abandoned.
    List<SinglePartMIMEAbandonReaderCallbackImpl> singlePartMIMEReaderCallbacks =
        _mimeServerRequestAbandonHandler.getTestMultiPartMIMEReaderCallback().getSinglePartMIMEReaderCallbacks();

    Assert.assertEquals(singlePartMIMEReaderCallbacks.size(), 6);

    //First the consumed
    for (int i = 0; i < singlePartMIMEReaderCallbacks.size(); i = i + 2)
    {
      //Actual 
      final SinglePartMIMEAbandonReaderCallbackImpl currentCallback = singlePartMIMEReaderCallbacks.get(i);
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

    //Then the abandoned
    for (int i = 1; i < singlePartMIMEReaderCallbacks.size(); i = i + 2)
    {
      //Actual 
      final SinglePartMIMEAbandonReaderCallbackImpl currentCallback = singlePartMIMEReaderCallbacks.get(i);
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
      //Verify that the bodies are empty
      Assert.assertEquals(currentCallback._finishedData, ByteString.empty());
    }
  }

  @Test(dataProvider = "allTypesOfBodiesDataSource")
  public void testSingleAll(final int chunkSize, final List<MimeBodyPart> bodyPartList) throws Exception
  {
    //Execute the request, verify the correct header came back to ensure the server took the proper abandon actions
    //and return the payload so we can assert deeper.
    MimeMultipart mimeMultipart = executeRequestWithAbandonStrategy(chunkSize, bodyPartList, SINGLE_ALL, "onFinished");

    //Single part abandons all, one by one
    List<SinglePartMIMEAbandonReaderCallbackImpl> singlePartMIMEReaderCallbacks =
        _mimeServerRequestAbandonHandler.getTestMultiPartMIMEReaderCallback().getSinglePartMIMEReaderCallbacks();

    Assert.assertEquals(singlePartMIMEReaderCallbacks.size(), 12);

    //Verify everything was abandoned
    for (int i = 0; i < singlePartMIMEReaderCallbacks.size(); i++)
    {
      //Actual 
      final SinglePartMIMEAbandonReaderCallbackImpl currentCallback = singlePartMIMEReaderCallbacks.get(i);
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
      //Verify that the bodies are empty
      Assert.assertEquals(currentCallback._finishedData, ByteString.empty());
    }
  }

  @Test(dataProvider = "allTypesOfBodiesDataSource")
  public void testSingleAlternate(final int chunkSize, final List<MimeBodyPart> bodyPartList) throws Exception
  {
    //Execute the request, verify the correct header came back to ensure the server took the proper abandon actions
    //and return the payload so we can assert deeper.
    MimeMultipart mimeMultipart = executeRequestWithAbandonStrategy(chunkSize, bodyPartList, SINGLE_ALTERNATE, "onFinished");

    //Single part alternates between consumption and abandoning for all 12 parts.
    //This means that parts 0, 2, 4, etc.. will be consumed and parts 1, 3, 5, etc... will be abandoned.
    List<SinglePartMIMEAbandonReaderCallbackImpl> singlePartMIMEReaderCallbacks =
        _mimeServerRequestAbandonHandler.getTestMultiPartMIMEReaderCallback().getSinglePartMIMEReaderCallbacks();

    Assert.assertEquals(singlePartMIMEReaderCallbacks.size(), 12);

    //First the consumed
    for (int i = 0; i < singlePartMIMEReaderCallbacks.size(); i = i + 2)
    {
      //Actual 
      final SinglePartMIMEAbandonReaderCallbackImpl currentCallback = singlePartMIMEReaderCallbacks.get(i);
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

    //Then the abandoned
    for (int i = 1; i < singlePartMIMEReaderCallbacks.size(); i = i + 2)
    {
      //Actual 
      final SinglePartMIMEAbandonReaderCallbackImpl currentCallback = singlePartMIMEReaderCallbacks.get(i);
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
      //Verify that the bodies are empty
      Assert.assertEquals(currentCallback._finishedData, ByteString.empty());
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  private MimeMultipart executeRequestWithAbandonStrategy(final int chunkSize, final List<MimeBodyPart> bodyPartList,
      final String abandonStrategy, final String serverHeaderPrefix) throws Exception
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
    final VariableByteStringWriter variableByteStringWriter = new VariableByteStringWriter(requestPayload, chunkSize);

    final EntityStream entityStream = EntityStreams.newEntityStream(variableByteStringWriter);
    final StreamRequestBuilder builder = new StreamRequestBuilder(Bootstrap.createHttpURI(PORT, SERVER_URI));

    StreamRequest request = builder.setMethod("POST").setHeader(HEADER_CONTENT_TYPE, multiPartMimeBody.getContentType())
                                   .setHeader(ABANDON_HEADER, abandonStrategy).build(entityStream);

    final AtomicInteger status = new AtomicInteger(-1);
    final CountDownLatch latch = new CountDownLatch(1);
    final Map<String, String> responseHeaders = new HashMap<String, String>();
    Callback<StreamResponse> callback = expectSuccessCallback(latch, status, responseHeaders);
    _client.streamRequest(request, callback);
    latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS);
    Assert.assertEquals(status.get(), RestStatus.OK);
    Assert.assertEquals(responseHeaders.get(ABANDON_HEADER), serverHeaderPrefix + abandonStrategy);
    return multiPartMimeBody;
  }

  private static class SinglePartMIMEAbandonReaderCallbackImpl implements SinglePartMIMEReaderCallback
  {
    final MultiPartMIMEReader.SinglePartMIMEReader _singlePartMIMEReader;
    static String _abandonValue;
    final ByteArrayOutputStream _byteArrayOutputStream = new ByteArrayOutputStream();
    Map<String, String> _headers;
    ByteString _finishedData = ByteString.empty();
    static int partCounter = 0;

    SinglePartMIMEAbandonReaderCallbackImpl(final MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader)
    {
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
    public void onAbandoned()
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

  private static class MultiPartMIMEAbandonReaderCallbackImpl implements MultiPartMIMEReaderCallback
  {
    final Callback<StreamResponse> _r2callback;
    final String _abandonValue;
    final MultiPartMIMEReader _reader;
    final List<SinglePartMIMEAbandonReaderCallbackImpl> _singlePartMIMEReaderCallbacks = new ArrayList<SinglePartMIMEAbandonReaderCallbackImpl>();

    MultiPartMIMEAbandonReaderCallbackImpl(final Callback<StreamResponse> r2callback, final String abandonValue,
                                           final MultiPartMIMEReader reader)
    {
      _r2callback = r2callback;
      _abandonValue = abandonValue;
      _reader = reader;
      SinglePartMIMEAbandonReaderCallbackImpl._abandonValue = _abandonValue;
    }

    public List<SinglePartMIMEAbandonReaderCallbackImpl> getSinglePartMIMEReaderCallbacks()
    {
      return _singlePartMIMEReaderCallbacks;
    }

    @Override
    public void onNewPart(MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader)
    {
      if (_abandonValue.equalsIgnoreCase(SINGLE_ALL_NO_CALLBACK))
      {
        singlePartMIMEReader.abandonPart();
        return;
      }

      if (_abandonValue.equalsIgnoreCase(TOP_ALL_WITH_CALLBACK))
      {
        _reader.abandonAllParts();
        return;
      }

      if (_abandonValue.equalsIgnoreCase(SINGLE_PARTIAL_TOP_REMAINING) && _singlePartMIMEReaderCallbacks.size() == 6)
      {
        _reader.abandonAllParts();
        return;
      }

      if (_abandonValue.equalsIgnoreCase(SINGLE_ALTERNATE_TOP_REMAINING) && _singlePartMIMEReaderCallbacks.size() == 6)
      {
        _reader.abandonAllParts();
        return;
      }

      //Now we know we have to either consume or abandon individually using a registered callback, so we
      //register with the SinglePartReader and take appropriate action based on the abandon strategy:
      SinglePartMIMEAbandonReaderCallbackImpl singlePartMIMEReaderCallback = new SinglePartMIMEAbandonReaderCallbackImpl(singlePartMIMEReader);
      singlePartMIMEReader.registerReaderCallback(singlePartMIMEReaderCallback);
      _singlePartMIMEReaderCallbacks.add(singlePartMIMEReaderCallback);

      if (_abandonValue.equalsIgnoreCase(SINGLE_ALL) || _abandonValue.equalsIgnoreCase(SINGLE_PARTIAL_TOP_REMAINING))
      {
        singlePartMIMEReader.abandonPart();
        return;
      }

      if (_abandonValue.equalsIgnoreCase(SINGLE_ALTERNATE) || _abandonValue.equalsIgnoreCase(SINGLE_ALTERNATE_TOP_REMAINING))
      {
        if (SinglePartMIMEAbandonReaderCallbackImpl.partCounter % 2 == 1)
        {
          singlePartMIMEReader.abandonPart();
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
      RestResponse response = new RestResponseBuilder().setStatus(RestStatus.OK).setHeader(ABANDON_HEADER, "onFinished" + _abandonValue).build();
      _r2callback.onSuccess(Messages.toStreamResponse(response));
    }

    @Override
    public void onAbandoned()
    {
      //Happens for TOP_ALL_WITH_CALLBACK, SINGLE_PARTIAL_TOP_REMAINING and SINGLE_ALTERNATE_TOP_REMAINING
      RestResponse response = new RestResponseBuilder().setStatus(RestStatus.OK).setHeader(ABANDON_HEADER, "onAbandoned" + _abandonValue).build();
      _r2callback.onSuccess(Messages.toStreamResponse(response));
    }

    @Override
    public void onStreamError(Throwable throwable)
    {
      RestException restException = new RestException(RestStatus.responseForError(400, throwable));
      _r2callback.onError(restException);
    }
  }

  private static class MimeServerRequestAbandonHandler implements StreamRequestHandler
  {
    private MultiPartMIMEAbandonReaderCallbackImpl _testMultiPartMIMEReaderCallback = null;

    MimeServerRequestAbandonHandler()
    {
    }

    public MultiPartMIMEAbandonReaderCallbackImpl getTestMultiPartMIMEReaderCallback()
    {
      return _testMultiPartMIMEReaderCallback;
    }

    @Override
    public void handleRequest(StreamRequest request, RequestContext requestContext,
        final Callback<StreamResponse> callback)
    {
      try
      {
        final MultiPartMIMEReader reader = MultiPartMIMEReader.createAndAcquireStream(request);
        final String shouldAbandonValue = request.getHeader(ABANDON_HEADER);

        //For all cases, except this, we will register a callback
        if (shouldAbandonValue.equalsIgnoreCase(TOP_ALL_NO_CALLBACK))
        {
          reader.abandonAllParts();
          RestResponse response =
              new RestResponseBuilder().setStatus(RestStatus.OK).setHeader(ABANDON_HEADER, "onAbandoned" + TOP_ALL_NO_CALLBACK).build();
          callback.onSuccess(Messages.toStreamResponse(response));
        }
        else
        {
          _testMultiPartMIMEReaderCallback = new MultiPartMIMEAbandonReaderCallbackImpl(callback, shouldAbandonValue, reader);
          reader.registerReaderCallback(_testMultiPartMIMEReaderCallback);
        }
      }
      catch (MultiPartIllegalFormatException illegalMimeFormatException)
      {
        RestException restException = new RestException(RestStatus.responseForError(400, illegalMimeFormatException));
        callback.onError(restException);
      }
    }
  }
}