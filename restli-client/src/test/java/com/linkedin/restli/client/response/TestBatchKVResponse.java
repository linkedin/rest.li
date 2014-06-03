/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.client.response;


import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.client.test.TestRecord;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.test.MyComplexKey;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.TestConstants;
import com.linkedin.restli.internal.common.URIParamUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author Keren Jin
 */
public class TestBatchKVResponse
{
  private static final EmptyRecord _EMPTY_RECORD = new EmptyRecord();
  private static final List<String> _primitiveKeys;
  private static final List<ComplexResourceKey<MyComplexKey, EmptyRecord>> _complexKeys;
  private static final TestRecord _record;
  private static final ErrorResponse _error;
  static
  {
    _primitiveKeys = Arrays.asList("Lorem", "ipsum");

    final MyComplexKey keyPart1 = new MyComplexKey();
    keyPart1.setA("dolor");
    keyPart1.setB(7);
    final ComplexResourceKey<MyComplexKey, EmptyRecord> complexKey1 = new ComplexResourceKey<MyComplexKey, EmptyRecord>(keyPart1, _EMPTY_RECORD);

    final MyComplexKey keyPart2 = new MyComplexKey();
    keyPart2.setA("sit");
    keyPart2.setB(27);
    final ComplexResourceKey<MyComplexKey, EmptyRecord> complexKey2 = new ComplexResourceKey<MyComplexKey, EmptyRecord>(keyPart2, _EMPTY_RECORD);

    @SuppressWarnings("unchecked")
    final List<ComplexResourceKey<MyComplexKey, EmptyRecord>> complexKeys = Arrays.asList(complexKey1, complexKey2);
    _complexKeys = complexKeys;

    _record = new TestRecord();
    _record.setId(42);
    _record.setMessage("amet");

    _error = new ErrorResponse();
    _error.setStatus(HttpStatus.S_400_BAD_REQUEST.getCode());
    _error.setMessage("consectetur");
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchKVResponseDataProvider")
  public <K> void testDeserialization(List<K> keys,
                                      Class<K> keyClass,
                                      Class<? extends RecordTemplate> keyKeyClass,
                                      Class<? extends RecordTemplate> keyParamsClass,
                                      ProtocolVersion protocolVersion)
  {
    final K resultKey = keys.get(0);
    final K errorKey = keys.get(1);

    final String resultKeyString = URIParamUtils.encodeKeyForBody(resultKey, false, protocolVersion);
    final String errorKeyString = URIParamUtils.encodeKeyForBody(errorKey, false, protocolVersion);

    final DataMap inputResults = new DataMap();
    inputResults.put(resultKeyString, _record.data());

    final DataMap inputErrors = new DataMap();
    inputErrors.put(errorKeyString, _error.data());

    final DataMap testData = new DataMap();
    testData.put(BatchKVResponse.RESULTS, inputResults);
    testData.put(BatchKVResponse.ERRORS, inputErrors);

    final BatchKVResponse<K, TestRecord> response = new BatchKVResponse<K, TestRecord>(testData,
                                                                                       keyClass,
                                                                                       TestRecord.class,
                                                                                       Collections.<String, CompoundKey.TypeInfo>emptyMap(),
                                                                                       keyKeyClass,
                                                                                       keyParamsClass,
                                                                                       protocolVersion);
    final Map<K, TestRecord> outputResults = response.getResults();
    final TestRecord outRecord = outputResults.get(resultKey);
    Assert.assertEquals(outRecord, outRecord);

    final Map<K, ErrorResponse> outputErrors = response.getErrors();
    final ErrorResponse outError = outputErrors.get(errorKey);
    Assert.assertEquals(outError, _error);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchKVResponseDataProvider")
  private static Object[][] batchKVResponseDataProvider()
  {
    final String firstPrimitiveKey = _primitiveKeys.get(0);
    final ComplexResourceKey<MyComplexKey, EmptyRecord> firstComplexKey = _complexKeys.get(0);

    @SuppressWarnings("unchecked")
    final List<ComplexResourceKey<MyComplexKey, EmptyRecord>> singleComplexKeyArray = Arrays.asList(firstComplexKey, firstComplexKey);

    return new Object[][] {
      { Arrays.asList(firstPrimitiveKey, firstPrimitiveKey), String.class, null, null, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion() },
      { Arrays.asList(firstPrimitiveKey, firstPrimitiveKey), String.class, null, null, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion() },
      { _primitiveKeys, String.class, null, null, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion() },
      { _primitiveKeys, String.class, null, null, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion() },
      { singleComplexKeyArray, ComplexResourceKey.class, MyComplexKey.class, EmptyRecord.class, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion() },
      { singleComplexKeyArray, ComplexResourceKey.class, MyComplexKey.class, EmptyRecord.class, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion() },
      { _complexKeys, ComplexResourceKey.class, MyComplexKey.class, EmptyRecord.class, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion() },
      { _complexKeys, ComplexResourceKey.class, MyComplexKey.class, EmptyRecord.class, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion() }
    };
  }
}
