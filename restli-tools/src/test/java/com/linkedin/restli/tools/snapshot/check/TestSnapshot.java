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

package com.linkedin.restli.tools.snapshot.check;


import com.linkedin.data.schema.NamedDataSchema;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class TestSnapshot
{
  private static final String FS = File.separator;
  private static final String CIRCULAR_FILE = "circular-circular.snapshot.json";
  private static final String SNAPSHOTS_DIR = "snapshots";

  // Test to make sure that Snapshots handle interpreting circularly dependent models correctly.
  @Test
  public void testCircularlyDependentModels() throws IOException
  {
    InputStream stream = getClass().getClassLoader().getResourceAsStream(SNAPSHOTS_DIR + FS + CIRCULAR_FILE);
    Snapshot snapshot = new Snapshot(stream);

    Map<String, NamedDataSchema> models = snapshot.getModels();
    Assert.assertEquals(models.size(), 4);

    List<String> expectedModelNames = new ArrayList<>();
    expectedModelNames.add("com.linkedin.restli.tools.snapshot.circular.A");
    expectedModelNames.add("com.linkedin.restli.tools.snapshot.circular.B");
    expectedModelNames.add("com.linkedin.restli.tools.snapshot.circular.C");
    expectedModelNames.add("com.linkedin.restli.tools.snapshot.circular.D");

    for (String expectedModelName : expectedModelNames)
    {
      Assert.assertTrue(models.containsKey(expectedModelName), "Expected model " + expectedModelName + " in list of models for " + CIRCULAR_FILE);
    }
  }

}
