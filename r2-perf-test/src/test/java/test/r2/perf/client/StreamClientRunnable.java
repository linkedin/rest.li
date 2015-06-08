package test.r2.perf.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.Client;
import test.r2.perf.Generator;
import test.r2.perf.PerfStreamReader;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @auther Zhenkai Zhu
 */

public class StreamClientRunnable extends AbstractClientRunnable<StreamRequest, StreamResponse>
{
  private final Client _client;

  public StreamClientRunnable(Client client,
                              AtomicReference<Stats> stats,
                              CountDownLatch startLatch,
                              Generator<StreamRequest> reqGen)
  {
    super(stats, startLatch, reqGen);
    _client = client;
  }

  @Override
  protected void sendMessage(StreamRequest nextMsg, final Callback<StreamResponse> timingCallback)
  {
    Callback<StreamResponse> callback = new Callback<StreamResponse>()
    {
      @Override
      public void onError(Throwable e)
      {
        timingCallback.onError(e);
      }

      @Override
      public void onSuccess(StreamResponse result)
      {
        result.getEntityStream().setReader(new PerfStreamReader<StreamResponse>(timingCallback, result));
      }
    };
    _client.streamRequest(nextMsg, callback);
  }
}
