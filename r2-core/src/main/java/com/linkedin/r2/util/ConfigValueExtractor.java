package com.linkedin.r2.util;


import java.util.ArrayList;
import java.util.List;


/**
 * @author kparikh
 */
public class ConfigValueExtractor
{
  /**
   * Converts a property value that logically represents a List into a List, regardless of how it's been expressed in
   * the config.
   * @param propertyValue the value we want to extract as a List
   * @param listSeparator the separator between values in the config
   * @return a list of the property values, or an empty list if propertyValue is null
   */
  public static List<String> buildList(Object propertyValue, String listSeparator)
  {
    List<String> valueList = new ArrayList<String>();
    if (propertyValue != null)
    {
      if (propertyValue instanceof List<?>)
      {
        @SuppressWarnings("unchecked")
        List<String> list = (List<String>)propertyValue;
        valueList.addAll(list);
      }
      else
      {
        // list was expressed as a String in the config
        String propertyValueString = (String)propertyValue;
        if (listSeparator == null)
        {
          throw new IllegalArgumentException("The separator cannot be null!");
        }
        for (String value: propertyValueString.split(listSeparator))
        {
          if (!value.isEmpty())
          {
            valueList.add(value.trim());
          }
        }
      }
    }
    return valueList;
  }
}
