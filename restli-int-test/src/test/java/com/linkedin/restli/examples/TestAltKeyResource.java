/*
   Copyright (c) 2019 LinkedIn Corp.

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
package com.linkedin.restli.examples;

import com.linkedin.data.DataMap;
import com.linkedin.r2.message.rest.RestMethod;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.TwoPartKey;
import com.linkedin.restli.examples.greetings.server.altkey.StringComplexKeyCoercer;
import com.linkedin.restli.examples.greetings.server.altkey.StringCompoundKeyCoercer;
import com.linkedin.restli.examples.greetings.server.altkey.StringLongCoercer;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.linkedin.restli.common.RestConstants.HEADER_RESTLI_ID;
import static com.linkedin.restli.common.RestConstants.HEADER_RESTLI_PROTOCOL_VERSION;
import static com.linkedin.restli.internal.common.AllProtocolVersions.RESTLI_PROTOCOL_2_0_0;

/**
 * Integration tests to test Alternative Key for different entity-level methods (get, batchGet, update, partialUpdate,
 * batchUpdate, batchPartialUpdate, delete, batchDelete, create, entity-level action)
 * Make sure that Alternative Key behaves in the same way as Primary key.
 *
 * These integration tests send requests to {@link com.linkedin.restli.examples.greetings.server.altkey.CollectionAltKeyResource},
 * {@link com.linkedin.restli.examples.greetings.server.altkey.AssociationAltKeyResource},
 * {@link com.linkedin.restli.examples.greetings.server.altkey.ComplexKeyAltKeyResource},
 * {@link com.linkedin.restli.examples.greetings.server.altkey.AltKeySubResource}.
 *
 * @author Yingjie Bi
 */
public class TestAltKeyResource extends RestLiIntegrationTest
{
  private Client client;
  private final static String URI_PREFIX = "http://localhost:";
  private final static String PROTOCOL_VERSION_2 = RESTLI_PROTOCOL_2_0_0.getProtocolVersion().toString();

  @BeforeClass
  public void initClass() throws Exception
  {
    super.init();
    Map<String, String> transportProperties = Collections.singletonMap(HttpClientFactory.HTTP_REQUEST_TIMEOUT, "1000");
    client = newTransportClient(transportProperties);
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  @Test
  public void testAltKeyGet() throws Throwable
  {
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/altKey/Alt1?altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.GET).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/altKey/1");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.GET).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    verifyResult(altKeyResponse, primaryKeyResponse);
  }

  @Test
  public void testAltKeyBatchGet() throws Throwable
  {
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/altKey?ids=List(Alt1,Alt2)&altkey=alt");
    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/altKey?ids=List(1,2)");

    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.GET).build();

    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.GET).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    Assert.assertNotNull(altKeyResponse.getEntity());
    String altKeyResult1 = getBatchResult(altKeyResponse, "Alt1");
    String altKeyResult2 = getBatchResult(altKeyResponse, "Alt2");

    Assert.assertNotNull(primaryKeyResponse.getEntity());
    String priKeyResult1 = getBatchResult(primaryKeyResponse, "1");
    String priKeyResult2 = getBatchResult(primaryKeyResponse, "2");

    Assert.assertEquals(altKeyResult1, priKeyResult1);
    Assert.assertEquals(altKeyResult2, priKeyResult2);
  }

  @Test
  public void testAltKeyUpdate() throws Throwable
  {
    Greeting greeting = new Greeting().setMessage("Update Message");
    byte[] jsonByte = DataMapUtils.dataTemplateToBytes(greeting, true);
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/altKey/Alt1?altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.PUT).setEntity(jsonByte).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/altKey/1");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.PUT).setEntity(jsonByte).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    verifyResult(altKeyResponse, primaryKeyResponse);
  }

  @Test
  public void testAltKeyPartialUpdate() throws Throwable
  {
    String json = "{ \n" + "  \"patch\" : { \n" + "    \"$set\" : { \n"
        + "      \"message\" : \"partial updated message\" \n" + "    } \n" + "  } \n" + "} ";
    byte[] jsonByte = json.getBytes();
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/altKey/Alt1?altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.POST).setEntity(jsonByte).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/altKey/1");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.POST).setEntity(jsonByte).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    verifyResult(altKeyResponse, primaryKeyResponse);
  }

  @Test
  public void testAltKeyBatchUpdate() throws Throwable
  {
    String json1 = "{ \n" + "  \"entities\" : { \n" + "    \"Alt1\" : { \n"
        + "      \"message\" : \"inserted message\" }, \n" + "    \"Alt2\" : { \n"
        + "      \"message\" : \"updated message\" }\n" + "  } \n" + "} ";
    byte[] jsonByte1 = json1.getBytes();
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/altKey?ids=List(Alt1,Alt2)&altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.PUT).setEntity(jsonByte1).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    String json2 = "{ \n" + "  \"entities\" : { \n" + "    \"1\" : { \n"
        + "      \"message\" : \"inserted message\" }, \n" + "    \"2\" : { \n"
        + "      \"message\" : \"updated message\" }\n" + "  } \n" + "} ";
    byte[] jsonByte2 = json2.getBytes();
    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/altKey?ids=List(1,2)");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.PUT).setEntity(jsonByte2).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    Assert.assertNotNull(altKeyResponse.getEntity());
    String altKeyResult1 = getBatchResult(altKeyResponse, "Alt1");
    String altKeyResult2 = getBatchResult(altKeyResponse, "Alt2");

    Assert.assertNotNull(primaryKeyResponse.getEntity());
    String priKeyResult1 = getBatchResult(primaryKeyResponse, "1");
    String priKeyResult2 = getBatchResult(primaryKeyResponse, "2");

    Assert.assertEquals(altKeyResult1, priKeyResult1);
    Assert.assertEquals(altKeyResult2, priKeyResult2);
  }

  @Test
  public void testAltKeyBatchPartialUpdate() throws Throwable
  {
    String json1 = "{ \n" + "  \"entities\" : { \n" + "    \"Alt1\" : { \n" + "      \"patch\" : { \n"
        + "        \"$set\" : { \n" + "          \"message\" : \"another partial message\" \n" + "        } \n"
        + "      } \n" + "    }, \n" + "    \"Alt2\" : { \n" + "      \"patch\" : { \n" + "        \"$set\" : { \n"
        + "          \"message\" : \"partial updated message\" \n" + "        } \n" + "      } \n" + "    } \n"
        + "  } \n" + "} ";
    byte[] jsonByte1 = json1.getBytes();
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/altKey?ids=List(Alt1,Alt2)&altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.POST).setEntity(jsonByte1).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    String json2 = "{ \n" + "  \"entities\" : { \n" + "    \"1\" : { \n" + "      \"patch\" : { \n"
        + "        \"$set\" : { \n" + "          \"message\" : \"another partial message\" \n" + "        } \n"
        + "      } \n" + "    }, \n" + "    \"2\" : { \n" + "      \"patch\" : { \n" + "        \"$set\" : { \n"
        + "          \"message\" : \"partial updated message\" \n" + "        } \n" + "      } \n" + "    } \n"
        + "  } \n" + "} ";
    byte[] jsonByte2 = json2.getBytes();
    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/altKey?ids=List(1,2)");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.POST).setEntity(jsonByte2).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    Assert.assertNotNull(altKeyResponse.getEntity());
    String altKeyResult1 = getBatchResult(altKeyResponse, "Alt1");
    String altKeyResult2 = getBatchResult(altKeyResponse, "Alt2");

    Assert.assertNotNull(primaryKeyResponse.getEntity());
    String priKeyResult1 = getBatchResult(primaryKeyResponse, "1");
    String priKeyResult2 = getBatchResult(primaryKeyResponse, "2");

    Assert.assertEquals(altKeyResult1, priKeyResult1);
    Assert.assertEquals(altKeyResult2, priKeyResult2);
  }

  @Test
  public void testAltKeyDelete() throws Throwable
  {
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/altKey/Alt1?altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.DELETE).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/altKey/1");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.DELETE).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    verifyResult(altKeyResponse, primaryKeyResponse);
  }

  @Test
  public void testAltKeyBatchDelete() throws Throwable
  {
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/altKey?ids=List(Alt1,Alt2)&altkey=alt");
    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/altKey?ids=List(1,2)");

    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.DELETE).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.DELETE).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    Assert.assertNotNull(altKeyResponse.getEntity());
    String altKeyResult1 = getBatchResult(altKeyResponse, "Alt1");
    String altKeyResult2 = getBatchResult(altKeyResponse, "Alt2");

    Assert.assertNotNull(primaryKeyResponse.getEntity());
    String priKeyResult1 = getBatchResult(primaryKeyResponse, "1");
    String priKeyResult2 = getBatchResult(primaryKeyResponse, "2");

    Assert.assertEquals(altKeyResult1, priKeyResult1);
    Assert.assertEquals(altKeyResult2, priKeyResult2);
  }

  @Test
  public void testAltKeyAction() throws Throwable
  {
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/altKey/Alt1?action=getKeyValue&altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.POST).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/altKey/1?action=getKeyValue");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.POST).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    verifyResult(altKeyResponse, primaryKeyResponse);
  }

  @Test
  public void testAssociationAltKeyGet() throws Throwable
  {
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/associationAltKey/messageaxgreetingId1?altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.GET).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/associationAltKey/(greetingId:1,message:a)");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.GET).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    verifyResult(altKeyResponse, primaryKeyResponse);
  }

  @Test
  public void testAssociationAltKeyBatchGet() throws Throwable
  {
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/associationAltKey?ids=List(messageaxgreetingId1,messagebxgreetingId2)&altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.GET).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/associationAltKey?ids=List((greetingId:1,message:a),(greetingId:2,message:b))");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.GET).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    Assert.assertNotNull(altKeyResponse.getEntity());
    String altKeyResult1 = getBatchResult(altKeyResponse, "messageaxgreetingId1");
    String altKeyResult2 = getBatchResult(altKeyResponse, "messagebxgreetingId2");

    Assert.assertNotNull(primaryKeyResponse.getEntity());
    String priKeyResult1 = getBatchResult(primaryKeyResponse, "(greetingId:1,message:a)");
    String priKeyResult2 = getBatchResult(primaryKeyResponse, "(greetingId:2,message:b)");

    Assert.assertEquals(altKeyResult1, priKeyResult1);
    Assert.assertEquals(altKeyResult2, priKeyResult2);
  }

  @Test
  public void testAssociationAltKeyUpdate() throws Throwable
  {
    Greeting greeting = new Greeting().setMessage("Update Message");
    byte[] jsonByte = DataMapUtils.dataTemplateToBytes(greeting, true);
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/associationAltKey/messageaxgreetingId1?altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.PUT).setEntity(jsonByte).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/associationAltKey/(greetingId:1,message:a)");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.PUT).setEntity(jsonByte).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    verifyResult(altKeyResponse, primaryKeyResponse);
  }

  @Test
  public void testAssociationAltKeyPartialUpdate() throws Throwable
  {
    String json = "{ \n" + "  \"patch\" : { \n" + "    \"$set\" : { \n"
        + "      \"message\" : \"partial updated message\" \n" + "    } \n" + "  } \n" + "} ";
    byte[] jsonByte = json.getBytes();
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/associationAltKey/messageaxgreetingId1?altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.POST).setEntity(jsonByte).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/associationAltKey/(greetingId:1,message:a)");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.POST).setEntity(jsonByte).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    verifyResult(altKeyResponse, primaryKeyResponse);
  }

  @Test
  public void testAssociationAltKeyBatchUpdate() throws Throwable
  {
    String json1 = "{ \n" + "  \"entities\" : { \n" + "    \"messageaxgreetingId1\" : { \n"
        + "      \"greeting\" : { \n" + "        \"message\" : \"updated message\" \n" + "      } \n"
        +"   }, \n"
        + "    \"messagebxgreetingId2\" : { \n" + "      \"greeting\" : { \n"
        + "        \"message\" : \"another updated message\" \n" + "      }"
        + "    } \n" + "  } \n" + "} ";
    byte[] jsonByte1 = json1.getBytes();
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/associationAltKey?ids=List(messageaxgreetingId1,messagebxgreetingId2)&altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.PUT).setEntity(jsonByte1).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    String json2 = "{ \n" + "  \"entities\" : { \n" + "    \"(greetingId:1,message:a)\" : { \n"
        + "      \"greeting\" : { \n" + "        \"message\" : \"updated message\" \n" + "      } \n"
         + "    }, \n"
        + "    \"(greetingId:2,message:b)\" : { \n" + "      \"greeting\" : { \n"
        + "        \"message\" : \"another updated message\" \n" + "      } \n" + "    "
        + "    } \n" + "  } \n" + "} ";
    byte[] jsonByte2 = json2.getBytes();
    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/associationAltKey?ids=List((greetingId:1,message:a),(greetingId:2,message:b))");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.PUT).setEntity(jsonByte2).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    Assert.assertNotNull(altKeyResponse.getEntity());
    String altKeyResult1 = getBatchResult(altKeyResponse, "messageaxgreetingId1");
    String altKeyResult2 = getBatchResult(altKeyResponse, "messagebxgreetingId2");

    Assert.assertNotNull(primaryKeyResponse.getEntity());
    String priKeyResult1 = getBatchResult(primaryKeyResponse, "(greetingId:1,message:a)");
    String priKeyResult2 = getBatchResult(primaryKeyResponse, "(greetingId:2,message:b)");

    Assert.assertEquals(altKeyResult1, priKeyResult1);
    Assert.assertEquals(altKeyResult2, priKeyResult2);
  }

  @Test
  public void testAssociationAltKeyBatchPartialUpdate() throws Throwable
  {
    String json1 = "{ \n" + "  \"entities\" : { \n" + "    \"messageaxgreetingId1\" : { \n" + "      \"patch\" : { \n"
        + "        \"$set\" : { \n" + "          \"message\" : \"another partial message\" \n" + "        } \n"
        + "      } \n" + "    }, \n" + "    \"messagebxgreetingId2\" : { \n" + "      \"patch\" : { \n" + "        \"$set\" : { \n"
        + "          \"message\" : \"partial updated message\" \n" + "        } \n" + "      } \n" + "    } \n"
        + "  } \n" + "} ";
    byte[] jsonByte1 = json1.getBytes();
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/associationAltKey?ids=List(messageaxgreetingId1,messagebxgreetingId2)&altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.POST).setEntity(jsonByte1).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    String json2 = "{ \n" + "  \"entities\" : { \n" + "    \"(greetingId:1,message:a)\" : { \n" + "      \"patch\" : { \n"
        + "        \"$set\" : { \n" + "          \"message\" : \"another partial message\" \n" + "        } \n"
        + "      } \n" + "    }, \n" + "    \"(greetingId:2,message:b)\" : { \n" + "      \"patch\" : { \n" + "        \"$set\" : { \n"
        + "          \"message\" : \"partial updated message\" \n" + "        } \n" + "      } \n" + "    } \n"
        + "  } \n" + "} ";
    byte[] jsonByte2 = json2.getBytes();
    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/associationAltKey?ids=List((greetingId:1,message:a),(greetingId:2,message:b))");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.POST).setEntity(jsonByte2).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    Assert.assertNotNull(altKeyResponse.getEntity());
    String altKeyResult1 = getBatchResult(altKeyResponse, "messageaxgreetingId1");
    String altKeyResult2 = getBatchResult(altKeyResponse, "messagebxgreetingId2");

    Assert.assertNotNull(primaryKeyResponse.getEntity());
    String priKeyResult1 = getBatchResult(primaryKeyResponse, "(greetingId:1,message:a)");
    String priKeyResult2 = getBatchResult(primaryKeyResponse, "(greetingId:2,message:b)");

    Assert.assertEquals(altKeyResult1, priKeyResult1);
    Assert.assertEquals(altKeyResult2, priKeyResult2);
  }

  @Test
  public void testAssociationAltKeyDelete() throws Throwable
  {
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/associationAltKey/messageaxgreetingId1?altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.DELETE).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/associationAltKey/(greetingId:1,message:a)");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.DELETE).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    verifyResult(altKeyResponse, primaryKeyResponse);
  }

  @Test
  public void testAssociationAltKeyBatchDelete() throws Throwable
  {
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/associationAltKey?ids=List(messageaxgreetingId1,messagebxgreetingId2)&altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.DELETE).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/associationAltKey?ids=List((greetingId:1,message:a),(greetingId:2,message:b))");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.DELETE).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    Assert.assertNotNull(altKeyResponse.getEntity());
    String altKeyResult1 = getBatchResult(altKeyResponse, "messageaxgreetingId1");
    String altKeyResult2 = getBatchResult(altKeyResponse, "messagebxgreetingId2");

    Assert.assertNotNull(primaryKeyResponse.getEntity());
    String priKeyResult1 = getBatchResult(primaryKeyResponse, "(greetingId:1,message:a)");
    String priKeyResult2 = getBatchResult(primaryKeyResponse, "(greetingId:2,message:b)");

    Assert.assertEquals(altKeyResult1, priKeyResult1);
    Assert.assertEquals(altKeyResult2, priKeyResult2);
  }

  @Test
  public void testAssociationAltKeyAction() throws Throwable
  {
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/associationAltKey/messageaxgreetingId1?action=testAction&altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.POST).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/associationAltKey/(greetingId:1,message:a)?action=testAction");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.POST).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    verifyResult(altKeyResponse, primaryKeyResponse);
  }

  @Test
  public void testComplexKeyAltKeyGet() throws Throwable
  {
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/complexKeyAltKey/majorxKEY%201xminorxKEY%202?altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.GET).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/complexKeyAltKey/(major:KEY%201,minor:KEY%202)");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.GET).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    verifyResult(altKeyResponse, primaryKeyResponse);
  }

  @Test
  public void testComplexKeyAltKeyBatchGet() throws Throwable
  {
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/complexKeyAltKey?ids=List(majorxKEY%201xminorxKEY%202,majorxKEY%203xminorxKEY%204)&altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.GET).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/complexKeyAltKey?ids=List((major:KEY%201,minor:KEY%202),(major:KEY%203,minor:KEY%204))");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.GET).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    Assert.assertNotNull(altKeyResponse.getEntity());
    String altKeyResult1 = getBatchResult(altKeyResponse, "majorxKEY 1xminorxKEY 2");
    String altKeyResult2 = getBatchResult(altKeyResponse, "majorxKEY 3xminorxKEY 4");

    Assert.assertNotNull(primaryKeyResponse.getEntity());
    String priKeyResult1 = getBatchResult(primaryKeyResponse, "(major:KEY 1,minor:KEY 2)");
    String priKeyResult2 = getBatchResult(primaryKeyResponse, "(major:KEY 3,minor:KEY 4)");

    Assert.assertEquals(altKeyResult1, priKeyResult1);
    Assert.assertEquals(altKeyResult2, priKeyResult2);
  }

  @Test
  public void testComplexKeyAltKeyUpdate() throws Throwable
  {
    Message message = new Message().setMessage("Update Message");
    byte[] byteArray = DataMapUtils.dataTemplateToBytes(message, true);
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/complexKeyAltKey/majorxKEY%201xminorxKEY%202?altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.PUT).setEntity(byteArray).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/complexKeyAltKey/(major:KEY%201,minor:KEY%202)");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.PUT).setEntity(byteArray).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    verifyResult(altKeyResponse, primaryKeyResponse);
  }

  @Test
  public void testComplexKeyAltKeyPartialUpdate() throws Throwable
  {
    String json = "{ \n" + "  \"patch\" : { \n" + "    \"$set\" : { \n"
        + "      \"message\" : \"update messaage\" \n" + "    } \n" + "  } \n" + "} ";
    byte[] jsonByte = json.getBytes();
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/complexKeyAltKey/majorxKEY%201xminorxKEY%202?altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.POST).setEntity(jsonByte).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/complexKeyAltKey/(major:KEY%201,minor:KEY%202)");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.POST).setEntity(jsonByte).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    verifyResult(altKeyResponse, primaryKeyResponse);
  }

  @Test
  public void testComplexKeyAltKeyBatchUpdate() throws Throwable
  {
    String json1 = "{ \n" + "  \"entities\" : { \n" + "    \"majorxKEY 1xminorxKEY 2\" : { \n"
        + "      \"message\" : { \n" + "        \"message\" : \"another updated message\" \n" + "      }\n"
        + "    }, \n" + "    \"majorxKEY 3xminorxKEY 4\" : { \n" + "      \"message\" : { \n"
        + "        \"message\" : \"updated message\" \n" + "      } \n" + "    } \n" + "  } \n" + "}";
    byte[] jsonByte1 = json1.getBytes();
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/complexKeyAltKey?ids=List(majorxKEY%201xminorxKEY%202,majorxKEY%203xminorxKEY%204)&altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.PUT).setEntity(jsonByte1).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    String json2 = "{ \n" + "  \"entities\" : { \n" + "    \"(major:KEY%201,minor:KEY%202)\" : { \n"
        + "      \"message\" : { \n" + "        \"message\" : \"another updated message\" \n" + "      }\n"
        + "    }, \n" + "    \"(major:KEY%203,minor:KEY%204)\" : { \n" + "      \"message\" : { \n"
        + "        \"message\" : \"updated message\" \n" + "      } \n" + "    } \n" + "  } \n" + "}";
    byte[] jsonByte2 = json2.getBytes();
    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/complexKeyAltKey?ids=List((major:KEY%201,minor:KEY%202),(major:KEY%203,minor:KEY%204))");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.PUT).setEntity(jsonByte2).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    Assert.assertNotNull(altKeyResponse.getEntity());
    String altKeyResult1 = getBatchResult(altKeyResponse, "majorxKEY 1xminorxKEY 2");
    String altKeyResult2 = getBatchResult(altKeyResponse, "majorxKEY 3xminorxKEY 4");

    Assert.assertNotNull(primaryKeyResponse.getEntity());
    String priKeyResult1 = getBatchResult(primaryKeyResponse, "(major:KEY 1,minor:KEY 2)");
    String priKeyResult2 = getBatchResult(primaryKeyResponse, "(major:KEY 3,minor:KEY 4)");

    Assert.assertEquals(altKeyResult1, priKeyResult1);
    Assert.assertEquals(altKeyResult2, priKeyResult2);
  }

  @Test
  public void testComplexKeyAltKeyBatchPartialUpdate() throws Throwable
  {
    String json1 = "{ \n" + "  \"entities\" : { \n" + "    \"majorxKEY 1xminorxKEY 2\" : { \n" + "      \"patch\" : { \n"
        + "        \"$set\" : { \n" + "          \"message\" : \"another partial message\" \n" + "        } \n"
        + "      } \n" + "    }, \n" + "    \"majorxKEY 3xminorxKEY 4\" : { \n" + "      \"patch\" : { \n" + "        \"$set\" : { \n"
        + "          \"message\" : \"partial updated message\" \n" + "        } \n" + "      } \n" + "    } \n"
        + "  } \n" + "} ";
    byte[] jsonByte1 = json1.getBytes();
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/complexKeyAltKey?ids=List(majorxKEY%201xminorxKEY%202,majorxKEY%203xminorxKEY%204)&altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.POST).setEntity(jsonByte1).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    String json2 = "{ \n" + "  \"entities\" : { \n" + "    \"(major:KEY%201,minor:KEY%202)\" : { \n" + "      \"patch\" : { \n"
        + "        \"$set\" : { \n" + "          \"message\" : \"another partial message\" \n" + "        } \n"
        + "      } \n" + "    }, \n" + "    \"(major:KEY%203,minor:KEY%204)\" : { \n" + "      \"patch\" : { \n" + "        \"$set\" : { \n"
        + "          \"message\" : \"partial updated message\" \n" + "        } \n" + "      } \n" + "    } \n"
        + "  } \n" + "} ";
    byte[] jsonByte2 = json2.getBytes();
    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/complexKeyAltKey?ids=List((major:KEY%201,minor:KEY%202),(major:KEY%203,minor:KEY%204))");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.POST).setEntity(jsonByte2).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    Assert.assertNotNull(altKeyResponse.getEntity());
    String altKeyResult1 = getBatchResult(altKeyResponse, "majorxKEY 1xminorxKEY 2");
    String altKeyResult2 = getBatchResult(altKeyResponse, "majorxKEY 3xminorxKEY 4");

    Assert.assertNotNull(primaryKeyResponse.getEntity());
    String priKeyResult1 = getBatchResult(primaryKeyResponse, "(major:KEY 1,minor:KEY 2)");
    String priKeyResult2 = getBatchResult(primaryKeyResponse, "(major:KEY 3,minor:KEY 4)");

    Assert.assertEquals(altKeyResult1, priKeyResult1);
    Assert.assertEquals(altKeyResult2, priKeyResult2);
  }

  @Test
  public void testComplexKeyAltKeyDelete() throws Throwable
  {
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/complexKeyAltKey/majorxKEY%201xminorxKEY%202?altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.DELETE).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/complexKeyAltKey/(major:KEY%201,minor:KEY%202)");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.DELETE).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    verifyResult(altKeyResponse, primaryKeyResponse);
  }

  @Test
  public void testComplexKeyAltKeyBatchDelete() throws Throwable
  {
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/complexKeyAltKey?ids=List(majorxKEY%201xminorxKEY%202,majorxKEY%203xminorxKEY%204)&altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.DELETE).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/complexKeyAltKey?ids=List((major:KEY%201,minor:KEY%202),(major:KEY%203,minor:KEY%204))");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.DELETE).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    Assert.assertNotNull(altKeyResponse.getEntity());
    String altKeyResult1 = getBatchResult(altKeyResponse, "majorxKEY 1xminorxKEY 2");
    String altKeyResult2 = getBatchResult(altKeyResponse, "majorxKEY 3xminorxKEY 4");

    Assert.assertNotNull(primaryKeyResponse.getEntity());
    String priKeyResult1 = getBatchResult(primaryKeyResponse, "(major:KEY 1,minor:KEY 2)");
    String priKeyResult2 = getBatchResult(primaryKeyResponse, "(major:KEY 3,minor:KEY 4)");

    Assert.assertEquals(altKeyResult1, priKeyResult1);
    Assert.assertEquals(altKeyResult2, priKeyResult2);
  }

  @Test
  public void testComplexAltKeyAction() throws Throwable
  {
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/complexKeyAltKey/majorxKEY%201xminorxKEY%202?action=testAction&altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.POST).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/complexKeyAltKey/(major:KEY%201,minor:KEY%202)?action=testAction");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.POST).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    verifyResult(altKeyResponse, primaryKeyResponse);
  }

  @Test
  public void testAltKeySubGet() throws Throwable
  {
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/altKey/Alt1/altKeySub/urn:li:message:1?altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.GET).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/altKey/1/altKeySub/1");
    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.GET).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    verifyResult(altKeyResponse, primaryKeyResponse);
  }

  @Test
  public void testAltKeySubBatchGet() throws Throwable
  {
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/altKey/Alt1/altKeySub?ids=List(urn%3Ali%3Amessage%3A1,urn%3Ali%3Amessage%3A2)&altkey=alt");
    URI primaryKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/altKey/1/altKeySub?ids=List(1,2)");

    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.GET).build();
    RestResponse altKeyResponse = client.restRequest(altKeyRequest).get();

    RestRequest primaryKeyRequest = new RestRequestBuilder(primaryKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setMethod(RestMethod.GET).build();
    RestResponse primaryKeyResponse = client.restRequest(primaryKeyRequest).get();

    Assert.assertNotNull(altKeyResponse.getEntity());
    String altKeyResult1 = getBatchResult(altKeyResponse, "urn:li:message:1");
    String altKeyResult2 = getBatchResult(altKeyResponse, "urn:li:message:2");

    Assert.assertNotNull(primaryKeyResponse.getEntity());
    String priKeyResult1 = getBatchResult(primaryKeyResponse, "1");
    String priKeyResult2 = getBatchResult(primaryKeyResponse, "2");

    Assert.assertEquals(altKeyResult1, priKeyResult1);
    Assert.assertEquals(altKeyResult2, priKeyResult2);
  }

  @Test
  public void testAltKeyCreate() throws Throwable
  {
    StringLongCoercer coercer = new StringLongCoercer();
    Long key = 3L;
    String altKey = coercer.coerceFromKey(key);

    Greeting greeting = new Greeting().setId(key).setMessage("message");
    byte[] byteArray = DataMapUtils.dataTemplateToBytes(greeting, true);

    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/altKey?altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setEntity(byteArray).setMethod(RestMethod.POST).build();
    RestResponse response  = client.restRequest(altKeyRequest).get();

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getStatus(), 201);
    Assert.assertEquals(response.getHeader(HEADER_RESTLI_ID), altKey);
  }

  @Test
  public void testAssociationAltKeyCreate() throws Throwable
  {
    // In AssociationAltKeyResource create method, we predefine a CompoundKey,
    // here we coerce that key to Alternative Key,
    // which will be used as expected key which will be returned by response.
    StringCompoundKeyCoercer coercer = new StringCompoundKeyCoercer();
    CompoundKey key = new CompoundKey();
    key.append("message", "h");
    key.append("greetingId", 3L);
    String altKey = coercer.coerceFromKey(key);

    Greeting greeting = new Greeting().setMessage("message");
    byte[] byteArray = DataMapUtils.dataTemplateToBytes(greeting, true);

    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/associationAltKey?altkey=alt");
    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setEntity(byteArray).setMethod(RestMethod.POST).build();
    RestResponse response  = client.restRequest(altKeyRequest).get();

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getStatus(), 201);
    Assert.assertEquals(response.getHeader(HEADER_RESTLI_ID), altKey);
  }

  @Test
  public void testComplexKeyAltKeyCreate() throws Throwable
  {
    // In ComplexKeyAltKeyResource create method, we predefine a ComplexKey,
    // here we coerce that key to Alternative Key,
    // which will be used as expected key which will be returned by response.
    StringComplexKeyCoercer coercer = new StringComplexKeyCoercer();
    TwoPartKey key = new TwoPartKey();
    key.setMajor("testKey");
    key.setMinor("testKey");
    ComplexResourceKey<TwoPartKey, TwoPartKey> complexKey = new ComplexResourceKey<TwoPartKey, TwoPartKey>(key, new TwoPartKey());
    String altKey = coercer.coerceFromKey(complexKey);

    Message message = new Message().setMessage("message");
    byte[] byteArray = DataMapUtils.dataTemplateToBytes(message, true);
    URI altKeyUri = URI.create(URI_PREFIX + RestLiIntTestServer.DEFAULT_PORT + "/complexKeyAltKey?altkey=alt");

    RestRequest altKeyRequest = new RestRequestBuilder(altKeyUri).setHeader(HEADER_RESTLI_PROTOCOL_VERSION,
        PROTOCOL_VERSION_2).setEntity(byteArray).setMethod(RestMethod.POST).build();
    RestResponse response  = client.restRequest(altKeyRequest).get();

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getStatus(), 201);
    Assert.assertEquals(response.getHeader(HEADER_RESTLI_ID), altKey);
  }

  private String getBatchResult(RestResponse restResponse, String keyName)
  {
    InputStream input = restResponse.getEntity().asInputStream();
    DataMap map = DataMapUtils.readMap(input);
    DataMap entities = (DataMap) map.get("results");
    return entities.get(keyName).toString();
  }

  private void verifyResult(RestResponse altKeyResponse, RestResponse primaryKeyResponse)
  {
    Assert.assertNotNull(altKeyResponse.getEntity());
    Assert.assertNotNull(primaryKeyResponse.getEntity());
    Assert.assertEquals(altKeyResponse.getStatus(), primaryKeyResponse.getStatus());
    Assert.assertEquals(altKeyResponse.getEntity(), primaryKeyResponse.getEntity());
  }
}