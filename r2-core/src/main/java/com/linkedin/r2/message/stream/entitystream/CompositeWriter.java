package com.linkedin.r2.message.stream.entitystream;

import com.linkedin.data.ByteString;

import java.util.Arrays;
import java.util.Iterator;


/**
 * @author Ang Xu
 */
public class CompositeWriter implements Writer
{
  private Iterator<EntityStream> _entityStreams;

  private WriteHandle _wh;

  private int _outstanding;
  private boolean _aborted = false;

  private ReadHandle _currentRh;
  private ReaderImpl _reader = new ReaderImpl();

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
    _outstanding = _wh.remaining();
    _currentRh.request(_outstanding);
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
    if (_entityStreams.hasNext())
    {
      EntityStream stream = _entityStreams.next();
      stream.setReader(_reader);
    }
    else
    {
      _wh.done();
    }
  }

  private void cancelAll()
  {
    while (_entityStreams.hasNext())
    {
      EntityStream stream = _entityStreams.next();
      stream.setReader(new CancelingReader());
    }
  }

  private class ReaderImpl implements Reader
  {
    @Override
    public void onInit(ReadHandle rh)
    {
      _currentRh = rh;
      if (_outstanding > 0)
      {
        _currentRh.request(_outstanding);
      }
    }

    @Override
    public void onDataAvailable(ByteString data)
    {
      if (!_aborted)
      {
        _wh.write(data);
        _outstanding--;
        int diff = _wh.remaining() - _outstanding;
        if (diff > 0)
        {
          _currentRh.request(diff);
          _outstanding += diff;
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
