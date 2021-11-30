/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.restli.examples;


import com.linkedin.data.ByteString;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.ProtocolVersionOption;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.client.RestliRequestOptionsBuilder;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.attachments.RestLiAttachmentReader;
import com.linkedin.restli.common.attachments.RestLiAttachmentReaderCallback;
import com.linkedin.restli.common.attachments.SingleRestLiAttachmentReaderCallback;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.StreamingGreetingsBuilders;
import com.linkedin.restli.internal.testutils.RestLiTestAttachmentDataSource;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Integration tests for rest.li attachment streaming.
 *
 * @author Karim Vidhani
 */
public class TestStreamingGreetings extends RestLiIntegrationTest
{
  @BeforeClass
  public void initClass() throws Exception
  {
    super.init();
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  @Override
  protected boolean forceUseStreamServer()
  {
    return true;
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  public void fullStreamTest(final RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    //Perform a create to the server to store some bytes via an attachment.
    final byte[] clientSuppliedBytes = "ClientSupplied".getBytes();
    final RestLiTestAttachmentDataSource greetingAttachment =
            new RestLiTestAttachmentDataSource("1", ByteString.copy(clientSuppliedBytes));

    final RootBuilderWrapper.MethodBuilderWrapper<Long, Greeting, EmptyRecord> methodBuilderWrapper = builders.create();

    methodBuilderWrapper.appendSingleAttachment(greetingAttachment);

    //Provide a header to verify the server's ability to transform the first part into the RestRequest.
    methodBuilderWrapper.setHeader("createHeader", "createHeaderValue");
    final Greeting greeting = new Greeting().setMessage("A greeting with an attachment");

    final Request<EmptyRecord> createRequest = methodBuilderWrapper.input(greeting).build();
    try
    {
      final Response<EmptyRecord> createResponse = getClient().sendRequest(createRequest).getResponse();
      Assert.assertEquals(createResponse.getStatus(), 201);
      //Verify that headers propagate properly.
      Assert.assertEquals(createResponse.getHeader("createHeader"), "createHeaderValue");
    }
    catch (final RestLiResponseException responseException)
    {
      Assert.fail("We should not reach here!", responseException);
    }

    //Then perform a GET and verify the bytes are present
    try
    {
      final Request<Greeting> getRequest = builders.get().id(1l).setHeader("getHeader", "getHeaderValue").build();
      final Response<Greeting> getResponse = getClient().sendRequest(getRequest).getResponse();
      Assert.assertEquals(getResponse.getStatus(), 200);

      //Verify that headers propagate properly.
      Assert.assertEquals(getResponse.getHeader("getHeader"), "getHeaderValue");
      Assert.assertEquals(getResponse.getHeader(RestConstants.HEADER_CONTENT_TYPE), RestConstants.HEADER_VALUE_APPLICATION_JSON);

      Assert.assertEquals(getResponse.getEntity().getMessage(),
                          "Your greeting has an attachment since you were kind and decided you wanted to read it!");
      Assert.assertTrue(getResponse.hasAttachments(), "We must have some response attachments");
      RestLiAttachmentReader attachmentReader = getResponse.getAttachmentReader();
      final CountDownLatch latch = new CountDownLatch(1);
      final GreetingBlobReaderCallback greetingBlobReaderCallback = new GreetingBlobReaderCallback(latch);
      attachmentReader.registerAttachmentReaderCallback(greetingBlobReaderCallback);
      try
      {
        latch.await(3000, TimeUnit.SECONDS);
        Assert.assertEquals(greetingBlobReaderCallback.getAttachmentList().size(), 1);
        Assert.assertEquals(greetingBlobReaderCallback.getAttachmentList().get(0), ByteString.copy(clientSuppliedBytes));
      }
      catch (Exception exception)
      {
        Assert.fail();
      }
    }
    catch (final RestLiResponseException responseException)
    {
      Assert.fail("We should not reach here!", responseException);
    }
  }

  //The delete and update tests here are simply to show that although not typical, it is possible to return
  //attachments from DELETE, UPDATE, PARTIAL_UPDATE, BATCH_DELETE, BATCH_UPDATE, and BATCH_PARTIAL_UPDATE. For the sake of
  //brevity DELETE and UPDATE are used as examples.
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  public void testDeleteReturnAttachments(final RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    try
    {
      final String headerAndAttachment = "someValue"; //This will be echoed back in the form of an attachment.
      final Request<EmptyRecord> deleteRequest =
          builders.delete().id(1l).setHeader("getHeader", headerAndAttachment).build();
      sendNonTypicalRequestAndVerifyAttachments(deleteRequest, headerAndAttachment);
    }
    catch (Exception exception)
    {
      Assert.fail();
    }
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  public void testUpdateReturnAttachments(final RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    try
    {
      final String headerAndAttachment = "someValue"; //This will be echoed back in the form of an attachment.
      final Request<EmptyRecord> updateRequest =
          builders.update().id(1l).input(new Greeting()).setHeader("getHeader", headerAndAttachment).build();
      sendNonTypicalRequestAndVerifyAttachments(updateRequest, headerAndAttachment);
    }
    catch (Exception exception)
    {
      Assert.fail();
    }
  }

  private void sendNonTypicalRequestAndVerifyAttachments(final Request<EmptyRecord> emptyRecordRequest,
                                                         final String headerAndAttachment) throws Exception
  {
    final Response<EmptyRecord> getResponse = getClient().sendRequest(emptyRecordRequest).getResponse();
    Assert.assertEquals(getResponse.getStatus(), 200);

    Assert.assertTrue(getResponse.hasAttachments(), "We must have some response attachments");
    RestLiAttachmentReader attachmentReader = getResponse.getAttachmentReader();
    final CountDownLatch latch = new CountDownLatch(1);
    final GreetingBlobReaderCallback greetingBlobReaderCallback = new GreetingBlobReaderCallback(latch);
    attachmentReader.registerAttachmentReaderCallback(greetingBlobReaderCallback);

    latch.await(3000, TimeUnit.SECONDS);
    Assert.assertEquals(greetingBlobReaderCallback.getAttachmentList().size(), 1);
    Assert.assertEquals(greetingBlobReaderCallback.getAttachmentList().get(0), ByteString.copy(
        headerAndAttachment.getBytes()));
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
          + "requestBuilderDataProvider")
  public void resourceMethodDoesNotAcceptAttachments(final RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    //Resource method does not desire request attachments. Assert that all the attachments are drained and that a 400
    //bad request is observed.
    final RestLiTestAttachmentDataSource greetingAttachment =
            new RestLiTestAttachmentDataSource("1", ByteString.copyString("clientData", Charset.defaultCharset()));

    RootBuilderWrapper.MethodBuilderWrapper<Long, Greeting, Object> methodBuilderWrapper =
            builders.action("actionNoAttachmentsAllowed");

    methodBuilderWrapper.appendSingleAttachment(greetingAttachment);

    final Request<Object> request = methodBuilderWrapper.build();
    try
    {
      getClient().sendRequest(request).getResponse().getEntity();
      Assert.fail();
    }
    catch (final RestLiResponseException responseException)
    {
      Assert.assertEquals(responseException.getStatus(), 400);
      Assert.assertEquals(responseException.getServiceErrorMessage(),
              "Resource method endpoint invoked does not accept any request attachments.");
    }

    //Then verify the response and request attachments were fully absorbed.
    Assert.assertTrue(greetingAttachment.finished());
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
          + "requestBuilderDataProvider")
  public void verifyNoAttachmentProvidedToResourceMethod(final RootBuilderWrapper<Long, Greeting> builders) throws RemoteInvocationException
  {
    //This test will call an endpoint that accepts attachments but without any attachments.
    //Verify that the resource method receives null.
    RootBuilderWrapper.MethodBuilderWrapper<Long, Greeting, Object> methodBuilderWrapper =
            builders.action("actionAttachmentsAllowedButDisliked");

    final Request<Object> actionRequest = methodBuilderWrapper.build();
    try
    {
      final Response<Object> response = getClient().sendRequest(actionRequest).getResponse();
      Assert.assertEquals(response.getStatus(), 200);
      Assert.assertTrue((Boolean)response.getEntity());
    }
    catch (final RestLiResponseException responseException)
    {
      Assert.fail("We should not reach here!", responseException);
    }
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX
      + "requestBuilderDataProvider")
  private static Object[][] requestBuilderDataProvider()
  {
    final RestliRequestOptions defaultOptions =
        new RestliRequestOptionsBuilder().setProtocolVersionOption(ProtocolVersionOption.USE_LATEST_IF_AVAILABLE)
            .setAcceptResponseAttachments(true)
            .build();

    final RestliRequestOptions nextOptions =
        new RestliRequestOptionsBuilder().setProtocolVersionOption(ProtocolVersionOption.FORCE_USE_NEXT)
            .setAcceptResponseAttachments(true)
            .build();

    return new Object[][]
        {
            {
                new RootBuilderWrapper<Long, Greeting>(new StreamingGreetingsBuilders(defaultOptions))
            },
            {
                new RootBuilderWrapper<Long, Greeting>(new StreamingGreetingsBuilders(nextOptions))
            }
        };
  }

  //For reading the response attachment
  private static class GreetingBlobReaderCallback implements RestLiAttachmentReaderCallback
  {
    private final CountDownLatch _countDownLatch;
    private List<ByteString> _attachmentsRead = new ArrayList<>();

    private GreetingBlobReaderCallback(CountDownLatch countDownLatch)
    {
      _countDownLatch = countDownLatch;
    }

    private void addAttachment(final ByteString attachment)
    {
      _attachmentsRead.add(attachment);
    }

    private List<ByteString> getAttachmentList()
    {
      return _attachmentsRead;
    }

    @Override
    public void onNewAttachment(RestLiAttachmentReader.SingleRestLiAttachmentReader singleRestLiAttachmentReader)
    {
      final SingleGreetingBlobReaderCallback singleGreetingBlobReaderCallback = new SingleGreetingBlobReaderCallback(this,
                                                                                             singleRestLiAttachmentReader);
      singleRestLiAttachmentReader.registerCallback(singleGreetingBlobReaderCallback);
      singleRestLiAttachmentReader.requestAttachmentData();
    }

    @Override
    public void onFinished()
    {
      _countDownLatch.countDown();
    }

    @Override
    public void onDrainComplete()
    {
      Assert.fail();
    }

    @Override
    public void onStreamError(Throwable throwable)
    {
      Assert.fail();
    }
  }

  private static class SingleGreetingBlobReaderCallback implements SingleRestLiAttachmentReaderCallback
  {
    private final GreetingBlobReaderCallback _topLevelCallback;
    private final RestLiAttachmentReader.SingleRestLiAttachmentReader _singleRestLiAttachmentReader;
    private final ByteArrayOutputStream _byteArrayOutputStream = new ByteArrayOutputStream();

    public SingleGreetingBlobReaderCallback(GreetingBlobReaderCallback topLevelCallback,
                                            RestLiAttachmentReader.SingleRestLiAttachmentReader singleRestLiAttachmentReader)
    {
      _topLevelCallback = topLevelCallback;
      _singleRestLiAttachmentReader = singleRestLiAttachmentReader;
    }

    @Override
    public void onAttachmentDataAvailable(ByteString attachmentData)
    {
      try
      {
        _byteArrayOutputStream.write(attachmentData.copyBytes());
        _singleRestLiAttachmentReader.requestAttachmentData();
      }
      catch (Exception exception)
      {
        _topLevelCallback.onStreamError(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR));
      }
    }

    @Override
    public void onFinished()
    {
      _topLevelCallback.addAttachment(ByteString.copy(_byteArrayOutputStream.toByteArray()));
    }

    @Override
    public void onDrainComplete()
    {
      Assert.fail();
    }

    @Override
    public void onAttachmentError(Throwable throwable)
    {
      //No need to do anything since the top level callback will get invoked with an error anyway
    }
  }
}
