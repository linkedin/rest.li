/*
   Copyright (c) 2013 LinkedIn Corp.

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


import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.ActionRequest;
import com.linkedin.restli.client.BatchGetKVRequest;
import com.linkedin.restli.client.FindRequest;
import com.linkedin.restli.client.GetRequest;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.client.AssociationsBatchGetBuilder;
import com.linkedin.restli.examples.greetings.client.AssociationsBatchPartialUpdateBuilder;
import com.linkedin.restli.examples.greetings.client.AssociationsBatchUpdateBuilder;
import com.linkedin.restli.examples.greetings.client.AssociationsBuilders;
import com.linkedin.restli.examples.greetings.client.AssociationsFindByAssocKeyFinderBuilder;
import com.linkedin.restli.examples.greetings.client.AssociationsFindByAssocKeyFinderOptBuilder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.linkedin.restli.examples.AssociationResourceHelpers.DB;
import static com.linkedin.restli.examples.AssociationResourceHelpers.SIMPLE_COMPOUND_KEY;
import static com.linkedin.restli.examples.AssociationResourceHelpers.URL_COMPOUND_KEY;
import com.linkedin.restli.examples.greetings.client.AssociationsSubBuilders;


public class TestAssociationsResource extends RestLiIntegrationTest
{
  private static final Client            CLIENT      = new TransportClientAdapter(new HttpClientFactory().getClient(Collections.<String, String> emptyMap()));
  private static final String            URI_PREFIX  = "http://localhost:1338/";
  private static final RestClient        REST_CLIENT = new RestClient(CLIENT, URI_PREFIX);

  @BeforeClass
  public void initClass() throws Exception
  {
    super.init();
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  @Test
  public void testOptionalAssociationKeyInFinder() throws Exception
  {
    // optional and present
    AssociationsFindByAssocKeyFinderOptBuilder finder = new AssociationsBuilders().findByAssocKeyFinderOpt().assocKey("src", "KEY1");
    Assert.assertEquals(200, REST_CLIENT.sendRequest(finder).getResponse().getStatus());

    // optional and not present
    AssociationsFindByAssocKeyFinderOptBuilder finderNoAssocKey = new AssociationsBuilders().findByAssocKeyFinderOpt();
    Assert.assertEquals(200, REST_CLIENT.sendRequest(finderNoAssocKey).getResponse().getStatus());
  }

  @Test
  public void testRequiredAssociationKeyInFinder() throws Exception
  {
    // required and present
    AssociationsFindByAssocKeyFinderBuilder finder = new AssociationsBuilders().findByAssocKeyFinder().assocKey("src", "KEY1");
    Assert.assertEquals(200, REST_CLIENT.sendRequest(finder).getResponse().getStatus());

    // required and not present
    AssociationsFindByAssocKeyFinderBuilder finderNoAssocKey = new AssociationsBuilders().findByAssocKeyFinder();
    try
    {
      REST_CLIENT.sendRequest(finderNoAssocKey).getResponse();
      Assert.fail("Calling a finder without a required association key should throw RestLiResponseException with status code 400");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(400, e.getStatus());
    }
  }

  @Test
  public void testBatchGet()
      throws RemoteInvocationException
  {
    AssociationsBatchGetBuilder builder = new AssociationsBuilders().batchGet();
    builder.ids(DB.keySet());

    BatchGetKVRequest<CompoundKey, Message> request = builder.buildKV();
    ResponseFuture<BatchKVResponse<CompoundKey, Message>> responseFuture = REST_CLIENT.sendRequest(request);
    Response<BatchKVResponse<CompoundKey, Message>> response = responseFuture.getResponse();

    BatchKVResponse<CompoundKey, Message> entity = response.getEntity();

    Assert.assertEquals(entity.getErrors().size(), 0);
    Assert.assertEquals(entity.getResults().size(), 2);
    for (CompoundKey id: DB.keySet())
    {
      Assert.assertTrue(entity.getResults().containsKey(id));
      Assert.assertEquals(entity.getResults().get(id), DB.get(id));
    }
  }

  @Test
  public void testSubresourceGet() throws RemoteInvocationException
  {
    GetRequest<Message> request = new AssociationsSubBuilders().get().destKey("dest").srcKey("src").id("id").build();
    Message message = REST_CLIENT.sendRequest(request).getResponse().getEntity();
    Assert.assertEquals(message.getId(), "src");
    Assert.assertEquals(message.getMessage(), "dest");
  }

  @Test
  public void testSubresourceGetForbiddenCharacters() throws RemoteInvocationException
  {
    GetRequest<Message> request = new AssociationsSubBuilders().get().destKey("d&est").srcKey("s&rc").id("id").build();
    Message message = REST_CLIENT.sendRequest(request).getResponse().getEntity();
    Assert.assertEquals(message.getId(), "s&rc");
    Assert.assertEquals(message.getMessage(), "d&est");
  }

  @Test
  public void testSubresourceFinder() throws RemoteInvocationException
  {
    FindRequest<Message> request = new AssociationsSubBuilders().findByTone().destKey("dest").srcKey("src").toneParam(Tone.FRIENDLY).build();
    List<Message> messages = REST_CLIENT.sendRequest(request).getResponse().getEntity().getElements();

    for (Message message : messages)
    {
      Assert.assertEquals(message.getTone(), Tone.FRIENDLY);
    }
  }

  @Test
  public void testSubresourceAction() throws RemoteInvocationException
  {
    ActionRequest<Integer> request = new AssociationsSubBuilders().actionAction().destKey("dest").srcKey("src").build();
    Integer integer = REST_CLIENT.sendRequest(request).getResponse().getEntity();

    Assert.assertEquals(integer, new Integer(1));
  }

  @Test
  public void testBatchUpdate()
      throws RemoteInvocationException
  {
    AssociationsBatchUpdateBuilder updateBuilder = new AssociationsBuilders().batchUpdate();

    BatchKVResponse<CompoundKey,UpdateStatus> entities =
        REST_CLIENT.sendRequest(updateBuilder.inputs(DB).build()).getResponse().getEntity();
    runAssertions(entities);
  }

  @Test
  public void testBatchPartialUpdate()
      throws RemoteInvocationException
  {
    AssociationsBatchPartialUpdateBuilder patchBuilder = new AssociationsBuilders().batchPartialUpdate();

    Map<CompoundKey, PatchRequest<Message>> patches = new HashMap<CompoundKey, PatchRequest<Message>>();
    patches.put(URL_COMPOUND_KEY, new PatchRequest<Message>());
    patches.put(SIMPLE_COMPOUND_KEY, new PatchRequest<Message>());

    BatchKVResponse<CompoundKey, UpdateStatus> entities =
        REST_CLIENT.sendRequest(patchBuilder.inputs(patches).build()).getResponse().getEntity();
    runAssertions(entities);
  }

  private void runAssertions(BatchKVResponse<CompoundKey, UpdateStatus> entities)
  {
    Assert.assertEquals(entities.getErrors().size(), 0);
    Assert.assertEquals(entities.getResults().keySet(), DB.keySet());
  }
}
