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

package com.linkedin.restli.examples;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.StringArray;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.MessageArray;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.api.ToneArray;
import com.linkedin.restli.examples.greetings.client.ActionsBuilders;
import com.linkedin.restli.examples.greetings.client.ActionsRequestBuilders;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestActionsResource extends RestLiIntegrationTest
{
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

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testPrimitiveReturningActions(RootBuilderWrapper<?, ?> builders) throws RemoteInvocationException
  {
    Request<Integer> intRequest = builders.<Integer>action("ReturnInt").build();
    Integer integer = getClient().sendRequest(intRequest).getResponse().getEntity();
    Assert.assertEquals(0, integer.intValue());

    Request<Boolean> boolRequest = builders.<Boolean>action("ReturnBool").build();
    Boolean bool = getClient().sendRequest(boolRequest).getResponse().getEntity();
    Assert.assertTrue(bool);
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testActionsSet(RootBuilderWrapper<?, ?> builders) throws RemoteInvocationException
  {
    Request<Integer> request = builders.<Integer>action("UltimateAnswer").build();
    Integer answer = getClient().sendRequest(request).getResponse().getEntity();
    Assert.assertEquals(answer, Integer.valueOf(42));
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testActionNamedGet(RootBuilderWrapper<?, ?> builders) throws RemoteInvocationException
  {
    Request<String> request = builders.<String>action("Get").build();
    String value = getClient().sendRequest(request).getResponse().getEntity();
    Assert.assertEquals(value, "Hello, World");
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testArrayTypesOnActions(RootBuilderWrapper<?, ?> builders) throws RemoteInvocationException
  {
    //Record template array
    MessageArray inputMessageArray = new MessageArray();
    inputMessageArray.add(new Message().setId("My Message Id").setMessage("My Message"));
    inputMessageArray.add(new Message().setId("My Message Id 2").setMessage("My Message 2"));
    Request<MessageArray> messageArrayRequest =
        builders.<MessageArray>action("EchoMessageArray").setActionParam("Messages", inputMessageArray).build();
    MessageArray messageArray = getClient().sendRequest(messageArrayRequest).getResponse().getEntity();

    Assert.assertEquals(messageArray.get(0).getId(), "My Message Id");
    Assert.assertEquals(messageArray.get(0).getMessage(), "My Message");

    Assert.assertEquals(messageArray.get(1).getId(), "My Message Id 2");
    Assert.assertEquals(messageArray.get(1).getMessage(), "My Message 2");

    //Primitive type array
    StringArray inputStringArray = new StringArray();
    inputStringArray.add("message1");
    inputStringArray.add("message2");
    Request<StringArray> stringArrayRequest =
        builders.<StringArray>action("EchoStringArray").setActionParam("Strings", inputStringArray).build();
    StringArray stringArray = getClient().sendRequest(stringArrayRequest).getResponse().getEntity();

    Assert.assertEquals(stringArray.get(0), "message1");
    Assert.assertEquals(stringArray.get(1), "message2");

    //Enum array
    ToneArray inputTonesArray = new ToneArray();
    inputTonesArray.add(Tone.SINCERE);
    inputTonesArray.add(Tone.FRIENDLY);

    Request<ToneArray> toneArrayRequest =
        builders.<ToneArray>action("EchoToneArray").setActionParam("Tones", inputTonesArray).build();
    ToneArray tones = getClient().sendRequest(toneArrayRequest).getResponse().getEntity();

    Assert.assertEquals(tones.get(0), Tone.SINCERE);
    Assert.assertEquals(tones.get(1), Tone.FRIENDLY);
  }

  // Not implemented until we switch back to using the "useContinuation" path by default
  // in the AbstractR2Servlet.
  @Test(groups = TestConstants.TESTNG_GROUP_NOT_IMPLEMENTED,
        dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testServerTimeout(RootBuilderWrapper<?, ?> builders) throws RemoteInvocationException
  {
    Request<Void> request = builders.<Void>action("Timeout").build();
    try
    {
      getClient().sendRequest(request).getResponse();
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
      Assert.assertTrue(e.getResponse().toString().contains("Server Timeout"));
    }
  }

  @Test(expectedExceptions = RestLiResponseException.class,
        dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testFailPromiseCall(RootBuilderWrapper<?, ?> builders) throws RemoteInvocationException
  {
    // this version gives a Task that RestLi runs
    Request<Void> req = builders.<Void>action("FailPromiseCall").build();
    getClient().sendRequest(req).getResponse();
  }

  @Test(expectedExceptions = RestLiResponseException.class,
        dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testFailPromiseThrow(RootBuilderWrapper<?, ?> builders) throws RemoteInvocationException
  {
    // this version gives a Task that RestLi runs
    Request<Void> req = builders.<Void>action("FailPromiseThrow").build();
    getClient().sendRequest(req).getResponse();
  }

  @Test(expectedExceptions = RestLiResponseException.class,
        dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testFailTaskCall(RootBuilderWrapper<?, ?> builders) throws RemoteInvocationException
  {
    // this version gives a Task that RestLi runs
    Request<Void> req = builders.<Void>action("FailTaskCall").build();
    getClient().sendRequest(req).getResponse();
  }

  @Test(expectedExceptions = RestLiResponseException.class,
        dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testFailTaskThrow(RootBuilderWrapper<?, ?> builders) throws RemoteInvocationException
  {
    // this version gives a Task that RestLi runs
    Request<Void> req = builders.<Void>action("FailTaskThrow").build();
    getClient().sendRequest(req).getResponse();
  }

  @Test(expectedExceptions = RestLiResponseException.class,
        dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testFailThrowInTask(RootBuilderWrapper<?, ?> builders) throws RemoteInvocationException
  {
    // this version gives a Task that RestLi runs
    Request<Void> req = builders.<Void>action("FailThrowInTask").build();
    getClient().sendRequest(req).getResponse();
  }

  @Test(expectedExceptions = RestLiResponseException.class,
        dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testNullPromise(RootBuilderWrapper<?, ?> builders) throws RemoteInvocationException
  {
    // this version gives a Task that RestLi runs
    Request<String> req = builders.<String>action("NullPromise").build();
    getClient().sendRequest(req).getResponse();
  }

  @Test(expectedExceptions = RestLiResponseException.class,
        dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testNullTask(RootBuilderWrapper<?, ?> builders) throws RemoteInvocationException
  {
    // this version gives a Task that RestLi runs
    Request<String> req = builders.<String>action("NullTask").build();
    getClient().sendRequest(req).getResponse();
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProviderForParseqActions")
  public void testBasicParseqBasedAction(RootBuilderWrapper<?, ?> builders, String actionName) throws RemoteInvocationException
  {
    // this version gives a Task that RestLi runs
    Request<String> req = builders.<String>action(actionName).
        setActionParam("a", 2).
        setActionParam("b", "abc").
        setActionParam("c", true).
        build();
    String result = getClient().sendRequest(req).getResponse().getEntity();

    Assert.assertEquals(result, "10 ABC true");
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProviderForParseqActions")
  private static Object[][] requestBuilderDataProviderForParseqActions()
  {
    return new Object[][] {
        { new RootBuilderWrapper<Object, RecordTemplate>(new ActionsBuilders()), "parseq" },
        //  This test cannot be compiled until we build with Java 8 by default.
        //{ new RootBuilderWrapper<Object, RecordTemplate>(new ActionsBuilders()), "parseq2" },
        { new RootBuilderWrapper<Object, RecordTemplate>(new ActionsBuilders()), "parseq3" },
        { new RootBuilderWrapper<Object, RecordTemplate>(new ActionsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)), "parseq" },
        //  This test cannot be compiled until we build with Java 8 by default.
        //{ new RootBuilderWrapper<Object, RecordTemplate>(new ActionsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)), "parseq2" },
        { new RootBuilderWrapper<Object, RecordTemplate>(new ActionsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)), "parseq3" },
        { new RootBuilderWrapper<Object, RecordTemplate>(new ActionsRequestBuilders()), "parseq" },
        //  This test cannot be compiled until we build with Java 8 by default.
        //{ new RootBuilderWrapper<Object, RecordTemplate>(new ActionsRequestBuilders()), "parseq2" },
        { new RootBuilderWrapper<Object, RecordTemplate>(new ActionsRequestBuilders()), "parseq3" },
        { new RootBuilderWrapper<Object, RecordTemplate>(new ActionsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)), "parseq" },
        //  This test cannot be compiled until we build with Java 8 by default.
        //{ new RootBuilderWrapper<Object, RecordTemplate>(new ActionsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)), "parseq2" },
        { new RootBuilderWrapper<Object, RecordTemplate>(new ActionsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)), "parseq3" }
    };
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  private static Object[][] requestBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<Object, RecordTemplate>(new ActionsBuilders()) },
      { new RootBuilderWrapper<Object, RecordTemplate>(new ActionsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<Object, RecordTemplate>(new ActionsRequestBuilders()) },
      { new RootBuilderWrapper<Object, RecordTemplate>(new ActionsRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }
    };
  }
}
