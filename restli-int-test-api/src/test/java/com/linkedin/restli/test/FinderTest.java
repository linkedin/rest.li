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

package com.linkedin.restli.test;


import com.linkedin.restli.examples.greetings.api.SearchMetadata;
import com.linkedin.restli.restspec.FinderSchema;
import com.linkedin.restli.restspec.FinderSchemaArray;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestSpecCodec;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


/**
 * Test finder return types and NoMetadata
 *
 * @author Keren Jin
 */
public class FinderTest
{
  public FinderTest()
  {
    String projectDir = System.getProperty(PROJECT_DIR_PROP);
    if (projectDir == null)
    {
      projectDir = "restli-int-test-api";
    }

    IDLS_DIR = projectDir + File.separator + IDLS_SUFFIX;
  }

  @Test
  public void test() throws IOException
  {
    final String findersFilename = IDLS_DIR + FINDERS_FILE;
    final ResourceSchema findersIdl = _codec.readResourceSchema(new FileInputStream(findersFilename));
    final FinderSchemaArray finders = findersIdl.getCollection().getFinders();

    for (FinderSchema finder : finders)
    {
      if ("searchWithoutMetadata".equals(finder.getName()))
      {
        Assert.assertFalse(finder.hasMetadata());
      }
      else if ("searchWithMetadata".equals(finder.getName()))
      {
        Assert.assertEquals(finder.getMetadata().getType(), SearchMetadata.class.getName());
      }
      else if ("basicSearch".equals(finder.getName()))
      {
        Assert.assertFalse(finder.hasMetadata());
      }
      else if ("predefinedSearch".equals(finder.getName()))
      {
        Assert.assertFalse(finder.hasMetadata());
      }
      else
      {
        throw new RuntimeException("Unknown finder is added to com.linkedin.restli.examples.greetings.server.FindersResource");
      }
    }
  }

  private static final String IDLS_SUFFIX = "src" + File.separator + "main" + File.separator + "idl" + File.separator;
  private static final String PROJECT_DIR_PROP = "test.projectDir";
  private static final String FINDERS_FILE = "com.linkedin.restli.examples.greetings.client.finders.restspec.json";

  private final String IDLS_DIR;
  private static final RestSpecCodec _codec = new RestSpecCodec();
}
