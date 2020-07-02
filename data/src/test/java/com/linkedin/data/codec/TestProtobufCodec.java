/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.data.codec;

import com.linkedin.data.DataComplex;
import com.linkedin.data.DataMap;
import com.linkedin.data.protobuf.Utf8Utils;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestProtobufCodec extends TestCodec
{
  @Test(dataProvider = "protobufCodecData", dataProviderClass = CodecDataProviders.class)
  public void testProtobufDataCodec(String testName, DataComplex dataComplex, boolean enableFixedLengthFloatDoubles) throws IOException
  {
    ProtobufDataCodec codec = new ProtobufDataCodec(
        new ProtobufCodecOptions.Builder().setEnableASCIIOnlyStrings(true)
            .setEnableFixedLengthFloatDoubles(enableFixedLengthFloatDoubles)
            .build());
    testDataCodec(codec, dataComplex);
    codec = new ProtobufDataCodec(
      new ProtobufCodecOptions.Builder().setEnableASCIIOnlyStrings(false)
          .setEnableFixedLengthFloatDoubles(enableFixedLengthFloatDoubles)
          .build());
    testDataCodec(codec, dataComplex);
  }

  @Test(dataProvider = "surrogatePairData", dataProviderClass = CodecDataProviders.class)
  public void testSurrogatePairs(String value, String expectedString, int expectedLength,
      boolean isValidSurrogatePair, boolean tolerateInvalidSurrogatePairs) throws Exception
  {
    DataCodec codec =
        new ProtobufDataCodec(new ProtobufCodecOptions.Builder()
            .setShouldTolerateInvalidSurrogatePairs(tolerateInvalidSurrogatePairs)
            .build());

    if (isValidSurrogatePair) {
      Assert.assertEquals(Utf8Utils.encodedLength(value, true), expectedLength);
      Assert.assertEquals(Utf8Utils.encodedLength(value, false), expectedLength);
    } else {
      Assert.assertEquals(Utf8Utils.encodedLength(value, true), expectedLength);
      try {
        Utf8Utils.encodedLength(value, false);
        Assert.fail("Exception not thrown for invalid surrogate pair");
      } catch (IllegalArgumentException e) {
        // Success.
      }
    }

    DataMap dataMap = new DataMap();
    dataMap.put("key", value);

    if (isValidSurrogatePair || tolerateInvalidSurrogatePairs) {
      DataMap roundtrip = codec.bytesToMap(codec.mapToBytes(dataMap));
      Assert.assertEquals(roundtrip.get("key"), expectedString);
    } else {
      try {
        codec.bytesToMap(codec.mapToBytes(dataMap));
        Assert.fail("Exception not thrown for invalid surrogate pair");
      } catch (IOException e) {
        // Success.
      }
    }
  }
}
