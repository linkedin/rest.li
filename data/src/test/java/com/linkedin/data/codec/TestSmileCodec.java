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
import java.io.IOException;
import org.testng.annotations.Test;

public class TestSmileCodec extends TestCodec
{
  @Test(dataProvider = "codecData", dataProviderClass = CodecDataProviders.class)
  public void testSmileDataCodec(String testName, DataComplex dataComplex) throws IOException
  {
    JacksonSmileDataCodec codec = new JacksonSmileDataCodec();
    testDataCodec(codec, dataComplex);
  }
}
