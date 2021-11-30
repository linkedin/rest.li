package test.r2.filter;

import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.testutils.filter.FilterUtil;
import com.linkedin.r2.testutils.filter.StreamCountFilter;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Zhenkai Zhu
 */

public class StreamFilterTest
{
  private StreamFilter _filter;
  private StreamCountFilter _beforeFilter;
  private StreamCountFilter _afterFilter;
  private FilterChain _fc;

  @BeforeMethod
  public void setUp() throws Exception
  {
    _filter = new StreamFilter() {};
    _beforeFilter = new StreamCountFilter();
    _afterFilter = new StreamCountFilter();
    _fc = FilterChains.createStreamChain(_beforeFilter, _filter, _afterFilter);
  }

  @Test
  public void testStreamRequestCallsNextFilter()
  {
    fireStreamRequest(_fc);

    Assert.assertEquals(1, _afterFilter.getStreamReqCount());
  }

  @Test
  public void testStreamResponseCallsNextFilter()
  {
    fireStreamResponse(_fc);

    Assert.assertEquals(1, _beforeFilter.getStreamResCount());
  }

  @Test
  public void testStreamErrorCallsNextFilter()
  {
    fireStreamError(_fc);

    Assert.assertEquals(1, _beforeFilter.getStreamErrCount());
  }

  private void fireStreamRequest(FilterChain fc)
  {
    fc.onStreamRequest(EasyMock.createMock(StreamRequest.class),
        createRequestContext(), createWireAttributes()
    );
  }

  private void fireStreamResponse(FilterChain fc)
  {
    fc.onStreamResponse(EasyMock.createMock(StreamResponse.class),
        createRequestContext(), createWireAttributes()
    );
  }

  private void fireStreamError(FilterChain fc)
  {
    fc.onStreamError(new Exception(),
        createRequestContext(), createWireAttributes()
    );
  }

  private Map<String, String> createWireAttributes()
  {
    return new HashMap<>();
  }

  private RequestContext createRequestContext()
  {
    return new RequestContext();
  }
}
