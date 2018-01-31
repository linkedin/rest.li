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
}
