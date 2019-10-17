/*
   Copyright (c) 2016 LinkedIn Corp.

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

import com.linkedin.data.ByteString;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.Null;
import com.linkedin.data.schema.DataSchemaUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Compare data objects, and return easy to understand error messages.
 * Inspired by JSONassert [https://github.com/skyscreamer/jsonassert] which has identical error messages for JSON.
 *
 * @author Anirudh Padmarao
 */
public class DataCompare
{
  private final Options _options;

  private DataCompare()
  {
    _options = new Options(true, true);
  }

  private DataCompare(Options options)
  {
    _options = options;
  }

  /**
   * Compare the expected and actual data objects, and return a comparison result.
   *
   * @param expected expected data object
   * @param actual   actual data object
   * @return comparison result
   */
  public static Result compare(Object expected, Object actual)
  {
    Result compareResult = new Result();
    new DataCompare().compare("", expected, actual, compareResult);

    return compareResult;
  }

  /**
   * Compare the expected and actual data objects according to the _options, and return a comparison result.
   *
   * @param expected expected data object
   * @param actual   actual data object
   * @param options  comparison _options
   * @return comparison result
   */
  public static Result compare(DataComplex expected, DataComplex actual, Options options)
  {
    Result compareResult = new Result();
    new DataCompare(options).compare("", expected, actual, compareResult);

    return compareResult;
  }

  private void compare(String path, Object expected, Object actual, Result result)
  {
    if (expected.getClass().isAssignableFrom(actual.getClass()))
    {
      if (expected instanceof DataMap && actual instanceof DataMap)
      {
        compareDataMap(path, (DataMap) expected, (DataMap) actual, result);
      }
      else if (expected instanceof DataList && actual instanceof DataList)
      {
        compareDataList(path, (DataList) expected, (DataList) actual, result);
      }
      else if (!expected.equals(actual))
      {
        result.mismatchedValue(path, expected, actual);
      }
      else
      {
        assert expected.equals(actual);
      }
    }
    else if (expected instanceof Number && actual instanceof Number)
    {
      compareNumbers(path, (Number) expected, (Number) actual, result);
    }
    else if (isStringLike(expected) && isStringLike(actual))
    {
      compareStringLike(path, expected, actual, result);
    }
    else
    {
      result.mismatchedType(path, expected, actual);
    }
  }

  private void compareDataMap(String path, DataMap expected, DataMap actual, Result result)
  {
    checkDataMapKeysExpectedInActual(path, expected, actual, result);
    checkDataMapKeysActualInExpected(path, expected, actual, result);
  }

  // Check that actual data map contains all the keys in expected data map, and that the values match
  private void checkDataMapKeysExpectedInActual(String path, DataMap expected, DataMap actual, Result result)
  {

    Set<String> expectedKeys = expected.keySet();
    for (String key : expectedKeys)
    {
      Object expectedValue = expected.get(key);
      if (actual.containsKey(key))
      {
        Object actualValue = actual.get(key);
        compare(qualify(path, key), expectedValue, actualValue, result);
      } else
      {
        result.missing(path, key);
      }
    }
  }

  // Check that expected data map contains all the keys in actual data map
  private static void checkDataMapKeysActualInExpected(String path, DataMap expected, DataMap actual, Result result)
  {

    actual.keySet().forEach(key -> {
      if (!expected.containsKey(key))
      {
        result.unexpected(path, key);
      }
    });
  }

  private void compareDataList(String path, DataList expected, DataList actual, Result result)
  {
    if (expected.size() != actual.size())
    {
      result.addMessage(path + "[] Expected " + expected.size() + " values but got " + actual.size());
      return;
    }

    if (_options._dataListComparator != null) {
      expected.sort(_options._dataListComparator);
      actual.sort(_options._dataListComparator);
    }

    for (int index = 0; index < expected.size(); ++index)
    {
      Object expectedItem = expected.get(index);
      Object actualItem = actual.get(index);

      compare(path + "[" + index + "]", expectedItem, actualItem, result);
    }
  }

  private void compareNumbers(String path, Number expected, Number actual, Result result)
  {
    if (expected.getClass().isAssignableFrom(actual.getClass()))
    { // compare by value for same type
      if (!expected.equals(actual))
      {
        result.mismatchedValue(path, expected, actual);
      }
    }
    else if (_options._shouldCoerceNumbers)
    { // coerce to BigDecimal and compare by value if coercion is enabled
      BigDecimal expectedBigDecimal = new BigDecimal(expected.toString());
      BigDecimal actualBigDecimal = new BigDecimal(actual.toString());
      if (expectedBigDecimal.compareTo(actualBigDecimal) != 0)
      {
        result.mismatchedValue(path, expected, actual);
      }
    }
    else
    {
      result.mismatchedType(path, expected, actual);
    }
  }

  private void compareStringLike(String path, Object expected, Object actual, Result result)
  {
    if (expected.getClass().isAssignableFrom(actual.getClass()))
    {
      if (!expected.equals(actual))
      {
        result.mismatchedValue(path, expected, actual);
      }
    }
    else if (_options._shouldCoerceByteStrings)
    {
      if (expected instanceof ByteString && actual instanceof String)
      {
        compareByteString(path, (ByteString) expected, (String) actual, result);
      }
      else if (expected instanceof String && actual instanceof ByteString)
      {
        compareByteString(path, (String) expected, (ByteString) actual, result);
      }
      else
      {
        result.mismatchedType(path, expected, actual);
      }
    }
    else
    {
      result.mismatchedType(path, expected, actual);
    }
  }

  private boolean isStringLike(Object object)
  {
    return object instanceof String || object instanceof ByteString;
  }

  private void compareByteString(String path, ByteString expected, String actual, Result result)
  {
    if (!expected.asAvroString().equals(actual))
    {
      result.mismatchedValue(path, expected.asAvroString(), actual);
    }
  }

  private void compareByteString(String path, String expected, ByteString actual, Result result)
  {
    if (!expected.equals(actual.asAvroString()))
    {
      result.mismatchedValue(path, expected, actual.asAvroString());
    }
  }

  /**
   * The options used to configure comparison of data objects.
   */
  public static class Options
  {
    /**
     * When comparing numbers for equality, whether to coerce numbers to double and compare by value,
     * or to compare using equals.
     */
    private final boolean _shouldCoerceNumbers;

    /**
     * When comparing a bytestring and a string, whether to coerce the bytestring to a string and compare,
     * or to compare using equals.
     */
    private final boolean _shouldCoerceByteStrings;

    /**
     * When comparing DataList, use the non-null comparator to sort and then compare.
     */
    private final Comparator<? super Object> _dataListComparator;

    public Options(boolean shouldCoerceNumbers, boolean shouldCoerceByteStrings)
    {
      this(shouldCoerceNumbers, shouldCoerceByteStrings, null);
    }

    public Options(boolean shouldCoerceNumbers, boolean shouldCoerceByteStrings,
        Comparator<? super Object> dataListComparator)
    {
      _shouldCoerceNumbers = shouldCoerceNumbers;
      _shouldCoerceByteStrings = shouldCoerceByteStrings;
      _dataListComparator = dataListComparator;
    }
  }

  /**
   * The result of comparing data objects.
   */
  public static class Result
  {

    private final List<String> _messages = new ArrayList<>();

    private Result()
    {
    }

    /**
     * Whether the expected and actual data objects did not match
     */
    public boolean hasError()
    {
      return !_messages.isEmpty();
    }

    @Override
    public String toString()
    {
      return "\n" + _messages.stream().collect(Collectors.joining("\n\n")) + "\n";
    }

    private void addMessage(String message)
    {
      _messages.add(message);
    }

    private void missing(String path, Object expectedKey)
    {
      _messages.add(path
          + "\nExpected: "
          + expectedKey.toString()
          + "\n    but none found");
    }

    private void unexpected(String path, Object unexpectedKey)
    {
      _messages.add(path
          + "\nUnexpected: "
          + unexpectedKey);
    }

    private void mismatchedValue(String path, Object expected, Object actual)
    {
      _messages.add(path
          + "\nExpected: "
          + expected.toString()
          + "\n     got: "
          + actual.toString());
    }

    private void mismatchedType(String path, Object expected, Object actual)
    {
      _messages.add(path
          + "\nExpected: "
          + describeType(expected)
          + "\n     got: " + describeType(actual));
    }
  }

  private static String qualify(String prefix, String key)
  {
    boolean isUnionMemberKey = key.contains(".");

    String valueToAppend;
    if (isUnionMemberKey)
    {
      // union member keys for named types are very verbose, so shorten them to their simple names
      // e.g. a prefix "foo" with union value "com.linkedin.restli.common.EmptyRecord" will have path "foo{EmptyRecord}"
      valueToAppend = "{" + key.substring(key.lastIndexOf(".") + 1) + "}";
    }
    else
    {
      valueToAppend = "." + key;
    }

    return "".equals(prefix) ? key : prefix + valueToAppend;
  }

  private static String describeType(Object value)
  {
    Class<?> valueClass = value.getClass();
    if (valueClass == Null.class)
    {
      return "null";
    }
    else if (isPrimitiveClass(valueClass))
    {
      return DataSchemaUtil.classToPrimitiveDataSchema(value.getClass()).getUnionMemberKey();
    }
    else
    {
      assert isComplexClass(valueClass);

      if (valueClass == DataMap.class)
      {
        return "data map";
      }
      else
      {
        assert valueClass == DataList.class;
        return "data list";
      }
    }
  }

  private static boolean isComplexClass(Class<?> clazz)
  {
    return clazz == DataMap.class || clazz == DataList.class;
  }

  private static boolean isPrimitiveClass(Class<?> clazz)
  {
    return clazz == String.class
        || clazz == Integer.class
        || clazz == Double.class
        || clazz == Boolean.class
        || clazz == Long.class
        || clazz == Float.class
        || clazz == ByteString.class
        || clazz == Null.class;
  }
}
