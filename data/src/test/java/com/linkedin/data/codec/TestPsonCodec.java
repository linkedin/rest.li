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

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;


public class TestPsonCodec extends TestCodec
{
  @Test(dataProvider = "codecData", dataProviderClass = CodecDataProviders.class)
  public void testPsonDataCodec(String testName, DataComplex dataComplex) throws IOException
  {
    int[] bufferSizesToTest = { 17, 19, 23, 29, 31, 37, 41, 43, 47, 0 };
    Boolean[] booleanValues = new Boolean[] { Boolean.TRUE, Boolean.FALSE };

    PsonDataCodec codec = new PsonDataCodec(true);

    PsonDataCodec.Options lastOption = null;
    for (int bufferSize : bufferSizesToTest)
    {
      for (boolean encodeCollectionCount : booleanValues)
      {
        for (boolean encodeStringLength : booleanValues)
        {
          PsonDataCodec.Options option = new PsonDataCodec.Options();
          option.setEncodeCollectionCount(encodeCollectionCount).setEncodeStringLength(encodeStringLength);
          if (bufferSize != 0)
          {
            option.setBufferSize(bufferSize);
          }

          codec.setOptions(option);
          testDataCodec(codec, dataComplex);

          if (lastOption != null)
          {
            assertFalse(option.equals(lastOption));
            assertNotSame(option.hashCode(), lastOption.hashCode());
            assertFalse(option.toString().equals(lastOption.toString()));
          }
          lastOption = option;
        }
      }
    }
  }

}
