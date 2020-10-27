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

package com.linkedin.data.schema.validation;


import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.Null;
import com.linkedin.data.element.DataElement;
import com.linkedin.data.element.DataElementUtil;
import com.linkedin.data.element.MutableDataElement;
import com.linkedin.data.element.SimpleDataElement;
import com.linkedin.data.it.IterationOrder;
import com.linkedin.data.it.ObjectIterator;
import com.linkedin.data.message.Message;
import com.linkedin.data.message.MessageList;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.BytesDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaConstants;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.FixedDataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import com.linkedin.data.schema.validator.Validator;
import com.linkedin.data.schema.validator.ValidatorContext;
import com.linkedin.data.template.DataTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A utility class to validate and fixup data objects according to the objects' {@link DataSchema}.
 */
public final class ValidateDataAgainstSchema
{
  // Private as the class is not to be instantiated.
  private ValidateDataAgainstSchema()
  {
  }

  @SuppressWarnings("serial")
  private static final HashMap<DataSchema.Type, Class<?>> _primitiveTypeToClassMap = new HashMap<DataSchema.Type, Class<?>>()
  {
    {
      put(DataSchema.Type.INT, Integer.class);
      put(DataSchema.Type.LONG, Long.class);
      put(DataSchema.Type.FLOAT, Float.class);
      put(DataSchema.Type.DOUBLE, Double.class);
      put(DataSchema.Type.STRING, String.class);
      put(DataSchema.Type.BOOLEAN, Boolean.class);
      put(DataSchema.Type.NULL, Null.class);
    }
  };

  public static ValidationResult validate(DataTemplate<?> dataTemplate, ValidationOptions options)
  {
    return validate(dataTemplate, options, null);
  }

  public static ValidationResult validate(DataTemplate<?> dataTemplate, ValidationOptions options, Validator validator)
  {
    if(dataTemplate.schema() == null)
    {
      return new State(options, validator); // return an empty validation response if no schema is available
    }

    return validate(dataTemplate.data(), dataTemplate.schema(), options, validator);
  }

  public static ValidationResult validate(Object object, DataSchema schema, ValidationOptions options)
  {
    return validate(object, schema, options, null);
  }

  public static ValidationResult validate(Object object, DataSchema schema, ValidationOptions options, Validator validator)
  {
    return validate(new SimpleDataElement(object, schema), options, validator);
  }

  public static ValidationResult validate(DataElement element, ValidationOptions options)
  {
    return validate(element, options, null);
  }

  public static ValidationResult validate(DataElement element, ValidationOptions options, Validator validator)
  {
    State state = new State(options, validator);
    state.validate(element);
    return state;
  }

  private static class State implements ValidationResult
  {
    private boolean _recursive;
    private final ValidationOptions _options;
    private final Validator _validator;
    private boolean _hasFix = false;
    private boolean _hasFixupReadOnlyError = false;
    private Object _fixed = null;
    private boolean _valid = true;
    private final Context _context;
    private List<FieldToTrim> _toTrim = new ArrayList<FieldToTrim>(0);

    private State(ValidationOptions options, Validator validator)
    {
      _options = options;
      _validator = validator;
      _context = (validator == null ? null : new Context());
    }

    protected void validate(DataElement element)
    {
      if (_options.isAvroUnionMode())
      {
        validateRecursive(element);
      }
      else
      {
        validateIterative(element);
      }

      if (_toTrim.size() > 0)
      {
        for (FieldToTrim fieldToTrim : _toTrim)
        {
          fieldToTrim.trim();
        }
      }
    }

    protected void validateRecursive(DataElement element)
    {
      _recursive = true;
      _fixed = validate(element, element.getSchema(), element.getValue());
    }

    protected void validateIterative(DataElement element)
    {
      _recursive = false;
      _fixed = element.getValue();
      UnrecognizedFieldMode unrecognizedFieldMode = _options.getUnrecognizedFieldMode();
      ObjectIterator it = new ObjectIterator(element, IterationOrder.POST_ORDER);
      DataElement nextElement = null;
      while (true)
      {
        try
        {
          if ((nextElement = it.next()) == null)
          {
            break;
          }
        }
        catch (IllegalArgumentException e)
        {
          addMessage(nextElement, e.getMessage());
          return;
        }

        DataSchema nextElementSchema = nextElement.getSchema();
        if (nextElementSchema != null)
        {
          validate(nextElement, nextElementSchema, nextElement.getValue());
        }
        else if (unrecognizedFieldMode != UnrecognizedFieldMode.IGNORE)
        {
          DataElement parentElement = nextElement.getParent();
          // We only need to trim elements where the parent type is recognized but the element
          // is not.
          if (parentElement != null && parentElement.getSchema() != null)
          {
            handleUnrecognizedField(nextElement);
          }
        }
      }
    }

    protected Object validate(DataElement element, DataSchema schema, Object object)
    {
      Object fixed;
      switch (schema.getType())
      {
        case ARRAY:
          fixed = validateArray(element, (ArrayDataSchema) schema, object);
          break;
        case BYTES:
          fixed = validateBytes(element, (BytesDataSchema) schema, object);
          break;
        case ENUM:
          fixed = validateEnum(element, (EnumDataSchema) schema, object);
          break;
        case FIXED:
          fixed = validateFixed(element, (FixedDataSchema) schema, object);
          break;
        case MAP:
          fixed = validateMap(element, (MapDataSchema) schema, object);
          break;
        case RECORD:
          fixed = validateRecord(element, (RecordDataSchema) schema, object);
          break;
        case TYPEREF:
          fixed = validateTyperef(element, (TyperefDataSchema) schema, object);
          break;
        case UNION:
          fixed = validateUnion(element, (UnionDataSchema) schema, object);
          break;
        default:
          fixed = validatePrimitive(element, schema, object);
          break;
      }
      if (fixed != object)
      {
        fixValue(element, fixed);
      }
      if (_validator != null && element.getSchema() == schema)
      {
        DataElement validatorElement;
        if (fixed == object)
        {
          validatorElement = element;
        }
        else if (element instanceof MutableDataElement)
        {
          ((MutableDataElement) element).setValue(fixed);
          validatorElement = element;
        }
        else
        {
           validatorElement = new SimpleDataElement(fixed, element.getName(), schema, element.getParent());
        }
        _context._dataElement = validatorElement;
        _validator.validate(_context);
      }
      return fixed;
    }

    protected void fixValue(DataElement element, Object fixed)
    {
      assert(_options.getCoercionMode() != CoercionMode.OFF);
      _hasFix = true;
      DataElement parentElement = element.getParent();
      if (parentElement == null)
      {
        _fixed = fixed;
      }
      else
      {
        Object parent = parentElement.getValue();
        if (parent.getClass() == DataMap.class)
        {
          DataMap map = (DataMap) parent;
          if (map.isReadOnly())
          {
            _hasFixupReadOnlyError = true;
            addMessage(element, "cannot be fixed because DataMap backing %1$s type is read-only", parentElement.getSchema().getUnionMemberKey());
          }
          else
          {
            map.put((String) element.getName(), fixed);
          }
        }
        else if (parent.getClass() == DataList.class)
        {
          DataList list = (DataList) parent;
          if (list.isReadOnly())
          {
            _hasFixupReadOnlyError = true;
            addMessage(element, "cannot be fixed because DataList backing an array type is read-only");
          }
          else
          {
            list.set((Integer) element.getName(), fixed);
          }
        }
      }
    }

    protected Object validateTyperef(DataElement element, TyperefDataSchema schema, Object object)
    {
      return validate(element, schema.getRef(), object);
    }

    protected void recurseRecord(DataElement element, RecordDataSchema schema, DataMap map)
    {
      MutableDataElement childElement = new MutableDataElement(element);
      for (Map.Entry<String, Object> entry : map.entrySet())
      {
        String key = entry.getKey();
        RecordDataSchema.Field field = schema.getField(key);
        Object value = entry.getValue();
        if (field != null)
        {
          DataSchema childSchema = field.getType();
          childElement.setValueNameSchema(value, key, childSchema);
          validate(childElement, childSchema, value);
        }
        else
        {
          childElement.setValueNameSchema(value, key, null);
          handleUnrecognizedField(childElement);
        }
      }
    }

    /**
     * Holds a the element of a {@link com.linkedin.data.DataMap} field for later removal.
     *
     * This is a specialized version of {@link com.linkedin.data.it.Remover.ToRemove} which is and
     * should remain private, so could not be used here.
     */
    private static class FieldToTrim
    {
      private FieldToTrim(DataMap parent, String fieldName)
      {
        assert(!parent.isReadOnly());
        _parent = parent;
        _fieldName = fieldName;
      }

      private void trim()
      {
        if (_parent.isReadOnly())
        {
          throw new ConcurrentModificationException("Map marked as read-only during validation.");
        }
        else
        {
          _parent.remove(_fieldName);
        }
      }

      private DataMap _parent;
      private String _fieldName;
    }

    private void trimUnrecognizedField(DataElement element)
    {
      DataElement parentElement = element.getParent();
      Object parent = parentElement.getValue();
      if (parent != null)
      {
        if (parent.getClass() == DataMap.class)
        {
          DataMap map = (DataMap) parent;
          Object name = element.getName();
          assert (name instanceof String);
          String fieldName = (String) name;

          if (map.isReadOnly())
          {
            _hasFixupReadOnlyError = true;
            addMessage(element, "unrecognized field cannot be trimmed because DataMap backing it is read-only");
          }
          else
          {
            _toTrim.add(new FieldToTrim(map, fieldName));
          }
        }
      }
    }

    private void handleUnrecognizedField(DataElement element)
    {
      switch (_options.getUnrecognizedFieldMode())
      {
        case IGNORE:
          break;
        case DISALLOW:
          addMessage(element, "unrecognized field found but not allowed");
          break;
        case TRIM:
          trimUnrecognizedField(element);
          break;
      }
    }

    private boolean isFieldOptional(RecordDataSchema.Field field, DataElement element)
    {
      if (field.getOptional())
      {
        return true;
      }
      return _options.getTreatOptional().evaluate(new SimpleDataElement(null, field.getName(), field.getType(), element));
    }

    protected Object validateRecord(DataElement element, RecordDataSchema schema, Object object)
    {
      if (object instanceof DataMap)
      {
        DataMap map = (DataMap) object;
        if (_recursive)
        {
          recurseRecord(element, schema, map);
        }
        RequiredMode requiredMode = _options.getRequiredMode();
        if (requiredMode != RequiredMode.IGNORE)
        {
          for (RecordDataSchema.Field field : schema.getFields())
          {
            if (isFieldOptional(field, element) == false && map.containsKey(field.getName()) == false)
            {
              switch (requiredMode)
              {
                case MUST_BE_PRESENT:
                  addIsRequiredMessage(
                    element, field,
                    "field is required but not found"
                  );
                  break;
                case CAN_BE_ABSENT_IF_HAS_DEFAULT:
                  if (field.getDefault() == null)
                  {
                    addIsRequiredMessage(
                      element, field,
                      "field is required but not found and has no default value"
                    );
                  }
                  break;
                case FIXUP_ABSENT_WITH_DEFAULT:
                  Object defaultValue = field.getDefault();
                  if (defaultValue == null)
                  {
                    addIsRequiredMessage(
                      element, field,
                      "field is required but not found and has no default value"
                    );
                  }
                  else if (map.isReadOnly())
                  {
                    _hasFix = true;
                    _hasFixupReadOnlyError = true;
                    addIsRequiredMessage(
                      element, field,
                      "field is required and has default value but not found and cannot be fixed because DataMap of record is read-only"
                    );
                  }
                  else
                  {
                    _hasFix = true;
                    map.put(field.getName(), defaultValue);
                  }
                  break;
              }
            }
          }
        }
      }
      else
      {
        addMessage(element, "record type is not backed by a DataMap");
      }
      return object;
    }

    protected Object validateUnion(DataElement element, UnionDataSchema schema, Object object)
    {
      if (object == Data.NULL)
      {
        if (schema.getTypeByMemberKey(DataSchemaConstants.NULL_TYPE) == null)
        {
          addMessage(element, "null is not a member type of union %1$s", schema);
        }
      }
      else if (_options.isAvroUnionMode())
      {
        // Avro union default value does not include member type discriminator
        List<UnionDataSchema.Member> members = schema.getMembers();
        if (members.isEmpty())
        {
          addMessage(element, "value %1$s is not valid for empty union", object.toString());
        }
        else
        {
          DataSchema memberSchema = members.get(0).getType();
          assert(_recursive);
          validate(element, memberSchema, object);
        }
      }
      else if (object instanceof DataMap)
      {
        // Pegasus mode
        DataMap map = (DataMap) object;
        if (map.size() != 1)
        {
          addMessage(element, "DataMap should have exactly one entry for a union type");
        }
        else
        {
          Map.Entry<String, Object> entry = map.entrySet().iterator().next();
          String key = entry.getKey();
          DataSchema memberSchema = schema.getTypeByMemberKey(key);
          if (memberSchema == null)
          {
            addMessage(element, "\"%1$s\" is not a member type of union %2$s", key, schema);
          }
          else if (_recursive)
          {
            Object value = entry.getValue();
            MutableDataElement memberElement = new MutableDataElement(value, key, memberSchema, element);
            validate(memberElement, memberSchema, value);
          }
        }
      }
      else
      {
        addMessage(element, "union type is not backed by a DataMap or null");
      }
      return object;
    }

    protected Object validateEnum(DataElement element, EnumDataSchema schema, Object object)
    {
      Object fixed = object;
      if (object instanceof String)
      {
        String value = (String) object;
        if (schema.contains(value) == false)
        {
          addMessage(element, "\"%1$s\" is not an enum symbol", value);
        }
      }
      else
      {
        addMessage(element, "enum type is not backed by a String");
      }
      return fixed;
    }

    protected void recurseArray(DataElement element, ArrayDataSchema schema, Object object)
    {
      DataList list = (DataList) object;
      DataSchema childSchema = schema.getItems();
      int index = 0;
      MutableDataElement childElement = new MutableDataElement(element);
      for (Object value : list)
      {
        childElement.setValueNameSchema(value, index, childSchema);
        validate(childElement, childSchema, value);
        index++;
      }
    }

    protected Object validateArray(DataElement element, ArrayDataSchema schema, Object object)
    {
      if (object instanceof DataList)
      {
        if (_recursive)
        {
          recurseArray(element, schema, object);
        }
      }
      else
      {
        addMessage(element, "array type is not backed by a DataList");
      }
      return object;
    }

    protected void recurseMap(DataElement element, MapDataSchema schema, Object object)
    {
      DataMap map = (DataMap) object;
      DataSchema childSchema = schema.getValues();
      MutableDataElement childElement = new MutableDataElement(element);
      for (Map.Entry<String, Object> entry : map.entrySet())
      {
        String key = entry.getKey();
        Object value = entry.getValue();
        childElement.setValueNameSchema(value, key, childSchema);
        validate(childElement, childSchema, value);
      }
    }

    protected Object validateMap(DataElement element, MapDataSchema schema, Object object)
    {
      if (object instanceof DataMap)
      {
        if (_recursive)
        {
          recurseMap(element, schema, object);
        }
      }
      else
      {
        addMessage(element, "map type is not backed by a DataMap");
      }
      return object;
    }

    protected Object validateFixed(DataElement element, FixedDataSchema schema, Object object)
    {
      Object fixed = object;
      Class<?> clazz = object.getClass();
      int size = schema.getSize();
      if (clazz == String.class)
      {
        String str = (String) object;
        boolean error = false;
        if (str.length() != size)
        {
          addMessage(element,
                     "\"%1$s\" length (%2$d) is inconsistent with expected fixed size of %3$d",
                     str,
                     str.length(),
                     size);
        }
        else
        {
          if (_options.getCoercionMode() != CoercionMode.OFF)
          {
            ByteString bytes = ByteString.copyAvroString(str, true);
            if (bytes != null)
            {
              _hasFix = true;
              fixed = bytes;
            }
            else
            {
              error = true;
            }
          }
          else
          {
            error = ! Data.validStringAsBytes(str);
          }
        }
        if (error)
        {
          addMessage(element, "\"%1$s\" is not a valid string representation of bytes", str);
        }
      }
      else if (clazz == ByteString.class)
      {
        ByteString bytes = (ByteString) object;
        if (bytes.length() != size)
        {
          addMessage(element, "\"%1$s\" length (%2$d) is inconsistent with expected fixed size of %3$d", bytes, bytes.length(), size);
        }
      }
      else
      {
        addMessage(element, "fixed type is not backed by a String or ByteString");
      }
      return fixed;
    }

    protected Object validateBytes(DataElement element, BytesDataSchema schema, Object object)
    {
      Object fixed = object;
      Class<?> clazz = object.getClass();
      if (clazz == String.class)
      {
        String str = (String) object;
        boolean error = false;
        if (_options.getCoercionMode() != CoercionMode.OFF)
        {
          ByteString bytes = ByteString.copyAvroString(str, true);
          if (bytes != null)
          {
            _hasFix = true;
            fixed = bytes;
          }
          else
          {
            error = true;
          }
        }
        else
        {
          error = ! Data.validStringAsBytes(str);
        }
        if (error)
        {
          addMessage(element, "\"%1$s\" is not a valid string representation of bytes", str);
        }
      }
      else if (clazz != ByteString.class)
      {
        addMessage(element, "bytes type is not backed by a String or ByteString");
      }
      return fixed;
    }

    protected Object validatePrimitive(DataElement element, DataSchema schema, Object object)
    {
      Class<?> primitiveClass = _primitiveTypeToClassMap.get(schema.getType());

      Object fixed = object;
      if (object.getClass() != primitiveClass)
      {
        if (_options.getCoercionMode() != CoercionMode.OFF)
        {
          fixed = fixupPrimitive(schema, object);
          if (fixed == object)
          {
            addMessage(element, "%1$s cannot be coerced to %2$s", String.valueOf(object), primitiveClass.getSimpleName());
          }
        }
        else
        {
          addMessage(element, "%1$s is not backed by a %2$s", String.valueOf(object), primitiveClass.getSimpleName());
        }
      }
      return fixed;
    }

    protected Object fixupPrimitive(DataSchema schema, Object object)
    {
      DataSchema.Type schemaType = schema.getType();

      try
      {
        switch (schemaType)
        {
          case INT:
            return
              (object instanceof Number) ?
                (((Number) object).intValue()) :
                (object.getClass() == String.class && _options.getCoercionMode() == CoercionMode.STRING_TO_PRIMITIVE) ?
                  (new BigDecimal((String) object)).intValue() :
                  object;
          case LONG:
            return
              (object instanceof Number) ?
                (((Number) object).longValue()) :
                (object.getClass() == String.class && _options.getCoercionMode() == CoercionMode.STRING_TO_PRIMITIVE) ?
                  (new BigDecimal((String) object)).longValue() :
                  object;
          case FLOAT:
            return
              (object instanceof Number) ?
                (((Number) object).floatValue()) :
                (object.getClass() == String.class && _options.getCoercionMode() == CoercionMode.STRING_TO_PRIMITIVE) ?
                  (new BigDecimal((String) object)).floatValue() :
                  object;
          case DOUBLE:
            return
              (object instanceof Number) ?
                (((Number) object).doubleValue()) :
                (object.getClass() == String.class && _options.getCoercionMode() == CoercionMode.STRING_TO_PRIMITIVE) ?
                  (new BigDecimal((String) object)).doubleValue() :
                  object;
          case BOOLEAN:
            if (object.getClass() == String.class && _options.getCoercionMode() == CoercionMode.STRING_TO_PRIMITIVE)
            {
              String string = (String) object;
              if ("true".equalsIgnoreCase(string))
              {
                return Boolean.TRUE;
              }
              if ("false".equalsIgnoreCase(string))
              {
                return Boolean.FALSE;
              }
            }
            return object;
          case STRING:
          case NULL:
          default:
            return object;
        }
      }
      catch (NumberFormatException exc)
      {
        return object;
      }
    }

    protected void addMessage(DataElement element, String format, Object... args)
    {
      _messages.add(new Message(element.path(), format, args));
      _valid = false;
    }

    protected void addIsRequiredMessage(DataElement element, RecordDataSchema.Field field, String msg)
    {
      _messages.add(new Message(element.path(field.getName()), msg));
      _valid = false;
    }

    private MessageList<Message> _messages = new MessageList<Message>();

    @Override
    public boolean hasFix()
    {
      return _hasFix;
    }

    @Override
    public boolean hasFixupReadOnlyError()
    {
      return _hasFixupReadOnlyError;
    }

    @Override
    public Object getFixed()
    {
      return _fixed;
    }

    @Override
    public boolean isValid()
    {
      return _valid;
    }

    @Override
    public Collection<Message> getMessages()
    {
      return Collections.unmodifiableList(_messages);
    }

    @Override
    public String toString()
    {
      StringBuilder sb = new StringBuilder();
      sb.append("hasFix=").append(_hasFix)
        .append(", hasFixupReadOnlyError=").append(_hasFixupReadOnlyError)
        .append(", valid=").append(_valid).append("\n")
        .append("fixed=").append(_fixed).append("\n");
      _messages.appendTo(sb);
      return sb.toString();
    }

    private class Context implements ValidatorContext
    {
      private DataElement _dataElement;

      @Override
      public DataElement dataElement()
      {
        return _dataElement;
      }

      @Override
      public void addResult(Message message)
      {
        _messages.add(message);
        if (message.isError())
        {
          _valid = false;
        }
      }

      @Override
      public void setHasFix(boolean value)
      {
        _hasFix = value;
      }

      @Override
      public void setHasFixupReadOnlyError(boolean value)
      {
        _hasFixupReadOnlyError = value;
      }

      @Override
      public ValidationOptions validationOptions()
      {
        return _options;
      }
    }
  }
}
