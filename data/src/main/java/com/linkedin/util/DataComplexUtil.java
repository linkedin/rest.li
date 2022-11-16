package com.linkedin.util;

import com.linkedin.data.Data;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.DataMapBuilder;
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.data.template.DataTemplateUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Utils to convert generic List/Map to DataList/DataMap or vice versa
 */
public class DataComplexUtil {

  private DataComplexUtil() { }

  /**
   * Converts the input {@link Map} to a {@link DataMap} recursively.
   * Optional to stringfy the value of the leaf nodes in the datacomplex
   */
  public static DataMap convertMap(Map<String, ?> input, boolean stringify) {
    return convertMap(input, false, stringify);
  }

  /**
   * Converts the input {@link Map} to a {@link DataMap} recursively.
   */
  public static DataMap convertMap(Map<String, ?> input) {
    return convertMap(input, false, false);
  }

  /**
   * Converts the input {@link Map} to a {@link DataMap} recursively while retaining <code>nulls</code>
   * in the process. The returned {@link DataMap} will have {@link Data#NULL} in places where <code>nulls</code>
   * were present in the original {@link Map}.
   */
  public static DataMap convertMapRetainingNulls(Map<String, ?> input) {
    return convertMap(input, true, false);
  }

  /**
   * Converts the input {@link List} to a {@link DataList} recursively.
   */
  public static DataList convertList(List<?> input) {
    return convertList(input, false, false);
  }

  /**
   * Convert the input {@link List} to a {@link DataList} recursively while retaining <code>nulls</code> in the process.
   * The returned {@link DataList} will have {@link Data#NULL} in places where <code>nulls</code> were present in the original {@link List}.
   */
  public static DataList convertListRetainingNulls(List<?> input) {
    return convertList(input, true, false);
  }

  /**
   * Attempts to convert object to {@link DataList} or {@link DataMap}. Otherwise, returns the original object.
   */
  public static Object convertObject(Object dataObject) {
    return convertObject(dataObject, false, false);
  }

  /**
   * Attempts to convert object to {@link DataList} or {@link DataMap}. Otherwise, returns the original object.
   * Optional to stringfy the value of the leaf nodes in the datacomplex
   */
  @SuppressWarnings("unchecked")
  public static Object convertObject(Object value, boolean retainNulls, boolean stringify) {
    if (value instanceof DataMap || value instanceof DataList) {
      return value;
    }

    if (value instanceof Map) {
      return convertMap((Map<String, ?>) value, retainNulls, stringify);
    }

    if (value instanceof List) {
      return convertList((List<?>) value, retainNulls, stringify);
    }

    return stringify ? DataTemplateUtil.stringify(value) : value;
  }

  /**
   * Attempts to convert DataComplex object to Java native types recursively. Otherwise, returns the original object.
   * Optional to specify to retainNulls, to indicate whether null means a null literal or means that a field should be dropped
   */
  @SuppressWarnings("unchecked")
  public static Object convertToJavaObject(Object value, boolean retainNulls) {
    if (value instanceof HashMap || value instanceof ArrayList) {
      return value;
    }

    if (value instanceof Map) {
      return convertToJavaMap((Map) value, retainNulls);
    }

    if (value instanceof List) {
      return convertToJavaList((List) value, retainNulls);
    }

    if (value == Data.NULL) {
      return null;
    }

    return value;
  }

  private static DataMap convertMap(Map<String, ?> input, boolean retainNulls, boolean stringify) {
    if (input instanceof DataMap) {
      return (DataMap) input;
    }

    DataMap result = new DataMap(DataMapBuilder.getOptimumHashMapCapacityFromSize(input.size()));
    input.forEach((key, value) -> {
      Object convertedValue = convertObject(value, retainNulls, stringify);

      if (convertedValue != null) {
        CheckedUtil.putWithoutCheckingOrChangeNotification(result, key, convertedValue);
      } else if (retainNulls) {
        CheckedUtil.putWithoutCheckingOrChangeNotification(result, key, Data.NULL);
      }
    });
    return result;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static HashMap<String, Object> convertToJavaMap(Map input, boolean retainNulls) {
    if (input instanceof HashMap) {
      return (HashMap) input;
    }

    HashMap<String, Object> result = new HashMap<>(input.size());
    input.forEach((key, value) -> {
      Object convertedValue = convertToJavaObject(value, retainNulls);

      if (convertedValue != null) {
        result.put((String) key, convertedValue);
      } else if (retainNulls) {
        result.put((String) key, null);
      }
    });
    return result;
  }

  private static DataList convertList(List<?> input, boolean retainNulls, boolean stringify) {
    if (input instanceof DataList) {
      return (DataList) input;
    }

    DataList result = new DataList(input.size());
    input.forEach(entry -> {
      Object convertedEntry = convertObject(entry, retainNulls, stringify);

      if (convertedEntry != null) {
        CheckedUtil.addWithoutChecking(result, convertedEntry);
      } else if (retainNulls) {
        CheckedUtil.addWithoutChecking(result, Data.NULL);
      }
    });
    return result;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static ArrayList<Object> convertToJavaList(List input, boolean retainNulls) {
    if (input instanceof ArrayList) {
      return (ArrayList) input;
    }

    ArrayList<Object> result = new ArrayList<>(input.size());
    input.forEach(entry -> {
      Object convertedEntry = convertToJavaObject(entry, retainNulls);

      if (convertedEntry != null) {
        result.add(convertedEntry);
      } else if (retainNulls) {
        result.add(null);
      }
    });
    return result;
  }
}
