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

package com.linkedin.restli.examples;

import com.linkedin.data.schema.PathSpec;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.GetRequest;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.common.BatchCollectionResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.examples.greetings.api.ValidateEmptyUnion;
import com.linkedin.restli.examples.greetings.api.ValidationDemo;
import com.linkedin.restli.examples.greetings.api.ValidationDemoCriteria;
import com.linkedin.restli.examples.greetings.client.EmptyUnionRequestBuilders;
import com.linkedin.restli.examples.greetings.client.GreetingsBuilders;
import com.linkedin.restli.examples.greetings.client.ValidationDemosRequestBuilders;
import com.linkedin.restli.server.validation.RestLiValidationFilter;
import com.linkedin.restli.test.util.RootBuilderWrapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestEmptyUnionValidation extends RestLiIntegrationTest
{
  @BeforeClass
  public void initClass() throws Exception
  {
    super.init(Collections.singletonList(new RestLiValidationFilter()));
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  @Test
  public void testUnionEmptyWithProjection() throws RemoteInvocationException
  {
    ValidateEmptyUnion expected = new ValidateEmptyUnion();
    expected.setFoo(new ValidateEmptyUnion.Foo());
    List<PathSpec> spec = Collections.singletonList(ValidateEmptyUnion.fields().foo().Fuzz());
    EmptyUnionRequestBuilders requestBuilders = new EmptyUnionRequestBuilders();
    GetRequest<ValidateEmptyUnion> req = requestBuilders.get().id(1L).fields(spec.toArray(new PathSpec[spec.size()])).build();
    ValidateEmptyUnion actual = getClient().sendRequest(req).getResponse().getEntity();
    Assert.assertEquals(actual, expected);
  }

  @Test(expectedExceptions = RestLiResponseException.class)
  public void testUnionEmptyWithoutProjection() throws RemoteInvocationException {
    String expectedSuffix = "projection";
    EmptyUnionRequestBuilders requestBuilders = new EmptyUnionRequestBuilders();
    GetRequest<ValidateEmptyUnion> req = requestBuilders.get().id(1L).build();
    ValidateEmptyUnion res = getClient().sendRequest(req).getResponse().getEntity();
  }
}
