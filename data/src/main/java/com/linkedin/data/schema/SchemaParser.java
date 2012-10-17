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


import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.DataLocation;
import com.linkedin.data.message.MessageUtil;
import com.linkedin.data.schema.resolver.DefaultDataSchemaResolver;
import com.linkedin.data.schema.validation.CoercionMode;
import com.linkedin.data.schema.validation.RequiredMode;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static com.linkedin.data.schema.DataSchemaConstants.*;


/**
 * Schema Parser.
 * <p>
 *
 * Inspired by Avro 1.4.1 specification.
 * <p>
 *
 * The parser can use an optional {@link DataSchemaResolver} to resolve names
 * not found parsed from its input.
 * <p>
 *
 * @author slim
 */
public class SchemaParser extends AbstractDataParser
{
  /**
   * Constructor.
   */
  public SchemaParser()
  {
    this(null);
  }

  /**
   * Constructor with resolver.
   *
   * @param resolver to be used to find {@link DataSchema}'s.
   */
  public SchemaParser(DataSchemaResolver resolver)
  {
    _resolver = (resolver == null ? new DefaultDataSchemaResolver() : resolver);
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
   * Get the {@link DataSchemaResolver}.
   *
   * @return the resolver to used to find {@link DataSchema}'s, may be null
   *         if no resolver has been provided to parser.
   */
  public DataSchemaResolver getResolver()
  {
    return _resolver;
  }

  /**
   * Return the top level {@link DataSchema}'s.
   *
   * The top level DataSchema's represent the types
   * that are not defined within other types.
   *
   * @return the list of top level {@link DataSchema}'s in the
   *         order that are defined.
   */
  public List<DataSchema> topLevelDataSchemas()
  {
    return Collections.unmodifiableList(_topLevelDataSchemas);
  }

  /**
   * Dump the top level schemas.
   *
   * @return a textual dump of the top level schemas.
   */
  public String schemasToString()
  {
    return SchemaToJsonEncoder.schemasToJson(_topLevelDataSchemas, JsonBuilder.Pretty.SPACES);
  }

  /**
   * Parse a JSON representation of a schema from an {@link InputStream}.
   *
   * The top level {@link DataSchema}'s parsed are in {@link #topLevelDataSchemas}.
   * These are the types that are not defined within other types.
   * Parse errors are in {@link #errorMessageBuilder} and indicated
   * by {@link #hasError()}.
   *
   * @param inputStream with the JSON representation of the schema.
   */
  public void parse(InputStream inputStream)
  {
    List<Object> objects = jsonInputStreamToObjects(inputStream);
    parse(objects);
  }

  /**
   * Parse list of Data objects.
   *
   * The {@link DataSchema}'s parsed are in {@link #topLevelDataSchemas}.
   * Parse errors are in {@link #errorMessageBuilder} and indicated
   * by {@link #hasError()}.
   *
   * @param list of Data objects.
   */
  public void parse(List<Object> list)
  {
    for (Object o : list)
    {
      DataSchema schema = parseObject(o);
      if (schema != null)
      {
        _topLevelDataSchemas.add(schema);
      }
    }
  }

  /**
   * Parse a Data object to obtain a {@link DataSchema}.
   *
   * If the Data object is a string, it may identify a {@link NamedDataSchema} or
   * a primitive type. If it is a {@link DataList}, it may be parsed to obtain
   * a {@link UnionDataSchema}. If it is a {@link DataMap}, it may be parsed to obtain
   * a {@link ComplexDataSchema}. Otherwise, it cannot be parsed to obtain a
   * {@link DataSchema}.
   *
   * The resulting DataSchema is not added to the top level domains.
   *
   * @param object to parse.
   * @return a {@link DataSchema} or null if a {@link DataSchema} cannot be
   *         parsed to obtain a {@link DataSchema}.
   */
  public DataSchema parseObject(Object object)
  {
    DataSchema schema = null;
    if (object instanceof String)
    {
      schema = stringToDataSchema((String) object);
    }
    else if (object instanceof DataList)
    {
      schema = dataListToDataSchema((DataList) object);
    }
    else if (object instanceof DataMap)
    {
      schema = dataMapToDataSchema((DataMap) object);
    }
    else
    {
      startErrorMessage(object).append(object).append(" is not a type.\n");
    }
    return schema;
  }

  /**
   * Parse a {@link DataList} to obtain the a list of fields.
   *
   * The {@link DataList} should contain a list of fields.
   *
   * @param recordSchema of the record that defines these fields.
   * @param list is the {@link DataList} to parse.
   * @return a list of fields.
   */
  public List<RecordDataSchema.Field> parseFields(RecordDataSchema recordSchema, DataList list)
  {
    List<RecordDataSchema.Field> fields = new ArrayList<RecordDataSchema.Field>();
    for (Object o : list)
    {
      boolean ok = true;
      if (o instanceof DataMap)
      {
        DataMap fieldMap = (DataMap) o;
        String name = getString(fieldMap, NAME_KEY, true);
        DataSchema type = getSchemaData(fieldMap, TYPE_KEY);
        String doc = getString(fieldMap, DOC_KEY, false);
        Boolean optional = getBoolean(fieldMap, OPTIONAL_KEY, false);
        RecordDataSchema.Field.Order sortOrder = null;
        String order = getString(fieldMap, ORDER_KEY, false);
        if (order != null)
        {
          try
          {
            sortOrder = RecordDataSchema.Field.Order.valueOf(order.toUpperCase());
          }
          catch (IllegalArgumentException exc)
          {
            startErrorMessage(order).append("\"").append(order).append("\" is an invalid sort order.\n");
          }
        }
        Map<String, Object> properties = extractProperties(fieldMap, FIELD_KEYS);
        List<String> aliases = getStringList(fieldMap, ALIASES_KEY, false);
        if (name != null && type != null)
        {
          RecordDataSchema.Field field = new RecordDataSchema.Field(type);
          field.setDefault(fieldMap.get(DEFAULT_KEY));
          if (doc != null)
          {
            field.setDoc(doc);
          }
          field.setName(name, startCalleeMessageBuilder());
          appendCalleeMessage(fieldMap);
          if (aliases != null)
          {
            field.setAliases(aliases, startCalleeMessageBuilder());
            appendCalleeMessage(fieldMap);
          }
          if (optional != null)
          {
            field.setOptional(optional);
          }
          if (sortOrder != null)
          {
            field.setOrder(sortOrder);
          }
          field.setProperties(properties);
          field.setRecord(recordSchema);
          fields.add(field);
        }
        else
        {
          ok = false;
        }
      }
      else
      {
        ok = false;
      }
      if (ok == false)
      {
        startErrorMessage(o).append(o).append(" is not a valid field.\n");
      }
    }
    return fields;
  }


  /**
   * Parse a {@link DataList} to obtain an {@link UnionDataSchema}.
   *
   * The resulting {@link UnionDataSchema} is not added to the top level domains.
   *
   * @param list is the {@link DataList} to parse.
   * @return the {@link UnionDataSchema} parsed from the list.
   */
  public UnionDataSchema parseUnion(DataList list)
  {
    return dataListToDataSchema(list);
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
      schema = _resolver.findDataSchema(fullName, errorMessageBuilder());
    }
    return schema;
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
   * Parse a {@link DataList} to obtain a {@link DataSchema}.
   *
   * @param list to create {@link DataSchema} from.
   * @return a {@link UnionDataSchema} obtained from {@link DataList}.
   */
  protected UnionDataSchema dataListToDataSchema(DataList list)
  {
    // Union
    List<DataSchema> types = new ArrayList<DataSchema>();
    for (Object o : list)
    {
      DataSchema type = parseObject(o);
      if (type != null)
      {
        types.add(type);
      }
    }
    UnionDataSchema schema = new UnionDataSchema();
    schema.setTypes(types, startCalleeMessageBuilder());
    appendCalleeMessage(list);
    return schema;
  }

  /**
   * Parse a {@link DataMap} to obtain a {@link DataSchema}.
   *
   * A {@link DataMap} may define an array, enum, fixed, map or record type.
   *
   * @param map to parse.
   * @return the {@link DataSchema} obtained by the {@link DataMap}.
   */
  protected DataSchema dataMapToDataSchema(DataMap map)
  {
    // potentially just {type=SomeType}, (where SomeType is primitive) in which case this should just return SomeType.
    // this also accepts things like {type=a, name=b}
    // This is kind of dumb but it is also how the out-of-the-box avro parser works.
    String s = getString(map, TYPE_KEY, true);
    if (DataSchemaUtil.typeStringToPrimitiveDataSchema(s) != null)
    {
      return stringToDataSchema(s);
    }

    DataSchema.Type type = getType(map);
    if (type == null)
    {
      return null;
    }
    ComplexDataSchema schema = null;
    NamedDataSchema namedSchema = null;
    String saveCurrentNamespace = getCurrentNamespace();

    Name name = null;
    List<Name> aliasNames = null;
    if (NAMED_DATA_SCHEMA_TYPE_SET.contains(type))
    {
      name = getNameFromDataMap(map, NAME_KEY, saveCurrentNamespace);
      setCurrentNamespace(name.getNamespace());
      aliasNames = getAliases(map);
    }
    else
    {
      Object found = map.get(NAME_KEY);
      if (found != null)
      {
        startErrorMessage(map).append(type).append(" must not have name.\n");
      }
      found = map.get(NAMESPACE_KEY);
      if (found != null)
      {
        startErrorMessage(map).append(type).append(" must not have namespace.\n");
      }
      found = map.get(ALIASES_KEY);
      if (found != null)
      {
        startErrorMessage(map).append(type).append(" must not have aliases.\n");
      }
    }

    switch (type)
    {
      case ARRAY:
        DataSchema itemsSchema = getSchemaData(map, ITEMS_KEY);
        ArrayDataSchema arraySchema = new ArrayDataSchema(itemsSchema);
        schema = arraySchema;
        break;
      case ENUM:
        List<String> symbols = getStringList(map, SYMBOLS_KEY, true);
        EnumDataSchema enumSchema = new EnumDataSchema(name);
        schema = namedSchema = enumSchema;
        bindNameToSchema(name, aliasNames, enumSchema);
        enumSchema.setSymbols(symbols, startCalleeMessageBuilder());
        appendCalleeMessage(map);
        break;
      case FIXED:
        Integer size = getInteger(map, SIZE_KEY, true);
        FixedDataSchema fixedSchema = new FixedDataSchema(name);
        schema = namedSchema = fixedSchema;
        bindNameToSchema(name, aliasNames, fixedSchema);
        fixedSchema.setSize(size, startCalleeMessageBuilder());
        appendCalleeMessage(size);
        break;
      case MAP:
        DataSchema valuesSchema = getSchemaData(map, VALUES_KEY);
        MapDataSchema mapSchema = new MapDataSchema(valuesSchema);
        schema = mapSchema;
        break;
      case RECORD:
        String typeUpper = ((String) map.get(TYPE_KEY)).toUpperCase();
        RecordDataSchema.RecordType recordType = RecordDataSchema.RecordType.valueOf(typeUpper);
        RecordDataSchema recordSchema = new RecordDataSchema(name, recordType);
        schema = namedSchema = recordSchema;
        bindNameToSchema(name, aliasNames, recordSchema);
        List<RecordDataSchema.Field> fields = new ArrayList<RecordDataSchema.Field>();

        DataList includeList = getDataList(map, INCLUDE_KEY, false);
        DataLocation includeListLocation = lookupDataLocation(includeList);
        DataList fieldsList = getDataList(map, FIELDS_KEY, true);
        DataLocation fieldsListLocation = lookupDataLocation(fieldsList);

        // the parser must parse fields and include in the same order that they appear in the input.
        // determine whether to process fields first or include first
        boolean fieldsBeforeInclude =
          (includeList != null && fieldsList != null && includeListLocation.compareTo(fieldsListLocation) > 0);

        if (fieldsBeforeInclude)
        {
          // fields is before include
          fields.addAll(parseFields(recordSchema, fieldsList));
          fields.addAll(parseInclude(recordSchema, includeList));
        }
        else
        {
          // include is before fields
          fields.addAll(parseInclude(recordSchema, includeList));
          fields.addAll(parseFields(recordSchema, fieldsList));
        }

        recordSchema.setFields(fields, startCalleeMessageBuilder());
        appendCalleeMessage(fieldsList);

        // does this need to be after setAliases? not for now since aliases don't affect validation.
        validateDefaults(recordSchema);
        break;
      case TYPEREF:
        TyperefDataSchema typerefSchema = new TyperefDataSchema(name);
        schema = namedSchema = typerefSchema;
        DataSchema referencedTypeSchema = getSchemaData(map, REF_KEY);
        typerefSchema.setReferencedType(referencedTypeSchema);
        // bind name after getSchemaData to prevent circular typeref
        // circular typeref is not possible because this typeref name cannot be resolved until
        // after the referenced type has been defined.
        bindNameToSchema(name, aliasNames, typerefSchema);
        break;
      default:
        startErrorMessage(map).append(type).append(" is not expected within ").append(map).append(".\n");
    }

    if (namedSchema != null)
    {
      String doc = getString(map, DOC_KEY, false);
      if (doc != null)
      {
        namedSchema.setDoc(doc);
      }
      if (aliasNames != null)
      {
        namedSchema.setAliases(aliasNames);
      }
    }

    if (schema != null)
    {
      Map<String, Object> properties = extractProperties(map, SCHEMA_KEYS);
      schema.setProperties(properties);
    }

    setCurrentNamespace(saveCurrentNamespace);
    return schema;
  }

  /**
   * Parse record include.
   *
   * @param recordSchema provides the {@link RecordDataSchema} to the include belongs to.
   * @param includeList provides list of include to be processed.
   * @return a list of fields to be added to the record.
   */
  private List<RecordDataSchema.Field> parseInclude(RecordDataSchema recordSchema, DataList includeList)
  {
    List<RecordDataSchema.Field> fields = Collections.emptyList();

    // handle include
    // only includes fields, does not include any attributes of the included record
    // should consider whether mechanisms for including other attributes.
    if (includeList != null && includeList.isEmpty() == false)
    {
      fields = new ArrayList<RecordDataSchema.Field>();
      List<NamedDataSchema> include = new ArrayList<NamedDataSchema>(includeList.size());
      for (Object anInclude : includeList)
      {
        DataSchema includedSchema = parseObject(anInclude);
        if (includedSchema == null)
        {
          // error message should already have been emitted providing
          // information on why the included schema could not be obtained.
        }
        else if (includedSchema.getDereferencedType() != DataSchema.Type.RECORD)
        {
          startErrorMessage(anInclude).
            append("\"").append(recordSchema.getFullName()).append("\" cannot include ").
            append(includedSchema).append(" because it is not a record.\n");
        }
        else
        {
          NamedDataSchema includedNamed = (NamedDataSchema) includedSchema;
          include.add(includedNamed);
          RecordDataSchema includedRecord = (RecordDataSchema) includedSchema.getDereferencedDataSchema();
          fields.addAll(includedRecord.getFields());
        }
      }
      recordSchema.setInclude(include);
    }
    return fields;
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
      DataSchema found = _resolver.existingDataSchema(name.getFullName());
      if (found != null)
      {
        startErrorMessage(name).append("\"").append(name.getFullName()).append("\" already defined as " + found + ".\n");
        ok = false;
      }
      else
      {
        _resolver.bindNameToSchema(name, schema, getLocation());
      }
    }
    return ok;
  }

  /**
   * Parse a {@link DataMap} to obtain aliases.
   *
   * @param map to parse.
   * @return aliases in the order defined or null if aliases are not defined.
   */
  protected List<Name> getAliases(DataMap map)
  {
    List<String> aliases = getStringList(map, ALIASES_KEY, false);
    List<Name> aliasNames = null;
    if (aliases != null)
    {
      aliasNames = new ArrayList<Name>(aliases.size());
      for (String alias : aliases)
      {
        Name name = null;
        if (alias.contains("."))
        {
          name = new Name(alias, startCalleeMessageBuilder());
          appendCalleeMessage(map);
        }
        else
        {
          name = new Name(alias, getCurrentNamespace(), startCalleeMessageBuilder());
          appendCalleeMessage(map);
        }
        if (name != null)
        {
          aliasNames.add(name);
          // remember location where name is defined,
          // this allows error messages related to duplicate definitions of names
          // to include the location where name is defined.
          addToDataLocationMap(name, lookupDataLocation(alias));
        }
      }
    }
    return aliasNames;
  }

  /**
   * Parse the value of a field within a {@link DataMap} to obtain a {@link DataSchema}.
   *
   * The value of the field identified by the specified key
   * should define or identity a type.
   * This method will always return a {@link DataSchema}. If there is an error,
   * a {@link NullDataSchema} will be returned and an error message will be
   * appended to {@link #errorMessageBuilder}.
   *
   * @param map to lookup the key.
   * @param key to lookup a field in the map.
   * @return a {@link DataSchema}.
   */
  protected DataSchema getSchemaData(DataMap map, String key)
  {
    DataSchema schema = NULL_DATA_SCHEMA;
    Object obj = map.get(key);
    if (obj != null)
    {
      schema = parseObject(obj);
    }
    else
    {
      startErrorMessage(map).append(key).append(" is required but it is not present.\n");
    }
    return schema;
  }

  /**
   * Parse a {@link DataMap} to determine the type defined by the {@link DataMap}.
   *
   * The "type" field of the {@link DataMap} defines the type defined by the {@link DataMap}.
   * If a type cannot be determined, append error message to {@link #errorMessageBuilder}
   * and return a "null" type.
   *
   * @param map to parse.
   * @return the type determined from the "type" field.
   */
  protected DataSchema.Type getType(DataMap map)
  {
    DataSchema.Type type = DataSchema.Type.NULL;
    String s = getString(map, TYPE_KEY, true);
    if (s != null)
    {
      DataSchema.Type found = DataSchemaUtil.typeStringToComplexDataSchemaType(s);
      if (found != null)
      {
        type = found;
      }
      else
      {
        startErrorMessage(map).append("\"").append(s).append("\" is an invalid type.\n");
      }
    }
    return type;
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

  @Override
  public StringBuilder errorMessageBuilder()
  {
    return _errorMessageBuilder;
  }

  @Override
  public Map<Object, DataLocation> dataLocationMap()
  {
    return _dataLocationMap;
  }

  /**
   * Current namespace, used to determine full name from unqualified name.
   */
  private String _currentNamespace = "";

  private final List<DataSchema> _topLevelDataSchemas = new ArrayList<DataSchema>();
  private final DataSchemaResolver _resolver;
  private final Map<Object, DataLocation> _dataLocationMap = new IdentityHashMap<Object, DataLocation>();
  private StringBuilder _errorMessageBuilder = new StringBuilder();
  private ValidationOptions _validationOptions = new ValidationOptions(RequiredMode.CAN_BE_ABSENT_IF_HAS_DEFAULT, CoercionMode.NORMAL);

}
