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

import com.linkedin.data.template.StringArray;
import com.linkedin.restli.restspec.AssocKeySchema;
import com.linkedin.restli.restspec.AssocKeySchemaArray;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    final ArrayList<CompatibilityInfo> testDiffs = new ArrayList<CompatibilityInfo>();
    testDiffs.add(new CompatibilityInfo(Arrays.<Object>asList(""),
                                        CompatibilityInfo.Type.OPTIONAL_VALUE, "namespace"));
    testDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "supports"),
                                        CompatibilityInfo.Type.SUPERSET, Arrays.asList("update")));
    testDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "finders", "search", "parameters", "tone"),
                                        CompatibilityInfo.Type.OPTIONAL_PARAMETER));
    testDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "finders", "search", "parameters"),
                                        CompatibilityInfo.Type.PARAMETER_NEW_OPTIONAL, "newParam"));
    testDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "actions", "anotherAction", "parameters"),
                                        CompatibilityInfo.Type.PARAMETER_NEW_OPTIONAL, "newParam"));
    testDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "actions", "exceptionTest", "throws"),
                                        CompatibilityInfo.Type.SUPERSET, Arrays.asList("java.lang.NullPointerException")));
    testDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "entity", "actions", "someAction", "parameters", "c"),
                                        CompatibilityInfo.Type.OPTIONAL_VALUE, "optional"));

    final RestLiResourceModelCompatibilityChecker checker = new RestLiResourceModelCompatibilityChecker();

    Assert.assertTrue(checker.check(IDLS_DIR + PREV_COLL_FILE,
                                    IDLS_DIR + CURR_COLL_PASS_FILE,
                                    CompatibilityLevel.BACKWARDS));

    final List<CompatibilityInfo> unableToChecks = checker.getUnableToChecks();
    final List<CompatibilityInfo> incompatibles = checker.getIncompatibles();
    final List<CompatibilityInfo> compatibles = checker.getCompatibles();

    for (CompatibilityInfo di : testDiffs)
    {
      Assert.assertTrue(compatibles.contains(di), "Reported compatibles should contain: " + di.toString());
    }

    Assert.assertEquals(unableToChecks.size(), 0);
    Assert.assertEquals(incompatibles.size(), 0);
    Assert.assertEquals(compatibles.size(), testDiffs.size());
  }

  @Test
  public void testPassAssociationFile()
  {
    final ArrayList<CompatibilityInfo> testDiffs = new ArrayList<CompatibilityInfo>();
    testDiffs.add(new CompatibilityInfo(Arrays.<Object>asList("", "association", "methods", "create", "parameters"),
                                        CompatibilityInfo.Type.PARAMETER_NEW_OPTIONAL, "type"));

    final RestLiResourceModelCompatibilityChecker checker = new RestLiResourceModelCompatibilityChecker();

    Assert.assertTrue(checker.check(IDLS_DIR + PREV_ASSOC_FILE,
                                    IDLS_DIR + CURR_ASSOC_PASS_FILE,
                                    CompatibilityLevel.BACKWARDS));

    final List<CompatibilityInfo> unableToChecks = checker.getUnableToChecks();
    final List<CompatibilityInfo> incompatibles = checker.getIncompatibles();
    final List<CompatibilityInfo> compatibles = checker.getCompatibles();

    for (CompatibilityInfo di : testDiffs)
    {
      Assert.assertTrue(compatibles.contains(di), "Reported compatibles should contain: " + di.toString());
    }

    Assert.assertEquals(unableToChecks.size(), 0);
    Assert.assertEquals(incompatibles.size(), 0);
    Assert.assertEquals(compatibles.size(), testDiffs.size());
  }

  @Test
  public void testFailCollectionFile()
  {
    final ArrayList<CompatibilityInfo> testErrors = new ArrayList<CompatibilityInfo>();
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
    testErrors.add(new PartialMessageCompatibilityInfo(Arrays.<Object>asList("", "collection", "actions", "anotherAction", "parameters", "someString", "type"),
                                                       CompatibilityInfo.Type.TYPE_UNKNOWN,
                                                       new String[] {"StringRef", "cannot be resolved"}));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "actions", "anotherAction", "parameters", "someString"),
                                         CompatibilityInfo.Type.VALUE_WRONG_OPTIONALITY, "default"));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "actions", "anotherAction", "parameters", "stringMap", "type"),
                                         CompatibilityInfo.Type.TYPE_INCOMPATIBLE,
                                         "{\"values\":\"string\",\"type\":\"map\"}",
                                         "{\"values\":\"int\",\"type\":\"map\"}"));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "entity", "actions", "anotherAction", "parameters"),
                                         CompatibilityInfo.Type.ARRAY_MISSING_ELEMENT, "stringMap"));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "entity", "actions", "exceptionTest", "throws"),
                                         CompatibilityInfo.Type.ARRAY_NOT_CONTAIN,
                                         new StringArray(Arrays.asList("com.linkedin.groups.api.GroupOwnerException",
                                                                       "java.io.FileNotFoundException"))));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "entity", "actions", "someAction", "parameters", "b", "type"),
                                         CompatibilityInfo.Type.TYPE_INCOMPATIBLE, "string", "int"));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "entity", "actions", "someAction", "parameters", "b", "default"),
                                         CompatibilityInfo.Type.VALUE_NOT_EQUAL, "default", "0"));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "entity", "actions", "someAction", "parameters", "c", "optional"),
                                         CompatibilityInfo.Type.PARAMETER_WRONG_OPTIONALITY));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "entity", "actions", "someAction", "parameters"),
                                         CompatibilityInfo.Type.ARRAY_MISSING_ELEMENT, "e"));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "entity", "actions", "someAction", "parameters"),
                                         CompatibilityInfo.Type.PARAMETER_NEW_REQUIRED, "f"));
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList("", "collection", "entity", "actions", "someAction", "returns"),
                                         CompatibilityInfo.Type.TYPE_MISSING));

    final RestLiResourceModelCompatibilityChecker checker = new RestLiResourceModelCompatibilityChecker();

    Assert.assertFalse(checker.check(IDLS_DIR + PREV_COLL_FILE,
                                     IDLS_DIR + CURR_COLL_FAIL_FILE,
                                     CompatibilityLevel.BACKWARDS));

    final List<CompatibilityInfo> unableToChecks = checker.getUnableToChecks();
    final List<CompatibilityInfo> incompatibles = checker.getIncompatibles();

    for (CompatibilityInfo te : testErrors)
    {
      Assert.assertTrue(incompatibles.contains(te), "Reported incompatibles should contain: " + te.toString());
    }

    Assert.assertEquals(unableToChecks.size(), 0);
    Assert.assertEquals(incompatibles.size(), testErrors.size());
  }

  @Test
  public void testFailAssociationFile()
  {
    final AssocKeySchema prevAssocKey = new AssocKeySchema();
    prevAssocKey.setName("key1");
    prevAssocKey.setType("string");

    final ArrayList<CompatibilityInfo> testErrors = new ArrayList<CompatibilityInfo>();
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

    final List<CompatibilityInfo> unableToChecks = checker.getUnableToChecks();
    final List<CompatibilityInfo> incompatibles = checker.getIncompatibles();

    for (CompatibilityInfo te : testErrors)
    {
      Assert.assertTrue(incompatibles.contains(te), "Reported incompatibles should contain: " + te.toString());
    }

    Assert.assertEquals(unableToChecks.size(), 0);
    Assert.assertEquals(incompatibles.size(), testErrors.size());
  }

  @Test
  public void testFailActionsSetFile()
  {
    final ArrayList<CompatibilityInfo> testErrors = new ArrayList<CompatibilityInfo>();
    testErrors.add(new CompatibilityInfo(Arrays.<Object>asList(""),
                                         CompatibilityInfo.Type.VALUE_WRONG_OPTIONALITY, "actionsSet"));

    final RestLiResourceModelCompatibilityChecker checker = new RestLiResourceModelCompatibilityChecker();

    Assert.assertFalse(checker.check(IDLS_DIR + PREV_AS_FILE,
                                     IDLS_DIR + CURR_AS_FAIL_FILE,
                                     CompatibilityLevel.BACKWARDS));

    final List<CompatibilityInfo> unableToChecks = checker.getUnableToChecks();
    final List<CompatibilityInfo> incompatibles = checker.getIncompatibles();

    for (CompatibilityInfo te : testErrors)
    {
      Assert.assertTrue(incompatibles.contains(te), "Reported incompatibles should contain: " + te.toString());
    }

    Assert.assertEquals(unableToChecks.size(), 0);
    Assert.assertEquals(incompatibles.size(), testErrors.size());
  }

  @Test
  public void testPassActionsSetFile()
  {
    final RestLiResourceModelCompatibilityChecker checker = new RestLiResourceModelCompatibilityChecker();

    Assert.assertTrue(checker.check(IDLS_DIR + PREV_AS_FILE,
                                    IDLS_DIR + CURR_AS_PASS_FILE,
                                    CompatibilityLevel.BACKWARDS));

    final List<CompatibilityInfo> unableToChecks = checker.getUnableToChecks();
    final List<CompatibilityInfo> incompatibles = checker.getIncompatibles();

    Assert.assertEquals(unableToChecks.size(), 0);
    Assert.assertEquals(incompatibles.size(), 0);
  }

  @Test
  public void testFileNotFound()
  {
    final String nonExistentFilename1 = "NonExistentFile1";
    final String nonExistentFilename2 = "NonExistentFile2";
    final ArrayList<CompatibilityInfo> testUnableToChecks = new ArrayList<CompatibilityInfo>();
    final ArrayList<CompatibilityInfo> testErrors = new ArrayList<CompatibilityInfo>();

    testUnableToChecks.add(new CompatibilityInfo(Arrays.<Object>asList(""),
                                                 CompatibilityInfo.Type.FILE_NOT_FOUND,
                                                 nonExistentFilename1));
    testUnableToChecks.add(new CompatibilityInfo(Arrays.<Object>asList(""),
                                                 CompatibilityInfo.Type.FILE_NOT_FOUND,
                                                 nonExistentFilename2));

    final RestLiResourceModelCompatibilityChecker checker = new RestLiResourceModelCompatibilityChecker();

    Assert.assertFalse(checker.check(nonExistentFilename1,
                                     nonExistentFilename2,
                                     CompatibilityLevel.BACKWARDS));

    final List<CompatibilityInfo> unableToChecks = checker.getUnableToChecks();
    final List<CompatibilityInfo> incompatibles = checker.getIncompatibles();

    for (CompatibilityInfo tutc : testUnableToChecks)
    {
      Assert.assertTrue(unableToChecks.contains(tutc), "Reported unable-to-checks should contain: " + tutc.toString());
    }
    for (CompatibilityInfo te : testErrors)
    {
      Assert.assertTrue(incompatibles.contains(te), "Reported incompatibles should contain: " + te.toString());
    }

    Assert.assertEquals(unableToChecks.size(), testUnableToChecks.size());
  }

  private class PartialMessageCompatibilityInfo extends CompatibilityInfo
  {
    public PartialMessageCompatibilityInfo(List<Object> path, Type type, String[] messageTokens)
    {
      super(path, type);
      _messageTokens = messageTokens;
    }

    @Override
    public int hashCode()
    {
      return new HashCodeBuilder(17, 29).
          append(_type).
          append(_path).
          toHashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
      if (this == obj)
      {
        return true;
      }

      if (obj == null)
      {
        return false;
      }

      if (!obj.getClass().isAssignableFrom(getClass()))
      {
        return false;
      }

      final CompatibilityInfo other = (CompatibilityInfo) obj;
      if (!new EqualsBuilder().
          append(_type, other._type).
          append(_path, other._path).
          isEquals())
      {
        return false;
      }

      assert(other._parameters.length == 1);

      for (String token: _messageTokens)
      {
        if (!((String) other._parameters[0]).contains(token))
        {
          return false;
        }
      }

      return true;
    }

    private String[] _messageTokens;
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
