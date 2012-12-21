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
import com.linkedin.restli.examples.typeref.client.TyperefDoBooleanFuncBuilder;
import com.linkedin.restli.examples.typeref.client.TyperefDoBytesFuncBuilder;
import com.linkedin.restli.examples.typeref.client.TyperefDoDoubleFuncBuilder;
import com.linkedin.restli.examples.typeref.client.TyperefDoFloatFuncBuilder;
import com.linkedin.restli.examples.typeref.client.TyperefDoFruitsRefBuilder;
import com.linkedin.restli.examples.typeref.client.TyperefDoIntArrayFuncBuilder;
import com.linkedin.restli.examples.typeref.client.TyperefDoIntFuncBuilder;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class TestTyperefRequestBuilders
{
  @Test
  public void testTyperefs() throws NoSuchMethodException
  {
    TyperefDoBooleanFuncBuilder .class.getMethod("paramArg1", Boolean.class);
    TyperefDoBytesFuncBuilder   .class.getMethod("paramArg1", ByteString.class);
    TyperefDoDoubleFuncBuilder  .class.getMethod("paramArg1", Double.class);
    TyperefDoFloatFuncBuilder   .class.getMethod("paramArg1", Float.class);
    TyperefDoFruitsRefBuilder   .class.getMethod("paramArg1", Fruits.class);
    TyperefDoIntArrayFuncBuilder.class.getMethod("paramArg1", IntegerArray.class);
    TyperefDoIntFuncBuilder     .class.getMethod("paramArg1", Integer.class);
  }



}
