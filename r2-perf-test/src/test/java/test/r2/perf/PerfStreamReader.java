package test.r2.perf;

import com.linkedin.common.callback.Callback;
import com.linkedin.data.ByteString;
import com.linkedin.r2.message.stream.entitystream.ReadHandle;
import com.linkedin.r2.message.stream.entitystream.Reader;

/**
 * @auther Zhenkai Zhu
 */

public class PerfStreamReader<T> implements Reader
{
  private ReadHandle _rh;
  private final Callback<T> _callback;
  private final T _response;

  public PerfStreamReader(Callback<T> callback, T response)
  {
    _callback = callback;
    _response = response;
  }

  @Override
  public void onInit(ReadHandle rh)
  {
    _rh = rh;
    _rh.request(1000);
  }

  @Override
  public void onDataAvailable(ByteString data)
  {
    _rh.request(1);
  }

  @Override
  public void onDone()
  {
    _callback.onSuccess(_response);
  }

  @Override
  public void onError(Throwable e)
  {
    _callback.onError(e);
  }
}
