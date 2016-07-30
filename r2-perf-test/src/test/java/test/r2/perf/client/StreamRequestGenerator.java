package test.r2.perf.client;

import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import java.util.stream.IntStream;
import test.r2.perf.Generator;
import test.r2.perf.PerfStreamWriter;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import test.r2.perf.StringGenerator;


/**
 * @auther Zhenkai Zhu
 */

public class StreamRequestGenerator implements Generator<StreamRequest>
{
  private static final String HTTP_POST_METHOD = "POST";
  private static final String STATIC_HEADER_PREFIX = "X-LI-HEADER-";

  private final URI _uri;
  private final int _msgSize;
  private final int _numHeaders;
  private final AtomicInteger _msgCounter;
  private final String _headerContent;

  public StreamRequestGenerator(URI uri, int numMsgs, int msgSize, int numHeaders, int headerSize)
  {
    _uri = uri;
    _msgCounter = new AtomicInteger(numMsgs);
    _msgSize = msgSize;
    _numHeaders = numHeaders;
    _headerContent = new StringGenerator(headerSize).nextMessage();
  }

  @Override
  public StreamRequest nextMessage()
  {
    if (_msgCounter.getAndDecrement() > 0)
    {
      StreamRequestBuilder builder = new StreamRequestBuilder(_uri);
      builder.setMethod(HTTP_POST_METHOD);
      for (int i = 0; i < _numHeaders; i++)
      {
        builder.setHeader(STATIC_HEADER_PREFIX + i, _headerContent);
      }
      return builder.build(EntityStreams.newEntityStream(new PerfStreamWriter(_msgSize)));
    }
    else
    {
      return null;
    }
  }
}
