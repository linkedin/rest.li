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

package com.linkedin.data.schema.validator;


import com.linkedin.data.DataMap;
import com.linkedin.data.element.DataElement;
import com.linkedin.data.message.Message;
import com.linkedin.data.message.MessageList;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaConstants;
import com.linkedin.data.schema.DataSchemaTraverse;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;


/**
 * Validator that will look for the "validate" property with a {@link DataSchema} and invoke
 * the {@link Validator}s specified by this property.
 * <p>
 *
 * For example, the following is schema that declares a "validate" property.
 * <p>
 *
 * <code>
 *   {
 *     "type" : "typeref",
 *     "name" : "Digits",
 *     "ref"  : "string",
 *     "validate" : {
 *       "regex" : {
 *         "regex" : "[0-9]*"
 *       }
 *     }
 *   }
 * </code>
 * <p>
 *
 * The value of this property must be a {@link DataMap}. Each entry in this {@link DataMap}
 * declares a {@link Validator} that has to be created. The key of the entry determines
 * {@link Validator} subclass to instantiate.
 *  <p>
 *
 * The key to {@link Validator} class lookup algorithm first looks up the key-to-class
 * map provided to the constructor to obtain the {@link Validator} subclass. If the key
 * does not exist in the map, then look for a class whose name is equal to the
 * provided key and is a subclass of {@link Validator}. If there is no match,
 * then look for a class whose fully qualified name is derived from the provided by key
 * by using {@code com.linkedin.data.schema.validator} as the Java package name and capitalizing
 * the first character of the key and appending "Validator" to the key as the name
 * of the class, and the class is a subclass of {@link Validator}.
 * <p>
 *
 * Once the {@link Validator} subclass is found, its one-argument constructor that
 * takes a single {@link DataMap} parameter is invoked with value of the entry.
 * <p>
 *
 * In the above example, this class will lookup "regex" in the key-to-class map,
 * find {@link RegexValidator} class, and invoke the {@link RegexValidator#RegexValidator(com.linkedin.data.DataMap)}
 * constructor with the following {@link DataMap}.
 * <p>
 *
 * <code>
 *   {
 *     "regex" : "[0-9]*"
 *   }
 * </code>
 * <p>
 *
 * This will construct a {@link RegexValidator} that will validate that values of
 * this type will contain only numeric digits.
 * <p>
 *
 * A {@link Validator} instantiated by this class is likely to be called concurrently from
 * multiple threads. The same instance will be called to validate different values.
 * For deterministic behavior an instance should not maintain state across multiple
 * invocations.
 * <p>
 *
 * Once initialized, a {@link DataSchemaAnnotationValidator} does not maintain state
 * across calls to its {@link #validate(ValidatorContext)} method. Hence, a single
 * instance may be called concurrently by multiple threads. Hence, the more
 * expensive tasks of iterating through a {@link DataSchema} and
 * constructing {@link Validator} instance only needs to occur once.
 * <p>
 *
 * Initialization is successful if all keys of "validate" properties can be successfully
 * resolved to a {@link Validator} class and an instance of the class can be successfully
 * constructed using the value associated with the key. The {@link #getInitMessages()}
 * method can be used to obtain a more detailed message on the "validate" properties
 * and associated keys that could not be initialized successfully.
 * <p>
 *
 * A partially successful initialized {@link DataSchemaAnnotationValidator} may still
 * be useful. It is possible that new keys added to the "validate" property may not be known
 * to other clients of schema.
 * <p>
 *
 * <b>Validator Execution Order</b>
 * <p>
 *
 * Execution ordering of multiple validators specified within the same "validate"
 * property is determined by the "validatorPriority" property of each validator.
 *
 * <code>
 *   "validate" : {
 *     "higherPriorityValidator" : {
 *       "validatorPriority" : 1
 *     },
 *     "defaultPriorityValidator" : {
 *     },
 *     "lowerPriorityValidator" : {
 *       "validatorPriority" : -1
 *     }
 *   }
 * </code>
 * <p>
 *
 * The higher the priority value, the higher the priority of the validator, i.e.
 * a validator with higher prority value will be executed before the validators
 * with lower priority values. The default priority value for a validator that
 * does not specify a priority value is 0. Execution order of validators with
 * the same priority value is not defined or specified.
 * <p>
 *
 * Validators may be attached to a field as well as the type of the field.
 * This class will always execute the validators associated to the type of the field
 * before it will execute the validators associated with the field.
 * <p>
 *
 * If schema of a data element is a typeref, then the validator associated with
 * the typeref is executed before the validator of the referenced type.
 * <p>
 *
 * Beyond the above execution ordering guarantees provided by this class,
 * the execution order of validators among different data elements is determined
 * by the traversal order of the caller (i.e. how data elements passed to the
 * {@link #validate(ValidatorContext)} method of this class. Typically, the caller will be
 * {@link com.linkedin.data.schema.validation.ValidateDataAgainstSchema}
 * and this caller performs a post-order traversal of data elements.
 */
public class DataSchemaAnnotationValidator implements Validator
{
  public static final String VALIDATE = "validate";
  public static final String VALIDATOR_PRIORITY = "validatorPriority";

  public static final int DEFAULT_VALIDATOR_PRIORITY = 0;

  private static final String MY_PACKAGE_NAME = DataSchemaAnnotationValidator.class.getPackage().getName();

  private static class ValidatorInfo
  {
    private final int _priority;
    private final Validator _validator;

    private ValidatorInfo(Integer priority, Validator validator)
    {
      _priority = priority == null ? DEFAULT_VALIDATOR_PRIORITY : priority;
      _validator = validator;
    }
  }

  private static final Comparator<ValidatorInfo> PRIORITY_COMPARATOR = new Comparator<ValidatorInfo>()
  {
    @Override
    public int compare(ValidatorInfo v1, ValidatorInfo v2)
    {
      return v2._priority - v1._priority;
    }
  };

  private boolean _debugMode = false;
  private DataSchema _schema = DataSchemaConstants.NULL_DATA_SCHEMA;
  private Map<String, Class<? extends Validator>> _classMap = Collections.emptyMap();
  private Map<Object, List<Validator>> _cache = Collections.emptyMap();
  private MessageList _initMessages = new MessageList();

  private static final List<Validator> NO_VALIDATORS = Collections.emptyList();

  /**
   * Default constructor.
   */
  public DataSchemaAnnotationValidator()
  {
  }

  /**
   * Constructor to initialize with the provided {@link DataSchema}.
   *
   * This is the same as calling the default constructor followed by calling
   * {@link #init(com.linkedin.data.schema.DataSchema)}.
   *
   * @param schema to be parsed to compute the {@link Validator}'s that have to be created.
   */
  public DataSchemaAnnotationValidator(DataSchema schema)
  {
    init(schema);
  }

  /**
   * Constructor to initialize with the provided {@link DataSchema} and key to {@link Validator} class map.
   *
   * This is the same as calling the default constructor followed by calling
   * {@link #init(com.linkedin.data.schema.DataSchema, java.util.Map)}.
   *
   * @param schema to be parsed to compute the {@link Validator}'s that have to be created.
   * @param classMap provides the key to {@link Validator} class map that is looked up first
   *                 before looking for classes using names derived from the key.
   */
  public DataSchemaAnnotationValidator(DataSchema schema, Map<String, Class<? extends Validator>> classMap)
  {
    init(schema, classMap);
  }

  /**
   * Initialize the {@link DataSchemaAnnotationValidator} with the provided {@link DataSchema}.
   *
   * This method causes the "validate" properties of the schema and referenced schemas to
   * be parsed and instances of {@link Validator}'s to be constructed.
   *
   * @param schema to be parsed to compute the {@link Validator}'s that have to be created.
   * @return true if initialization is successful, else return false, additional information
   *              may be obtained using {@link #getInitMessages()}.
   */
  public boolean init(DataSchema schema)
  {
    return init(schema, Collections.<String, Class<? extends Validator>>emptyMap());
  }

  /**
   * Initialize the {@link DataSchemaAnnotationValidator} with the provided
   * {@link DataSchema} and key to {@link Validator} class map.
   *
   * This method causes the "validate" properties of the schema and referenced schemas to
   * be parsed and appropriate instances of {@link Validator}'s to be constructed.
   *
   * @param schema to be parsed to compute the {@link Validator}'s that have to be created.
   * @param classMap provides the map of the keys to {@link Validator}s that is looked up first
   *                 before looking for classes with names derived from the key.
   * @return true if initialization is successful, else return false, additional information
   *              may be obtained using {@link #getInitMessages()}.
   */
  public boolean init(DataSchema schema, Map<String, Class<? extends Validator>> classMap)
  {
    _initMessages.clear();
    _schema = schema;
    _classMap = classMap;
    _cache = cacheValidators(_schema);
    return isInitOk();
  }

  /**
   * Return whether initialization is successful.
   *
   * @return true if initialization is successful.
   */
  public boolean isInitOk()
  {
    return _initMessages.isError() == false;
  }

  /**
   * Return {@link Message}s providing more detailed information regarding initialization.
   *
   * @return {@link Message}s providing more detailed information regarding initialization.
   */
  public Collection<Message> getInitMessages()
  {
    return _initMessages;
  }

  /**
   * Set the debug mode.
   *
   * The debug mode will cause the {@link #validate(ValidatorContext)} method
   * to emit non-error diagnostic messages and add them to {@link ValidatorContext}.
   *
   * @param debugMode specifies the value of the debug mode.
   */
  public void setDebugMode(boolean debugMode)
  {
    _debugMode = debugMode;
  }

  /**
   * Get the debug mode.
   *
   * @return the debug mode.
   */
  public boolean isDebugMode()
  {
    return _debugMode;
  }

  /**
   * Build a cache of {@link Validator}s declared for the specified schema.
   *
   * @param schema to cache {@link Validator}s for.
   * @return the cache if successful.
   */
  private IdentityHashMap<Object, List<Validator>> cacheValidators(DataSchema schema)
  {
    final IdentityHashMap<Object, List<Validator>> map = new IdentityHashMap<Object, List<Validator>>();

    DataSchemaTraverse traverse = new DataSchemaTraverse();
    traverse.traverse(schema, new DataSchemaTraverse.Callback()
    {
      @Override
      public void callback(List<String> path, DataSchema schema)
      {
        List<Validator> validatorList = map.get(schema);
        if (validatorList == null)
        {
          Object validateObject = schema.getProperties().get(VALIDATE);
          if (validateObject == null)
          {
            validatorList = NO_VALIDATORS;
          }
          else
          {
            validatorList = buildValidatorList(validateObject, path, schema);
          }
          map.put(schema, validatorList);
          if (schema.getType() == DataSchema.Type.RECORD)
          {
            RecordDataSchema recordDataSchema = (RecordDataSchema) schema;
            for (RecordDataSchema.Field field : recordDataSchema.getFields())
            {
              validateObject = field.getProperties().get(VALIDATE);
              if (validateObject == null)
              {
                validatorList = NO_VALIDATORS;
              }
              else
              {
                path.add(field.getName());
                validatorList = buildValidatorList(validateObject, path, field);
                path.remove(path.size() - 1);
              }
              map.put(field, validatorList);
            }
          }
        }
      }
    });
    return map;
  }

  /**
   * Build list of {@link Validator} instances for a given "validate" property.
   *
   * The value of the "validate" property should be a {@link DataMap}.
   *
   * @param validateObject the value of the "validate" property.
   * @param path to the schema.
   * @param source is the source that contains the "validate" property. The source is usually the
   *               {@link DataSchema} that contains the "validate" property except when a field
   *               contains the "validate" property, in this case the source is a
   *               {@link RecordDataSchema.Field}.
   * @return the list of {@link Validator} instances constructed for the "validate" property.
   */
  private List<Validator> buildValidatorList(Object validateObject, List<String> path, Object source)
  {
    List<Validator> validatorList;
    if (validateObject.getClass() != DataMap.class)
    {
      addMessage(path, "\"validate\" property of %1$s is not a DataMap\n", source);
      validatorList = NO_VALIDATORS;
    }
    else
    {
      DataMap validateMap = (DataMap) validateObject;
      List<ValidatorInfo> validatorInfoList = new ArrayList<ValidatorInfo>(validateMap.size());
      for (Map.Entry<String, Object> entry : validateMap.entrySet())
      {
        Object config = entry.getValue();
        String key = entry.getKey();
        Class<? extends Validator> clazz = locateValidatorClass(key, path, source);
        if (clazz == null)
        {
          addMessage(path, "\"validate\" property of %1$s, unable to find Validator for \"%2$s\"\n", source, key);
          continue;
        }
        if (config.getClass() != DataMap.class)
        {
          addMessage(path, "\"validate\" property of %1$s, value of \"%2$s\" is not a DataMap\n", source, key);
          continue;
        }
        try
        {
          Constructor<? extends Validator> ctor = clazz.getConstructor(DataMap.class);
          DataMap configDataMap = (DataMap) config;
          Integer priority = configDataMap.getInteger(VALIDATOR_PRIORITY);
          Validator validator = ctor.newInstance(configDataMap);
          validatorInfoList.add(new ValidatorInfo(priority, validator));
        }
        catch (Exception e)
        {
          addMessage(path,
                     "\"validate\" property of %1$s, %2$s cannot be instantiated for \"%3$s\", %4$s\n",
                     source,
                     clazz.getName(),
                     key,
                     e);
        }
      }
      Collections.sort(validatorInfoList, PRIORITY_COMPARATOR);
      validatorList = new ArrayList<Validator>(validatorInfoList.size());
      for (ValidatorInfo validatorInfo : validatorInfoList)
      {
        validatorList.add(validatorInfo._validator);
      }
    }
    assert(validatorList != null);
    return validatorList;
  }

  /**
   * Locate the {@link Validator} class for the specified key.
   *
   * @param key provides the key that identifies a {@link Validator} class.
   * @param path to the schema.
   * @param source is the source that contains the "validate" property. The source is usually the
   *               {@link DataSchema} that contains the "validate" property except when a field
   *               contains the "validate" property, in this case the source is a
   *               {@link com.linkedin.data.schema.RecordDataSchema.Field}.
   * @return the {@link Validator} class identified by the key, if no class can be located for
   *         the specified key, return null.
   */
  protected Class<? extends Validator> locateValidatorClass(String key, List<String> path, Object source)
  {
    Class<? extends Validator> clazz = _classMap.get(key);
    if (clazz == null)
    {
      Iterator<String> it = validatorClassNamesForKey(key);
      while (it.hasNext())
      {
        String className = it.next();
        try
        {
          Class<?> classFromName = Class.forName(className);
          if (Validator.class.isAssignableFrom(classFromName))
          {
            @SuppressWarnings("unchecked")
            Class<? extends Validator> validatorClass = (Class<? extends Validator>) classFromName;
            clazz = validatorClass;
            break;
          }
          else
          {
            addMessage(path,
                       (className.equals(key) ? true : false),
                       "\"validate\" property of %1$s, %2$s is not a %3$s",
                       source,
                       classFromName.getName(),
                       Validator.class.getName());
          }
        }
        catch (ClassNotFoundException e)
        {
        }
      }
    }
    return clazz;
  }

  /**
   * Return the class names to check for existence of a {@link Validator} class.
   *
   * <p>
   * The first class name is the key.
   * The second class name is a fully qualified name whose Java package name is
   * "com.linkedin.data.validator" and the name is computed by capitalizing
   * the first character of the key and appending "Validator".
   * <p>
   * Subclasses may override this method to provide different algorithms for
   * deriving the class names for the key.
   *
   * @param key provides the key that identifies a {@link Validator} class.
   * @return {@link Iterator} that provide the potential class names for the specified key.
   */
  protected Iterator<String> validatorClassNamesForKey(final String key)
  {
    return new Iterator<String>()
    {
      private int index = 0;

      @Override
      public boolean hasNext()
      {
        return index < 2;
      }
      @Override
      public String next()
      {
        String name;
        switch (index)
        {
          case 0:
            name = key;
            break;
          case 1:
            StringBuilder sb = new StringBuilder();
            sb.append(MY_PACKAGE_NAME);
            sb.append('.');
            if (key.length() > 0)
            {
              sb.append(Character.toUpperCase(key.charAt(0)));
              sb.append(key, 1, key.length());
            }
            sb.append("Validator");
            name = sb.toString();
            break;
          default:
            throw new NoSuchElementException();
        }
        index++;
        return name;
      }
      @Override
      public void remove()
      {
        throw new UnsupportedOperationException();
      }
    };
  }

  private void addMessage(List<String> path, String format, Object... args)
  {
    _initMessages.add(new Message(path.toArray(), format, args));
  }

  private void addMessage(List<String> path, boolean error, String format, Object... args)
  {
    _initMessages.add(new Message(path.toArray(), error, format, args));
  }

  private void getAndInvokeValidatorList(ValidatorContext ctx, Object key)
  {
    List<Validator> validatorList = _cache.get(key);
    if (validatorList == null)
    {
      // this means schema or field to be validated has not been cached.
      ctx.addResult(new Message(ctx.dataElement().path(),
                                "validation skipped, %1$s have not been initialized for use by %2$s",
                                key, getClass().getSimpleName()));
    }
    else if (validatorList != NO_VALIDATORS)
    {
      for (Validator validator : validatorList)
      {
        if (_debugMode)
        {
          ctx.addResult(new Message(ctx.dataElement().path(), false, "validating with %1$s", validator));
        }
        validator.validate(ctx);
      }
    }
  }

  @Override
  public void validate(ValidatorContext context)
  {
    DataElement element = context.dataElement();
    DataSchema schema = element.getSchema();
    while (schema != null)
    {
      getAndInvokeValidatorList(context, schema);
      // check if the value belongs to a field in a record
      // if it belongs to a field, check if the field has
      // validators.
      schema = (schema.getType() == DataSchema.Type.TYPEREF) ? ((TyperefDataSchema) schema).getRef() : null;
    }
    DataElement parentElement = element.getParent();
    if (parentElement != null)
    {
      DataSchema parentSchema = parentElement.getSchema();
      if (parentSchema != null && parentSchema.getType() == DataSchema.Type.RECORD)
      {
        Object name = element.getName();
        if (name.getClass() == String.class)
        {
          RecordDataSchema recordDataSchema = (RecordDataSchema) parentSchema;
          RecordDataSchema.Field field = recordDataSchema.getField((String) name);
          if (field != null)
          {
            getAndInvokeValidatorList(context, field);
          }
        }
      }
    }
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append("\n");
    if (_initMessages.isEmpty() == false)
    {
      sb.append("Initialization message:\n");
      _initMessages.appendTo(sb);
    }
    sb.append("Validators:\n");
    for (Map.Entry<Object, List<Validator>> e : _cache.entrySet())
    {
      sb.append("  ");
      Object key = e.getKey();
      if (key instanceof RecordDataSchema.Field)
      {
        sb.append(((RecordDataSchema.Field) key).getName()).append(" (field)");
      }
      else if (key instanceof NamedDataSchema)
      {
        sb.append(((NamedDataSchema) key).getFullName()).append(" (named schema)");
      }
      else
      {
        sb.append(key.toString());
      }
      sb.append("\n");
      for (Validator v : e.getValue())
      {
        sb.append("    ").append(v).append("\n");
      }
    }
    return sb.toString();
  }
}
