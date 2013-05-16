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
package com.linkedin.restli.tools.compatibility;

import com.linkedin.data.DataMap;
import com.linkedin.data.message.Message;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.compatibility.CompatibilityChecker;
import com.linkedin.data.schema.compatibility.CompatibilityMessage;
import com.linkedin.data.schema.compatibility.CompatibilityOptions;
import com.linkedin.data.schema.compatibility.CompatibilityResult;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.template.GetMode;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.StringArray;
import com.linkedin.data.template.WrappingArrayTemplate;
import com.linkedin.restli.restspec.ActionSchema;
import com.linkedin.restli.restspec.ActionsSetSchema;
import com.linkedin.restli.restspec.AssocKeySchema;
import com.linkedin.restli.restspec.AssociationSchema;
import com.linkedin.restli.restspec.CollectionSchema;
import com.linkedin.restli.restspec.EntitySchema;
import com.linkedin.restli.restspec.FinderSchema;
import com.linkedin.restli.restspec.IdentifierSchema;
import com.linkedin.restli.restspec.MetadataSchema;
import com.linkedin.restli.restspec.ParameterSchema;
import com.linkedin.restli.restspec.ParameterSchemaArray;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestMethodSchema;
import com.linkedin.restli.restspec.RestSpecCodec;
import com.linkedin.restli.tools.idlcheck.CompatibilityInfo;
import com.linkedin.restli.tools.idlcheck.CompatibilityLevel;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class ResourceCompatibilityChecker
{
  private final ResourceSchema _prevSchema;
  private final ResourceSchema _currSchema;

  private final DataSchemaResolver _prevSchemaResolver;
  private final DataSchemaResolver _currSchemaResolver;

  private boolean _checked;
  private CompatibilityInfoMap _map = new CompatibilityInfoMap();
  private Stack<Object> _path = new Stack<Object>();

  private Set<String> _namedSchemasChecked = new HashSet<String>();

  private static final CompatibilityOptions defaultOptions =
    new CompatibilityOptions().setMode(CompatibilityOptions.Mode.SCHEMA).setAllowPromotions(true);

  public ResourceCompatibilityChecker(ResourceSchema prevSchema,
                                      DataSchemaResolver prevSchemaResolver,
                                      ResourceSchema currSchema,
                                      DataSchemaResolver currSchemaResolver)
  {
    _prevSchema = prevSchema;
    _currSchema = currSchema;
    _prevSchemaResolver = prevSchemaResolver;
    _currSchemaResolver = currSchemaResolver;

    _checked = false;
    _path.push("");
  }

  public boolean check(CompatibilityLevel level)
  {
    if (!_checked) runCheck();
    return _map.isCompatible(level);

  }

  public void check()
  {
    if (!_checked) runCheck();
  }

  public CompatibilityInfoMap getMap()
  {
    return _map;
  }

  private void runCheck()
  {
    final ValidationOptions valOptions = new ValidationOptions();
    boolean valResult = validateData(_prevSchema.data(), _prevSchema.schema(), valOptions);
    valResult &= validateData(_currSchema.data(), _currSchema.schema(), valOptions);
    if (valResult)
    {
      checkResourceSchema(_prevSchema, _currSchema);
    }
    _checked = true;
  }

  private boolean validateData(DataMap object, DataSchema schema, ValidationOptions options)
  {
    final ValidationResult valResult = ValidateDataAgainstSchema.validate(object, schema, options);
    if (valResult.isValid())
    {
      return true;
    }

    final Collection<Message> valErrorMessages = valResult.getMessages();
    for (final Message message : valErrorMessages)
    {
      _map.addInfo(message);
    }

    return false;
  }


  private boolean isOptionalityCompatible(RecordDataSchema.Field field, Object leader, Object follower)
  {
    /*
    compatible:
    leader   : follower : is optional
    null     : null     : true
    null     : not null : true
    not null : not null : true
    not null : not null : false

    incompatible:
    leader   : follower : is optional
    not null : null     : true
    null     : null     : false (caught by validator)
    null     : not null : false (caught by validator)
    not null : null     : false (caught by validator)
    */

    final boolean isLeaderNull = (leader == null);
    final boolean isFollowerNull = (follower == null);
    final boolean isCompatible = !(!isLeaderNull && isFollowerNull && field.getOptional());

    if (isCompatible && isLeaderNull != isFollowerNull)
    {
      _map.addInfo(CompatibilityInfo.Type.OPTIONAL_VALUE, _path, field.getName());
    }

    return isCompatible;
  }

  /**
   * @return true if the check passes, even with compatible changes.
   *         false if the check fails, i.e. incompatible changes are detected
   */
  private boolean checkEqualSingleValue(RecordDataSchema.Field field, Object prevData, Object currData)
  {
    assert (field != null);

    if (!isOptionalityCompatible(field, prevData, currData))
    {
      _map.addInfo(CompatibilityInfo.Type.VALUE_WRONG_OPTIONALITY, _path, field.getName());
      return false;
    }

    // if both prev and curr are null, they are considered equal
    // if prev is null and curr is not null, it has to be a optional field, which is compatible
    // if both prev and curr are not null, they are compatible iff they are equal

    if (prevData != null && !prevData.equals(currData))
    {
      _map.addInfo(field.getName(), CompatibilityInfo.Type.VALUE_NOT_EQUAL, _path, prevData, currData);
      return false;
    }

    return true;
  }

  /**
   * @return whether the optionality check passes
   */
  private boolean checkArrayContainment(RecordDataSchema.Field field,
                                        List<? extends Object> container,
                                        List<? extends Object> containee)
  {
    assert (field != null);

    if (!isOptionalityCompatible(field, containee, container))
    {
      _map.addInfo(CompatibilityInfo.Type.VALUE_WRONG_OPTIONALITY, _path, field.getName());
      return false;
    }

    if (containee == null)
    {
      return true;
    }

    final boolean isContained = container.containsAll(containee);
    if (isContained)
    {
      if (container.size() > containee.size())
      {
        final Set<Object> diff = new HashSet<Object>(container);
        diff.removeAll(containee);
        _map.addInfo(field.getName(), CompatibilityInfo.Type.SUPERSET, _path, diff);
      }

      return true;
    }
    else
    {
      _map.addInfo(field.getName(), CompatibilityInfo.Type.ARRAY_NOT_CONTAIN, _path, containee);
      return false;
    }
  }

  /**
   * @return whether the optionality check passes
   */
  private boolean checkType(Object pathTail, String prevType, String currType, boolean allowNull)
  {
    if (prevType == null && currType == null && allowNull)
    {
      return true;
    }

    if (prevType == null || currType == null)
    {
      _map.addInfo(pathTail, CompatibilityInfo.Type.TYPE_MISSING, _path);
      return false;
    }

    try
    {
      final DataSchema prevSchema = RestSpecCodec.textToSchema(prevType, _prevSchemaResolver);
      final DataSchema currSchema = RestSpecCodec.textToSchema(currType, _currSchemaResolver);

      CompatibilityResult compatibilityResult = CompatibilityChecker.checkCompatibility(prevSchema,
                                                                                        currSchema,
                                                                                        defaultOptions);

      if (!compatibilityResult.getMessages().isEmpty())
      {
        if (prevType.equals(currType) && prevSchema instanceof NamedDataSchema)
        {
          if (!_namedSchemasChecked.contains(prevType))
          {
            addNamedCompatibilityMessages(compatibilityResult.getMessages());
            _namedSchemasChecked.add(prevType);

          }
          return compatibilityResult.isError();
        }
        addCompatibilityMessages(pathTail, compatibilityResult.getMessages());
        return compatibilityResult.isError();
      }

      return true;
    }
    catch (IllegalArgumentException e)
    {
      _map.addInfo(pathTail, CompatibilityInfo.Type.TYPE_UNKNOWN, _path, e.getMessage());
      return false;
    }
  }

  private void addNamedCompatibilityMessages(Collection<CompatibilityMessage> messages)
  {
    for(CompatibilityMessage message : messages)
    {
      _map.addInfo(message);
    }
  }

  private void addCompatibilityMessages(Object pathTail, Collection<CompatibilityMessage> messages)
  {
    for(CompatibilityMessage message : messages)
    {
      _map.addInfo(pathTail, message, _path);
    }
  }

  /**
   * @return whether the optionality check passes
   */
  private boolean checkParameterOptionality(RecordDataSchema.Field field, Boolean prevOptional, Boolean currOptional)
  {
    assert (field != null);

    // optional parameter is compatible in the opposite way of other fields
    // here curr is the leader, prev is the follower
    if (!isOptionalityCompatible(field, currOptional, prevOptional))
    {
      _map.addInfo(CompatibilityInfo.Type.VALUE_WRONG_OPTIONALITY, _path, field.getName());
      return false;
    }

    if (currOptional == null)
    {
      return true;
    }

    if (prevOptional && !currOptional)
    {
      _map.addInfo(field.getName(), CompatibilityInfo.Type.PARAMETER_WRONG_OPTIONALITY, _path);
      return false;
    }

    if (!prevOptional && currOptional)
    {
      // previous required and currently optional should be the only difference that retains backwards compatibility
     _map.addInfo(CompatibilityInfo.Type.OPTIONAL_PARAMETER, _path);
    }

    return true;
  }

  private <T extends RecordTemplate> void checkRecordTemplate(T prevRec, T currRec)
  {
    final Class<?> prevClass = prevRec.getClass();
    if (prevClass == ResourceSchema.class)
    {
      checkResourceSchema((ResourceSchema) prevRec, (ResourceSchema) currRec);
    }
    else if (prevClass == CollectionSchema.class)
    {
      checkCollectionSchema((CollectionSchema) prevRec, (CollectionSchema) currRec);
    }
    else if (prevClass == IdentifierSchema.class)
    {
      checkIdentifierSchema((IdentifierSchema) prevRec, (IdentifierSchema) currRec);
    }
    else if (prevClass == FinderSchema.class)
    {
      checkFinderSchema((FinderSchema) prevRec, (FinderSchema) currRec);
    }
    else if (prevClass == ParameterSchema.class)
    {
      checkParameterSchema((ParameterSchema) prevRec, (ParameterSchema) currRec);
    }
    else if (prevClass == MetadataSchema.class)
    {
      checkMetadataSchema((MetadataSchema) prevRec, (MetadataSchema) currRec);
    }
    else if (prevClass == ActionSchema.class)
    {
      checkActionSchema((ActionSchema) prevRec, (ActionSchema) currRec);
    }
    else if (prevClass == EntitySchema.class)
    {
      checkEntitySchema((EntitySchema) prevRec, (EntitySchema) currRec);
    }
    else if (prevClass == AssociationSchema.class)
    {
      checkAssociationSchema((AssociationSchema) prevRec, (AssociationSchema) currRec);
    }
    else if (prevClass == AssocKeySchema.class)
    {
      checkAssocKeySchema((AssocKeySchema) prevRec, (AssocKeySchema) currRec);
    }
    else if (prevClass == ActionsSetSchema.class)
    {
      checkActionsSetSchema((ActionsSetSchema) prevRec, (ActionsSetSchema) currRec);
    }
    else if (prevClass == RestMethodSchema.class)
    {
      checkRestMethodSchema((RestMethodSchema) prevRec, (RestMethodSchema) currRec);
    }
    else
    {
      _map.addInfo(CompatibilityInfo.Type.OTHER_ERROR, _path, "Unknown schema type: \"" + prevRec.getClass() + "\"");
    }
  }

  /**
   * @return whether the optionality check passes. it does not cover the result of RecordTemplate check
   */
  private <T extends RecordTemplate> boolean checkComplexField(RecordDataSchema.Field field, T prevRec, T currRec)
  {
    assert (field != null);

    if (!isOptionalityCompatible(field, prevRec, currRec))
    {
      _map.addInfo(CompatibilityInfo.Type.VALUE_WRONG_OPTIONALITY, _path, field.getName());
      return false;
    }

    if (prevRec != null)
    {
      _path.push(field.getName());
      checkRecordTemplate(prevRec, currRec);
      _path.pop();
    }

    return true;
  }

  /**
   * @return whether the basic check passes. also true if there is missing array element
   */
  private <T extends WrappingArrayTemplate<? extends RecordTemplate>>
  boolean checkComplexArrayField(RecordDataSchema.Field field,
                                 String keyName,
                                 T prevArray,
                                 T currArray,
                                 HashMap<String, Integer> currRemainder,
                                 boolean checkRemainder)
  {
    assert (field != null);
    assert (currRemainder != null);

    if (!isOptionalityCompatible(field, prevArray, currArray))
    {
      _map.addInfo(CompatibilityInfo.Type.VALUE_WRONG_OPTIONALITY, _path, field.getName());
      return false;
    }

    if (prevArray == null)
    {
      return true;
    }

    assert (prevArray.getClass() == currArray.getClass());

    _path.push(field.getName());

    for (int i = 0; i < currArray.size(); ++i)
    {
      currRemainder.put(currArray.get(i).data().getString(keyName), i);
    }

    for (RecordTemplate prevElement: prevArray)
    {
      // find prev and curr element with same key name
      final String prevKey = prevElement.data().getString(keyName);
      final Integer currIndex = currRemainder.get(prevKey);

      if (currIndex == null)
      {
        _map.addInfo(CompatibilityInfo.Type.ARRAY_MISSING_ELEMENT, _path, prevKey);
      }
      else
      {
        final RecordTemplate currElement = currArray.get(currIndex);
        currRemainder.remove(prevKey);

        _path.push(prevKey);
        checkRecordTemplate(prevElement, currElement);
        _path.pop();
      }
    }

    if (checkRemainder && !currRemainder.isEmpty())
    {
      _map.addInfo(CompatibilityInfo.Type.SUPERSET, _path, currRemainder.keySet());
    }

    _path.pop();

    // all missing element errors have been recorded, avoid duplicate errors by returning true
    return true;
  }

  private <T extends WrappingArrayTemplate<? extends RecordTemplate>>
  boolean checkComplexArrayField(RecordDataSchema.Field field, String keyName, T prevArray, T currArray)
  {
    return checkComplexArrayField(field, keyName, prevArray, currArray, new HashMap<String, Integer>(), true);
  }

  /**
   * @return whether the check passes
   */
  private <T extends WrappingArrayTemplate<? extends RecordTemplate>>
  boolean checkEqualComplexArrayField(RecordDataSchema.Field field, String keyName, T prevArray, T currArray)
  {
    final HashMap<String, Integer> currRemainder = new HashMap<String, Integer>();

    // if prev has more than curr, array missing element
    // this should catch it
    if (!checkComplexArrayField(field, keyName, prevArray, currArray, currRemainder, false))
    {
      return false;
    }

    // if prev has less than curr, the remainder will contain the extra current elements
    if (!currRemainder.isEmpty())
    {
      _map.addInfo(field.getName(), CompatibilityInfo.Type.ARRAY_NOT_EQUAL, _path, prevArray);
      return false;
    }

    return true;
  }

  /**
   * @return whether the check passes
   */
  private boolean checkParameterArrayField(RecordDataSchema.Field field, ParameterSchemaArray prevArray, ParameterSchemaArray currArray)
  {
    final HashMap<String, Integer> currRemainder = new HashMap<String, Integer>();

    if (!checkComplexArrayField(field, "name", prevArray, currArray, currRemainder, false))
    {
      return false;
    }

    _path.push(field.getName());

    boolean result = true;
    for (int paramIndex : currRemainder.values())
    {
      // all parameters only appear in curr must not be required
      final ParameterSchema param = currArray.get(paramIndex);
      if (isQueryParameterOptional(param.isOptional(), param.getDefault(GetMode.DEFAULT)))
      {
        _map.addInfo(CompatibilityInfo.Type.PARAMETER_NEW_OPTIONAL, _path, param.getName());
      }
      else
      {
        _map.addInfo(CompatibilityInfo.Type.PARAMETER_NEW_REQUIRED, _path, param.getName());
        result = false;
      }
    }

    _path.pop();

    // all parameter errors have been recorded, avoid duplicate errors by returning true
    return result;
  }

  private String getParameterItems(ParameterSchema param, DataSchemaResolver resolver)
  {
    if (param.hasItems())
    {
      _map.addInfo(CompatibilityInfo.Type.DEPRECATED, _path, "The \"items\" field");
      return param.getItems(GetMode.DEFAULT);
    }

    final DataSchema paramDataSchema = RestSpecCodec.textToSchema(param.getType(GetMode.DEFAULT), resolver);
    if (paramDataSchema instanceof ArrayDataSchema)
    {
      // Union member key works because previously only primitive types are allowed for items field
      return ((ArrayDataSchema) paramDataSchema).getItems().getUnionMemberKey();
    }
    else
    {
      _map.addInfo("type", CompatibilityInfo.Type.TYPE_INCOMPATIBLE, _path, "items", paramDataSchema.getType());
      return null;
    }
  }

  private void checkResourceSchema(ResourceSchema prevRec, ResourceSchema currRec)
  {
    checkEqualSingleValue(prevRec.schema().getField("name"),
                          prevRec.getName(GetMode.DEFAULT),
                          currRec.getName(GetMode.DEFAULT));

    checkEqualSingleValue(prevRec.schema().getField("namespace"),
                          prevRec.getNamespace(GetMode.DEFAULT),
                          currRec.getNamespace(GetMode.DEFAULT));

    checkEqualSingleValue(prevRec.schema().getField("path"),
                          prevRec.getPath(GetMode.DEFAULT),
                          currRec.getPath(GetMode.DEFAULT));

    checkType("schema",
              prevRec.getSchema(GetMode.DEFAULT),
              currRec.getSchema(GetMode.DEFAULT),
              prevRec.hasActionsSet()); // action sets do not have schemas.

    checkComplexField(prevRec.schema().getField("collection"), prevRec.getCollection(), currRec.getCollection());

    checkComplexField(prevRec.schema().getField("association"), prevRec.getAssociation(), currRec.getAssociation());

    checkComplexField(prevRec.schema().getField("actionsSet"), prevRec.getActionsSet(), currRec.getActionsSet());
  }

  private void checkCollectionSchema(CollectionSchema prevRec, CollectionSchema currRec)
  {
    checkComplexField(prevRec.schema().getField("identifier"),
                      prevRec.getIdentifier(GetMode.DEFAULT),
                      currRec.getIdentifier(GetMode.DEFAULT));

    checkArrayContainment(prevRec.schema().getField("supports"),
                          currRec.getSupports(GetMode.DEFAULT),
                          prevRec.getSupports(GetMode.DEFAULT));

    checkComplexArrayField(prevRec.schema().getField("methods"),
                           "method",
                           prevRec.getMethods(GetMode.DEFAULT),
                           currRec.getMethods(GetMode.DEFAULT));

    checkComplexArrayField(prevRec.schema().getField("finders"),
                           "name",
                           prevRec.getFinders(GetMode.DEFAULT),
                           currRec.getFinders(GetMode.DEFAULT));

    checkComplexArrayField(prevRec.schema().getField("actions"),
                           "name",
                           prevRec.getActions(GetMode.DEFAULT),
                           currRec.getActions(GetMode.DEFAULT));

    checkComplexField(prevRec.schema().getField("entity"),
                      prevRec.getEntity(GetMode.DEFAULT),
                      currRec.getEntity(GetMode.DEFAULT));
  }

  private void checkIdentifierSchema(IdentifierSchema prevRec, IdentifierSchema currRec)
  {
    checkEqualSingleValue(prevRec.schema().getField("name"),
                          prevRec.getName(GetMode.DEFAULT),
                          currRec.getName(GetMode.DEFAULT));

    // type in IdentifierSchema currently can only be named type, thus guaranteed to be string
    checkType("type", prevRec.getType(GetMode.DEFAULT), currRec.getType(GetMode.DEFAULT), false);

    checkType("params", prevRec.getParams(GetMode.DEFAULT), currRec.getParams(GetMode.DEFAULT), true);
  }

  private void checkFinderSchema(FinderSchema prevRec, FinderSchema currRec)
  {
    checkEqualSingleValue(prevRec.schema().getField("name"),
                          prevRec.getName(GetMode.DEFAULT),
                          currRec.getName(GetMode.DEFAULT));

    checkParameterArrayField(prevRec.schema().getField("parameters"),
                             prevRec.getParameters(GetMode.DEFAULT),
                             currRec.getParameters(GetMode.DEFAULT));

    checkComplexField(prevRec.schema().getField("metadata"),
                      prevRec.getMetadata(GetMode.DEFAULT),
                      currRec.getMetadata(GetMode.DEFAULT));

    final String prevAssocKey = prevRec.getAssocKey(GetMode.DEFAULT);
    final String currAssocKey = currRec.getAssocKey(GetMode.DEFAULT);
    final StringArray prevAssocKeys = prevRec.getAssocKeys(GetMode.DEFAULT);
    final StringArray currAssocKeys = currRec.getAssocKeys(GetMode.DEFAULT);

    // assocKey and assocKeys are mutually exclusive
    assert((prevAssocKey == null || prevAssocKeys == null) && (currAssocKey == null || currAssocKeys == null));

    // assocKey is deprecated
    // assocKey upgrading to single-element assocKeys is compatible
    // the opposite direction is deprecated thus incompatible

    if (prevAssocKeys == null && currAssocKeys == null)
    {
      checkEqualSingleValue(prevRec.schema().getField("assocKey"), prevAssocKey, currAssocKey);
    }
    else if (prevAssocKey == null && currAssocKey == null)
    {
      checkEqualSingleValue(prevRec.schema().getField("assocKeys"), prevAssocKeys, currAssocKeys);
    }
    else if (prevAssocKeys == null)
    {
      // upgrade case

      final StringArray upgradedPrevAssocKeys = new StringArray();
      upgradedPrevAssocKeys.add(prevAssocKey);
      checkEqualSingleValue(prevRec.schema().getField("assocKey"), upgradedPrevAssocKeys, currAssocKeys);
    }
    else
    {
      // downgrade case

      _map.addInfo("assocKeys", CompatibilityInfo.Type.FINDER_ASSOCKEYS_DOWNGRADE, _path);
    }
  }

  private void checkParameterSchema(ParameterSchema prevRec, ParameterSchema currRec)
  {
    checkEqualSingleValue(prevRec.schema().getField("name"),
                          prevRec.getName(GetMode.DEFAULT),
                          currRec.getName(GetMode.DEFAULT));

    /*
    Compatibility of the deprecated "items" field:
    O: Has items   X: Has Not items
    prev  curr  Result
     O     O    Check type compatibility of "items" field
     O     X    If curr type is ArrayDataSchema, extract curr's "items" and check compatibility of "items"
                Otherwise, incompatible
     X     O    Same as above
     X     X    Check type compatibility of "type" field

    In any case, emit "deprecated" compatible message if non-array "items" field is found.
     */
    if (prevRec.hasItems() || currRec.hasItems())
    {
      final String prevItems = getParameterItems(prevRec, _prevSchemaResolver);
      final String currItems = getParameterItems(currRec, _currSchemaResolver);
      if (prevItems != null && currItems != null)
      {
        checkType("items", prevItems, currItems, false);
      }
    }
    else
    {
      checkType("type", prevRec.getType(GetMode.DEFAULT), currRec.getType(GetMode.DEFAULT), false);
    }

    final Boolean prevOptional = prevRec.isOptional(GetMode.DEFAULT);
    final Boolean currOptional = currRec.isOptional(GetMode.DEFAULT);
    final String prevDefault = prevRec.getDefault(GetMode.DEFAULT);
    final String currDefault = currRec.getDefault(GetMode.DEFAULT);

    // {@link com.linkedin.restli.internal.server.model.ResourceModelEncoder} assures that "optional" and "default" are mutually exclusive.
    assert((prevOptional == null || prevDefault == null) && (currOptional == null || currDefault == null));

    checkParameterOptionality(prevRec.schema().getField("optional"),
                              isQueryParameterOptional(prevOptional, prevDefault),
                              isQueryParameterOptional(currOptional, currDefault));

    if (prevDefault != null && currDefault != null && !prevDefault.equals(currDefault))
    {
      _map.addInfo("default", CompatibilityInfo.Type.VALUE_DIFFERENT, _path, prevDefault, currDefault);
    }
  }

  private void checkMetadataSchema(MetadataSchema prevRec, MetadataSchema currRec)
  {
    checkType("type", prevRec.getType(GetMode.DEFAULT), currRec.getType(GetMode.DEFAULT), false);
  }

  private void checkActionSchema(ActionSchema prevRec, ActionSchema currRec)
  {
    checkEqualSingleValue(prevRec.schema().getField("name"),
                          prevRec.getName(GetMode.DEFAULT),
                          currRec.getName(GetMode.DEFAULT));

    checkParameterArrayField(prevRec.schema().getField("parameters"),
                             prevRec.getParameters(GetMode.DEFAULT),
                             currRec.getParameters(GetMode.DEFAULT));

    checkType("returns", prevRec.getReturns(), currRec.getReturns(), true);

    checkArrayContainment(prevRec.schema().getField("throws"),
                          prevRec.getThrows(GetMode.DEFAULT),
                          currRec.getThrows(GetMode.DEFAULT));
  }

  private void checkEntitySchema(EntitySchema prevRec, EntitySchema currRec)
  {
    checkEqualSingleValue(prevRec.schema().getField("path"),
                          prevRec.getPath(GetMode.DEFAULT),
                          currRec.getPath(GetMode.DEFAULT));

    checkComplexArrayField(prevRec.schema().getField("actions"),
                           "name",
                           prevRec.getActions(GetMode.DEFAULT),
                           currRec.getActions(GetMode.DEFAULT));

    checkComplexArrayField(prevRec.schema().getField("subresources"),
                           "name",
                           prevRec.getSubresources(GetMode.DEFAULT),
                           currRec.getSubresources(GetMode.DEFAULT));
  }

  private void checkAssociationSchema(AssociationSchema prevRec, AssociationSchema currRec)
  {
    checkEqualComplexArrayField(prevRec.schema().getField("assocKeys"),
                                "name",
                                prevRec.getAssocKeys(GetMode.DEFAULT),
                                currRec.getAssocKeys(GetMode.DEFAULT));

    checkArrayContainment(prevRec.schema().getField("supports"),
                          currRec.getSupports(GetMode.DEFAULT),
                          prevRec.getSupports(GetMode.DEFAULT));

    checkComplexArrayField(prevRec.schema().getField("methods"),
                           "method",
                           prevRec.getMethods(GetMode.DEFAULT),
                           currRec.getMethods(GetMode.DEFAULT));

    checkComplexArrayField(prevRec.schema().getField("finders"),
                           "name",
                           prevRec.getFinders(GetMode.DEFAULT),
                           currRec.getFinders(GetMode.DEFAULT));

    checkComplexArrayField(prevRec.schema().getField("actions"),
                           "name",
                           prevRec.getActions(GetMode.DEFAULT),
                           currRec.getActions(GetMode.DEFAULT));

    checkComplexField(prevRec.schema().getField("entity"),
                      prevRec.getEntity(GetMode.DEFAULT),
                      currRec.getEntity(GetMode.DEFAULT));
  }

  private void checkAssocKeySchema(AssocKeySchema prevRec, AssocKeySchema currRec)
  {
    checkEqualSingleValue(prevRec.schema().getField("name"),
                          prevRec.getName(GetMode.DEFAULT),
                          currRec.getName(GetMode.DEFAULT));

    checkType("type", prevRec.getType(GetMode.DEFAULT), currRec.getType(GetMode.DEFAULT), false);
  }

  private void checkActionsSetSchema(ActionsSetSchema prevRec, ActionsSetSchema currRec)
  {
    checkComplexArrayField(prevRec.schema().getField("actions"),
                           "name",
                           prevRec.getActions(GetMode.DEFAULT),
                           currRec.getActions(GetMode.DEFAULT));
  }

  private void checkRestMethodSchema(RestMethodSchema prevRec, RestMethodSchema currRec)
  {
    checkEqualSingleValue(prevRec.schema().getField("method"),
                          prevRec.getMethod(GetMode.DEFAULT),
                          currRec.getMethod(GetMode.DEFAULT));

    checkParameterArrayField(prevRec.schema().getField("parameters"),
                             prevRec.getParameters(GetMode.DEFAULT),
                             currRec.getParameters(GetMode.DEFAULT));
  }

  /**
   * Query parameter is considered compatible if both the old and new are optional or has non-null default value.
   */
  private boolean isQueryParameterOptional(Boolean isOptional, String defaultValue)
  {
    return (isOptional == null ? defaultValue != null : isOptional);
  }

}
