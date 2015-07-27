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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * A wrapper around an {@link java.io.InputStream} to function as a data source to for a
 * {@link com.linkedin.multipart.MultiPartMIMEWriter}. This class uses the provided
 * {@link java.util.concurrent.ExecutorService} to perform blocking reads and respond to write requests
 * asynchronously.
 *
 * This class will close the underlying input stream when either of these events happen:
 * 1. The stream is finished being read.
 * 2. There was an exception reading the stream, at which point the stream is closed and error() is called on the write
 * handle.
 * 3. The writer was aborted.
 *
 * @author Karim Vidhani
 */
public final class MultiPartMIMEInputStream implements MultiPartMIMEDataSourceWriter
{
  public static final int DEFAULT_MAXIMUM_BLOCKING_DURATION = 3000;
  public static final int DEFAULT_WRITE_CHUNK_SIZE = 5000;
  public static final int DEFAULT_ABORT_INPUT_STREAM_TIMEOUT = 5000;

  private volatile WriteHandle _writeHandle;
  private final Map<String, String> _headers;
  private final InputStream _inputStream;
  private volatile boolean _dataSourceFinished = false;
  //since there is no way to see if an InputStream has already been closed
  private final ExecutorService _executorService;
  private final int _maximumBlockingTime;
  private final int _writeChunkSize;
  private final int _abortTimeout;
  private volatile Future<?> _currentReadTask = null;

  @Override
  public void onInit(final WriteHandle wh)
  {
    _writeHandle = wh;
  }

  @Override
  public void onWritePossible()
  {
    //Delegate the while (writeHandle.remaining() > 0) task to the input stream reader manager task.
    //Its considerably easier this way then spawning a new manager task for each write available on
    //the writeHandle.
    _currentReadTask = _executorService.submit(new InputStreamReaderManager());
  }

  //Indicates there was a request to abort writing that was not this particular part's fault.
  @Override
  public void onAbort(Throwable e)
  {
    //Ideally we would like to just do _inputStream.close(), but that is not thread safe since
    //we could be in the middle of a read. Hence we spawn a task that performs a close() on the
    //input stream any outstanding read tasks are finished. If there was already a manager thread in the middle of
    //attempting to honor all the writes on the write handle we let him finish first.
    _executorService.submit(new InputStreamCloser());
  }

  @Override
  public Map<String, String> dataSourceHeaders()
  {
    return Collections.unmodifiableMap(_headers);
  }

  private class InputStreamCloser implements Runnable
  {
    private InputStreamCloser()
    {
    }

    @Override
    public void run()
    {
      try
      {
        //In order to prevent the performance overhead of synchronization on the input stream,
        //we simply wait for the outstanding reader task on the input stream to finish before we
        //close it.
        //There is no danger of a race condition here because R2 will never call onWritePossible() after
        //signalling abort.

        //No need to wait if the abort came before any writes were requested.
        if (_currentReadTask != null)
        {
          //If there was no currently running task, _currentReadTask will represent the most recent
          //completed reading task. In such a case the .get() will return right away.
          _currentReadTask.get(_abortTimeout, TimeUnit.MILLISECONDS);
        }
      }
      catch (InterruptedException interruptedException)
      {
        //Safe to swallow
      }
      catch (ExecutionException executionException)
      {
        //Safe to swallow
      }
      catch (CancellationException cancellationException)
      {
        //Safe to swallow
      }
      catch (TimeoutException timeoutException)
      {
        //Safe to swallow
      }
      finally
      {
        try
        {
          _inputStream.close();
        }
        catch (IOException ioException)
        {
          //Safe to swallow
        }
      }
    }
  }

  private class InputStreamReaderManager implements Runnable
  {
    private InputStreamReaderManager()
    {
    }

    @Override
    public void run()
    {
      //We use two threads from the client provided thread pool. We must use this technique
      //because if the read from the input stream hangs, there is no way to recover.
      //Therefore we have a reader thread which performs the actual read and and a boss thread that waits for the reader
      //to complete.

      //We want to invoke the callback on only one (boss) thread because:
      //1. It makes the threading model more complicated if the reader thread invokes the callbacks as well as
      //the boss thread.
      //2. In cases where there is a timeout, meaning the read took too long, we want to close the
      //input stream on the boss thread to unblock the reader thread. In such cases it is indeterminate
      //as to what the behavior is when the aforementioned blocked thread wakes up. This thread could receive an IOException
      //or come back with a few bytes. Due to this indeterminate behavior we don't want to trust the reader thread
      //to invoke any callbacks. We want our boss thread to explicitly invoke error() in such cases.

      while (_writeHandle.remaining() > 0)
      {
        //Note that we follow the orthodox writer pattern here by honoring all writes available on the writeHandle.
        //Furthermore this logic will guarantee serial and sequential reading through the input stream.
        final CountDownLatch latch = new CountDownLatch(1);
        final InputStreamReader inputStreamReader = new InputStreamReader(latch);
        _executorService.submit(inputStreamReader);

        try
        {
          boolean nonTimeoutFinish = latch.await(_maximumBlockingTime, TimeUnit.MILLISECONDS);
          if (nonTimeoutFinish)
          {
            if (inputStreamReader._result != null)
            {
              //If the call went through, extract the info and write
              if (_dataSourceFinished)
              {
                //No sense in writing an empty ByteString. This happens if we the data source was coincidentally
                //evenly divisible by the chunk size OR the input stream returned 0 bytes.
                if (!inputStreamReader._result.equals(ByteString.empty()))
                {
                  _writeHandle.write(inputStreamReader._result);
                }
                _writeHandle.done();
                //Close the stream since we won't be invoked again
                try
                {
                  _inputStream.close();
                }
                catch (IOException ioException)
                {
                  //Safe to swallow
                  //An exception thrown when we try to close the InputStream should not really
                  //make its way down as an error...
                }
                //Break here, even though there may be more writes on the writeHandle.
                //We cannot continue writing if our data source has finished.
                break;
              }
              else
              {
                //Just a normal write
                _writeHandle.write(inputStreamReader._result);
              }
            }
            else
            {
              //This means the result is null which implies that a throwable exists.
              //In this case the read on the InputStream threw an IOException.
              //First close the InputStream:
              try
              {
                _inputStream.close();
              }
              catch (IOException ioException)
              {
                //Safe to swallow
                //An exception thrown when we try to close the InputStream should not really
                //make its way down as an error...
              }
              //Now mark is it an error using the correct throwable:
              _writeHandle.error(inputStreamReader._error);
              //Break here, even though there may be more writes on the writeHandle.
              //We cannot continue writing in this condition.
              break;
            }
          }
          else
          {
            //There was a timeout when trying to read
            try
            {
              //Close the input stream here since we won't be doing any more reading. This should also potentially
              //free up any threads blocked on the read()
              _inputStream.close();
            }
            catch (IOException ioException)
            {
              //Safe to swallow
            }
            _writeHandle.error(new TimeoutException("InputStream reading timed out"));
            //Break here, even though there may be more writes on the writeHandle.
            //We cannot continue writing in this condition.
            break;
          }
        }
        catch (InterruptedException exception)
        {
          //If this thread interrupted, then we have no choice but to abort everything.
          try
          {
            //Close the input stream here since we won't be doing any more reading.
            _inputStream.close();
          }
          catch (IOException ioException)
          {
            //Safe to swallow
          }
          _writeHandle.error(exception);
          //Break here, even though there may be more writes on the writeHandle.
          //We cannot continue writing in this condition.
          break;
        }
      }
    }
  }

  private class InputStreamReader implements Runnable
  {
    private final CountDownLatch _countDownLatch;
    private ByteString _result = null;
    private Throwable _error = null;

    @Override
    public void run()
    {
      try
      {
        final byte[] bytes = new byte[_writeChunkSize];
        final int bytesRead = _inputStream.read(bytes);
        //The number of bytes 'N' here could be the following:
        if (bytesRead == -1)
        {
          //1. N==-1. This signifies the stream is complete in the case that we coincidentally read to completion on the
          //last read from the InputStream.
          _dataSourceFinished = true;
          _result = ByteString.empty();
        }
        else if (bytesRead == _writeChunkSize)
        {
          //2. N==Capacity. This signifies the most common case which is that we read as many bytes as we originally desired.
          _result = ByteString.copy(bytes);
        }
        else
        {
          //3. Capacity > N >= 0. This signifies that the input stream is wrapping up and we just got the last few bytes.
          _dataSourceFinished = true;
          _result = ByteString.copy(bytes, 0, bytesRead);
        }
      }
      catch (IOException ioException)
      {
        _error = ioException;
      }
      finally
      {
        _countDownLatch.countDown();
      }
    }

    private InputStreamReader(final CountDownLatch latch)
    {
      _countDownLatch = latch;
    }
  }

  /**
   * Builder to create a new instance of a MultiPartMIMEInputStream that wraps the provided InputStream.
   */
  public static class Builder
  {
    private final InputStream _inputStream;
    private final ExecutorService _executorService;
    private final Map<String, String> _headers;
    private int _maximumBlockingTime = DEFAULT_MAXIMUM_BLOCKING_DURATION;
    private int _writeChunkSize = DEFAULT_WRITE_CHUNK_SIZE;
    private int _abortTimeout = DEFAULT_ABORT_INPUT_STREAM_TIMEOUT;

    /**
     * Construct the builder to eventually build a MultiPartMIMEInputStream.
     *
     * @param inputStream the input stream to wrap.
     * @param executorService the thread pool to run jobs to read.
     * @param headers the headers representing this part.
     */
    public Builder(final InputStream inputStream, final ExecutorService executorService,
        final Map<String, String> headers)
    {
      _inputStream = inputStream;
      _executorService = executorService;
      _headers = headers;
    }

    /**
     * The maximum amount of time to wait, in milliseconds, on a synchronous read from the underlying input stream.
     *
     * @param maximumBlockingTime
     */
    public Builder withMaximumBlockingTime(final int maximumBlockingTime)
    {
      _maximumBlockingTime = maximumBlockingTime;
      return this;
    }

    /**
     * The number of bytes to read from the input stream in order to fulfill write request.
     *
     * @param writeChunkSize
     *
     */
    public Builder withWriteChunkSize(final int writeChunkSize)
    {
      _writeChunkSize = writeChunkSize;
      return this;
    }

    /**
     * The maximum amount of time to wait, in milliseconds, that a request to abort has to wait
     * for any outstanding read (and therefore write) operations to finish.
     *
     * @param abortTimeout
     */
    public Builder withDefaultAbortInputStreamTimeout(final int abortTimeout)
    {
      _abortTimeout = abortTimeout;
      return this;
    }

    /**
     * Build and return the MultiPartMIMEInputStream.
     */
    public MultiPartMIMEInputStream build()
    {
      return new MultiPartMIMEInputStream(_inputStream,
          _executorService,
          _headers,
          _maximumBlockingTime,
          _writeChunkSize,
          _abortTimeout);
    }
  }

  //Private construction due to the builder
  private MultiPartMIMEInputStream(final InputStream inputStream, final ExecutorService executorService,
      final Map<String, String> headers, final int maximumBlockingTime, final int writeChunkSize,
      final int abortTimeout)
  {
    _inputStream = inputStream;
    _executorService = executorService;
    _headers = new HashMap<String, String>(headers); //defensive copy
    _maximumBlockingTime = maximumBlockingTime;
    _writeChunkSize = writeChunkSize;
    _abortTimeout = abortTimeout;
  }
}