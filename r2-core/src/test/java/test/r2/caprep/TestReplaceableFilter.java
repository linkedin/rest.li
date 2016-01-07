/*
   Copyright (c) 2012 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

/* $Id$ */
package test.r2.caprep;

import com.linkedin.data.ByteString;
import com.linkedin.r2.caprep.PassThroughFilter;
import com.linkedin.r2.caprep.ReplaceableFilter;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.Observer;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.message.stream.entitystream.Writer;
import com.linkedin.r2.testutils.filter.FilterUtil;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.linkedin.r2.testutils.filter.BaseFilterTest;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class TestReplaceableFilter extends BaseFilterTest
{
  // TODO: Pseudo-test to get gradle working with TestNG
  @Test
  public void psuedoTest()
  {
    Assert.assertTrue(true);
  }

  @Override
  protected ReplaceableFilter getFilter()
  {
    return new ReplaceableFilter(new PassThroughFilter());
  }

  @Test
  public void testSetAndGet()
  {
    final ReplaceableFilter filter = getFilter();
    final RestFilter newFilter = new PassThroughFilter();

    Assert.assertTrue(!filter.getFilter().equals(newFilter));
    filter.setFilter(newFilter);
    Assert.assertEquals(newFilter, filter.getFilter());
  }

  @Test
  public void testStreamingPassThrough()
  {
    ReplaceableFilter filter = getFilter();

    CountRequestChunksFilter countRequestChunksFilter = new CountRequestChunksFilter();
    CountResponseChunksFilter countResponseChunksFilter = new CountResponseChunksFilter();
    FilterChain filterChain = FilterChains.empty()
        .addLast(countResponseChunksFilter)
        .addLast(filter)
        .addLast(countRequestChunksFilter);

    int requestChunks = 100;
    int responseChunks = 200;
    StreamRequest streamRequest = new StreamRequestBuilder(URI.create("/test"))
        .build(EntityStreams.newEntityStream(new Writer()
        {
          private WriteHandle _wh;
          int _count = 0;

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
              if (_count < requestChunks)
              {
                _wh.write(ByteString.copy(new byte[10]));
                _count++;
              }
              else
              {
                _wh.done();
              }
            }
          }

          @Override
          public void onAbort(Throwable e)
          {

          }
        }));

    StreamResponse streamResponse = new StreamResponseBuilder()
        .build(EntityStreams.newEntityStream(new Writer()
        {
          private WriteHandle _wh;
          int _count = 0;

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
              if (_count < responseChunks)
              {
                _wh.write(ByteString.copy(new byte[10]));
                _count++;
              }
              else
              {
                _wh.done();
              }
            }
          }

          @Override
          public void onAbort(Throwable e)
          {

          }
        }));

    FilterUtil.fireStreamRequest(filterChain, streamRequest);
    FilterUtil.fireStreamResponse(filterChain, streamResponse, new RequestContext(), new HashMap<>());

    Assert.assertEquals(countRequestChunksFilter.getRequestChunks(), requestChunks);
    Assert.assertEquals(countResponseChunksFilter.getResponseChunks(), responseChunks);
  }

  private static class CountRequestChunksFilter implements StreamFilter
  {
    private int _requestChunks = 0;

    @Override
    public void onStreamRequest(StreamRequest req,
                                 RequestContext requestContext,
                                 Map<String, String> wireAttrs,
                                 NextFilter<StreamRequest, StreamResponse> nextFilter)
    {
      req.getEntityStream().addObserver(new Observer()
      {
        @Override
        public void onDataAvailable(ByteString data)
        {
          _requestChunks++;
        }

        @Override
        public void onDone()
        {

        }

        @Override
        public void onError(Throwable e)
        {

        }
      });
      nextFilter.onRequest(req, requestContext, wireAttrs);
    }

    int getRequestChunks()
    {
      return _requestChunks;
    }
  }

  private static class CountResponseChunksFilter implements StreamFilter
  {
    private int _responseChunks = 0;

    @Override
    public void onStreamResponse(StreamResponse res,
                                RequestContext requestContext,
                                Map<String, String> wireAttrs,
                                NextFilter<StreamRequest, StreamResponse> nextFilter)
    {
      res.getEntityStream().addObserver(new Observer()
      {
        @Override
        public void onDataAvailable(ByteString data)
        {
          _responseChunks++;
        }

        @Override
        public void onDone()
        {

        }

        @Override
        public void onError(Throwable e)
        {

        }
      });
      nextFilter.onResponse(res, requestContext, wireAttrs);
    }

    int getResponseChunks()
    {
      return _responseChunks;
    }
  }
}
