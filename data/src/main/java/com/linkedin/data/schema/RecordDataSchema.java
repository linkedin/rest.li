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

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.linkedin.data.schema.DataSchemaConstants.FIELD_NAME_PATTERN;


/**
 * {@link DataSchema} for record.
 *
 * @author slim
 */
public final class RecordDataSchema extends NamedDataSchema
{
  /**
   * Do not allow an optional field to be an union that includes the null type.
   *
   * Currently, this is not enabled. This will be enabled in the future after
   * more examination of existing schemas.
   */
  public static final boolean OPTIONAL_FIELD_CANNOT_BE_UNION_WITH_NULL = false;

  /**
   * Interface for representing a field within a record.
   *
   * @author slim
   */
  public static class Field
  {

    public static enum Order
    {
      ASCENDING,
      DESCENDING,
      IGNORE;

      private final String _name;
      private Order()
      {
        _name = name().toLowerCase();
      }

      @Override
      public String toString()
      {
        return _name;
      }
    }

    /**
     * Constructor.
     *
     * @param type of the field.
     */
    public Field(DataSchema type)
    {
      setType(type);
    }

    /**
     * Set the type of the field.
     *
     * @param type of the field.
     */
    public void setType(DataSchema type)
    {
      if (type == null)
      {
        _type = DataSchemaConstants.NULL_DATA_SCHEMA;
        _hasError = true;
      }
      else
      {
        _type = type;
      }
    }

    /**
     * Set the name of the field.
     *
     * @param name of the field.
     * @param errorMessageBuilder to append error message to.
     * @return false if the name is not a valid field name.
     */
    public boolean setName(String name, StringBuilder errorMessageBuilder)
    {
      boolean ok = true;
      if (isValidFieldName(name) == false)
      {
        errorMessageBuilder.append("\"").append(name).append("\" is an invalid field name.\n");
        ok = false;
      }
      _name = name;
      _hasError |= !ok;
      return ok;
    }

    /**
     * Return whether the field has an error.
     *
     * @return if the field has an error.
     */
    public boolean hasError()
    {
      return _hasError;
    }

    /**
     * Set the documentation of the field.
     *
     * @param documentation of the field.
     */
    public void setDoc(String documentation)
    {
      _doc = documentation;
    }

    /**
     * Set the properties of the field.
     *
     * @param properties of the field.
     */
    public void setProperties(Map<String, Object> properties)
    {
      _properties = Collections.unmodifiableMap(properties);
    }

    /**
     * Set the aliases of the field.
     *
     * @param aliases of the field.
     * @param errorMessageBuilder to append error message to.
     * @return false if the one or more aliases are not valid field names.
     */
    public boolean setAliases(List<String> aliases, StringBuilder errorMessageBuilder)
    {
      boolean ok = true;
      for (String alias : aliases)
      {
        if (isValidFieldName(alias) == false)
        {
          errorMessageBuilder.append(alias).append(" is an invalid field name alias.\n");
          ok = false;
        }
        // is there a need to check for uniqueness with field and across fields of a record?
      }
      _aliases = Collections.unmodifiableList(aliases);
      _hasError |= !ok;
      return ok;
    }

    /**
     * Set the default value.
     *
     * This method does not validate that the default value complies with the
     * DataSchema of the field. This is because validation cannot occur until
     * all fields of a record have been parsed and provided to the {@link RecordDataSchema}.
     *
     * @param defaultValue of the field.
     */
    public void setDefault(Object defaultValue)
    {
      _defaultValue = defaultValue;
    }

    /**
     * Set the optional flag.
     *
     * @param optional flag of the field.
     */
    public void setOptional(boolean optional)
    {
      _optional = optional;
    }

    /**
     * Set the sort order for the field.
     *
     * @param sortOrder of the field.
     */
    public void setOrder(Order sortOrder)
    {
      _order = sortOrder;
    }

    /**
     * Set the {@link RecordDataSchema} that this field is defined in.
     *
     * A field defined in another record may be included in the list of fields
     * of this record if this field is included from the other record.
     *
     * @param record where this field is defined in.
     */
    public void setRecord(RecordDataSchema record)
    {
      _record = record;
    }

    /**
     * Return the name of the field.
     *
     * @return the name of the field.
     */
    public String getName()
    {
      return _name;
    }

    /**
     * Return the {@link DataSchema} of the field.
     *
     * @return the {@link DataSchema} of the field.
     */
    public DataSchema getType()
    {
      return _type;
    }

    /**
     * Return the documentation of the field.
     *
     * @return the documentation of the field.
     */
    public String getDoc()
    {
      return _doc;
    }

    /**
     * Return the properties of the field.
     *
     * @return the properties of the field.
     */
    public Map<String, Object> getProperties()
    {
      return _properties;
    }

    /**
     * Return the aliases of the field.
     *
     * @return the aliases of the field.
     */
    public List<String> getAliases()
    {
      return _aliases;
    }

    /**
     * Return default value.
     *
     * @return default value if there is one, else return null.
     */
    public Object getDefault()
    {
      return _defaultValue;
    }

    /**
     * Return optional flag.
     *
     * @return the optional flag, true if optional
     */
    public boolean getOptional()
    {
      return _optional;
    }

    /**
     * Return the sort order of the field.
     *
     * @return the sort order of the field.
     */
    public Order getOrder()
    {
      return _order;
    }

    /**
     * Return the {@link RecordDataSchema} that this field is defined in.
     *
     * A field defined in another record may be included in the list of fields
     * of this record if this field is included from the other record.
     *
     * @return the {@link RecordDataSchema} that this field is defined in.
     */
    public RecordDataSchema getRecord()
    {
      return _record;
    }

    /**
     * Sets if the record field type is declared inline in the schema.
     * @param declaredInline true if the record field type is declared inline, false if it is referenced by name.
     */
    public void setDeclaredInline(boolean declaredInline)
    {
      _declaredInline = declaredInline;
    }

    /**
     * Checks if record field type is declared inline.
     * @return true if the record field type is declared inline, false if it is referenced by name.
     */
    public boolean isDeclaredInline()
    {
      return _declaredInline;
    }

    @Override
    public boolean equals(Object object)
    {
      // _record is not considered when computing equals

      boolean result;
      if (object == this)
      {
        result = true;
      }
      else if (object != null && object.getClass() == Field.class)
      {
        Field other = (Field) object;
        result = _hasError == other._hasError &&
                 _type.equals(other._type) &&
                 _name.equals(other._name) &&
                 _doc.equals(other._doc) &&
                 ((_defaultValue == null && other._defaultValue == null) ||
                  (_defaultValue != null && _defaultValue.equals(other._defaultValue))) &&
                 _optional == other._optional &&
                 _order == other._order &&
                 _aliases.equals(other._aliases) &&
                 _properties.equals(other._properties);
      }
      else
      {
        result = false;
      }
      return result;
    }

    @Override
    public int hashCode()
    {
      // _record is not considered when computing hashCode

      return _type.hashCode() ^
             _name.hashCode() ^
             _doc.hashCode() ^
             (_defaultValue == null ? 0 : _defaultValue.hashCode()) ^
             (_optional ? 0xAAAAAAAA : 0x55555555) ^
             _order.hashCode() ^
             _aliases.hashCode() ^
             _properties.hashCode();
    }

    /**
     * Return whether the input string is a valid field name.
     *
     * @param input string to check.
     * @return whether the input string is a valid field name.
     */
    static public boolean isValidFieldName(String input)
    {
      return FIELD_NAME_PATTERN.matcher(input).matches();
    }

    private boolean _hasError = false;
    private DataSchema _type = DataSchemaConstants.NULL_DATA_SCHEMA;
    private String _name = "";
    private String _doc = "";
    private Object _defaultValue;
    private boolean _optional;
    private Order _order = Order.ASCENDING;
    private RecordDataSchema _record = null;
    private List<String> _aliases = _emptyAliases;
    private Map<String, Object> _properties = _emptyProperties;
    private boolean _declaredInline = false;

    static private final Map<String, Object> _emptyProperties = Collections.emptyMap();
    static private final List<String> _emptyAliases = Collections.emptyList();
  }

  public static enum RecordType
  {
    RECORD,
    ERROR
  }

  public RecordDataSchema(Name name, RecordType recordType)
  {
    super(Type.RECORD, name);
    _recordType = recordType;
  }

  /**
   * Return the type of this record.
   *
   * @return type of this record.
   */
  public RecordType recordType()
  {
    return _recordType ;
  }

  /**
   * Return whether the record is an error record
   *
   * @return whether the record is an error record.
   */
  public boolean isErrorRecord()
  {
    return _recordType == RecordType.ERROR;
  }

  /**
   * Return fields in the order declared.
   *
   * @return the fields in the the order declared.
   */
  public List<Field> getFields()
  {
    return _fields;
  }

  /**
   * Returns the index of a field.
   *
   * @param fieldName to obtain index for.
   * @return positive integer which is the index of the field if found else return -1.
   */
  public int index(String fieldName)
  {
    Integer i = _fieldNameToIndexMap.get(fieldName);
    return (i == null ? -1 : i);
  }

  /**
   * Returns whether the fieldName is a member of the record.
   *
   * @param fieldName to check.
   * @return true if fieldName is a member of the record.
   */
  public boolean contains(String fieldName)
  {
    return _fieldNameToIndexMap.containsKey(fieldName);
  }

  /**
   * Returns the {@link Field} with the specified name.
   *
   * @param fieldName to lookup Field.
   * @return the {@link Field} with the specified name if found, else return null.
   */
  public Field getField(String fieldName)
  {
    Integer index = _fieldNameToIndexMap.get(fieldName);
    return (index == null ? null : _fields.get(index));
  }

  /**
   * Sets the fields of the record.
   *
   * @param fields of the record in the order they are defined.
   * @param errorMessageBuilder to append error message to.
   * @return false if the field name is defined more than once.
   */
  public boolean setFields(List<Field> fields, StringBuilder errorMessageBuilder)
  {
    boolean ok = true;
    _fields = Collections.unmodifiableList(fields);
    Map<String, Integer> map = new HashMap<String, Integer>();
    int index = 0;
    for (Field field : _fields)
    {
      if (OPTIONAL_FIELD_CANNOT_BE_UNION_WITH_NULL &&
          field.getType().getDereferencedType() == DataSchema.Type.UNION)
      {
        UnionDataSchema unionDataSchema = (UnionDataSchema) field.getType().getDereferencedDataSchema();
        if (field.getOptional() == true && unionDataSchema.getTypeByMemberKey(DataSchemaConstants.NULL_TYPE) != null)
        {
          errorMessageBuilder.append("Field \"").append(field.getName());
          errorMessageBuilder.append("\" is optional and its type is a union with null.\n");
          ok = false;
        }
      }
      Integer oldIndex = map.put(field.getName(), index);
      if (oldIndex != null)
      {
        Field oldField = fields.get(oldIndex);
        boolean emitSource = ! ((field.getRecord() == oldField.getRecord()) && (field.getRecord() == this));
        errorMessageBuilder.append("Field \"").append(field.getName());
        errorMessageBuilder.append("\" defined more than once, with ").append(oldField.getType());
        if (emitSource)
        {
          errorMessageBuilder.append(" defined in \"").append(oldField.getRecord().getFullName()).append("\"");
        }
        errorMessageBuilder.append(" and ").append(field.getType());
        if (emitSource)
        {
          errorMessageBuilder.append(" defined in \"").append(field.getRecord().getFullName()).append("\"");
        }
        errorMessageBuilder.append(".\n");
        map.put(field.getName(), oldIndex);
        ok = false;
      }
      index++;
    }
    _fieldNameToIndexMap = Collections.unmodifiableMap(map);
    if (ok == false)
    {
      setHasError();
    }
    return ok;
  }

  /**
   * Get the list of included {@link NamedDataSchema}'s.
   *
   * The schema's must resolve to a record. The type is {@link NamedDataSchema}
   * because the included type may be a typeref which resolves to a record.
   *
   * @return the list of included {@link NamedDataSchema}'s.
   */
  public List<NamedDataSchema> getInclude()
  {
    return _include;
  }

  /**
   * Set the list of included {@link RecordDataSchema}'s.
   *
   * The schema's must resolve to a record. The type is {@link NamedDataSchema}
   * because the included type may be a typeref which resolves to a record.
   *
   * @param include that have been included.
   */
  public void setInclude(List<NamedDataSchema> include)
  {
    _include = Collections.unmodifiableList(include);
  }


  public void setIncludesDeclaredInline(Set<NamedDataSchema> includesDeclaredInline) {
    _includesDeclaredInline = Collections.unmodifiableSet(includesDeclaredInline);
  }

  public boolean isIncludeDeclaredInline(NamedDataSchema type) {
    return _includesDeclaredInline.contains(type);
  }

  public void setFieldsBeforeIncludes(boolean fieldsBeforeIncludes)
  {
    _fieldsBeforeIncludes = fieldsBeforeIncludes;
  }

  public boolean isFieldsBeforeIncludes()
  {
    return _fieldsBeforeIncludes;
  }

  @Override
  public boolean equals(Object object)
  {
    // included records are not considered for equals, but the included fields are.

    if (object == this)
    {
      return true;
    }
    if (object != null && object.getClass() == RecordDataSchema.class)
    {
      RecordDataSchema other = (RecordDataSchema) object;
      if (super.equals(other) == false || _recordType != other._recordType)
      {
        return false;
      }
      IdentityHashMap<RecordDataSchema, RecordDataSchema> trackingMap = _equalsTracking.get();
      boolean startTracking = (trackingMap == null);
      try
      {
        if (startTracking)
        {
          trackingMap = new IdentityHashMap<RecordDataSchema, RecordDataSchema>();
          _equalsTracking.set(trackingMap);
        }
        else
        {
          RecordDataSchema trackedOther = trackingMap.get(this);
          if (trackedOther == other)
          {
            return true;
          }
          else if (trackedOther != null)
          {
            return false;
          }
        }
        trackingMap.put(this, other);
        return _fields.equals(other._fields);
      }
      finally
      {
        if (startTracking)
        {
          _equalsTracking.remove();
        }
      }
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    // included records are not considered for hashCode, but the included fields are.

    IdentityHashMap<RecordDataSchema, Boolean> trackingMap = _hashCodeTracking.get();
    boolean startTracking = (trackingMap == null);
    try
    {
      if (startTracking)
      {
        trackingMap = new IdentityHashMap<RecordDataSchema, Boolean>();
        _hashCodeTracking.set(trackingMap);
      }
      if (trackingMap.containsKey(this))
      {
        return 0;
      }
      trackingMap.put(this, Boolean.TRUE);
      return super.hashCode() ^ _recordType.hashCode() ^ _fields.hashCode();
    }
    finally
    {
      if (startTracking)
      {
        _hashCodeTracking.remove();
      }
    }
  }

  private List<NamedDataSchema> _include = _emptyNamedSchemas;
  private List<Field> _fields = _emptyFields;
  private Map<String, Integer> _fieldNameToIndexMap = _emptyFieldNameToIndexMap;
  private final RecordType _recordType;
  private Set<NamedDataSchema> _includesDeclaredInline = _emptyIncludesDeclaredInline;
  private boolean _fieldsBeforeIncludes = false;

  private static ThreadLocal<IdentityHashMap<RecordDataSchema, RecordDataSchema>> _equalsTracking =
      new ThreadLocal<IdentityHashMap<RecordDataSchema, RecordDataSchema>>()
  {
    @Override
    protected IdentityHashMap<RecordDataSchema, RecordDataSchema> initialValue()
    {
      return null;
    }
  };

  private static ThreadLocal<IdentityHashMap<RecordDataSchema, Boolean>> _hashCodeTracking =
    new ThreadLocal<IdentityHashMap<RecordDataSchema, Boolean>>()
  {
    @Override
    protected IdentityHashMap<RecordDataSchema, Boolean> initialValue()
    {
      return null;
    }
  };

  private static final List<NamedDataSchema> _emptyNamedSchemas = Collections.emptyList();
  private static final List<Field> _emptyFields = Collections.emptyList();
  private static final Map<String, Integer> _emptyFieldNameToIndexMap = Collections.emptyMap();
  private static final Set<NamedDataSchema> _emptyIncludesDeclaredInline = Collections.emptySet();
}
