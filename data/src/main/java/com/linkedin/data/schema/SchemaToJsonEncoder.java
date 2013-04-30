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


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.linkedin.data.schema.DataSchemaConstants.*;

/**
 * Encodes a {@link DataSchema} to a JSON representation.
 */
public class SchemaToJsonEncoder
{
  /**
   * Encode a {@link DataSchema} to a JSON encoded string.
   *
   * @param schema is the {@link DataSchema} to build a JSON encoded output for.
   * @param pretty is the pretty printing mode.
   * @return the JSON encoded string representing the {@link DataSchema}.
   */
  public static String schemaToJson(DataSchema schema, JsonBuilder.Pretty pretty)
  {
    try
    {
      JsonBuilder builder = new JsonBuilder(pretty);
      SchemaToJsonEncoder encoder = new SchemaToJsonEncoder(builder);
      encoder.encode(schema);
      return builder.result();
    }
    catch (IOException exc)
    {
      return exc.getMessage();
    }
  }

  /**
   * Encode a collection of {@link DataSchema}'s to a JSON encoded string.
   *
   * @param schemas is the list {@link DataSchema}'s to build a JSON encoded output for.
   * @param pretty is the pretty printing mode.
   * @return the JSON encoded string representing the {@link DataSchema}.
   */
  public static String schemasToJson(Collection<DataSchema> schemas, JsonBuilder.Pretty pretty)
  {
    try
    {
      JsonBuilder builder = new JsonBuilder(pretty);
      SchemaToJsonEncoder encoder = new SchemaToJsonEncoder(builder);
      for (DataSchema schema : schemas)
      {
        encoder.encode(schema);
      }
      return builder.result();
    }
    catch (IOException exc)
    {
      return exc.getMessage();
    }
  }

  protected final JsonBuilder _builder;
  protected String _currentNamespace = "";
  protected final Set<String> _alreadyDumped = new HashSet<String>();

  public SchemaToJsonEncoder(JsonBuilder builder)
  {
    this._builder = builder;
  }

  /**
   * Set the current namespace.
   *
   * @param namespace to set as the current namespace.
   */
  public void setCurrentNamespace(String namespace)
  {
    _currentNamespace = namespace;
  }

  /**
   * Return the current namespace.
   *
   * @return the current namespace.
   */
  public String getCurrentNamespace()
  {
    return _currentNamespace;
  }

  /**
   * Encode the specified {@link DataSchema}.
   * @param schema to encode.
   * @throws IOException if there is an error while encoding.
   */
  public void encode(DataSchema schema) throws IOException
  {
    if (schema.isPrimitive())
    {
      _builder.writeString(schema.getUnionMemberKey());
    }
    else if (schema instanceof NamedDataSchema)
    {
      encodeNamed((NamedDataSchema) schema);
    }
    else
    {
      encodeUnnamed(schema);
    }
  }

  /**
   * Encode the specified un-named {@link DataSchema}.
   *
   * Un-named {@link DataSchema}'s are the {@link DataSchema}'s for the
   * map, array, and union types.
   *
   * @param schema to encode.
   * @throws IOException if there is an error while encoding.
   */
  protected void encodeUnnamed(DataSchema schema) throws IOException
  {
    DataSchema.Type type = schema.getType();
    switch (type)
    {
      case ARRAY:
        _builder.writeStartObject();
        _builder.writeStringField(TYPE_KEY, ARRAY_TYPE, true);
        _builder.writeFieldName(ITEMS_KEY);
        encode(((ArrayDataSchema) schema).getItems());
        encodeProperties(schema);
        _builder.writeEndObject();
        break;
      case MAP:
        _builder.writeStartObject();
        _builder.writeStringField(TYPE_KEY, MAP_TYPE, true);
        _builder.writeFieldName(VALUES_KEY);
        encode(((MapDataSchema) schema).getValues());
        encodeProperties(schema);
        _builder.writeEndObject();
        break;
      case UNION:
        _builder.writeStartArray();
        for (DataSchema memberSchema : ((UnionDataSchema) schema).getTypes())
        {
          encode(memberSchema);
        }
        _builder.writeEndArray();
        break;
      default:
        throw new IllegalStateException("schema type " + schema.getType() + " is not a known unnamed DataSchema type");
    }
  }

  /**
   * Encode a {@link NamedDataSchema}.
   *
   * A {@link NamedDataSchema}'s are the {@link DataSchema}'s for the
   * typeref, enum, fixed, and record types.
   *
   * @param schema to encode.
   * @throws IOException if there is an error while encoding.
   */
  protected void encodeNamed(NamedDataSchema schema) throws IOException
  {
    if (_alreadyDumped.contains(schema.getFullName()))
    {
      writeSchemaName(schema);
      return;
    }

    String saveCurrentNamespace = _currentNamespace;

    _builder.writeStartObject();
    _builder.writeStringField(TYPE_KEY, schema.getType().toString().toLowerCase(), true);
    encodeName(NAME_KEY, schema);
    _builder.writeStringField(DOC_KEY, schema.getDoc(), false);

    switch(schema.getType())
    {
      case TYPEREF:
        _builder.writeFieldName(REF_KEY);
        encode(((TyperefDataSchema) schema).getRef());
        break;
      case ENUM:
        _builder.writeStringArrayField(SYMBOLS_KEY, ((EnumDataSchema) schema).getSymbols(), true);
        break;
      case FIXED:
        _builder.writeIntField(SIZE_KEY, ((FixedDataSchema) schema).getSize());
        break;
      case RECORD:
        RecordDataSchema recordDataSchema = (RecordDataSchema) schema;
        if (isEncodeInclude() && recordDataSchema.getInclude().isEmpty() == false)
        {
          _builder.writeFieldName(INCLUDE_KEY);
          _builder.writeStartArray();
          for (NamedDataSchema includedSchema : recordDataSchema.getInclude())
          {
            encode(includedSchema);
          }
          _builder.writeEndArray();
        }
        _builder.writeFieldName(FIELDS_KEY);
        encodeFields(recordDataSchema);
        break;
      default:
        throw new IllegalStateException("schema type " + schema.getType() + " is not a known NamedDataSchema type");
    }

    encodeProperties(schema);
    List<String> aliases = new ArrayList<String>();
    for (Name name : schema.getAliases())
    {
      aliases.add(name.getFullName());
    }
    _builder.writeStringArrayField(ALIASES_KEY, aliases, false);
    _builder.writeEndObject();

    _currentNamespace = saveCurrentNamespace;
  }

  protected void writeSchemaName(NamedDataSchema schema) throws IOException
  {
    _builder.writeString(_currentNamespace.equals(schema.getNamespace()) ? schema.getName() : schema.getFullName());
  }

  /**
   * Encode the properties of the {@link DataSchema}
   *
   * @param schema the {@link DataSchema} being encoded.
   * @throws IOException if there is an error while encoding.
   */
  protected void encodeProperties(DataSchema schema) throws IOException
  {
    _builder.writeProperties(schema.getProperties());
  }

  /**
   * Encode a the fields of a {@link RecordDataSchema}.
   *
   * This method does not output a key. The key should be emitted before calling this method.
   * If {@link #isEncodeInclude()} returns true, then only fields that are defined in the record being
   * encoded will be encoded, else all fields including those from included records will be encoded.
   *
   * @param recordDataSchema the {@link RecordDataSchema} being encoded.
   * @throws IOException if there is an error while encoding.
   */
  protected void encodeFields(RecordDataSchema recordDataSchema) throws IOException
  {
    Collection<RecordDataSchema.Field> fields = recordDataSchema.getFields();

    _builder.writeStartArray();

    boolean encodeInclude = isEncodeInclude();
    for (RecordDataSchema.Field field : fields)
    {
      if (encodeInclude == false || recordDataSchema == field.getRecord())
      {
        encodeField(field);
      }
    }

    _builder.writeEndArray();
  }

  /**
   * Encode a field.
   *
   * @param field to encode.
   * @throws IOException if there is an error while encoding.
   */
  protected void encodeField(RecordDataSchema.Field field) throws IOException
  {
    _builder.writeStartObject();

    // name
    _builder.writeStringField(NAME_KEY, field.getName(), true);

    // type
    encodeFieldType(field);

    // doc
    _builder.writeStringField(DOC_KEY, field.getDoc(), false);

    // default
    encodeFieldDefault(field);

    // optional
    encodeFieldOptional(field);

    // order
    RecordDataSchema.Field.Order order = field.getOrder();
    if (order != RecordDataSchema.Field.Order.ASCENDING)
    {
      _builder.writeStringField(ORDER_KEY, order.toString().toLowerCase(), true);
    }

    // properties
    _builder.writeProperties(field.getProperties());

    // aliases
    _builder.writeStringArrayField(ALIASES_KEY, field.getAliases(), false);

    // done
    _builder.writeEndObject();
  }

  /**
   * Encode a field's type (i.e. {@link DataSchema}.
   *
   * @param field providing the type to encode.
   * @throws IOException if there is an error while encoding.
   */
  protected void encodeFieldType(RecordDataSchema.Field field) throws IOException
  {
    _builder.writeFieldName(TYPE_KEY);
    DataSchema fieldSchema = field.getType();
    encode(fieldSchema);
  }

  /**
   * Encode a field's default value.
   *
   * @param field providing the default value to encode.
   * @throws IOException if there is an error while encoding.
   */
  protected void encodeFieldDefault(RecordDataSchema.Field field) throws IOException
  {
    if (field.getDefault() != null)
    {
      _builder.writeFieldName(DEFAULT_KEY);
      _builder.writeData(field.getDefault());
    }
  }

  /**
   * Encode a field's optional flag.
   *
   * @param field providing the optional flag to encode.
   * @throws IOException if there is an error while encoding.
   */
  protected void encodeFieldOptional(RecordDataSchema.Field field) throws IOException
  {
    boolean optional = field.getOptional();
    if (optional)
    {
      _builder.writeBooleanField(OPTIONAL_KEY, optional);
    }
  }

  /**
   * Encode a {@link Named}.
   *
   * This method writes the unqualified name and namespace fields.
   * The namespace field will not be written if the namespace
   * is the same as the current namespace.
   *
   * It also adds the fully qualified name to the set of names already dumped
   * and updates the current namespace.
   *
   * @param nameKey provides the key used for the name.
   * @param schema provides the {@link NamedDataSchema}.
   * @throws IOException if there is an error while encoding.
   */
  protected void encodeName(String nameKey, Named schema) throws IOException
  {
    String fullName = schema.getFullName();
    if (fullName.isEmpty() == false)
    {
      String namespace = schema.getNamespace();
      _alreadyDumped.add(fullName);
      _builder.writeStringField(nameKey, schema.getName(), true);
      if (_currentNamespace.equals(namespace) == false)
      {
        _builder.writeStringField(NAMESPACE_KEY, namespace, true);
      }
      _currentNamespace = namespace;
    }
  }

  /**
   * Whether to encode the "include" attribute and not encode the included fields.
   *
   * If enabled, the "include" attribute of a record will be encoded and
   * fields defined in included records will not be encoded.
   */
  protected boolean isEncodeInclude()
  {
    return true;
  }
}
