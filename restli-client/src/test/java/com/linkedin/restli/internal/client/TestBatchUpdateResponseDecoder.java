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


import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.TestConstants;


/**
 * @author Jun Chen
 */
public class TestBatchUpdateResponseDecoder
{
  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchEntityResponseDataProvider")
  private static Object[][] batchEntityResponseDataProvider()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion() },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion() },
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchEntityResponseDataProvider")
  public void testDecodingWithEmptyDataMap(ProtocolVersion protocolVersion)
    throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, IOException
  {
    final BatchUpdateResponseDecoder<String> decoder =
      new BatchUpdateResponseDecoder<String>(new TypeSpec<String>(String.class), Collections.<String, CompoundKey.TypeInfo>emptyMap(), null);

    final BatchKVResponse<String, UpdateStatus> response = decoder.wrapResponse(null, Collections.<String, String>emptyMap(), protocolVersion);
    Assert.assertNull(response);
  }
}
