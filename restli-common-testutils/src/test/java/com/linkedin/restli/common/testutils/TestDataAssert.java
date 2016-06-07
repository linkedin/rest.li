/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.common.testutils;


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.validation.CoercionMode;
import com.linkedin.data.schema.validation.RequiredMode;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.test.RecordTemplateWithDefaultValue;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author kparikh
 */
@SuppressWarnings("deprecation")
public class TestDataAssert
{
  @Test
  public void testEqualDataMaps()
  {
    DataMap d1 = new DataMap();
    DataMap d2 = new DataMap();

    d1.put("key1", "value1");
    d1.put("key2", "value2");

    d2.put("key1", "value1");
    d2.put("key2", "value2");

    DataAssert.assertDataMapsEqual(d1, d2, Collections.<String>emptySet(), false);
  }

  @Test
  public void testUnequalDataMaps()
  {
    DataMap actual = new DataMap();
    DataMap expected = new DataMap();

    actual.put("key1", "value1");
    actual.put("key2", "value2");

    expected.put("key1", "value11");
    expected.put("key2", "value22");

    String expectedKey1ErrorMessage = "Mismatch on property \"key1\", expected:<value11> but was:<value1>";
    String expectedKey2ErrorMessage = "Mismatch on property \"key2\", expected:<value22> but was:<value2>";

    try
    {
      DataAssert.assertDataMapsEqual(actual, expected, Collections.<String>emptySet(), false);
      Assert.fail("Should have failed!");
    }
    catch (Throwable t)
    {
      Assert.assertTrue(t.getMessage().contains(expectedKey1ErrorMessage));
      Assert.assertTrue(t.getMessage().contains(expectedKey2ErrorMessage));
    }
  }

  @Test
  public void testIgnoreKeysInDataMapChecking()
  {
    DataMap actual = new DataMap();
    DataMap expected = new DataMap();

    actual.put("key1", "value1");
    actual.put("key2", "value2");
    actual.put("key3", "value3");

    expected.put("key1", "value1");
    expected.put("key2", "value2");
    expected.put("key3", "value33");

    // values are different at "key3"
    DataAssert.assertDataMapsEqual(actual, expected, new HashSet<String>(Collections.singletonList("key3")), false);
  }

  @Test
  public void testDataMapsWithDifferentNullCheckOptions()
  {
    DataMap actual = new DataMap();
    actual.put("key1", "value1");

    DataMap expected = new DataMap();
    expected.put("key1", "value1");
    expected.put("key2", new DataList());
    expected.put("key3", new DataMap());

    try
    {
      DataAssert.assertDataMapsEqual(actual, expected, Collections.<String>emptySet(), false);
      Assert.fail("Assertion should have failed as the data maps are not equal!");
    }
    catch (Throwable t)
    {
      // expected
    }
    DataAssert.assertDataMapsEqual(actual, expected, Collections.<String>emptySet(), true);
  }

  @Test
  public void testEqualRecordTemplates()
  {
    String message = "foo";
    Long id = 1L;

    Greeting actual = new Greeting().setMessage(message).setId(id);
    Greeting expected = new Greeting().setMessage(message).setId(id);

    DataAssert.assertRecordTemplateDataEqual(actual, expected, null);
  }

  @Test
  public void testUnequalRecordTemplates()
  {
    Greeting actual = new Greeting().setMessage("actual").setId(1L);
    Greeting expected = new Greeting().setMessage("expected").setId(2L);

    String expectedErrorMessage1 = "Mismatch on property \"message\", expected:<expected> but was:<actual>";
    String expectedErrorMessage2 = "Mismatch on property \"id\", expected:<2> but was:<1>";

    try
    {
      DataAssert.assertRecordTemplateDataEqual(actual, expected, null);
      Assert.fail();
    }
    catch (Throwable t)
    {
      Assert.assertTrue(t.getMessage().contains(expectedErrorMessage1));
      Assert.assertTrue(t.getMessage().contains(expectedErrorMessage2));
    }
  }

  @Test
  public void testEqualCollections()
  {
    Greeting g1 = new Greeting().setMessage("foo").setId(1L);
    Greeting g2 = new Greeting().setMessage("foo").setId(2L);

    Greeting g3 = new Greeting().setMessage("foo").setId(1L);
    Greeting g4 = new Greeting().setMessage("foo").setId(2L);

    List<Greeting> actual = Arrays.asList(g1, g2);
    List<Greeting> expected = Arrays.asList(g3, g4);

    DataAssert.assertRecordTemplateCollectionsEqual(actual, expected, null);
  }

  @Test
  public void testUnequalCollections()
  {
    Greeting g1 = new Greeting().setMessage("foo").setId(1L);
    Greeting g2 = new Greeting().setMessage("foo").setId(2L);

    List<Greeting> actual = Arrays.asList(g1, g2);
    List<Greeting> expected = Arrays.asList(g1, new Greeting().setId(3L).setMessage("foo"));

    String indexErrorMessage = "are not equal at index 1";
    String propertyErrorMessage = "Mismatch on property \"id\", expected:<3> but was:<2>";

    try
    {
      DataAssert.assertRecordTemplateCollectionsEqual(actual, expected, null);
    }
    catch (Throwable t)
    {
      Assert.assertTrue(t.getMessage().contains(indexErrorMessage));
      Assert.assertTrue(t.getMessage().contains(propertyErrorMessage));
    }
  }

  @Test
  public void testRecordTemplatesAfterFixup()
  {
    RecordTemplateWithDefaultValue actual = new RecordTemplateWithDefaultValue().setId(1L);
    RecordTemplateWithDefaultValue expected = new RecordTemplateWithDefaultValue().setId(1L).setMessage("message");
    ValidationOptions validationOptions = new ValidationOptions(RequiredMode.FIXUP_ABSENT_WITH_DEFAULT,
                                                                CoercionMode.STRING_TO_PRIMITIVE);

    try
    {
      DataAssert.assertRecordTemplateDataEqual(actual, expected, validationOptions);
      Assert.fail("Assertion should have failed as the record templates are not equal w/o fix-up and coercion!");
    }
    catch (Throwable t)
    {
      // expected
    }
    DataAssert.assertRecordTemplateDataEqual(actual, expected, validationOptions);
  }
}
