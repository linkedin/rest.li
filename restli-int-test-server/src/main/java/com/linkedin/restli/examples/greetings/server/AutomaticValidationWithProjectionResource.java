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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.validation.CreateOnly;
import com.linkedin.restli.common.validation.ReadOnly;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.GreetingMap;
import com.linkedin.restli.examples.greetings.api.MyItemArray;
import com.linkedin.restli.examples.greetings.api.ValidationDemo;
import com.linkedin.restli.examples.greetings.api.myEnum;
import com.linkedin.restli.examples.greetings.api.myItem;
import com.linkedin.restli.examples.greetings.api.myRecord;
import com.linkedin.restli.server.BatchCreateKVResult;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchResult;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CreateKVResponse;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.annotations.ReturnEntity;
import com.linkedin.restli.server.resources.KeyValueResource;


/**
 * A simplied resource for testing Rest.li data automatic validation with automatic projection.
 *
 * @author jnchen
 */
@RestLiCollection(name = "autoValidationWithProjection", namespace = "com.linkedin.restli.examples.greetings.client")
@ReadOnly({"stringA", "intA", "UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo1",
           "ArrayWithInlineRecord/*/bar1", "validationDemoNext/stringB", "validationDemoNext/UnionFieldWithInlineRecord"})
@CreateOnly({"stringB", "intB", "UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo2",
             "MapWithTyperefs/*/id", "ArrayWithInlineRecord/*/bar3"})
public class AutomaticValidationWithProjectionResource implements KeyValueResource<Integer, ValidationDemo>
{
  // A return entity that contains mix of valid and invalid fields in all levels for projection testing.
  private static ValidationDemo _returnEntity;

  // A return entity list that contains one _returnEntity
  private static List<ValidationDemo> _returnEntityList;

  static
  {
    _returnEntity = new ValidationDemo();

    // mix of valid/invalid primitive fields
    _returnEntity.setStringB("valid");
    _returnEntity.setIntB(8); // invalid but optional
    _returnEntity.setIncludedA("invalid, length is larger than the max"); //invalid include
    // _returnEntity.setStringA() invalid, missing require
    // _returnEntity.setIntA() valid, missing optional

    // partially valid field -- union
    ValidationDemo.UnionFieldWithInlineRecord union = new ValidationDemo.UnionFieldWithInlineRecord();
    myRecord record = new myRecord();
    // record.setFoo1(); invalid, missing require
    // record.setFoo2(); valid, missing optional
    union.setMyRecord(record);
    _returnEntity.setUnionFieldWithInlineRecord(union);

    // partially valid field -- array
    MyItemArray array = new MyItemArray();
    myItem item = new myItem();
    item.setBar1("bar1"); // valid
    // item.setBar2(); invalid, missing require
    array.add(item);
    _returnEntity.setArrayWithInlineRecord(array);

    // partially valid field -- typeref
    GreetingMap map = new GreetingMap();
    Greeting greeting = new Greeting();
    greeting.setId(1);
    // greeting.setMessage() invalid, missing require
    // greeting.setTone() invalid, missing require
    map.put("foo", greeting);
    _returnEntity.setMapWithTyperefs(map);

    // partially valid field -- record
    ValidationDemo nextDemo = new ValidationDemo();
    nextDemo.setStringA("invalid, length is larger than the max");
    nextDemo.setIntB(7); // valid
    nextDemo.setUnionFieldWithInlineRecord(union);
    nextDemo.setArrayWithInlineRecord(array);
    nextDemo.setMapWithTyperefs(map);
    // nextDemo.setStringB() invalid, missing require
    // _returnEntity.setIntA() valid, missing optional
    _returnEntity.setValidationDemoNext(nextDemo);

    _returnEntityList = new ArrayList<ValidationDemo>();
    _returnEntityList.add(_returnEntity);
  }

  @RestMethod.Create
  @ReturnEntity
  public CreateKVResponse<Integer, ValidationDemo> create() throws CloneNotSupportedException
  {
    return new CreateKVResponse<Integer, ValidationDemo>(1, _returnEntity);
  }

  @RestMethod.BatchCreate
  @ReturnEntity
  public BatchCreateKVResult<Integer, ValidationDemo> batchCreate()
  {
    List<CreateKVResponse<Integer, ValidationDemo>> results = new ArrayList<CreateKVResponse<Integer, ValidationDemo>>();
    results.add(new CreateKVResponse<Integer, ValidationDemo>(1, _returnEntity));
    return new BatchCreateKVResult<Integer, ValidationDemo>(results);
  }

  @RestMethod.Get
  public ValidationDemo get()
  {
    return _returnEntity;
  }

  @RestMethod.BatchGet
  public BatchResult<Integer, ValidationDemo> batchGet()
  {
    Map<Integer, ValidationDemo> resultMap = new HashMap<Integer, ValidationDemo>();
    resultMap.put(1, _returnEntity);
    return new BatchResult<Integer, ValidationDemo>(resultMap, new HashMap<Integer, RestLiServiceException>());
  }

  @RestMethod.GetAll
  public List<ValidationDemo> getAll()
  {
    return _returnEntityList;
  }

  @Finder("searchWithProjection")
  public List<ValidationDemo> searchWithProjection()
  {
    List<ValidationDemo> validationDemos = new ArrayList<ValidationDemo>();
    validationDemos.add(_returnEntity);
    return validationDemos;
  }
}
