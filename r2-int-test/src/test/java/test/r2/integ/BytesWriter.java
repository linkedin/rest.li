package test.r2.integ;

/**
 * @author Zhenkai Zhu
 */

import com.linkedin.data.ByteString;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.message.stream.entitystream.Writer;

import java.util.Arrays;

/** package private */ class BytesWriter implements Writer
{
  private final long _total;
  private final byte _fill;
  private long _written;
  private WriteHandle _wh;
  private volatile boolean _error = false;
  private volatile boolean _isDone = false;

  BytesWriter(long total, byte fill)
  {
    _total = total;
    _fill = fill;
    _written = 0;
  }

  @Override
  public void onInit(WriteHandle wh)
  {
    _wh = wh;
  }

  @Override
  public void onWritePossible()
  {

    while(_wh.remaining() >  0 && _written < _total && !_error)
    {
      int bytesNum = (int)Math.min(R2Constants.DEFAULT_DATA_CHUNK_SIZE, _total - _written);
      _wh.write(generate(bytesNum));
      _written += bytesNum;
      afterWrite(_wh, _written);
    }

    if (_written == _total && !_error)
    {
      _wh.done();
      _isDone = true;
      onFinish();
    }
  }

  @Override
  public void onAbort(Throwable ex)
  {
    // do nothing
  }

  public boolean isDone()
  {
    return _isDone;
  }

  protected void onFinish()
  {
    // nothing
  }

  protected void afterWrite(WriteHandle wh, long written)
  {
    // nothing
  }

  protected void markError()
  {
    _error = true;
  }

  private ByteString generate(int size)
  {
    byte[] result = new byte[size];
    Arrays.fill(result, _fill);
    return ByteString.copy(result);
  }
}
