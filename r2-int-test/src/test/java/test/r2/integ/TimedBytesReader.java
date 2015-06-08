package test.r2.integ;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.message.stream.entitystream.ReadHandle;

/**
 * @author Zhenkai Zhu
 */
public class TimedBytesReader extends BytesReader
{
  private long _startTime;
  private long _stopTime;

  TimedBytesReader(byte b, Callback<None> callback)
  {
    super(b, callback);
  }

  @Override
  public void onInit(ReadHandle rh)
  {
    _startTime = System.currentTimeMillis();
    super.onInit(rh);
  }

  @Override
  public void onDone()
  {
    super.onDone();
    _stopTime = System.currentTimeMillis();
  }

  public long getStartTime()
  {
    return _startTime;
  }

  public long getStopTime()
  {
    return _stopTime;
  }
}
