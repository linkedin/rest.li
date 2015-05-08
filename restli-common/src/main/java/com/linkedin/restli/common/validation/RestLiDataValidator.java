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


import com.linkedin.data.element.DataElement;
import com.linkedin.data.element.DataElementUtil;
import com.linkedin.data.element.SimpleDataElement;
import com.linkedin.data.message.Message;
import com.linkedin.data.message.MessageList;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import com.linkedin.data.schema.validation.RequiredMode;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.schema.validator.DataSchemaAnnotationValidator;
import com.linkedin.data.schema.validator.ValidatorContext;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.TemplateRuntimeException;
import com.linkedin.data.transform.DataComplexProcessor;
import com.linkedin.data.transform.DataProcessingException;
import com.linkedin.data.transform.patch.Patch;
import com.linkedin.data.transform.patch.PatchConstants;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.restspec.RestSpecAnnotation;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
 * It can also validate patches, in which case only rules 2 and 3 are applied.<br>
 * <p>
 * Rest.li annotations should be used on top of the resource and should specify paths in the following format:
 * <ul>
 *   <li> For a non-nested field, put the field name. e.g. "stringA"
 *   <li> For a nested field, put the full path separated by / characters. e.g. "location/latitude"
 *   <li> For a field of an array item, specify the array name followed by the field name. e.g. "ArrayWithInlineRecord/bar1" for
 *   <code>{"name": "ArrayWithInlineRecord", "type": {"type": "array", "items":
 *   {"type": "record", "name": "myItem", "fields": [ {"name": "bar1", "type": "string"}, {"name": "bar2", "type": "string"} ] }}}</code>
 *   <li> Similarly, for a field of a map value, specify the map name followed by the field name.
 *   <li> For a field of a record inside a union, specify the union name, followed by the fully qualified record schema name,
 *        and then the field name. e.g. "UnionFieldWithInlineRecord/com.linkedin.restli.examples.greetings.api.myRecord/foo2"
 * </ul>
 * Because full paths are listed, different rules can be specified for records that have the same schema.
 * For example, if the schema contains two Photos, you can make the id of photo1 ReadOnly and id of photo2 non-ReadOnly.
 * This is different from the optional/required distinction where if the id of photo1 is required, the id of photo2 will also be required.
 * Since the array indices and map keys are omitted from the paths, the same rule will apply to all items in the same array or map.
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

  private final Set<String> _readOnlyPaths;
  private final Set<String> _createOnlyPaths;
  private final Class<? extends RecordTemplate> _valueClass;
  private final ResourceMethod _resourceMethod;

  private static final String INSTANTIATION_ERROR = "InstantiationException while trying to instantiate the record template class";
  private static final String ILLEGAL_ACCESS_ERROR = "IllegalAccessException while trying to instantiate the record template class";
  private static final String TEMPLATE_RUNTIME_ERROR = "TemplateRuntimeException while trying to find the schema class";

  /**
   * Constructor.
   *
   * @param annotations annotations on the resource class
   * @param valueClass class of the record template
   * @param resourceMethod resource method type
   */
  public RestLiDataValidator(Annotation[] annotations, Class<? extends RecordTemplate> valueClass, ResourceMethod resourceMethod)
  {
    _readOnlyPaths = new HashSet<String>();
    _createOnlyPaths = new HashSet<String>();
    if (annotations != null)
    {
      for (Annotation annotation : annotations)
      {
        if (annotation.annotationType() == ReadOnly.class)
        {
          _readOnlyPaths.addAll(Arrays.asList(((ReadOnly) annotation).value()));
        }
        else if (annotation.annotationType() == CreateOnly.class)
        {
          _createOnlyPaths.addAll(Arrays.asList(((CreateOnly) annotation).value()));
        }
      }
    }
    _valueClass = valueClass;
    _resourceMethod = resourceMethod;
  }

  /**
   * Constructor.
   *
   * @param annotations map from annotation name to annotation values
   * @param valueClass class of the record template
   * @param resourceMethod resource method type
   */
  public RestLiDataValidator(Map<String, List<String>> annotations, Class<? extends RecordTemplate> valueClass, ResourceMethod resourceMethod)
  {
    _readOnlyPaths = new HashSet<String>();
    _createOnlyPaths = new HashSet<String>();
    if (annotations != null)
    {
      for (Map.Entry<String, List<String>> entry : annotations.entrySet())
      {
        String annotationName = entry.getKey();
        if (annotationName.equals(ReadOnly.class.getAnnotation(RestSpecAnnotation.class).name()))
        {
          _readOnlyPaths.addAll(entry.getValue());
        }
        else if (annotationName.equals(CreateOnly.class.getAnnotation(RestSpecAnnotation.class).name()))
        {
          _createOnlyPaths.addAll(entry.getValue());
        }
      }
    }
    _valueClass = valueClass;
    _resourceMethod = resourceMethod;
  }

  private class DataValidator extends DataSchemaAnnotationValidator
  {
    private DataValidator(DataSchema schema)
    {
      super(schema);
    }

    @Override
    public void validate(ValidatorContext context)
    {
      super.validate(context);
      DataElement element = context.dataElement();
      String path = DataElementUtil.pathWithoutKeysAsString(element);
      if (path.length() > 0)
      {
        path = path.substring(1);
        if (readOnlyRestrictedMethods.contains(_resourceMethod) && _readOnlyPaths.contains(path))
        {
          context.addResult(new Message(element.path(), "ReadOnly field present in a %s request", _resourceMethod.toString()));
        }
        if (createOnlyRestrictedMethods.contains(_resourceMethod) && _createOnlyPaths.contains(path))
        {
          context.addResult(new Message(element.path(), "CreateOnly field present in a %s request", _resourceMethod.toString()));
        }
      }
    }
  }

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
        return validateInputEntity(dataTemplate);
      case GET:
      case BATCH_GET:
      case FINDER:
      case GET_ALL:
        return validateOutputEntity(dataTemplate);
      default:
        throw new IllegalArgumentException("Cannot perform Rest.li validation for " + _resourceMethod.toString());
    }
  }

  /**
   * Applies the patch to the data, and also returns the list of paths that were attempted to be deleted.
   *
   * @param original original record.
   * @param patch patch request.
   * @param <T> class of record.
   * @return list of paths for fields that were attempted to be deleted by the patch.
   * @throws DataProcessingException
   */
  private static <T extends RecordTemplate> List<String> applyPatchAndReturnDeletes
      (T original, PatchRequest<T> patch) throws DataProcessingException
  {
    DataComplexProcessor processor =
        new DataComplexProcessor(new Patch(true), patch.getPatchDocument(), original.data());
    MessageList<Message> messages = processor.runDataProcessing(false);
    DataSchema dataSchema = original.schema();
    List<String> deletedFields = new ArrayList<String>();
    for (Message message : messages)
    {
      StringBuilder sb = new StringBuilder();
      List<Object> path = new ArrayList<Object>(Arrays.asList(message.getPath()));
      // Replace the final $delete with the field name to get the full path
      path.remove(PatchConstants.DELETE_COMMAND);
      path.add(message.getFormat());
      // Use the information from data schema to remove map key names from the path
      for (Object component : path)
      {
        // Clear possible typerefs
        dataSchema = dataSchema.getDereferencedDataSchema();

        String comp = component.toString();
        if (dataSchema.getType() == DataSchema.Type.MAP)
        {
          // Parent is a map, so current component is a key name (should not be included in the final path)
          dataSchema = ((MapDataSchema) dataSchema).getValues();
          continue;
        }
        else if (dataSchema.getType() == DataSchema.Type.RECORD)
        {
          dataSchema = ((RecordDataSchema) dataSchema).getField(comp).getType();
        }
        else if (dataSchema.getType() == DataSchema.Type.UNION)
        {
          dataSchema = ((UnionDataSchema) dataSchema).getType(comp);
        }
        if (sb.length() > 0)
        {
          sb.append(DataElement.SEPARATOR);
        }
        sb.append(comp);
      }
      deletedFields.add(sb.toString());
    }
    return deletedFields;
  }

  private ValidationResult validatePatch(PatchRequest<?> patchRequest)
  {
    RecordTemplate record;
    try
    {
      record = _valueClass.newInstance();
      @SuppressWarnings("unchecked")
      PatchRequest<RecordTemplate> patch = (PatchRequest<RecordTemplate>) patchRequest;
      List<String> deletePaths = applyPatchAndReturnDeletes(record, patch);
      ValidationErrorResult result = new ValidationErrorResult();
      if (readOnlyRestrictedMethods.contains(_resourceMethod))
      {
        for (String path : deletePaths)
        {
          for (String readOnlyPath : _readOnlyPaths)
          {
            if (path.startsWith(readOnlyPath))
            {
              result.addMessage(new Message(path.split(DataElement.SEPARATOR.toString()), "delete operation on a ReadOnly field is forbidden"));
            }
          }
        }
      }
      if (createOnlyRestrictedMethods.contains(_resourceMethod))
      {
        for (String path : deletePaths)
        {
          for (String createOnlyPath : _createOnlyPaths)
          {
            if (path.startsWith(createOnlyPath))
            {
              result.addMessage(new Message(path.split(DataElement.SEPARATOR.toString()), "delete operation on a CreateOnly field is forbidden"));
            }
          }
        }
      }
      if (!result.isValid())
      {
        return result;
      }
    }
    catch (InstantiationException e)
    {
      return validationResultWithErrorMessage(INSTANTIATION_ERROR);
    }
    catch (IllegalAccessException e)
    {
      return validationResultWithErrorMessage(ILLEGAL_ACCESS_ERROR);
    }
    catch (DataProcessingException e)
    {
      return validationResultWithErrorMessage("Error while applying patch: " + e.getMessage());
    }
    // It's okay if required fields are absent in a partial update request, so use ignore mode.
    return ValidateDataAgainstSchema.validate(new SimpleDataElement(record.data(), record.schema()),
        new ValidationOptions(RequiredMode.IGNORE), new DataValidator(record.schema()));
  }

  private ValidationResult validateInputEntity(DataTemplate<?> entity)
  {
    Set<String> optionalFields = new HashSet<String>();
    if (readOnlyOptional.contains(_resourceMethod))
    {
      // Even if ReadOnly fields are non-optional, the client cannot supply them in a create request, so they should be treated as optional.
      optionalFields.addAll(_readOnlyPaths);
    }
    ValidationOptions validationOptions = new ValidationOptions();
    validationOptions.setOptionalFields(optionalFields);
    ValidationResult result = ValidateDataAgainstSchema.validate(entity, validationOptions, new DataValidator(entity.schema()));
    return result;
  }

  private ValidationResult validateOutputEntity(DataTemplate<?> entity)
  {
    try
    {
      DataSchema schema;
      if (_resourceMethod == ResourceMethod.BATCH_GET)
      {
        schema = entity.schema();
      }
      else
      {
        // The output entity is an AnyRecord and does not have the schema information.
        schema = DataTemplateUtil.getSchema(_valueClass);
      }
      return ValidateDataAgainstSchema.validate(entity.data(), schema, new ValidationOptions(), new DataSchemaAnnotationValidator(schema));
    }
    catch (TemplateRuntimeException e)
    {
      return validationResultWithErrorMessage(TEMPLATE_RUNTIME_ERROR);
    }
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
