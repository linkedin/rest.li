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

package com.linkedin.data.schema.validator;

import com.linkedin.data.DataMap;
import com.linkedin.data.element.DataElement;
import com.linkedin.data.element.SimpleDataElement;
import com.linkedin.data.message.Message;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;
import java.util.Map;


/**
 * Validator for AnyRecord.
 *
 * The {@link AnyRecordValidator.Parameter} provides application specific parameters
 * to the {@link AnyRecordValidator}. Its {@code resolver} attribute allows the application to provide
 * a {@link DataSchemaResolver}. Depending the application's needs, the resolver
 * may obtain schemas from a schema registry or from generated {@link com.linkedin.data.template.DataTemplate}
 * classes {@link com.linkedin.data.schema.resolver.ClassNameDataSchemaResolver}.
 * The {@code validSchema} attribute specifies whether unresolvable or invalid schemas
 * is permitted.
 *
 * This {@link Validator} performs the following validations.
 * <ul>
 *   <li>The value must be a {@link DataMap} with a single entry.</li>
 *   <li>The entry's value is also a {@link DataMap}.</li>
 *   <li>
 *     The entry's key provides the name of the schema.
 *     The validator resolves the schema name to a {@link NamedDataSchema} using the
 *     resolver provided by {@link com.linkedin.data.schema.validator.AnyRecordValidator.Parameter#resolver()}
 *     (if a resolver is provided).
 *     Then it constructs a {@link DataSchemaAnnotationValidator} for the resolved {@link NamedDataSchema}.
 *     If the {@link com.linkedin.data.schema.validator.AnyRecordValidator.Parameter#isValidSchema()}
 *     flag is set, then validation fails if a {@link DataSchemaAnnotationValidator} cannot be constructed
 *     and error messages will be emitted to explain the failure.
 *     If the flag is not set, then further validation is not performed and the value passes validation
 *     and informational messages will be emitted to indicate the entry's value is not validated against
 *     the entry's type including possible reasons for why the schema name cannot be resolved
 *     or why a {@link DataSchemaAnnotationValidator} cannot be constructed.
 *   </li>
 *   <li>
 *     If a {@link DataSchemaAnnotationValidator} has been constructed successfully, validate the
 *     entry's value using the {@link NamedDataSchema} obtained from the resolver, and the constructed
 *     {@link DataSchemaAnnotationValidator}.
 *   </li>
 * </ul>
 */
public class AnyRecordValidator  extends AbstractValidator
{
  private static final String SCHEMA_NAME = TestAnyRecordValidator.ANYRECORD_SCHEMA_FULLNAME;
  private static final String PARAMETER_KEY = AnyRecordValidator.class.getName();

  public static class Parameter
  {
    public static Parameter DEFAULT = new Parameter();

    private final boolean _validSchema;
    private final DataSchemaResolver _resolver;

    public Parameter()
    {
      this(false, null);
    }

    public Parameter(boolean validSchema, DataSchemaResolver resolver)
    {
      _validSchema = validSchema;
      _resolver = resolver;
    }

    /**
     * Whether it is mandatory to validate all any value with its type.
     *
     * This implies that the schema for type must be resolvable and
     * a {@link DataSchemaAnnotationValidator} can be instantiated to
     * process the "validate" properties of this schema.
     */
    boolean isValidSchema()
    {
      return _validSchema;
    }

    DataSchemaResolver resolver()
    {
      return _resolver;
    }
  }

  /**
   * Sets a parameter into {@link ValidationOptions} to be passed to instances of this class.
   *
   * @param validationOptions provides the {@link ValidationOptions} that will receive the parameter.
   * @param parameter provides the parameter to set.
   */
  public static void setParameter(ValidationOptions validationOptions, Parameter parameter)
  {
    validationOptions.setValidatorParameter(PARAMETER_KEY, parameter);
  }

  /**
   * Returns the parameter set into {@link ValidationOptions} to be passed to instances of this class.
   *
   * @param validationOptions provides the {@link ValidationOptions} to obtain the parameter from.
   * @return the parameter previously set or null if not set.
   */
  public static Parameter getParameter(ValidationOptions validationOptions)
  {
    return (Parameter) validationOptions.getValidatorParameter(PARAMETER_KEY);
  }

  public AnyRecordValidator(DataMap config)
  {
    super(config);
  }

  @Override
  public void validate(ValidatorContext context)
  {
    DataElement dataElement = context.dataElement();
    Object value = dataElement.getValue();
    DataSchema schema = dataElement.getSchema();
    if (schema.getType() != DataSchema.Type.RECORD ||
        (((RecordDataSchema) schema).getFullName()).equals(SCHEMA_NAME) == false)
    {
      context.addResult(new Message(context.dataElement().path(),
                                    "%1$s invoked on schema that is not %2$s",
                                    AnyRecordValidator.class.getName(), SCHEMA_NAME));
    }
    else if (value.getClass() != DataMap.class)
    {
      context.addResult(new Message(context.dataElement().path(),
                                    "%1$s expects data to be a DataMap, data is %2$s",
                                    AnyRecordValidator.class.getName(), value));

    }
    else
    {
      DataMap dataMap = (DataMap) value;
      if (dataMap.size() != 1)
      {
        context.addResult(new Message(context.dataElement().path(),
                                      "%1$s expects data to be a DataMap with one entry, data is %2$s",
                                      AnyRecordValidator.class.getName(), value));
      }
      else
      {
        Map.Entry<String, Object> entry = dataMap.entrySet().iterator().next();
        String anySchemaName = entry.getKey();
        Object anyValue = entry.getValue();
        DataSchema anySchema = schemaFromName(context, anySchemaName);
        if (anySchema != null)
        {
          DataElement anyElement = new SimpleDataElement(anyValue, entry.getKey(), anySchema, dataElement);
          // do we want to have cache for anySchemaName to validator
          // do we care about classMap argument to DataSchemaAnnotationValidator
          DataSchemaAnnotationValidator validator = new DataSchemaAnnotationValidator(anySchema);
          if (validator.isInitOk() == false)
          {
            boolean errorIfNotValidated = getParameter(context.validationOptions()).isValidSchema();
            context.addResult(new Message(context.dataElement().path(),
                                          errorIfNotValidated,
                                          "%1$s failed to initialize %2$s with %3$s",
                                          AnyRecordValidator.class.getName(),
                                          DataSchemaAnnotationValidator.class.getSimpleName(),
                                          anySchema));
            addResult(context, errorIfNotValidated, validator.getInitMessages());
          }
          else
          {
            ValidationResult result = ValidateDataAgainstSchema.validate(anyElement, context.validationOptions(), validator);
            addResult(context, result.getMessages());
            if (result.hasFix())
              context.setHasFix(true);
            if (result.hasFixupReadOnlyError())
              context.setHasFixupReadOnlyError(true);
          }
        }
      }
    }
  }

  protected DataSchema schemaFromName(ValidatorContext context, String schemaName)
  {
    StringBuilder sb = new StringBuilder();
    Parameter parameter = getParameter(context.validationOptions());
    DataSchemaResolver resolver = parameter.resolver();
    NamedDataSchema schema;
    if (resolver == null)
    {
      schema = null;
      context.addResult(new Message(context.dataElement().path(schemaName),
                                    parameter.isValidSchema(),
                                    "%1$s cannot obtain schema for \"%2$s\", no resolver",
                                    AnyRecordValidator.class.getName(), schemaName));
    }
    else
    {
      schema = resolver.findDataSchema(schemaName, sb);
      if (schema == null)
      {
        context.addResult(new Message(context.dataElement().path(schemaName),
                                      parameter.isValidSchema(),
                                      "%1$s cannot obtain schema for \"%2$s\" (%3$s)",
                                      AnyRecordValidator.class.getName(), schemaName, sb.toString()));
      }
    }
    return schema;
  }

  private static void addResult(ValidatorContext context, Iterable<Message> messages)
  {
    for (Message message : messages)
    {
      context.addResult(message);
    }
  }

  private static void addResult(ValidatorContext context, boolean error, Iterable<Message> messages)
  {
    for (Message message : messages)
    {
      context.addResult(error ? message.asErrorMessage() : message.asInfoMessage());
    }
  }

}
