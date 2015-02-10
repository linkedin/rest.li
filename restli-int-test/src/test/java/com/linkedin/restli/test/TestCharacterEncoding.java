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

package com.linkedin.restli.test;


import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.client.uribuilders.RestliUriBuilderUtil;
import java.io.IOException;
import java.util.Collections;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.linkedin.common.callback.Callback;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.client.GetRequest;
import com.linkedin.restli.client.GetRequestBuilder;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpecImpl;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.TestConstants;
import com.linkedin.restli.server.RestLiConfig;
import com.linkedin.restli.server.RestLiServer;
import com.linkedin.restli.server.resources.PrototypeResourceFactory;


/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class TestCharacterEncoding
{
  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "protocolVersions")
  public void testQueryParamValueEncoding(ProtocolVersion protocolVersion)
  {
    RestLiConfig config = new RestLiConfig();
    config.setResourcePackageNames(QueryParamMockCollection.class.getPackage().getName());
    RestLiServer server = new RestLiServer(config, new PrototypeResourceFactory(), null);

    for (char c = 0; c < 256; ++c)
    {
      final String testValue = String.valueOf(c);
      GetRequest<EmptyRecord> req =
              new GetRequestBuilder<String, EmptyRecord>(
                      QueryParamMockCollection.RESOURCE_NAME,
                      EmptyRecord.class,
                      new ResourceSpecImpl(Collections.<ResourceMethod> emptySet(),
                                           Collections.<String, DynamicRecordMetadata> emptyMap(),
                                           Collections.<String, DynamicRecordMetadata> emptyMap(),
                                           String.class,
                                           null,
                                           null,
                                           EmptyRecord.class,
                                           Collections.<String, CompoundKey.TypeInfo> emptyMap()),
                      RestliRequestOptions.DEFAULT_OPTIONS)
                      .id("dummy")
                      .setParam(QueryParamMockCollection.VALUE_KEY, testValue).build();
      RestRequest restRequest = new RestRequestBuilder(RestliUriBuilderUtil.createUriBuilder(req, protocolVersion).build())
              .setMethod(req.getMethod().getHttpMethod().toString()).build();

      // N.B. since QueryParamMockCollection is implemented using the synchronous rest.li interface,
      // RestLiServer.handleRequest() will invoke the application resource *and* the callback
      // *synchronously*, ensuring that the all instances of the callback are invoked before the
      // loop terminates.
      server.handleRequest(restRequest, new RequestContext(), new Callback<RestResponse>()
      {
        @Override
        public void onError(Throwable e)
        {
          Assert.fail();
        }

        @Override
        public void onSuccess(RestResponse result)
        {
          try
          {
            DataMap data = new JacksonDataCodec().readMap(result.getEntity().asInputStream());
            Assert.assertEquals(data.get(QueryParamMockCollection.VALUE_KEY), testValue);
            Assert.assertEquals(QueryParamMockCollection._lastQueryParamValue, testValue);
          }
          catch (IOException e)
          {
            Assert.fail();
          }
        }
      });

    }
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "protocolVersions")
  public Object[][] protocolVersionsDataProvider()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), }
    };
  }
}
