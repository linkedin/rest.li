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
import com.linkedin.data.schema.grammar.PdlSchemaParserFactory;
import com.linkedin.internal.common.SchemaDirLocation;
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
 * Tests for {@link FileDataSchemaResolver}.
 *
 * @author Evan Williams
 */
public class TestFileDataSchemaResolver
{
  // Mapping of JAR entry names (resource names) to the content to be written at that entry
  private static final Map<String, String> JAR_ENTRIES = new HashMap<>();
  static
  {
    JAR_ENTRIES.put("pegasus/com/example/models/Foo.pdl", "namespace com.example.models @legit record Foo {}");
    JAR_ENTRIES.put("extensions/com/example/models/FooExtension.pdl", "namespace com.example.models @legit record FooExtension {}");
    JAR_ENTRIES.put("legacyPegasusSchemas/com/example/models/Foo.pdl", "namespace com.example.models @impostor record Foo {}");
    JAR_ENTRIES.put("legacyPegasusSchemas/com/example/models/IgnoreAlternative.pdl", "namespace com.example.models record IgnoreAlternative {}");
    JAR_ENTRIES.put("com/example/models/Foo.pdl", "namespace com.example.models @impostor record Foo {}");
    JAR_ENTRIES.put("com/example/models/IgnoreRoot.pdl", "namespace com.example.models record IgnoreRoot {}");
  }

  private File _tempJar;

  @BeforeClass
  public void beforeClass() throws IOException
  {
    // Write a temp JAR file using the entries defined above
    _tempJar = File.createTempFile(getClass().getCanonicalName(), ".jar");
    _tempJar.deleteOnExit();
    JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(_tempJar));
    for (Map.Entry<String, String> entry : JAR_ENTRIES.entrySet())
    {
      jarOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
      jarOutputStream.write(entry.getValue().getBytes(Charset.defaultCharset()));
      jarOutputStream.closeEntry();
    }
    jarOutputStream.finish();
    jarOutputStream.close();
  }

  /**
   * Ensures that the resolver only detects schemas packaged under the default root 'pegasus'
   * or {@link FileDataSchemaResolver#getSchemasDirLocation()} directory in data template JARs.
   * Any schemas placed at the root or under some alternative root directory should be ignored by the resolver.
   */
  @Test
  public void testJarResolution() throws IOException
  {
    FileDataSchemaResolver resolver = new FileDataSchemaResolver(PdlSchemaParserFactory.instance(), _tempJar.getCanonicalPath());
    resolver.setExtension(".pdl");

    // Multiple schemas with this name exist, but assert only the one under 'pegasus' is resolved
    NamedDataSchema schema = resolver.findDataSchema("com.example.models.Foo", new StringBuilder());
    Assert.assertNotNull(schema);
    Assert.assertTrue(schema.getProperties().containsKey("legit"));
    Assert.assertFalse(schema.getProperties().containsKey("impostor"));
    // Assert extension schemas are not searched.
    Assert.assertNull(resolver.findDataSchema("com.example.models.FooExtension", new StringBuilder()));

    // Assert that schemas are resolved from provided directory path
    resolver.setSchemasDirLocation(SchemaDirLocation.extensions);
    schema = resolver.findDataSchema("com.example.models.FooExtension", new StringBuilder());
    Assert.assertTrue(schema.getProperties().containsKey("legit"));

    // Assert that alternative root directories are not searched
    schema = resolver.findDataSchema("com.example.models.IgnoreAlternative", new StringBuilder());
    Assert.assertNull(schema);

    // Assert that the resolver doesn't search from the root
    schema = resolver.findDataSchema("com.example.models.IgnoreRoot", new StringBuilder());
    Assert.assertNull(schema);

  }
}
