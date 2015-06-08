package test.r2.filter;

import com.linkedin.common.callback.Callback;
import com.linkedin.data.ByteString;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.filter.message.stream.StreamFilterAdapters;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.stream.StreamException;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.FullEntityReader;
import com.linkedin.r2.testutils.filter.FilterUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.Map;

/**
 * @author Zhenkai Zhu
 */
public class TestStreamFilterAdapters
{
  private static final URI SIMPLE_URI = URI.create("simple_uri");
  private CaptureFilter _beforeFilter;
  private CaptureFilter _afterFilter;

  @Test
  public void testRequestFilterAdapterPassThrough()
  {
    FilterChain fc = adaptAndCreateFilterChain(new RestFilter()
    {
      @Override
      public void onRestRequest(RestRequest req, RequestContext requestContext, Map<String, String> wireAttrs, NextFilter<RestRequest, RestResponse> nextFilter)
      {
        nextFilter.onRequest(req, requestContext, wireAttrs);
      }
    });
    fc.onStreamRequest(simpleStreamRequest("12345"), FilterUtil.emptyRequestContext(), FilterUtil.emptyWireAttrs());
    StreamRequest capturedReq = _afterFilter.getRequest();
    Assert.assertEquals(capturedReq.getURI(), SIMPLE_URI);
    capturedReq.getEntityStream().setReader(new FullEntityReader(new Callback<ByteString>()
    {
      @Override
      public void onError(Throwable e)
      {
        Assert.fail("shouldn't have error");
      }

      @Override
      public void onSuccess(ByteString result)
      {
        Assert.assertEquals(result.asString("UTF8"), "12345");
      }
    }));
  }

  @Test
  public void testRequestFilterAdapterChangeRequest()
  {
    FilterChain fc = adaptAndCreateFilterChain(new RestFilter()
    {
      @Override
      public void onRestRequest(RestRequest req, RequestContext requestContext, Map<String, String> wireAttrs, NextFilter<RestRequest, RestResponse> nextFilter)
      {
        nextFilter.onRequest(req.builder().
            setEntity(req.getEntity().asString("UTF8").replace('1', '0').getBytes()).build(),
            requestContext, wireAttrs);
      }
    });

    fc.onStreamRequest(simpleStreamRequest("12345"), FilterUtil.emptyRequestContext(), FilterUtil.emptyWireAttrs());
    StreamRequest capturedReq = _afterFilter.getRequest();
    Assert.assertEquals(capturedReq.getURI(), SIMPLE_URI);
    capturedReq.getEntityStream().setReader(new FullEntityReader(new Callback<ByteString>()
    {
      @Override
      public void onError(Throwable e)
      {
        Assert.fail("shouldn't have error");
      }

      @Override
      public void onSuccess(ByteString result)
      {
        Assert.assertEquals(result.asString("UTF8"), "02345");
      }
    }));
  }

  @Test
  public void testRequestFilterAdapterCallsOnResponse()
  {
    FilterChain fc = adaptAndCreateFilterChain(new RestFilter()
    {
      @Override
      public void onRestRequest(RestRequest req, RequestContext requestContext, Map<String, String> wireAttrs, NextFilter<RestRequest, RestResponse> nextFilter)
      {
        nextFilter.onResponse(simpleRestResponse(req.getEntity().asString("UTF8")), requestContext, wireAttrs);
      }
    });

    fc.onStreamRequest(simpleStreamRequest("12345"), FilterUtil.emptyRequestContext(), FilterUtil.emptyWireAttrs());
    StreamResponse capturedReq = _beforeFilter.getResponse();
    capturedReq.getEntityStream().setReader(new FullEntityReader(new Callback<ByteString>()
    {
      @Override
      public void onError(Throwable e)
      {
        Assert.fail("shouldn't have error");
      }

      @Override
      public void onSuccess(ByteString result)
      {
        Assert.assertEquals(result.asString("UTF8"), "12345");
      }
    }));
  }

  @Test
  public void testRequestFilterAdapterCallsOnError()
  {
    final Exception runTimeException = new RuntimeException();
    FilterChain fc = adaptAndCreateFilterChain(new RestFilter()
    {
      @Override
      public void onRestRequest(RestRequest req, RequestContext requestContext, Map<String, String> wireAttrs, NextFilter<RestRequest, RestResponse> nextFilter)
      {
        nextFilter.onError(runTimeException, requestContext, wireAttrs);
      }
    });

    fc.onStreamRequest(simpleStreamRequest("12345"), FilterUtil.emptyRequestContext(), FilterUtil.emptyWireAttrs());

    Throwable ex = _beforeFilter.getThrowable();
    Assert.assertSame(ex, runTimeException);

    fc = adaptAndCreateFilterChain(new RestFilter()
    {
      @Override
      public void onRestRequest(RestRequest req, RequestContext requestContext, Map<String, String> wireAttrs, NextFilter<RestRequest, RestResponse> nextFilter)
      {
        nextFilter.onError(simpleRestException(req.getEntity().asString("UTF8")), requestContext, wireAttrs);
      }
    });

    fc.onStreamRequest(simpleStreamRequest("12345"), FilterUtil.emptyRequestContext(), FilterUtil.emptyWireAttrs());
    ex = _beforeFilter.getThrowable();
    Assert.assertTrue(ex instanceof StreamException);
    StreamResponse errorResponse = ((StreamException) ex).getResponse();
    errorResponse.getEntityStream().setReader(new FullEntityReader(new Callback<ByteString>()
    {
      @Override
      public void onError(Throwable e)
      {
        Assert.fail("should not happen");
      }

      @Override
      public void onSuccess(ByteString result)
      {
        Assert.assertEquals(result.asString("UTF8"), "12345");
      }
    }));
  }

  @Test
  public void testResponseFilterAdapterPassThrough()
  {
    FilterChain fc = adaptAndCreateFilterChain(new RestFilter()
    {
      @Override
      public void onRestResponse(RestResponse res, RequestContext requestContext, Map<String, String> wireAttrs, NextFilter<RestRequest, RestResponse> nextFilter)
      {
        nextFilter.onResponse(res, requestContext, wireAttrs);
      }

      @Override
      public void onRestError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs, NextFilter<RestRequest, RestResponse> nextFilter)
      {
        nextFilter.onError(ex, requestContext, wireAttrs);
      }
    });

    fc.onStreamResponse(simpleStreamResponse("12345"), FilterUtil.emptyRequestContext(), FilterUtil.emptyWireAttrs());
    StreamResponse capturedResponse = _beforeFilter.getResponse();
    capturedResponse.getEntityStream().setReader(new FullEntityReader(new Callback<ByteString>()
    {
      @Override
      public void onError(Throwable e)
      {
        Assert.fail("should not happen");
      }

      @Override
      public void onSuccess(ByteString result)
      {
        Assert.assertEquals(result.asString("UTF8"), "12345");
      }
    }));

    fc.onStreamError(simpleStreamException("12345"), FilterUtil.emptyRequestContext(), FilterUtil.emptyWireAttrs());
    Throwable capturedEx = _beforeFilter.getThrowable();

    Assert.assertTrue(capturedEx instanceof StreamException);
    ((StreamException) capturedEx).getResponse().getEntityStream().setReader(new FullEntityReader(new Callback<ByteString>()
    {
      @Override
      public void onError(Throwable e)
      {
        Assert.fail("should not happen");
      }

      @Override
      public void onSuccess(ByteString result)
      {
        Assert.assertEquals(result.asString("UTF8"), "12345");
      }
    }));
  }

  @Test
  public void testResponseFilterAdapterChangeResponse()
  {
    FilterChain fc = adaptAndCreateFilterChain(new RestFilter()
    {
      @Override
      public void onRestResponse(RestResponse res, RequestContext requestContext, Map<String, String> wireAttrs, NextFilter<RestRequest, RestResponse> nextFilter)
      {
        nextFilter.onResponse(res.builder().setEntity(res.getEntity().asString("UTF8").replace('1', '0').getBytes()).build(),
            requestContext, wireAttrs);
      }

      @Override
      public void onRestError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs, NextFilter<RestRequest, RestResponse> nextFilter)
      {
      }
    });

    fc.onStreamResponse(simpleStreamResponse("12345"), FilterUtil.emptyRequestContext(), FilterUtil.emptyWireAttrs());
    StreamResponse capturedResponse = _beforeFilter.getResponse();
    capturedResponse.getEntityStream().setReader(new FullEntityReader(new Callback<ByteString>()
    {
      @Override
      public void onError(Throwable e)
      {
        Assert.fail("should not happen");
      }

      @Override
      public void onSuccess(ByteString result)
      {
        Assert.assertEquals(result.asString("UTF8"), "02345");
      }
    }));
  }

  @Test
  public void testResponseFilterAdapterChangeError()
  {
    FilterChain fc = adaptAndCreateFilterChain(new RestFilter()
    {
      @Override
      public void onRestResponse(RestResponse res, RequestContext requestContext, Map<String, String> wireAttrs, NextFilter<RestRequest, RestResponse> nextFilter)
      {
      }

      @Override
      public void onRestError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs, NextFilter<RestRequest, RestResponse> nextFilter)
      {
        if (ex instanceof RestException)
        {
          RestResponse res = ((RestException) ex).getResponse();
          String newEntityStr = res.getEntity().asString("UTF8").replace('1', '0');
          nextFilter.onError(new RestException(
                  (res.builder().setEntity(newEntityStr.getBytes()).build())),
              requestContext, wireAttrs);
        }
        else
        {
          nextFilter.onError(new IllegalStateException(), requestContext, wireAttrs);
        }
      }
    });

    fc.onStreamError(simpleStreamException("12345"), FilterUtil.emptyRequestContext(), FilterUtil.emptyWireAttrs());
    Throwable capturedEx = _beforeFilter.getThrowable();

    Assert.assertTrue(capturedEx instanceof StreamException);
    ((StreamException) capturedEx).getResponse().getEntityStream().setReader(new FullEntityReader(new Callback<ByteString>()
    {
      @Override
      public void onError(Throwable e)
      {
        Assert.fail("should not happen");
      }

      @Override
      public void onSuccess(ByteString result)
      {
        Assert.assertEquals(result.asString("UTF8"), "02345");
      }
    }));

    fc.onStreamError(new IllegalArgumentException(), FilterUtil.emptyRequestContext(), FilterUtil.emptyWireAttrs());
    capturedEx = _beforeFilter.getThrowable();
    Assert.assertTrue(capturedEx instanceof IllegalStateException);
  }

  @Test
  public void testResponseFilterAdapterCallsOnErrorInOnResponse()
  {
    FilterChain fc = adaptAndCreateFilterChain(new RestFilter()
    {
      @Override
      public void onRestResponse(RestResponse res, RequestContext requestContext, Map<String, String> wireAttrs, NextFilter<RestRequest, RestResponse> nextFilter)
      {
        nextFilter.onError(simpleRestException(res.getEntity().asString("UTF8")), requestContext, wireAttrs);
      }

      @Override
      public void onRestError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs, NextFilter<RestRequest, RestResponse> nextFilter)
      {
      }
    });

    fc.onStreamResponse(simpleStreamResponse("12345"), FilterUtil.emptyRequestContext(), FilterUtil.emptyWireAttrs());
    Throwable capturedEx = _beforeFilter.getThrowable();

    Assert.assertTrue(capturedEx instanceof StreamException);
    ((StreamException) capturedEx).getResponse().getEntityStream().setReader(new FullEntityReader(new Callback<ByteString>()
    {
      @Override
      public void onError(Throwable e)
      {
        Assert.fail("should not happen");
      }

      @Override
      public void onSuccess(ByteString result)
      {
        Assert.assertEquals(result.asString("UTF8"), "12345");
      }
    }));

    fc = adaptAndCreateFilterChain(new RestFilter()
    {
      @Override
      public void onRestResponse(RestResponse res, RequestContext requestContext, Map<String, String> wireAttrs, NextFilter<RestRequest, RestResponse> nextFilter)
      {
        nextFilter.onError(new IllegalStateException(), requestContext, wireAttrs);
      }

      @Override
      public void onRestError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs, NextFilter<RestRequest, RestResponse> nextFilter)
      {
      }
    });

    fc.onStreamResponse(simpleStreamResponse("12345"), FilterUtil.emptyRequestContext(), FilterUtil.emptyWireAttrs());
    capturedEx = _beforeFilter.getThrowable();

    Assert.assertTrue(capturedEx instanceof IllegalStateException);
  }

  private FilterChain adaptAndCreateFilterChain(RestFilter filter)
  {
    _beforeFilter = new CaptureFilter();
    _afterFilter = new CaptureFilter();
    return FilterChains.createStreamChain(_beforeFilter, StreamFilterAdapters.adaptRestFilter(filter), _afterFilter);
  }


  private static class CaptureFilter implements StreamFilter
  {
    private StreamRequest _req = null;
    private StreamResponse _res = null;
    private Throwable _ex = null;

    @Override
    public void onStreamRequest(StreamRequest req,
                   RequestContext requestContext,
                   Map<String, String> wireAttrs,
                   NextFilter<StreamRequest, StreamResponse> nextFilter)
    {
      _req = req;
      nextFilter.onRequest(req, requestContext, wireAttrs);
    }

    @Override
    public void onStreamResponse(StreamResponse res,
                    RequestContext requestContext,
                    Map<String, String> wireAttrs,
                    NextFilter<StreamRequest, StreamResponse> nextFilter)
    {
      _res = res;
      nextFilter.onResponse(res, requestContext, wireAttrs);
    }

    @Override
    public void onStreamError(Throwable ex,
                 RequestContext requestContext,
                 Map<String, String> wireAttrs,
                 NextFilter<StreamRequest, StreamResponse> nextFilter)
    {
      _ex = ex;
      nextFilter.onError(ex, requestContext, wireAttrs);
    }

    public StreamRequest getRequest()
    {
      return _req;
    }

    public StreamResponse getResponse()
    {
      return _res;
    }

    public Throwable getThrowable()
    {
      return _ex;
    }
  }

  private static StreamRequest simpleStreamRequest(String str)
  {
    return new StreamRequestBuilder(SIMPLE_URI)
        .build(EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy(str.getBytes()))));
  }

  private static StreamResponse simpleStreamResponse(String str)
  {
    return new StreamResponseBuilder()
        .build(EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy(str.getBytes()))));
  }

  private static StreamException simpleStreamException(String str)
  {
    return new StreamException(simpleStreamResponse(str));
  }

  private static RestRequest simpleRestRequest(String str)
  {
    return new RestRequestBuilder(SIMPLE_URI).setEntity(str.getBytes())
        .build();
  }

  private static RestResponse simpleRestResponse(String str)
  {
    return new RestResponseBuilder().setEntity(str.getBytes())
        .build();
  }

  private static RestException simpleRestException(String str)
  {
    return new RestException(simpleRestResponse(str));
  }
}
