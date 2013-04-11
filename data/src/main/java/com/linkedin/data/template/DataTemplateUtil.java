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

package com.linkedin.data.template;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.DataSchemaUtil;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.SchemaParserFactory;

public class DataTemplateUtil
{
  public static final String SCHEMA_FIELD_NAME = "SCHEMA";
  public static final String TYPEREFINFO_FIELD_NAME = "TYPEREFINFO";
  public static final String UNKNOWN_ENUM = "$UNKNOWN";
  public static final PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out));
  private static final boolean debug = false;

  private DataTemplateUtil()
  {
  }

  /**
   * Get the constructor from the provided concrete {@link DataTemplate} class that can be used
   * in the future to wrap Data objects.
   *
   * It has knowledge of the argument type expected by the constructor for the different {@link DataTemplate} classes.
   *
   * @param templateClass provides the concrete {@link DataTemplate} class to look for the constructor.
   * @param <T> provides the concrete {@link DataTemplate} type.
   * @return a {@link Constructor} that can be used to construct instances of the provided {@link DataTemplate} class.
   * @throws TemplateOutputCastException if a constructor with the appropriate method signature
   *                                     for the provided {@link DataTemplate} class cannot be obtained.
   */
  public static <T extends DataTemplate<?>> Constructor<T> templateConstructor(Class<T> templateClass)
      throws TemplateOutputCastException
  {
    try
    {
      if (RecordTemplate.class.isAssignableFrom(templateClass) ||
          AbstractMapTemplate.class.isAssignableFrom(templateClass))
      {
        return templateClass.getConstructor(DataMap.class);
      }
      else if (AbstractArrayTemplate.class.isAssignableFrom(templateClass))
      {
        return templateClass.getConstructor(DataList.class);
      }
      else if (FixedTemplate.class.isAssignableFrom(templateClass) ||
               UnionTemplate.class.isAssignableFrom(templateClass))
      {
        return templateClass.getConstructor(Object.class);
      }
      else
      {
        throw new TemplateOutputCastException("No DataTemplate for " + templateClass.getName());
      }
    }
    catch (SecurityException e)
    {
      throw new TemplateOutputCastException(templateClass.getName() + " get constructor failed", e);
    }
    catch (NoSuchMethodException e)
    {
      throw new TemplateOutputCastException(templateClass.getName() + " does not have required constructor", e);
    }
  }

  /**
   * Wrap a Data object by using the provided concrete {@link DataTemplate} class to create an instance of the class
   * that wraps the Data object.
   *
   * @param object provides the Data object to wrap.
   * @param wrapperClass provides the concrete {@link DataTemplate} class.
   * @param <T> provides the concrete {@link DataTemplate} type.
   * @return an instance of the provided {@link DataTemplate} class that wraps the Data object.
   * @throws TemplateOutputCastException if the provided object cannot be wrapped by the provided concrete
   *                                     {@link DataTemplate} class.
   */
  public static <T extends DataTemplate<?>> T wrap(Object object, Class<T> wrapperClass)
    throws TemplateOutputCastException
  {
    try
    {
      return templateConstructor(wrapperClass).newInstance(object);
    }
    catch (IllegalArgumentException e)
    {
      throw new TemplateOutputCastException("Cannot create wrapper: " + wrapperClass.getName(), e);
    }
    catch (InstantiationException e)
    {
      throw new TemplateOutputCastException("Cannot create wrapper: " + wrapperClass.getName(), e);
    }
    catch (IllegalAccessException e)
    {
      throw new TemplateOutputCastException("Cannot create wrapper: " + wrapperClass.getName(), e);
    }
    catch (InvocationTargetException e)
    {
      throw new TemplateOutputCastException("Cannot create wrapper: " + wrapperClass.getName(), e);
    }
    catch (ClassCastException e)
    {
      throw new TemplateOutputCastException("Cannot create wrapper: " + wrapperClass.getName(), e);
    }
  }

  /**
   * Get the constructor from the provided concrete {@link DataTemplate} class that can be used
   * in the future to wrap Data objects.
   *
   * This method is usually used to pre-fetch the constructor likely to be used multiple times, e.g.
   * getting the constructor for the value types of {@link WrappingMapTemplate} or the element types of
   * {@link WrappingArrayTemplate}.
   *
   * It has knowledge of the argument type expected by the constructor for the different {@link DataSchema} types
   * associated with the {@link DataTemplate} classes.
   *
   * @param templateClass provides the concrete {@link DataTemplate} class to look for the constructor.
   * @param schema provides the {@link DataSchema} of the provided {@link DataTemplate} class.
   * @param <T> provides the concrete {@link DataTemplate} type.
   * @return a {@link Constructor} that can be used to construct instances of the provided {@link DataTemplate} class.
   * @throws TemplateOutputCastException if a constructor with the appropriate method signature
   *                                     for the provided {@link DataSchema} cannot be obtained.
   */
  public static <T extends DataTemplate<?>> Constructor<T> templateConstructor(Class<T> templateClass, DataSchema schema)
      throws TemplateOutputCastException
  {
    try
    {
      switch (schema.getDereferencedType())
      {
        case MAP:
        case RECORD:
          return templateClass.getConstructor(DataMap.class);
        case ARRAY:
          return templateClass.getConstructor(DataList.class);
        case FIXED:
        case UNION:
          return templateClass.getConstructor(Object.class);
        default:
          throw new TemplateOutputCastException("No DataTemplate for " + templateClass.getName());
      }
    }
    catch (SecurityException e)
    {
      throw new TemplateOutputCastException(templateClass.getName() + " get constructor failed", e);
    }
    catch (NoSuchMethodException e)
    {
      throw new TemplateOutputCastException(templateClass.getName() + " does not have required constructor", e);
    }
  }

  /**
   * Wrap a Data object by using the provided concrete {@link DataTemplate} class to create an instance of the class
   * that wraps the Data object.
   *
   * @param object provides the Data object to wrap.
   * @param schema provides the {@link DataSchema} of the concrete {@link DataTemplate} class.
   * @param wrapperClass provides the concrete {@link DataTemplate} class.
   * @param <T> provides the concrete {@link DataTemplate} type.
   * @return an instance of the provided {@link DataTemplate} class that wraps the Data object.
   * @throws TemplateOutputCastException if the provided object cannot be wrapped by the provided concrete
   *                                     {@link DataTemplate} class.
   */
  public static <T extends DataTemplate<?>> T wrap(Object object, DataSchema schema, Class<T> wrapperClass)
    throws TemplateOutputCastException
  {
    try
    {
      return templateConstructor(wrapperClass, schema).newInstance(object);
    }
    catch (IllegalArgumentException e)
    {
      throw new TemplateOutputCastException("Cannot create wrapper: " + wrapperClass.getName(), e);
    }
    catch (InstantiationException e)
    {
      throw new TemplateOutputCastException("Cannot create wrapper: " + wrapperClass.getName(), e);
    }
    catch (IllegalAccessException e)
    {
      throw new TemplateOutputCastException("Cannot create wrapper: " + wrapperClass.getName(), e);
    }
    catch (InvocationTargetException e)
    {
      throw new TemplateOutputCastException("Cannot create wrapper: " + wrapperClass.getName(), e);
    }
    catch (ClassCastException e)
    {
      throw new TemplateOutputCastException("Cannot create wrapper: " + wrapperClass.getName(), e);
    }
  }

  /**
   * Wrap a Data object by using the provided constructor to create a {@link DataTemplate} to wrap the Data object.
   *
   * @param object provides the Data object to wrap.
   * @param constructor provides the constructor previously obtained from
   *                    {@link DataTemplateUtil#templateConstructor(Class, DataSchema)}.
   * @param <T> provides the concrete {@link DataTemplate} type.
   * @return an instance of {@link DataTemplate} that wraps the Data object.
   * @throws TemplateOutputCastException if a {@link DataTemplate} cannot be created to wrap the provided Data object using
   *                                     the provided constructor.
   */
  public static <T extends DataTemplate<?>> T wrap(Object object, Constructor<T> constructor)
  {
    try
    {
      return constructor.newInstance(object);
    }
    catch (IllegalArgumentException e)
    {
      throw new TemplateOutputCastException("Cannot create wrapper with " + constructor, e);
    }
    catch (InstantiationException e)
    {
      throw new TemplateOutputCastException("Cannot create wrapper with " + constructor, e);
    }
    catch (IllegalAccessException e)
    {
      throw new TemplateOutputCastException("Cannot create wrapper with " + constructor, e);
    }
    catch (InvocationTargetException e)
    {
      throw new TemplateOutputCastException("Cannot create wrapper with " + constructor, e);
    }
    catch (ClassCastException e)
    {
      throw new TemplateOutputCastException("Cannot create wrapper with " + constructor, e);
    }
  }

  /**
   * Parse data schema in JSON format to obtain a {@link DataSchema}.
   *
   * @param schemaText provides the data schema in JSON format.
   * @return the {@link DataSchema} parsed from the data schema in JSON format.
   * @throws IllegalArgumentException if the data schema in JSON format is invalid or
   *                                  there is more than one top level schema.
   */
  public static DataSchema parseSchema(String schemaText) throws IllegalArgumentException
  {
    return parseSchema(schemaText, null);
  }

  /**
   * Parse data schema in JSON format to obtain a {@link DataSchema}.
   *
   * @param schemaText provides the data schema in JSON format.
   * @param schemaResolver for resolving referenced schemas
   * @return the {@link DataSchema} parsed from the data schema in JSON format.
   * @throws IllegalArgumentException if the data schema in JSON format is invalid or
   *                                  there is more than one top level schema.
   */
  public static DataSchema parseSchema(String schemaText, DataSchemaResolver schemaResolver) throws IllegalArgumentException
  {
    SchemaParser parser = SchemaParserFactory.instance().create(schemaResolver);
    parser.parse(schemaText);
    if (parser.hasError())
    {
      if (debug)
      {
        out.println(parser.errorMessage());
      }
      throw new IllegalArgumentException(parser.errorMessage());
    }
    if (parser.topLevelDataSchemas().size() != 1)
    {
      throw new IllegalArgumentException("More than one top level schemas");
    }

    return parser.topLevelDataSchemas().get(0);
  }

  /**
   * Gets the {@link TyperefInfo} for a given data template.
   *
   * The data template has a {@link TyperefInfo} if it extends {@link HasTyperefInfo}
   * and has private static final holding the {@link TyperefInfo}.
   *
   * @param type is a generated data template.
   * @return null if the generated data template does not have a {@link TyperefInfo}
   *         else return the {@link TyperefInfo} of the data template.
   */
  public static TyperefInfo getTyperefInfo(Class<? extends DataTemplate> type)
  {
    TyperefInfo typerefInfo;
    if (HasTyperefInfo.class.isAssignableFrom(type))
    {
      try
      {
        Field typerefInfoField = type.getDeclaredField(TYPEREFINFO_FIELD_NAME);
        typerefInfoField.setAccessible(true);
        typerefInfo = (TyperefInfo) typerefInfoField.get(null);
      }
      catch (IllegalAccessException e)
      {
        throw new RuntimeException("Error evaluating TyperefInfo for class: " + type.getName(), e);
      }
      catch (NoSuchFieldException e)
      {
        typerefInfo = null;
      }
    }
    else
    {
      typerefInfo = null;
    }
    return typerefInfo;
  }

  /**
   * Gets the data schema for a given java type.
   *
   * @param type to get a schema for. Has to be primitive or a generated data template.
   * @throws TemplateRuntimeException if the {@link DataSchema} for the specified type cannot be provided.
   */
  public static DataSchema getSchema(Class<?> type) throws TemplateRuntimeException
  {
    final DataSchema primitiveSchema = DataSchemaUtil.classToPrimitiveDataSchema(type);
    if (primitiveSchema != null)
    {
      return primitiveSchema;
    }

    try
    {
      Field schemaField = type.getDeclaredField(SCHEMA_FIELD_NAME);
      schemaField.setAccessible(true);
      DataSchema schema = (DataSchema) schemaField.get(null);
      if (schema == null)
      {
        throw new TemplateRuntimeException("Class missing schema: " + type.getName());
      }

      return schema;
    }
    catch (IllegalAccessException e)
    {
      throw new TemplateRuntimeException("Error evaluating schema name for class: " + type.getName(), e);
    }
    catch (NoSuchFieldException e)
    {
      throw new TemplateRuntimeException("Class missing schema field: " + type.getName(), e);
    }
  }

  /**
   * Gets the data schema name for a given java type.
   * @param type to get a schema for. Has to be a named data type.
   */
  public static String getSchemaName(Class<?> type)
  {
    DataSchema schema = getSchema(type);
    if (! (schema instanceof NamedDataSchema))
    {
      throw new TemplateRuntimeException("Class' schema is unnamed: " + type.getName());
    }

    return ((NamedDataSchema) schema).getFullName();
  }

  private abstract static class NativeCoercer<T> implements DirectCoercer<T>
  {
    protected Class<T> _targetClass;

    protected NativeCoercer(Class<T> targetClass)
    {
      _targetClass = targetClass;
    }

    @Override
    public Object coerceInput(T object) throws ClassCastException
    {
      // this code should never be called unless the client is using raw types
      // that bypass compile time checks.
      throw new ClassCastException("Input " + object + " is not a " + _targetClass.getName());
    }
  }

  private abstract static class NumberCoercer<T extends Number> implements DirectCoercer<T>
  {
    protected Class<T> _targetClass;

    NumberCoercer(Class<T> targetClass)
    {
      _targetClass = targetClass;
    }

    @Override
    public Object coerceInput(T object) throws ClassCastException
    {
      if (object instanceof Number)
      {
        return coerce(object);
      }
      else
      {
        throw new ClassCastException("Input " + object + " is a not a Number or a " + _targetClass.getName());
      }
    }

    @Override
    public T coerceOutput(Object object) throws TemplateOutputCastException
    {
      if (object instanceof Number)
      {
        return coerce(object);
      }
      else
      {
        throw new TemplateOutputCastException("Output " + object + " is a not a Number or a " + _targetClass.getName());
      }
    }

    protected abstract T coerce(Object object);
  }

  /**
   * Coerces Boolean objects to boolean primitives.
   */
  private static class BooleanCoercer extends NativeCoercer<Boolean>
  {
    BooleanCoercer()
    {
      super(Boolean.class);
    }

    @Override
    public Boolean coerceOutput(Object object) throws TemplateOutputCastException
    {
      if (object instanceof Boolean)
      {
        return (Boolean) object;
      }

      throw new TemplateOutputCastException("Output " + object + " is a not a " + _targetClass.getName());
    }
  }

  private static class BytesCoercer extends NativeCoercer<ByteString>
  {
    BytesCoercer()
    {
      super(ByteString.class);
    }

    @Override
    public ByteString coerceOutput(Object object) throws TemplateOutputCastException
    {
      if (object.getClass() == String.class)
      {
        String input = (String) object;
        ByteString bytes = ByteString.copyAvroString(input, true);
        if (bytes == null)
        {
          throw new TemplateOutputCastException("Output " + object + " is not a valid string encoding of bytes");
        }
        return bytes;
      }
      else
      {
        throw new TemplateOutputCastException("Output " + object + " is a not a " + String.class.getName() + " or " + _targetClass.getName());
      }
    }
  }

  private static class IntegerCoercer extends NumberCoercer<Integer>
  {
    IntegerCoercer()
    {
      super(Integer.class);
    }

    @Override
    protected Integer coerce(Object object)
    {
      return ((Number) object).intValue();
    }
  }

  private static class LongCoercer extends NumberCoercer<Long>
  {
    LongCoercer()
    {
      super(Long.class);
    }

    @Override
    protected Long coerce(Object object)
    {
      return ((Number) object).longValue();
    }
  }

  private static class FloatCoercer extends NumberCoercer<Float>
  {
    FloatCoercer()
    {
      super(Float.class);
    }

    @Override
    protected Float coerce(Object object)
    {
      return ((Number) object).floatValue();
    }
  }

  private static class DoubleCoercer extends NumberCoercer<Double>
  {
    DoubleCoercer()
    {
      super(Double.class);
    }

    @Override
    protected Double coerce(Object object)
    {
      return ((Number) object).doubleValue();
    }
  }

  private static final Object _classToCoercerMutex = new Object();
  private static Map<Class<?>, DirectCoercer<?>> _classToCoercerMap;
  private static final DirectCoercer<Integer> INTEGER_COERCER = new IntegerCoercer();
  private static final DirectCoercer<Long> LONG_COERCER = new LongCoercer();
  private static final DirectCoercer<Float> FLOAT_COERCER = new FloatCoercer();
  private static final DirectCoercer<Double> DOUBLE_COERCER = new DoubleCoercer();
  private static final DirectCoercer<ByteString> BYTES_COERCER = new BytesCoercer();
  private static final BooleanCoercer BOOLEAN_COERCER = new BooleanCoercer();

  static
  {
    IdentityHashMap<Class<?>, DirectCoercer<?>> map = new IdentityHashMap<Class<?>, DirectCoercer<?>>();
    map.put(Integer.TYPE, INTEGER_COERCER);
    map.put(Integer.class, INTEGER_COERCER);
    map.put(Long.TYPE, LONG_COERCER);
    map.put(Long.class, LONG_COERCER);
    map.put(Float.TYPE, FLOAT_COERCER);
    map.put(Float.class, FLOAT_COERCER);
    map.put(Double.TYPE, DOUBLE_COERCER);
    map.put(Double.class, DOUBLE_COERCER);
    map.put(ByteString.class, BYTES_COERCER);
    map.put(Boolean.TYPE, BOOLEAN_COERCER);
    _classToCoercerMap = Collections.unmodifiableMap(map);
  }

  /* package scope */
  static <T> void registerCoercer(Class<T> targetClass, DirectCoercer<T> coercer) throws IllegalArgumentException
  {
    synchronized (_classToCoercerMutex)
    {
      if (_classToCoercerMap.containsKey(targetClass))
      {
        DirectCoercer<?> existingCoercer = _classToCoercerMap.get(targetClass);

        // allow re-registration of a coercer of the same class name, this can happen if there when there are multiple classloaders
        if(existingCoercer == null || !existingCoercer.getClass().equals(coercer.getClass())) 
        {
          throw new IllegalArgumentException(targetClass.getName() + " already has a coercer");
        }
      }
      Map<Class<?>, DirectCoercer<?>> newMap = new IdentityHashMap<Class<?>, DirectCoercer<?>>(_classToCoercerMap);
      newMap.put(targetClass, coercer);
      _classToCoercerMap = Collections.unmodifiableMap(newMap);
    }
  }

  public static boolean hasCoercer(Class<?> klass)
  {
    return _classToCoercerMap.containsKey(klass);
  }

  /**
   * Coerce an input value so that it can stored in {@link DataMap} or {@link DataList}.
   *
   * <p>
   * Coercion is used to convert from one Java representation of a primitive and enum
   * type to another Java representation.
   * <p>
   * Coercion is used for types that are not wrapped by a {@link DataTemplate}.
   * Specifically, coercion is used with the primitive schema types and enums.
   * It is used to coerce from one numeric type to another, e.g. Long to Integer.
   * <p>
   * It is also used when there is a custom Java class binding for a primitive type.
   * For example, there could be a custom Java class binding that binds a long schema type
   * to the {@link java.util.Date} class. The binding depends on a {@link DirectCoercer}
   * has been registered using {@link Custom#registerCoercer}. Once the coercer has been
   * registered, this method could be invoked to coerce from a {@link java.util.Date}
   * (which cannot be stored in a {@link DataMap} or {@link DataList}) to
   * a {@link Long} (which is the Java representation that can be stored in the
   * a {@link DataMap} or {@link DataList}). This is accomplished by specifying
   * by specifying the {@link Long} class as the {@code fromClass} and
   * the {@link java.util.Date} as the {@code toClass}.
   * <p>
   *
   * @param object provides the input value to be coerced.
   * @param fromClass provides the expected class of the specified object. The object's
   *                  actual class may or may not be the same as {@code fromClass} due to
   *                  erasure when using generics.
   * @param toClass provides the class that the coerced value should be. It should be
   *                class that can be stored into a {@link DataMap} or {@link DataList}.
   * @param <T> provides the expected type of the input value.
   * @return the coerced value.
   * @throws ClassCastException if the input value cannot be coerced to the specified {@code toClass}.
   */
  public static <T> Object coerceInput(T object, Class<T> fromClass, Class<?> toClass)
      throws ClassCastException
  {
    if (object.getClass() == fromClass)
    {
      if (fromClass == toClass)
        return object;
      if (fromClass.isEnum())
        return object.toString();
    }
    @SuppressWarnings("unchecked") DirectCoercer<T> coercer = (DirectCoercer<T>) _classToCoercerMap.get(fromClass);
    if (coercer == null)
    {
      throw new ClassCastException("Input " + object + " cannot be coerced from " + fromClass.getName());
    }
    else
    {
      return coercer.coerceInput(object);
    }
  }

  @SuppressWarnings("unchecked")
  private static final Object stringToEnum(@SuppressWarnings("rawtypes") Class targetClass, String value)
      throws TemplateOutputCastException
  {
    try
    {
      return Enum.valueOf(targetClass, value);
    }
    catch (IllegalArgumentException ignored)
    {
      try
      {
        return Enum.valueOf(targetClass, UNKNOWN_ENUM);
      }
      catch (IllegalArgumentException exc)
      {
        throw new TemplateOutputCastException("Enum " + targetClass.getName() + " does not have " + value + " as member", exc);
      }
    }
  }

  /**
   * Coerce an internal representation to the desired target class.
   *
   * <p>
   * See {@link #coerceInput}. Unlike {@link #coerceInput}, this method
   * coerces from a type that can be stored in {@link DataMap} or
   * {@link DataList} to the Java class that best represents the type.
   * For example, for an enum type, it coerces from a {@link String}
   * representation of the enum value to the corresponding Java enum
   * constant. Similarly, it can coerce a {@link Long} to a {@link java.util.Date}
   * provided a coercer has been registered for this {@link java.util.Date}.
   *
   * @param object provides the value that should be coerced to {@code targetClass}.
   * @param targetClass provides the class that the value should be coerced to.
   * @param <T> provides the type of coerced output value.
   * @return the coerced output value.
   * @throws TemplateOutputCastException if the specified object cannot be coerced to the
   *                                     specified target class.
   */
  @SuppressWarnings("unchecked")
  public static <T> T coerceOutput(Object object, Class<T> targetClass)
      throws TemplateOutputCastException
  {
    Class<?> objectClass = object.getClass();
    if (objectClass == targetClass)
    {
      return (T) object;
    }
    if (targetClass.isEnum())
    {
      if (objectClass == String.class)
      {
        return (T) stringToEnum(targetClass, (String) object);
      }
      throw new TemplateOutputCastException("Output " + object + " cannot be coerced to enum " + targetClass.getName());
    }
    DirectCoercer<?> coercer = _classToCoercerMap.get(targetClass);
    if (coercer == null)
    {
      throw new TemplateOutputCastException("Output " + object + " is not a and cannot be coerced to " + targetClass.getName());
    }
    else
    {
      return (T) coercer.coerceOutput(object);
    }
  }

  /* package scope */
  static void initializeClass(Class<?> clazz)
  {
    Exception exc;

    try
    {
      Class.forName(clazz.getName(), true, clazz.getClassLoader());
    }
    catch (ClassNotFoundException e)
    {
      exc = e;
      throw new IllegalArgumentException(clazz + " cannot be initialized", exc);
    }
  }

}
