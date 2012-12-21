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

package com.linkedin.data.schema;


import com.linkedin.data.Data;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.DataLocation;
import com.linkedin.data.codec.JacksonDataCodec;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Common base class for parsers that parse Data objects.
 *
 * @author slim
 */
abstract public class AbstractDataParser
{
  protected AbstractDataParser()
  {
  }

  /**
   * Return the {@link StringBuilder} containing the error message from parsing.
   *
   * @return the {@link StringBuilder} containing the error message from parsing.
   */
  public abstract StringBuilder errorMessageBuilder();

  /**
   * Return whether any error occurred during parsing.
   *
   * @return true if at least one error occurred during parsing.
   */
  public boolean hasError()
  {
    return errorMessageBuilder().length() != 0;
  }

  /**
   * Return the error message from parsing.
   *
   * @return the error message.
   */
  public String errorMessage()
  {
    return errorMessageBuilder().toString();
  }

  /**
   * Parse an {@link InputStream} containing JSON to a list of Data objects.
   *
   * @param inputStream containing JSON.
   */
  protected List<Object> jsonInputStreamToObjects(InputStream inputStream)
  {
    List<Object> objects;
    try
    {
      objects = _codec.parse(inputStream, errorMessageBuilder(), dataLocationMap());
    }
    catch (IOException e)
    {
      errorMessageBuilder().append(e).append("\n");
      e.printStackTrace();
      return Collections.emptyList();
    }
    return objects;
  }

  /**
   * Parse an {@link Reader} containing JSON to a list of Data objects.
   *
   * @param reader containing JSON.
   */
  protected List<Object> jsonReaderToObjects(Reader reader)
  {
    List<Object> objects;
    try
    {
      objects = _codec.parse(reader, errorMessageBuilder(), dataLocationMap());
    }
    catch (IOException e)
    {
      errorMessageBuilder().append(e).append("\n");
      e.printStackTrace();
      return Collections.emptyList();
    }
    return objects;
  }

  /**
   * Parse a {@link DataMap} to obtain a {@link Name}.
   *
   * Return an empty {@link Name} (see {@link Name#isEmpty()}) if
   * a name cannot be obtained from the {@link DataMap}.
   *
   * @param map to parse.
   * @param nameKey is the key used to find the name in the map.
   * @param currentNamespace is the current namespace.
   * @return a {@link Name} parsed from the {@link DataMap}.
   */
  protected Name getNameFromDataMap(DataMap map, String nameKey, String currentNamespace)
  {
    String nameString = getString(map, nameKey, true);
    String namespaceString = getString(map, NAMESPACE_KEY, false);
    Name name = getName(nameString, namespaceString, currentNamespace);
    // associate a name with a location,
    // this allows error messages such re-definition of a name to include a location.
    addToDataLocationMap(name, lookupDataLocation(nameString));
    return name;
  }

  /**
   * Compute {@link Name} from name, namespace and current namespace.
   *
   * @param name obtained from a {@link DataMap}, may be null if not present,
   *             name may be unqualified or fully qualified.
   * @param namespace obtained from a {@link DataMap}, may be null if not present.
   * @param currentNamespace is the current namespace.
   * @return the {@link Name} computed from inputs.
   */
  protected Name getName(String name, String namespace, String currentNamespace)
  {
    Name n = new Name();
    if (name != null && name != SUBSTITUTE_FOR_REQUIRED_STRING)
    {
      if (Name.isFullName(name))
      {
        n.setName(name, startCalleeMessageBuilder());
        appendCalleeMessage(name);
      }
      else
      {
        if (namespace == null)
        {
          namespace = currentNamespace;
        }
        n.setName(name, namespace, startCalleeMessageBuilder());
        appendCalleeMessage(name);
      }
    }
    return n;
  }

  /**
   * Get a string value from the field identified by the specified key.
   *
   * If the field is a required field or the value is not a string,
   * append an error message to {@link #errorMessageBuilder}.
   * If the field is required and the key is not found, return null.
   * If the field is not required and key is not found, return an empty string.
   *
   * @param map to lookup key in.
   * @param key to lookup a field in the map.
   * @param required specifies whether the field is a required field.
   * @return a string.
   */
  protected String getString(DataMap map, String key, boolean required)
  {
    String value = null;
    Object obj = map.get(key);
    if (obj != null)
    {
      if (obj instanceof String)
      {
        value = (String) obj;
      }
      else
      {
        startErrorMessage(obj).append(key).append(" with value ").append(obj).append(" is not a string.\n");
      }
    }
    else if (required)
    {
      startErrorMessage(map).append(key).append(" (with string value) is required but it is not present.\n");
    }
    if (required && value == null)
    {
      value = SUBSTITUTE_FOR_REQUIRED_STRING;
    }
    return value;
  }

  /**
   * Get an integer value from the field identified by the specified key.
   *
   * If the field is a required field or the value is not an integer,
   * append an error message to {@link #errorMessageBuilder}.
   * If the field is required and the key is not found, return null.
   * If the field is not required and key is not found, return 0.
   *
   * @param map to lookup key in.
   * @param key to lookup a field in the map.
   * @param required specifies whether the field is a required field.
   * @return an integer.
   */
  protected Integer getInteger(DataMap map, String key, boolean required)
  {
    Integer value = null;
    Object obj = map.get(key);
    if (obj != null)
    {
      if (obj instanceof Integer)
      {
        value = (Integer) obj;
      }
      else if (obj instanceof Long)
      {
        value = ((Long) obj).intValue();
      }
      else
      {
        startErrorMessage(obj).append(key).append(" with value ").append(obj).append(" is not an integer.\n");
      }
    }
    else if (required)
    {
      startErrorMessage(map).append(key).append(" (with integer value) is required but it is not present.\n");
    }
    if (required && value == null)
    {
      value = 0;
    }
    return value;
  }

  /**
   * Get a boolean value from the field identified by the specified key.
   *
   * If the field is a required field or the value is not a boolean,
   * append an error message to {@link #errorMessageBuilder}.
   * If the field is required and the key is not found, return null.
   * If the field is not required and key is not found, return false.
   *
   * @param map to lookup key in.
   * @param key to lookup a field in the map.
   * @param required specifies whether the field is a required field.
   * @return a boolean.
   */
  protected Boolean getBoolean(DataMap map, String key, boolean required)
  {
    Boolean value = null;
    Object obj = map.get(key);
    if (obj != null)
    {
      if (obj instanceof Boolean)
      {
        value = (Boolean) obj;
      }
      else
      {
        startErrorMessage(obj).append(key).append(" with value ").append(obj).append(" is not a boolean.\n");
      }
    }
    else if (required)
    {
      startErrorMessage(map).append(key).append(" (with boolean value) is required but it is not present.\n");
    }
    if (required && value == null)
    {
      value = false;
    }
    return value;
  }

  /**
   * Get a {@link DataMap} value from the field identified by the specified key.
   *
   * If the field is a required field or the value is not a DataMap,
   * append an error message to {@link #errorMessageBuilder}.
   * If the field is required and the key is not found, return null.
   * If the field is not required and key is not found, return an empty {@link DataMap}.
   *
   * @param map to lookup key in.
   * @param key to lookup a field in the map.
   * @param required specifies whether the field is a required field.
   * @return a {@link DataMap}.
   */
  protected DataMap getDataMap(DataMap map, String key, boolean required)
  {
    DataMap result = null;
    Object obj = map.get(key);
    if (obj != null)
    {
      if (obj instanceof DataMap)
      {
        result = (DataMap) obj;
      }
      else
      {
        startErrorMessage(obj).append(key).append(" is not a map.\n");
      }
    }
    else if (required)
    {
      startErrorMessage(map).append(key).append(" (with map value) is required but it is not present.\n");
    }
    if (required && result == null)
    {
      result = new DataMap();
    }
    return result;
  }

  /**
   * Get a {@link DataList} value from the field identified by the specified key.
   *
   * If the field is a required field or the value is not a {@link DataList},
   * append an error message to {@link #errorMessageBuilder}.
   * If the field is required and the key is not found, return null.
   * If the field is not required and key is not found, return an empty {@link DataList}.
   *
   * @param map to lookup key in.
   * @param key to lookup a field in the map.
   * @param required specifies whether the field is a required field.
   * @return a {@link DataList}.
   */
  protected DataList getDataList(DataMap map, String key, boolean required)
  {
    DataList list = null;
    Object obj = map.get(key);
    if (obj != null)
    {
      if (obj instanceof DataList)
      {
        list = (DataList) obj;
      }
      else
      {
        startErrorMessage(obj).append(key).append(" is not an array.\n");
      }
    }
    else if (required)
    {
      startErrorMessage(map).append(key).append(" (with array value) is required but it is not present.\n");
    }
    if (required && list == null)
    {
      list = new DataList();
    }
    return list;
  }

  /**
   * Get a list of strings from the field identified by the specified key.
   *
   * If the field is a required field or the value is not a array of strings,
   * append an error message to {@link #errorMessageBuilder}.
   * If the field is required and the key is not found, return null.
   * If the field is not required and key is not found, return an empty list.
   *
   * @param map to lookup key in.
   * @param key to lookup a field in the map.
   * @param required specifies whether the field is a required field.
   * @return a {@link DataList}.
   */
  protected List<String> getStringList(DataMap map, String key, boolean required)
  {
    DataList dataList = getDataList(map, key, required);
    List<String> list = null;
    if (dataList != null)
    {
      list = new ArrayList<String>();
      for (Object o : dataList)
      {
        if (o instanceof String)
        {
          list.add((String) o);
        }
        else
        {
          startErrorMessage(o).append(o).append(" is not a string.\n");
        }
      }
    }
    return list;
  }

  /**
   * Extract the properties from a {@link DataMap}.
   *
   * @param map to extract properties from.
   * @param reserved is the list of reserved names.
   * @return the properties extracted from the {@link DataMap}.
   */
  protected Map<String, Object> extractProperties(DataMap map, Set<String> reserved)
  {
    // Use TreeMap to keep properties in sorted order.
    Map<String, Object> props = new TreeMap<String, Object>();
    for (Map.Entry<String, Object> e : map.entrySet())
    {
      String key = e.getKey();
      if (reserved.contains(key) == false)
      {
        Object value = e.getValue();
        if (value != Data.NULL)
        {
          Object replaced = props.put(key, value);
          assert(replaced == null);
        }
        else
        {
          startErrorMessage(value).append("\"").append(key).append("\" is a property and its value must not be null.\n");
        }
      }
    }
    return props;
  }

  /**
   * Set the current location for the source of input to the parser.
   *
   * This current location is will be used to annotate {@link NamedDataSchema}'s
   * generated from parsing.
   *
   * @param location of the input source.
   */
  public void setLocation(DataSchemaLocation location)
  {
    _location = location;
  }

  /**
   * Get the current location for the source of input to the parser.
   *
   * @return the location of the input source.
   */
  public DataSchemaLocation getLocation()
  {
    return _location;
  }

  /**
   * Return the map of objects to their locations in the input source.
   *
   * @return the map of objects to their locations in the input source.
   */
  public abstract Map<Object, DataLocation> dataLocationMap();

  /**
   * Add a new mapping to the map of Data object to their locations in the input source.
   *
   * The new mapping is added only if both arguments are not {@code null}.
   *
   * @param object provides the object.
   * @param dataLocation provides the location associated with the object.
   */
  protected void addToDataLocationMap(Object object, DataLocation dataLocation)
  {
    if (object != null && dataLocation != null)
    {
      dataLocationMap().put(object, dataLocation);
    }
  }

  /**
   * Return the location of an object in the input source.
   *
   * @param object provides the object.
   * @return the location of the object specified.
   */
  protected DataLocation lookupDataLocation(Object object)
  {
    return dataLocationMap().get(object);
  }

  /**
   * Start an error message by appending the location of the object (if available) to
   * {@link #errorMessageBuilder()}.
   *
   * If a location is not known for the specified object, the {@link #errorMessageBuilder()}
   * is not modified.
   *
   * @param object that to use to lookup for a location to append to {@link #errorMessageBuilder()}.
   * @return {@link #errorMessageBuilder()}.
   */
  protected StringBuilder startErrorMessage(Object object)
  {
    if (object != null)
    {
      DataLocation dataLocation = lookupDataLocation(object);
      if (dataLocation != null)
      {
        errorMessageBuilder().append(dataLocation).append(": ");
      }
    }
    return errorMessageBuilder();
  }

  /**
   * Return {@link StringBuilder} for buffering a message generated by a callee.
   *
   * This method is used with {@link #appendCalleeMessage(Object)} to output
   * the location associated with the callee generated message when the
   * message is emitted to {@link #errorMessageBuilder()}.
   *
   * @return an empty {@link StringBuilder} that the callee may modify.
   */
  protected StringBuilder startCalleeMessageBuilder()
  {
    assert(_calleeMessageBuilder.length() == 0);
    return _calleeMessageBuilder;
  }

  /**
   * If the callee has generated any message, then append location of specified
   * Data object and the callee's message (which is in the {@link StringBuilder}
   * returned by {@link #startCalleeMessageBuilder()}) to {@link #errorMessageBuilder()}.
   *
   * @param object provides the location associated with the message.
   */
  protected void appendCalleeMessage(Object object)
  {
    int len = _calleeMessageBuilder.length();
    if (len != 0)
    {
      startErrorMessage(object).append(_calleeMessageBuilder);
      _calleeMessageBuilder.delete(0, len);
    }
  }

  /**
   * Used to store the message returned by a callee.
   *
   * If the callee provides a message, it allows the caller to prepend the
   * message with a location in when writing the message to {@link #errorMessageBuilder()}.
   *
   * @see #startCalleeMessageBuilder()
   * @see #appendCalleeMessage(Object)
   */
  private final StringBuilder _calleeMessageBuilder = new StringBuilder();

  private final JacksonDataCodec _codec = new JacksonDataCodec();
  private DataSchemaLocation _location = DataSchemaLocation.NO_LOCATION;

  private static final String NAMESPACE_KEY = "namespace";
  private static final String SUBSTITUTE_FOR_REQUIRED_STRING = new String();
}
