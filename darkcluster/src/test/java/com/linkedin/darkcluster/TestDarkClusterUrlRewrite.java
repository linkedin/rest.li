package com.linkedin.darkcluster;

import java.net.URI;

import com.linkedin.d2.balancer.util.D2URIRewriter;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestDarkClusterUrlRewrite
{
  D2URIRewriter _rewriter;

  @Test
  public void testRewriteGood()
  {
    String darkServiceName = "FooCluster-dark";
    URI configuredURI = URI.create("d2://" + darkServiceName);
    D2URIRewriter rewriter = new D2URIRewriter(configuredURI);

    URI inputUri = URI.create("/MyRestliResource/foo/1");
    URI expectedURI = URI.create("d2://"+ darkServiceName + "/MyRestliResource/foo/1");
    URI outputURI = rewriter.rewriteURI(inputUri);
    Assert.assertEquals(outputURI, expectedURI, "URI's don't match");
  }

  @Test
  public void testRewriteWithQueryParams()
  {
    String darkServiceName = "FooCluster-dark";
    URI configuredURI = URI.create("d2://" + darkServiceName);
    D2URIRewriter rewriter = new D2URIRewriter(configuredURI);

    URI inputUri = URI.create("/MyRestliResource/foo/1?param1=bar&param2=baz");
    URI expectedURI = URI.create("d2://"+ darkServiceName + "/MyRestliResource/foo/1?param1=bar&param2=baz");
    URI outputURI = rewriter.rewriteURI(inputUri);
    Assert.assertEquals(outputURI, expectedURI, "URI's don't match");
  }
}
