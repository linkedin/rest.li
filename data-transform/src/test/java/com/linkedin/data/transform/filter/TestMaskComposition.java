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

package com.linkedin.data.transform.filter;


import com.fasterxml.jackson.core.JsonParseException;
import com.linkedin.data.DataMap;
import com.linkedin.data.transform.DataComplexProcessor;
import com.linkedin.data.transform.DataProcessingException;
import org.testng.annotations.Test;

import java.io.IOException;

import static com.linkedin.data.TestUtil.dataMapFromString;
import static org.testng.Assert.assertEquals;

public class TestMaskComposition
{

  @Test
  public void testCompositionOfIncorrectRanges() throws JsonParseException,
      IOException,
      DataProcessingException
  {
    DataMap f1 = dataMapFromString("{ 'a': { '$start': -2, '$count': -3}}".replace('\'', '"'));
    DataMap f2 = dataMapFromString("{ 'a': { '$start': 0}}".replace('\'', '"'));
    DataComplexProcessor processor = new DataComplexProcessor(new MaskComposition(), f2, f1);
    boolean thrown = false;
    try {
      processor.run(false);
    } catch (DataProcessingException e) {
      assertEquals(e.getMessages().size(), 2, "expected exactly 2 errors");
      thrown = true;
    }
    assertEquals(thrown, true, "exception should have been thrown");
  }

}
