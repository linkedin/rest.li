/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.common.validation;


import com.linkedin.data.DataMap;
import com.linkedin.data.element.DataElement;
import com.linkedin.data.element.DataElementUtil;
import com.linkedin.data.element.SimpleDataElement;
import com.linkedin.data.it.PathMatchesPatternPredicate;
import com.linkedin.data.it.Predicate;
import com.linkedin.data.it.Predicates;
import com.linkedin.data.it.Wildcard;
import com.linkedin.data.message.Message;
import com.linkedin.data.message.MessageList;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaUtil;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import com.linkedin.data.schema.validation.RequiredMode;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.schema.validator.DataSchemaAnnotationValidator;
import com.linkedin.data.schema.validator.Validator;
import com.linkedin.data.schema.validator.ValidatorContext;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.TemplateRuntimeException;
import com.linkedin.data.transform.DataComplexProcessor;
import com.linkedin.data.transform.DataProcessingException;
import com.linkedin.data.transform.filter.FilterConstants;
import com.linkedin.data.transform.filter.request.MaskOperation;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.data.transform.patch.Patch;
import com.linkedin.data.transform.patch.PatchConstants;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.restspec.RestSpecAnnotation;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Rest.li data validator validates Rest.li data using information from the data schema
 * as well as additional Rest.li context such as method types.<p>
 *
 * This validator uses 3 types of rules:
 * <ol>
 *   <li> Whether a field is optional or required (the validator uses {@link RequiredMode#CAN_BE_ABSENT_IF_HAS_DEFAULT},
 *   so it is okay for a required field to be missing if it has a default value).
 *   <li> Data schema annotations specified with the "validate" property (see {@link DataSchemaAnnotationValidator}).
 *   <li> (From Rest.li resource) Rest.li annotations such as {@link CreateOnly} and {@link ReadOnly}.
 * </ol>
 * <p>
 * Rest.li annotations should be used on top of the resource and should specify paths in the same format as
 * calling .toString() on the field's {@link com.linkedin.data.schema.PathSpec}.
 * Because full paths are listed, different rules can be specified for records that have the same schema.
 * For example, if the schema contains two Photos, you can make the id of photo1 ReadOnly and id of photo2 non-ReadOnly.
 * This is different from the optional/required distinction where if the id of photo1 is required, the id of photo2 will also be required.
 * <p>
 * To use the validator from the server side, there are two options:
 * <ol>
 *   <li> Inject the validator as a parameter of the resource method.<br>
 *        e.g. <code>public CreateResponse create(final ValidationDemo entity, @ValidatorParam RestLiDataValidator validator)</code><br>
 *        Call the validate() method with the entity or the patch.
 *        For batch requests or responses, the validate() method has to be called for each entity/patch.
 *   <li> Use the Rest.li input / output validation filters. The filter(s) will throw up on invalid requests / responses.
 * </ol>
 * From the client side, Rest.li validation is only supported for inputs (requests).<br>
 * Request builders for CRUD methods with write operations have the validateInput() method.<br>
 * e.g. <code>ValidationResult result = new PhotosRequestBuilders().create().validateInput(photo);</code><br>
 * Clients have to use the pegasus data validator ({@link ValidateDataAgainstSchema}) if they want to validate responses.
 * @author Soojung Ha
 */
public class RestLiDataValidator
{
  // ReadOnly fields should not be specified for these types of requests
  private static final Set<ResourceMethod> readOnlyRestrictedMethods = new HashSet<ResourceMethod>(
      Arrays.asList(ResourceMethod.CREATE, ResourceMethod.PARTIAL_UPDATE, ResourceMethod.BATCH_CREATE, ResourceMethod.BATCH_PARTIAL_UPDATE));
  // CreateOnly fields should not be specified for these types of requests
  private static final Set<ResourceMethod> createOnlyRestrictedMethods = new HashSet<ResourceMethod>(
      Arrays.asList(ResourceMethod.PARTIAL_UPDATE, ResourceMethod.BATCH_PARTIAL_UPDATE));
  // ReadOnly fields are treated as optional for these types of requests
  private static final Set<ResourceMethod> readOnlyOptional = new HashSet<ResourceMethod>(
      Arrays.asList(ResourceMethod.CREATE, ResourceMethod.BATCH_CREATE));

  // A path is ReadOnly if it satisfies this predicate
  private final Predicate _readOnlyPredicate;
  // A path is CreateOnly if it satisfies this predicate
  private final Predicate _createOnlyPredicate;
  // A path is a descendant of a ReadOnly field if it satisfies this predicate
  private final Predicate _readOnlyDescendantPredicate;
  // A path is a descendant of a CreateOnly field if it satisfies this predicate
  private final Predicate _createOnlyDescendantPredicate;
  private final Class<? extends RecordTemplate> _valueClass;
  private final ResourceMethod _resourceMethod;
  // To be passed into DataSchemaAnnotationValidator.
  private final Map<String, Class<? extends Validator>> _validatorClassMap;

  private static final String INSTANTIATION_ERROR = "InstantiationException while trying to instantiate the record template class";
  private static final String ILLEGAL_ACCESS_ERROR = "IllegalAccessException while trying to instantiate the record template class";
  private static final String TEMPLATE_RUNTIME_ERROR = "TemplateRuntimeException while trying to find the schema class";

  private static PathMatchesPatternPredicate stringToPredicate(String path, boolean includeDescendants)
  {
    // Discard the initial / character if present
    if (path.length() > 0 && path.charAt(0) == DataElement.SEPARATOR)
    {
      path = path.substring(1);
    }
    String[] components = path.split(DataElement.SEPARATOR.toString());
    int length = components.length + (includeDescendants ? 1 : 0);
    Object[] componentsWithWildcards = new Object[length];
    int i = 0;
    for (String component : components)
    {
      if (component.equals(PathSpec.WILDCARD)) // Treat * as wildcard even if it's not the same object as WILDCARD
      {
        componentsWithWildcards[i++] = Wildcard.ANY_ONE;
      }
      else
      {
        componentsWithWildcards[i++] = component;
      }
    }
    if (includeDescendants)
    {
      componentsWithWildcards[components.length] = Wildcard.ANY_ZERO_OR_MORE;
    }
    return new PathMatchesPatternPredicate(componentsWithWildcards);
  }

  private static Map<String, List<String>> annotationsToMap(Annotation[] annotations)
  {
    Map<String, List<String>> annotationMap = new HashMap<String, List<String>>();
    if (annotations != null)
    {
      for (Annotation annotation : annotations)
      {
        if (annotation.annotationType() == ReadOnly.class)
        {
          annotationMap.put(ReadOnly.class.getAnnotation(RestSpecAnnotation.class).name(),
                            Arrays.asList(((ReadOnly) annotation).value()));
        }
        else if (annotation.annotationType() == CreateOnly.class)
        {
          annotationMap.put(CreateOnly.class.getAnnotation(RestSpecAnnotation.class).name(),
                            Arrays.asList(((CreateOnly) annotation).value()));
        }
      }
    }
    return annotationMap;
  }

  /**
   * Constructor.
   *
   * @param annotations annotations on the resource class
   * @param valueClass class of the record template
   * @param resourceMethod resource method type
   */
  public RestLiDataValidator(Annotation[] annotations, Class<? extends RecordTemplate> valueClass, ResourceMethod resourceMethod)
  {
    this(annotations, valueClass, resourceMethod, Collections.emptyMap());
  }

  /**
   * Constructor.
   *
   * @param annotations annotations on the resource class
   * @param valueClass class of the record template
   * @param resourceMethod resource method type
   * @param validatorClassMap custom validator class map (see {@link #RestLiDataValidator(Map, Class, ResourceMethod, Map)} for explanation)
   */
  public RestLiDataValidator(Annotation[] annotations,
                             Class<? extends RecordTemplate> valueClass,
                             ResourceMethod resourceMethod,
                             Map<String, Class<? extends Validator>> validatorClassMap)
  {
    this(annotationsToMap(annotations), valueClass, resourceMethod, validatorClassMap);
  }

  /**
   * Constructor.
   *
   * @param annotations map from annotation name to annotation values
   * @param valueClass class of the record template
   * @param resourceMethod resource method type
   */
  public RestLiDataValidator(Map<String, List<String>> annotations,
                             Class<? extends RecordTemplate> valueClass,
                             ResourceMethod resourceMethod)
  {
    this(annotations, valueClass, resourceMethod, Collections.emptyMap());
  }

  /**
   * Constructor.
   *
   * @param annotations map from annotation name to annotation values
   * @param valueClass class of the record template
   * @param resourceMethod resource method type
   * @param validatorClassMap custom validator class map, with keys as "validate" property keys
   *                          (See {@link DataSchemaAnnotationValidator}) and values as validator class types
   *                          (e.g. "strlen" as key and StrlenValidator.class as value)
   */
  public RestLiDataValidator(Map<String, List<String>> annotations,
                             Class<? extends RecordTemplate> valueClass,
                             ResourceMethod resourceMethod,
                             Map<String, Class<? extends Validator>> validatorClassMap)
  {
    List<Predicate> readOnly = new ArrayList<Predicate>();
    List<Predicate> createOnly = new ArrayList<Predicate>();
    List<Predicate> readOnlyDescendant = new ArrayList<Predicate>();
    List<Predicate> createOnlyDescendant = new ArrayList<Predicate>();
    if (annotations != null)
    {
      for (Map.Entry<String, List<String>> entry : annotations.entrySet())
      {
        String annotationName = entry.getKey();
        if (annotationName.equals(ReadOnly.class.getAnnotation(RestSpecAnnotation.class).name())
            && readOnlyRestrictedMethods.contains(resourceMethod))
        {
          for (String path : entry.getValue())
          {
            readOnly.add(stringToPredicate(path, false));
            readOnlyDescendant.add(stringToPredicate(path, true));
          }
        }
        else if (annotationName.equals(CreateOnly.class.getAnnotation(RestSpecAnnotation.class).name())
            && createOnlyRestrictedMethods.contains(resourceMethod))
        {
          for (String path : entry.getValue())
          {
            createOnly.add(stringToPredicate(path, false));
            createOnlyDescendant.add(stringToPredicate(path, true));
          }
        }
      }
    }
    _readOnlyPredicate = Predicates.or(readOnly);
    _createOnlyPredicate = Predicates.or(createOnly);
    _readOnlyDescendantPredicate = Predicates.or(readOnlyDescendant);
    _createOnlyDescendantPredicate = Predicates.or(createOnlyDescendant);
    _valueClass = valueClass;
    _resourceMethod = resourceMethod;
    _validatorClassMap = Collections.unmodifiableMap(validatorClassMap);
  }

  private class DataValidator extends DataSchemaAnnotationValidator
  {
    private DataValidator(DataSchema schema)
    {
      super(schema, _validatorClassMap);
    }

    @Override
    public void validate(ValidatorContext context)
    {
      super.validate(context);
      DataElement element = context.dataElement();
      if (_readOnlyPredicate.evaluate(element))
      {
        context.addResult(new Message(element.path(), "ReadOnly field present in a %s request", _resourceMethod.toString()));
      }
      if (_createOnlyPredicate.evaluate(element))
      {
        context.addResult(new Message(element.path(), "CreateOnly field present in a %s request", _resourceMethod.toString()));
      }
    }
  }

  /**
   * Validate Rest.li data (single entity or patch).
   * Does not accept batch data.
   *
   * @deprecated This method only validates the input for create / batch create methods.
   * Use {@link #validateInput(RecordTemplate)}, {@link #validateInput(PatchRequest)} or {@link #validateOutput(RecordTemplate)} instead.
   * @param dataTemplate data to validate
   * @return validation result
   */
  @Deprecated
  public ValidationResult validate(DataTemplate<?> dataTemplate)
  {
    switch (_resourceMethod)
    {
      case PARTIAL_UPDATE:
      case BATCH_PARTIAL_UPDATE:
        return validatePatch((PatchRequest) dataTemplate);
      case CREATE:
      case BATCH_CREATE:
      case UPDATE:
      case BATCH_UPDATE:
        return validateInputEntity((RecordTemplate) dataTemplate);
      case GET:
      case BATCH_GET:
      case FINDER:
      case GET_ALL:
        return validateOutputEntity((RecordTemplate) dataTemplate, null);
      default:
        throw new IllegalArgumentException("Cannot perform Rest.li validation for " + _resourceMethod.toString());
    }
  }

  /**
   * Validate Rest.li input data (single entity).
   *
   * @param dataTemplate data to validate
   * @return validation result
   */
  public ValidationResult validateInput(RecordTemplate dataTemplate)
  {
    if (dataTemplate == null)
    {
      throw new IllegalArgumentException("Record template is null.");
    }
    if (dataTemplate.data() == null)
    {
      throw new IllegalArgumentException("Record template does not have data.");
    }
    if (dataTemplate.schema() == null)
    {
      throw new IllegalArgumentException("Record template does not have a schema.");
    }
    switch (_resourceMethod)
    {
      case CREATE:
      case BATCH_CREATE:
      case UPDATE:
      case BATCH_UPDATE:
        return validateInputEntity(dataTemplate);
      default:
        throw new IllegalArgumentException("Cannot perform Rest.li input (entity) validation for " + _resourceMethod.toString());
    }
  }

  /**
   * Validate Rest.li input data (single patch).
   *
   * @param patchRequest patch to validate
   * @return validation result
   */
  public ValidationResult validateInput(PatchRequest<?> patchRequest)
  {
    if (patchRequest == null)
    {
      throw new IllegalArgumentException("Patch request is null.");
    }
    if (patchRequest.getPatchDocument() == null)
    {
      throw new IllegalArgumentException("Patch request does not have a patch document.");
    }
    switch (_resourceMethod)
    {
      case PARTIAL_UPDATE:
      case BATCH_PARTIAL_UPDATE:
        return validatePatch(patchRequest);
      default:
        throw new IllegalArgumentException("Cannot perform Rest.li input (patch) validation for " + _resourceMethod.toString());
    }
  }

  /**
   * Validate Rest.li output data (single entity).
   *
   * @param dataTemplate data to validate
   * @return validation result
   */
  public ValidationResult validateOutput(RecordTemplate dataTemplate)
  {
    return validateOutput(dataTemplate, null);
  }

  public ValidationResult validateOutput(RecordTemplate dataTemplate, MaskTree projectionMask)
  {
    if (dataTemplate == null)
    {
      throw new IllegalArgumentException("Record template is null.");
    }
    if (dataTemplate.data() == null)
    {
      throw new IllegalArgumentException("Record template does not have data.");
    }
    switch (_resourceMethod)
    {
      case CREATE:
      case BATCH_CREATE:
      case GET:
      case BATCH_GET:
      case FINDER:
      case GET_ALL:
        return validateOutputEntity(dataTemplate, projectionMask);
      default:
        throw new IllegalArgumentException("Cannot perform Rest.li output validation for " + _resourceMethod.toString());
    }
  }

  /**
   * Checks that if the patch is applied to a valid entity, the modified entity will also be valid.
   * This method
   * (1) Checks that required/ReadOnly/CreateOnly fields are not deleted.
   * (2) Checks that new values for record templates contain all required fields.
   * (3) Applies the patch to an empty entity and validates the entity for custom validation rules
   * and Rest.li annotations (Allows required fields to be absent by using {@link RequiredMode#IGNORE},
   * because a patch does not necessarily contain all fields).
   *
   * NOTE: Updating a part of an array is not supported. So if the array contains a required field that is
   * readonly or createonly, the field cannot be present (no partial updates on readonly/createonly)
   * but cannot be absent either (no missing required fields). This means the array cannot be changed by a
   * partial update request. This is something that should be fixed.
   *
   * @param patchRequest the patch
   * @return the final validation result
   */
  private ValidationResult validatePatch(PatchRequest<?> patchRequest)
  {
    // Instantiate an empty entity.
    RecordTemplate entity;
    try
    {
      entity = _valueClass.newInstance();
    }
    catch (InstantiationException e)
    {
      return validationResultWithErrorMessage(INSTANTIATION_ERROR);
    }
    catch (IllegalAccessException e)
    {
      return validationResultWithErrorMessage(ILLEGAL_ACCESS_ERROR);
    }
    // Apply the patch to the entity and get paths that $set and $delete operations were performed on.
    @SuppressWarnings("unchecked")
    PatchRequest<RecordTemplate> patch = (PatchRequest<RecordTemplate>) patchRequest;
    DataComplexProcessor processor =
        new DataComplexProcessor(new Patch(true), patch.getPatchDocument(), entity.data());
    MessageList<Message> messages;
    try
    {
      messages = processor.runDataProcessing(false);
    }
    catch (DataProcessingException e)
    {
      return validationResultWithErrorMessage("Error while applying patch: " + e.getMessage());
    }
    ValidationErrorResult checkDeleteResult = new ValidationErrorResult();
    checkDeletesAreValid(entity.schema(), messages, checkDeleteResult);
    if (!checkDeleteResult.isValid())
    {
      return checkDeleteResult;
    }
    ValidationResult checkSetResult = checkNewRecordsAreNotMissingFields(entity, messages);
    if (checkSetResult != null)
    {
      return checkSetResult;
    }
    // Custom validation rules and Rest.li annotations for set operations are checked here.
    // It's okay if required fields are absent in a partial update request, so use ignore mode.
    return ValidateDataAgainstSchema.validate(new SimpleDataElement(entity.data(), entity.schema()),
        new ValidationOptions(RequiredMode.IGNORE), new DataValidator(entity.schema()));
  }

  private ValidationResult checkNewRecordsAreNotMissingFields(RecordTemplate entity, MessageList<Message> messages)
  {
    for (Message message : messages)
    {
      Object[] path = message.getPath();
      if (path[path.length - 1].toString().equals(PatchConstants.SET_COMMAND))
      {
        // Replace $set with the field name to get the full path
        path[path.length - 1] = message.getFormat();
        DataElement element = DataElementUtil.element(new SimpleDataElement(entity.data(), entity.schema()), path);
        ValidationResult result = ValidateDataAgainstSchema.validate(element, new ValidationOptions());
        if (!result.isValid())
        {
          return result;
        }
      }
    }
    return null;
  }

  /**
   * Create a hollow data element in which only getName() and getParent() work correctly.
   * This method is used to test $delete partial update paths against {@link PathMatchesPatternPredicate}.
   *
   * @param path the path from the root to the element, including the name of the element
   * @return a hollow data element
   */
  private static DataElement hollowElementFromPath(Object[] path)
  {
    DataElement root = new SimpleDataElement(null, null);
    DataElement current = root;
    for (Object component : path)
    {
      DataElement child = new SimpleDataElement(null, component.toString(), null, current);
      current = child;
    }
    return current;
  }

  private void checkDeletesAreValid(DataSchema schema, MessageList<Message> messages, ValidationErrorResult result)
  {
    for (Message message : messages)
    {
      Object[] path = message.getPath();
      if (path[path.length - 1].toString().equals(PatchConstants.DELETE_COMMAND))
      {
        // Replace $delete with the field name to get the full path
        path[path.length - 1] = message.getFormat();
        RecordDataSchema.Field field = DataSchemaUtil.getField(schema, path);
        if (field != null && !field.getOptional() && field.getDefault() == null)
        {
          result.addMessage(new Message(path, "cannot delete a required field"));
        }
        DataElement fakeElement = hollowElementFromPath(path);
        if (_readOnlyDescendantPredicate.evaluate(fakeElement))
        {
          result.addMessage(new Message(path, "cannot delete a ReadOnly field or its descendants"));
        }
        else if (_createOnlyDescendantPredicate.evaluate(fakeElement))
        {
          result.addMessage(new Message(path, "cannot delete a CreateOnly field or its descendants"));
        }
      }
    }
  }

  private ValidationResult validateInputEntity(RecordTemplate entity)
  {
    ValidationOptions validationOptions = new ValidationOptions();
    if (readOnlyOptional.contains(_resourceMethod))
    {
      // Even if ReadOnly fields are non-optional, the client cannot supply them in a create request, so they should be treated as optional.
      validationOptions.setTreatOptional(_readOnlyPredicate);
    }
    ValidationResult result = ValidateDataAgainstSchema.validate(entity, validationOptions, new DataValidator(entity.schema()));
    return result;
  }

  private ValidationResult validateOutputEntity(RecordTemplate entity, MaskTree projectionMask)
  {
    try
    {
      // Value class from resource model is the only source of truth for record schema.
      // Schema from the record template itself should not be used.
      DataSchema originalSchema = DataTemplateUtil.getSchema(_valueClass);

      // When a projection is defined, we only need to validate the projected fields.
      DataSchema validatingSchema = (projectionMask != null) ?
        buildSchemaByProjection(originalSchema, projectionMask.getDataMap()) : originalSchema;

      DataSchemaAnnotationValidator validator = new DataSchemaAnnotationValidator(validatingSchema);
      return ValidateDataAgainstSchema.validate(entity.data(), validatingSchema, new ValidationOptions(), validator);
    }
    catch (TemplateRuntimeException e)
    {
      return validationResultWithErrorMessage(TEMPLATE_RUNTIME_ERROR);
    }
  }

  /**
   * Build a new schema that contains only the projected fields from the original schema recursively.
   * @param schema schema to build from
   * @param maskMap projection mask data map
   * @return new schema containing only projected fields
   */
  private static DataSchema buildSchemaByProjection(DataSchema schema, DataMap maskMap) {
    if (maskMap == null || maskMap.isEmpty()) {
      throw new IllegalArgumentException("Invalid projection masks.");
    }

    if (schema instanceof RecordDataSchema) {
      return buildRecordDataSchemaByProjection((RecordDataSchema) schema, maskMap);
    } else if (schema instanceof UnionDataSchema) {
      return buildUnionDataSchemaByProjection((UnionDataSchema) schema, maskMap);
    } else if (schema instanceof ArrayDataSchema) {
      return buildArrayDataSchemaByProjection((ArrayDataSchema) schema, maskMap);
    } else if (schema instanceof MapDataSchema) {
      return buildMapDataSchemaByProjection((MapDataSchema) schema, maskMap);
    } else if (schema instanceof TyperefDataSchema) {
      return buildTyperefDataSchemaByProjection((TyperefDataSchema) schema, maskMap);
    }

    throw new IllegalArgumentException("Unexpected data schema type: " + schema);
  }

  /**
   * Build a new {@link TyperefDataSchema} schema that contains only the masked fields.
   */
  private static TyperefDataSchema buildTyperefDataSchemaByProjection(TyperefDataSchema originalSchema, DataMap maskMap) {
    TyperefDataSchema newSchema = new TyperefDataSchema(new Name(originalSchema.getFullName()));
    if (originalSchema.getProperties() != null) {
      newSchema.setProperties(originalSchema.getProperties());
    }
    if (originalSchema.getDoc() != null) {
      newSchema.setDoc(originalSchema.getDoc());
    }
    if (originalSchema.getAliases() != null) {
      newSchema.setAliases(originalSchema.getAliases());
    }
    DataSchema newRefSchema = buildSchemaByProjection(originalSchema.getRef(), maskMap);
    newSchema.setReferencedType(newRefSchema);
    return newSchema;
  }

  /**
   * Build a new {@link MapDataSchema} schema that contains only the masked fields.
   */
  private static MapDataSchema buildMapDataSchemaByProjection(MapDataSchema originalSchema, DataMap maskMap) {
    if (maskMap.containsKey(FilterConstants.WILDCARD)) {
      DataSchema newValuesSchema = reuseOrBuildDataSchema(originalSchema.getValues(), maskMap.get(FilterConstants.WILDCARD));
      MapDataSchema newSchema = new MapDataSchema(newValuesSchema);
      if (originalSchema.getProperties() != null) {
        newSchema.setProperties(originalSchema.getProperties());
      }
      return newSchema;
    }

    throw new IllegalArgumentException("Missing wildcard key in projection mask: " + maskMap.keySet());
  }

  /**
   * Build a new {@link ArrayDataSchema} schema that contains only the masked fields.
   */
  private static ArrayDataSchema buildArrayDataSchemaByProjection(ArrayDataSchema originalSchema, DataMap maskMap) {
    if (maskMap.containsKey(FilterConstants.WILDCARD)) {
      DataSchema newItemsSchema = reuseOrBuildDataSchema(originalSchema.getItems(), maskMap.get(FilterConstants.WILDCARD));
      ArrayDataSchema newSchema = new ArrayDataSchema(newItemsSchema);
      if (originalSchema.getProperties() != null) {
        newSchema.setProperties(originalSchema.getProperties());
      }
      return newSchema;
    }

    throw new IllegalArgumentException("Missing wildcard key in projection mask: " + maskMap.keySet());
  }

  /**
   * Build a new {@link UnionDataSchema} schema that contains only the masked fields.
   */
  private static UnionDataSchema buildUnionDataSchemaByProjection(UnionDataSchema originalSchema, DataMap maskMap) {
    List<DataSchema> newUnionTypeSchemas = new ArrayList<>();

    // Get the wildcard mask if one is available
    Object wildcardMask = maskMap.get(FilterConstants.WILDCARD);

    for (DataSchema member: originalSchema.getTypes()) {
      Object maskValue = maskMap.get(member.getUnionMemberKey());
      // If a mask is available for this specific member use that, else use the wildcard mask if that is available
      if (maskValue != null) {
        newUnionTypeSchemas.add(reuseOrBuildDataSchema(member, maskValue));
      } else if (wildcardMask != null) {
        newUnionTypeSchemas.add(reuseOrBuildDataSchema(member, wildcardMask));
      }
    }

    UnionDataSchema newSchema = new UnionDataSchema();
    newSchema.setTypes(newUnionTypeSchemas, new StringBuilder());
    if (originalSchema.getProperties() != null) {
      newSchema.setProperties(originalSchema.getProperties());
    }
    return newSchema;
  }

  /**
   * Build a new {@link RecordDataSchema} schema that contains only the masked fields.
   */
  private static RecordDataSchema buildRecordDataSchemaByProjection(RecordDataSchema originalSchema, DataMap maskMap) {
    RecordDataSchema newRecordSchema = new RecordDataSchema(new Name(originalSchema.getFullName()), RecordDataSchema.RecordType.RECORD);
    List<RecordDataSchema.Field> newFields = new ArrayList<RecordDataSchema.Field>();
    for (Map.Entry<String, Object> maskEntry : maskMap.entrySet()) {
      RecordDataSchema.Field originalField = originalSchema.getField(maskEntry.getKey());
      DataSchema fieldSchemaToUse = reuseOrBuildDataSchema(originalField.getType(), maskEntry.getValue());
      RecordDataSchema.Field newField = buildRecordField(originalField, fieldSchemaToUse, newRecordSchema);
      newFields.add(newField);
    }

    // Fields from 'include' are no difference from other fields from original schema,
    // therefore, we are not calling newRecordSchema.setInclude() here.
    newRecordSchema.setFields(newFields, new StringBuilder()); // No errors are expected here, as the new schema is merely subset of the original
    if (originalSchema.getAliases() != null) {
      newRecordSchema.setAliases(originalSchema.getAliases());
    }
    if (originalSchema.getDoc() != null) {
      newRecordSchema.setDoc(originalSchema.getDoc());
    }
    if (originalSchema.getProperties() != null) {
      newRecordSchema.setProperties(originalSchema.getProperties());
    }
    return newRecordSchema;
  }

  /**
   * The maskValue from a rest.li projection mask is expected to be either:
   * 1) Integer that has value 1, which means all fields in the original schema are projected (negative projection not supported)
   * 2) DataMap, which means only selected fields in the original schema are projected
   */
  private static DataSchema reuseOrBuildDataSchema(DataSchema originalSchema, Object maskValue) {
    if (maskValue instanceof Integer && maskValue.equals(FilterConstants.POSITIVE)) {
      return originalSchema;
    } else if (maskValue instanceof DataMap) {
      return buildSchemaByProjection(originalSchema, (DataMap) maskValue);
    }
    throw new IllegalArgumentException("Expected mask value to be either positive mask op or DataMap: " + maskValue);
  }

  /**
   * Build a new record field with a new projected field schema.
   * All other properties are copied over from the originalField.
   */
  private static RecordDataSchema.Field buildRecordField(RecordDataSchema.Field originalField,
                                                         DataSchema fieldSchemaToReplace,
                                                         RecordDataSchema recordSchemaToReplace) {
    RecordDataSchema.Field newField = new RecordDataSchema.Field(fieldSchemaToReplace);
    if (originalField.getAliases() != null) {
      // No errors are expected here, as the new schema is merely subset of the original
      newField.setAliases(originalField.getAliases(), new StringBuilder());
    }
    if (originalField.getDefault() != null) {
      newField.setDefault(originalField.getDefault());
    }
    if (originalField.getDoc() != null) {
      newField.setDoc(originalField.getDoc());
    }
    if (originalField.getName() != null) {
      // No errors are expected here, as the new schema is merely subset of the original
      newField.setName(originalField.getName(), new StringBuilder());
    }
    if (originalField.getOrder() != null) {
      newField.setOrder(originalField.getOrder());
    }
    if (originalField.getProperties() != null) {
      newField.setProperties(originalField.getProperties());
    }
    newField.setRecord(recordSchemaToReplace);
    newField.setOptional(originalField.getOptional());
    return newField;
  }

  private static ValidationErrorResult validationResultWithErrorMessage(String errorMessage)
  {
    ValidationErrorResult result = new ValidationErrorResult();
    result.addMessage(new Message(new Object[]{}, errorMessage));
    return result;
  }

  private static class ValidationErrorResult implements ValidationResult
  {
    private MessageList<Message> _messages;

    private ValidationErrorResult()
    {
      _messages = new MessageList<Message>();
    }

    @Override
    public boolean hasFix()
    {
      return false;
    }

    @Override
    public boolean hasFixupReadOnlyError()
    {
      return false;
    }

    @Override
    public Object getFixed()
    {
      return null;
    }

    @Override
    public boolean isValid()
    {
      return _messages.isEmpty();
    }

    public void addMessage(Message message)
    {
      _messages.add(message);
    }

    @Override
    public Collection<Message> getMessages()
    {
      return _messages;
    }
  }
}
