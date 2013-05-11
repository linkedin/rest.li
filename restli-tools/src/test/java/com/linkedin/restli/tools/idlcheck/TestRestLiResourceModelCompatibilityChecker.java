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

package com.linkedin.restli.tools.idlcheck;


import com.linkedin.data.Data;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.template.StringArray;
import com.linkedin.restli.restspec.AssocKeySchema;
import com.linkedin.restli.restspec.AssocKeySchemaArray;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;


public class TestRestLiResourceModelCompatibilityChecker
{
  public TestRestLiResourceModelCompatibilityChecker()
  {
    String projectDir = System.getProperty(PROJECT_DIR_PROP);
    if (projectDir == null)
    {
      projectDir = "restli-tools";
    }

    final String RESOURCES_DIR = projectDir + File.separator + RESOURCES_SUFFIX;
    IDLS_DIR = RESOURCES_DIR + IDLS_SUFFIX;

    if (System.getProperty("generator.resolver.path") == null)
    {
      System.setProperty("generator.resolver.path", RESOURCES_DIR + PEGASUS_SUFFIX);
    }
  }

  @Test
  public void testPassCollectionFile()
  {
    final Collection<CompatibilityInfo> testDiffs = new HashSet<CompatibilityInfo>();
    testDiffs.add(new CompatibilityInfo(Arrays.<Object>asList(""),
                                        CompatibilityInfo.Type.OPTIONAL_VALUE, "namespace"));
    testDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "supports"),
                                        CompatibilityInfo.Type.SUPERSET, Arrays.asList("update")));
    testDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "finders", "search", "parameters", "tone"),
                                        CompatibilityInfo.Type.OPTIONAL_PARAMETER));
    testDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "finders", "search", "parameters"),
                                        CompatibilityInfo.Type.PARAMETER_NEW_OPTIONAL, "newParam"));
    testDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "actions", "oneAction", "parameters"),
                                        CompatibilityInfo.Type.PARAMETER_NEW_OPTIONAL, "newParam"));
    testDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "actions", "oneAction", "parameters", "someString"),
                                        CompatibilityInfo.Type.OPTIONAL_PARAMETER));
    testDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "actions", "exceptionTest", "throws"),
                                        CompatibilityInfo.Type.SUPERSET, Arrays.asList("java.lang.NullPointerException")));
    testDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "entity", "actions", "someAction", "parameters", "b", "default"),
                                        CompatibilityInfo.Type.VALUE_DIFFERENT, "default", "changed"));

    final RestLiResourceModelCompatibilityChecker checker = new RestLiResourceModelCompatibilityChecker();

    Assert.assertTrue(checker.check(IDLS_DIR + PREV_COLL_FILE,
                                    IDLS_DIR + CURR_COLL_PASS_FILE,
                                    CompatibilityLevel.BACKWARDS));

    final Collection<CompatibilityInfo> incompatibles = checker.getIncompatibles();
    final Collection<CompatibilityInfo> compatibles = new HashSet<CompatibilityInfo>(checker.getCompatibles());

    for (CompatibilityInfo di : testDiffs)
    {
      Assert.assertTrue(compatibles.contains(di), "Reported compatibles should contain: " + di.toString());
      compatibles.remove(di);
    }

    Assert.assertTrue(incompatibles.isEmpty());
    Assert.assertTrue(compatibles.isEmpty());
  }

  @Test
  public void testPassAssociationFile()
  {
    final Collection<CompatibilityInfo> testDiffs = new HashSet<CompatibilityInfo>();
    testDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "association", "methods", "create", "parameters"),
                                        CompatibilityInfo.Type.PARAMETER_NEW_OPTIONAL, "type"));

    final RestLiResourceModelCompatibilityChecker checker = new RestLiResourceModelCompatibilityChecker();

    Assert.assertTrue(checker.check(IDLS_DIR + PREV_ASSOC_FILE,
                                    IDLS_DIR + CURR_ASSOC_PASS_FILE,
                                    CompatibilityLevel.BACKWARDS));

    final Collection<CompatibilityInfo> incompatibles = checker.getIncompatibles();
    final Collection<CompatibilityInfo> compatibles = new HashSet<CompatibilityInfo>(checker.getCompatibles());

    for (CompatibilityInfo di : testDiffs)
    {
      Assert.assertTrue(compatibles.contains(di), "Reported compatibles should contain: " + di.toString());
      compatibles.remove(di);
    }

    Assert.assertTrue(incompatibles.isEmpty());
    Assert.assertTrue(compatibles.isEmpty());
  }

  @Test
  public void testFailCollectionFile()
  {
    final SchemaParser sp = new SchemaParser();
    sp.parse("\"StringRef\"");

    final Collection<CompatibilityInfo> testErrors = new HashSet<CompatibilityInfo>();
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "identifier", "params"),
                                         CompatibilityInfo.Type.TYPE_INCOMPATIBLE, "string", "long"));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "supports"),
                                         CompatibilityInfo.Type.ARRAY_NOT_CONTAIN,
                                         new StringArray(Arrays.asList("batch_get", "create", "delete", "get"))));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "methods"),
                                         CompatibilityInfo.Type.ARRAY_MISSING_ELEMENT, "batch_get"));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "finders", "search", "metadata", "type"),
                                         CompatibilityInfo.Type.TYPE_INCOMPATIBLE,
                                         "{\"items\":\"int\",\"type\":\"array\"}",
                                         "int"));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "finders", "search", "assocKeys"),
                                         CompatibilityInfo.Type.VALUE_NOT_EQUAL,
                                         new StringArray(Arrays.asList("q", "s")),
                                         new StringArray(Arrays.asList("q", "changed_key"))));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "finders", "find_assocKey_downgrade", "assocKeys"),
                                         CompatibilityInfo.Type.FINDER_ASSOCKEYS_DOWNGRADE));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "actions", "oneAction", "parameters", "someString", "type"),
                                         CompatibilityInfo.Type.TYPE_UNKNOWN,
                                         sp.errorMessage()));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "actions", "oneAction", "parameters", "stringMap", "type"),
                                         CompatibilityInfo.Type.TYPE_INCOMPATIBLE,
                                         "{\"values\":\"string\",\"type\":\"map\"}",
                                         "{\"values\":\"int\",\"type\":\"map\"}"));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "entity", "actions", "anotherAction", "parameters"),
                                         CompatibilityInfo.Type.ARRAY_MISSING_ELEMENT, "subMap"));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "entity", "actions", "exceptionTest", "throws"),
                                         CompatibilityInfo.Type.ARRAY_NOT_CONTAIN,
                                         new StringArray(Arrays.asList("com.linkedin.groups.api.GroupOwnerException",
                                                                       "java.io.FileNotFoundException"))));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "entity", "actions", "someAction", "parameters", "a", "optional"),
                                         CompatibilityInfo.Type.PARAMETER_WRONG_OPTIONALITY));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "entity", "actions", "someAction", "parameters", "b", "type"),
                                         CompatibilityInfo.Type.TYPE_INCOMPATIBLE, "string", "int"));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "entity", "actions", "someAction", "parameters"),
                                         CompatibilityInfo.Type.ARRAY_MISSING_ELEMENT, "e"));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("",
                                                               "collection",
                                                               "entity",
                                                               "actions",
                                                               "someAction",
                                                               "parameters"),
                                         CompatibilityInfo.Type.PARAMETER_NEW_REQUIRED,
                                         "f"));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "entity", "actions", "someAction", "returns"),
                                         CompatibilityInfo.Type.TYPE_MISSING));

    final RestLiResourceModelCompatibilityChecker checker = new RestLiResourceModelCompatibilityChecker();

    Assert.assertFalse(checker.check(IDLS_DIR + PREV_COLL_FILE,
                                     IDLS_DIR + CURR_COLL_FAIL_FILE,
                                     CompatibilityLevel.BACKWARDS));

    final Collection<CompatibilityInfo> incompatible = new HashSet<CompatibilityInfo>(checker.getIncompatibles());

    for (CompatibilityInfo te : testErrors)
    {
      Assert.assertTrue(incompatible.contains(te), "Reported incompatibles should contain: " + te.toString());
      incompatible.remove(te);
    }

    Assert.assertTrue(incompatible.isEmpty());
  }

  @Test
  public void testFailAssociationFile()
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
                                         "/greetings/assoc/{id}",
                                         "/greetings/association/{id}"));

    final RestLiResourceModelCompatibilityChecker checker = new RestLiResourceModelCompatibilityChecker();

    Assert.assertFalse(checker.check(IDLS_DIR + PREV_ASSOC_FILE,
                                     IDLS_DIR + CURR_ASSOC_FAIL_FILE,
                                     CompatibilityLevel.BACKWARDS));

    final Collection<CompatibilityInfo> incompatibles = new HashSet<CompatibilityInfo>(checker.getIncompatibles());

    for (CompatibilityInfo te : testErrors)
    {
      Assert.assertTrue(incompatibles.contains(te), "Reported incompatibles should contain: " + te.toString());
      incompatibles.remove(te);
    }

    Assert.assertTrue(incompatibles.isEmpty());
  }

  @Test
  public void testFailActionsSetFile()
  {
    final Collection<CompatibilityInfo> testErrors = new HashSet<CompatibilityInfo>();
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList(""),
                                         CompatibilityInfo.Type.VALUE_WRONG_OPTIONALITY, "actionsSet"));

    final RestLiResourceModelCompatibilityChecker checker = new RestLiResourceModelCompatibilityChecker();

    Assert.assertFalse(checker.check(IDLS_DIR + PREV_AS_FILE,
                                     IDLS_DIR + CURR_AS_FAIL_FILE,
                                     CompatibilityLevel.BACKWARDS));

    final Collection<CompatibilityInfo> incompatibles = new HashSet<CompatibilityInfo>(checker.getIncompatibles());

    for (CompatibilityInfo te : testErrors)
    {
      Assert.assertTrue(incompatibles.contains(te), "Reported incompatibles should contain: " + te.toString());
      incompatibles.remove(te);
    }

    Assert.assertTrue(incompatibles.isEmpty());
  }

  @Test
  public void testPassActionsSetFile()
  {
    final RestLiResourceModelCompatibilityChecker checker = new RestLiResourceModelCompatibilityChecker();

    Assert.assertTrue(checker.check(IDLS_DIR + PREV_AS_FILE,
                                    IDLS_DIR + CURR_AS_PASS_FILE,
                                    CompatibilityLevel.BACKWARDS));

    final Collection<CompatibilityInfo> incompatibles = checker.getIncompatibles();
    Assert.assertTrue(incompatibles.isEmpty());
  }

  @Test
  public void testFileNotFound()
  {
    final String nonExistentFilename1 = "NonExistentFile1";
    final String nonExistentFilename2 = "NonExistentFile2";
    final Collection<CompatibilityInfo> testIncompatibles = new HashSet<CompatibilityInfo>();
    final Collection<CompatibilityInfo> testCompatibles = new HashSet<CompatibilityInfo>();

    testIncompatibles.add(new CompatibilityInfo(Arrays.<Object>asList(""),
                                                 CompatibilityInfo.Type.RESOURCE_MISSING,
                                                 nonExistentFilename1));
    testCompatibles.add(new CompatibilityInfo(Arrays.<Object>asList(""),
                                                 CompatibilityInfo.Type.RESOURCE_NEW,
                                                 nonExistentFilename2));

    final RestLiResourceModelCompatibilityChecker checker = new RestLiResourceModelCompatibilityChecker();

    Assert.assertFalse(checker.check(nonExistentFilename1,
                                     nonExistentFilename2,
                                     CompatibilityLevel.BACKWARDS));

    final Collection<CompatibilityInfo> incompatibles = new HashSet<CompatibilityInfo>(checker.getIncompatibles());
    final Collection<CompatibilityInfo> compatibles = new HashSet<CompatibilityInfo>(checker.getCompatibles());

    for (CompatibilityInfo te : incompatibles)
    {
      Assert.assertTrue(testIncompatibles.contains(te), "Reported incompatibles should contain: " + te.toString());
      incompatibles.remove(te);
    }

    for (CompatibilityInfo di : compatibles)
    {
      Assert.assertTrue(testCompatibles.contains(di), "Reported compatibles should contain: " + di.toString());
      compatibles.remove(di);
    }
  }

  private static final String IDLS_SUFFIX = "idls" + File.separator;
  private static final String PEGASUS_SUFFIX = "pegasus" + File.separator;
  private static final String RESOURCES_SUFFIX = "src" + File.separator + "test" + File.separator + "resources" + File.separator;
  private static final String PROJECT_DIR_PROP = "test.projectDir";
  private static final String PREV_COLL_FILE = "prev-greetings-coll.restspec.json";
  private static final String PREV_ASSOC_FILE = "prev-greetings-assoc.restspec.json";
  private static final String PREV_AS_FILE = "prev-greetings-as.restspec.json";
  private static final String CURR_COLL_PASS_FILE = "curr-greetings-coll-pass.restspec.json";
  private static final String CURR_ASSOC_PASS_FILE = "curr-greetings-assoc-pass.restspec.json";
  private static final String CURR_COLL_FAIL_FILE = "curr-greetings-coll-fail.restspec.json";
  private static final String CURR_ASSOC_FAIL_FILE = "curr-greetings-assoc-fail.restspec.json";
  private static final String CURR_AS_FAIL_FILE = "curr-greetings-as-fail.restspec.json";
  private static final String CURR_AS_PASS_FILE = "curr-greetings-as-pass.restspec.json";

  private final String IDLS_DIR;
}
