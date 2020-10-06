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


import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.DataSchemaUtil;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.SchemaFormatType;
import com.linkedin.data.schema.PegasusSchemaParser;
import com.linkedin.data.schema.validation.CoercionMode;
import com.linkedin.data.schema.validation.RequiredMode;
import com.linkedin.data.schema.validation.UnrecognizedFieldMode;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DataTemplateUtil
{
  public static final String SCHEMA_FIELD_NAME = "SCHEMA";
  public static final String TYPEREFINFO_FIELD_NAME = "TYPEREFINFO";
  public static final String UNKNOWN_ENUM = "$UNKNOWN";
  public static final PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out));
  private static final boolean debug = false;
  // Cache to speed up data schema retrieval
  private static final Map<Class<?>, DataSchema> _classToSchemaMap = new ConcurrentHashMap<>();

  private DataTemplateUtil()
  {
  }

  /**
   * Cast the given value to the given class. If the cast fails, throw a {@link TemplateOutputCastException}. If the
   * input is null, this method returns null.
   *
   * @param value   The given value.
   * @param klass   The target class to cast to.
   * @param <E>     The type of the object we want to cast to.
   *
   * @return The cast object.
   */
  public static <E> E castOrThrow(Object value, Class<E> klass)
  {
    try
    {
      return value == null ? null : klass.cast(value);
    }
    catch (ClassCastException e)
    {
      throw new TemplateOutputCastException("Cannot coerce " + value + " to desired class " + klass, e);
    }
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
    Class<?> classArg = DataMap.class;
    try
    {
      if (RecordTemplate.class.isAssignableFrom(templateClass) ||
          AbstractMapTemplate.class.isAssignableFrom(templateClass))
      {
        return templateClass.getConstructor(DataMap.class);
      }
      else if (AbstractArrayTemplate.class.isAssignableFrom(templateClass))
      {
        classArg = DataList.class;
        return templateClass.getConstructor(DataList.class);
      }
      else if (FixedTemplate.class.isAssignableFrom(templateClass) ||
               UnionTemplate.class.isAssignableFrom(templateClass))
      {
        classArg = Object.class;
        return templateClass.getConstructor(Object.class);
      }
      else
      {
        throw new TemplateOutputCastException("Could not get constructor: " + templateClass.getName() + " does not match any DataTemplate classes");
      }
    }
    catch (SecurityException e)
    {
      throw new TemplateOutputCastException("getConstructor failed for class " + templateClass.getName() + " with argument " + classArg.getName() + ": the security manager denies access to the constructor, or the caller's class loader differs from the class loader for the current class and the security manager denies access to the package of this class.", e);
    }
    catch (NoSuchMethodException e)
    {
      throw new TemplateOutputCastException("getConstructor failed for class " + templateClass.getName() + " with argument " + classArg.getName() + ": no matching method was found.", e);
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
      throw new TemplateOutputCastException("Could not create new instance of " + wrapperClass.getName() + " with argument " + object, e);
    }
    catch (InstantiationException e)
    {
      throw new TemplateOutputCastException("Could not create new instance of " + wrapperClass.getName() + ": cannot initialize an abstract class", e);
    }
    catch (IllegalAccessException e)
    {
      throw new TemplateOutputCastException("Could not create new instance of " + wrapperClass.getName() + ": access control denies access to constructor", e);
    }
    catch (InvocationTargetException e)
    {
      throw new TemplateOutputCastException("Could not create new instance of " + wrapperClass.getName() + ": constructor throws an exception", e);
    }
    catch (ClassCastException e)
    {
      throw new TemplateOutputCastException("Could not create new instance of " + wrapperClass.getName() + " with argument " + object, e);
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
    Class<?> classArg = DataMap.class;
    try
    {
      switch (schema.getDereferencedType())
      {
        case MAP:
        case RECORD:
          return templateClass.getConstructor(DataMap.class);
        case ARRAY:
          classArg = DataList.class;
          return templateClass.getConstructor(DataList.class);
        case FIXED:
        case UNION:
          classArg = Object.class;
          return templateClass.getConstructor(Object.class);
        default:
          throw new TemplateOutputCastException("Could not get constructor for schema: " + schema.getDereferencedType().name() + " does not match any DataTemplate classes");
      }
    }
    catch (SecurityException e)
    {
      throw new TemplateOutputCastException("getConstructor failed for class " + templateClass.getName() + " with argument " + classArg.getName() + ": the security manager denies access to the constructor, or the caller's class loader differs from the class loader for the current class and the security manager denies access to the package of this class.", e);
    }
    catch (NoSuchMethodException e)
    {
      throw new TemplateOutputCastException("getConstructor failed for class " + templateClass.getName() + " with argument " + classArg.getName() + ": no matching method was found.", e);
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
      throw new TemplateOutputCastException("Could not create new instance of " + wrapperClass.getName() + " with argument " + object, e);
    }
    catch (InstantiationException e)
    {
      throw new TemplateOutputCastException("Could not create new instance of " + wrapperClass.getName() + ": cannot initialize an abstract class", e);
    }
    catch (IllegalAccessException e)
    {
      throw new TemplateOutputCastException("Could not create new instance of " + wrapperClass.getName() + ": access control denies access to constructor", e);
    }
    catch (InvocationTargetException e)
    {
      throw new TemplateOutputCastException("Could not create new instance of " + wrapperClass.getName() + ": constructor throws an exception", e);
    }
    catch (ClassCastException e)
    {
      throw new TemplateOutputCastException("Could not create new instance of " + wrapperClass.getName() + " with argument " + object, e);
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
      throw new TemplateOutputCastException("Could not create new instance of " + constructor.getClass().getName() + " using constructor " + constructor.getName() + " with argument " + object, e);
    }
    catch (InstantiationException e)
    {
      throw new TemplateOutputCastException("Could not create new instance of " + constructor.getClass().getName() + " using constructor " + constructor.getName() + ": cannot initialize an abstract class", e);
    }
    catch (IllegalAccessException e)
    {
      throw new TemplateOutputCastException("Could not create new instance of " + constructor.getClass().getName() + " using constructor " + constructor.getName() + ": access control denies access to constructor", e);
    }
    catch (InvocationTargetException e)
    {
      throw new TemplateOutputCastException("Could not create new instance of " + constructor.getClass().getName() + " using constructor " + constructor.getName() + ": constructor throws an exception", e);
    }
    catch (ClassCastException e)
    {
      throw new TemplateOutputCastException("Could not create new instance of " + constructor.getClass().getName() + " using constructor " + constructor.getName() + " with argument " + object, e);
    }
  }

  /**
   * Parse data schema in JSON format to obtain a {@link DataSchema}.
   *
   * TODO: deprecate this later, since current use cases still use this in generated data templates.
   *
   * @param schemaText provides the data schema in JSON format.
   * @return the {@link DataSchema} parsed from the data schema in JSON format.
   * @throws IllegalArgumentException if the data schema in JSON format is invalid or
   *                                  there is more than one top level schema.
   */
  public static DataSchema parseSchema(String schemaText) throws IllegalArgumentException
  {
    return parseSchema(schemaText, null, SchemaFormatType.PDSC);
  }

  /**
   * Parse data schema in JSON format to obtain a {@link DataSchema}.
   *
   * @param schemaText provides the data schema in JSON format.
   * @param schemaResolver for resolving referenced schemas
   * @return the {@link DataSchema} parsed from the data schema in JSON format.
   * @throws IllegalArgumentException if the data schema in JSON format is invalid or
   *                                  there is more than one top level schema.
   * @deprecated This method assumes the data schema is encoded in {@link SchemaFormatType#PDSC},
   *             use {@link #parseSchema(String, DataSchemaResolver, SchemaFormatType)} instead.
   */
  @Deprecated
  public static DataSchema parseSchema(String schemaText, DataSchemaResolver schemaResolver) throws IllegalArgumentException
  {
    return parseSchema(schemaText, schemaResolver, SchemaFormatType.PDSC);
  }

  /**
   * Parse data schema encoded in any format to obtain a {@link DataSchema}.
   *
   * @param schemaText the encoded data schema.
   * @param schemaFormatType the format in which the schema is encoded.
   * @return the {@link DataSchema} parsed from the encoded data schema.
   * @throws IllegalArgumentException if the encoded data schema is invalid or there is more than one top-level schema.
   */
  public static DataSchema parseSchema(String schemaText, SchemaFormatType schemaFormatType) throws IllegalArgumentException
  {
    return parseSchema(schemaText, null, schemaFormatType);
  }

  /**
   * Parse data schema encoded in any format to obtain a {@link DataSchema}.
   *
   * @param schemaText the encoded data schema.
   * @param schemaFormatType the format in which the schema is encoded.
   * @param schemaResolver resolver for resolving referenced schemas.
   * @return the {@link DataSchema} parsed from the encoded data schema.
   * @throws IllegalArgumentException if the encoded data schema is invalid or there is more than one top-level schema.
   */
  public static DataSchema parseSchema(String schemaText, DataSchemaResolver schemaResolver,
      SchemaFormatType schemaFormatType) throws IllegalArgumentException
  {
    final PegasusSchemaParser parser = schemaFormatType.getSchemaParserFactory().create(schemaResolver);

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
      throw new IllegalArgumentException("More than one top level schema");
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
  @SuppressWarnings({"rawtypes"})
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
   * @return The class for the raw in-memory representation for objects of the given schema.
   */
  public static Class<?> getDataClass(DataSchema schema)
  {
    DataSchema.Type type = Optional.ofNullable(schema.getDereferencedType()).orElse(DataSchema.Type.NULL);
    switch (type)
    {
      case ENUM:
      case STRING:
        return String.class;
      case MAP:
      case UNION:
      case RECORD:
        return DataMap.class;
      case ARRAY:
        return DataList.class;
      case BYTES:
      case FIXED:
        return ByteString.class;
      case INT:
        return Integer.class;
      case LONG:
        return Long.class;
      case FLOAT:
        return Float.class;
      case DOUBLE:
        return Double.class;
      case BOOLEAN:
        return Boolean.class;
      default:
        return Object.class;
    }
  }

  /**
   * Gets the data schema for a given java type. We will first get cached data schema for the given type if it has already
   * been accessed before, otherwise we will use reflection to retrieve its data schema and cache it for later use.
   *
   * @param type to get a schema for. Has to be primitive or a generated data template.
   * @throws TemplateRuntimeException if the {@link DataSchema} for the specified type cannot be provided.
   */
  public static DataSchema getSchema(Class<?> type) throws TemplateRuntimeException
  {
    // primitive type has already been cached in a static map
    final DataSchema primitiveSchema = DataSchemaUtil.classToPrimitiveDataSchema(type);
    if (primitiveSchema != null)
    {
      return primitiveSchema;
    }

    // complex type
    // NOTE: due to a non-optimized implementation of ConcurrentHashMap.computeIfAbsent in Java 8
    // (https://bugs.openjdk.java.net/browse/JDK-8161372), we are doing a pre-screen here before calling
    // computeIfAbsent to avoid pessimistic locking in case of key present. This tradeoff
    // (http://cs.oswego.edu/pipermail/concurrency-interest/2014-December/013360.html) can be removed
    // when we upgrade to Java 9 when this concurrent issue is improved.
    DataSchema typeSchema = _classToSchemaMap.get(type);
    return (typeSchema != null) ? typeSchema : _classToSchemaMap.computeIfAbsent(type, key ->
    {
      try
      {
        Field schemaField = type.getDeclaredField(SCHEMA_FIELD_NAME);
        schemaField.setAccessible(true);
        DataSchema schema = (DataSchema) schemaField.get(null);
        if (schema == null)
        {
          throw new TemplateRuntimeException("Schema field is not set in class: " + type.getName());
        }

        return schema;
      }
      catch (IllegalAccessException | NoSuchFieldException e)
      {
        throw new TemplateRuntimeException("Error accessing schema field in class: " + type.getName(), e);
      }
    });
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
      throw new TemplateRuntimeException("Schema is unnamed in class: " + type.getName());
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
      throw new ClassCastException("Input " + object + " has type " + object.getClass().getName() + " but expected type is " + _targetClass.getName());
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
      if (object.getClass() == _targetClass)
      {
        return object;
      }
      else if (object instanceof Number)
      {
        return coerce(object);
      }
      else
      {
        throw new ClassCastException("Input " + object + " has type " + object.getClass().getName() + ", but expected type is " + Number.class.getName());
      }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T coerceOutput(Object object) throws TemplateOutputCastException
    {
      if (object.getClass() == _targetClass)
      {
        return (T) object;
      }
      else if (object instanceof Number)
      {
        return coerce(object);
      }
      else if (object instanceof String && isStringAllowed())
      {
        return coerceString((String) object);
      }
      else
      {
        throw new TemplateOutputCastException("Output " + object + " has type " + object.getClass().getName() + ", but expected type is " + _targetClass.getName());
      }
    }

    /**
     * Indicates if this coercer accepts String values.
     * @return false, as default value.
     */
    protected boolean isStringAllowed()
    {
      return false;
    }

    /**
     * Checks if the input string is non-numeric string : NaN, Infinity or -Infinity.
     * @param object, input string.
     * @return true, if the input string is non-numeric string.
     */
    protected Boolean isNonNumericFloat(String object)
    {
      return object.equals(String.valueOf(Float.NaN)) || object.equals(String.valueOf(Float.POSITIVE_INFINITY)) || object.equals(String.valueOf(Float.NEGATIVE_INFINITY));
    }

    protected TemplateOutputCastException generateExceptionForInvalidString(String object)
    {
      return new TemplateOutputCastException("Cannot coerce String value : " + object +  " to type : " + _targetClass.getName());
    }

    protected T coerceString(String object)
    {
      throw new UnsupportedOperationException("Only supported for floating-point number coercers");
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

      throw new TemplateOutputCastException("Output " + object + " has type " + object.getClass().getName() + ", but expected type is " + Boolean.class.getName());
    }
  }

  /**
   * Pass through coercer for strings.
   *
   * This is used by _classToCoercerMap to add support for dynamically setting enums using their string symbol.
   */
  private static class StringCoercer extends NativeCoercer<String>
  {
    StringCoercer()
    {
      super(String.class);
    }

    @Override
    public Object coerceInput(String object) throws ClassCastException
    {
      return object;
    }

    @Override
    public String coerceOutput(Object object) throws TemplateOutputCastException
    {
      if (object instanceof String)
      {
        return (String)object;
      }
      else
      {
        throw new TemplateOutputCastException("Output " + object + " has type " + object.getClass().getName() + ", but expected type is " + String.class.getName());
      }
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
      if (object instanceof ByteString)
      {
        return (ByteString) object;
      }
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
        throw new TemplateOutputCastException("Output " + object + " has type " + object.getClass().getName() + ", but expected type is " + String.class.getName());
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

    @Override
    protected Float coerceString(String object)
    {
      if(isNonNumericFloat(object))
      {
        return Float.valueOf(object);
      }
      else
      {
        throw generateExceptionForInvalidString(object);
      }
    }

    @Override
    protected boolean isStringAllowed()
    {
      return true;
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

    @Override
    protected Double coerceString(String object)
    {
      if(isNonNumericFloat(object))
      {
        return Double.valueOf(object);
      }
      else
      {
        throw generateExceptionForInvalidString(object);
      }
    }

    @Override
    protected boolean isStringAllowed()
    {
      return true;
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
  private static final StringCoercer STRING_COERCER = new StringCoercer();

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

    /* This coercer provides a safe way to set a enum value on a record template using a plain string.  This is
     * required in cases where The concrete enum class is unavailable, such as when dynamically generating requests
     * as runtime for arbitrary resource schemas like is done by our example generator.
     *
     * The usual approach of setting an enum is to do this (usually indirectly via RecordTemplate.putDirect):
     *
     * DataTemplateUtil.coerceInput(SomeEnumClass.ENUM_SYMBOL, SomeEnumClass.class, String.class)
     *
     * But making this call requires that SomeEnumClass be known (and in the classpath), which is not always the case.
     *
     * So for such cases, we offer an alternative way to set an enum:
     *
     * DataTemplateUtil.coerceInput("ENUM_SYMBOL", Enum.class, String.class)
     *
     * Where Enum.class is a marker.  It makes the intent clear here and prevents this coercer from being used except
     * when intended.
     *
     * Because enums are always coerced to strings, the implementation is just passthrough.  The important part of this
     * coercer is that it is keyed in _classToCoercerMap by Enum.class.
     */
    map.put(Enum.class, STRING_COERCER);
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
        if (existingCoercer == null || !existingCoercer.getClass().getName().equals(coercer.getClass().getName()))
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

    return coerceCustomInput(object, fromClass);
  }

  public static Object coerceIntInput(Integer value)
  {
    return value == null ? null : INTEGER_COERCER.coerceInput(value);
  }

  public static Object coerceLongInput(Long value)
  {
    return value == null ? null : LONG_COERCER.coerceInput(value);
  }

  public static Object coerceFloatInput(Float value)
  {
    return value == null ? null : FLOAT_COERCER.coerceInput(value);
  }

  public static Object coerceDoubleInput(Double value)
  {
    return value == null ? null : DOUBLE_COERCER.coerceInput(value);
  }

  public static <C> Object coerceCustomInput(C value, Class<C> customClass)
  {
    if (value == null)
    {
      return null;
    }

    @SuppressWarnings("unchecked") DirectCoercer<C> coercer = (DirectCoercer<C>) _classToCoercerMap.get(customClass);
    if (coercer == null)
    {
      throw new ClassCastException("Input " + value + " has type " + value.getClass().getName() + ", but does not have a registered coercer");

    }
    else
    {
      return coercer.coerceInput(value);
    }
  }

  public static String stringify(Object object)
  {
    return stringify(object, object.getClass());
  }

  /**
   * Convert an input value to its string representation. Valid input type includes
   * <ul>
   *   <li>Java primitive types and their boxed versions</li>
   *   <li>Java Enums</li>
   *   <li>Custom classes that have their coercers registered</li>
   * </ul>
   *
   * @param object provides the input value to be coerced.
   * @param fromClass class to be coerced from
   * @return string representation of the input object.
   */
  public static String stringify(Object object, Class<?> fromClass)
  {
    if (fromClass == null)
    {
      fromClass = object.getClass();
    }
    if (object instanceof ByteString)
    {
      return ((ByteString) object).asAvroString();
    }
    if (DataTemplateUtil.hasCoercer(fromClass))
    {
      @SuppressWarnings("unchecked")
      final Class<Object> clazz = (Class<Object>) fromClass;
      Object coerced = DataTemplateUtil.coerceInput(object, clazz, Object.class);
      if (coerced instanceof ByteString)
      {
        return ((ByteString) coerced).asAvroString();
      }
      else
      {
        return coerced.toString();
      }
    }
    return object.toString();
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
        throw new TemplateOutputCastException("Enum " + targetClass.getName() + " does not have a member with the name " + value, exc);
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
      throw new TemplateOutputCastException("Output " + object + " has type " + object.getClass().getName() + ", and cannot be coerced to enum type " + targetClass.getName());
    }

    return coerceCustomOutput(object, targetClass);
  }

  public static Integer coerceIntOutput(Object value)
  {
    return value == null ? null : INTEGER_COERCER.coerceOutput(value);
  }

  public static Long coerceLongOutput(Object value)
  {
    return value == null ? null : LONG_COERCER.coerceOutput(value);
  }

  public static Float coerceFloatOutput(Object value)
  {
    return value == null ? null : FLOAT_COERCER.coerceOutput(value);
  }

  public static Double coerceDoubleOutput(Object value)
  {
    return value == null ? null : DOUBLE_COERCER.coerceOutput(value);
  }

  public static ByteString coerceBytesOutput(Object value)
  {
    return value == null ? null : BYTES_COERCER.coerceOutput(value);
  }

  public static Boolean coerceBooleanOutput(Object value)
  {
    return value == null ? null : BOOLEAN_COERCER.coerceOutput(value);
  }

  public static String coerceStringOutput(Object value)
  {
    return value == null ? null : STRING_COERCER.coerceOutput(value);
  }

  public static <E extends Enum<E>> E coerceEnumOutput(Object value, Class<E> targetClass, E fallback)
  {
    if (value == null)
    {
      return null;
    }

    if (value instanceof String)
    {
      try
      {
        return Enum.valueOf(targetClass, (String) value);
      }
      catch (IllegalArgumentException e)
      {
        return fallback;
      }
    }
    throw new TemplateOutputCastException("Output " + value + " has type " + value.getClass().getName() + ", and cannot be coerced to enum type " + targetClass.getName());
  }

  @SuppressWarnings("unchecked")
  public static <C> C coerceCustomOutput(Object value, Class<C> customClass)
  {
    if (value == null)
    {
      return null;
    }

    DirectCoercer<?> coercer = _classToCoercerMap.get(customClass);
    if (coercer == null)
    {
      throw new TemplateOutputCastException("Output " + value + " has type " + value.getClass().getName() +
          ", but does not have a registered coercer and cannot be coerced to type " + customClass.getName());
    }
    else
    {
      return (C) coercer.coerceOutput(value);
    }
  }

  /**
   * Convert a {@link DataList} to the array of desired target class.
   *
   * This conversion is recursive. For example, a {@link DataList} containing collection of
   * {@link DataList}s can be converted to int[][].
   *
   * @param list {@link DataList} that provides the value to be converted
   * @param arrayItemType provides the item class of the result array.
   * @return the converted result array.
   * @throws TemplateOutputCastException if the specified object cannot be coerced to the
   *                                     specified target class.
   */
  public static Object convertDataListToArray(DataList list, Class<?> arrayItemType)
  {
    final Object result = Array.newInstance(arrayItemType, list.size());

    for (int i = 0; i < list.size(); ++i)
    {
      final Object valueItem = list.get(i);
      final Object arrayItem;
      if (arrayItemType.isArray())
      {
        final Class<?> componentType = arrayItemType.getComponentType();
        if (!(valueItem instanceof DataList))
        {
          throw new TemplateOutputCastException("Cannot coerce item " + valueItem + " with type " + valueItem.getClass().getName() + " to array of " + componentType.getName() + ": Expected type is " + DataList.class.getName());
        }
        arrayItem = convertDataListToArray((DataList) valueItem, componentType);
      }
      else if (DataTemplate.class.isAssignableFrom(arrayItemType))
      {
        arrayItem = wrap(valueItem, arrayItemType.asSubclass(DataTemplate.class));
      }
      else
      {
        arrayItem = coerceOutput(valueItem, arrayItemType);
      }
      Array.set(result, i, arrayItem);
    }

    return result;
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

  /**
   * Check if two data templates of the same type are semantically equal. We call two data templates semantically equal if all their
   * fields satisfy any of the following:
   * <ul>
   *   <li>Both objects have the field set to the same value
   *   <li>One object has the required field set to the default value and the other doesn't have the field set
   *   <li>They both don't have the field set
   * </ul>
   * We will first check if data stored in the two data templates are equal. If not equal, we will apply fix-up to their data
   * and compare equality of the fixed-up data. The fix-up will do the following massage to the original data:
   * <ul>
   *    <li>populate absent fields with their default values</li>
   *    <li>coerce numeric values and strings containing numeric values to the schema's numeric type</li>
   *    <li>keep any unrecognized fields not in the data template schema</li>
   * </ul>
   * @param data1 first data template.
   * @param data2 second data template
   * @return true if two data templates are semantically equal, false otherwise.
   */
  public static <T extends DataTemplate<?>> boolean areEqual(T data1, T data2)
  {
    return areEqual(data1, data2, false);
  }

  /**
   * Check if two data templates of the same type are semantically equal. We call two data templates semantically equal if all their
   * fields satisfy any of the following:
   * <ul>
   *   <li>Both objects have the field set to the same value
   *   <li>One object has the required field set to the default value and the other doesn't have the field set
   *   <li>They both don't have the field set
   * </ul>
   * We will first check if data stored in the two data templates are equal. If not equal, we will apply fix-up to their data
   * and compare equality of the fixed-up data. The fix-up will do the following massage to the original data:
   * <ul>
   *    <li>populate absent fields with their default values</li>
   *    <li>coerce numeric values and strings containing numeric values to data template schema's numeric type</li>
   *    <li>ignore any unrecognized fields not in the data template schema if ignoreUnrecognizedField flag is true, otherwise we will
   * keep those unrecognized fields in comparison.</li>
   * </ul>
   * @param data1 first data template.
   * @param data2 second data template
   * @param ignoreUnrecognizedField if this flag is set to true, we don't include unrecognized fields into data template comparison.
   *                                Otherwise, we will compare those fields as well.
   * @return true if two data templates are semantically equal, false otherwise.
   */
  public static <T extends DataTemplate<?>> boolean areEqual(T data1, T data2, boolean ignoreUnrecognizedField)
  {
    // default fix-up option
    ValidationOptions validationOption = new ValidationOptions(RequiredMode.FIXUP_ABSENT_WITH_DEFAULT,
            CoercionMode.NORMAL, ignoreUnrecognizedField ? UnrecognizedFieldMode.TRIM : UnrecognizedFieldMode.IGNORE);
    return areEqual(data1, data2, validationOption);
  }

  @SuppressWarnings("unchecked")
  private static <T extends DataTemplate<?>> boolean areEqual(T data1, T data2, ValidationOptions validationOption)
  {
    if (data1 == null || data2 == null)
    {
      return data1 == data2;
    }
    // return true if data1 and data2 are already equal
    if (data1.equals(data2))
    {
      return true;
    }
    // try to fix up two data based on given data schema
    // since fix-up will modify the original data, we need to make a copy before performing fix-up to avoid such side-effects.
    try {
      T data1Copy = (T)data1.copy();
      T data2Copy = (T)data2.copy();
      ValidationResult validateResult1 = ValidateDataAgainstSchema.validate(data1Copy, validationOption);
      ValidationResult validateResult2 = ValidateDataAgainstSchema.validate(data2Copy, validationOption);
      if (validateResult1.hasFix() || validateResult2.hasFix())
      {
        return data1Copy.equals(data2Copy);
      }
      else
      {
        // no fix-up is done
        return false;
      }
    }
    catch (CloneNotSupportedException e)
    {
      return false;
    }
  }

}
