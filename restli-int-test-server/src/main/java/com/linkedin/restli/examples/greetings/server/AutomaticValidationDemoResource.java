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

package com.linkedin.restli.examples.greetings.server;


import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.validation.CreateOnly;
import com.linkedin.restli.common.validation.ReadOnly;
import com.linkedin.restli.examples.greetings.api.ValidationDemo;
import com.linkedin.restli.examples.greetings.api.myEnum;
import com.linkedin.restli.examples.greetings.api.myRecord;
import com.linkedin.restli.server.BasicCollectionResult;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchResult;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.KeyValueResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Free-form resource for testing Rest.li data validation.
 * This class shows how to validate data automatically by using the validation filters.
 * Invalid incoming data or outgoing data are rejected, and an error response is returned to the client.
 *
 * @author Soojung Ha
 */
@RestLiCollection(name = "autoValidationDemos", namespace = "com.linkedin.restli.examples.greetings.client")
@ReadOnly({"stringA", "intA", "UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo1",
           "ArrayWithInlineRecord/bar1", "validationDemoNext/stringB", "validationDemoNext/UnionFieldWithInlineRecord"})
@CreateOnly({"stringB", "intB", "UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo2",
             "MapWithTyperefs/id"})
public class AutomaticValidationDemoResource implements KeyValueResource<Integer, ValidationDemo>
{
  @RestMethod.Create
  public CreateResponse create(final ValidationDemo entity)
  {
    return new CreateResponse(1234);
  }

  @RestMethod.BatchCreate
  public BatchCreateResult<Integer, ValidationDemo> batchCreate(final BatchCreateRequest<Integer, ValidationDemo> entities)
  {
    List<CreateResponse> results = new ArrayList<CreateResponse>();
    int id = 0;
    for (ValidationDemo entity : entities.getInput())
    {
      results.add(new CreateResponse(id));
      id++;
    }
    return new BatchCreateResult<Integer, ValidationDemo>(results);
  }

  @RestMethod.Update
  public UpdateResponse update(final Integer key, final ValidationDemo entity)
  {
    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }

  @RestMethod.BatchUpdate
  public BatchUpdateResult<Integer, ValidationDemo> batchUpdate(final BatchUpdateRequest<Integer, ValidationDemo> entities)
  {
    Map<Integer, UpdateResponse> results = new HashMap<Integer, UpdateResponse>();
    Map<Integer, RestLiServiceException> errors = new HashMap<Integer, RestLiServiceException>();
    for (Map.Entry<Integer, ValidationDemo> entry : entities.getData().entrySet())
    {
      Integer key = entry.getKey();
      ValidationDemo entity = entry.getValue();
      results.put(key, new UpdateResponse(HttpStatus.S_204_NO_CONTENT));
    }
    return new BatchUpdateResult<Integer, ValidationDemo>(results, errors);
  }

  @RestMethod.PartialUpdate
  public UpdateResponse update(final Integer key, final PatchRequest<ValidationDemo> patch)
  {
    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }

  @RestMethod.BatchPartialUpdate
  public BatchUpdateResult<Integer, ValidationDemo> batchUpdate(final BatchPatchRequest<Integer, ValidationDemo> entityUpdates)
  {
    Map<Integer, UpdateResponse> results = new HashMap<Integer, UpdateResponse>();
    Map<Integer, RestLiServiceException> errors = new HashMap<Integer, RestLiServiceException>();
    for (Map.Entry<Integer, PatchRequest<ValidationDemo>> entry : entityUpdates.getData().entrySet())
    {
      Integer key = entry.getKey();
      PatchRequest<ValidationDemo> patch = entry.getValue();
      results.put(key, new UpdateResponse(HttpStatus.S_204_NO_CONTENT));
    }
    return new BatchUpdateResult<Integer, ValidationDemo>(results, errors);
  }

  @RestMethod.Get
  public ValidationDemo get(final Integer key)
  {
    if (key == 0)
    {
      throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST);
    }
    // Generate an entity that does not conform to the data schema
    ValidationDemo.UnionFieldWithInlineRecord union = new ValidationDemo.UnionFieldWithInlineRecord();
    union.setMyEnum(myEnum.BARBAR);
    ValidationDemo validationDemo = new ValidationDemo().setStringA("stringA is readOnly").setUnionFieldWithInlineRecord(union);
    return validationDemo;
  }

  @RestMethod.BatchGet
  public BatchResult<Integer, ValidationDemo> batchGet(Set<Integer> ids)
  {
    Map<Integer, ValidationDemo> resultMap = new HashMap<Integer, ValidationDemo>();
    Map<Integer, RestLiServiceException> errorMap = new HashMap<Integer, RestLiServiceException>();
    // Generate entities that are missing a required field
    for (Integer id : ids)
    {
      if (id == 0)
      {
        errorMap.put(id, new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST));
      }
      else if (id == 1)
      {
        ValidationDemo.UnionFieldWithInlineRecord union = new ValidationDemo.UnionFieldWithInlineRecord();
        union.setMyRecord(new myRecord().setFoo1(100).setFoo2(200));
        resultMap.put(id, new ValidationDemo().setStringA("a").setStringB("b").setUnionFieldWithInlineRecord(union));
      }
      else
      {
        ValidationDemo.UnionFieldWithInlineRecord union = new ValidationDemo.UnionFieldWithInlineRecord();
        union.setMyRecord(new myRecord());
        ValidationDemo validationDemo = new ValidationDemo().setStringA("a").setStringB("b").setUnionFieldWithInlineRecord(union);
        resultMap.put(id, validationDemo);
      }
    };
    return new BatchResult<Integer, ValidationDemo>(resultMap, errorMap);
  }

  @RestMethod.GetAll
  public List<ValidationDemo> getAll()
  {
    List<ValidationDemo> validationDemos = new ArrayList<ValidationDemo>();
    // Generate entities with stringA fields that are too long
    for (int i = 0; i < 4; i++)
    {
      ValidationDemo.UnionFieldWithInlineRecord union = new ValidationDemo.UnionFieldWithInlineRecord();
      union.setMyEnum(myEnum.FOOFOO);
      validationDemos.add(new ValidationDemo().setStringA("This string is too long to pass validation.")
                              .setStringB("stringB").setUnionFieldWithInlineRecord(union));
    }
    return validationDemos;
  }

  @Finder("search")
  public List<ValidationDemo> search(@QueryParam("intA") Integer intA)
  {
    if (intA == 0)
    {
      throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST);
    }
    List<ValidationDemo> validationDemos = new ArrayList<ValidationDemo>();
    // Generate entities that are missing stringB fields
    for (int i = 0; i < 3; i++)
    {
      ValidationDemo.UnionFieldWithInlineRecord union = new ValidationDemo.UnionFieldWithInlineRecord();
      union.setMyEnum(myEnum.FOOFOO);
      validationDemos.add(new ValidationDemo().setStringA("valueA").setIntA(intA).setUnionFieldWithInlineRecord(union));
    }
    return validationDemos;
  }
}
