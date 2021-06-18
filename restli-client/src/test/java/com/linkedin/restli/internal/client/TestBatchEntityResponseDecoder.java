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

package com.linkedin.restli.internal.client;


import com.linkedin.data.DataMap;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.test.TestRecord;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.TestConstants;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author Keren Jin
 */
public class TestBatchEntityResponseDecoder
{
  private static final List<String> _keys;
  private static final TestRecord _record;
  private static final HttpStatus _status;
  private static final ErrorResponse _error;
  static
  {
    _keys = Arrays.asList("Lorem", "ipsum", "dolor");

    _record = new TestRecord();
    _record.setId(42);
    _record.setMessage("sit");

    _status = HttpStatus.S_200_OK;

    _error = new ErrorResponse();
    _error.setStatus(HttpStatus.S_400_BAD_REQUEST.getCode());
    _error.setMessage("amet");
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchEntityResponseDataProvider")
  public void testDecoding(List<String> keys, ProtocolVersion protocolVersion)
    throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, IOException
  {
    final String resultKey = keys.get(0);
    final String statusKey = keys.get(1);
    final String errorKey = keys.get(2);

    final DataMap resultData = new DataMap();
    resultData.put(resultKey, _record.data());

    final DataMap statusData = new DataMap();
    statusData.put(statusKey, _status.getCode());

    final DataMap errorData = new DataMap();
    errorData.put(errorKey, _error.data());

    final DataMap data = new DataMap();
    data.put(BatchResponse.RESULTS, resultData);
    data.put(BatchResponse.STATUSES, statusData);
    data.put(BatchResponse.ERRORS, errorData);

    final BatchEntityResponseDecoder<String, TestRecord> decoder =
        new BatchEntityResponseDecoder<>(new TypeSpec<>(TestRecord.class),
            new TypeSpec<>(String.class),
            Collections.<String, CompoundKey.TypeInfo>emptyMap(),
            null);

    final BatchKVResponse<String, EntityResponse<TestRecord>> response = decoder.wrapResponse(data, Collections.<String, String>emptyMap(), protocolVersion);
    final Map<String, EntityResponse<TestRecord>> results = response.getResults();
    final Map<String, ErrorResponse> errors = response.getErrors();

    final Collection<String> uniqueKeys = new HashSet<>(keys);
    Assert.assertEquals(results.size(), uniqueKeys.size());
    Assert.assertEquals(errors.size(), 1);

    Assert.assertEquals(results.get(resultKey).getEntity(), _record);
    Assert.assertEquals(results.get(statusKey).getStatus(), _status);
    Assert.assertEquals(results.get(errorKey).getError(), _error);
    Assert.assertEquals(errors.get(errorKey), _error);

    // Check that the response still contains the original data map
    Assert.assertEquals(response.data(), data);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchEntityResponseDataProvider")
  private static Object[][] batchEntityResponseDataProvider()
  {
    final String firstKey = _keys.get(0);

    return new Object[][] {
      { Arrays.asList(firstKey, firstKey, firstKey), AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion() },
      { Arrays.asList(firstKey, firstKey, firstKey), AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion() },
      { _keys, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion() },
      { _keys, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion() }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchEntityResponseDataProvider")
  public void testDecodingWithEmptyDataMap(List<String> keys, ProtocolVersion protocolVersion)
    throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, IOException
  {
    final BatchEntityResponseDecoder<String, TestRecord> decoder =
        new BatchEntityResponseDecoder<>(new TypeSpec<>(TestRecord.class),
            new TypeSpec<>(String.class),
            Collections.<String, CompoundKey.TypeInfo>emptyMap(),
            null);

    final BatchKVResponse<String, EntityResponse<TestRecord>> response = decoder.wrapResponse(null, Collections.<String, String>emptyMap(), protocolVersion);
    Assert.assertNull(response);
  }
}
