package com.linkedin.r2.streaming.sample;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.StreamRequestHandler;

import java.net.URI;

/**
 * A simple proxy that rewrites URI for request to downstream and relays response back to upstream.
 * Nevertheless, back pressure is still achieved.
 *
 * @author Zhenkai Zhu
 */
public class UriRewriteProxy implements StreamRequestHandler
{
  final private Client _client;
  final private UriRewriter _uriRewriter;

  public UriRewriteProxy(Client client, UriRewriter uriRewriter)
  {
    _client = client;
    _uriRewriter = uriRewriter;
  }

  @Override
  public void handleRequest(StreamRequest request, RequestContext requestContext, final Callback<StreamResponse> callback)
  {
    URI newUri = _uriRewriter.rewrite(request.getURI());
    StreamRequestBuilder builder = new StreamRequestBuilder(request);
    builder.setURI(newUri);
    StreamRequest newRequest = builder.build(request.getEntityStream());

    _client.streamRequest(newRequest, requestContext, callback);
  }

  public interface UriRewriter
  {
    /**
     * Returns a new URI based on the input uri
     * e.g. d2://company/1000 -> http://192.168.0.1/company/1000
     * @param uri input uri
     * @return the rewritten uri
     */
    URI rewrite(URI uri);
  }
}
