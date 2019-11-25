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

package com.linkedin.restli.tools.symbol;

import com.linkedin.parseq.function.Tuple3;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestSymbolTableNameHandler
{
  private static final SymbolTableNameHandler SYMBOL_TABLE_NAME_HANDLER =
      new SymbolTableNameHandler("Prefix", "Host", 100);

  @Test
  public void testGenerateName()
  {
    List<String> symbols = Collections.unmodifiableList(Arrays.asList("Haha", "Hehe"));
    String name = SYMBOL_TABLE_NAME_HANDLER.generateName(symbols);
    Assert.assertEquals(name, "Host:100|Prefix-" + symbols.hashCode());
  }

  @Test
  public void testExtractTableInfoLocalTable()
  {
    Tuple3<String, String, Boolean> tuple = SYMBOL_TABLE_NAME_HANDLER.extractTableInfo("Host:100|Prefix-1000");
    Assert.assertEquals(tuple._1(), "Host:100");
    Assert.assertEquals(tuple._2(), "Prefix-1000");
    Assert.assertTrue(tuple._3());
  }

  @Test
  public void testExtractTableInfoNonLocalTable()
  {
    Tuple3<String, String, Boolean> tuple = SYMBOL_TABLE_NAME_HANDLER.extractTableInfo("OtherHost:100|Prefix-1000");
    Assert.assertEquals(tuple._1(), "OtherHost:100");
    Assert.assertEquals(tuple._2(), "Prefix-1000");
    Assert.assertFalse(tuple._3());
  }

  @Test
  public void testReplaceHostName()
  {
    String name = "SomeOldHostName:100|Prefix-1000";
    String replacedName = SYMBOL_TABLE_NAME_HANDLER.replaceHostName(name);
    Assert.assertEquals(replacedName, "Host:100|Prefix-1000");
  }
}
