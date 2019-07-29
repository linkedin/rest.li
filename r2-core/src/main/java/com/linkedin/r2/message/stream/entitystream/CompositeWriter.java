package com.linkedin.r2.message.stream.entitystream;

import com.linkedin.data.ByteString;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * A writer composed of multiple writers. Each individual writer will be used to write to the stream in the order they
 * are provided.
 *
 * @author Ang Xu
 * @author Karthik Balasubramanian
 */
public class CompositeWriter implements Writer
{
  private final Iterator<EntityStream> _entityStreams;

  private WriteHandle _wh;

  private volatile int _outstanding;
  private volatile boolean _aborted = false;

  private volatile ReadHandle _currentRh;
  private final ReaderImpl _reader = new ReaderImpl();
  private final Object _lock = new Object();

  public CompositeWriter(Writer... writers)
  {
    this(toEntityStreams(writers));
  }

  public CompositeWriter(EntityStream... entityStreams)
  {
    this(Arrays.asList(entityStreams));
  }

  public CompositeWriter(Iterable<EntityStream> entityStreams)
  {
    _entityStreams = entityStreams.iterator();
    _outstanding = 0;
  }

  @Override
  public void onInit(WriteHandle wh)
  {
    _wh = wh;
    readNextStream();
  }

  @Override
  public void onWritePossible()
  {
    // Entry point when the stream notifies more data can be written. This can be invoked when one of the input writers
    // is executing in a separate threadpool.
    int newOutstanding = _wh.remaining();
    synchronized (_lock)
    {
      _outstanding = newOutstanding;
    }
    if (newOutstanding > 0)
    {
      _currentRh.request(newOutstanding);
    }
  }

  @Override
  public void onAbort(Throwable e)
  {
    _aborted = true;
    _currentRh.cancel();
    cancelAll();
  }

  private void readNextStream()
  {
    EntityStream nextStream = null;
    synchronized (_lock)
    {
      if (_entityStreams.hasNext())
      {
        nextStream = _entityStreams.next();
      }
    }
    if (nextStream != null)
    {
      nextStream.setReader(_reader);
    }
    else
    {
      _wh.done();
    }
  }

  private void cancelAll()
  {
    List<EntityStream> pendingStreams = new LinkedList<>();
    synchronized (_lock)
    {
      while (_entityStreams.hasNext())
      {
        pendingStreams.add(_entityStreams.next());
      }
    }
    pendingStreams.forEach(stream -> stream.setReader(new CancelingReader()));
  }

  private class ReaderImpl implements Reader
  {
    @Override
    public void onInit(ReadHandle rh)
    {
      _currentRh = rh;
      int outstanding = _outstanding;
      if (outstanding > 0)
      {
        _currentRh.request(outstanding);
      }
    }

    @Override
    public void onDataAvailable(ByteString data)
    {
      // Entry point from individual writers when they have data to write.
      // This can be invoked only by the current writer, but can be invoked in parallel to notifications from the
      // stream this composite writer is writing to.
      if (!_aborted)
      {
        _wh.write(data);
        int diff;
        synchronized (_lock)
        {
          _outstanding--;
          int newOutstanding = _wh.remaining();
          diff = newOutstanding - _outstanding;
          if (diff > 0)
          {
            _outstanding = newOutstanding;
          }
        }
        if (diff > 0)
        {
          _currentRh.request(diff);
        }
      }
    }

    @Override
    public void onDone()
    {
      if (!_aborted)
      {
        readNextStream();
      }
    }

    @Override
    public void onError(Throwable e)
    {
      _wh.error(e);
      cancelAll();
    }
  }

  private static EntityStream[] toEntityStreams(Writer... writers)
  {
    EntityStream[] entityStreams = new EntityStream[writers.length];
    for (int i = 0; i < writers.length; ++i)
    {
      entityStreams[i] = EntityStreams.newEntityStream(writers[i]);
    }
    return entityStreams;
  }
}
