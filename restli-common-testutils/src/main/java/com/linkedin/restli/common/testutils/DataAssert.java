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
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.template.RecordTemplate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.testng.Assert;


/**
 * Helper methods to assert equality between the underlying {@link DataMap}s of two {@link RecordTemplate}s in an
 * easy to read way.
 *
 * Uses TestNG for assertions.
 *
 * @author jflorencio
 * @author kparikh
 *
 * @deprecated Replaced by {@link DataAsserts}
 */
@Deprecated
public final class DataAssert
{
  private static final String ERROR_MESSAGE_SEPARATOR = "\n";

  private DataAssert() { }

  /**
   * Assert that the data in two collections is the same.
   * Assumes that the underlying {@link Iterator} of each collection returns elements in the same order.
   *
   * @param actual the object under test
   * @param expected the expected object
   * @param validationOptions the {@link ValidationOptions} that should be used for fix-up and coercion. Can be null.
   * @param <T>
   */
  public static <T extends RecordTemplate> void assertRecordTemplateCollectionsEqual(Collection<T> actual,
                                                                                     Collection<T> expected,
                                                                                     ValidationOptions validationOptions)
  {
    if(actual == null || expected == null)
    {
      Assert.assertEquals(actual, expected, "Only one of the two collections is null.");
      return;
    }

    Assert.assertEquals(actual.size(), expected.size(), "The sizes of the collections are not the same!");

    Iterator<T> actualIterator = actual.iterator();
    Iterator<T> expectedIterator = expected.iterator();
    int index = 0;

    while (actualIterator.hasNext() && expectedIterator.hasNext())
    {
      T actualRecordTemplate = actualIterator.next();
      T expectedRecordTemplate = expectedIterator.next();
      try
      {
        assertRecordTemplateDataEqual(actualRecordTemplate, expectedRecordTemplate, validationOptions);
      }
      catch (Throwable t)
      {
        Assert.fail("The record templates are not equal at index " + index + ". Error is: " + t.getMessage());
      }
      index++;
    }
  }

  /**
   * Asserts that the {@link DataMap}s in two {@link RecordTemplate}s are the same
   *
   * @param actual the object under test
   * @param expected the expected object
   * @param validationOptions the {@link ValidationOptions} that should be used for fix-up and coercion. Can be null.
   */
  public static void assertRecordTemplateDataEqual(RecordTemplate actual,
                                                   RecordTemplate expected,
                                                   ValidationOptions validationOptions)
  {
    DataMap actualData = actual.data();
    DataMap expectedData = expected.data();

    if (validationOptions != null)
    {
      actualData = getFixedUpDataMap(actual, validationOptions);
      expectedData = getFixedUpDataMap(expected, validationOptions);
    }

    // we pass in false as the last argument to assertDataMapsEqual with the assumption that the caller for this
    // function would have enabled coercion and fix-up for this method.
    assertDataMapsEqual(actualData, expectedData, Collections.<String>emptySet(), false);
  }

  /**
   * Asserts that two {@link DataMap}s are equal, subject to the {@code excludedProperties} and
   * {@code nullShouldEqualEmptyListOrMap} arguments.
   *
   * @param actualMap the {@link DataMap} we are checking
   * @param expectedMap the expected {@link DataMap}
   * @param excludedProperties the properties that will be ignored while checking the two DataMaps
   * @param nullShouldEqualEmptyListOrMap true if null should equal an empty {@link DataMap} or {@link DataList}
   */
  public static void assertDataMapsEqual(DataMap actualMap,
                                         DataMap expectedMap,
                                         Set<String> excludedProperties,
                                         boolean nullShouldEqualEmptyListOrMap)
  {
    if (excludedProperties == null)
    {
      excludedProperties = Collections.emptySet();
    }

    if(actualMap == null || expectedMap == null)
    {
      Assert.assertEquals(actualMap, expectedMap, "Only one of the data maps is null!");
      return;
    }

    Set<String> failKeys = new HashSet<>();

    // Assert key by key so it's easy to debug on assertion failure
    Set<String> allKeys = new HashSet<>(actualMap.keySet());
    allKeys.addAll(expectedMap.keySet());
    for(String key : allKeys)
    {
      if(excludedProperties.contains(key))
      {
        continue;
      }

      Object actualObject = actualMap.get(key);
      Object expectedObject = expectedMap.get(key);
      if(actualObject == null)
      {
        if (nullShouldEqualEmptyListOrMap && isEmptyListOrMap(expectedObject))
        {
          continue;
        }
        if(expectedObject != null)
        {
          failKeys.add(key);
        }
      }
      else if(!actualObject.equals(expectedObject))
      {
        if (nullShouldEqualEmptyListOrMap && expectedObject == null && isEmptyListOrMap(actualObject))
        {
          continue;
        }
        failKeys.add(key);
      }
    }

    if(!failKeys.isEmpty())
    {
      List<String> errorMessages = new ArrayList<>();
      errorMessages.add(failKeys.size() + " properties don't match:");
      for(String k : failKeys)
      {
        errorMessages.add("\tMismatch on property \"" + k + "\", expected:<" +
                              expectedMap.get(k) + "> but was:<" + actualMap.get(k) + ">");
      }
      Assert.fail(StringUtils.join(errorMessages, ERROR_MESSAGE_SEPARATOR));
    }
  }

  /**
   * Checks if the passed in object is an empty {@link DataList} or {@link DataMap}
   * @param object the object we are testing
   * @return if the object is an empty {@link DataList} or {@link DataMap}
   */
  private static boolean isEmptyListOrMap(Object object)
  {
    if (object instanceof DataMap)
    {
      return ((DataMap)object).isEmpty();
    }
    if (object instanceof DataList)
    {
      return ((DataList)object).isEmpty();
    }
    return false;
  }

  /**
   * @param recordTemplate the object we want to fix-up and coerce
   * @param validationOptions the {@link ValidationOptions} that should be used for fix-up and coercion. Can be null.
   *
   * @return the fixed-up and coerced {@link DataMap}
   */
  private static DataMap getFixedUpDataMap(RecordTemplate recordTemplate, ValidationOptions validationOptions)
  {
    return (DataMap) ValidateDataAgainstSchema.validate(recordTemplate, validationOptions).getFixed();
  }
}
