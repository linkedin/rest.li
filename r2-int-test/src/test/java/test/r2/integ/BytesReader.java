package test.r2.integ;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.data.ByteString;
import com.linkedin.r2.message.stream.entitystream.ReadHandle;
import com.linkedin.r2.message.stream.entitystream.Reader;

/**
 * @author Zhenkai Zhu
 */
class BytesReader implements Reader
{
  private final byte _b;
  private final Callback<None> _callback;
  private int _length;
  private boolean _bytesCorrect;
  private ReadHandle _rh;

  BytesReader(byte b, Callback<None> callback)
  {
    _b = b;
    _callback = callback;
    _bytesCorrect = true;
    _length = 0;
  }

  @Override
  public void onInit(ReadHandle rh)
  {
    _rh = rh;
    _rh.request(3);
  }

  @Override
  public void onDataAvailable(ByteString data)
  {
    _length += data.length();
    byte [] bytes = data.copyBytes();
    for (byte b : bytes)
    {
      if (b != _b)
      {
        _bytesCorrect = false;
      }
    }
    requestMore(_rh);
  }

  @Override
  public void onDone()
  {
    _callback.onSuccess(None.none());
  }

  @Override
  public void onError(Throwable e)
  {
    _callback.onError(e);
  }

  public int getTotalBytes()
  {
    return _length;
  }

  public boolean allBytesCorrect()
  {
    return _bytesCorrect;
  }

  protected void requestMore(ReadHandle rh)
  {
    rh.request(1);
  }
}
