package com.linkedin.r2.message.stream.entitystream;

import com.linkedin.data.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
;

/**
 * A class consists exclusively of static methods to deal with EntityStream {@link com.linkedin.r2.message.stream.entitystream.EntityStream}
 *
 * @author Zhenkai Zhu
 */
public final class EntityStreams
{
  private static final Logger LOG = LoggerFactory.getLogger(EntityStreams.class);

  private EntityStreams() {}

  public static EntityStream emptyStream()
  {
    return newEntityStream(new Writer()
    {
      private WriteHandle _wh;

      @Override
      public void onInit(WriteHandle wh)
      {
        _wh = wh;
      }

      @Override
      public void onWritePossible()
      {
        _wh.done();
      }

      @Override
      public void onAbort(Throwable e)
      {
        // do nothing
      }
    });
  }

  /**
   * The method to create a new EntityStream with a writer for the stream
   *
   * @param writer the writer for the stream who would provide the data
   * @return an instance of EntityStream
   */
  public static EntityStream newEntityStream(Writer writer)
  {
    return new EntityStreamImpl(writer);
  }

  private enum State
  {
    UNINITIALIZED,
    ACTIVE,
    FINISHED,
    ABORTED,
    ABORT_REQUESTED,
  }

  private static class EntityStreamImpl implements EntityStream
  {
    private final Writer _writer;
    private final Object _lock;
    private List<Observer> _observers;
    private Reader _reader;

    private int _remaining;
    private boolean _notifyWritePossible;
    private State _state;

    EntityStreamImpl(Writer writer)
    {
      _writer = writer;
      _lock = new Object();
      _observers = new ArrayList<Observer>();
      _remaining = 0;
      _notifyWritePossible = true;
      _state = State.UNINITIALIZED;
    }

    public void addObserver(Observer o)
    {
      synchronized (_lock)
      {
        checkInit();
        _observers.add(o);
      }
    }

    public void setReader(Reader r)
    {
      synchronized (_lock)
      {
        checkInit();
        _state = State.ACTIVE;
        _reader = r;
        _observers = Collections.unmodifiableList(_observers);
      }

      final WriteHandle wh = new WriteHandleImpl();
      RuntimeException writerInitEx = null;
      try
      {
        _writer.onInit(wh);
      }
      catch (RuntimeException ex)
      {
        synchronized (_lock)
        {
          _state = State.ABORTED;
        }
        safeAbortWriter(ex);
        writerInitEx = ex;
      }

      final AtomicBoolean _notified = new AtomicBoolean(false);
      final ReadHandle rh;
      if (writerInitEx == null)
      {
        rh = new ReadHandleImpl();
      }
      else
      {
        final Throwable cause = writerInitEx;
        rh = new ReadHandle()
        {
          @Override
          public void request(int n)
          {
            notifyError();
          }

          @Override
          public void cancel()
          {
            notifyError();
          }

          void notifyError()
          {
            if (_notified.compareAndSet(false, true))
            {
              safeNotifyErrorToObservers(cause);
              safeNotifyErrorToReader(cause);
            }
          }
        };
      }

      try
      {
        _reader.onInit(rh);
      }
      catch (RuntimeException ex)
      {
        synchronized (_lock)
        {
          if (_state != State.ACTIVE && _state != State.ABORT_REQUESTED && writerInitEx == null)
          {
            return;
          }
          else
          {
            _state = State.ABORTED;
          }
        }
        if (writerInitEx == null)
        {
          doCancel(ex, true);
        }
        else
        {
          if (_notified.compareAndSet(false, true))
          {
            safeNotifyErrorToObservers(ex);
            safeNotifyErrorToReader(ex);
          }
        }
      }
    }

    private class WriteHandleImpl implements WriteHandle
    {
      @Override
      public void write(final ByteString data)
      {
        boolean doCancelNow = false;

        synchronized (_lock)
        {
          if (_state == State.FINISHED)
          {
            throw new IllegalStateException("Attempting to write after done or error of WriteHandle is invoked");
          }

          if (_state == State.ABORTED)
          {
            return;
          }

          _remaining--;

          if (_remaining < 0)
          {
            throw new IllegalStateException("Attempt to write when remaining is 0");
          }

          if (_state == State.ABORT_REQUESTED)
          {
            doCancelNow = true;
            _state = State.ABORTED;
          }
        }

        if (doCancelNow)
        {
          doCancel(getAbortedException(), false);
          return;
        }

        for (Observer observer : _observers)
        {
          try
          {
            observer.onDataAvailable(data);
          }
          catch (RuntimeException ex)
          {
            LOG.warn("Observer throws exception at onDataAvailable", ex);
          }
        }

        try
        {
          _reader.onDataAvailable(data);
        }
        catch (RuntimeException ex)
        {
          // the lock ensures that once we change the _state to ABORTED, it will stay as ABORTED
          synchronized (_lock)
          {
            _state = State.ABORTED;
          }

          // we can safely do cancel here because no other place could be doing cancel (mutually exclusively by design)
          doCancel(ex, true);
        }
      }

      @Override
      public void done()
      {
        boolean doCancelNow = false;
        synchronized (_lock)
        {
          if (_state != State.ACTIVE && _state != State.ABORT_REQUESTED)
          {
            return;
          }

          if (_state == State.ABORT_REQUESTED)
          {
            doCancelNow = true;
            _state = State.ABORTED;
          }
          else
          {
            _state = State.FINISHED;
          }
        }

        if (doCancelNow)
        {
          doCancel(getAbortedException(), false);
          return;
        }


        for (Observer observer : _observers)
        {
          try
          {
            observer.onDone();
          }
          catch (RuntimeException ex)
          {
            LOG.warn("Observer throws exception at onDone, ignored.", ex);
          }
        }

        try
        {
          _reader.onDone();
        }
        catch (RuntimeException ex)
        {
          LOG.warn("Reader throws exception at onDone; notifying writer", ex);
          // At this point, no cancel had happened and no cancel will happen, _writer.onAbort will not be invoked more than once
          // This is still a value to let writer know about this exception, e.g. see DispatcherRequestFilter.Connector
          safeAbortWriter(ex);
        }
      }

      @Override
      public void error(final Throwable e)
      {
        boolean doCancelNow = false;
        synchronized (_lock)
        {
          if (_state != State.ACTIVE && _state != State.ABORT_REQUESTED)
          {
            return;
          }

          if (_state == State.ABORT_REQUESTED)
          {
            doCancelNow = true;
            _state = State.ABORTED;
          }
          else
          {
            _state = State.FINISHED;
          }
        }

        if (doCancelNow)
        {
          doCancel(getAbortedException(), false);
          return;
        }

        safeNotifyErrorToObservers(e);

        try
        {
          _reader.onError(e);
        }
        catch (RuntimeException ex)
        {
          LOG.warn("Reader throws exception at onError; notifying writer", ex);
          // at this point, no cancel had happened and no cancel will happen, _writer.onAbort will not be invoked more than once
          // This is still a value to let writer know about this exception, e.g. see DispatcherRequestFilter.Connector
          safeAbortWriter(ex);
        }
      }

      @Override
      public int remaining()
      {
        int result;
        boolean doCancelNow = false;
        synchronized (_lock)
        {
          if (_state != State.ACTIVE && _state != State.ABORT_REQUESTED)
          {
            return 0;
          }

          if (_state == State.ABORT_REQUESTED)
          {
            doCancelNow = true;
            _state = State.ABORTED;
            result = 0;
          }
          else
          {
            if (_remaining == 0)
            {
              _notifyWritePossible = true;
            }
            result = _remaining;
          }
        }

        if (doCancelNow)
        {
          doCancel(getAbortedException(), false);
        }

        return result;
      }
    }

    private class ReadHandleImpl implements ReadHandle
    {
      @Override
      public void request(final int chunkNum)
      {
        if (chunkNum <= 0)
        {
          throw new IllegalArgumentException("cannot request non-positive number of data chunks: " + chunkNum);
        }

        boolean needNotify = false;
        synchronized (_lock)
        {
          if (_state != State.ACTIVE)
          {
            return;
          }

          _remaining += chunkNum;
          // overflow
          if (_remaining < 0)
          {
            LOG.warn("chunkNum overflow, setting to Integer.MAX_VALUE");
            _remaining = Integer.MAX_VALUE;
          }

          // notify the writer if needed
          if (_notifyWritePossible)
          {
            needNotify = true;
            _notifyWritePossible = false;
          }
        }

        if (needNotify)
        {
          try
          {
            _writer.onWritePossible();
          }
          catch (RuntimeException ex)
          {
            LOG.warn("Writer throws at onWritePossible", ex);
            // we can safely do cancel here as no WriteHandle method could be called at the same time
            synchronized (_lock)
            {
              _state = State.ABORTED;
            }
            doCancel(ex, true);
          }
        }
      }

      @Override
      public void cancel()
      {
        boolean doCancelNow;
        synchronized (_lock)
        {
          // this means writer is waiting for on WritePossible (cannot call WriteHandle.write) and has not called
          // WriteHandle.onDone() or WriteHandle.onError() yet, so we can safely do cancel here

          // otherwise, we would let the writer thread invoke doCancel later
          doCancelNow = _notifyWritePossible && _state == State.ACTIVE;
          if (doCancelNow)
          {
            _state = State.ABORTED;
          }
          else if (_state == State.ACTIVE)
          {
            _state = State.ABORT_REQUESTED;
          }
        }

        if (doCancelNow)
        {
          doCancel(getAbortedException(), false);
        }
      }
    }

    private void checkInit()
    {
      if (_state != State.UNINITIALIZED)
      {
        throw new IllegalStateException("EntityStream had already been initialized and can no longer accept Observers or Reader");
      }
    }

    private void safeAbortWriter(Throwable throwable)
    {
      try
      {
        _writer.onAbort(throwable);
      }
      catch (RuntimeException ex)
      {
        LOG.warn("Writer throws exception at onAbort", ex);
      }
    }

    private void safeNotifyErrorToObservers(Throwable throwable)
    {
      for (Observer observer : _observers)
      {
        try
        {
          observer.onError(throwable);
        }
        catch (RuntimeException ex)
        {
          LOG.warn("Observer throws exception at onError, ignored.", ex);
        }
      }
    }

    private void safeNotifyErrorToReader(Throwable throwable)
    {
      try
      {
        _reader.onError(throwable);
      }
      catch (RuntimeException ex)
      {
        LOG.error("Reader throws exception at onError", ex);
      }
    }

    private void doCancel(Throwable e, boolean notifyReader)
    {
      safeAbortWriter(e);

      safeNotifyErrorToObservers(e);

      if (notifyReader)
      {
        safeNotifyErrorToReader(e);
      }
    }

    private static Exception getAbortedException()
    {
      return new AbortedException("Reader aborted");
    }
  }
}
