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
      new SymbolTableNameHandler("Prefix", "https://Host:100/service");

  @Test
  public void testGenerateName()
  {
    List<String> symbols = Collections.unmodifiableList(Arrays.asList("Haha", "Hehe"));
    String name = SYMBOL_TABLE_NAME_HANDLER.generateName(symbols);
    Assert.assertEquals(name, "https://Host:100/service|Prefix-" + symbols.hashCode());
  }

  @Test
  public void testExtractTableInfoLocalTable()
  {
    Tuple3<String, String, Boolean> tuple = SYMBOL_TABLE_NAME_HANDLER.extractTableInfo("https://Host:100/service|Prefix-1000");
    Assert.assertEquals(tuple._1(), "https://Host:100/service");
    Assert.assertEquals(tuple._2(), "Prefix-1000");
    Assert.assertTrue(tuple._3());
  }

  @Test
  public void testExtractTableInfoNonLocalTable()
  {
    Tuple3<String, String, Boolean> tuple = SYMBOL_TABLE_NAME_HANDLER.extractTableInfo("https://OtherHost:100/service|Prefix-1000");
    Assert.assertEquals(tuple._1(), "https://OtherHost:100/service");
    Assert.assertEquals(tuple._2(), "Prefix-1000");
    Assert.assertFalse(tuple._3());
  }

  @Test
  public void testReplaceServerNodeUri()
  {
    String name = "https://SomeOldHostName:100/SomeOtherService|SomeOtherPrefix-1000";
    String replacedName = SYMBOL_TABLE_NAME_HANDLER.replaceServerNodeUri(name);
    Assert.assertEquals(replacedName, "https://Host:100/service|SomeOtherPrefix-1000");
  }
}
