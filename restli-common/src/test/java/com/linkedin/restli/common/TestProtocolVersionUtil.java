/*
   Copyright (c) 2014 LinkedIn Corp.

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


package com.linkedin.restli.common;

import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.restli.internal.common.ProtocolVersionUtil;
import java.net.URI;
import java.net.URISyntaxException;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Soojung Ha
 */
public class TestProtocolVersionUtil
{
  @Test
  public void testExtractProtocolVersion() throws URISyntaxException
  {
    ProtocolVersion p1 = new ProtocolVersion("1.2.3");
    RestRequestBuilder requestBuilder = new RestRequestBuilder(
        new URI("/test/1")).setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, p1.toString());
    ProtocolVersion p2 = ProtocolVersionUtil.extractProtocolVersion(requestBuilder.getHeaders());
    Assert.assertEquals(p2, p1);
  }
}