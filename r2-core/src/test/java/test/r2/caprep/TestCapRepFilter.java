package test.r2.caprep;

import com.linkedin.r2.caprep.CapRepFilter;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.message.stream.StreamFilterAdapters;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.testutils.filter.CaptureLastCallFilter;
import com.linkedin.r2.testutils.filter.FilterUtil;
import com.linkedin.r2.testutils.filter.RestCountFilter;
import com.linkedin.r2.testutils.filter.StreamCountFilter;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @auther Zhenkai Zhu
 */
public class TestCapRepFilter
{
  private CapRepFilter _filter = new CapRepFilter();

  @Test
  public void testRestResponse() throws IOException
  {
    Path dirPath = Files.createTempDirectory("caprep-test");
    RestCountFilter after = new RestCountFilter();
    RestCountFilter before = new RestCountFilter();
    CaptureLastCallFilter lastCallFilter = new CaptureLastCallFilter();
    FilterChain fc = FilterChains.createRestChain(lastCallFilter, before, _filter, after);
    RestRequest myRequest = new RestRequestBuilder(URI.create("/req1")).setEntity("123".getBytes()).build();
    RestResponse myResponse = new RestResponseBuilder().setStatus(201).setEntity("321".getBytes()).build();
    _filter.capture(dirPath.toString());
    RequestContext requestContext = new RequestContext();
    FilterUtil.fireRestRequest(fc, myRequest, requestContext, FilterUtil.emptyWireAttrs());
    FilterUtil.fireRestResponse(fc, myResponse, requestContext, FilterUtil.emptyWireAttrs());

    Assert.assertEquals(after.getRestReqCount(), 1);
    Assert.assertEquals(after.getRestResCount(), 1);
    Assert.assertEquals(before.getRestReqCount(), 1);
    Assert.assertEquals(before.getRestResCount(), 1);

    lastCallFilter.reset();

    _filter.passThrough();
    FilterUtil.fireRestRequest(fc, myRequest);
    Assert.assertEquals(after.getRestReqCount(), 2);
    Assert.assertEquals(after.getRestResCount(), 1);
    Assert.assertEquals(before.getRestReqCount(), 2);
    Assert.assertEquals(before.getRestResCount(), 1);
    Assert.assertNull(lastCallFilter.getLastRes());

    _filter.replay(dirPath.toString());
    FilterUtil.fireSimpleRestRequest(fc);
    Assert.assertNull(lastCallFilter.getLastRes());

    FilterUtil.fireRestRequest(fc, myRequest);
    Assert.assertEquals(lastCallFilter.getLastRes(), myResponse);
    Assert.assertEquals(after.getRestReqCount(), 3);
    Assert.assertEquals(after.getRestResCount(), 1);
    Assert.assertEquals(before.getRestReqCount(), 4);
    Assert.assertEquals(before.getRestResCount(), 2);
  }

  @Test
  public void testRestException() throws IOException
  {
    Path dirPath = Files.createTempDirectory("caprep-test");
    CaptureLastCallFilter lastCallFilter = new CaptureLastCallFilter();
    FilterChain fc = FilterChains.createRestChain(lastCallFilter, _filter);
    RestRequest myRequest = new RestRequestBuilder(URI.create("/req1")).setEntity("123".getBytes()).build();
    RestResponse myErrorResponse = new RestResponseBuilder().setStatus(400).setEntity("321".getBytes()).build();
    RestException myRestException = new RestException(myErrorResponse);

    _filter.capture(dirPath.toString());
    RequestContext requestContext = new RequestContext();
    FilterUtil.fireRestRequest(fc, myRequest, requestContext, FilterUtil.emptyWireAttrs());
    FilterUtil.fireRestError(fc, myRestException, requestContext, FilterUtil.emptyWireAttrs());

    lastCallFilter.reset();
    _filter.replay(dirPath.toString());
    FilterUtil.fireSimpleRestRequest(fc);
    Assert.assertNull(lastCallFilter.getLastErr());

    FilterUtil.fireRestRequest(fc, myRequest);
    Assert.assertTrue(lastCallFilter.getLastErr() instanceof RestException);
    Assert.assertEquals(((RestException)lastCallFilter.getLastErr()).getResponse(), myErrorResponse);
  }

  @Test
  public void testStreamResponse() throws IOException
  {
    Path dirPath = Files.createTempDirectory("caprep-test");
    StreamCountFilter after = new StreamCountFilter();
    StreamCountFilter before = new StreamCountFilter();
    CaptureLastCallFilter lastCallFilter = new CaptureLastCallFilter();
    FilterChain fc = FilterChains.createStreamChain(StreamFilterAdapters.adaptRestFilter(lastCallFilter), before, _filter, after);
    RestRequest myRequest = new RestRequestBuilder(URI.create("/req1")).setEntity("123".getBytes()).build();
    RestResponse myResponse = new RestResponseBuilder().setStatus(201).setEntity("321".getBytes()).build();

    _filter.capture(dirPath.toString());
    RequestContext requestContext = new RequestContext();
    FilterUtil.fireStreamRequest(fc, Messages.toStreamRequest(myRequest), requestContext, FilterUtil.emptyWireAttrs());
    FilterUtil.fireStreamResponse(fc, Messages.toStreamResponse(myResponse), requestContext, FilterUtil.emptyWireAttrs());

    Assert.assertEquals(after.getStreamReqCount(), 1);
    Assert.assertEquals(after.getStreamResCount(), 1);
    Assert.assertEquals(before.getStreamReqCount(), 1);
    Assert.assertEquals(before.getStreamResCount(), 1);

    lastCallFilter.reset();

    _filter.passThrough();
    FilterUtil.fireStreamRequest(fc, Messages.toStreamRequest(myRequest));
    Assert.assertEquals(after.getStreamReqCount(), 2);
    Assert.assertEquals(after.getStreamResCount(), 1);
    Assert.assertEquals(before.getStreamReqCount(), 2);
    Assert.assertEquals(before.getStreamResCount(), 1);
    Assert.assertNull(lastCallFilter.getLastRes());

    _filter.replay(dirPath.toString());
    FilterUtil.fireSimpleStreamRequest(fc);
    Assert.assertNull(lastCallFilter.getLastRes());

    FilterUtil.fireStreamRequest(fc, Messages.toStreamRequest(myRequest), new RequestContext(), FilterUtil.emptyWireAttrs());
    Assert.assertEquals(lastCallFilter.getLastRes(), myResponse);
    Assert.assertEquals(after.getStreamReqCount(), 3);
    Assert.assertEquals(after.getStreamResCount(), 1);
    Assert.assertEquals(before.getStreamReqCount(), 4);
    Assert.assertEquals(before.getStreamResCount(), 2);
  }

  @Test
  public void testStreamException() throws IOException
  {
    Path dirPath = Files.createTempDirectory("caprep-test");
    CaptureLastCallFilter lastCallFilter = new CaptureLastCallFilter();
    FilterChain fc = FilterChains.createStreamChain(StreamFilterAdapters.adaptRestFilter(lastCallFilter), _filter);
    RestRequest myRequest = new RestRequestBuilder(URI.create("/req1")).setEntity("123".getBytes()).build();
    RestResponse myErrorResponse = new RestResponseBuilder().setStatus(400).setEntity("321".getBytes()).build();
    RestException myRestException = new RestException(myErrorResponse);

    _filter.capture(dirPath.toString());
    RequestContext requestContext = new RequestContext();
    FilterUtil.fireStreamRequest(fc, Messages.toStreamRequest(myRequest), requestContext, FilterUtil.emptyWireAttrs());
    FilterUtil.fireStreamError(fc, Messages.toStreamException(myRestException), requestContext, FilterUtil.emptyWireAttrs());

    lastCallFilter.reset();
    _filter.replay(dirPath.toString());
    FilterUtil.fireSimpleStreamRequest(fc);
    Assert.assertNull(lastCallFilter.getLastErr());

    FilterUtil.fireStreamRequest(fc, Messages.toStreamRequest(myRequest));
    Assert.assertTrue(lastCallFilter.getLastErr() instanceof RestException);
    Assert.assertEquals(((RestException)lastCallFilter.getLastErr()).getResponse(), myErrorResponse);
  }
}
