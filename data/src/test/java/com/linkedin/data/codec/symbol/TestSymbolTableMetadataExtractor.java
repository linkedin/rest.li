/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.data.codec.symbol;

import org.testng.Assert;
import org.testng.annotations.Test;


public class TestSymbolTableMetadataExtractor
{
  @Test
  public void testExtractTableInfoRemoteTable()
  {
    SymbolTableMetadata metadata =
        new SymbolTableMetadataExtractor().extractMetadata("https://Host:100/service|Prefix-1000");
    Assert.assertEquals(metadata.getServerNodeUri(), "https://Host:100/service");
    Assert.assertEquals(metadata.getSymbolTableName(), "Prefix-1000");
    Assert.assertTrue(metadata.isRemote());
  }

  @Test
  public void testExtractTableInfoLocalTable()
  {
    SymbolTableMetadata metadata =
        new SymbolTableMetadataExtractor().extractMetadata("Prefix-1000");
    Assert.assertNull(metadata.getServerNodeUri());
    Assert.assertEquals(metadata.getSymbolTableName(), "Prefix-1000");
    Assert.assertFalse(metadata.isRemote());
  }
}
