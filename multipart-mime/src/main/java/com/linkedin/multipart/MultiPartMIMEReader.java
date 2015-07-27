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
import com.linkedin.multipart.exceptions.MultiPartIllegalFormatException;
import com.linkedin.multipart.exceptions.MultiPartReaderFinishedException;
import com.linkedin.multipart.exceptions.SinglePartBindException;
import com.linkedin.multipart.exceptions.SinglePartFinishedException;
import com.linkedin.multipart.exceptions.SinglePartNotInitializedException;
import com.linkedin.multipart.exceptions.StreamBusyException;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.ReadHandle;
import com.linkedin.r2.message.stream.entitystream.Reader;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.util.LinkedDeque;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.Callable;


/**
 * Zero copy, async streaming multipart mime reader based on the official RFC for multipart/mime.
 *
 * This class uses R2 streaming and a look-ahead buffer to allow clients to walk through the data of all parts using an async,
 * callback based approach.
 *
 * Clients must first create a callback of type {@link com.linkedin.multipart.MultiPartMIMEReaderCallback} to pass to
 * {@link MultiPartMIMEReader#registerReaderCallback(com.linkedin.multipart.MultiPartMIMEReaderCallback)}. This is the first
 * step to using the MultiPartMIMEReader.
 *
 * Upon registration, at some time in the future, MultiPartMIMEReader will create {@link com.linkedin.multipart.MultiPartMIMEReader.SinglePartMIMEReader}s
 * which will be passed to {@link MultiPartMIMEReaderCallback#onNewPart(com.linkedin.multipart.MultiPartMIMEReader.SinglePartMIMEReader)}.
 *
 * Clients will then have to create an instance of {@link com.linkedin.multipart.SinglePartMIMEReaderCallback} to bind
 * and commit to reading these parts.
 *
 * Note that NONE of the APIs in this class are thread safe. Furthermore it is to be noted that API calls must be event driven.
 * For example, asking for more part data using {@link com.linkedin.multipart.MultiPartMIMEReader.SinglePartMIMEReader#requestPartData()}
 * can only be done either upon binding to the {@link com.linkedin.multipart.MultiPartMIMEReader.SinglePartMIMEReader} or
 * after receiving data on {@link SinglePartMIMEReaderCallback#onPartDataAvailable(com.linkedin.data.ByteString)}. Therefore
 * attempting to queue multiple reads, instead of waiting for callback invocations to drive forward, will result in runtime exceptions.
 *
 * @author Karim Vidhani
 */
public class MultiPartMIMEReader implements MultiPartMIMEDataSourceIterator
{
  private final R2MultiPartMIMEReader _reader;
  private final EntityStream _entityStream;
  private volatile MultiPartMIMEReaderCallback _clientCallback;
  private volatile String _preamble;
  private volatile MultiPartReaderState _multiPartReaderState;
  private volatile SinglePartMIMEReader _currentSinglePartMIMEReader;

  class R2MultiPartMIMEReader implements Reader
  {
    private volatile ReadHandle _rh;
    private volatile ByteString _compoundByteStringBuffer = ByteString.empty();
    //The reason for the first boundary vs normal boundary difference is because the first boundary MAY be missing the
    //leading CRLF.
    //Note that even though it is incorrect for a client to send a multipart/mime payload in this manner,
    //the RFC states that readers should be tolerant and be able to handle such cases.
    private final String _firstBoundary;
    private final String _normalBoundary;
    private final String _finishingBoundary;
    private byte[] _firstBoundaryBytes;
    private byte[] _normalBoundaryBytes;
    private byte[] _finishingBoundaryBytes;
    private volatile boolean _firstBoundaryEvaluated = false;
    //A signal from the R2 reader has been notified that all data is done being sent over. This does NOT mean that our
    //top level reader can be notified that they are done since data could still be in the buffer.
    private volatile boolean _r2Done = false;

    //These two fields are needed to support our iterative invocation of callbacks so that we don't end up with a recursive loop
    //which would lead to a stack overflow.
    private final Queue<Callable<Void>> _callbackQueue = new LinkedDeque<Callable<Void>>();
    private volatile boolean _callbackInProgress = false;

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    //Reader interface implementation

    @Override
    public void onInit(ReadHandle rh)
    {
      //If there was a top level abandon performed without the registration of a callback, then at this point
      //_multiPartReaderState will be FINISHED. Therefore we just cancel and return.
      if (_multiPartReaderState == MultiPartReaderState.FINISHED)
      {
        rh.cancel();
        return;
      }

      //Otherwise start the reading process since the top level callback has been bound.
      //Note that we read ahead here and we read only what we need. Our policy is always to read only 1 chunk at a time.
      _rh = rh;
      _rh.request(1);
    }

    @Override
    public void onDataAvailable(ByteString data)
    {
      //A response for our _rh.request(1) has come
      processEventAndInvokeClient(data);
    }

    @Override
    public void onDone()
    {
      //Be careful, we still could have space left in our buffer.
      _r2Done = true;
      //We need to trigger onDataAvailable() again with empty data because there is an outstanding request
      //to _rh.request(1).
      processEventAndInvokeClient();
    }

    @Override
    public void onError(Throwable e)
    {
      //R2 has informed us of an error. So we notify our readers and shut things down.

      //It is important to note that R2 will only call onError only once we exit onDataAvailable().
      //Therefore there are no concurrency issues to be concerned with. If we are going back and forth and honoring
      //client requests using data from memory we will eventually need to ask R2 for more data. At this point,
      //onError() will be called by R2 and we can clean up state and notify our clients on their callbacks.

      //It could be the case that we already finished, or reached an erroneous state earlier on our own
      //(i.e malformed multipart mime body or a client threw an exception when we invoked their callback).
      //In such a case, just return.
      if (_multiPartReaderState == MultiPartReaderState.FINISHED)
      {
        return;
      }

      handleExceptions(e);
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    //Core look-ahead buffering and callback logic begins here:

    //Client APIs invoke this method
    private void processEventAndInvokeClient()
    {
      processEventAndInvokeClient(ByteString.empty());
    }

    //OnDataAvailable() will invoke this method
    private void processEventAndInvokeClient(final ByteString data)
    {
      //Note that only one thread should ever be calling processEventAndInvokeClient() at any time.
      //We control invocation of this method using the various states.
      //Client API calls (from the readers) will call processEventAndInvokeClient() to refresh the logic and drive forward
      //in a forcefully sequential manner.
      //When more data is needed to fulfill a client's request, we will ask R2 and R2 will call us sequentially as well.
      //During the time we are waiting for R2 to fulfill our request, client API calls cannot do anything further.
      //It is for this reason we don't have to synchronize any data used within this method. This is a big
      //performance win.
      //
      //Any runtime exceptions thrown by us will make it to R2 or to our clients, depending on who invoked us.
      //This should ideally never ever happen and if it happens its a bug.
      //If R2 does receive a RuntimeException from us then:
      //A. In case of the server, R2 send back a 500 internal server error.
      //B. In case of the client, R2 will close the connection.

      if (checkAndProcessEpilogue())
      {
        return;
      }

      if (checkAndProcessAbandonment())
      {
        return;
      }

      //Read data into our local buffer for further processing. All subsequent operations require this.
      _compoundByteStringBuffer = new ByteString.Builder().append(_compoundByteStringBuffer).append(data).build();

      if (checkAndProcessPreamble())
      {
        return;
      }

      //Since this is the last step we end up returning anyway.
      performPartReading();
    }

    //This method is used to iteratively invoke our callbacks to prevent a stack overflow.
    private void processAndInvokeCallableQueue()
    {
      //This variable indicates that there is no current iterative invocation taking place. We can start one here.
      _callbackInProgress = true;

      while (!_callbackQueue.isEmpty())
      {
        final Callable<Void> callable = _callbackQueue.poll();
        try
        {
          callable.call();
        }
        catch (Throwable clientCallbackException)
        {
          handleExceptions(clientCallbackException);
        }
      }
      _callbackInProgress = false;
    }

    private boolean checkAndProcessEpilogue()
    {
      //Drop the epilogue on the ground. No need to read into our buffer.
      if (_multiPartReaderState == MultiPartReaderState.READING_EPILOGUE)
      {
        if (_r2Done)
        {
          //If r2 has already notified we are done, we can wrap up. There is no need to use our
          //iterative technique to call this callback because a client cannot possibly invoke us again.
          _multiPartReaderState = MultiPartReaderState.FINISHED;
          try
          {
            //This can throw so we need to notify the client that their APIs threw an exception when we invoked them.
            MultiPartMIMEReader.this._clientCallback.onFinished();
          }
          catch (RuntimeException clientCallbackException)
          {
            handleExceptions(clientCallbackException);
          }

          return true; //Regardless of whether the invocation to onFinished() threw or not we need to return here
        }
        //Otherwise r2 has not notified us that we are done. So we keep getting more bytes and dropping them.
        _rh.request(1);
        return true;
      }

      return false;
    }

    private boolean checkAndProcessAbandonment()
    {
      //Drop bytes for a top level abandonment.
      if (_multiPartReaderState == MultiPartReaderState.ABANDONING)
      {
        if (_r2Done)
        {
          //If r2 has already notified we are done, we can wrap up. No need to look at remaining bytes in buffer.
          //Also there is no need to use our iterative technique to call this callback because a client cannot
          //possibly invoke us again.
          _multiPartReaderState = MultiPartReaderState.FINISHED;
          try
          {
            //This can throw so we need to notify the client that their APIs threw an exception when we invoked them.
            MultiPartMIMEReader.this._clientCallback.onAbandoned();
          }
          catch (RuntimeException clientCallbackException)
          {
            handleExceptions(clientCallbackException);
          }

          return true; //Regardless of whether the invocation to onFinished() threw or not we need to return here
        }
        //Otherwise we keep on chugging forward and dropping bytes.
        _rh.request(1);
        return true;
      }

      return false;
    }

    private boolean checkAndProcessPreamble()
    {
      //Read the preamble in.
      if (_multiPartReaderState == MultiPartReaderState.CALLBACK_BOUND_AND_READING_PREAMBLE)
      {
        final int firstBoundaryLookup = _compoundByteStringBuffer.indexOfBytes(_firstBoundaryBytes);
        final int lastBoundaryLookup = _compoundByteStringBuffer.indexOfBytes(_finishingBoundaryBytes);

        //Before reading the preamble, check to see if this is an empty multipart mime envelope. This can be checked by
        //examining if the location of the first boundary matches the location of the finishing boundary.
        //We also have to take into consideration that the first boundary doesn't include the leading CRLF so we subtract
        //the length of the CRLF when doing the comparison.
        //This means that the envelope looked like:
        //Content-Type: multipart/<subType>; boundary="someBoundary"
        //
        //--someBoundary--
        if (firstBoundaryLookup - MultiPartMIMEUtils.CRLF_BYTES.length == lastBoundaryLookup)
        {
          //In such a case we need to let the client know that reading is complete since there were no parts.
          //There is no need to use our iterative technique to call this callback because a
          //client cannot possibly invoke us again.
          _multiPartReaderState = MultiPartReaderState.FINISHED;
          try
          {
            //This can throw so we need to notify the client that their APIs threw an exception when we invoked them.
            MultiPartMIMEReader.this._clientCallback.onFinished();
          }
          catch (RuntimeException clientCallbackException)
          {
            handleExceptions(clientCallbackException);
          }

          return true; //Regardless of whether the invocation to onFinished() threw or not we need to return here.
        }

        //Otherwise proceed.
        if (firstBoundaryLookup > -1)
        {
          //The boundary has been found. Everything up until this point is the preamble.
          final ByteString preambleSlice = _compoundByteStringBuffer.slice(0, firstBoundaryLookup);
          _preamble = preambleSlice.asString(Charset.defaultCharset());

          //Make a new copy with the bytes we need leaving the old list to be GC'd
          _compoundByteStringBuffer = _compoundByteStringBuffer.slice(firstBoundaryLookup, _compoundByteStringBuffer.length() - firstBoundaryLookup);

          //We can now transition to normal reading.
          _multiPartReaderState = MultiPartReaderState.READING_PARTS;
        }
        else
        {
          //The boundary has not been found in the buffer, so keep looking.
          if (_r2Done)
          {
            //If this happens that means that there was a problem. This means that r2 has
            //fully given us all of the stream and we haven't found the boundary.
            handleExceptions(new MultiPartIllegalFormatException("Malformed multipart mime request. No boundary found!"));
            return true; //We are in an unusable state so we return here.
          }
          _rh.request(1);
          return true;
        }
      }
      return false;
    }

    private boolean checkForSufficientBufferSize()
    {
      //We buffer forward a bit if we have is less then the finishing boundary size.
      //This is the minimum amount of data we need in the buffer before we can go forward.
      if (_compoundByteStringBuffer.length() < _finishingBoundaryBytes.length)
      {
        //If this happens and r2 has not notified us that we are done, then this is a problem. This means that
        //r2 has already notified that we are done and we didn't see the finishing boundary earlier.
        //This should never happen.
        if (_r2Done)
        {
          //Notify the reader of the issue.
          handleExceptions(new MultiPartIllegalFormatException("Malformed multipart mime request. Finishing boundary missing!"));
          return true; //We are in an unusable state so we return here.
        }

        //Otherwise we need to read in some more data.
        _rh.request(1);
        return true;
      }

      return false;
    }

    private void performPartReading()
    {
      if (checkForSufficientBufferSize())
      {
        return;
      }

      //READING_PARTS represents normal part reading operation and is where most of the time will be spent.
      //At this point in time in our logic this is guaranteed to be in this state.
      assert (_multiPartReaderState == MultiPartReaderState.READING_PARTS);

      //The goal of the logic here is the following:
      //1. If the buffer does not start with the boundary, then we fully consume as much of the buffer as possible.
      //We notify clients of as much data we can drain. Note that in such a case, even if the buffer does not start with
      //the boundary, it could still contain the boundary. In such a case we read up until the boundary. In this situation
      //the bytes read would be the last bits of data they need for the current part. Subsequent invocations of
      //requestPartData() would then lead to the buffer starting with the boundary.
      //
      //2. Otherwise if the buffer does start with boundary then we wrap up the previous part and
      //begin the new one.

      //Another invariant to note is that the result of this logic below will result in ONE of the
      //following (assuming there are no error conditions):
      //1. onPartDataAvailable()
      //OR
      //2. OnAbandoned() on SinglePartCallback followed by onNewPart() on MultiPartCallback
      //OR
      //3. OnFinished() on SinglePartCallback followed by onNewPart() on MultiPartCallback
      //OR
      //4. OnAbandoned() on SinglePartCallback followed by onFinished() on MultiPartCallback
      //OR
      //5. OnFinished() on SinglePartCallback followed by onFinished() on MultiPartCallback
      //
      //Note that onPartDataAvailable() and onNewPart() are never called one after another in the logic
      //below because upon invocation of these callbacks, clients may come back to us immediately
      //and it can potentially lead to very confusing states. Furthermore its also more intuitive
      //to answer each client's request with only one callback. This also allows us to use the iterative
      //callback invocation technique and return at a location in the code that is different then the original
      //invocation location.

      final int boundaryIndex;
      final int boundarySize;
      if (_firstBoundaryEvaluated == false)
      {
        //Immediately after the preamble, i.e the first part we are seeing
        boundaryIndex = _compoundByteStringBuffer.indexOfBytes(_firstBoundaryBytes);
        boundarySize = _firstBoundaryBytes.length;
      }
      else
      {
        boundaryIndex = _compoundByteStringBuffer.indexOfBytes(_normalBoundaryBytes);
        boundarySize = _normalBoundaryBytes.length;
      }

      //Continue processing part data based on whether or not the buffer begins with a boundary.
      //We return no matter what anyway, so no need to check to see if we should return for either of these.
      if (boundaryIndex != 0)
      {
        processBufferStartingWithoutBoundary(boundaryIndex);
      }
      else
      {
        processBufferStartingWithBoundary(boundarySize);
      }
    }

    private void processBufferStartingWithoutBoundary(final int boundaryIndex)
    {
      //We proceed forward since a reader SHOULD be ready at this point. By ready we mean that:
      //1. They are ready to receive requested data on their onPartDataAvailable() callback, meaning
      //REQUESTED_DATA.
      //or
      //2. They have requested an abandonment and are waiting for it to finish, meaning REQUESTED_ABANDON.
      //
      //It is further important to note that in the current implementation, the reader will ALWAYS be ready at this point in time.
      //This is because we strictly allow only our clients to push us forward. This means they must be in a ready state
      //when all of this logic is executed.
      //
      //Formally, here is why we don't do _rh.request(2)...i.e _rh.request(n>1):
      //A. If we did this, the first onDataAvailable() invoked by R2 would potentially satisfy a client's
      //request. The second onDataAvailable() invoked by R2 would then just write data into the local buffer. However
      //now we have to distinguish on whether or not the client drove us forward by refreshing us or our desire for more data
      //drove us forward. This leads to more complication and also performs reading of data that we don't need yet.
      //
      //B. Multiple threads could call the logic here concurrently thereby violating the guarantee we get that
      //the logic here is only run by one thread concurrently. For example:
      //If we did a _rh.request(2), then the first invocation of onDataAvailable() would satisfy a
      //client's request. The client could then drive us forward again by invoking onPartDataAvailable()
      //to refresh the logic. However at this time the answer to our second _rh.request() could also come in
      //thereby causing multiple threads to operate in an area where there is no synchronization.

      //Note that _currentSinglePartMIMEReader is guaranteed to be non-null at this point.
      assert(_currentSinglePartMIMEReader != null);

      final SingleReaderState currentState = _currentSinglePartMIMEReader._singleReaderState;

      //Assert on our invariant described above.
      assert (currentState == SingleReaderState.REQUESTED_DATA || currentState == SingleReaderState.REQUESTED_ABANDON);

      //We know the buffer doesn't begin with the boundary, but we can take different action if a boundary
      //exists in the buffer. This way we can consume the maximum amount of data.
      //If the boundary exists in the buffer we know we can read right up until it begins.
      //If it doesn't the maximum we can read out is limited (since we don't want to consume possible
      //future boundary data).
      if (boundaryIndex == -1)
      {
        //Boundary doesn't exist here, let's drain what we can.
        processBufferNotContainingBoundary(currentState);
      }
      else
      {
        //Boundary is in buffer. We can only consume data up until the boundary.
        processBufferContainingBoundary(boundaryIndex, currentState);
      }
    }

    private void processBufferNotContainingBoundary(final SingleReaderState singleReaderState)
    {
      //Note that we can't fully drain the buffer because the end of the buffer may include the partial
      //beginning of the next boundary.
      final int amountToLeaveBehind = _normalBoundaryBytes.length - 1;
      final int maxAmountAvailableForClient = _compoundByteStringBuffer.length() - amountToLeaveBehind;

      final ByteString clientData = decomposeClientDataAndUpdateBuffer(maxAmountAvailableForClient);

      if (singleReaderState == SingleReaderState.REQUESTED_DATA)
      {
        //We must set this before we provide the data. Otherwise if the client immediately decides to requestPartData()
        //they will see an exception because we are still in REQUESTED_DATA.
        _currentSinglePartMIMEReader._singleReaderState = SingleReaderState.CALLBACK_BOUND_AND_READY;

        //This effectively does:
        //_currentSinglePartMIMEReader._callback.onPartDataAvailable(clientData);
        final Callable<Void> onPartDataAvailableInvocation =
            new OnPartDataCallable(_currentSinglePartMIMEReader._callback, clientData);

        //Queue up this operation
        _callbackQueue.add(onPartDataAvailableInvocation);

        //If the while loop before us is in progress, we just return
        if (_callbackInProgress)
        {
          //At this point since callbackInProgress is true, we know that we have a client callback invocation in our
          //call stack. The while loop (see processAndInvokeCallableQueue) that is now executing the callback in our
          //call stack will also execute an invocation to the newly added callback.
          return;
        }
        else
        {
          processAndInvokeCallableQueue();
          //If invoking the callables resulting in things stopping, we will return anyway.
        }

        //The client single part reader can then drive forward themselves.
      }
      else
      {
        //This is an abandon operation, so we need to drop the bytes and keep moving forward.
        //Note that we don't have a client to drive us forward so we do it ourselves.
        final Callable<Void> recursiveCallable = new RecursiveCallable(this);

        //Queue up this operation
        _callbackQueue.add(recursiveCallable);

        //If the while loop before us is in progress, we just return
        if (_callbackInProgress)
        {
          //At this point since callbackInProgress is true, we know that we have a client callback invocation in our
          //call stack. The while loop (see processAndInvokeCallableQueue) that is now executing the callback in our
          //call stack will also execute an invocation to the newly added callback.
          return;
        }
        else
        {
          processAndInvokeCallableQueue();
          //If invoking the callables resulting in things stopping, we will return anyway.
        }
        //No need to explicitly return here.
      }
    }

    private void processBufferContainingBoundary(final int boundaryIndex, final SingleReaderState singleReaderState)
    {
      //Could be normal boundary or it could be finishing boundary. We can only construct client data right up
      //until the boundary's location.
      final int maxAmountAvailableForClient = boundaryIndex;

      final ByteString clientData = decomposeClientDataAndUpdateBuffer(maxAmountAvailableForClient);

      if (singleReaderState == SingleReaderState.REQUESTED_DATA)
      {
        //We must set this before we provide the data.
        _currentSinglePartMIMEReader._singleReaderState = SingleReaderState.CALLBACK_BOUND_AND_READY;

        //This effectively does:
        //_currentSinglePartMIMEReader._callback.onPartDataAvailable(clientData);
        final Callable<Void> onPartDataAvailableInvocation =
            new OnPartDataCallable(_currentSinglePartMIMEReader._callback, clientData);

        //Queue up this operation
        _callbackQueue.add(onPartDataAvailableInvocation);

        //If the while loop before us is in progress, we just return
        if (_callbackInProgress)
        {
          //At this point since callbackInProgress is true, we know that we have a client callback invocation in our
          //call stack. The while loop (see processAndInvokeCallableQueue) that is now executing the callback in our
          //call stack will also execute an invocation to the newly added callback.
          return;
        }
        else
        {
          processAndInvokeCallableQueue();
          //If invoking the callables resulting in things stopping, we will return anyway.
        }
      }
      else
      {
        //drop the bytes
        final Callable<Void> recursiveCallable = new RecursiveCallable(this);

        //Queue up this operation
        _callbackQueue.add(recursiveCallable);

        //If the while loop before us is in progress, we just return
        if (_callbackInProgress)
        {
          //At this point since callbackInProgress is true, we know that we have a client callback invocation in our
          //call stack. The while loop (see processAndInvokeCallableQueue) that is now executing the callback in our
          //call stack will also execute an invocation to the newly added callback.
          return;
        }
        else
        {
          processAndInvokeCallableQueue();
          //If invoking the callables resulting in things stopping, we will return anyway.
        }
        //No need to return explicitly from here.
      }
      //This part is finished. Subsequently when the client asks for more data our logic
      //will now see that the buffer begins with the boundary. This will finish up this part
      //and then make a new part.
    }

    //This will decompose our _compoundByteStringBuffer and obtain the client data needed to satisfy the amount
    //of data requested. Subsequently this will update the _compoundByteStringBuffer to drop any references to
    //data that we gave to our Client.
    private ByteString decomposeClientDataAndUpdateBuffer(final int maxAmountAvailableForClient)
    {
      //Note that we have to provide ByteStrings that are NOT compound internally. This is because clients who consume
      //these will invariably call APIs such as asByteBuffer() which, if compound, will incur a copy in memory
      //due to re-assembly.
      final List<ByteString> decomposedByteStrings = _compoundByteStringBuffer.decompose();

      //Find the first non empty ByteString
      ByteString firstNonEmptyByteString = null;
      for (int i = 0; i < decomposedByteStrings.size(); i++)
      {
        if (decomposedByteStrings.get(i).length() > 0)
        {
          firstNonEmptyByteString = decomposedByteStrings.get(i);
          break;
        }
      }

      //This is guaranteed to be non-null since we verified earlier that we have sufficient buffer size.
      assert(firstNonEmptyByteString != null);

      //We can take as much as maxAmountAvailableForClient. Therefore we have one of three options:
      //1. If firstNonEmptyByteString is less then maxAmountAvailableForClient, then we can give this
      //entire ByteString to the client. We then update our _compoundByteStringBuffer by creating a slice of it
      //that starts with an offset based off of the size of firstNonEmptyByteString.
      //2. If firstNonEmptyByteString is greater then maxAmountAvailableForClient, then we have to take a slice
      //of firstNonEmptyByteString from 0 to maxAmountAvailableForClient. We provide this slice to our client.
      //We then update our _compoundByteStringBuffer by creating a slice of it that starts with an offset based
      //on the size of the ByteString we gave to our client.
      //3. If firstNonEmptyByteString is equal to maxAmountAvailableForClient, then either of the above are equivalent.
      //Meaning that firstNonEmptyByteString (coincidentally) is exactly the maximum we can give to our client.

      final ByteString clientData;

      //We lump less then or equal to into the same case.
      if (firstNonEmptyByteString.length() <= maxAmountAvailableForClient)
      {
        clientData = firstNonEmptyByteString;
      }
      else
      {
        clientData = firstNonEmptyByteString.slice(0, maxAmountAvailableForClient);
      }

      //Update our buffer by trimming off references to what we don't need anymore.
      _compoundByteStringBuffer = _compoundByteStringBuffer.slice(clientData.length(),
                                                                  _compoundByteStringBuffer.length() - clientData.length());

      return clientData;
    }

    private void processBufferStartingWithBoundary(final int boundarySize)
    {
      //The beginning of the buffer contains a boundary. Finish of the current part first.
      //If performing this results in an exception then we can't continue so we return.
      if (finishCurrentPart())
      {
        return;
      }

      //Before continuing verify that this isn't the final boundary in front of us.
      if (_compoundByteStringBuffer.startsWith(_finishingBoundaryBytes))
      {
        _multiPartReaderState = MultiPartReaderState.READING_EPILOGUE;
        //If r2 has already notified we are done, we can wrap up. Note that there still may be bytes
        //sitting in our byteBuffer that haven't been consumed. These bytes must be the epilogue
        //bytes so we can safely ignore them.
        if (_r2Done)
        {
          _multiPartReaderState = MultiPartReaderState.FINISHED;
          //There is no need to use our iterative technique to call this callback because a
          //client cannot possibly invoke us again.
          try
          {
            //This can throw so we need to notify the client that their APIs threw an exception when we invoked them.
            MultiPartMIMEReader.this._clientCallback.onFinished();
          }
          catch (RuntimeException clientCallbackException)
          {
            handleExceptions(clientCallbackException);
          }

          return; //Regardless of whether or not the onFinished() threw, we're done so we must return here.

        }
        //Keep on reading bytes and dropping them.
        _rh.request(1);
        return;
      }

      processNewPart(boundarySize);
    }

    private boolean finishCurrentPart()
    {
      //Close the current single part reader (except if this is the first boundary)
      if (_currentSinglePartMIMEReader != null)
      {
        if (_currentSinglePartMIMEReader._singleReaderState == SingleReaderState.REQUESTED_ABANDON)
        {
          //If they cared to be notified of the abandonment.
          if (_currentSinglePartMIMEReader._callback != null)
          {
            //We need to prevent the client from asking for more data because they are done.
            _currentSinglePartMIMEReader._singleReaderState = SingleReaderState.FINISHED;

            //Note we do not need to use our iterative invocation technique here because
            //the client can't request more data.
            //Also it is important to note that we CANNOT use the iterative technique.
            //This is because the iterative technique will NOT return back here (to this line of
            //code) which does NOT guarantee us proceeding forward from here.
            //We need to proceed forward from here to move onto the next part.
            try
            {
              _currentSinglePartMIMEReader._callback.onAbandoned();
            }
            catch (RuntimeException clientCallbackException)
            {
              //This could throw so handle appropriately.
              handleExceptions(clientCallbackException);
              return true; //We return since we are in an unusable state.
            }
          } //else no notification will happen since there was no callback registered.
        }
        else
        {
          //This was a part that cared about its data. Let's finish him up.

          //We need to prevent the client from asking for more data because they are done.
          _currentSinglePartMIMEReader._singleReaderState = SingleReaderState.FINISHED;

          //Note we do not need to use our iterative invocation technique here because
          //the client can't request more data.
          //Also it is important to note that we CANNOT use the iterative technique.
          //This is because the iterative technique will NOT return back here (to this line of
          //code) which does NOT guarantee us proceeding forward from here.
          //We need to proceed forward from here to move onto the next part.
          try
          {
            _currentSinglePartMIMEReader._callback.onFinished();
          }
          catch (RuntimeException clientCallbackException)
          {
            //This could throw so handle appropriately.
            handleExceptions(clientCallbackException);
            return true; //We return since we are in an unusable state.
          }
        }
        _currentSinglePartMIMEReader = null;
      }

      return false;
    }

    private void processNewPart(final int boundarySize)
    {
      //Now read until we have all the headers. Headers may or may not exist. According to the RFC:
      //If the headers do not exist, we will see two CRLFs one after another.
      //If at least one header does exist, we will see the headers followed by two CRLFs
      //Essentially we are looking for the first occurrence of two CRLFs after we see the boundary.

      //We need to make sure we can look ahead a bit here first. The minimum size of the buffer must be
      //the size of the normal boundary plus consecutive CRLFs. This would be the bare minimum as it conveys
      //empty headers.
      if ((boundarySize + MultiPartMIMEUtils.CONSECUTIVE_CRLFS_BYTES.length) > _compoundByteStringBuffer.length())
      {
        if (_r2Done)
        {
          //If r2 has already notified we are done, then this is a problem. This means that
          //we have the remainder of the stream in memory and we see a non-finishing boundary that terminates
          //immediately without a CRLF_BYTES. This is a sign of a stream that was prematurely terminated.
          //MultiPartMIMEReader.this._clientCallback.onStreamError();
          handleExceptions(new MultiPartIllegalFormatException("Malformed multipart mime request. Premature"
              + " termination of multipart mime body due to a boundary without a subsequent consecutive CRLF."));
          return; //Unusable state, so return.
        }
        _rh.request(1);
        return;
      }

      //Now we will determine the existence of headers.
      //In order to do this we construct a window to look into. We will look inside of the buffer starting at the
      //end of the boundary until the end of the buffer.
      final ByteString possibleHeaderArea = _compoundByteStringBuffer.slice(boundarySize, _compoundByteStringBuffer.length() - boundarySize);

      //Find the two consecutive CRLFs.
      final int headerEnding = possibleHeaderArea.indexOfBytes(MultiPartMIMEUtils.CONSECUTIVE_CRLFS_BYTES);

      if (headerEnding == -1)
      {
        if (_r2Done)
        {
          //If r2 has already notified us we are done, then this is a problem. This means that we saw a
          //a boundary followed by a potential header area. This header area does not contain
          //two consecutive CRLF_BYTES characters. This is a malformed stream.
          handleExceptions(new MultiPartIllegalFormatException("Malformed multipart mime request. Premature " +
              "termination of headers within a part."));
          return;//Unusable state, so return.
        }
        //We need more data since the current buffer doesn't contain the CRLFs.
        _rh.request(1);
        return;
      }

      //At this point, headerEnding represents the location of the first occurrence of consecutive CRLFs.
      //It is important to note that it is possible for a malformed stream to not end its headers with consecutive
      //CRLFs. In such a case, everything up until the first occurrence of the consecutive CRLFs will be considered
      //part of the header area.

      //Let's make a window into the header area. Note that we need to include the trailing consecutive CRLF bytes
      //because we need to verify if the header area is empty, meaning it contains only consecutive CRLF bytes.
      final ByteString headerBytesSlice = possibleHeaderArea.slice(0, headerEnding + MultiPartMIMEUtils.CONSECUTIVE_CRLFS_BYTES.length);

      //Parse the headers
      final Map<String, String> headers = parseHeaders(headerBytesSlice);

      if (headers == null)
      {
        return; //Exception occurred when parsing headers. We are in an unusable state.
      }

      //At this point we have actual part data starting from headerEnding going forward
      //which means we can dump everything else beforehand. We need to skip past the trailing consecutive CRLFs.
      final int consumedDataIndex = boundarySize + headerEnding + MultiPartMIMEUtils.CONSECUTIVE_CRLFS_BYTES.length;
      //Update our buffer by trimming off references to what we don't need anymore.
      _compoundByteStringBuffer = _compoundByteStringBuffer.slice(consumedDataIndex, _compoundByteStringBuffer.length() - consumedDataIndex);

      //Notify the callback that we have a new part
      _currentSinglePartMIMEReader = new SinglePartMIMEReader(headers);

      //_clientCallback.onNewPart(_currentSinglePartMIMEReader);
      final Callable<Void> onNewPartInvocation =
          new OnNewPartCallable(_clientCallback, _currentSinglePartMIMEReader);

      //Queue up this operation
      _callbackQueue.add(onNewPartInvocation);

      //We can now switch to absorbing the normal boundary
      _firstBoundaryEvaluated = true;

      //If the while loop before us is in progress, we just return
      if (_callbackInProgress)
      {
        //At this point since callbackInProgress is true, we know that we have a client callback invocation in our
        //call stack. The while loop (see processAndInvokeCallableQueue) that is now executing the callback in our
        //call stack will also execute an invocation to the newly added callback.
        return;
      }
      else
      {
        processAndInvokeCallableQueue();
        //No need to explicitly return here even if this invocation results in an exception.
      }
    }

    //This will return null for the headers if there is an exception when parsing.
    private Map<String, String> parseHeaders(final ByteString headerBytes)
    {
      final Map<String, String> headers;
      if (headerBytes.equals(MultiPartMIMEUtils.BYTE_STRING_CONSECUTIVE_CRLFS_BYTES))
      {
        //The region of bytes after the boundary is composed of two CRLFs. Therefore we have no headers.
        headers = Collections.emptyMap();
      }
      else
      {
        headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

        //We have headers, lets read them in - we search using a sliding window.

        //Currently our buffer is sitting just past the end of the boundary. Beyond this boundary
        //there should be a CRLF followed by the first header. We will verify that this is indeed a CRLF
        //and then we will skip it below.

        final ByteString leadingBytes = headerBytes.slice(0, MultiPartMIMEUtils.CRLF_BYTES.length);
        if (!leadingBytes.equals(MultiPartMIMEUtils.BYTE_STRING_CRLF_BYTES))
        {
          handleExceptions(new MultiPartIllegalFormatException("Malformed multipart mime request. Headers are improperly constructed."));
          return null; //Unusable state, so return.
        }

        //The sliding-window-header-split technique here works because we are essentially splitting the buffer
        //by looking at occurrences of CRLF bytes. This is analogous to splitting a String in Java but instead
        //we are splitting a byte array.

        //We start at an offset of i and currentHeaderStart because we need to skip the first CRLF.
        int currentHeaderStart = MultiPartMIMEUtils.CRLF_BYTES.length;
        final StringBuilder runningFoldedHeader = new StringBuilder(); //For folded headers. See below for details.

        //Note that the end of the buffer we are sliding through is composed of two consecutive CRLFs.
        //Our sliding window algorithm here will NOT evaluate the very last CRLF bytes (which would otherwise
        //erroneously result in an empty header).
        for (int i = MultiPartMIMEUtils.CRLF_BYTES.length; i < headerBytes.length() - MultiPartMIMEUtils.CRLF_BYTES.length; i++)
        {
          final ByteString currentWindow = headerBytes.slice(i, MultiPartMIMEUtils.CRLF_BYTES.length);
          if (currentWindow.equals(MultiPartMIMEUtils.BYTE_STRING_CRLF_BYTES))
          {
            final ByteString currentHeader = headerBytes.slice(currentHeaderStart, i - currentHeaderStart);

            //At this point we MAY have found the end of a header because the current window is a CRLF.
            //This could POTENTIALLY mean that from currentHeaderStart until i is a header.

            //However before we can reach this conclusion we must check for header folding. Header folding is described
            //in RFC 822 which states that headers may take up multiple lines (therefore delimited by CRLFs).
            //This rule only holds true if there is exactly one CRLF followed by atleast one LWSP (linear white space).
            //A LWSP can be composed of multiple spaces, tabs or newlines. However most implementations only use
            //spaces or tabs. Therefore our reading of folded headers will support only CRLFs followed by atleast one
            //space or tab.
            //Furthermore this syntax is deprecated so there is no need for us to formally support the RFC here as
            //long as we cover interoperability with major libraries.

            //Therefore we have two options here:
            //1. If the character in front of us IS a tab or a white space, we must consider this the first part of a
            //multi line header value. In such a case we have to keep going forward and append the current header value.
            //2. Otherwise the character in front of us is NOT a tab or a white space. We can then consider the current
            //header bytes to compose a header that fits on a single line.

            String header = currentHeader.asString(Charset.defaultCharset());

            if (headerBytes.getByte(i + MultiPartMIMEUtils.CRLF_BYTES.length) == MultiPartMIMEUtils.SPACE_BYTE
                || headerBytes.getByte(i + MultiPartMIMEUtils.CRLF_BYTES.length) == MultiPartMIMEUtils.TAB_BYTE)
            {
              //Append the running concatenation of the folded header. We need to preserve the original header so
              //we also include the CRLF. The subsequent LWSP(s) will be also preserved because we don't trim here.
              runningFoldedHeader.append(header + MultiPartMIMEUtils.CRLF_STRING);
            }
            else
            {
              //This is a single line header OR we arrived at the last line of a folded header.
              if (runningFoldedHeader.length() != 0)
              {
                runningFoldedHeader.append(header);
                header = runningFoldedHeader.toString();
                runningFoldedHeader.setLength(0); //Clear the buffer for future folded headers in this part
              }

              //Note that according to the RFC that header values may contain semi colons but header names may not.
              //Therefore it is acceptable to split on semicolon here and derive the header name from 0 -> semicolonIndex.
              final int colonIndex = header.indexOf(":");
              if (colonIndex == -1)
              {
                handleExceptions(new MultiPartIllegalFormatException(
                    "Malformed multipart mime request. Individual headers are improperly formatted."));
                return null; //Unusable state, so return.
              }
              headers.put(header.substring(0, colonIndex).trim(),
                          header.substring(colonIndex + 1, header.length()).trim());
            }
            currentHeaderStart = i + MultiPartMIMEUtils.CRLF_BYTES.length;
          }
        }
      }

      return headers;
    }

    void handleExceptions(final Throwable throwable)
    {
      //All exceptions caught here should put the reader in a non-usable state. Continuing from this point forward
      //is not feasible.
      //We also will cancel here and have R2 read and drop all bytes on the floor. Otherwise we are obliged to read
      //and drop all bytes on the floor. It does not make any sense to enter this obligation when we are in
      //a non-usable state.
      //Exceptions here are indicative that there was malformed data provided to the MultiPartMIMEReader
      //OR that the client APIs threw exceptions themselves when their callbacks were invoked.
      //We will also invoke the appropriate callbacks here indicating there is an exception while reading.
      //It is the responsibility of the consumer of this library to catch these exceptions and return 4xx.

      _rh.cancel();

      //Call the single part callback first.
      if (_currentSinglePartMIMEReader != null)
      {
        _currentSinglePartMIMEReader._singleReaderState = SingleReaderState.FINISHED;

        try
        {
          _currentSinglePartMIMEReader._callback.onStreamError(throwable);
        }
        catch (RuntimeException runtimeException)
        {
          //Swallow. What more can we do here?
        }
      }

      _multiPartReaderState = MultiPartReaderState.FINISHED;
      try
      {
        _clientCallback.onStreamError(throwable);
      }
      catch (RuntimeException runtimeException)
      {
        //Swallow. What more can we do here?
      }
    }

    private R2MultiPartMIMEReader(final String boundary)
    {
      _firstBoundary = "--" + boundary;
      _normalBoundary = MultiPartMIMEUtils.CRLF_STRING + "--" + boundary;
      _finishingBoundary = _normalBoundary + "--";

      _firstBoundaryBytes = _firstBoundary.getBytes();
      _normalBoundaryBytes = _normalBoundary.getBytes();
      _finishingBoundaryBytes = _finishingBoundary.getBytes();
    }
  }

  //Package private for testing
  enum MultiPartReaderState
  {
    CREATED, //At the very beginning. Before the callback is even bound.
    CALLBACK_BOUND_AND_READING_PREAMBLE, //Callback is bound and we have started to read the preamble in.
    READING_PARTS, //Normal operation. Most time should be spent in this state.
    READING_EPILOGUE, //Epilogue is being read.
    ABANDONING, //Client asked for an complete abandonment.
    FINISHED //The reader is no longer usable.
  }

  /**
   * Create a MultiPartMIMEReader by acquiring the {@link com.linkedin.r2.message.stream.entitystream.EntityStream} from the
   * provided {@link com.linkedin.r2.message.stream.StreamRequest}.
   *
   * Interacting with the MultiPartMIMEReader will happen through callbacks invoked on the provided
   * {@link com.linkedin.multipart.MultiPartMIMEReaderCallback}.
   *
   * @param request the request containing the {@link com.linkedin.r2.message.stream.entitystream.EntityStream} to read from.
   * @param clientCallback the callback to invoke in order to drive the client forward for reading part data.
   * @return the newly created MultiPartMIMEReader
   * @throws com.linkedin.multipart.exceptions.MultiPartIllegalFormatException if the request is in any way not a valid multipart/mime request.
   */
  public static MultiPartMIMEReader createAndAcquireStream(final StreamRequest request,
      final MultiPartMIMEReaderCallback clientCallback) throws MultiPartIllegalFormatException
  {
    return new MultiPartMIMEReader(request, clientCallback);
  }

  /**
   * Create a MultiPartMIMEReader by acquiring the {@link com.linkedin.r2.message.stream.entitystream.EntityStream} from the
   * provided {@link com.linkedin.r2.message.stream.StreamResponse}.
   *
   * Interacting with the MultiPartMIMEReader will happen through callbacks invoked on the provided
   * {@link com.linkedin.multipart.MultiPartMIMEReaderCallback}.
   *
   * @param response the response containing the {@link com.linkedin.r2.message.stream.entitystream.EntityStream} to read from.
   * @param clientCallback the callback to invoke in order to drive the client forward for reading part data.
   * @return the newly created MultiPartMIMEReader
   * @throws com.linkedin.multipart.exceptions.MultiPartIllegalFormatException if the response is in any way not a valid multipart/mime response.
   */
  public static MultiPartMIMEReader createAndAcquireStream(final StreamResponse response,
      final MultiPartMIMEReaderCallback clientCallback) throws MultiPartIllegalFormatException
  {
    return new MultiPartMIMEReader(response, clientCallback);
  }

  /**
   * Create a MultiPartMIMEReader by acquiring the {@link com.linkedin.r2.message.stream.entitystream.EntityStream} from the
   * provided {@link com.linkedin.r2.message.stream.StreamRequest}.
   *
   * Interacting with the MultiPartMIMEReader will require subsequent registration using a
   * {@link com.linkedin.multipart.MultiPartMIMEReaderCallback}.
   *
   * @param request the request containing the {@link com.linkedin.r2.message.stream.entitystream.EntityStream} to read from.
   * @return the newly created MultiPartMIMEReader
   * @throws com.linkedin.multipart.exceptions.MultiPartIllegalFormatException if the request is in any way not a valid multipart/mime request.
   */
  public static MultiPartMIMEReader createAndAcquireStream(final StreamRequest request)
      throws MultiPartIllegalFormatException
  {
    return new MultiPartMIMEReader(request, null);
  }

  /**
   * Create a MultiPartMIMEReader by acquiring the {@link com.linkedin.r2.message.stream.entitystream.EntityStream} from the
   * provided {@link com.linkedin.r2.message.stream.StreamResponse}.
   *
   * Interacting with the MultiPartMIMEReader will require subsequent registration using a
   * {@link com.linkedin.multipart.MultiPartMIMEReaderCallback}.
   *
   * @param response the response containing the {@link com.linkedin.r2.message.stream.entitystream.EntityStream} to read from.
   * @return the newly created MultiPartMIMEReader
   * @throws com.linkedin.multipart.exceptions.MultiPartIllegalFormatException if the response is in any way not a valid multipart/mime response.
   */
  public static MultiPartMIMEReader createAndAcquireStream(final StreamResponse response)
      throws MultiPartIllegalFormatException
  {
    return new MultiPartMIMEReader(response, null);
  }

  private MultiPartMIMEReader(final StreamRequest request, final MultiPartMIMEReaderCallback clientCallback)
      throws MultiPartIllegalFormatException
  {
    final String contentTypeHeaderValue = request.getHeader(MultiPartMIMEUtils.CONTENT_TYPE_HEADER);
    if (contentTypeHeaderValue == null)
    {
      throw new MultiPartIllegalFormatException(
          "Malformed multipart mime request. No Content-Type header in this request");
    }

    _reader = new R2MultiPartMIMEReader(MultiPartMIMEUtils.extractBoundary(contentTypeHeaderValue));
    _entityStream = request.getEntityStream();
    _multiPartReaderState = MultiPartReaderState.CREATED;
    if (clientCallback != null)
    {
      _clientCallback = clientCallback;
      _entityStream.setReader(_reader);
    }
  }

  private MultiPartMIMEReader(StreamResponse response, MultiPartMIMEReaderCallback clientCallback)
      throws MultiPartIllegalFormatException
  {
    final String contentTypeHeaderValue = response.getHeader(MultiPartMIMEUtils.CONTENT_TYPE_HEADER);
    if (contentTypeHeaderValue == null)
    {
      throw new MultiPartIllegalFormatException(
          "Malformed multipart mime request. No Content-Type header in this response");
    }

    _reader = new R2MultiPartMIMEReader(MultiPartMIMEUtils.extractBoundary(contentTypeHeaderValue));
    _entityStream = response.getEntityStream();
    _multiPartReaderState = MultiPartReaderState.CREATED;
    if (clientCallback != null)
    {
      _clientCallback = clientCallback;
      _entityStream.setReader(_reader);
    }
  }

  /**
   * Indicates if all parts have been finished and completely read from this MultiPartMIMEReader. If the last part is
   * in the process of being read, this will return false.
   *
   * @return true if the reader is completely finished.
   */
  public boolean haveAllPartsFinished()
  {
    return _multiPartReaderState == MultiPartReaderState.FINISHED;
  }

  /**
   * Reads through and abandons the current new part (if applicable) and additionally the whole stream.
   *
   * This API can be used in only the following scenarios:
   *
   * 1. Without registering a {@link com.linkedin.multipart.MultiPartMIMEReaderCallback}. Abandonment will begin
   * and since no callback is registered, there will be no notification when it is completed.
   *
   * 2. After registration using a {@link com.linkedin.multipart.MultiPartMIMEReaderCallback}
   * and after an invocation on {@link MultiPartMIMEReaderCallback#onNewPart(com.linkedin.multipart.MultiPartMIMEReader.SinglePartMIMEReader)}.
   * Abandonment will begin and when it is complete, a call will be made to {@link MultiPartMIMEReaderCallback#onAbandoned()}.
   *
   * If this is called after registration and before an invocation on
   * {@link MultiPartMIMEReaderCallback#onNewPart(com.linkedin.multipart.MultiPartMIMEReader.SinglePartMIMEReader)},
   * then a {@link com.linkedin.multipart.exceptions.StreamBusyException} will be thrown.
   *
   * If this used after registration, then this can ONLY be called if there is no part being actively read, meaning that
   * the current {@link com.linkedin.multipart.MultiPartMIMEReader.SinglePartMIMEReader} has not been initialized
   * with a {@link com.linkedin.multipart.SinglePartMIMEReaderCallback}. If this is violated a
   * {@link com.linkedin.multipart.exceptions.StreamBusyException} will be thrown.
   *
   * If the stream is finished, subsequent calls will throw {@link com.linkedin.multipart.exceptions.MultiPartReaderFinishedException}.
   *
   * Since this is async and request queueing is not allowed, repetitive calls will result in
   * {@link com.linkedin.multipart.exceptions.StreamBusyException}.
   */
  public void abandonAllParts()
  {
    //We are already done or almost done.
    if (_multiPartReaderState == MultiPartReaderState.FINISHED || _multiPartReaderState == MultiPartReaderState.READING_EPILOGUE)
    {
      throw new MultiPartReaderFinishedException("The reader is finished therefore it cannot proceed.");
    }

    if (_multiPartReaderState == MultiPartReaderState.CALLBACK_BOUND_AND_READING_PREAMBLE)
    {
      throw new StreamBusyException("The reader is busy processing the preamble. Unable to proceed with abandonment. "
          + "Please only call abandonAllParts() upon invocation of onNewPart() on the client callback.");
    }

    if (_multiPartReaderState == MultiPartReaderState.ABANDONING)
    {
      throw new StreamBusyException("Reader already busy abandoning.");
    }

    //At this point we know we are in CREATED or READING_PARTS which is the desired state.
    if (_multiPartReaderState == MultiPartReaderState.CREATED)
    {
      //There was a request to abandon without a top level callback. We have to eventually call _rh.cancel().
      //Therefore we set the state to finished and set the reader on the entityStream. When our reader is invoked onInit(),
      //the cancel will take place.
      _multiPartReaderState = MultiPartReaderState.FINISHED;
      _entityStream.setReader(_reader);
      return;
    }
    else
    {
      assert(_multiPartReaderState == MultiPartReaderState.READING_PARTS);
      //We are in READING_PARTS. At this point we require that there exist a valid, non-null SinglePartMIMEReader before
      //we continue since the contract is that the top level callback can only abandon upon witnessing onNewPart().

      //Note that there is a small window of opportunity where a client registers the callback and invokes
      //abandonAllParts() after the reader has read the preamble in but before the reader has invoked onNewPart().
      //At this point, _currentSinglePartMIMEReader may potentially be null.
      //This can happen, but so can a client invoking us concurrently which is forbidden. Therefore we will not check
      //for such a race.

      //As stated earlier, we know for a fact that onNewPart() has been invoked on the reader callback. Just make sure its
      //at the beginning of a new part before we continue allowing the abandonment.
      if (_currentSinglePartMIMEReader._singleReaderState != SingleReaderState.CREATED)
      {
        throw new StreamBusyException("Unable to abandon all parts due to current SinglePartMIMEReader in use.");
      }

      _currentSinglePartMIMEReader._singleReaderState = SingleReaderState.FINISHED;
      _multiPartReaderState = MultiPartReaderState.ABANDONING;

      _reader.processEventAndInvokeClient();
    }
  }

  /**
   * Register to read using this MultiPartMIMEReader. Upon registration, at some point in the future, an invocation will be
   * made on {@link MultiPartMIMEReaderCallback#onNewPart(com.linkedin.multipart.MultiPartMIMEReader.SinglePartMIMEReader)}.
   *
   * This can ONLY be called if there is no part being actively read; meaning that the current
   * {@link com.linkedin.multipart.MultiPartMIMEReader.SinglePartMIMEReader} has not had a callback registered with it.
   * Violation of this will throw a {@link com.linkedin.multipart.exceptions.StreamBusyException}.
   *
   * This can even be set if no parts in the stream have actually been consumed, i.e after the very first invocation of
   * {@link MultiPartMIMEReaderCallback#onNewPart(com.linkedin.multipart.MultiPartMIMEReader.SinglePartMIMEReader)}.
   *
   * If this MultiPartMIMEReader is finished, then attempts to register a callback will throw
   * {@link com.linkedin.multipart.exceptions.MultiPartReaderFinishedException}.
   *
   * @param clientCallback the {@link com.linkedin.multipart.MultiPartMIMEReaderCallback} which will be invoked upon
   *                       to read this multipart mime body.
   */
  public void registerReaderCallback(final MultiPartMIMEReaderCallback clientCallback)
  {
    //First we throw exceptions for all _reader states where it is incorrect to transfer callbacks.
    //We have to handle all the individual incorrect states one by one so that we can that we can throw
    //fine grain exceptions.

    if (_multiPartReaderState == MultiPartReaderState.FINISHED || _multiPartReaderState == MultiPartReaderState.READING_EPILOGUE)
    {
      throw new MultiPartReaderFinishedException("Unable to register a callback. This reader has already finished reading.");
    }

    if (_multiPartReaderState == MultiPartReaderState.CALLBACK_BOUND_AND_READING_PREAMBLE)
    {
      //This would happen if a client registers a callback multiple times
      //immediately.
      throw new StreamBusyException(
          "Reader is busy reading in the preamble. Unable to register the callback at this time.");
    }

    if (_multiPartReaderState == MultiPartReaderState.ABANDONING)
    {
      throw new StreamBusyException("Reader is busy performing a complete abandonment. Unable to register the callback.");
    }

    //At this point we know that _reader is in CREATED or READING_PARTS

    //Now we verify that single part reader is in the correct state.
    //The first time the callback is registered, _currentSinglePartMIMEReader will be null which is fine.
    //Subsequent calls will verify that the _currentSinglePartMIMEReader is in the desired state.
    if (_currentSinglePartMIMEReader != null && _currentSinglePartMIMEReader._singleReaderState != SingleReaderState.CREATED)
    {
      throw new StreamBusyException(
          "Unable to register callback on the reader since there is currently a SinglePartMIMEReader in use, meaning "
              + "that it was registered with a SinglePartMIMEReaderCallback."); //Can't transition at this point in time
    }

    if (_clientCallback == null)
    {
      //This is the first time it's being set
      _multiPartReaderState = MultiPartReaderState.CALLBACK_BOUND_AND_READING_PREAMBLE;
      _clientCallback = clientCallback;
      _entityStream.setReader(_reader);
      return;
    }
    else
    {
      //This is just a transfer to a new callback.
      _clientCallback = clientCallback;
    }

    //Start off the new client callback. If there was already a _currentSinglePartMIMEReader, we get them started off
    //on that. Otherwise if this is the first time then we can just return from here and let the fact we setReader()
    //above drive the first invocation of onNewPart().
    //Note that it is not possible for _currentSinglePartMIMEReader to be null at ANY time except the very first time
    //registerReaderCallback() is invoked.

    if (_currentSinglePartMIMEReader != null)
    {
      //Also note that if the client is really abusive it is possible for them
      //to call registerReaderCallback() over and over again which would lead to a stack overflow.
      //However this is clearly a client bug so we will not account for it here.

      try
      {
        _clientCallback.onNewPart(_currentSinglePartMIMEReader);
      }
      catch (RuntimeException exception)
      {
        //The callback could throw here, at which point we let them know what they just did and shut things down.
        _reader.handleExceptions(exception);
      }
    }
  }

  R2MultiPartMIMEReader getR2MultiPartMIMEReader()
  {
    return _reader;
  }

  /* Package private and used for testing */
  void setState(final MultiPartReaderState multiPartReaderState)
  {
    _multiPartReaderState = multiPartReaderState;
  }

  /* Package private and used for testing */
  void setCurrentSinglePartMIMEReader(final SinglePartMIMEReader singlePartMIMEReader)
  {
    _currentSinglePartMIMEReader = singlePartMIMEReader;
  }

  //MultiPartMIMEReader callable wrappers:
  private final static class OnNewPartCallable implements Callable<Void>
  {
    private final MultiPartMIMEReaderCallback _multiPartMIMEReaderCallback;
    private final MultiPartMIMEReader.SinglePartMIMEReader _singlePartMIMEReader;

    @Override
    public Void call() throws Exception
    {
      _multiPartMIMEReaderCallback.onNewPart(_singlePartMIMEReader);
      return null; //This is ignored
    }

    OnNewPartCallable(final MultiPartMIMEReaderCallback multiPartMIMEReaderCallback,
                      final MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader)
    {
      _multiPartMIMEReaderCallback = multiPartMIMEReaderCallback;
      _singlePartMIMEReader = singlePartMIMEReader;
    }
  }

  private final static class RecursiveCallable implements Callable<Void>
  {
    private final MultiPartMIMEReader.R2MultiPartMIMEReader _r2MultiPartMIMEReader;

    @Override
    public Void call() throws Exception
    {
      _r2MultiPartMIMEReader.onDataAvailable(ByteString.empty());
      return null; //This is ignored
    }

    RecursiveCallable(final MultiPartMIMEReader.R2MultiPartMIMEReader r2MultiPartMIMEReader)
    {
      _r2MultiPartMIMEReader = r2MultiPartMIMEReader;
    }
  }

  //SinglePartMIMEReaderCallback callable wrappers:
  private final static class OnPartDataCallable implements Callable<Void>
  {
    private final SinglePartMIMEReaderCallback _singlePartMIMEReaderCallback;
    private final ByteString _data;

    @Override
    public Void call() throws Exception
    {
      _singlePartMIMEReaderCallback.onPartDataAvailable(_data);
      return null; //This is ignored.
    }

    OnPartDataCallable(final SinglePartMIMEReaderCallback singlePartMIMEReaderCallback, final ByteString data)
    {
      _singlePartMIMEReaderCallback = singlePartMIMEReaderCallback;
      _data = data;
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////
  // Chaining interface implementation. These should not be used directly by external consumers.

  /**
   * Please do not use. This is for internal use only.
   *
   * Invoked when all the potential data sources that this MultiPartMIMEDataSourceIterator represents need to be aborted
   * since they will not be given a chance to produce data.
   */
  @Override
  public void abortAllDataSources()
  {
    abandonAllParts();
  }

  /**
   * Please do not use. This is for internal use only.
   *
   * Invoked as the first step to walk through all potential data sources represented by this MultiPartMIMEDataSourceIterator.
   *
   * @param callback the callback that will be invoked as data sources become available for consumption.
   */
  @Override
  public void registerDataSourceReaderCallback(final MultiPartMIMEDataSourceIteratorCallback callback)
  {
    registerReaderCallback(new MultiPartMIMEReaderCallback()
    {
      @Override
      public void onNewPart(SinglePartMIMEReader singlePartMIMEReader)
      {
        callback.onNewDataSource(singlePartMIMEReader);
      }

      @Override
      public void onFinished()
      {
        callback.onFinished();
      }

      @Override
      public void onAbandoned()
      {
        callback.onAbandoned();
      }

      @Override
      public void onStreamError(Throwable throwable)
      {
        callback.onStreamError(throwable);
      }
    });
  }

  /**
   * A reader to register with and walk through an individual multipart mime body.
   *
   * When a new SinglePartMIMEReader is available, clients will be invoked on
   * {@link MultiPartMIMEReaderCallback#onNewPart(com.linkedin.multipart.MultiPartMIMEReader.SinglePartMIMEReader)}.
   * At this time, clients should register an instance of {@link com.linkedin.multipart.SinglePartMIMEReaderCallback}
   * and then call {@link com.linkedin.multipart.MultiPartMIMEReader.SinglePartMIMEReader#requestPartData()}
   * to start the flow of data.
   */
  public class SinglePartMIMEReader implements MultiPartMIMEDataSourceWriter
  {
    private final Map<String, String> _headers;
    private volatile SinglePartMIMEReaderCallback _callback = null;
    private final R2MultiPartMIMEReader _r2MultiPartMIMEReader;
    private volatile SingleReaderState _singleReaderState = SingleReaderState.CREATED;

    /**
     * Only MultiPartMIMEReader can ever create one of these.
     */
    SinglePartMIMEReader(Map<String, String> headers)
    {
      _r2MultiPartMIMEReader = MultiPartMIMEReader.this._reader;
      _headers = Collections.unmodifiableMap(headers);
    }

    /**
     * This call registers a callback and commits to reading this part. This can only happen once per life of each
     * SinglePartMIMEReader. Subsequent attempts to modify this will throw
     * {@link com.linkedin.multipart.exceptions.SinglePartBindException}.
     *
     * @param callback the callback to be invoked on in order to read data
     */
    public void registerReaderCallback(SinglePartMIMEReaderCallback callback)
    {
      //We don't have to check each and every state here. In order to reach any of those states the _callback must
      //have been not null to begin with, so we just check for that.
      if (_singleReaderState != SingleReaderState.CREATED)
      {
        throw new SinglePartBindException("Callback already registered.");
      }
      _singleReaderState = SingleReaderState.CALLBACK_BOUND_AND_READY;
      _callback = callback;
    }

    /**
     * Reads bytes from this part and notifies the registered callback on
     * {@link SinglePartMIMEReaderCallback#onPartDataAvailable(com.linkedin.data.ByteString)}.
     *
     * Usage of this API requires registration using a {@link com.linkedin.multipart.SinglePartMIMEReaderCallback}.
     * Failure to do so will throw a {@link com.linkedin.multipart.exceptions.SinglePartNotInitializedException}.
     *
     * If this part is fully consumed, meaning {@link SinglePartMIMEReaderCallback#onFinished()} has been called,
     * then any subsequent calls to requestPartData() will throw {@link com.linkedin.multipart.exceptions.SinglePartFinishedException}.
     *
     * Since this is async and request queueing is not allowed, repetitive calls will result in
     * {@link com.linkedin.multipart.exceptions.StreamBusyException}.
     *
     * If the r2 reader is done, either through an error or a proper finish. Calls to requestPartData() will throw
     * {@link com.linkedin.multipart.exceptions.SinglePartFinishedException}.
     */
    public void requestPartData()
    {
      verifyUsableState();

      //Additionally, unlike abandonPartData(), requestPartData() can only be used if a callback is registered.
      if (_singleReaderState == SingleReaderState.CREATED)
      {
        throw new SinglePartNotInitializedException("This SinglePartMIMEReader has not had a callback registered with it yet.");
      }

      //We know we are now at SingleReaderState.CALLBACK_BOUND_AND_READY
      _singleReaderState = SingleReaderState.REQUESTED_DATA;

      //We have updated our desire to be notified of data. Now we signal the reader to refresh itself and forcing it
      //to read from the internal buffer as much as possible. We do this by notifying it of an empty ByteString.
      _r2MultiPartMIMEReader.processEventAndInvokeClient();
    }

    /**
     * Abandons all bytes from this part and then notifies the registered callback (if present) on
     * {@link SinglePartMIMEReaderCallback#onAbandoned()}.
     *
     * Usage of this API does NOT require registration using a {@link com.linkedin.multipart.SinglePartMIMEReaderCallback}.
     * If there is no callback registration then there is no notification provided upon completion of abandoning
     * this part.
     *
     * If this part is fully consumed, meaning {@link SinglePartMIMEReaderCallback#onFinished()} has been called,
     * then any subsequent calls to abandonPart() will throw {@link com.linkedin.multipart.exceptions.SinglePartFinishedException}.
     *
     * Since this is async and request queueing is not allowed, repetitive calls will result in
     * {@link com.linkedin.multipart.exceptions.StreamBusyException}.
     *
     * * If the r2 reader is done, either through an error or a proper finish. Calls to abandonPart() will throw
     * {@link com.linkedin.multipart.exceptions.SinglePartFinishedException}.
     */
    public void abandonPart()
    {
      verifyUsableState();

      //We know we are now at SingleReaderState.CALLBACK_BOUND_AND_READY
      _singleReaderState = SingleReaderState.REQUESTED_ABANDON;

      //We have updated our desire to be abandoned. Now we signal the reader to refresh itself and forcing it
      //to read from the internal buffer as much as possible. We do this by notifying it of an empty ByteString.
      _r2MultiPartMIMEReader.processEventAndInvokeClient();
    }

    //Package private for testing.
    void verifyUsableState()
    {
      if (_singleReaderState == SingleReaderState.FINISHED)
      {
        throw new SinglePartFinishedException("This SinglePartMIMEReader has already finished.");
      }

      if (_singleReaderState == SingleReaderState.REQUESTED_DATA)
      {
        throw new StreamBusyException(
            "This SinglePartMIMEReader is currently busy fulfilling a call to requestPartData().");
      }

      if (_singleReaderState == SingleReaderState.REQUESTED_ABANDON)
      {
        throw new StreamBusyException("This SinglePartMIMEReader is currently busy fulfilling a call to abandonPart().");
      }
    }

    /* Package private for testing */
    void setState(final SingleReaderState singleReaderState)
    {
      _singleReaderState = singleReaderState;
    }

    /**
     * Returns the headers for this part. For parts that have no headers, this will return
     * {@link java.util.Collections#emptyMap()}
     *
     * @return
     */
    @Override
    public Map<String, String> dataSourceHeaders()
    {
      return _headers;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    //Chaining interface implementation. These should not be used directly by external consumers.

    /**
     * Please do not use. This is for internal use only.
     */
    @Override
    public void onInit(WriteHandle writeHandle)
    {
      //We have been informed that this part will be treated as a data source by the MultiPartMIMEWriter.
      SinglePartMIMEReaderCallback singlePartMIMEChainReaderCallback = new SinglePartMIMEChainReaderCallback(writeHandle, this);
      registerReaderCallback(singlePartMIMEChainReaderCallback);
    }

    /**
     * Please do not use. This is for internal use only.
     */
    @Override
    public void onWritePossible()
    {
      //When we are told to produce some data we will requestPartData() on ourselves which will
      //result in onPartDataAvailable() in SinglePartMIMEReaderDataSourceCallback(). The result of that will write
      //to the writeHandle which will write it further down stream.
      requestPartData();
    }

    /**
     * Please do not use. This is for internal use only.
     */
    @Override
    public void onAbort(Throwable e)
    {
      //If this happens, this means that there was a call on
      //{@link com.linkedin.r2.message.stream.entitystream.CompositeWriter.onAbort} which caused it to walk through
      //each of its writers and call onAbort() on each one of them. This class happened to be one of those writers.
      //
      //In terms of the original call made to the CompositeWriter to abort, this could arise due to the following:
      //1. This can be invoked by R2 if it tells the composite writer to abort.
      //2. This can also be invoked if there is a functional need to abort all data sources.

      //Regardless of how it was called we need to completely drain and drop all bytes to the ground. We can't
      //leave these bytes in the SinglePartMIMEReader untouched.
      abandonPart();
    }
  }

  //Package private for testing
  enum SingleReaderState
  {
    CREATED, //Initial construction, no callback bound.
    CALLBACK_BOUND_AND_READY, //Callback has been bound, ready to use APIs.
    REQUESTED_DATA, //Requested data, waiting to be notified.
    REQUESTED_ABANDON, //Waiting for an abandon to finish.
    FINISHED //This reader is done.
  }
}