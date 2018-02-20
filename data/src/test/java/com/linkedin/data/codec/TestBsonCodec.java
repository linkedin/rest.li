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

package com.linkedin.data.codec;

import com.linkedin.data.DataComplex;

import org.testng.annotations.Test;

import java.io.IOException;


public class TestBsonCodec extends TestCodec
{
  @Test(dataProvider = "codecData", dataProviderClass = CodecDataProviders.class)
  public void testBsonDataCodec(String testName, DataComplex dataComplex) throws IOException
  {
    BsonDataCodec codec = new BsonDataCodec();
    testDataCodec(codec, dataComplex);
  }

  @Test(dataProvider = "codecData", dataProviderClass = CodecDataProviders.class)
  public void testBsonStressBufferSizeDataCodec(String testName, DataComplex dataComplex) throws IOException
  {
    for (int i = 16; i < 32; ++i)
    {
      BsonDataCodec codec = new BsonDataCodec(i, true);
      testDataCodec(codec, dataComplex);
    }
  }

}
