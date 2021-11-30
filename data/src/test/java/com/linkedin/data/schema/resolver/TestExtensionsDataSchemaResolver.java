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

package com.linkedin.data.schema.resolver;

import com.linkedin.data.schema.NamedDataSchema;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * Tests for {@link ExtensionsDataSchemaResolver}.
 *
 * @author Aman Gupta
 */
@SuppressWarnings("deprecation")
public class TestExtensionsDataSchemaResolver
{
  // Mapping of JAR entry names (resource names) to the content to be written at that entry
  private static final Map<String, String> JAR_ENTRIES = new HashMap<>();
  static
  {
    JAR_ENTRIES.put("pegasus/com/example/models/Foo.pdl", "namespace com.example.models @legit record Foo {}");
    JAR_ENTRIES.put("extensions/com/example/models/FooExtension.pdl", "namespace com.example.models @legit record FooExtension includes Foo{}");
    JAR_ENTRIES.put("legacyPegasusSchemas/com/example/models/IgnoreAlternative.pdl", "namespace com.example.models record IgnoreAlternative {}");
  }

  private File _tempJar;

  @BeforeClass
  public void beforeClass() throws IOException
  {
    _tempJar = TestDataSchemaResolver.buildTempJar(JAR_ENTRIES);
  }

  /**
   * Ensures that the resolver detects extensions schemas packaged under the root 'extensions' and resolves dependent
   * data schemas from 'pegasus' directory in data template JARs.
   * Any schemas placed at the root or under some alternative root directory should be ignored by the resolver.
   */
  @Test
  public void testJarResolution() throws IOException
  {
    ExtensionsDataSchemaResolver resolver = new ExtensionsDataSchemaResolver(_tempJar.getCanonicalPath());
    // Assert that schemas are resolved from provided directory path
    NamedDataSchema schema = resolver.findDataSchema("com.example.models.FooExtension", new StringBuilder());
    Assert.assertTrue(schema.getProperties().containsKey("legit"));

    // Assert that alternative root directories are not searched
    schema = resolver.findDataSchema("com.example.models.IgnoreAlternative", new StringBuilder());
    Assert.assertNull(schema);
  }
}
