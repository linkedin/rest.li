package test.r2.perf;

import com.linkedin.data.ByteString;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.message.stream.entitystream.Writer;

/**
 * @auther Zhenkai Zhu
 */

public class PerfStreamWriter implements Writer
{
  private WriteHandle _wh;
  private int _offset;
  private final int _msgSize;

  public PerfStreamWriter(int msgSize)
  {
    _msgSize = msgSize;
  }
  @Override
  public void onInit(WriteHandle wh)
  {
    _wh = wh;
  }

  @Override
  public void onWritePossible()
  {
    while(_wh.remaining() > 0)
    {
      if (_offset == _msgSize)
      {
        _wh.done();
        break;
      }
      int bytesToWrite = Math.min(R2Constants.DEFAULT_DATA_CHUNK_SIZE, _msgSize - _offset);
      _wh.write(ByteString.copy(new byte[bytesToWrite]));
      _offset += bytesToWrite;
    }
  }

  @Override
  public void onAbort(Throwable e)
  {

  }
}
