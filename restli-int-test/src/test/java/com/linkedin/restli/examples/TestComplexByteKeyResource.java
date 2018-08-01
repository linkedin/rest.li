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


import com.linkedin.data.ByteString;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.examples.greetings.api.TwoPartKey;
import com.linkedin.restli.examples.greetings.client.ComplexByteKeysBuilders;
import com.linkedin.restli.examples.greetings.client.ComplexByteKeysRequestBuilders;
import com.linkedin.restli.examples.typeref.api.TyperefRecord;
import com.linkedin.restli.test.util.RootBuilderWrapper;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestComplexByteKeyResource extends RestLiIntegrationTest
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
  public void testGet(RootBuilderWrapper<ComplexResourceKey<TyperefRecord, TwoPartKey>, TyperefRecord> builders) throws RemoteInvocationException
  {
    testGetMain(builders.get());
  }

  private void testGetMain(RootBuilderWrapper.MethodBuilderWrapper<ComplexResourceKey<TyperefRecord, TwoPartKey>, TyperefRecord, TyperefRecord> requestBuilder) throws RemoteInvocationException
  {
    final ByteString byteData = ByteString.copy(new byte[] {0, 32, -95});
    Request<TyperefRecord> request = requestBuilder.id(getComplexKey(byteData)).build();
    ResponseFuture<TyperefRecord> future = getClient().sendRequest(request);
    Response<TyperefRecord> response = future.getResponse();

    Assert.assertEquals(response.getEntity().getBytes(), byteData);
  }

  private static ComplexResourceKey<TyperefRecord, TwoPartKey> getComplexKey(ByteString bytes)
  {
    return new ComplexResourceKey<TyperefRecord, TwoPartKey>(
        new TyperefRecord().setBytes(bytes),
        new TwoPartKey());
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  private static Object[][] requestBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<ComplexResourceKey<TyperefRecord, TwoPartKey>, TyperefRecord>(new ComplexByteKeysBuilders()) },
      { new RootBuilderWrapper<ComplexResourceKey<TyperefRecord, TwoPartKey>, TyperefRecord>(new ComplexByteKeysBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) },
      { new RootBuilderWrapper<ComplexResourceKey<TyperefRecord, TwoPartKey>, TyperefRecord>(new ComplexByteKeysRequestBuilders()) },
      { new RootBuilderWrapper<ComplexResourceKey<TyperefRecord, TwoPartKey>, TyperefRecord>(new ComplexByteKeysRequestBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS)) }
    };
  }
}