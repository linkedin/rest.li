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

package com.linkedin.data;


import com.linkedin.util.ArgumentUtil;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;


/**
 * This class holds common methods used to operate on Data objects.
 * <p>
 *
 * A Data object may be a primitive object or a complex object.
 * A primitive object is a Java {@link Boolean}, {@link Integer},
 * {@link Long}, {@link Float}, {@link Double}, {@link String}, or
 * {@link ByteString}. A primitive object is immutable once created.
 * <p>
 *
 * A complex object is either a {@link DataMap} or {@link DataList}
 * and implements {@link DataComplex}.
 * A {@link DataMap} contains key to value mappings.
 * The key is always a string and the value is a Data object.
 * A {@link DataList} is a ordered list of Data objects.
 * A complex object may be made read-only.
 * <p>
 *
 * Null is not a valid Data object.
 * <p>
 *
 * There are two ways to copy a complex object, one is
 * shallow copy and the other is a deep copy. A shallow copy only
 * copies the internal state of the complex object. For a {@link DataMap},
 * it logically copies the entries that relates keys to values,
 * i.e., it does not copy the keys and values.
 * For a {@link DataList}, it copies the list holding references to objects
 * and does not copy the referenced objects.
 * The {@link DataComplex#clone()} method performs a shallow copy.
 * <p>
 *
 * A deep copy copies both the internal state and referenced complex
 * objects recursively. To keep track of complex objects that are referenced
 * more than once in the object graph, and also to avoid infinite loops in
 * recursive traversal of a non-acyclic object graph, a deep copy
 * keeps track of complex objects that have been copied.
 * The {@link DataComplex#copy()} method performs a deep copy; its implementations depend
 * on {@link Data#copy(Object, DataComplexTable)}.
 * The {@link DataComplexTable} is used to track the complex objects
 * that have already been copied.
 * <p>
 *
 * Primitive objects are immutable, neither deep copy nor shallow copy
 * will copy them.
 * <p>
 *
 * Complex objects implement copy-on-write, logical copying of
 * their internal state is done by incrementing a reference count
 * and creating a copy of the internal state is delayed until the
 * clone is about to be modified.
 * <p>
 *
 * When deep copying an object graph, complex objects that contain only
 * primitive objects does not cause the internal state to be copied.
 * However, a complex object that contains another complex object will
 * have its internal state copied because its copy will be modified
 * to store references to the clones of the referenced
 * complex objects.
 * <p>
 *
 * Binary data is stored as a Java String with each byte represented by a character
 * and storing the byte's value in the least significant 8-bits of the character,
 * (following the Avro specification.)
 * <p>
 *
 * @see #copy(Object, DataComplexTable)
 * @see DataList
 * @see DataMap
 *
 * @author slim
 */
public class Data
{
  /**
   * Constant value used to indicate that a null value was de-serialized.
   */
  public static final Null NULL = Null.getInstance();

  /**
   * Charset UTF-8
   */
  public static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");

  /**
   * A map of all underlying types supported by Data objects.
   */
  public static final Map<Class<?>, Byte> TYPE_MAP = new HashMap<>();
  static
  {
    TYPE_MAP.put(String.class, (byte) 1);
    TYPE_MAP.put(Integer.class, (byte) 2);
    TYPE_MAP.put(DataMap.class, (byte) 3);
    TYPE_MAP.put(DataList.class, (byte) 4);
    TYPE_MAP.put(Boolean.class, (byte) 5);
    TYPE_MAP.put(Long.class, (byte) 6);
    TYPE_MAP.put(Float.class, (byte) 7);
    TYPE_MAP.put(Double.class, (byte) 8);
    TYPE_MAP.put(ByteString.class, (byte) 9);
  }

  /**
   * Callback interface invoked by traverse method.
   *
   * This interface contains callback methods that are invoked when
   * traversing a Data object. Each callback method represents a
   * different kind of traversal event.
   *
   * Callback methods can throw IOException as a checked exception to
   * indicate traversal error.
   *
   * @see #traverse(Object obj, TraverseCallback callback)
   * @author slim
   */
  public interface TraverseCallback extends Closeable
  {
    /**
     * Return an {@link Iterable} with the
     * desired output order of the entries in the traversed {@link DataMap}.
     *
     * If the order is not significant, then this method should return
     * the result of {@link DataMap#entrySet()}.
     *
     * This interface provides default NoOp for all operations except
     * {@link TraverseCallback#orderMap(DataMap)}.
     *
     * @param map provides the {@link DataMap}.
     * @return entries of the {@link DataMap} entries in the desired output order.
     */
    default Iterable<Map.Entry<String, Object>> orderMap(DataMap map)
    {
      return map.entrySet();
    }

    /**
     * Invoked when a null value is traversed.
     * This should not happen.
     */
    default void nullValue() throws IOException
    {
    }

    /**
     * Invoked when a boolean value is traversed.
     *
     * @param value the boolean value.
     */
    default void booleanValue(boolean value) throws IOException
    {
    }

    /**
     * Invoked when a integer value is traversed.
     *
     * @param value the integer value.
     */
    default void integerValue(int value) throws IOException
    {
    }

    /**
     * Invoked when a long value is traversed.
     *
     * @param value the long value.
     */
    default void longValue(long value) throws IOException
    {
    }

    /**
     * Invoked when a float value is traversed.
     *
     * @param value the float value.
     */
    default void floatValue(float value) throws IOException
    {
    }

    /**
     * Invoked when a double value is traversed.
     *
     * @param value the double value.
     */
    default void doubleValue(double value) throws IOException
    {
    }

    /**
     * Invoked when a string value is traversed.
     *
     * @param value the string value.
     */
    default void stringValue(String value) throws IOException
    {
    }

    /**
     * Invoked when a {@link ByteString} value is traversed.
     *
     * @param value the string value.
     */
    default void byteStringValue(ByteString value) throws IOException
    {
    }

    /**
     * Invoked when an illegal value is traversed.
     * This occurs when the value's type is not one of the allowed types.
     *
     * @param value the illegal value.
     */
    default void illegalValue(Object value) throws IOException
    {
    }

    /**
     * Invoked when an empty {@link DataMap} is traversed.
     * The {@link #startMap}, {@link #key(String)}, various value,
     * and {@link #endMap} callbacks will not
     * be invoked for an empty {@link DataMap}.
     */
    default void emptyMap() throws IOException
    {
    }

    /**
     * Invoked when the start of {@link DataMap} is traversed.
     *
     * @param map provides the {@link DataMap}to be traversed.
     */
    default void startMap(DataMap map) throws IOException
    {
    }

    /**
     * Invoked when the key of {@link DataMap} entry is traversed.
     * This callback is invoked before the value callback.
     *
     * @param key of the {@link DataMap} entry.
     */
    default void key(String key) throws IOException
    {
    }

    /**
     * Invoked when the end of {@link DataMap} is traversed.
     */
    default void endMap() throws IOException
    {
    }

    /**
     * Invoked when an empty list is traversed.
     * There {@link #startList}, {@link #index(int)}, various value, and
     * {@link #endList} callbacks will not
     * be invoked for an empty {@link DataList}.
     */
    default void emptyList() throws IOException
    {
    }

    /**
     * Invoked when the start of a {@link DataList} is traversed.
     *
     * @param list provides the {@link DataList}to be traversed.
     */
    default void startList(DataList list) throws IOException
    {
    }

    /**
     * Invoked to provide the index of the next {@link DataList} entry.
     * This callback is invoked before the value callback.
     *
     * @param index of the next {@link DataList} entry, starts from 0.
     */
    default void index(int index) throws IOException
    {
    }

    /**
     * Invoked when the end of a {@link DataList} is traversed.
     */
    default void endList() throws IOException
    {
    }

    @Override
    default void close() throws IOException
    {
    }
  }

  /**
   * Traverse object and invoke the callback object with parse events.
   *
   * @param obj object to parse
   * @param callback to receive parse events.
   */
  public static void traverse(Object obj, TraverseCallback callback) throws IOException
  {
    if (obj == null || obj == Data.NULL)
    {
      callback.nullValue();
      return;
    }

    switch (Data.TYPE_MAP.get(obj.getClass()))
    {
      case 1:
        callback.stringValue((String) obj);
        return;
      case 2:
        callback.integerValue((Integer) obj);
        return;
      case 3:
      {
        DataMap map = (DataMap) obj;
        if (map.isEmpty())
        {
          callback.emptyMap();
        }
        else
        {
          callback.startMap(map);
          Iterable<Map.Entry<String, Object>> orderedEntrySet = callback.orderMap(map);
          for (Map.Entry<String, Object> entry : orderedEntrySet)
          {
            callback.key(entry.getKey());
            traverse(entry.getValue(), callback);
          }
          callback.endMap();
        }

        return;
      }
      case 4:
      {
        DataList list = (DataList) obj;
        if (list.isEmpty())
        {
          callback.emptyList();
        }
        else
        {
          callback.startList(list);
          for (int index = 0; index < list.size(); index++)
          {
            callback.index(index);
            traverse(list.get(index), callback);
          }
          callback.endList();
        }

        return;
      }
      case 5:
        callback.booleanValue((Boolean) obj);
        return;
      case 6:
        callback.longValue((Long) obj);
        return;
      case 7:
        callback.floatValue((Float) obj);
        return;
      case 8:
        callback.doubleValue((Double) obj);
        return;
      case 9:
        callback.byteStringValue((ByteString) obj);
        return;
    }

    callback.illegalValue(obj);
  }

  /**
   * Dump Data object with the given name and prefix to the given string builder.
   *
   * @param name of object, may be null or empty string if there is no name.
   * @param obj object to dump.
   * @param prefix for each line of output.
   * @param builder to dump to.
   */
  public static void dump(String name, Object obj, String prefix, StringBuilder builder)
  {
    try
    {
      traverse(obj, new DumpTraverseCallback(name, prefix, builder));
    }
    catch (IOException e)
    {
      builder.append("( Exception: ").append(e).append(" )");
    }
  }

  /**
   * Dump Data object with the given name and prefix to a {@link StringBuilder}.
   *
   * @param name of object.
   * @param obj object to dump.
   * @param prefix for each line of output.
   * @return the dump output.
   */
  public static String dump(String name, Object obj, String prefix)
  {
    StringBuilder builder = new StringBuilder();
    dump(name, obj, prefix, builder);
    return builder.toString();
  }

  /**
   * Return a list of entries of a {@link DataMap} ordered by the values of
   * map's keys.
   *
   * @param map provide the {@link DataMap}.
   * @return a list of the entries of the {@link DataMap} sorted by the map's keys.
   */
  public static List<Map.Entry<String,Object>> orderMapEntries(DataMap map)
  {
    List<Map.Entry<String,Object>> copy = new ArrayList<Map.Entry<String,Object>>(map.entrySet());
    Collections.sort(copy,
                     new Comparator<Map.Entry<String, Object>>()
                     {
                       @Override
                       public int compare(Map.Entry<String, Object> o1,
                                          Map.Entry<String, Object> o2)
                       {
                         return o1.getKey().compareTo(o2.getKey());
                       }
                     });
    return copy;
  }

  /**
   * {@link TraverseCallback} that dumps the contents of Data objects.
   *
   * @author slim
   */
  public static class DumpTraverseCallback implements TraverseCallback
  {
    protected final StringBuilder _builder;
    protected String _prefix;
    protected String _indent = "  ";
    protected String _nameValueSeparator = " : ";
    protected String _listStart = "[";
    protected String _listEnd = "]";
    protected String _mapStart = "{";
    protected String _mapEnd = "}";
    protected String _break = "\n";
    protected String _null = "NULL";

    /**
     * Constructor.
     *
     * @param name is the name of the Data object, may be null if there is no name.
     * @param prefix specifies the prefix to be printed at the beginning of each line.
     * @param builder is the {@link StringBuilder} that the dump should be written to.
     */
    DumpTraverseCallback(String name, String prefix, StringBuilder builder)
    {
      _prefix = prefix;
      _builder = builder;
      _builder.append(_prefix);
      if (name != null && name.isEmpty() == false)
      {
        _builder.append(name).append(_nameValueSeparator);
      }
    }

    @Override
    public Iterable<Map.Entry<String,Object>> orderMap(DataMap map)
    {
      return orderMapEntries(map);
    }

    @Override
    public void nullValue()
    {
      _builder.append(_null).append(_break);
    }

    @Override
    public void booleanValue(boolean value)
    {
      _builder.append(value).append(_break);
    }

    @Override
    public void integerValue(int value)
    {
      _builder.append(value).append(_break);
    }

    @Override
    public void longValue(long value)
    {
      _builder.append(value).append(_break);
    }

    @Override
    public void floatValue(float value)
    {
      _builder.append(value).append(_break);
    }

    @Override
    public void doubleValue(double value)
    {
      _builder.append(value).append(_break);
    }

    @Override
    public void stringValue(String value)
    {
      _builder.append(value).append(_break);
    }

    @Override
    public void byteStringValue(ByteString value)
    {
      _builder.append(value.asAvroString()).append(_break);
    }

    @Override
    public void illegalValue(Object value)
    {
      _builder.append("ILLEGAL VALUE \"").append(value).append("\"");
    }

    @Override
    public void emptyMap()
    {
      _builder.append(_mapStart).append(_mapEnd).append(_break);
    }

    @Override
    public void startMap(DataMap map)
    {
      _builder.append(_mapStart).append(_break);
      indent();
    }

    @Override
    public void key(String key)
    {
      _builder.append(_prefix).append(key).append(_nameValueSeparator);
    }

    @Override
    public void endMap()
    {
      outdent();
      _builder.append(_prefix).append(_mapEnd).append(_break);
    }

    @Override
    public void emptyList()
    {
      _builder.append(_listStart).append(_listEnd).append(_break);
    }

    @Override
    public void startList(DataList list)
    {
      _builder.append(_listStart).append(_break);
      indent();
    }

    @Override
    public void index(int index)
    {
      _builder.append(_prefix);
    }

    @Override
    public void endList()
    {
      outdent();
      _builder.append(_prefix).append(_listEnd).append(_break);
    }

    protected void indent()
    {
      if (_indent.isEmpty() == false)
      {
        _prefix = _prefix + _indent;
      }
    }

    protected void outdent()
    {
      if (_indent.isEmpty() == false)
      {
        _prefix = _prefix.substring(0, _prefix.length() - _indent.length());
      }
    }

    StringBuilder getStringBuilder()
    {
      return _builder;
    }
  }

  /**
   * Throws {@link IllegalArgumentException} if the value object is not a Data object or
   * the parent object is reachable from the new object.  Throws NullPointerException
   * if the value object is null.
   *
   * @see #isAllowed(Object)
   *
   * @param parent is the object about to add the value object.
   * @param value is object to about to be added as a value to the parent object.
   * @throws IllegalArgumentException if the the value object is not a Data object or
   *                                  the parent object is reachable from the new object.
   * @throws NullPointerException if the value object is null
   */
  static void checkAllowed(DataComplex parent, Object value) throws IllegalArgumentException
  {
    ArgumentUtil.notNull(value, "value");
    Class<?> clas = value.getClass();
    if (isPrimitiveClass(clas))
    {
      // nothing more to check
    }
    else if (isComplex(value))
    {
      // check reachability
      if (parent == value || reachable((DataComplex) value, parent))
      {
        throw new IllegalArgumentException("Adding value to Data object will result in a loop");
      }
    }
    else
    {
      throw new IllegalArgumentException("Type is not allowed: " + clas);
    }
  }

  /**
   * Return whether a destination complex object is reachable from a
   * source complex object.
   *
   * This method assumes all source complex object is acyclic.
   * It will not terminate if the complex object is not acyclic.
   *
   * @param source is where to start search for the destination.
   * @param destination is the object to find.
   * @return true if destination is reachable from source.
   */
  private static boolean reachable(DataComplex source, Object destination)
  {
    Collection<Object> values = source.values();
    for (Object value : values)
    {
      if (value == destination ||
          value instanceof DataComplex && reachable((DataComplex) value, destination)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return whether the input object is Data object.
   *
   * A Data object must be a primitive or complex object (and cannot be null.)
   *
   * @see #isPrimitive(Object)
   * @see #isComplex(Object)
   *
   * @param o is the object to check.
   * @return true if the object is a Data object.
   */
  static boolean isAllowed(Object o)
  {
    return o != null && (isPrimitive(o) || isComplex(o));
  }
  /**
   * Return whether an object of the input class is a Data object.
   *
   * A Data object must be a primitive or complex object.
   *
   * @see #isPrimitiveClass(Class)
   * @see #isComplexClass(Class)
   *
   * @param clas is the class to check.
   * @return true if an object of the input class is a Data object.
   */
  static boolean isAllowedClass(Class<?> clas)
  {
    return isPrimitiveClass(clas) || isComplexClass(clas);
  }
  /**
   * Return whether the input object is primitive object.
   *
   * @see #isPrimitiveClass(Class)
   *
   * @param o is the object to check.
   * @return true if the object is a primitive object.
   */
  static boolean isPrimitive(Object o)
  {
    return isPrimitiveClass(o.getClass());
  }
  /**
   * Return whether an object of the input class is a primitive object.
   *
   * A primitive object's type must be a {@link Boolean}, {@link Integer},
   * {@link Long}, {@link Float}, {@link Double}, {@link String},
   * {@link ByteString} or {@link Null} and not a sub-class of these
   * classes.
   *
   * @param clas is the class to check.
   * @return true if an object of the input class is a primitive object.
   */
  static boolean isPrimitiveClass(Class<?> clas)
  {
    return
      clas == String.class ||
      clas == Integer.class ||
      clas == Double.class ||
      clas == Boolean.class ||
      clas == Long.class ||
      clas == Float.class ||
      clas == ByteString.class ||
      clas == Null.class;
  }
  /**
   * Return whether the input object is complex object.
   *
   * @see #isPrimitive(Object)
   *
   * @param o is the object to check.
   * @return true if the object is a complex object.
   */
  static boolean isComplex(Object o)
  {
    return isComplexClass(o.getClass());
  }
  /**
   * Return whether an object of the input class is a complex object.
   *
   * A complex object's type must be a {@link DataList} or {@link DataMap}
   * and not a sub-class of these classes.
   *
   * @param clas is the class to check.
   * @return true if an object of the input class is a complex object.
   */
  static boolean isComplexClass(Class<?> clas)
  {
    return
      clas == DataMap.class ||
      clas == DataList.class;
  }

  /**
   * Deep copy a Data object.
   *
   * Recursively invoke clone on complex objects and storing
   * the clone in the cloned containing object. Primitive objects
   * are not cloned.
   *
   * @param object is the object to deep copy.
   * @param alreadyCopied provides the objects that have already been copied.
   * @return the copy.
   * @throws CloneNotSupportedException if the complex object cannot be deep copied.
   */
  static <T> T copy(T object, DataComplexTable alreadyCopied) throws CloneNotSupportedException
  {
    if (object == null)
    {
      return null;
    }
    else if (isComplex(object))
    {
      DataComplex src = (DataComplex) object;

      @SuppressWarnings("unchecked")
      T found = (T) alreadyCopied.get(src);

      if (found != null)
      {
        return found;
      }
      else
      {
        DataComplex clone = src.clone();
        alreadyCopied.put(src, clone);

        if (clone instanceof DataMap)
        {
          ((DataMap)clone).copyReferencedObjects(alreadyCopied);
        }
        else if (clone instanceof DataList)
        {
          ((DataList)clone).copyReferencedObjects(alreadyCopied);
        }

        @SuppressWarnings("unchecked")
        T converted = (T) clone;
        return converted;
      }
    }
    else if (isPrimitive(object))
    {
      return object;
    }
    else
    {
      throw new CloneNotSupportedException("Illegal value encountered: " + object);
    }
  }

  /**
   * Make a Data object and its contained mutable Data objects read-only.
   *
   * @param o is the Data object to make read-only.
   */
  static void makeReadOnly(Object o)
  {
    if (isComplex(o))
    {
      ((DataComplex) o).makeReadOnly();
    }
  }

  /**
   * Get string from bytes following Avro convention.
   *
   * This method expands each byte into a character in the output string by encoding
   * the byte's value into the least significant 8-bits of the character. The returned
   * string will have the same length as the byte array, i.e. if there are 8 bytes in
   * the byte array, the string will have 8 characters.
   *
   * @param input byte array to get string from.
   * @param offset the offset to read in the input byte array
   * @param length the length to read in the input byte array
   * @return string whose least significant 8-bits of each character represents one byte.
   */
  public static String bytesToString(byte[] input, int offset, int length)
  {
    return new String(bytesToCharArray(input, offset, length));
  }

  /**
   * Get character array from bytes following Avro convention.
   *
   * This method expands each byte into a character in the output array by encoding
   * the byte's value into the least significant 8-bits of the character. The returned
   * array will have the same length as the byte array, i.e. if there are 8 bytes in
   * the byte array, the array will have 8 characters.
   *
   * @param input byte array to get characters from.
   * @param offset the offset to read in the input byte array
   * @param length the length to read in the input byte array
   * @return array whose least significant 8-bits of each character represents one byte.
   */
  public static char[] bytesToCharArray(byte[] input, int offset, int length)
  {
    char[] charArray = new char[length];
    bytesToCharArray(input, offset, length, charArray, 0);

    return charArray;
  }

  /**
   * Store character array retrieved from bytes following Avro convention.
   *
   * This method expands each byte into a character in the output array by encoding
   * the byte's value into the least significant 8-bits of the character. The returned
   * array will have the same length as the byte array, i.e. if there are 8 bytes in
   * the byte array, the array will have 8 characters.
   *
   * @param input byte array to get characters from.
   * @param offset the offset to read in the input byte array
   * @param length the length to read in the input byte array
   * @param dest the destination character array.
   * @param destOffset the offset to start writing from in the destination character array.
   */
  public static void bytesToCharArray(byte[] input, int offset, int length, char[] dest, int destOffset)
  {
    ArgumentUtil.checkBounds(input.length, offset, length);
    ArgumentUtil.checkBounds(dest.length, destOffset, length);

    for (int i = 0; i < length; i++)
    {
      dest[destOffset++] = (char) (((char) input[i + offset]) & 0x00ff);
    }
  }

  /**
   * Get string from bytes following Avro convention.
   *
   * This method expands each byte into a character in the output string by encoding
   * the byte's value into the least significant 8-bits of the character. The returned
   * string will have the same length as the byte array, i.e. if there are 8 bytes in
   * the byte array, the string will have 8 characters.
   *
   * @param input byte array to get string from.
   * @return string whose least significant 8-bits of each character represents one byte.
   */
  public static String bytesToString(byte[] input)
  {
    return bytesToString(input, 0, input.length);
  }

  /**
   * Get bytes from string following Avro convention.
   *
   * This method extracts the least significant 8-bits of each character in the string
   * (following Avro convention.) The returned byte array is the same length as the
   * string, i.e. if there are 8 characters in the string, the byte array will have 8 bytes.
   *
   * Validation is optional. If validation is enabled, then the input is valid if
   * the most significant 8-bits of all characters is always 0.
   *
   * @param input string to get bytes from.
   * @param validate indicates whether validation is enabled, validation is enabled if true.
   * @return extracted bytes if the string is valid or validation is not enabled, else return null.
   */
  public static byte[] stringToBytes(String input, boolean validate)
  {
    char orChar = 0;
    int length = input.length();
    byte[] bytes = new byte[length];
    for (int i = 0; i < length; ++i)
    {
      char c = input.charAt(i);
      orChar |=  c;
      bytes[i] = (byte) (c & 0x00ff);
    }
    if (validate && (orChar & 0xff00) != 0)
    {
      return null;
    }
    return bytes;
  }
  /**
   * Validate string is a valid encoding of bytes following Avro convention.
   *
   * The input string is valid if the most significant 8-bits of all characters is always 0.
   *
   * @param input string to validate.
   * @return true if string is a valid encoding of bytes following Avro convention.
   */
  public static boolean validStringAsBytes(String input)
  {
    char orChar = 0;
    int length = input.length();
    for (int i = 0; i < length; ++i)
    {
      char c = input.charAt(i);
      orChar |=  c;
    }
    return ((orChar & 0xff00) == 0);
  }

  /**
   * Validate that the Data object is acyclic, i.e. has no loops.
   *
   * @param o is the Data object to validate.
   * @return true if the Data object is a acyclic, else return false.
   */
  static boolean objectIsAcyclic(Object o)
  {
    return new Object()
    {
      private IdentityHashMap<DataComplex, Boolean> _visited = new IdentityHashMap<DataComplex, Boolean>();
      private IdentityHashMap<DataComplex, Boolean> _path = new IdentityHashMap<DataComplex, Boolean>();

      boolean objectIsAcyclic(Object object)
      {
        if (object == null)
        {
          return true;
        }
        Class<?> clas = object.getClass();
        if (isPrimitiveClass(clas))
        {
          return true;
        }
        else if (isComplex(object))
        {
          DataComplex mutable = (DataComplex) object;
          Collection<Object> values = mutable.values();
          Boolean loop = _path.put(mutable, Boolean.TRUE);
          if (loop == Boolean.TRUE)
          {
            // already seen this object in path to root
            // must be in a loop
            return false;
          }
          // mark as visited to avoid traversing again
          Boolean visited = _visited.put(mutable, Boolean.TRUE);
          if (visited == null)
          {
            // have not visited this object
            for (Object value : values)
            {
              if (objectIsAcyclic(value) == false)
              {
                return false;
              }
            }
            // remove object from path to root
          }
          _path.remove(mutable);
          return true;
        }
        else
        {
          throw new IllegalStateException("Object of unknown type: " + object);
        }
      }
    }.objectIsAcyclic(o);
  }

  /**
   * Output the names that addresses a Data object within
   * a complex object.
   *
   * The input is a collection of names. Each name
   * is either a String addresses a field name within a {@link DataMap} or
   * an Integer that addresses an element within a {@link DataList}.
   *
   * For a field name, the appended output is a dot (".") followed
   * the field name, unless the field name is the first element
   * in the collection, then only the field name is appended.
   *
   * For an index, the appended output is a the index surrounded
   * by square brackets, e.g. "[0]".
   *
   * @param builder is the string builder to append the names.
   * @param names is the collection of names.
   */
  static public void appendNames(StringBuilder builder, Collection<Object> names)
  {
    boolean first = true;
    for (Object name : names)
    {
      if (name instanceof Integer)
      {
        builder.append('[').append(name).append(']');
      }
      else
      {
        if (first == false)
        {
          builder.append('.');
        }
        builder.append(name);
      }
      first = false;
    }
  }

  /**
   * Start instrumenting access on a collection of Data objects.
   *
   * @param c provides the collection of Data objects.
   */
  static void startInstrumentingAccess(Collection<Object> c)
  {
    for (Object o : c)
    {
      if (o instanceof Instrumentable)
      {
        ((Instrumentable) o).startInstrumentingAccess();
      }
    }
  }

  /**
   * Stop instrumenting access on a collection of Data objects.
   *
   * @param c provides the collection of Data objects.
   */
  static void stopInstrumentingAccess(Collection<Object> c)
  {
    for (Object o : c)
    {
      if (o instanceof Instrumentable)
      {
        ((Instrumentable) o).stopInstrumentingAccess();
      }
    }
  }

  /**
   * If the Data object is a complex Data object, recursively collect instrumented data
   * from the complex Data object. Otherwise, if {@code collectAllData} is true or
   * {@code timesAccessed} > 0, then add an entry with number of
   * times access and another entry with a String representation of object
   * to the instrumented data map.
   *
   * @param key is the key for the entry to be added to the {@code instrumentedData} map, or
   *            the prefix for entries to be collected from the complex Data object.
   * @param object provides the Data object.
   * @param timesAccessed provides the number of times accessed.
   * @param instrumentedData provides the map used to collect instrumented data.
   * @param collectAllData whether to collect all Data object or only data for
   *                       keys or indices that have been accessed.
   */
  static void collectInstrumentedData(StringBuilder key,
                                      Object object,
                                      Integer timesAccessed,
                                      Map<String, Map<String, Object>> instrumentedData,
                                      boolean collectAllData)
  {
    if (isComplex(object))
    {
      ((DataComplex) object).collectInstrumentedData(key, instrumentedData, collectAllData);
    }
    else if (collectAllData || timesAccessed > 0)
    {
      InstrumentationUtil.emitInstrumentationData(key, object, timesAccessed, instrumentedData);
    }
  }
}
