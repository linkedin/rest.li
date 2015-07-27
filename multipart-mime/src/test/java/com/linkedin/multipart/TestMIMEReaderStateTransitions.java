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
import com.linkedin.multipart.exceptions.SinglePartBindException;
import com.linkedin.multipart.exceptions.SinglePartFinishedException;
import com.linkedin.multipart.exceptions.SinglePartNotInitializedException;
import com.linkedin.multipart.exceptions.StreamBusyException;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.entitystream.EntityStream;

import java.util.Collections;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * These tests will verify that the correct exceptions are thrown in the face of unorthodox clients.
 *
 * @author Karim Vidhani
 */
public class TestMIMEReaderStateTransitions
{
  private static final EmptyMultiPartMIMEReaderCallback EMPTY_MULTI_PART_MIME_READER_CALLBACK = new EmptyMultiPartMIMEReaderCallback();
  private static final EmptySinglePartMIMEReaderCallback EMPTY_SINGLE_PART_MIME_READER_CALLBACK = new EmptySinglePartMIMEReaderCallback();

  //MultiPartMIMEReader exceptions:
  @Test
  public void testRegisterCallbackMultiPartMIMEReader()
  {
    final EntityStream entityStream = mock(EntityStream.class);
    final StreamRequest streamRequest = mock(StreamRequest.class);
    when(streamRequest.getEntityStream()).thenReturn(entityStream);
    when(streamRequest.getHeader(MultiPartMIMEUtils.CONTENT_TYPE_HEADER)).thenReturn("multipart/mixed; boundary=\"--123\"");
    MultiPartMIMEReader reader = MultiPartMIMEReader.createAndAcquireStream(streamRequest);

    //Test each possible exception:
    reader.setState(MultiPartMIMEReader.MultiPartReaderState.FINISHED);
    try
    {
      reader.registerReaderCallback(EMPTY_MULTI_PART_MIME_READER_CALLBACK);
      Assert.fail();
    }
    catch (MultiPartReaderFinishedException multiPartReaderFinishedException)
    {
    }

    reader.setState(MultiPartMIMEReader.MultiPartReaderState.READING_EPILOGUE);
    try
    {
      reader.registerReaderCallback(EMPTY_MULTI_PART_MIME_READER_CALLBACK);
      Assert.fail();
    }
    catch (MultiPartReaderFinishedException multiPartReaderFinishedException)
    {
    }

    reader.setState(MultiPartMIMEReader.MultiPartReaderState.CALLBACK_BOUND_AND_READING_PREAMBLE);
    try
    {
      reader.registerReaderCallback(EMPTY_MULTI_PART_MIME_READER_CALLBACK);
      Assert.fail();
    }
    catch (StreamBusyException streamBusyException)
    {
    }

    reader.setState(MultiPartMIMEReader.MultiPartReaderState.ABANDONING);
    try
    {
      reader.registerReaderCallback(EMPTY_MULTI_PART_MIME_READER_CALLBACK);
      Assert.fail();
    }
    catch (StreamBusyException streamBusyException)
    {
    }

    reader.setState(MultiPartMIMEReader.MultiPartReaderState.READING_PARTS); //This is a desired top level reader state
    final MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader = reader.new SinglePartMIMEReader(Collections.<String, String>emptyMap());
    singlePartMIMEReader.setState(MultiPartMIMEReader.SingleReaderState.REQUESTED_DATA); //This is a undesired single part state
    reader.setCurrentSinglePartMIMEReader(singlePartMIMEReader);
    try
    {
      reader.registerReaderCallback(EMPTY_MULTI_PART_MIME_READER_CALLBACK);
      Assert.fail();
    }
    catch (StreamBusyException streamBusyException)
    {
    }
  }

  @Test
  public void testAbandonAllPartsMultiPartMIMEReader()
  {
    final EntityStream entityStream = mock(EntityStream.class);
    final StreamRequest streamRequest = mock(StreamRequest.class);
    when(streamRequest.getEntityStream()).thenReturn(entityStream);
    when(streamRequest.getHeader(MultiPartMIMEUtils.CONTENT_TYPE_HEADER)).thenReturn("multipart/mixed; boundary=\"--123\"");

    MultiPartMIMEReader reader = MultiPartMIMEReader.createAndAcquireStream(streamRequest);

    //Test each possible exception:
    reader.setState(MultiPartMIMEReader.MultiPartReaderState.FINISHED);
    try
    {
      reader.abandonAllParts();
      Assert.fail();
    }
    catch (MultiPartReaderFinishedException multiPartReaderFinishedException)
    {
    }

    reader.setState(MultiPartMIMEReader.MultiPartReaderState.READING_EPILOGUE);
    try
    {
      reader.abandonAllParts();
      Assert.fail();
    }
    catch (MultiPartReaderFinishedException multiPartReaderFinishedException)
    {
    }

    reader.setState(MultiPartMIMEReader.MultiPartReaderState.CALLBACK_BOUND_AND_READING_PREAMBLE);
    try
    {
      reader.abandonAllParts();
      Assert.fail();
    }
    catch (StreamBusyException streamBusyException)
    {
    }

    reader.setState(MultiPartMIMEReader.MultiPartReaderState.ABANDONING);
    try
    {
      reader.abandonAllParts();
      Assert.fail();
    }
    catch (StreamBusyException streamBusyException)
    {
    }

    reader.setState(MultiPartMIMEReader.MultiPartReaderState.READING_PARTS); //This is the desired top level reader state
    final MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader = reader.new SinglePartMIMEReader(Collections.<String, String>emptyMap());
    singlePartMIMEReader.setState(MultiPartMIMEReader.SingleReaderState.REQUESTED_DATA); //This is a undesired single part state
    reader.setCurrentSinglePartMIMEReader(singlePartMIMEReader);
    try
    {
      reader.abandonAllParts();
      Assert.fail();
    }
    catch (StreamBusyException streamBusyException)
    {
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////
  //SinglePartMIMEReader exceptions:

  @Test
  public void testRegisterSinglePartMIMEReaderCallbackTwice()
  {
    final EntityStream entityStream = mock(EntityStream.class);

    final StreamRequest streamRequest = mock(StreamRequest.class);
    when(streamRequest.getEntityStream()).thenReturn(entityStream);
    when(streamRequest.getHeader(MultiPartMIMEUtils.CONTENT_TYPE_HEADER)).thenReturn("multipart/mixed; boundary=\"--123\"");

    MultiPartMIMEReader reader = MultiPartMIMEReader.createAndAcquireStream(streamRequest);

    final MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader =
        reader.new SinglePartMIMEReader(Collections.<String, String>emptyMap());
    singlePartMIMEReader.setState(MultiPartMIMEReader.SingleReaderState.REQUESTED_DATA); //This is a undesired single part state
    try
    {
      singlePartMIMEReader.registerReaderCallback(EMPTY_SINGLE_PART_MIME_READER_CALLBACK);
      Assert.fail();
    }
    catch (SinglePartBindException singlePartBindException)
    {
    }
  }

  @Test
  public void testSinglePartMIMEReaderVerifyState()
  {
    //This will cover abandonPart() and most of requestPartData().
    //The caveat is that requestPartData() requires a callback to be registered. This
    //will be covered in the next test.

    final EntityStream entityStream = mock(EntityStream.class);
    final StreamRequest streamRequest = mock(StreamRequest.class);
    when(streamRequest.getEntityStream()).thenReturn(entityStream);
    when(streamRequest.getHeader(MultiPartMIMEUtils.CONTENT_TYPE_HEADER)).thenReturn("multipart/mixed; boundary=\"--123\"");

    MultiPartMIMEReader reader = MultiPartMIMEReader.createAndAcquireStream(streamRequest);

    final MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader =
        reader.new SinglePartMIMEReader(Collections.<String, String>emptyMap());

    singlePartMIMEReader.setState(MultiPartMIMEReader.SingleReaderState.FINISHED);
    try
    {
      singlePartMIMEReader.verifyUsableState();
      Assert.fail();
    }
    catch (SinglePartFinishedException singlePartFinishedException)
    {
    }

    singlePartMIMEReader.setState(MultiPartMIMEReader.SingleReaderState.REQUESTED_DATA);
    try
    {
      singlePartMIMEReader.verifyUsableState();
      Assert.fail();
    }
    catch (StreamBusyException streamBusyException)
    {
    }

    singlePartMIMEReader.setState(MultiPartMIMEReader.SingleReaderState.REQUESTED_ABANDON);
    try
    {
      singlePartMIMEReader.verifyUsableState();
      Assert.fail();
    }
    catch (StreamBusyException streamBusyException)
    {
    }
  }

  @Test
  public void testSinglePartMIMEReaderRequestData()
  {
    final EntityStream entityStream = mock(EntityStream.class);
    final StreamRequest streamRequest = mock(StreamRequest.class);
    when(streamRequest.getEntityStream()).thenReturn(entityStream);
    when(streamRequest.getHeader(MultiPartMIMEUtils.CONTENT_TYPE_HEADER)).thenReturn("multipart/mixed; boundary=\"--123\"");
    MultiPartMIMEReader reader = MultiPartMIMEReader.createAndAcquireStream(streamRequest);

    final MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader = reader.new SinglePartMIMEReader(Collections.<String, String>emptyMap());

    singlePartMIMEReader.setState(MultiPartMIMEReader.SingleReaderState.CREATED);
    try
    {
      singlePartMIMEReader.requestPartData();
      Assert.fail();
    }
    catch (SinglePartNotInitializedException singlePartNotInitializedException)
    {
    }
  }

  private static final class EmptyMultiPartMIMEReaderCallback implements MultiPartMIMEReaderCallback
  {
    @Override
    public void onNewPart(MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader)
    {
    }

    @Override
    public void onFinished()
    {
    }

    @Override
    public void onAbandoned()
    {
    }

    @Override
    public void onStreamError(Throwable throwable)
    {
    }
  }

  private static final class EmptySinglePartMIMEReaderCallback implements SinglePartMIMEReaderCallback
  {
    @Override
    public void onPartDataAvailable(ByteString partData)
    {
    }

    @Override
    public void onFinished()
    {
    }

    @Override
    public void onAbandoned()
    {
    }

    @Override
    public void onStreamError(Throwable throwable)
    {
    }
  }
}