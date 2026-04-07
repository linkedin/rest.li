/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.darkcluster;

import java.net.URI;

import com.linkedin.d2.balancer.util.D2URIRewriter;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestDarkClusterUrlRewrite
{

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

  @Test
  public void testRewriteGoodSkipReEncoding()
  {
    String darkServiceName = "FooCluster-dark";
    URI configuredURI = URI.create("d2://" + darkServiceName);
    D2URIRewriter rewriter = new D2URIRewriter(configuredURI, true);

    URI inputUri = URI.create("/MyRestliResource/foo/1");
    URI expectedURI = URI.create("d2://"+ darkServiceName + "/MyRestliResource/foo/1");
    URI outputURI = rewriter.rewriteURI(inputUri);
    Assert.assertEquals(outputURI, expectedURI, "URI's don't match");
  }

  @Test
  public void testRewriteWithQueryParamsSkipReEncoding()
  {
    String darkServiceName = "FooCluster-dark";
    URI configuredURI = URI.create("d2://" + darkServiceName);
    D2URIRewriter rewriter = new D2URIRewriter(configuredURI, true);

    URI inputUri = URI.create("/MyRestliResource/foo/1?param1=bar&param2=baz");
    URI expectedURI = URI.create("d2://"+ darkServiceName + "/MyRestliResource/foo/1?param1=bar&param2=baz");
    URI outputURI = rewriter.rewriteURI(inputUri);
    Assert.assertEquals(outputURI, expectedURI, "URI's don't match");
  }

  @Test
  public void testRewriteWithEncodedQueryParamsSkipReEncoding()
  {
    String darkServiceName = "FooCluster-dark";
    URI configuredURI = URI.create("d2://" + darkServiceName);
    D2URIRewriter rewriter = new D2URIRewriter(configuredURI, true);

    URI inputUri = URI.create("/MyRestliResource/foo/1?param1=hello%20world&param2=a%26b%3Dc");
    URI expectedURI = URI.create("d2://"+ darkServiceName + "/MyRestliResource/foo/1?param1=hello%20world&param2=a%26b%3Dc");
    URI outputURI = rewriter.rewriteURI(inputUri);
    Assert.assertEquals(outputURI, expectedURI, "URI's don't match");
  }

  @Test
  public void testRewriteSkipReEncodingMatchesDefault()
  {
    String darkServiceName = "FooCluster-dark";
    URI configuredURI = URI.create("d2://" + darkServiceName);
    D2URIRewriter defaultRewriter = new D2URIRewriter(configuredURI);
    D2URIRewriter fastRewriter = new D2URIRewriter(configuredURI, true);

    URI inputUri = URI.create("/MyRestliResource/foo/1?param1=bar&param2=baz");
    Assert.assertEquals(fastRewriter.rewriteURI(inputUri), defaultRewriter.rewriteURI(inputUri),
        "skipReEncoding result should match default");
  }
}
