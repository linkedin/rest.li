/*
   Copyright (c) 2013 LinkedIn Corp.

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

package com.linkedin.pegasus.generator.test;


import org.testng.Assert;
import org.testng.annotations.Test;


public class TestTypeRefReferences
{
  @Test
  public void TestTypeRefReferenceInArray()
  {
    checkTypeExistence("com.linkedin.pegasus.generator.test.TypeRefInArray");
    checkTypeExistence("com.linkedin.pegasus.generator.test.TypeRefInArray2");
  }

  @Test
  public void TestTypeRefReferenceInMap()
  {
    checkTypeExistence("com.linkedin.pegasus.generator.test.TypeRefInMap");
    checkTypeExistence("com.linkedin.pegasus.generator.test.TypeRefInMap2");
  }

  @Test
  public void TestTypeRefReferenceInUnion()
  {
    checkTypeExistence("com.linkedin.pegasus.generator.test.TypeRefInUnion");
    checkTypeExistence("com.linkedin.pegasus.generator.test.TypeRefInUnion2");
  }

  @Test
  public void TestTypeRefReferenceInNestedCollections()
  {
    checkTypeExistence("com.linkedin.pegasus.generator.test.TypeRefInNestedCollections");
  }

  private static void checkTypeExistence(String className)
  {
    Class<?> clazz = null;
    try
    {
      clazz = Class.forName(className);
    }
    catch(ClassNotFoundException e)
    {
    }

    Assert.assertNotNull(clazz);
  }
}
