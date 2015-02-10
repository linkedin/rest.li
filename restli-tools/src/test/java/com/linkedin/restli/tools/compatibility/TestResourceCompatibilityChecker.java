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

package com.linkedin.restli.tools.compatibility;

import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.LongDataSchema;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.SchemaParserFactory;
import com.linkedin.data.schema.StringDataSchema;
import com.linkedin.data.schema.generator.AbstractGenerator;
import com.linkedin.data.schema.resolver.FileDataSchemaResolver;
import com.linkedin.data.template.StringArray;
import com.linkedin.restli.restspec.AssocKeySchema;
import com.linkedin.restli.restspec.AssocKeySchemaArray;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestSpecCodec;
import com.linkedin.restli.tools.idlcheck.CompatibilityInfo;
import com.linkedin.restli.tools.idlcheck.CompatibilityLevel;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * @author Moira Tagle
 * @version $Revision: $
 */
// Gradle by default will use the module directory as the working directory
// IDE such as IntelliJ IDEA may use the project directory instead
// If you create test in IDE, make sure the working directory is always the module directory
public class TestResourceCompatibilityChecker
{
  @BeforeClass
  public void setUp()
  {
    final String resourcesDir = System.getProperty("user.dir") + File.separator + RESOURCES_SUFFIX;
    String resolverPath = System.getProperty(AbstractGenerator.GENERATOR_RESOLVER_PATH);
    if (resolverPath == null)
    {
      resolverPath = resourcesDir + PEGASUS_SUFFIX;
    }

    prevSchemaResolver = new FileDataSchemaResolver(SchemaParserFactory.instance(), resolverPath);
    compatSchemaResolver = new FileDataSchemaResolver(SchemaParserFactory.instance(), resolverPath);
    incompatSchemaResolver = new FileDataSchemaResolver(SchemaParserFactory.instance(), resolverPath);

    bindSchemaResolvers();
  }

  @Test
  public void testPassCollectionFile() throws IOException
  {
    final Collection<CompatibilityInfo> resourceTestDiffs = new HashSet<CompatibilityInfo>();
    final Collection<CompatibilityInfo> modelTestDiffs = new HashSet<CompatibilityInfo>();

    resourceTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList(""),
                                                CompatibilityInfo.Type.OPTIONAL_VALUE, "namespace"));
    resourceTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "supports"),
                                                CompatibilityInfo.Type.SUPERSET, new HashSet<String>(Arrays.asList("update"))));
    resourceTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "methods"),
                                                CompatibilityInfo.Type.SUPERSET, new HashSet<String>(Arrays.asList("update"))));
    resourceTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "finders", "search", "parameters", "tone"),
                                                CompatibilityInfo.Type.OPTIONAL_PARAMETER));
    resourceTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "finders", "search", "parameters"),
                                                CompatibilityInfo.Type.PARAMETER_NEW_OPTIONAL, "newParam"));
    resourceTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "finders", "search", "parameters", "tone"),
                                                CompatibilityInfo.Type.DEPRECATED, "The \"items\" field"));
    resourceTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "actions", "oneAction", "parameters"),
                                                CompatibilityInfo.Type.PARAMETER_NEW_OPTIONAL, "newParam"));
    resourceTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "actions", "oneAction", "parameters", "bitfield"),
                                                CompatibilityInfo.Type.DEPRECATED, "The \"items\" field"));
    resourceTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "actions", "oneAction", "parameters", "someString"),
                                                CompatibilityInfo.Type.OPTIONAL_PARAMETER));
    resourceTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "actions", "exceptionTest", "throws"),
                                                CompatibilityInfo.Type.SUPERSET, new HashSet<String>(Arrays.asList("java.lang.NullPointerException"))));
    resourceTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "entity", "actions", "someAction", "parameters", "b", "default"),
                                                CompatibilityInfo.Type.VALUE_DIFFERENT, "default", "changed"));
    resourceTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "finders", "oneFinder", "annotations", "deprecated"),
                                                CompatibilityInfo.Type.ANNOTATIONS_CHANGED, "added"));
    resourceTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "finders", "oneFinder", "parameters", "param1", "annotations", "deprecated"),
                                                CompatibilityInfo.Type.ANNOTATIONS_CHANGED, "added"));
    modelTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("com.linkedin.greetings.api.Greeting"),
                                             CompatibilityInfo.Type.TYPE_INFO, "new record removed optional fields tone"));
    modelTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("com.linkedin.greetings.api.Greeting"),
                                             CompatibilityInfo.Type.TYPE_INFO, "new record added optional fields newField"));

    final ResourceSchema prevResource = idlToResource(IDLS_SUFFIX + PREV_COLL_FILE);
    final ResourceSchema currResource = idlToResource(IDLS_SUFFIX + CURR_COLL_PASS_FILE);

    ResourceCompatibilityChecker checker = new ResourceCompatibilityChecker(prevResource, prevSchemaResolver,
                                                                            currResource, compatSchemaResolver);

    boolean check = checker.check(CompatibilityLevel.BACKWARDS);
    Assert.assertTrue(check);

    final Collection<CompatibilityInfo> resourceIncompatibles = checker.getInfoMap().getRestSpecIncompatibles();
    final Collection<CompatibilityInfo> resourceCompatibles = new HashSet<CompatibilityInfo>(checker.getInfoMap().getRestSpecCompatibles());

    for (CompatibilityInfo di : resourceTestDiffs)
    {
      Assert.assertTrue(resourceCompatibles.contains(di), "Reported resource compatibles should contain: " + di.toString());
      resourceCompatibles.remove(di);
    }

    Assert.assertTrue(resourceIncompatibles.isEmpty());
    Assert.assertTrue(resourceCompatibles.isEmpty());

    final Collection<CompatibilityInfo> modelIncompatibles = checker.getInfoMap().getModelIncompatibles();
    final Collection<CompatibilityInfo> modelCompatibles = new HashSet<CompatibilityInfo>(checker.getInfoMap().getModelCompatibles());

    for (CompatibilityInfo di : modelTestDiffs)
    {
      Assert.assertTrue(modelCompatibles.contains(di), "Reported model compatibles should contain: " + di.toString());
      modelCompatibles.remove(di);
    }

    Assert.assertTrue(modelIncompatibles.isEmpty());
    Assert.assertTrue(modelCompatibles.isEmpty());
  }

  @Test
  public void testPassAssociationFile() throws IOException
  {
    final Collection<CompatibilityInfo> testDiffs = new HashSet<CompatibilityInfo>();
    testDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "association", "methods", "create", "parameters"),
                                        CompatibilityInfo.Type.PARAMETER_NEW_OPTIONAL, "type"));

    final ResourceSchema prevResource = idlToResource(IDLS_SUFFIX + PREV_ASSOC_FILE);
    final ResourceSchema currResource = idlToResource(IDLS_SUFFIX + CURR_ASSOC_PASS_FILE);

    ResourceCompatibilityChecker checker = new ResourceCompatibilityChecker(prevResource, prevSchemaResolver,
                                                                            currResource, prevSchemaResolver);

    Assert.assertTrue(checker.check(CompatibilityLevel.BACKWARDS));

    final Collection<CompatibilityInfo> incompatibles = checker.getInfoMap().getIncompatibles();
    final Collection<CompatibilityInfo> compatibles = new HashSet<CompatibilityInfo>(checker.getInfoMap().getCompatibles());

    for (CompatibilityInfo di : testDiffs)
    {
      Assert.assertTrue(compatibles.contains(di), "Reported compatibles should contain: " + di.toString());
      compatibles.remove(di);
    }

    Assert.assertTrue(incompatibles.isEmpty());
    Assert.assertTrue(compatibles.isEmpty());
  }

  @Test
  public void testPassSimpleFile() throws IOException
  {
    final Collection<CompatibilityInfo> resourceTestDiffs = new HashSet<CompatibilityInfo>();
    final Collection<CompatibilityInfo> modelTestDiffs = new HashSet<CompatibilityInfo>();

    resourceTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList(""),
                                                CompatibilityInfo.Type.OPTIONAL_VALUE, "namespace"));
    resourceTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "simple", "supports"),
                                                CompatibilityInfo.Type.SUPERSET, new HashSet<String>(Arrays.asList("update"))));
    resourceTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "simple", "methods"),
                                                CompatibilityInfo.Type.SUPERSET, new HashSet<String>(Arrays.asList("update"))));
    resourceTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "simple", "methods", "get", "parameters", "param1", "default"),
                                                CompatibilityInfo.Type.VALUE_DIFFERENT, "abcd", "abc"));
    resourceTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "simple", "actions", "oneAction", "parameters", "bitfield"),
                                                CompatibilityInfo.Type.DEPRECATED, "The \"items\" field"));
    resourceTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "simple", "actions", "oneAction", "parameters", "someString"),
                                                CompatibilityInfo.Type.OPTIONAL_PARAMETER));
    resourceTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "simple", "actions", "oneAction", "parameters"),
                                                CompatibilityInfo.Type.PARAMETER_NEW_OPTIONAL, "newParam"));
    resourceTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "simple", "actions", "oneAction", "parameters", "someString2", "default"),
                                                CompatibilityInfo.Type.VALUE_DIFFERENT, "default", "changed"));
    resourceTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "simple", "actions", "twoAction", "annotations", "deprecated"),
                                                CompatibilityInfo.Type.ANNOTATIONS_CHANGED, "added"));
    resourceTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "annotations", "deprecated"),
                                                CompatibilityInfo.Type.ANNOTATIONS_CHANGED, "added"));
    resourceTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "simple", "methods", "get", "annotations", "deprecated"),
                                                CompatibilityInfo.Type.ANNOTATIONS_CHANGED, "added"));
    modelTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("com.linkedin.greetings.api.Greeting"),
                                             CompatibilityInfo.Type.TYPE_INFO, "new record removed optional fields tone"));
    modelTestDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("com.linkedin.greetings.api.Greeting"),
                                             CompatibilityInfo.Type.TYPE_INFO, "new record added optional fields newField"));

    final ResourceSchema prevResource = idlToResource(IDLS_SUFFIX + PREV_SIMPLE_FILE);
    final ResourceSchema currResource = idlToResource(IDLS_SUFFIX + CURR_SIMPLE_PASS_FILE);

    ResourceCompatibilityChecker checker = new ResourceCompatibilityChecker(prevResource, prevSchemaResolver,
                                                                            currResource, compatSchemaResolver);

    boolean check = checker.check(CompatibilityLevel.BACKWARDS);
    Assert.assertTrue(check);

    final Collection<CompatibilityInfo> resourceIncompatibles = checker.getInfoMap().getRestSpecIncompatibles();
    final Collection<CompatibilityInfo> resourceCompatibles = new HashSet<CompatibilityInfo>(checker.getInfoMap().getRestSpecCompatibles());

    for (CompatibilityInfo di : resourceTestDiffs)
    {
      Assert.assertTrue(resourceCompatibles.contains(di), "Reported resource compatibles should contain: " + di.toString());
      resourceCompatibles.remove(di);
    }

    Assert.assertTrue(resourceIncompatibles.isEmpty());
    Assert.assertTrue(resourceCompatibles.isEmpty());

    final Collection<CompatibilityInfo> modelIncompatibles = checker.getInfoMap().getModelIncompatibles();
    final Collection<CompatibilityInfo> modelCompatibles = new HashSet<CompatibilityInfo>(checker.getInfoMap().getModelCompatibles());

    for (CompatibilityInfo di : modelTestDiffs)
    {
      Assert.assertTrue(modelCompatibles.contains(di), "Reported model compatibles should contain: " + di.toString());
      modelCompatibles.remove(di);
    }

    Assert.assertTrue(modelIncompatibles.isEmpty());
    Assert.assertTrue(modelCompatibles.isEmpty());
  }

  @Test
  public void testFailCollectionFile() throws IOException
  {
    final SchemaParser sp = new SchemaParser();
    sp.parse("\"StringRef\"");

    final Collection<CompatibilityInfo> resourceTestErrors = new HashSet<CompatibilityInfo>();
    final Collection<CompatibilityInfo> modelTestErrors = new HashSet<CompatibilityInfo>();
    resourceTestErrors.add(
      new CompatibilityInfo(Arrays.<Object>asList("", "collection", "identifier", "params"),
                            CompatibilityInfo.Type.TYPE_ERROR, "schema type changed from string to long"));
    resourceTestErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "supports"),
                                         CompatibilityInfo.Type.ARRAY_NOT_CONTAIN,
                                         new StringArray(Arrays.asList("batch_get", "create", "delete", "get"))));
    resourceTestErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "methods"),
                                         CompatibilityInfo.Type.ARRAY_MISSING_ELEMENT, "batch_get"));
    resourceTestErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "finders", "search", "metadata", "type"),
                                                 CompatibilityInfo.Type.TYPE_ERROR, "schema type changed from array to int"));
    resourceTestErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "finders", "search", "assocKeys"),
                                                 CompatibilityInfo.Type.VALUE_NOT_EQUAL,
                                                 new StringArray(Arrays.asList("q", "s")), new StringArray(Arrays.asList("q", "changed_key"))));
    resourceTestErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "finders", "find_assocKey_downgrade", "assocKeys"),
                                                 CompatibilityInfo.Type.FINDER_ASSOCKEYS_DOWNGRADE));
    resourceTestErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "actions", "oneAction", "parameters", "bitfield", "items"),
                                                 CompatibilityInfo.Type.TYPE_ERROR, "schema type changed from boolean to int"));
    resourceTestErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "actions", "oneAction", "parameters", "someString", "type"),
                                                 CompatibilityInfo.Type.TYPE_UNKNOWN, sp.errorMessage()));
    resourceTestErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "actions", "oneAction", "parameters", "stringMap", "type"),
                                                 CompatibilityInfo.Type.TYPE_ERROR, "schema type changed from string to int"));
    resourceTestErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "entity", "actions", "anotherAction", "parameters"),
                                                 CompatibilityInfo.Type.ARRAY_MISSING_ELEMENT, "subMap"));
    resourceTestErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "entity", "actions", "exceptionTest", "throws"),
                                                 CompatibilityInfo.Type.ARRAY_NOT_CONTAIN,
                                                 new StringArray(Arrays.asList("com.linkedin.groups.api.GroupOwnerException", "java.io.FileNotFoundException"))));
    resourceTestErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "entity", "actions", "someAction", "parameters", "a", "optional"),
                                                 CompatibilityInfo.Type.PARAMETER_WRONG_OPTIONALITY));
    resourceTestErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "entity", "actions", "someAction", "parameters", "b", "type"),
                                                 CompatibilityInfo.Type.TYPE_ERROR, "schema type changed from string to int"));
    resourceTestErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "entity", "actions", "someAction", "parameters"),
                                                 CompatibilityInfo.Type.ARRAY_MISSING_ELEMENT, "e"));
    resourceTestErrors.add(new CompatibilityInfo(Arrays.<Object>asList("",
                                                               "collection",
                                                               "entity",
                                                               "actions",
                                                               "someAction",
                                                               "parameters"),
                                         CompatibilityInfo.Type.PARAMETER_NEW_REQUIRED,
                                         "f"));
    resourceTestErrors.add(new CompatibilityInfo(
      Arrays.<Object>asList("", "collection", "entity", "actions", "someAction", "returns"),
      CompatibilityInfo.Type.TYPE_MISSING));
    modelTestErrors.add(
      new CompatibilityInfo(Arrays.<Object>asList("com.linkedin.greetings.api.Greeting"),
                            CompatibilityInfo.Type.TYPE_BREAKS_NEW_READER,
                            "new record added required fields newField"));
    modelTestErrors.add(
      new CompatibilityInfo(Arrays.<Object>asList("com.linkedin.greetings.api.Greeting"),
                            CompatibilityInfo.Type.TYPE_BREAKS_OLD_READER,
                            "new record removed required fields message"));
    modelTestErrors.add(
      new CompatibilityInfo(Arrays.<Object>asList("com.linkedin.greetings.api.Greeting", "id", "string"),
                            CompatibilityInfo.Type.TYPE_BREAKS_NEW_AND_OLD_READERS,
                            "schema type changed from long to string"));

    final ResourceSchema prevResource = idlToResource(IDLS_SUFFIX + PREV_COLL_FILE);
    final  ResourceSchema currResource = idlToResource(IDLS_SUFFIX + CURR_COLL_FAIL_FILE);

    ResourceCompatibilityChecker checker = new ResourceCompatibilityChecker(prevResource, prevSchemaResolver,
                                                                            currResource, incompatSchemaResolver);

    Assert.assertFalse(checker.check(CompatibilityLevel.BACKWARDS));

    final Collection<CompatibilityInfo> resourceIncompatibles = new HashSet<CompatibilityInfo>(checker.getInfoMap().getRestSpecIncompatibles());
    for (CompatibilityInfo te : resourceTestErrors)
    {
      Assert.assertTrue(resourceIncompatibles.contains(te), "Reported resource incompatibles should contain: " + te.toString());
      resourceIncompatibles.remove(te);
    }
    Assert.assertTrue(resourceIncompatibles.isEmpty());

    final Collection<CompatibilityInfo> modelIncompatibles = new HashSet<CompatibilityInfo>(checker.getInfoMap().getModelIncompatibles());
    for (CompatibilityInfo te : modelTestErrors)
    {
      Assert.assertTrue(modelIncompatibles.contains(te), "Reported model incompatibles should contain: " + te.toString());
      modelIncompatibles.remove(te);
    }
    Assert.assertTrue(modelIncompatibles.isEmpty());

    // ignore compatibles
  }

  @Test
  public void testFailAssociationFile() throws IOException
  {
    final AssocKeySchema prevAssocKey = new AssocKeySchema();
    prevAssocKey.setName("key1");
    prevAssocKey.setType("string");

    final Collection<CompatibilityInfo> testErrors = new HashSet<CompatibilityInfo>();
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "association", "assocKeys"),
                                         CompatibilityInfo.Type.ARRAY_NOT_EQUAL,
                                         new AssocKeySchemaArray(Arrays.asList(prevAssocKey))));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "association", "supports"),
                                         CompatibilityInfo.Type.ARRAY_NOT_CONTAIN,
                                         new StringArray(Arrays.asList("create", "get"))));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "association", "methods", "create", "parameters"),
                                         CompatibilityInfo.Type.PARAMETER_NEW_REQUIRED, "data"));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "association", "methods"),
                                         CompatibilityInfo.Type.ARRAY_MISSING_ELEMENT, "get"));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "association", "entity", "path"),
                                         CompatibilityInfo.Type.VALUE_NOT_EQUAL,
                                         "/greetings/assoc/{greetingsId}",
                                         "/greetings/association/{greetingsId}"));

    final ResourceSchema prevResource = idlToResource(IDLS_SUFFIX + PREV_ASSOC_FILE);
    final ResourceSchema currResource = idlToResource(IDLS_SUFFIX + CURR_ASSOC_FAIL_FILE);

    ResourceCompatibilityChecker checker = new ResourceCompatibilityChecker(prevResource, prevSchemaResolver,
                                                                            currResource, prevSchemaResolver);

    Assert.assertFalse(checker.check(CompatibilityLevel.BACKWARDS));

    final Collection<CompatibilityInfo> incompatibles = new HashSet<CompatibilityInfo>(checker.getInfoMap().getIncompatibles());

    for (CompatibilityInfo te : testErrors)
    {
      Assert.assertTrue(incompatibles.contains(te), "Reported incompatibles should contain: " + te.toString());
      incompatibles.remove(te);
    }

    Assert.assertTrue(incompatibles.isEmpty());
  }

  @Test
  public void testFailSimpleFile() throws IOException
  {
    final Collection<CompatibilityInfo> resourceTestErrors = new HashSet<CompatibilityInfo>();
    final Collection<CompatibilityInfo> modelTestErrors = new HashSet<CompatibilityInfo>();

    resourceTestErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "simple", "supports"),
                                                 CompatibilityInfo.Type.ARRAY_NOT_CONTAIN,
                                                 new StringArray(Arrays.asList("delete", "get"))));
    resourceTestErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "simple", "methods"),
                                                 CompatibilityInfo.Type.ARRAY_MISSING_ELEMENT, "delete"));
    resourceTestErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "simple", "methods", "get", "parameters", "param1", "type"),
                                                 CompatibilityInfo.Type.TYPE_ERROR, "schema type changed from string to int"));
    resourceTestErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "simple", "actions", "oneAction", "parameters", "bitfield", "items"),
                                                 CompatibilityInfo.Type.TYPE_ERROR, "schema type changed from boolean to int"));
    resourceTestErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "simple", "actions", "oneAction", "parameters"),
                                                 CompatibilityInfo.Type.ARRAY_MISSING_ELEMENT, "someString"));
    resourceTestErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "simple", "actions", "oneAction", "parameters"),
                                                 CompatibilityInfo.Type.PARAMETER_NEW_REQUIRED, "someStringNew"));
    modelTestErrors.add(new CompatibilityInfo(Arrays.<Object>asList("com.linkedin.greetings.api.Greeting"),
                                              CompatibilityInfo.Type.TYPE_BREAKS_NEW_READER, "new record added required fields newField"));
    modelTestErrors.add(new CompatibilityInfo(Arrays.<Object>asList("com.linkedin.greetings.api.Greeting"),
                                              CompatibilityInfo.Type.TYPE_BREAKS_OLD_READER, "new record removed required fields message"));
    modelTestErrors.add(new CompatibilityInfo(Arrays.<Object>asList("com.linkedin.greetings.api.Greeting", "id", "string"),
                                              CompatibilityInfo.Type.TYPE_BREAKS_NEW_AND_OLD_READERS, "schema type changed from long to string"));

    final ResourceSchema prevResource = idlToResource(IDLS_SUFFIX + PREV_SIMPLE_FILE);
    final ResourceSchema currResource = idlToResource(IDLS_SUFFIX + CURR_SIMPLE_FAIL_FILE);

    ResourceCompatibilityChecker checker = new ResourceCompatibilityChecker(prevResource, prevSchemaResolver,
                                                                            currResource, incompatSchemaResolver);

    Assert.assertFalse(checker.check(CompatibilityLevel.BACKWARDS));

    final Collection<CompatibilityInfo> resourceIncompatible = new HashSet<CompatibilityInfo>(checker.getInfoMap().getRestSpecIncompatibles());

    for (CompatibilityInfo te : resourceTestErrors)
    {
      Assert.assertTrue(resourceIncompatible.contains(te), "Reported resource incompatibles should contain: " + te.toString());
      resourceIncompatible.remove(te);
    }

    Assert.assertTrue(resourceIncompatible.isEmpty());

    final Collection<CompatibilityInfo> modelIncompatible = new HashSet<CompatibilityInfo>(checker.getInfoMap().getModelIncompatibles());

    for (CompatibilityInfo te : modelTestErrors)
    {
      Assert.assertTrue(modelIncompatible.contains(te), "Reported model incompatibles should contain: " + te.toString());
      modelIncompatible.remove(te);
    }

    Assert.assertTrue(modelIncompatible.isEmpty());

    // ignore compatibles
  }

  @Test
  public void testFailActionsSetFile() throws IOException
  {
    final Collection<CompatibilityInfo> testErrors = new HashSet<CompatibilityInfo>();
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList(""),
                                         CompatibilityInfo.Type.VALUE_WRONG_OPTIONALITY, "actionsSet"));

    ResourceSchema prevResource = idlToResource(IDLS_SUFFIX + PREV_AS_FILE);
    ResourceSchema currResource = idlToResource(IDLS_SUFFIX + CURR_AS_FAIL_FILE);

    ResourceCompatibilityChecker checker = new ResourceCompatibilityChecker(prevResource, prevSchemaResolver,
                                                                            currResource, prevSchemaResolver);

    Assert.assertFalse(checker.check(CompatibilityLevel.BACKWARDS));

    final Collection<CompatibilityInfo> incompatibles = new HashSet<CompatibilityInfo>(checker.getInfoMap().getIncompatibles());
    final Collection<CompatibilityInfo> compatibles = new HashSet<CompatibilityInfo>(checker.getInfoMap().getCompatibles());

    for (CompatibilityInfo te : testErrors)
    {
      Assert.assertTrue(incompatibles.contains(te), "Reported incompatibles should contain: " + te.toString());
      incompatibles.remove(te);
    }

    Assert.assertTrue(incompatibles.isEmpty());
    Assert.assertTrue(compatibles.isEmpty());
  }

  @Test
  public void testPassActionsSetFile() throws IOException
  {
    final Collection<CompatibilityInfo> testDiffs = new HashSet<CompatibilityInfo>();
    testDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "doc"),
                                        CompatibilityInfo.Type.DOC_NOT_EQUAL));
    testDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "actionsSet", "actions", "handshake", "parameters"),
                                        CompatibilityInfo.Type.PARAMETER_NEW_OPTIONAL, "param"));
    testDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "actionsSet", "actions", "handshake", "parameters", "me", "doc"),
                                        CompatibilityInfo.Type.DOC_NOT_EQUAL));

    final ResourceSchema prevResource = idlToResource(IDLS_SUFFIX + PREV_AS_FILE);
    final ResourceSchema currResource = idlToResource(IDLS_SUFFIX + CURR_AS_PASS_FILE);

    ResourceCompatibilityChecker checker = new ResourceCompatibilityChecker(prevResource, prevSchemaResolver,
                                                                            currResource, prevSchemaResolver);

    Assert.assertTrue(checker.check(CompatibilityLevel.BACKWARDS));

    final Collection<CompatibilityInfo> incompatibles = checker.getInfoMap().getIncompatibles();
    Assert.assertTrue(incompatibles.isEmpty());

    final Collection<CompatibilityInfo> compatibles = new HashSet<CompatibilityInfo>(checker.getInfoMap().getCompatibles());

    for (CompatibilityInfo td : testDiffs)
    {
      Assert.assertTrue(compatibles.contains(td), "Reported compatibles should contain: " + td.toString());
      compatibles.remove(td);
    }

    Assert.assertTrue(compatibles.isEmpty());
  }

  private ResourceSchema idlToResource(String path) throws IOException
  {
    final InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(path);
    return _codec.readResourceSchema(resourceStream);
  }

  private void bindSchemaResolvers()
  {
    StringBuilder errors = new StringBuilder();

    Name toneName = new Name("com.linkedin.greetings.api.Tone");
    EnumDataSchema tone = new EnumDataSchema(toneName);
    List<String> symbols = new ArrayList<String>();
    symbols.add("FRIENDLY");
    symbols.add("SINCERE");
    symbols.add("INSULTING");
    tone.setSymbols(symbols, errors);

    Name greetingName = new Name("com.linkedin.greetings.api.Greeting");
    RecordDataSchema prevGreeting = new RecordDataSchema(greetingName, RecordDataSchema.RecordType.RECORD);
    List<RecordDataSchema.Field> oldFields = new ArrayList<RecordDataSchema.Field>();
    RecordDataSchema.Field id = new RecordDataSchema.Field(new LongDataSchema());
    id.setName("id", errors);
    oldFields.add(id);
    RecordDataSchema.Field message = new RecordDataSchema.Field(new StringDataSchema());
    message.setName("message", errors);
    oldFields.add(message);
    RecordDataSchema.Field toneField = new RecordDataSchema.Field(tone);
    toneField.setName("tone", errors);
    toneField.setOptional(true);
    oldFields.add(toneField);
    prevGreeting.setFields(oldFields, errors);

    prevSchemaResolver.bindNameToSchema(toneName, tone, null);
    prevSchemaResolver.bindNameToSchema(greetingName, prevGreeting, null);

    // compat greeting has removed optional field "tone" and added a new optional field "newField"
    RecordDataSchema compatGreeting = new RecordDataSchema(greetingName, RecordDataSchema.RecordType.RECORD);
    List<RecordDataSchema.Field> compatFields = new ArrayList<RecordDataSchema.Field>();
    compatFields.add(id);
    compatFields.add(message);
    RecordDataSchema.Field newCompatField = new RecordDataSchema.Field(new StringDataSchema());
    newCompatField.setName("newField", errors);
    newCompatField.setOptional(true);
    compatFields.add(newCompatField);
    compatGreeting.setFields(compatFields, errors);

    compatSchemaResolver.bindNameToSchema(toneName, tone, null);
    compatSchemaResolver.bindNameToSchema(greetingName, compatGreeting, null);

    // incompat greeting has removed non-optional field "message",
    // has changed the type of "id" to string,
    // and added a new non-optional field "newField"
    RecordDataSchema incompatGreeting = new RecordDataSchema(greetingName, RecordDataSchema.RecordType.RECORD);
    List<RecordDataSchema.Field> incompatFields = new ArrayList<RecordDataSchema.Field>();
    RecordDataSchema.Field incompatId = new RecordDataSchema.Field(new StringDataSchema());
    incompatId.setName("id", errors);
    oldFields.add(incompatId);
    incompatFields.add(incompatId);
    incompatFields.add(toneField);
    RecordDataSchema.Field newIncompatField = new RecordDataSchema.Field(new StringDataSchema());
    newIncompatField.setName("newField", errors);
    incompatFields.add(newIncompatField);
    incompatGreeting.setFields(incompatFields, errors);

    incompatSchemaResolver.bindNameToSchema(toneName, tone, null);
    incompatSchemaResolver.bindNameToSchema(greetingName, incompatGreeting, null);
  }

  private static final String IDLS_SUFFIX = "idls" + File.separator;
  private static final String PEGASUS_SUFFIX = "pegasus" + File.separator;
  private static final String RESOURCES_SUFFIX = "src" + File.separator + "test" + File.separator + "resources" + File.separator;

  private static final String PREV_COLL_FILE = "prev-greetings-coll.restspec.json";
  private static final String PREV_ASSOC_FILE = "prev-greetings-assoc.restspec.json";
  private static final String PREV_AS_FILE = "prev-greetings-as.restspec.json";
  private static final String PREV_SIMPLE_FILE = "prev-greeting-simple.restspec.json";
  private static final String CURR_COLL_PASS_FILE = "curr-greetings-coll-pass.restspec.json";
  private static final String CURR_ASSOC_PASS_FILE = "curr-greetings-assoc-pass.restspec.json";
  private static final String CURR_SIMPLE_PASS_FILE = "curr-greeting-simple-pass.restspec.json";
  private static final String CURR_COLL_FAIL_FILE = "curr-greetings-coll-fail.restspec.json";
  private static final String CURR_ASSOC_FAIL_FILE = "curr-greetings-assoc-fail.restspec.json";
  private static final String CURR_SIMPLE_FAIL_FILE = "curr-greeting-simple-fail.restspec.json";
  private static final String CURR_AS_FAIL_FILE = "curr-greetings-as-fail.restspec.json";
  private static final String CURR_AS_PASS_FILE = "curr-greetings-as-pass.restspec.json";

  private static final RestSpecCodec _codec = new RestSpecCodec();

  private DataSchemaResolver prevSchemaResolver;
  private DataSchemaResolver compatSchemaResolver;
  private DataSchemaResolver incompatSchemaResolver;
}
