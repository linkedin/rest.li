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

package com.linkedin.data.schema.compatibility;

import com.linkedin.data.DataMap;
import com.linkedin.data.message.MessageList;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaConstants;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.FixedDataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.PrimitiveDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import com.linkedin.data.schema.validator.DataSchemaAnnotationValidator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Compare two {@link com.linkedin.data.schema.DataSchema} for compatibility.
 */
public class CompatibilityChecker
{
  public static CompatibilityResult checkCompatibility(DataSchema older, DataSchema newer, CompatibilityOptions options)
  {
    CompatibilityChecker checker = new CompatibilityChecker();
    checker.run(older, newer, options);
    return checker._result;
  }

  private static class Checked
  {
    private Checked(DataSchema older, DataSchema newer)
    {
      _older = older;
      _newer = newer;
    }

    @Override
    public boolean equals(Object o)
    {
      if (o == null)
        return false;
      Checked other = (Checked) o;
      return (other._older == _older && other._newer == _newer);
    }

    @Override
    public int hashCode()
    {
      return _older.hashCode() + _newer.hashCode();
    }

    private final DataSchema _older;
    private final DataSchema _newer;
  }

  private final ArrayDeque<String> _path = new ArrayDeque<>();
  private final HashSet<Checked> _checked = new HashSet<>();
  private Result _result;
  private CompatibilityOptions _options;

  private CompatibilityChecker()
  {
  }

  private CompatibilityResult run(DataSchema older, DataSchema newer, CompatibilityOptions options)
  {
    _path.clear();
    _checked.clear();
    _options = options;
    _result = new Result();
    check(older, newer);
    return _result;
  }

  private void check(DataSchema older, DataSchema newer)
  {
    Checked toCheck = new Checked(older, newer);
    if (_checked.contains(toCheck))
    {
      return;
    }
    _checked.add(toCheck);

    if (older == newer)
    {
      return;
    }

    int pathCount = 1;
    if (_options.getMode() == CompatibilityOptions.Mode.DATA || _options.getMode() == CompatibilityOptions.Mode.EXTENSION )
    {
      older = older.getDereferencedDataSchema();
      while (newer.getType() == DataSchema.Type.TYPEREF)
      {
        TyperefDataSchema typerefDataSchema = ((TyperefDataSchema) newer);
        _path.addLast(typerefDataSchema.getFullName());
        _path.addLast(DataSchemaConstants.REF_KEY);
        pathCount++;
        newer = typerefDataSchema.getRef();
      }
    }
    if (newer.getType() == DataSchema.Type.TYPEREF)
    {
      _path.addLast(((TyperefDataSchema) newer).getFullName());
    }
    else
    {
      _path.addLast(newer.getUnionMemberKey());
    }

    switch (newer.getType())
    {
      case TYPEREF:
        if (isSameType(older, newer))
         checkTyperef((TyperefDataSchema) older, (TyperefDataSchema) newer);
        break;
      case RECORD:
        if (isSameType(older, newer))
          checkRecord((RecordDataSchema) older, (RecordDataSchema) newer);
        break;
      case ARRAY:
        if (isSameType(older, newer))
          checkArray((ArrayDataSchema) older, (ArrayDataSchema) newer);
        break;
      case MAP:
        if (isSameType(older, newer))
          checkMap((MapDataSchema) older, (MapDataSchema) newer);
        break;
      case ENUM:
        if (isSameType(older, newer))
          checkEnum((EnumDataSchema) older, (EnumDataSchema) newer);
        break;
      case FIXED:
        if (isSameType(older, newer))
          checkFixed((FixedDataSchema) older, (FixedDataSchema) newer);
        break;
      case UNION:
        if (isSameType(older, newer))
          checkUnion((UnionDataSchema) older, (UnionDataSchema) newer);
        break;
      default:
        if (newer instanceof PrimitiveDataSchema)
          checkPrimitive(older, newer);
        else
          throw new IllegalStateException("Unknown schema type " + newer.getType() +
                                          ", checking old schema " + older +
                                          ", new schema " + newer);
        break;
    }

    for (;pathCount > 0; pathCount--)
    {
      _path.removeLast();
    }
    return;
  }

  private void appendTypeChangedMessage(DataSchema.Type olderType, DataSchema.Type newerType)
  {
    appendMessage(CompatibilityMessage.Impact.BREAKS_NEW_AND_OLD_READERS,
                  "schema type changed from %s to %s",
                  olderType.toString().toLowerCase(),
                  newerType.toString().toLowerCase());
  }

  private boolean isSameType(DataSchema.Type olderType, DataSchema.Type newerType)
  {
    boolean isSameType = (olderType == newerType);
    if (isSameType == false)
    {
      appendTypeChangedMessage(olderType, newerType);
    }
    return isSameType;
  }

  private boolean isSameType(DataSchema older, DataSchema newer)
  {
    DataSchema.Type olderType = older.getType();
    DataSchema.Type newerType = newer.getType();
    return isSameType(olderType, newerType);
  }

  private void checkPrimitive(DataSchema older, DataSchema newer)
  {
    DataSchema.Type newerType = newer.getType();
    switch (newerType)
    {
      case LONG:
        checkAllowedOlderTypes(older.getType(), newerType, DataSchema.Type.INT);
        break;
      case FLOAT:
        checkAllowedOlderTypes(older.getType(), newerType, DataSchema.Type.INT, DataSchema.Type.LONG);
        break;
      case DOUBLE:
        checkAllowedOlderTypes(older.getType(), newerType,
                               DataSchema.Type.INT,
                               DataSchema.Type.LONG,
                               DataSchema.Type.FLOAT);
        break;
      default:
        isSameType(older, newer);
        break;
    }
  }

  private void checkAllowedOlderTypes(DataSchema.Type olderType,
                                      DataSchema.Type newerType,
                                      DataSchema.Type... allowedOlderTypes)
  {
    if (_options.isAllowPromotions())
    {
      if (olderType != newerType)
      {
        boolean allowed = false;
        for (DataSchema.Type type : allowedOlderTypes)
        {
          if (type == olderType)
          {
            allowed = true;
            break;
          }
        }
        if (allowed)
        {
          appendMessage(CompatibilityMessage.Impact.VALUES_MAY_BE_TRUNCATED_OR_OVERFLOW,
                        "numeric type promoted from %s to %s",
                        olderType.toString().toLowerCase(),
                        newerType.toString().toLowerCase());
        }
        else
        {
          appendTypeChangedMessage(olderType, newerType);
        }
      }
    }
    else
    {
      isSameType(olderType, newerType);
    }
  }

  private static enum FieldModifier
  {
    OPTIONAL,
    REQUIRED,
    REQUIRED_WITH_DEFAULT
  }

  private static FieldModifier toFieldModifier(RecordDataSchema.Field field)
  {
    if (field.getOptional())
    {
      return FieldModifier.OPTIONAL;
    }
    else
    {
      if (field.getDefault() != null)
      {
        return FieldModifier.REQUIRED_WITH_DEFAULT;
      }
      else
      {
        return FieldModifier.REQUIRED;
      }
    }
  }

  private void checkRecord(RecordDataSchema older, RecordDataSchema newer)
  {
    checkName(older, newer);

    List<RecordDataSchema.Field> commonFields = new ArrayList<>(newer.getFields().size());
    List<String> newerRequiredAdded = new CheckerArrayList<>();
    List<String> newerRequiredWithDefaultAdded = new CheckerArrayList<>();
    List<String> newerOptionalAdded = new CheckerArrayList<>();
    List<String> requiredToOptional = new CheckerArrayList<>();
    List<String> requiredWithDefaultToOptional = new CheckerArrayList<>();
    List<String> optionalToRequired = new CheckerArrayList<>();
    List<String> optionalToRequiredWithDefault = new CheckerArrayList<>();
    List<String> newerRequiredRemoved = new CheckerArrayList<>();
    List<String> newerOptionalRemoved = new CheckerArrayList<>();
    List<String> requiredWithDefaultToRequired = new CheckerArrayList<>();
    List<String> requiredToRequiredWithDefault = new CheckerArrayList<>();

    for (RecordDataSchema.Field newerField : newer.getFields())
    {
      String fieldName = newerField.getName();
      RecordDataSchema.Field olderField = older.getField(fieldName);

      FieldModifier newerFieldModifier = toFieldModifier(newerField);

      if (olderField == null)
      {
        if (newerFieldModifier == FieldModifier.OPTIONAL)
        {
          newerOptionalAdded.add(fieldName);
        }
        // Required fields with defaults are considered compatible and are not added to newerRequiredAdded
        else if (newerFieldModifier == FieldModifier.REQUIRED)
        {
          newerRequiredAdded.add(fieldName);
        }
        else if (newerFieldModifier == FieldModifier.REQUIRED_WITH_DEFAULT)
        {
          newerRequiredWithDefaultAdded.add(fieldName);
        }
      }
      else
      {
        checkFieldValidators(olderField, newerField);

        FieldModifier olderFieldModifier = toFieldModifier(olderField);

        commonFields.add(newerField);
        if (olderFieldModifier == FieldModifier.OPTIONAL && newerFieldModifier == FieldModifier.REQUIRED_WITH_DEFAULT) {
          optionalToRequiredWithDefault.add(fieldName);
        }
        else if (olderFieldModifier == FieldModifier.OPTIONAL && newerFieldModifier == FieldModifier.REQUIRED) {
          optionalToRequired.add(fieldName);
        }
        else if (olderFieldModifier == FieldModifier.REQUIRED && newerFieldModifier == FieldModifier.OPTIONAL)
        {
          requiredToOptional.add(fieldName);
        }
        else if (olderFieldModifier == FieldModifier.REQUIRED && newerFieldModifier == FieldModifier.REQUIRED_WITH_DEFAULT)
        {
          requiredToRequiredWithDefault.add(fieldName);
        }
        else if (olderFieldModifier == FieldModifier.REQUIRED_WITH_DEFAULT && newerFieldModifier == FieldModifier.OPTIONAL)
        {
          requiredWithDefaultToOptional.add(fieldName);
        }
        else if (olderFieldModifier == FieldModifier.REQUIRED_WITH_DEFAULT && newerFieldModifier == FieldModifier.REQUIRED)
        {
          requiredWithDefaultToRequired.add(fieldName);
        }
      }
    }
    for (RecordDataSchema.Field olderField : older.getFields())
    {
      String fieldName = olderField.getName();
      RecordDataSchema.Field newerField = newer.getField(fieldName);
      if (newerField == null)
      {
        (olderField.getOptional() ? newerOptionalRemoved : newerRequiredRemoved).add(fieldName);
      }
    }

    if (newerRequiredAdded.isEmpty() == false && _options.getMode() != CompatibilityOptions.Mode.EXTENSION)
    {
      appendMessage(CompatibilityMessage.Impact.BREAKS_NEW_READER,
                    "new record added required fields %s",
                    newerRequiredAdded);
    }

    if (newerRequiredWithDefaultAdded.isEmpty() == false)
    {
      appendMessage(CompatibilityMessage.Impact.OLD_READER_IGNORES_DATA,
          "new record added required with default fields %s",
          newerRequiredAdded);
    }

    if (newerRequiredRemoved.isEmpty() == false)
    {
      appendMessage(CompatibilityMessage.Impact.BREAKS_OLD_READER,
                    "new record removed required fields %s",
                    newerRequiredRemoved);
    }

    if (optionalToRequired.isEmpty() == false)
    {
      appendMessage(CompatibilityMessage.Impact.BREAKS_NEW_READER,
                    "new record changed optional fields to required fields %s",
                    optionalToRequired);
    }

    if (optionalToRequiredWithDefault.isEmpty() == false)
    {
      appendMessage(CompatibilityMessage.Impact.BREAKS_NEW_AND_OLD_READERS,
          "new record changed optional fields to required fields with defaults %s. This change is compatible for "
          + "Pegasus but incompatible for Avro, if this record schema is never converted to Avro, this error may "
          + "safely be ignored.",
          optionalToRequiredWithDefault);
    }

    if (requiredToOptional.isEmpty() == false)
    {
      appendMessage(CompatibilityMessage.Impact.BREAKS_OLD_READER,
                    "new record changed required fields to optional fields %s",
                    requiredToOptional);
    }

    if (requiredWithDefaultToOptional.isEmpty() == false)
    {
      appendMessage(CompatibilityMessage.Impact.BREAKS_NEW_AND_OLD_READERS,
          "new record changed required fields with defaults to optional fields %s. This change is compatible for "
          + "Pegasus but incompatible for Avro, if this record schema is never converted to Avro, this error may "
          + "safely be ignored.",
          requiredWithDefaultToOptional);
    }

    if (newerOptionalAdded.isEmpty() == false)
    {
      appendMessage(CompatibilityMessage.Impact.OLD_READER_IGNORES_DATA,
                    "new record added optional fields %s",
                    newerOptionalAdded);
    }

    if (newerOptionalRemoved.isEmpty() == false)
    {
      appendMessage(CompatibilityMessage.Impact.BREAKS_NEW_AND_OLD_READERS,
                    "new record removed optional fields %s. This allows a new field to be added " +
                        "with the same name but different type in the future.",
                    newerOptionalRemoved);
    }

    if (requiredWithDefaultToRequired.isEmpty() == false)
    {
      appendMessage(CompatibilityMessage.Impact.BREAKS_NEW_READER,
          "new record removed default from required fields %s",
          requiredWithDefaultToRequired);
    }

    if (requiredToRequiredWithDefault.isEmpty() == false)
    {
      appendMessage(CompatibilityMessage.Impact.BREAKS_OLD_READER,
          "new record added default to required fields %s",
          requiredToRequiredWithDefault);
    }

    for (RecordDataSchema.Field newerField : commonFields)
    {
      String fieldName = newerField.getName();

      _path.addLast(fieldName);

      RecordDataSchema.Field olderField = older.getField(fieldName);
      assert(olderField != null);
      check(olderField.getType(), newerField.getType());

      _path.removeLast();
    }
  }

  private void computeAddedUnionMembers(UnionDataSchema base, UnionDataSchema changed,
                                        List<String> added, List<UnionDataSchema.Member> commonMembers)
  {
    for (UnionDataSchema.Member member : changed.getMembers())
    {
      String unionMemberKey = member.getUnionMemberKey();
      boolean isMemberNewlyAdded = (base.getTypeByMemberKey(unionMemberKey) == null);

      if (isMemberNewlyAdded)
      {
        added.add(unionMemberKey);
      }
      else if (commonMembers != null)
      {
        commonMembers.add(member);
      }
    }
  }

  private void checkUnion(UnionDataSchema older, UnionDataSchema newer)
  {
    // Check for any changes in member aliasing
    if (older.areMembersAliased() != newer.areMembersAliased())
    {
      appendMessage(CompatibilityMessage.Impact.BREAKS_NEW_AND_OLD_READERS,
          "new union %s member aliases",
          newer.areMembersAliased() ? "added" : "removed");
    }

    // using list to preserve union member order
    List<UnionDataSchema.Member> commonMembers = new CheckerArrayList<>(newer.getMembers().size());
    List<String> newerAdded = new CheckerArrayList<>();
    List<String> olderAdded = new CheckerArrayList<>();

    computeAddedUnionMembers(older, newer, newerAdded, commonMembers);
    computeAddedUnionMembers(newer, older, olderAdded, null);

    if (!newerAdded.isEmpty())
    {
      appendMessage(CompatibilityMessage.Impact.BREAKS_OLD_READER,
                    "new union added members %s",
                    newerAdded);
    }

    if (!olderAdded.isEmpty())
    {
      appendMessage(CompatibilityMessage.Impact.BREAKS_NEW_READER,
                    "new union removed members %s",
                    olderAdded);
    }

    for (UnionDataSchema.Member newerMember : commonMembers)
    {
      DataSchema newerSchema = newerMember.getType();
      DataSchema olderSchema = older.getTypeByMemberKey(newerMember.getUnionMemberKey());

      assert(olderSchema != null);

      if (newerMember.hasAlias())
      {
        _path.addLast(newerMember.getAlias());
      }

      check(olderSchema, newerSchema);

      if (newerMember.hasAlias())
      {
        _path.removeLast();
      }
    }
  }

  private void checkEnum(EnumDataSchema older, EnumDataSchema newer)
  {
    checkName(older, newer);

    _path.addLast(DataSchemaConstants.SYMBOLS_KEY);

    // using list to preserve symbol order
    List<String> newerOnlySymbols = new CheckerArrayList<>(newer.getSymbols());
    newerOnlySymbols.removeAll(older.getSymbols());

    List<String> olderOnlySymbols = new CheckerArrayList<>(older.getSymbols());
    olderOnlySymbols.removeAll(newer.getSymbols());

    if (newerOnlySymbols.isEmpty() == false)
    {
      appendMessage(CompatibilityMessage.Impact.BREAKS_OLD_READER,
                    "new enum added symbols %s",
                    newerOnlySymbols);
    }

    if (olderOnlySymbols.isEmpty() == false)
    {
      appendMessage(CompatibilityMessage.Impact.BREAKS_NEW_READER,
                    "new enum removed symbols %s",
                    olderOnlySymbols);
    }

    _path.removeLast();
  }

  private void checkFixed(FixedDataSchema older, FixedDataSchema newer)
  {
    checkName(older, newer);

    _path.addLast(DataSchemaConstants.SIZE_KEY);

    int olderSize = older.getSize();
    int newerSize = newer.getSize();
    if (olderSize != newerSize)
    {
      appendMessage(CompatibilityMessage.Impact.BREAKS_NEW_AND_OLD_READERS,
                    "fixed size changed from %d to %d",
                    olderSize,
                    newerSize);
    }

    _path.removeLast();
  }

  private void checkTyperef(TyperefDataSchema older, TyperefDataSchema newer)
  {
    checkName(older, newer);
    _path.addLast(DataSchemaConstants.REF_KEY);
    check(older.getDereferencedDataSchema(), newer.getDereferencedDataSchema());
    _path.removeLast();
  }

  private void checkArray(ArrayDataSchema older, ArrayDataSchema newer)
  {
    _path.addLast(DataSchemaConstants.ITEMS_KEY);
    check(older.getItems(), newer.getItems());
    _path.removeLast();
  }

  private void checkMap(MapDataSchema older, MapDataSchema newer)
  {
    _path.addLast(DataSchemaConstants.VALUES_KEY);
    check(older.getValues(), newer.getValues());
    _path.removeLast();
  }

  private void checkName(NamedDataSchema older, NamedDataSchema newer)
  {
    if (_options.isCheckNames() && older.getFullName().equals(newer.getFullName()) == false)
    {
      appendMessage(CompatibilityMessage.Impact.BREAKS_NEW_AND_OLD_READERS,
                    "name changed from %s to %s", older.getFullName(), newer.getFullName());
    }
  }

  /**
   * Checks the compatibility of the validation rules specified on some field.
   *
   * @param older older schema field
   * @param newer newer schema field
   */
  private void checkFieldValidators(RecordDataSchema.Field older, RecordDataSchema.Field newer)
  {
    final DataMap olderValidators = (DataMap) older.getProperties().getOrDefault(DataSchemaAnnotationValidator.VALIDATE, new DataMap());
    final DataMap newerValidators = (DataMap) newer.getProperties().getOrDefault(DataSchemaAnnotationValidator.VALIDATE, new DataMap());

    // Compute the union of the previous validation rules and the current validation rules
    final Set<String> validatorKeysUnion = new HashSet<>();
    validatorKeysUnion.addAll(olderValidators.keySet());
    validatorKeysUnion.addAll(newerValidators.keySet());

    // Check the compatibility of each validation rule
    for (String key : validatorKeysUnion)
    {
      if (!olderValidators.containsKey(key) && newerValidators.containsKey(key))
      {
        // Added validation rule, thus old writer may write data that a new reader doesn't expect
        appendMessage(CompatibilityMessage.Impact.BREAKS_NEW_READER, "added new validation rule \"%s\"", key);
      }
      else if (olderValidators.containsKey(key) && !newerValidators.containsKey(key))
      {
        // Removed validation rule, thus new writer may write data that an old reader doesn't expect
        appendMessage(CompatibilityMessage.Impact.BREAKS_OLD_READER, "removed old validation rule \"%s\"", key);
      }
    }
  }

  private void appendMessage(CompatibilityMessage.Impact impact, String format, Object... args)
  {
    CompatibilityMessage message = new CompatibilityMessage(_path.toArray(), impact, format, args);
    _result._messages.add(message);
  }

  private static class Result implements CompatibilityResult
  {
    private Result()
    {
      _messages = new MessageList<>();
    }

    @Override
    public Collection<CompatibilityMessage> getMessages()
    {
      return _messages;
    }

    @Override
    public boolean isError()
    {
      return _messages.isError();
    }

    private final MessageList<CompatibilityMessage> _messages;
  }

  /**
   * Override {@link #toString()} to print list without square brackets.
   *
   * @param <T> element type of list.
   */
  private static class CheckerArrayList<T> extends ArrayList<T>
  {
    private static final long serialVersionUID = 1L;

    private CheckerArrayList()
    {
      super();
    }

    private CheckerArrayList(int reserve)
    {
      super(reserve);
    }

    private CheckerArrayList(Collection<T> c)
    {
      super(c);
    }

    @Override
    public String toString()
    {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < size(); i++)
      {
        if (i != 0)
          sb.append(", ");
        sb.append(get(i));
      }
      return sb.toString();
    }
  }
}
