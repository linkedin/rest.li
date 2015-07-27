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
import com.linkedin.r2.message.stream.entitystream.WriteHandle;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;


/**
 * Tests for {@link com.linkedin.multipart.MultiPartMIMEInputStream}
 *
 * @author Karim Vidhani
 */
public class TestMIMEInputStream extends AbstractMIMEUnitTest
{
  private static final int TEST_CHUNK_SIZE = 4;

  ///////////////////////////////////////////////////////////////////////////////////////
  //Classes for tests below

  //We need a convenient input stream for testing so we use ByteArrayInputStream.
  //This input stream should also be a bit more rigid in behavior. Therefore it should complain about being closed more
  //then once so that we don't over-close our data sources. This input stream should also complain when reads are made
  //after closing. ByteArrayInputStream is convenient, but out of the box its too flexible.
  private static class StrictByteArrayInputStream extends ByteArrayInputStream
  {
    private boolean _isClosed = false;

    private StrictByteArrayInputStream(final byte[] bytes)
    {
      super(bytes);
    }

    @Override
    public void close() throws IOException
    {
      if (_isClosed)
      {
        throw new IOException("Can only close once");
      }
      _isClosed = true;
      super.close();
    }

    @Override
    public int read(byte[] b) throws IOException
    {
      if (_isClosed)
      {
        throw new IOException("Already closed!");
      }
      return super.read(b);
    }

    //Doesn't exist in ByteArrayInputStream so we introduce our own check.
    boolean isClosed()
    {
      return _isClosed;
    }
  }

  //Simulates an input stream that responds slower and slower. We also need the strict behavior
  //here as well.
  private static class SlowByteArrayInputStream extends StrictByteArrayInputStream
  {
    private int _readDelay = 0;
    private final int _delayIncrement;

    private SlowByteArrayInputStream(final byte[] bytes, final int initialDelay, final int delayIncrement)
    {
      super(bytes);
      _readDelay = initialDelay;
      _delayIncrement = delayIncrement;
    }

    @Override
    public int read(byte[] b) throws IOException
    {
      try
      {
        Thread.sleep(_readDelay);
        _readDelay = _readDelay + _delayIncrement;
      }
      catch (InterruptedException interruptException)
      {
        Assert.fail();
      }
      return super.read(b);
    }
  }

  //Simulates an input stream that throws IOException after a configurable number of reads.
  private static class ExceptionThrowingByteArrayInputStream extends StrictByteArrayInputStream
  {
    private final int _permissibleReads;
    private int _readCount = 0;
    private final IOException _throwable;

    private ExceptionThrowingByteArrayInputStream(final byte[] bytes, final int permissibleReads, final IOException throwable)
    {
      super(bytes);
      _permissibleReads = permissibleReads;
      _throwable = throwable;
    }

    @Override
    public int read(byte[] b) throws IOException
    {
      if (_readCount == _permissibleReads)
      {
        throw _throwable;
      }
      _readCount++;
      return super.read(b);
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  //This test will use a small and large data source to verify behavior with a single
  //invocation of onWritePossible().
  @DataProvider(name = "singleOnWritePossibleDataSources")
  public Object[][] singleOnWritePossibleDataSources() throws Exception
  {
    final byte[] smallInputData = "b".getBytes();

    //Ensure three writes for the bigger data
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < (TEST_CHUNK_SIZE * 2) + 2; i++)
    {
      builder.append('a');
    }

    final byte[] largeInputData = builder.toString().getBytes();

    return new Object[][]
        {
          //One onWritePossible() providing one write on the writeHandle which results in 1 expected write
          {smallInputData, new StrictByteArrayInputStream(smallInputData), 1, 1},
          //One OnWritePossible() providing three writes on the writeHandle, which results in 3 expected writes
          {largeInputData, new StrictByteArrayInputStream(largeInputData), 3, 3},

          //Also verify that extra writes handles available do no harm:
          {smallInputData, new StrictByteArrayInputStream(smallInputData), 3, 1},
          {largeInputData, new StrictByteArrayInputStream(largeInputData), 5, 3},
        };
  }

  @Test(dataProvider = "singleOnWritePossibleDataSources")
  public void testSingleOnWritePossibleDataSources(final byte[] inputData, final StrictByteArrayInputStream inputStream,
                                                   final int writesRemainingPerOnWritePossibles, final int expectedTotalWrites)
  {
    //Setup:
    final WriteHandle writeHandle = Mockito.mock(WriteHandle.class);
    final MultiPartMIMEInputStream multiPartMIMEInputStream =
        new MultiPartMIMEInputStream.Builder(inputStream, _scheduledExecutorService,
                                             Collections.<String, String>emptyMap()).withWriteChunkSize(TEST_CHUNK_SIZE).build();

    //Simulate a write handle that offers decreasing writeHandle.remaining()
    final Integer[] remainingWriteHandleCount = simulateDecreasingWriteHandleCount(writesRemainingPerOnWritePossibles);
    when(writeHandle.remaining()).thenReturn(writesRemainingPerOnWritePossibles, remainingWriteHandleCount);

    final ByteArrayOutputStream byteArrayOutputStream = setupMockWriteHandleToOutputStream(writeHandle);

    //Setup for done()
    final CountDownLatch doneLatch = new CountDownLatch(1);
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        doneLatch.countDown();
        return null;
      }
    }).when(writeHandle).done();

    ///////////////////////////////////
    //Start things off

    //Init the data source
    multiPartMIMEInputStream.onInit(writeHandle);
    multiPartMIMEInputStream.onWritePossible();

    //Wait to finish
    try
    {
      boolean successful = doneLatch.await(_testTimeout, TimeUnit.MILLISECONDS);
      if (!successful)
      {
        Assert.fail("Timeout when waiting for input stream to completely transfer");
      }
    }
    catch (Exception exception)
    {
      Assert.fail("Unexpected exception when waiting for input stream to completely transfer");
    }

    ///////////////////////////////////
    //Assert
    Assert.assertEquals(byteArrayOutputStream.toByteArray(), inputData, "All data from the input stream should have successfully been transferred");
    Assert.assertEquals(inputStream.isClosed(), true);

    //Mock verifies:
    verify(writeHandle, times(expectedTotalWrites)).write(isA(ByteString.class));
    verify(writeHandle, times(expectedTotalWrites)).remaining();
    verify(writeHandle, never()).error(isA(Throwable.class));
    verify(writeHandle, times(1)).done();
    verifyNoMoreInteractions(writeHandle);
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  //This test will use a small and large data source to verify behavior with a multiple
  //invocations of onWritePossible().

  @DataProvider(name = "multipleOnWritePossibleDataSources")
  public Object[][] multipleOnWritePossibleDataSources() throws Exception
  {
    //Ensure three writes for the bigger data
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < (TEST_CHUNK_SIZE * 2) + 2; i++)
    {
      builder.append('a');
    }
    final byte[] largeInputData = builder.toString().getBytes();

    return new Object[][]
        {
          //Represents 3 invocations of onWritePossible(), each providing 1 write on the write handle.
          //We expect a total of 3 writes based on our chunk size.
          //We also expect 5 invocations of writeHandle.remaining(). This is because the first two
          //onWritePossibles() will lead to writeHandle.remaining() being called twice (returning 1,0)
          //and the last onWritePossible() will lead to writeHandle.remaining() being called once (returning 1)
          //at which point the data is finished.
          {largeInputData, new StrictByteArrayInputStream(largeInputData), 3, 1, 3, 5},

          //Represents 2 invocation of onWritePossible, each providing 2 writes on the write handle.
          //We expect a total of 3 writes based on our chunk size.
          //We also expect 4 invocation of writeHandle.remaining(). This is because the first onWritePossible()
          //will lead to writeHandle.remaining() being called thrice (returning 2,1,0) and the second
          //onWritePossible() will lead to writeHandle.remaining() being called once (returning 2)
          //at which point the data is finished.
          {largeInputData, new StrictByteArrayInputStream(largeInputData), 2, 2, 3, 4},
        };
  }

  @Test(dataProvider = "multipleOnWritePossibleDataSources")
  public void testMultipleOnWritePossibleDataSources(final byte[] inputData, final StrictByteArrayInputStream inputStream,
                                                     final int onWritePossibles, final int writesRemainingPerOnWritePossible,
                                                     final int expectedTotalWrites, final int expectedWriteHandleRemainingCalls)
  {
    //Setup:
    final WriteHandle writeHandle = Mockito.mock(WriteHandle.class);
    final MultiPartMIMEInputStream multiPartMIMEInputStream =
        new MultiPartMIMEInputStream.Builder(inputStream, _scheduledExecutorService,
                                             Collections.<String, String>emptyMap()).withWriteChunkSize(TEST_CHUNK_SIZE).build();

    //We want to simulate a decreasing return from writeHandle.remaining().
    //Note that the 0 is added on later so we stop at 1:
    final Integer[] remainingWriteHandleCount;
    if (writesRemainingPerOnWritePossible > 1)
    {
      //This represents writeHandle.remaining() -> n, n -1, n - 2, .... 1, 0
      remainingWriteHandleCount = new Integer[writesRemainingPerOnWritePossible - 1];
      int writeHandleCountTemp = writesRemainingPerOnWritePossible;
      for (int i = 0; i < writesRemainingPerOnWritePossible - 1; i++)
      {
        remainingWriteHandleCount[i] = --writeHandleCountTemp;
      }
    }
    else
    {
      //This represents writeHandle.remaining() -> 1, 0
      remainingWriteHandleCount = new Integer[]{};
    }

    OngoingStubbing<Integer> writeHandleOngoingStubbing = when(writeHandle.remaining());

    for (int i = 0; i < onWritePossibles; i++)
    {
      //Each onWritePossible() will provide the corresponding number of writes remaining on the write handle.
      //When the writeHandle.remaining() reaches zero, onWritePossible() will be invoked again.

      //Mockito does not mention that chaining requires keeping the references and only allows one append at a time.
      //A painful lesson that I had to learn so I will leave a comment here for future people.
      writeHandleOngoingStubbing = writeHandleOngoingStubbing.thenReturn(writesRemainingPerOnWritePossible, remainingWriteHandleCount);

      writeHandleOngoingStubbing = writeHandleOngoingStubbing.thenAnswer(new Answer<Integer>()
      {
        @Override
        public Integer answer(InvocationOnMock invocation) throws Throwable
        {
          //There is no better way to do this with mockito. R2 observes that 0 has been returned and THEN
          //invokes onWritePossible(). We need to make sure that the value of 0 is returned
          //and then onWritePossible() is invoked afterwards.
          _scheduledExecutorService.schedule(new Runnable()
          {
            @Override
            public void run()
            {
              multiPartMIMEInputStream.onWritePossible();
            }
          }, 1000, TimeUnit.MILLISECONDS);
          return 0;
        }
      });
    }

    final ByteArrayOutputStream byteArrayOutputStream = setupMockWriteHandleToOutputStream(writeHandle);

    //Setup for done()
    final CountDownLatch doneLatch = new CountDownLatch(1);
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        doneLatch.countDown();
        return null;
      }
    }).when(writeHandle).done();

    ///////////////////////////////////
    //Start things off

    //Init the data source
    multiPartMIMEInputStream.onInit(writeHandle);
    multiPartMIMEInputStream.onWritePossible();

    //Wait to finish
    try
    {
      boolean successful = doneLatch.await(_testTimeout, TimeUnit.MILLISECONDS);
      if (!successful)
      {
        Assert.fail("Timeout when waiting for input stream to completely transfer");
      }
    }
    catch (Exception exception)
    {
      Assert.fail("Unexpected exception when waiting for input stream to completely transfer");
    }

    ///////////////////////////////////
    //Assert
    Assert.assertEquals(byteArrayOutputStream.toByteArray(), inputData,
                        "All data from the input stream should have successfully been transferred");
    Assert.assertEquals(inputStream.isClosed(), true);

    //Mock verifies:
    //The amount of times we write and the amount of times we call remaining() is the same.
    verify(writeHandle, times(expectedTotalWrites)).write(isA(ByteString.class));
    verify(writeHandle, times(expectedWriteHandleRemainingCalls)).remaining();
    verify(writeHandle, never()).error(isA(Throwable.class));
    verify(writeHandle, times(1)).done();
    verifyNoMoreInteractions(writeHandle);
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  //This test will verify that timeouts on reads are handled properly
  @DataProvider(name = "timeoutDataSources")
  public Object[][] timeoutDataSources() throws Exception
  {
    final StringBuilder builder = new StringBuilder();
    final Random rand = new Random();
    //Create some large data
    for (int i = 0; i < (TEST_CHUNK_SIZE * 120); i++)
    {
      //Randomly populate
      char c = (char) (rand.nextInt(26) + 'a');
      builder.append(c);
    }

    final byte[] largeInputData = builder.toString().getBytes();
    final SlowByteArrayInputStream timeoutFirst = new SlowByteArrayInputStream(largeInputData, 500, 0);
    final SlowByteArrayInputStream timeoutSubsequently = new SlowByteArrayInputStream(largeInputData, 0, 10);

    //TEST_CHUNK_SIZE * 5 writes should be how much data was copied over
    final byte[] largeInputDataPartial = Arrays.copyOf(largeInputData, TEST_CHUNK_SIZE * 5);

    return new Object[][]
        {
          //Timeout on first read. Nothing should have been read. One call on writeHandle.remaining() should have been seen.
          {timeoutFirst, 0, 1, new byte[0]},
          //Timeout on the 6th read. We should expect 5 writes. Six calls on writeHandle.remaining() should have been seen.
          {timeoutSubsequently, 5, 6, largeInputDataPartial}
        };
  }

  @Test(dataProvider = "timeoutDataSources")
  public void testTimeoutDataSources(final SlowByteArrayInputStream slowByteArrayInputStream,
      final int expectedTotalWrites, final int expectedWriteHandleRemainingCalls, final byte[] expectedDataWritten)
  {
    //Setup:
    final WriteHandle writeHandle = Mockito.mock(WriteHandle.class);
    final MultiPartMIMEInputStream multiPartMIMEInputStream =
        new MultiPartMIMEInputStream.Builder(slowByteArrayInputStream, _scheduledExecutorService,
                                             Collections.<String, String>emptyMap()).withWriteChunkSize(TEST_CHUNK_SIZE)
            .withMaximumBlockingTime(45)
            .build();

    //Doesn't matter what we return here as long as its constant and above 0.
    when(writeHandle.remaining()).thenReturn(500);

    final ByteArrayOutputStream byteArrayOutputStream = setupMockWriteHandleToOutputStream(writeHandle);

    //Setup for error()
    final CountDownLatch errorLatch = new CountDownLatch(1);
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        errorLatch.countDown();
        return null;
      }
    }).when(writeHandle).error(isA(Throwable.class));

    ///////////////////////////////////
    //Start things off

    //Init the data source
    multiPartMIMEInputStream.onInit(writeHandle);
    multiPartMIMEInputStream.onWritePossible();

    //Wait to finish
    try
    {
      boolean successful = errorLatch.await(_testTimeout, TimeUnit.MILLISECONDS);
      if (!successful)
      {
        Assert.fail("Timeout when waiting for input stream to completely transfer");
      }
    }
    catch (Exception exception)
    {
      Assert.fail("Unexpected exception when waiting for input stream to transfer");
    }

    ///////////////////////////////////
    //Assert
    Assert.assertEquals(byteArrayOutputStream.toByteArray(), expectedDataWritten,
                        "Partial data should have been transferred in the case of a timeout");
    Assert.assertEquals(slowByteArrayInputStream.isClosed(), true);

    //Mock verifies:
    verify(writeHandle, times(expectedTotalWrites)).write(isA(ByteString.class));
    verify(writeHandle, times(expectedWriteHandleRemainingCalls)).remaining();
    verify(writeHandle, times(1)).error(isA(TimeoutException.class)); //Since we can't override equals
    verify(writeHandle, never()).done();
    verifyNoMoreInteractions(writeHandle);
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  //This test will make sure that exceptions thrown by the read are handled properly
  @DataProvider(name = "exceptionDataSources")
  public Object[][] exceptionDataSources() throws Exception
  {
    final StringBuilder builder = new StringBuilder();
    final Random rand = new Random();
    //Create some large data
    for (int i = 0; i < (TEST_CHUNK_SIZE * 120); i++)
    {
      //Randomly populate
      char c = (char) (rand.nextInt(26) + 'a');
      builder.append(c);
    }

    final byte[] largeInputData = builder.toString().getBytes();
    final ExceptionThrowingByteArrayInputStream exceptionFirst =
        new ExceptionThrowingByteArrayInputStream(largeInputData, 0, new IOException());

    final ExceptionThrowingByteArrayInputStream exceptionSubsequently =
        new ExceptionThrowingByteArrayInputStream(largeInputData, 5, new IOException());

    //TEST_CHUNK_SIZE * 5 writes should be how much data was copied over
    final byte[] largeInputDataPartial = Arrays.copyOf(largeInputData, TEST_CHUNK_SIZE * 5);

    return new Object[][]
        {
            //Timeout on first read. Nothing should have been read. One call on writeHandle.remaining() should have been seen.
            {exceptionFirst, 0, 1, new byte[0]},
            //Timeout on the 6th read. We should expect 5 writes. Six calls on writeHandle.remaining() should have been seen.
            {exceptionSubsequently, 5, 6, largeInputDataPartial}
        };
  }

  @Test(dataProvider = "exceptionDataSources")
  public void testExceptionDataSources(
      final ExceptionThrowingByteArrayInputStream exceptionThrowingByteArrayInputStream, final int expectedTotalWrites,
      final int expectedWriteHandleRemainingCalls, final byte[] expectedDataWritten)
  {
    //Setup:
    final WriteHandle writeHandle = Mockito.mock(WriteHandle.class);
    final MultiPartMIMEInputStream multiPartMIMEInputStream =
        new MultiPartMIMEInputStream.Builder(exceptionThrowingByteArrayInputStream, _scheduledExecutorService,
                                             Collections.<String, String>emptyMap()).withWriteChunkSize(TEST_CHUNK_SIZE).build();

    //Doesn't matter what we return here as long as its constant and above 0.
    when(writeHandle.remaining()).thenReturn(500);

    final ByteArrayOutputStream byteArrayOutputStream = setupMockWriteHandleToOutputStream(writeHandle);

    //Setup for error()
    final CountDownLatch errorLatch = new CountDownLatch(1);
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        errorLatch.countDown();
        return null;
      }
    }).when(writeHandle).error(isA(Throwable.class));

    ///////////////////////////////////
    //Start things off

    //Init the data source
    multiPartMIMEInputStream.onInit(writeHandle);
    multiPartMIMEInputStream.onWritePossible();

    //Wait to finish
    try
    {
      boolean successful = errorLatch.await(_testTimeout, TimeUnit.MILLISECONDS);
      if (!successful)
      {
        Assert.fail("Timeout when waiting for input stream to completely transfer");
      }
    }
    catch (Exception exception)
    {
      Assert.fail("Unexpected exception when waiting for input stream to transfer");
    }

    ///////////////////////////////////
    //Assert
    Assert.assertEquals(byteArrayOutputStream.toByteArray(), expectedDataWritten,
        "Partial data should have been transferred in the case of an exception");
    Assert.assertEquals(exceptionThrowingByteArrayInputStream.isClosed(), true);

    //Mock verifies:
    verify(writeHandle, times(expectedTotalWrites)).write(isA(ByteString.class));
    verify(writeHandle, times(expectedWriteHandleRemainingCalls)).remaining();
    verify(writeHandle, times(1)).error(isA(IOException.class)); //Since we can't override equals
    verify(writeHandle, never()).done();
    verifyNoMoreInteractions(writeHandle);
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  //These tests will verify that MultiPartMIMEInputStream behaves correctly in the face of
  //different bytesRead values inside of the input stream reader task. Essentially we are testing:
  //
  //if (bytesRead == -1) {
  //    1. N==-1. This signifies the stream is complete in the case that we coincidentally read to completion on the
  //    last read from the InputStream.
  //} else if (bytesRead == _writeChunkSize) {
  //    2. N==Capacity. This signifies the most common case which is that we read as many bytes as we originally desired.
  //} else {
  //    3. Capacity > N >= 0. This signifies that the input stream is wrapping up and we just got the last few bytes.
  //
  @DataProvider(name = "differentDataSourceSizes")
  public Object[][] differentDataSourceSizes() throws Exception
  {
    //The data source is evenly divisible by the number of chunks. This should handle case 1 and it should
    //also handle case 2.
    final StringBuilder multipleEvenlyDivisibleChunksBuilder = new StringBuilder();
    for (int i = 0; i < TEST_CHUNK_SIZE * 3; i++)
    {
      multipleEvenlyDivisibleChunksBuilder.append('a');
    }
    final byte[] multipleEvenlyDivisibleChunks = multipleEvenlyDivisibleChunksBuilder.toString().getBytes();

    //Less then one chunk of data. This should handle case 3.
    final StringBuilder lessThenOneChunkBuilder = new StringBuilder();
    for (int i = 0; i < TEST_CHUNK_SIZE - 2; i++)
    {
      lessThenOneChunkBuilder.append('a');
    }
    final byte[] lessThenOneChunk = lessThenOneChunkBuilder.toString().getBytes();

    return new Object[][]
        {
            //One onWritePossible() providing 4 writes on the writeHandle which results in 3 expected writes.
            //We need the 4th write on the writeHandle since we coincidentally have modulo 0 chunk sizes.
            //The first three writes on the write handles write each chunk and the 4th is needed to realize
            //we just reached the end (-1 returned).
            {multipleEvenlyDivisibleChunks, new StrictByteArrayInputStream(multipleEvenlyDivisibleChunks), 4, 4, 3},
            //One OnWritePossible() providing 1 write on the writeHandle, which results in 1 expected write.
            {lessThenOneChunk, new StrictByteArrayInputStream(lessThenOneChunk), 1, 1, 1},

            //Also verify that extra writes handles available do no harm:
            {multipleEvenlyDivisibleChunks, new StrictByteArrayInputStream(multipleEvenlyDivisibleChunks), 10, 4, 3},
            {lessThenOneChunk, new StrictByteArrayInputStream(lessThenOneChunk), 10, 1, 1}
        };
  }

  @Test(dataProvider = "differentDataSourceSizes")
  public void testDifferentDataSourceSizes(final byte[] inputData, final StrictByteArrayInputStream inputStream,
                                           final int writesRemainingPerOnWritePossibles, final int expectedWriteHandleRemaining,
                                           final int expectedTotalWrites)
  {
    //Setup:
    final WriteHandle writeHandle = Mockito.mock(WriteHandle.class);
    final MultiPartMIMEInputStream multiPartMIMEInputStream =
        new MultiPartMIMEInputStream.Builder(inputStream, _scheduledExecutorService,
                                             Collections.<String, String>emptyMap()).withWriteChunkSize(TEST_CHUNK_SIZE).build();

    //Simulate a write handle that offers decreasing writeHandle.remaining()
    final Integer[] remainingWriteHandleCount = simulateDecreasingWriteHandleCount(writesRemainingPerOnWritePossibles);
    when(writeHandle.remaining()).thenReturn(writesRemainingPerOnWritePossibles, remainingWriteHandleCount);

    final ByteArrayOutputStream byteArrayOutputStream = setupMockWriteHandleToOutputStream(writeHandle);

    //Setup for done()
    final CountDownLatch doneLatch = new CountDownLatch(1);
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        doneLatch.countDown();
        return null;
      }
    }).when(writeHandle).done();

    ///////////////////////////////////
    //Start things off

    //Init the data source
    multiPartMIMEInputStream.onInit(writeHandle);
    multiPartMIMEInputStream.onWritePossible();

    //Wait to finish
    try
    {
      boolean successful = doneLatch.await(_testTimeout, TimeUnit.MILLISECONDS);
      if (!successful)
      {
        Assert.fail("Timeout when waiting for input stream to completely transfer");
      }
    }
    catch (Exception exception)
    {
      Assert.fail("Unexpected exception when waiting for input stream to completely transfer");
    }

    ///////////////////////////////////
    //Assert
    Assert.assertEquals(byteArrayOutputStream.toByteArray(), inputData,
        "All data from the input stream should have successfully been transferred");
    Assert.assertEquals(inputStream.isClosed(), true);

    //Mock verifies:
    verify(writeHandle, times(expectedTotalWrites)).write(isA(ByteString.class));
    verify(writeHandle, times(expectedWriteHandleRemaining)).remaining();
    verify(writeHandle, never()).error(isA(Throwable.class));
    verify(writeHandle, times(1)).done();

    verifyNoMoreInteractions(writeHandle);
  }

  ///////////////////////////////////////////////////////////////////////////////////////
  //
  //These tests will verify that aborts work properly and close the input stream regardless of the
  //current state.

  //This test will simulate an abort before any writes requested.
  @Test
  public void testAbortBeforeWrite() throws Exception
  {
    final byte[] smallInputData = "b".getBytes();
    final StrictByteArrayInputStream inputStream = new StrictByteArrayInputStream(smallInputData);
    final StrictByteArrayInputStream spyInputStream = spy(inputStream);

    //Setup:
    final WriteHandle writeHandle = Mockito.mock(WriteHandle.class);
    final MultiPartMIMEInputStream multiPartMIMEInputStream =
        new MultiPartMIMEInputStream.Builder(spyInputStream, _scheduledExecutorService,
                                             Collections.<String, String>emptyMap()).withWriteChunkSize(TEST_CHUNK_SIZE).build();

    //Setup for the close on the input stream.
    //The close must happen for the test to finish.
    final CountDownLatch closeInputStreamLatch = new CountDownLatch(1);
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        closeInputStreamLatch.countDown();
        return null;
      }
    }).when(spyInputStream).close();

    ///////////////////////////////////
    //Start things off

    //Init the data source
    multiPartMIMEInputStream.onInit(writeHandle);
    multiPartMIMEInputStream.onAbort(new IOException());

    //Wait to finish
    try
    {
      boolean successful = closeInputStreamLatch.await(_testTimeout, TimeUnit.MILLISECONDS);
      if (!successful)
      {
        Assert.fail("Timeout when waiting for abort to happen!");
      }
    }
    catch (Exception exception)
    {
      Assert.fail("Unexpected exception when waiting for input stream to be closed!");
    }

    ///////////////////////////////////
    //Mock verifies:
    verify(spyInputStream, times(1)).close();
    verify(spyInputStream, never()).read(isA(byte[].class));
    verify(writeHandle, never()).write(isA(ByteString.class));
    verify(writeHandle, never()).remaining();
    verify(writeHandle, never()).error(isA(Throwable.class));
    verify(writeHandle, never()).done();
    verifyNoMoreInteractions(writeHandle);
  }

  //Abort in the middle of a write task.
  @Test
  public void abortWhenNoOutstandingReadTask() throws Exception
  {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < (TEST_CHUNK_SIZE * 10) + 2; i++)
    {
      builder.append('a');
    }

    //The slow byte array input stream will verify that we call an abort before the first read task is finished.
    final byte[] largeInputData = builder.toString().getBytes();
    final SlowByteArrayInputStream inputStream = new SlowByteArrayInputStream(largeInputData, 300, 10);
    final SlowByteArrayInputStream spyInputStream = spy(inputStream);

    //Setup:
    final WriteHandle writeHandle = Mockito.mock(WriteHandle.class);
    final MultiPartMIMEInputStream multiPartMIMEInputStream =
        new MultiPartMIMEInputStream.Builder(spyInputStream, _scheduledExecutorService,
                                             Collections.<String, String>emptyMap()).withWriteChunkSize(TEST_CHUNK_SIZE).build();

    //By the time the first onWritePossible() completes, half the data should be transferred
    //Then the abort task will run.
    when(writeHandle.remaining()).thenReturn(5, new Integer[]{4, 3, 2, 1, 0});

    final ByteArrayOutputStream byteArrayOutputStream = setupMockWriteHandleToOutputStream(writeHandle);

    //Setup for the close on the input stream.
    //The close must happen for the test to finish.
    final CountDownLatch closeInputStreamLatch = new CountDownLatch(1);
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        closeInputStreamLatch.countDown();
        return null;
      }
    }).when(spyInputStream).close();

    ///////////////////////////////////
    //Start things off

    //Init the data source
    multiPartMIMEInputStream.onInit(writeHandle);
    multiPartMIMEInputStream.onWritePossible();
    multiPartMIMEInputStream.onAbort(new IOException());

    //Wait to finish
    try
    {
      boolean successful = closeInputStreamLatch.await(_testTimeout, TimeUnit.MILLISECONDS);
      if (!successful)
      {
        Assert.fail("Timeout when waiting for abort to happen!");
      }
    }
    catch (Exception exception)
    {
      Assert.fail("Unexpected exception when waiting for input stream to be closed!");
    }

    ///////////////////////////////////
    //Assert
    final byte[] expectedBytes = Arrays.copyOf(largeInputData, TEST_CHUNK_SIZE * 5);
    Assert.assertEquals(byteArrayOutputStream.toByteArray(), expectedBytes,
        "Partial data from the input stream should have successfully been transferred");

    //Mock verifies:
    verify(spyInputStream, times(1)).close();
    verify(spyInputStream, times(5)).read(isA(byte[].class));
    verify(writeHandle, times(5)).write(isA(ByteString.class));
    verify(writeHandle, times(6)).remaining();
    verify(writeHandle, never()).error(isA(Throwable.class));
    verify(writeHandle, never()).done();
    verifyNoMoreInteractions(writeHandle);
  }

  //We want to simulate a decreasing return from writeHandle.remaining().
  //I.e 3, 2, 1, 0....
  private static Integer[] simulateDecreasingWriteHandleCount(final int writesRemainingPerOnWritePossibles)
  {
    final Integer[] remainingWriteHandleCount;
    if (writesRemainingPerOnWritePossibles > 1)
    {
      //This represents writeHandle.remaining() -> n, n -1, n - 2, .... 1, 0
      remainingWriteHandleCount = new Integer[writesRemainingPerOnWritePossibles];
      int writeHandleCountTemp = writesRemainingPerOnWritePossibles;
      for (int i = 0; i < writesRemainingPerOnWritePossibles; i++)
      {
        remainingWriteHandleCount[i] = --writeHandleCountTemp;
      }
    }
    else
    {
      //This represents writeHandle.remaining() -> 1, 0
      remainingWriteHandleCount = new Integer[]{0};
    }

    return remainingWriteHandleCount;
  }

  //Adjust the write handle so that when its written to it writes to the ByteArrayOutputStream so we can aggregate the bytes.
  private static ByteArrayOutputStream setupMockWriteHandleToOutputStream(final WriteHandle writeHandle)
  {
    //When data is written to the write handle, we append to the buffer
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    doAnswer(new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        final ByteString argument = (ByteString) invocation.getArguments()[0];
        appendByteStringToBuffer(byteArrayOutputStream, argument);
        return null;
      }
    }).when(writeHandle).write(isA(ByteString.class));
    return byteArrayOutputStream;
  }

  private static void appendByteStringToBuffer(final ByteArrayOutputStream outputStream, final ByteString byteString)
  {
    try
    {
      outputStream.write(byteString.copyBytes());
    }
    catch (Exception e)
    {
      Assert.fail();
    }
  }
}