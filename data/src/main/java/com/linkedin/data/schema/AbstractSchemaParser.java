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
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.DataLocation;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.message.MessageUtil;

import com.linkedin.data.schema.grammar.PdlSchemaParser;
import com.linkedin.data.schema.resolver.DefaultDataSchemaResolver;
import com.linkedin.data.schema.validation.CoercionMode;
import com.linkedin.data.schema.validation.RequiredMode;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Common base class for parsers that parse Data objects.
 *
 * @author slim
 */
abstract public class AbstractSchemaParser implements PegasusSchemaParser
{
  /**
   * Constructor with resolver.
   *
   * @param resolver to be used to find {@link DataSchema}s.
   */
  protected AbstractSchemaParser(DataSchemaResolver resolver)
  {
    _resolver = resolver == null ? new DefaultDataSchemaResolver() : resolver;
  }

  /**
   * Get the {@link DataSchemaResolver}.
   *
   * @return the resolver to used to find {@link DataSchema}s, may be null
   *         if no resolver has been provided to parser.
   */
  public DataSchemaResolver getResolver()
  {
    return _resolver;
  }

  /**
   * Return the top level {@link DataSchema}s.
   *
   * The top level DataSchema's represent the types
   * that are not defined within other types.
   *
   * @return the list of top level {@link DataSchema}s in the
   *         order that are defined.
   */
  public List<DataSchema> topLevelDataSchemas()
  {
    return Collections.unmodifiableList(_topLevelDataSchemas);
  }

  public Map<Object, DataLocation> dataLocationMap()
  {
    return _dataLocationMap;
  }

  /**
   * Set the {@link ValidationOptions} used to validate default values.
   *
   * @param validationOptions used to validate default values.
   */
  public void setValidationOptions(ValidationOptions validationOptions)
  {
    _validationOptions = validationOptions;
  }

  /**
   * Return the {@link ValidationOptions} used to validate default values.
   *
   * @return the {@link ValidationOptions} used to validate default values.
   */
  public ValidationOptions getValidationOptions()
  {
    return _validationOptions;
  }

  /**
   * Bind name and aliases to {@link NamedDataSchema}.
   *
   * @param name to bind.
   * @param aliasNames to bind.
   * @param schema to be bound to the name.
   * @return true if all names are bound to the specified {@link NamedDataSchema}.
   */
  protected boolean bindNameToSchema(Name name, List<Name> aliasNames, NamedDataSchema schema)
  {
    boolean ok = true;
    ok &= bindNameToSchema(name, schema);
    if (aliasNames != null)
    {
      for (Name aliasName : aliasNames)
      {
        ok &= bindNameToSchema(aliasName, schema);
      }
    }
    return ok;
  }

  /**
   * Bind a name to {@link NamedDataSchema}.
   *
   * @param name to bind.
   * @param schema to be bound to the name.
   * @return true if name is bound to the specified {@link NamedDataSchema}.
   */
  public boolean bindNameToSchema(Name name, NamedDataSchema schema)
  {
    boolean ok = true;
    String fullName = name.getFullName();
    if (name.isEmpty())
    {
      ok = false;
    }
    if (ok && DataSchemaUtil.typeStringToPrimitiveDataSchema(fullName) != null)
    {
      startErrorMessage(name).append("\"").append(fullName).append("\" is a pre-defined type and cannot be redefined.\n");
      ok = false;
    }
    if (ok)
    {
      DataSchemaLocation found = getResolver().existingSchemaLocation(name.getFullName());
      if (found != null)
      {
        if (found == DataSchemaLocation.NO_LOCATION)
        {
          startErrorMessage(name).append("\"").append(name.getFullName())
                  .append("\" already defined as " + getResolver().existingDataSchema(name.getFullName()) + ".\n");
        }
        else
        {
          startErrorMessage(name).append("\"").append(name.getFullName()).append("\" already defined at " + found + ".\n");
        }
        ok = false;
      }
      else
      {
        getResolver().bindNameToSchema(name, schema, getLocation());
      }
    }
    return ok;
  }

  /**
   * Look for {@link DataSchema} with the specified name.
   *
   * @param fullName to lookup.
   * @return the {@link DataSchema} if lookup was successful else return null.
   */
  public DataSchema lookupName(String fullName)
  {
    DataSchema schema = DataSchemaUtil.typeStringToPrimitiveDataSchema(fullName);
    if (schema == null)
    {
      schema = getResolver().findDataSchema(fullName, errorMessageBuilder());
      if (schema != null)
      {
        checkForCycleWithInclude(((NamedDataSchema) schema).getFullName());
      }
    }
    return schema;
  }

  protected void checkForCycleWithInclude(String fullName)
  {
    LinkedHashMap<String, Boolean> pendingSchemas = getResolver().getPendingSchemas();
    // Return if there is no cycle.
    if (!pendingSchemas.containsKey(fullName))
    {
      return;
    }

    boolean cycleFound = false;
    List<String> schemasInCycle = new ArrayList<>(pendingSchemas.size());
    for (Map.Entry<String, Boolean> pendingSchema : pendingSchemas.entrySet())
    {
      // Lookup the schema that started the cycle.
      if (cycleFound || pendingSchema.getKey().equals(fullName))
      {
        cycleFound = true;
        // Get all the schemas that form the cycle.
        schemasInCycle.add(pendingSchema.getKey());
      }
    }
    // Add error message if there is an include in the cycle.
    if (schemasInCycle.stream().anyMatch(pendingSchemas::get))
    {
      startErrorMessage(fullName)
          .append("\"").append(fullName).append("\"")
          .append(" cannot be parsed as it is part of circular reference involving includes.")
          .append(" Record(s) with include in the cycle: ")
          .append(schemasInCycle);
    }
  }

  /**
   * Lookup a name to obtain a {@link DataSchema}.
   *
   * The name may identify a {@link NamedDataSchema} obtained or a primitive type.
   *
   * @param name to lookup.
   * @return the {@link DataSchema} of a primitive or named type
   *         if the name can be resolved, else return null.
   */
  protected DataSchema stringToDataSchema(String name)
  {
    DataSchema schema = null;
    // Either primitive or name
    String fullName = computeFullName(name);
    DataSchema found = lookupName(fullName);
    if (found == null && !name.equals(fullName))
    {
      found = lookupName(name);
    }
    if (found == null)
    {
      StringBuilder sb = startErrorMessage(name).append("\"").append(name).append("\"");
      if (!name.equals(fullName))
      {
        sb.append(" or \"").append(fullName).append("\"");
      }
      sb.append(" cannot be resolved.\n");
    }
    else
    {
      schema = found;
    }
    return schema;
  }

  /**
   * Compute the full name from a name.
   *
   * If the name identifies a primitive type, return the name.
   * If the name is unqualified, the full name is computed by
   * pre-pending the current namespace and "." to the input name.
   * If the name is a full name, i.e. it contains a ".", then
   * return the name.
   *
   * @param name as input to compute the full name.
   * @return the computed full name.
   */
  public String computeFullName(String name)
  {
    String fullname;
    DataSchema schema = DataSchemaUtil.typeStringToPrimitiveDataSchema(name);
    if (schema != null)
    {
      fullname = name;
    }
    else if (Name.isFullName(name) || getCurrentNamespace().isEmpty())
    {
      fullname = name;
    }
    else
    {
      fullname = getCurrentNamespace() + "." + name;
    }
    return fullname;
  }

  /**
   * Set the current namespace.
   *
   * Current namespace is used to compute the full name from an unqualified name.
   *
   * @param namespace to set as current namespace.
   */
  public void setCurrentNamespace(String namespace)
  {
    _currentNamespace = namespace;
  }

  /**
   * Get the current namespace.
   *
   * @return the current namespace.
   */
  public String getCurrentNamespace()
  {
    return _currentNamespace;
  }

  /**
   * Set the current package.
   *
   * Current package for generated data bindings. It is prepended to the unqualified name of pegasus types to produce the
   * fully qualified data binding name.
   *
   * @param packageName to set as current package.
   */
  public void setCurrentPackage(String packageName)
  {
    _currentPackage = packageName;
  }

  /**
   * Get the current package.
   *
   * @return the current package.
   */
  public String getCurrentPackage()
  {
    return _currentPackage;
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
   * Parse a {@link DataMap} to obtain a package name for data binding.
   *
   * Return the package name if explicitly specified for the named schema in the {@link DataMap}. If package is not
   * specified, there are three cases:
   * <p><ul>
   * <li>If the namespace of the named schema is the same as currentNamespace, then it should inherit currentPackage as its package.
   * <li>If the namespace of the named schema is a sub-namespace of currentNamespace, then it should inherit currentPackage as its package prefix and
   * its package should be a sub-package of currentPackage.
   * <li>Otherwise, we will return null for the package name to indicate that no package override for this named schema, and by default its namespace
   * will be used in generating data binding.
   * </ul><p>
   *
   * @param map to parse.
   * @param packageKey is the key used to find the package in the map.
   * @param currentPackage is the current package.
   * @param currentNamespace is the current namespace.
   * @param name {@link Name} parsed from the {@link DataMap}
   * @return the package name for current named schema.
   */
  protected String getPackageFromDataMap(DataMap map, String packageKey, String currentPackage, String currentNamespace, Name name)
  {
    String packageName = getString(map, packageKey, false);
    if (packageName == null)
    {
      packageName = currentPackage;
      // check if the namespace of the named schema is a sub-namespace of currentNamespace, then it should inherit currentPackage as its package
      // prefix and its package should be a sub-package of currentPackage. This normally happens for a nested named schema with a fully qualified
      // name specified in its "name" field in the DataMap.
      if (name.getNamespace().startsWith(currentNamespace + ".") && packageName != null && !packageName.isEmpty())
      {
        // in this case, if package is not explicitly specified, we should append sub-namespace to saveCurrentPackage
        // but if saveCurrentPackage is not specified, then we should treat no package override for this nested type.
        packageName += name.getNamespace().substring(currentNamespace.length());
      }
    }
    return packageName;
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
      list = new ArrayList<>();
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
    Map<String, Object> props = new TreeMap<>();
    for (Map.Entry<String, Object> e : map.entrySet())
    {
      String key = e.getKey();
      if (reserved.contains(key) == false)
      {
        Object value = e.getValue();
        Object replaced = props.put(key, value);
        assert(replaced == null);
      }
    }
    return props;
  }

  /**
   * Validate that the default value complies with the {@link DataSchema} of the record.
   *
   * @param recordSchema of the record.
   */
  protected void validateDefaults(RecordDataSchema recordSchema)
  {
    for (RecordDataSchema.Field field : recordSchema.getFields())
    {
      Object value = field.getDefault();
      if (value != null)
      {
        DataSchema valueSchema = field.getType();
        ValidationResult result = ValidateDataAgainstSchema.validate(value, valueSchema, _validationOptions);
        if (result.isValid() == false)
        {
          startErrorMessage(value).
              append("Default value ").append(value).
              append(" of field \"").append(field.getName()).
              append("\" declared in record \"").append(recordSchema.getFullName()).
              append("\" failed validation.\n");
          MessageUtil.appendMessages(errorMessageBuilder(), result.getMessages());
        }
        Object fixed = result.getFixed();
        field.setDefault(fixed);
      }
      if (field.getDefault() instanceof DataComplex)
      {
        ((DataComplex) field.getDefault()).setReadOnly();
      }
    }
  }

  /**
   * Set the current location for the source of input to the parser.
   *
   * This current location is will be used to annotate {@link NamedDataSchema}s
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

  protected void checkTyperefCycle(TyperefDataSchema sourceSchema, DataSchema refSchema)
  {
    if (refSchema == null)
    {
      return;
    }
    if (refSchema.getType() == DataSchema.Type.TYPEREF)
    {
      if (sourceSchema.getFullName().equals(((TyperefDataSchema) refSchema).getFullName()))
      {
        startErrorMessage(sourceSchema.getFullName()).append("\"")
            .append(sourceSchema.getFullName())
            .append("\"")
            .append(" cannot be parsed as the typeref has a circular reference to itself.");
      }
      else
      {
        checkTyperefCycle(sourceSchema, ((TyperefDataSchema) refSchema).getRef());
      }
    }
    else if (refSchema.getType() == DataSchema.Type.UNION)
    {
      for (UnionDataSchema.Member member : ((UnionDataSchema) refSchema).getMembers())
      {
        checkTyperefCycle(sourceSchema, member.getType());
      }
    }
    else if (refSchema.getType() == DataSchema.Type.ARRAY)
    {
      checkTyperefCycle(sourceSchema, ((ArrayDataSchema) refSchema).getItems());
    }
    else if (refSchema.getType() == DataSchema.Type.MAP)
    {
      checkTyperefCycle(sourceSchema, ((MapDataSchema) refSchema).getValues());
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


  protected void addTopLevelSchema(DataSchema schema) {
    _topLevelDataSchemas.add(schema);
  }

  /**
   * Current namespace, used to determine full name from unqualified name.
   * This is used for over-the-wire rest.li protocol.
   */
  private String _currentNamespace = "";

  /**
   * Current package, used to pass package override information to nested unqualified name.
   * This is used for generated data models to resolve class name conflict.
   */
  private String _currentPackage = "";

  private final Map<Object, DataLocation> _dataLocationMap = new IdentityHashMap<>();
  private final List<DataSchema> _topLevelDataSchemas = new ArrayList<>();
  private final DataSchemaResolver _resolver;

  public static final ValidationOptions getDefaultSchemaParserValidationOptions()
  {
    return new ValidationOptions(RequiredMode.CAN_BE_ABSENT_IF_HAS_DEFAULT, CoercionMode.NORMAL);
  }

  private ValidationOptions _validationOptions = getDefaultSchemaParserValidationOptions();

  public static PegasusSchemaParser parserForFile(File schemaSourceFile, DataSchemaResolver resolver)
  {
    return parserForFileExtension(FileUtil.getExtension(schemaSourceFile), resolver);
  }

  public static PegasusSchemaParser parserForFileExtension(String extension, DataSchemaResolver resolver)
  {
    if (extension.equals(SchemaParser.FILETYPE))
    {
      return new SchemaParser(resolver);
    }
    else if (extension.equals(PdlSchemaParser.FILETYPE))
    {
      return new PdlSchemaParser(resolver);
    }
    else
    {
      throw new IllegalArgumentException("Unrecognized file extension: " + extension);
    }
  }
}
