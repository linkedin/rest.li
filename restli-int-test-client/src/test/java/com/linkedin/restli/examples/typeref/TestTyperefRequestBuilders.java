/*
   Copyright (c) 2012 LinkedIn Corp.

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

/**
 * $Id: $
 */

package com.linkedin.restli.examples.typeref;

import com.linkedin.data.ByteString;
import com.linkedin.data.template.IntegerArray;
import com.linkedin.restli.examples.typeref.api.Fruits;
import com.linkedin.restli.examples.typeref.client.TyperefDoBooleanFuncRequestBuilder;
import com.linkedin.restli.examples.typeref.client.TyperefDoBytesFuncRequestBuilder;
import com.linkedin.restli.examples.typeref.client.TyperefDoDoubleFuncRequestBuilder;
import com.linkedin.restli.examples.typeref.client.TyperefDoFloatFuncRequestBuilder;
import com.linkedin.restli.examples.typeref.client.TyperefDoFruitsRefRequestBuilder;
import com.linkedin.restli.examples.typeref.client.TyperefDoIntArrayFuncRequestBuilder;
import com.linkedin.restli.examples.typeref.client.TyperefDoIntFuncRequestBuilder;
import org.testng.annotations.Test;

/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class TestTyperefRequestBuilders
{
  @Test
  public void testTyperefs() throws NoSuchMethodException
  {
    TyperefDoBooleanFuncRequestBuilder .class.getMethod("arg1Param", Boolean.class);
    TyperefDoBytesFuncRequestBuilder   .class.getMethod("arg1Param", ByteString.class);
    TyperefDoDoubleFuncRequestBuilder  .class.getMethod("arg1Param", Double.class);
    TyperefDoFloatFuncRequestBuilder   .class.getMethod("arg1Param", Float.class);
    TyperefDoFruitsRefRequestBuilder   .class.getMethod("arg1Param", Fruits.class);
    TyperefDoIntArrayFuncRequestBuilder.class.getMethod("arg1Param", IntegerArray.class);
    TyperefDoIntFuncRequestBuilder     .class.getMethod("arg1Param", Integer.class);
  }



}
