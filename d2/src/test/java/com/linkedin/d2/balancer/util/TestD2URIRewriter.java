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
import java.util.ArrayList;
import java.util.List;
import org.apache.http.client.utils.URIBuilder;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
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
  public void testRewriteWithEncodedQuery() throws URISyntaxException
  {
    final URI httpURI = new URIBuilder("http://www.linkedin.com:1234/test").build();
    final URI d2URI = URI.create("d2://serviceName/request/query?q=hello%20world&name=foo%26bar");
    final String expectURL = "http://www.linkedin.com:1234/test/request/query?q=hello%20world&name=foo%26bar";

    URI result = new D2URIRewriter(httpURI).rewriteURI(d2URI);
    Assert.assertEquals(result.toString(), expectURL);
  }

  @Test
  public void testRewriteNoQuery() throws URISyntaxException
  {
    final URI httpURI = new URIBuilder("http://www.linkedin.com:1234/test").build();
    final URI d2URI = URI.create("d2://serviceName/request/path");
    final String expectURL = "http://www.linkedin.com:1234/test/request/path";

    URI result = new D2URIRewriter(httpURI).rewriteURI(d2URI);
    Assert.assertEquals(result.toString(), expectURL);
  }

  @Test
  public void testRewriteWithFragment() throws URISyntaxException
  {
    final URI httpURI = new URIBuilder("http://www.linkedin.com:1234").build();
    final URI d2URI = URI.create("d2://serviceName/path?q=1#section");
    final String expectURL = "http://www.linkedin.com:1234/path?q=1#section";

    URI result = new D2URIRewriter(httpURI).rewriteURI(d2URI);
    Assert.assertEquals(result.toString(), expectURL);
  }

  /**
   * Verify that rewriting preserves percent-encoded characters in query strings.
   * Uses {@link com.linkedin.jersey.api.uri.UriBuilder#replaceQueryFrom(URI)} which copies
   * the raw query directly, avoiding character-by-character re-encoding.
   */
  @DataProvider(name = "asciiQueryCharsEncoded")
  public Object[][] asciiQueryCharsEncoded()
  {
    List<Object[]> cases = new ArrayList<>();
    for (int c = 0x20; c <= 0x7E; c++)
    {
      // Every character in its percent-encoded form — this is how characters arrive
      // in real RestRequest URIs from the HTTP layer.
      String percentEncoded = String.format("%%%02X", c);
      cases.add(new Object[]{
          "percent-encoded 0x" + String.format("%02X", c) + " '" + (char) c + "'",
          "/path?q=" + percentEncoded
      });
    }

    // Realistic rest.li query strings
    cases.add(new Object[]{"restli query", "/myResource/1?q=findByName&name=hello%20world"});
    cases.add(new Object[]{"restli complex query",
        "/myResource?q=search&keywords=java%20engineer&start=0&count=10&fields=id,firstName,lastName"});
    cases.add(new Object[]{"restli batch get",
        "/myResource?ids=List(1,2,3)&fields=id,name"});
    cases.add(new Object[]{"query with encoded special chars",
        "/myResource?filter=(key%3Avalue)&sort=name%26date"});
    cases.add(new Object[]{"restli encoded brackets",
        "/myResource?criteria%5B0%5D=valueA&criteria%5B1%5D=valueB"});

    return cases.toArray(new Object[0][]);
  }

  @Test(dataProvider = "asciiQueryCharsEncoded")
  public void testRewritePreservesEncodedQuery(String description, String uriString)
  {
    URI configuredURI = URI.create("d2://testService");
    D2URIRewriter rewriter = new D2URIRewriter(configuredURI);

    URI input = URI.create(uriString);
    URI result = rewriter.rewriteURI(input);

    // The raw query should be preserved exactly as-is
    Assert.assertEquals(result.getRawQuery(), input.getRawQuery(),
        "Query not preserved for [" + description + "] input=" + uriString);
  }

  @DataProvider(name = "asciiQueryCharsLiteral")
  public Object[][] asciiQueryCharsLiteral()
  {
    List<Object[]> cases = new ArrayList<>();
    for (int c = 0x20; c <= 0x7E; c++)
    {
      // # terminates query, % needs hex pair
      if (c == '#' || c == '%')
      {
        continue;
      }

      String literal = "/path?q=" + (char) c;
      try
      {
        URI.create(literal);
        cases.add(new Object[]{
            "literal 0x" + String.format("%02X", c) + " '" + (char) c + "'",
            literal
        });
      }
      catch (IllegalArgumentException e)
      {
        // Character not valid in URI.create — skip
      }
    }
    return cases.toArray(new Object[0][]);
  }

  @Test(dataProvider = "asciiQueryCharsLiteral")
  public void testRewritePreservesLiteralQuery(String description, String uriString)
  {
    URI configuredURI = URI.create("d2://testService");
    D2URIRewriter rewriter = new D2URIRewriter(configuredURI);

    URI input = URI.create(uriString);
    URI result = rewriter.rewriteURI(input);

    // The raw query should be preserved exactly as-is
    Assert.assertEquals(result.getRawQuery(), input.getRawQuery(),
        "Query not preserved for [" + description + "] input=" + uriString);
  }
}
