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

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.element.DataElement;
import com.linkedin.data.message.Message;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
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
import com.linkedin.restli.common.validation.CreateOnly;
import com.linkedin.restli.common.validation.ReadOnly;
import com.linkedin.restli.common.validation.ValidationUtil;
import com.linkedin.restli.restspec.ActionSchema;
import com.linkedin.restli.restspec.ActionsSetSchema;
import com.linkedin.restli.restspec.AssocKeySchema;
import com.linkedin.restli.restspec.AssociationSchema;
import com.linkedin.restli.restspec.CollectionSchema;
import com.linkedin.restli.restspec.CustomAnnotationContentSchema;
import com.linkedin.restli.restspec.CustomAnnotationContentSchemaMap;
import com.linkedin.restli.restspec.EntitySchema;
import com.linkedin.restli.restspec.FinderSchema;
import com.linkedin.restli.restspec.IdentifierSchema;
import com.linkedin.restli.restspec.MetadataSchema;
import com.linkedin.restli.restspec.ParameterSchema;
import com.linkedin.restli.restspec.ParameterSchemaArray;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestMethodSchema;
import com.linkedin.restli.restspec.RestSpecAnnotation;
import com.linkedin.restli.restspec.SimpleSchema;
import com.linkedin.restli.restspec.RestSpecCodec;
import com.linkedin.restli.tools.idlcheck.CompatibilityInfo;
import com.linkedin.restli.tools.idlcheck.CompatibilityLevel;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

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
  private CompatibilityInfoMap _infoMap = new CompatibilityInfoMap();
  private Stack<Object> _infoPath = new Stack<Object>();

  private Set<String> _namedSchemasChecked = new HashSet<String>();

  private static final CompatibilityOptions defaultOptions =
    new CompatibilityOptions().setMode(CompatibilityOptions.Mode.SCHEMA).setAllowPromotions(false);

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
    _infoPath.push("");
  }

  public boolean check(CompatibilityLevel level)
  {
    if (!_checked) runCheck();
    return _infoMap.isCompatible(level);

  }

  public void check()
  {
    if (!_checked) runCheck();
  }

  public CompatibilityInfoMap getInfoMap()
  {
    return _infoMap;
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
      _infoMap.addRestSpecInfo(message);
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
      _infoMap.addRestSpecInfo(CompatibilityInfo.Type.OPTIONAL_VALUE, _infoPath, field.getName());
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
      _infoMap.addRestSpecInfo(CompatibilityInfo.Type.VALUE_WRONG_OPTIONALITY, _infoPath,
                               field.getName());
      return false;
    }

    // if both prev and curr are null, they are considered equal
    // if prev is null and curr is not null, it has to be a optional field, which is compatible
    // if both prev and curr are not null, they are compatible iff they are equal

    if (prevData != null && !prevData.equals(currData))
    {
      _infoMap.addRestSpecInfo(field.getName(), CompatibilityInfo.Type.VALUE_NOT_EQUAL, _infoPath,
                               prevData, currData);
      return false;
    }

    return true;
  }

  private boolean checkDoc(RecordDataSchema.Field field, Object prevData, Object currData)
  {
    assert (field != null);

    if ((prevData == null) != (currData == null))
    {
      _infoMap.addRestSpecInfo(field.getName(), CompatibilityInfo.Type.DOC_NOT_EQUAL, _infoPath);
      return false;
    }

    if (prevData != null && !prevData.equals(currData))
    {
      _infoMap.addRestSpecInfo(field.getName(), CompatibilityInfo.Type.DOC_NOT_EQUAL, _infoPath);
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
      _infoMap.addRestSpecInfo(CompatibilityInfo.Type.VALUE_WRONG_OPTIONALITY, _infoPath,
                               field.getName());
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
        _infoMap.addRestSpecInfo(field.getName(), CompatibilityInfo.Type.SUPERSET, _infoPath, diff);
      }

      return true;
    }
    else
    {
      _infoMap.addRestSpecInfo(field.getName(), CompatibilityInfo.Type.ARRAY_NOT_CONTAIN, _infoPath,
                               containee);
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
      _infoMap.addRestSpecInfo(pathTail, CompatibilityInfo.Type.TYPE_MISSING, _infoPath);
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
      _infoMap.addRestSpecInfo(pathTail, CompatibilityInfo.Type.TYPE_UNKNOWN, _infoPath,
                               e.getMessage());
      return false;
    }
  }

  private void addNamedCompatibilityMessages(Collection<CompatibilityMessage> messages)
  {
    for(CompatibilityMessage message : messages)
    {
      _infoMap.addModelInfo(message);
    }
  }

  private void addCompatibilityMessages(Object pathTail, Collection<CompatibilityMessage> messages)
  {
    for(CompatibilityMessage message : messages)
    {
      _infoMap.addRestSpecInfo(pathTail, message, _infoPath);
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
      _infoMap.addRestSpecInfo(CompatibilityInfo.Type.VALUE_WRONG_OPTIONALITY, _infoPath,
                               field.getName());
      return false;
    }

    if (currOptional == null)
    {
      return true;
    }

    if (prevOptional && !currOptional)
    {
      _infoMap.addRestSpecInfo(field.getName(), CompatibilityInfo.Type.PARAMETER_WRONG_OPTIONALITY,
                               _infoPath);
      return false;
    }

    if (!prevOptional && currOptional)
    {
      // previous required and currently optional should be the only difference that retains backwards compatibility
     _infoMap.addRestSpecInfo(CompatibilityInfo.Type.OPTIONAL_PARAMETER, _infoPath);
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
    else if (prevClass == SimpleSchema.class)
    {
      checkSimpleSchema((SimpleSchema) prevRec, (SimpleSchema) currRec);
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
      _infoMap.addRestSpecInfo(CompatibilityInfo.Type.OTHER_ERROR,
                               _infoPath, "Unknown schema type: \"" + prevRec.getClass() + "\"");
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
      _infoMap.addRestSpecInfo(CompatibilityInfo.Type.VALUE_WRONG_OPTIONALITY, _infoPath,
                               field.getName());
      return false;
    }

    if (prevRec != null)
    {
      _infoPath.push(field.getName());
      checkRecordTemplate(prevRec, currRec);
      _infoPath.pop();
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
      _infoMap.addRestSpecInfo(CompatibilityInfo.Type.VALUE_WRONG_OPTIONALITY, _infoPath,
                               field.getName());
      return false;
    }

    if (prevArray == null)
    {
      return true;
    }

    assert (prevArray.getClass() == currArray.getClass());

    _infoPath.push(field.getName());

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
        _infoMap.addRestSpecInfo(CompatibilityInfo.Type.ARRAY_MISSING_ELEMENT, _infoPath, prevKey);
      }
      else
      {
        final RecordTemplate currElement = currArray.get(currIndex);
        currRemainder.remove(prevKey);

        _infoPath.push(prevKey);
        checkRecordTemplate(prevElement, currElement);
        _infoPath.pop();
      }
    }

    if (checkRemainder && !currRemainder.isEmpty())
    {
      _infoMap.addRestSpecInfo(CompatibilityInfo.Type.SUPERSET, _infoPath, currRemainder.keySet());
    }

    _infoPath.pop();

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
      _infoMap.addRestSpecInfo(field.getName(), CompatibilityInfo.Type.ARRAY_NOT_EQUAL, _infoPath,
                               prevArray);
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

    _infoPath.push(field.getName());

    boolean result = true;
    for (int paramIndex : currRemainder.values())
    {
      // all parameters only appear in curr must not be required
      final ParameterSchema param = currArray.get(paramIndex);
      if (isQueryParameterOptional(param.isOptional(), param.getDefault(GetMode.DEFAULT)))
      {
        _infoMap.addRestSpecInfo(CompatibilityInfo.Type.PARAMETER_NEW_OPTIONAL, _infoPath,
                                 param.getName());
      }
      else
      {
        _infoMap.addRestSpecInfo(CompatibilityInfo.Type.PARAMETER_NEW_REQUIRED, _infoPath,
                                 param.getName());
        result = false;
      }
    }

    _infoPath.pop();

    // all parameter errors have been recorded, avoid duplicate errors by returning true
    return result;
  }

  private String getParameterItems(ParameterSchema param, DataSchemaResolver resolver)
  {
    if (param.hasItems())
    {
      _infoMap.addRestSpecInfo(CompatibilityInfo.Type.DEPRECATED, _infoPath, "The \"items\" field");
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
      _infoMap.addRestSpecInfo("type", CompatibilityInfo.Type.TYPE_ERROR, _infoPath, "expect an array, got " + paramDataSchema.getType());
      return null;
    }
  }

  private void checkResourceSchema(ResourceSchema prevRec, ResourceSchema currRec)
  {
    checkEqualSingleValue(prevRec.schema().getField("name"),
                          prevRec.getName(GetMode.DEFAULT),
                          currRec.getName(GetMode.DEFAULT));

    checkDoc(prevRec.schema().getField("doc"), prevRec.getDoc(GetMode.DEFAULT), currRec.getDoc(GetMode.DEFAULT));

    checkAnnotationsMap(prevRec.schema().getField("annotations"),
                        prevRec.getAnnotations(GetMode.DEFAULT),
                        currRec.getAnnotations(GetMode.DEFAULT));

    checkRestLiDataAnnotations(prevRec.schema().getField("annotations"),
                               prevRec.getAnnotations(GetMode.DEFAULT),
                               currRec.getAnnotations(GetMode.DEFAULT),
                               prevRec.getSchema(GetMode.DEFAULT),
                               currRec.getSchema(GetMode.DEFAULT));

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

    checkComplexField(prevRec.schema().getField("simple"), prevRec.getSimple(), currRec.getSimple());

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

    checkDoc(prevRec.schema().getField("doc"), prevRec.getDoc(GetMode.DEFAULT), currRec.getDoc(GetMode.DEFAULT));

    checkAnnotationsMap(prevRec.schema().getField("annotations"),
                        prevRec.getAnnotations(GetMode.DEFAULT),
                        currRec.getAnnotations(GetMode.DEFAULT));

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

      _infoMap.addRestSpecInfo("assocKeys", CompatibilityInfo.Type.FINDER_ASSOCKEYS_DOWNGRADE,
                               _infoPath);
    }
  }

  private void checkSimpleSchema(SimpleSchema prevRec, SimpleSchema currRec)
  {
    checkArrayContainment(prevRec.schema().getField("supports"),
                          currRec.getSupports(GetMode.DEFAULT),
                          prevRec.getSupports(GetMode.DEFAULT));

    checkComplexArrayField(prevRec.schema().getField("methods"),
                           "method",
                           prevRec.getMethods(GetMode.DEFAULT),
                           currRec.getMethods(GetMode.DEFAULT));

    checkComplexArrayField(prevRec.schema().getField("actions"),
                           "name",
                           prevRec.getActions(GetMode.DEFAULT),
                           currRec.getActions(GetMode.DEFAULT));

    checkComplexField(prevRec.schema().getField("entity"),
                      prevRec.getEntity(GetMode.DEFAULT),
                      currRec.getEntity(GetMode.DEFAULT));
  }

  private void checkParameterSchema(ParameterSchema prevRec, ParameterSchema currRec)
  {
    checkEqualSingleValue(prevRec.schema().getField("name"),
                          prevRec.getName(GetMode.DEFAULT),
                          currRec.getName(GetMode.DEFAULT));

    checkDoc(prevRec.schema().getField("doc"), prevRec.getDoc(GetMode.DEFAULT), currRec.getDoc(GetMode.DEFAULT));

    checkAnnotationsMap(prevRec.schema().getField("annotations"),
                        prevRec.getAnnotations(GetMode.DEFAULT),
                        currRec.getAnnotations(GetMode.DEFAULT));

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
      _infoMap.addRestSpecInfo("default", CompatibilityInfo.Type.VALUE_DIFFERENT, _infoPath,
                               prevDefault, currDefault);
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

    checkDoc(prevRec.schema().getField("doc"), prevRec.getDoc(GetMode.DEFAULT), currRec.getDoc(GetMode.DEFAULT));

    checkAnnotationsMap(prevRec.schema().getField("annotations"),
                        prevRec.getAnnotations(GetMode.DEFAULT),
                        currRec.getAnnotations(GetMode.DEFAULT));

    checkParameterArrayField(prevRec.schema().getField("parameters"),
                             prevRec.getParameters(GetMode.DEFAULT),
                             currRec.getParameters(GetMode.DEFAULT));

    checkType("returns", prevRec.getReturns(), currRec.getReturns(), true);

    checkArrayContainment(prevRec.schema().getField("throws"),
                          prevRec.getThrows(GetMode.DEFAULT),
                          currRec.getThrows(GetMode.DEFAULT));
  }

  /**
   * Determines whether a path is valid according to the given data schema.
   *
   * @param schema data schema
   * @param pathWithoutKeys full path for a field, without map key names and array indices.
   * @return true if path denotes a field in the data schema
   */
  /* package private */ static boolean containsPath(DataSchema schema, String pathWithoutKeys)
  {
    StringTokenizer st = new StringTokenizer(pathWithoutKeys, DataElement.SEPARATOR.toString());
    DataSchema dataSchema = schema;
    while (st.hasMoreTokens())
    {
      String comp = st.nextToken();
      dataSchema = dataSchema.getDereferencedDataSchema();

      if (dataSchema.getType() == DataSchema.Type.MAP)
      {
        // Parent is a map, so current component is a key name
        dataSchema = ((MapDataSchema) dataSchema).getValues();
        continue;
      }
      else if (dataSchema.getType() == DataSchema.Type.ARRAY)
      {
        // Parent is an array, so current component is an index
        dataSchema = ((ArrayDataSchema) dataSchema).getItems();
        continue;
      }
      else if (dataSchema.getType() == DataSchema.Type.RECORD)
      {
        RecordDataSchema.Field field = ((RecordDataSchema) dataSchema).getField(comp);
        if (field == null) return false;
        dataSchema = field.getType();
      }
      else if (dataSchema.getType() == DataSchema.Type.UNION)
      {
        dataSchema = ((UnionDataSchema) dataSchema).getType(comp);
        if (dataSchema == null) return false;
      }
      else
      {
        // Parent cannot be a primitive type
        return false;
      }
    }
    return true;
  }

  private void checkRestLiDataAnnotations(RecordDataSchema.Field field, CustomAnnotationContentSchemaMap prevMap, CustomAnnotationContentSchemaMap currMap,
                                          String prevType, String currType)
  {
    if (prevType == null || currType == null)
    {
      return;
    }
    _infoPath.push(field.getName());

    DataSchema prevSchema = RestSpecCodec.textToSchema(prevType, _prevSchemaResolver);
    DataSchema currSchema = RestSpecCodec.textToSchema(currType, _currSchemaResolver);

    for (Class<?> annotationClass : new Class<?>[]{ReadOnly.class, CreateOnly.class})
    {
      String annotationName = annotationClass.getAnnotation(RestSpecAnnotation.class).name();
      Set<Object> prevPaths = new HashSet<Object>();
      if (prevMap != null && prevMap.containsKey(annotationName)) prevPaths.addAll((DataList) prevMap.get(annotationName).data().get("value"));
      Set<Object> currPaths = new HashSet<Object>();
      if (currMap != null && currMap.containsKey(annotationName)) currPaths.addAll((DataList) currMap.get(annotationName).data().get("value"));
      // Adding an annotation is only valid if the field was newly added to the schema.
      Set<Object> addedPaths = new HashSet<Object>(currPaths);
      addedPaths.removeAll(prevPaths);
      for (Object path : addedPaths)
      {
        String pathString = path.toString();
        if (ValidationUtil.containsPath(prevSchema, pathString))
        {
          _infoMap.addRestSpecInfo(pathString, CompatibilityInfo.Type.ANNOTATION_CHANGE_BREAKS_OLD_CLIENT, _infoPath,
                                   "Cannot add " + annotationClass.getSimpleName() + " annotation");
        }
      }
      // Removing an annotation is only valid if the field was removed from the schema.
      Set<Object> removedPaths = new HashSet<Object>(prevPaths);
      removedPaths.removeAll(currPaths);
      for (Object path : removedPaths)
      {
        String pathString = path.toString();
        if (ValidationUtil.containsPath(currSchema, pathString))
        {
          if (!ValidationUtil.getField(currSchema, pathString).getOptional() && annotationClass.equals(ReadOnly.class))
          {
            _infoMap.addRestSpecInfo(pathString, CompatibilityInfo.Type.ANNOTATION_CHANGE_BREAKS_NEW_SERVER, _infoPath,
                                     "Cannot remove " + annotationClass.getSimpleName() + " annotation");
          }
          else
          {
            _infoMap.addRestSpecInfo(pathString, CompatibilityInfo.Type.ANNOTATION_CHANGE_MAY_REQUIRE_CLIENT_CODE_CHANGE, _infoPath,
                                     "Cannot remove " + annotationClass.getSimpleName() + " annotation");
          }
        }
      }
    }

    _infoPath.pop();
  }

  private void checkAnnotationsMap(RecordDataSchema.Field field, CustomAnnotationContentSchemaMap prevMap, CustomAnnotationContentSchemaMap currMap)
  {
    Set<String> allKeys = new HashSet<String>();
    if (prevMap != null) allKeys.addAll(prevMap.keySet());
    if (currMap != null) allKeys.addAll(currMap.keySet());
    for(String key : allKeys)
    {
      CustomAnnotationContentSchema prevMapAnnotation = prevMap == null ? null : prevMap.get(key);
      CustomAnnotationContentSchema currMapAnnotation = currMap == null ? null : currMap.get(key);
      _infoPath.push(field.getName());
      checkAnnotationsSchema(key, prevMapAnnotation, currMapAnnotation);
      _infoPath.pop();
    }
  }

  private void checkAnnotationsSchema(String key, CustomAnnotationContentSchema prevRec, CustomAnnotationContentSchema currRec)
  {
    if (prevRec == null)
    {
      _infoMap.addRestSpecInfo(key, CompatibilityInfo.Type.ANNOTATIONS_CHANGED, _infoPath, "added");
    }
    else if (currRec == null)
    {
      _infoMap.addRestSpecInfo(key, CompatibilityInfo.Type.ANNOTATIONS_CHANGED, _infoPath, "removed");
    }
    else if (!prevRec.equals(currRec))
    {
      _infoMap.addRestSpecInfo(key, CompatibilityInfo.Type.ANNOTATIONS_CHANGED, _infoPath, "value changed");
    }
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
    checkEqualSingleValue(prevRec.schema().getField("identifier"),
                          prevRec.getIdentifier(GetMode.DEFAULT),
                          currRec.getIdentifier(GetMode.DEFAULT));

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

    checkDoc(prevRec.schema().getField("doc"), prevRec.getDoc(GetMode.DEFAULT), currRec.getDoc(GetMode.DEFAULT));

    checkAnnotationsMap(prevRec.schema().getField("annotations"),
                        prevRec.getAnnotations(GetMode.DEFAULT),
                        currRec.getAnnotations(GetMode.DEFAULT));

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
