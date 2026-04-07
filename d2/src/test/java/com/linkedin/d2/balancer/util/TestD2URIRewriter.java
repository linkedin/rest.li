/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.d2.balancer.util;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.client.utils.URIBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Test D2URIRewriter
 */
public class TestD2URIRewriter
{
  @Test
  public void testSimpleD2Rewrite() throws URISyntaxException
  {
    final URI httpURI = new URIBuilder("http://www.linkedin.com:1234/test").build();
    final URI d2URI = new URIBuilder("d2://serviceName/request/query?q=5678").build();
    final String expectURL = "http://www.linkedin.com:1234/test/request/query?q=5678";

    URIRewriter URIRewriter = new D2URIRewriter(httpURI);

    URI finalURI = URIRewriter.rewriteURI(d2URI);
    Assert.assertEquals(finalURI.toString(), expectURL);
  }

  @Test
  public void testSimpleD2RewriteSkipReEncoding() throws URISyntaxException
  {
    final URI httpURI = new URIBuilder("http://www.linkedin.com:1234/test").build();
    final URI d2URI = new URIBuilder("d2://serviceName/request/query?q=5678").build();
    final String expectURL = "http://www.linkedin.com:1234/test/request/query?q=5678";

    URIRewriter URIRewriter = new D2URIRewriter(httpURI, true);

    URI finalURI = URIRewriter.rewriteURI(d2URI);
    Assert.assertEquals(finalURI.toString(), expectURL);
  }

  @Test
  public void testSkipReEncodingWithEncodedQuery() throws URISyntaxException
  {
    final URI httpURI = new URIBuilder("http://www.linkedin.com:1234/test").build();
    final URI d2URI = URI.create("d2://serviceName/request/query?q=hello%20world&name=foo%26bar");
    final String expectURL = "http://www.linkedin.com:1234/test/request/query?q=hello%20world&name=foo%26bar";

    URI resultDefault = new D2URIRewriter(httpURI).rewriteURI(d2URI);
    URI resultFast = new D2URIRewriter(httpURI, true).rewriteURI(d2URI);
    Assert.assertEquals(resultDefault.toString(), expectURL);
    Assert.assertEquals(resultFast.toString(), expectURL);
  }

  @Test
  public void testSkipReEncodingNoQuery() throws URISyntaxException
  {
    final URI httpURI = new URIBuilder("http://www.linkedin.com:1234/test").build();
    final URI d2URI = URI.create("d2://serviceName/request/path");
    final String expectURL = "http://www.linkedin.com:1234/test/request/path";

    URI resultDefault = new D2URIRewriter(httpURI).rewriteURI(d2URI);
    URI resultFast = new D2URIRewriter(httpURI, true).rewriteURI(d2URI);
    Assert.assertEquals(resultDefault.toString(), expectURL);
    Assert.assertEquals(resultFast.toString(), expectURL);
  }

  @Test
  public void testSkipReEncodingWithFragment() throws URISyntaxException
  {
    final URI httpURI = new URIBuilder("http://www.linkedin.com:1234").build();
    final URI d2URI = URI.create("d2://serviceName/path?q=1#section");
    final String expectURL = "http://www.linkedin.com:1234/path?q=1#section";

    URI resultDefault = new D2URIRewriter(httpURI).rewriteURI(d2URI);
    URI resultFast = new D2URIRewriter(httpURI, true).rewriteURI(d2URI);
    Assert.assertEquals(resultDefault.toString(), expectURL);
    Assert.assertEquals(resultFast.toString(), expectURL);
  }
}
