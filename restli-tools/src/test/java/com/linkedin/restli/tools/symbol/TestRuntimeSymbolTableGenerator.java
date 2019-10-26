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

import com.linkedin.data.codec.symbol.InMemorySymbolTable;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.restli.tools.sample.SimpleGreeting;
import java.util.Collections;
import org.junit.Assert;
import org.testng.annotations.Test;


public class TestRuntimeSymbolTableGenerator {

  @Test
  public void testSymbolTableGenerator()
  {
    DataSchema schema = DataTemplateUtil.getSchema(SimpleGreeting.class);
    InMemorySymbolTable symbolTable = RuntimeSymbolTableGenerator.generate("Haha", Collections.singleton(schema));
    Assert.assertEquals(37, symbolTable.size());
  }
}
