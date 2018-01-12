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

package com.linkedin.test.util;

/**
 * Util class to generate dummy data
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public final class DataGeneration
{
  public static Object[][] generateAllBooleanCombinationMatrix(int nElements)
  {
    Object[][] objects = new Object[(int)Math.pow(2, nElements)][nElements];

    for (int i = 0; i < Math.pow(2, nElements); i++)
    {
      String bin = Integer.toBinaryString(i);
      while (bin.length() < nElements)
      {
        bin = "0" + bin;
      }
      char[] chars = bin.toCharArray();
      Object[] boolArray = new Object[nElements];
      for (int j = 0; j < chars.length; j++)
      {
        boolArray[j] = chars[j] == '0';
      }
      objects[i] = boolArray;
    }
    return objects;
  }
}
