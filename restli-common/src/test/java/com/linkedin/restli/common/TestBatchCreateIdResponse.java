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

package com.linkedin.restli.common;


import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.linkedin.restli.common.test.MyComplexKey;
import com.linkedin.restli.internal.common.AllProtocolVersions;


/**
 * @author xma
 */
public class TestBatchCreateIdResponse
{
  private CompoundKey buildCompoundKey(String part1, int part2)
  {
    return new CompoundKey().append("part1", part1).append("part2", part2);
  }

  private ComplexResourceKey<MyComplexKey, EmptyRecord> buildComplexResourceKey(Long id)
  {
    MyComplexKey complexKey = new MyComplexKey().setB(id).setA(id + "");
    return new ComplexResourceKey<MyComplexKey, EmptyRecord>(complexKey, new EmptyRecord());
  }

  @DataProvider
  public Object[][] provideKeys()
  {
    return new Object[][] {
        new Object[] {new Long[] {1L, 2L, 3L}},
        new Object[] {new MyCustomString[] {new MyCustomString("1"), new MyCustomString("2"), new MyCustomString("3")}},
        new Object[] {new CompoundKey[] {buildCompoundKey("c1", 1), buildCompoundKey("c2", 2), buildCompoundKey("c3", 3)}},
        new Object[] {new ComplexResourceKey<?, ?>[] {buildComplexResourceKey(1L), buildComplexResourceKey(2L), buildComplexResourceKey(3L)}}
    };
  }

  @Test(dataProvider = "provideKeys")
  public <K> void testCreate(K[] keys)
  {
    ProtocolVersion version = AllProtocolVersions.BASELINE_PROTOCOL_VERSION;

    List<CreateIdStatus<K>> elements = new ArrayList<CreateIdStatus<K>>();
    elements.add(new CreateIdStatus<K>(HttpStatus.S_201_CREATED.getCode(), keys[0], null, version));
    elements.add(new CreateIdStatus<K>(HttpStatus.S_201_CREATED.getCode(), keys[1], null, version));

    ErrorResponse error = new ErrorResponse().setMessage("3");
    elements.add(new CreateIdStatus<K>(HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode(), keys[2], error, version));

    BatchCreateIdResponse<K> batchResp = new BatchCreateIdResponse<K>(elements);

    Assert.assertEquals(batchResp.getElements(), elements);
  }
}
