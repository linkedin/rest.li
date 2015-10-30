package com.linkedin.r2.message.stream.entitystream;

import com.linkedin.data.ByteString;

/**
 * This is a convenient class to connect two entity streams and apply certain extra logic when passing along
 * read requests or data. E.g. wrap and handling errors, data transformation, etc.
 *
 * @author Zhenkai Zhu
 */
public class BaseConnector implements Reader, Writer
{
  private WriteHandle _wh;
  private ReadHandle _rh;
  private int _outstanding;
  private volatile boolean _aborted;

  public BaseConnector()
  {
    _outstanding = 0;
    _aborted = false;
  }

  @Override
  public void onInit(ReadHandle rh)
  {
    _rh = wrapReadHandle(rh);
  }

  @Override
  public void onInit(final WriteHandle wh)
  {
    _wh = wrapWriteHandle(wh);
  }


  @Override
  public void onDataAvailable(ByteString data)
  {
    if (!_aborted)
    {
      _outstanding--;
      _wh.write(data);
      int diff = _wh.remaining() - _outstanding;
      if (diff > 0)
      {
        _rh.request(diff);
        _outstanding += diff;
      }
    }
  }

  @Override
  public void onDone()
  {
    _wh.done();
  }

  @Override
  public void onError(Throwable e)
  {
    _wh.error(e);
  }

  @Override
  public void onWritePossible()
  {
    _outstanding = _wh.remaining();
    _rh.request(_outstanding);
  }

  @Override
  public void onAbort(Throwable e)
  {
    _aborted = true;
    _rh.cancel();
  }

  public void cancel()
  {
    if (_rh != null)
    {
      _rh.cancel();
    }
  }

  protected WriteHandle wrapWriteHandle(WriteHandle wh)
  {
    return wh;
  }

  protected ReadHandle wrapReadHandle(ReadHandle rh)
  {
    return rh;
  }

  protected final WriteHandle getWriteHandle()
  {
    return _wh;
  }

  protected final ReadHandle getReadHandle()
  {
    return _rh;
  }
}