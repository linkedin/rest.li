/*
   Copyright (c) 2015 LinkedIn Corp.

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


import java.util.Arrays;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.transform.DataProcessingException;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.common.BatchCreateIdEntityResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.IdEntityResponse;
import com.linkedin.restli.examples.greetings.api.ValidationDemo;
import com.linkedin.restli.examples.greetings.api.myEnum;
import com.linkedin.restli.examples.greetings.client.AutoValidationWithProjectionBuilders;
import com.linkedin.restli.server.validation.RestLiValidationFilter;
import com.linkedin.restli.test.util.RootBuilderWrapper;


/**
 * Integration tests for Rest.li data validation with conjunction of rest.li projection.
 *
 * @author jnchen
 */
public class TestResLiValidationWithProjection extends RestLiIntegrationTest
{
  private RestClient _restClientAuto;

  @BeforeClass
  public void initClass() throws Exception
  {
    super.init();
    super.init(Arrays.asList(new RestLiValidationFilter()));
    _restClientAuto = getClient();
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  @Test
  public void testNoProjection() throws RemoteInvocationException
  {
    RootBuilderWrapper<Integer, ValidationDemo> wrapper = new RootBuilderWrapper<Integer, ValidationDemo>(new AutoValidationWithProjectionBuilders());
    Request<CollectionResponse<ValidationDemo>> request = wrapper.findBy("searchWithProjection").build();

    try {
      _restClientAuto.sendRequest(request).getResponse();
    } catch (RestLiResponseException e) {
      Assert.assertEquals(e.getServiceErrorMessage(),
                          "ERROR :: /validationDemoNext/UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo1 :: field is required but not found and has no default value\n" +
                              "ERROR :: /validationDemoNext/stringA :: length of \"invalid, length is larger than the max\" is out of range 1...10\n" +
                              "ERROR :: /validationDemoNext/ArrayWithInlineRecord/0/bar2 :: field is required but not found and has no default value\n" +
                              "ERROR :: /validationDemoNext/MapWithTyperefs/foo/message :: field is required but not found and has no default value\n" +
                              "ERROR :: /validationDemoNext/MapWithTyperefs/foo/tone :: field is required but not found and has no default value\n" +
                              "ERROR :: /validationDemoNext/stringB :: field is required but not found and has no default value\n" +
                              "ERROR :: /includedA :: length of \"invalid, length is larger than the max\" is out of range 1...10\n" +
                              "ERROR :: /UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo1 :: field is required but not found and has no default value\n" +
                              "ERROR :: /ArrayWithInlineRecord/0/bar2 :: field is required but not found and has no default value\n" +
                              "ERROR :: /MapWithTyperefs/foo/message :: field is required but not found and has no default value\n" +
                              "ERROR :: /MapWithTyperefs/foo/tone :: field is required but not found and has no default value\n" +
                              "ERROR :: /stringA :: field is required but not found and has no default value\n");
    }
  }

  @DataProvider
  private Object[][] provideProjectionWithValidFieldsBuilders() throws DataProcessingException
  {
    List<PathSpec> spec = Arrays.asList(ValidationDemo.fields().stringB(),
                                        ValidationDemo.fields().includedB(),
                                        ValidationDemo.fields().UnionFieldWithInlineRecord().MyRecord().foo2(),
                                        ValidationDemo.fields().ArrayWithInlineRecord().items().bar1(),
                                        ValidationDemo.fields().MapWithTyperefs().values().id(),
                                        ValidationDemo.fields().validationDemoNext().intB());

    RootBuilderWrapper<Integer, ValidationDemo> wrapper =
        new RootBuilderWrapper<Integer, ValidationDemo>(new AutoValidationWithProjectionBuilders());

    Request<CollectionResponse<ValidationDemo>> findRequest =
        wrapper.findBy("searchWithProjection").fields(spec.toArray(new PathSpec[spec.size()])).build();

    Request<ValidationDemo> getRequest =
        wrapper.get().id(1).fields(spec.toArray(new PathSpec[spec.size()])).build();

    Request<CollectionResponse<ValidationDemo>> getAllRequest =
        wrapper.getAll().fields(spec.toArray(new PathSpec[spec.size()])).build();

    // Valid input for CreateAndGet
    ValidationDemo.UnionFieldWithInlineRecord unionField = new ValidationDemo.UnionFieldWithInlineRecord();
    unionField.setMyEnum(myEnum.FOOFOO);
    ValidationDemo validDemo = new ValidationDemo().setStringB("b").setUnionFieldWithInlineRecord(unionField);

    Request<IdEntityResponse<Integer, ValidationDemo>> createAndGetRequest =
        wrapper.createAndGet().input(validDemo).fields(spec.toArray(new PathSpec[spec.size()])).build();

    Request<BatchCreateIdEntityResponse<Integer, ValidationDemo>> batchCreateAndGetRequest =
        wrapper.batchCreateAndGet().inputs(Arrays.asList(validDemo)).fields(spec.toArray(new PathSpec[spec.size()])).build();

    return new Object[][] {
        { findRequest, HttpStatus.S_200_OK },
        { getRequest, HttpStatus.S_200_OK },
        { getAllRequest, HttpStatus.S_200_OK },
        { createAndGetRequest, HttpStatus.S_201_CREATED },
        { batchCreateAndGetRequest, HttpStatus.S_200_OK }
    };
  }

  @Test(dataProvider = "provideProjectionWithValidFieldsBuilders")
  public void testProjectionWithValidFields(Request<?> request, HttpStatus expectedStatus) throws RemoteInvocationException
  {
    Response<?> response = _restClientAuto.sendRequest(request).getResponse();
    Assert.assertEquals(response.getStatus(), expectedStatus.getCode());
  }

  @Test
  public void testProjectionWithInvalidFields() throws RemoteInvocationException
  {
    RootBuilderWrapper<Integer, ValidationDemo> wrapper = new RootBuilderWrapper<Integer, ValidationDemo>(new AutoValidationWithProjectionBuilders());
    Request<CollectionResponse<ValidationDemo>> request =
        wrapper.findBy("searchWithProjection")
            .fields(ValidationDemo.fields().stringA(), //invalid
                    ValidationDemo.fields().stringB(),
                    ValidationDemo.fields().includedA(), //invalid
                    ValidationDemo.fields().UnionFieldWithInlineRecord().MyRecord().foo1(), //invalid
                    ValidationDemo.fields().UnionFieldWithInlineRecord().MyRecord().foo2(),
                    ValidationDemo.fields().ArrayWithInlineRecord().items().bar1(),
                    ValidationDemo.fields().ArrayWithInlineRecord().items().bar2(), //invalid
                    ValidationDemo.fields().MapWithTyperefs().values().id(),
                    ValidationDemo.fields().MapWithTyperefs().values().tone(), //invalid
                    ValidationDemo.fields().validationDemoNext().stringA()) //invalid
            .build();
    try {
      _restClientAuto.sendRequest(request).getResponse();
    } catch (RestLiResponseException e) {
      Assert.assertEquals(e.getServiceErrorMessage(),
                          "ERROR :: /validationDemoNext/stringA :: length of \"invalid, length is larger than the max\" is out of range 1...10\n" +
                              "ERROR :: /includedA :: length of \"invalid, length is larger than the max\" is out of range 1...10\n" +
                              "ERROR :: /UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo1 :: field is required but not found and has no default value\n" +
                              "ERROR :: /ArrayWithInlineRecord/0/bar2 :: field is required but not found and has no default value\n" +
                              "ERROR :: /MapWithTyperefs/foo/tone :: field is required but not found and has no default value\n" +
                              "ERROR :: /stringA :: field is required but not found and has no default value\n");
    }
  }
}
