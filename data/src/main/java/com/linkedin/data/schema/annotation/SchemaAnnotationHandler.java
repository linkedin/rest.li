/*
   Copyright (c) 2019 LinkedIn Corp.

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
package com.linkedin.data.schema.annotation;

import com.linkedin.data.message.Message;
import com.linkedin.data.message.MessageList;
import com.linkedin.data.schema.DataSchema;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;


/**
 * Interface provided for applications to implement their pluggable annotation handler to handle
 * custom Schema annotation overrides and validation
 *
 * This interface will be triggered by {@link SchemaAnnotationProcessor}
 *
 * Each implementation of the handler is expected to handle one annotation namespace
 *
 *   e.g. Take an example of the annotation namespace "customAnnotation" below
 *
 *   pdsc:
 *   <pre>
 *   ...
 *   "fields" : [ {
 *    "name" : "exampleField",
 *    "type" : "string",
 *    "customAnnotation" : "None"
 *   }],
 *   ...
 *   </pre>
 *
 *   pdl:
 *   <pre>
 *   ...
 *   {@literal @}customAnnotation="None"
 *   exampleField:string
 *   ...
 *   </pre>
 *
 *   "customAnnotation" is an annotation namespace whose annotation will be handled by the handler.
 *
 * Each implementation of the handler is expected to
 * (1) Resolve a chain of overridden properties to its correct final values
 * (2) provide a validate function to validate on each schema that will be traversed in the schema, for this annotation namespace
 *
 */
public interface SchemaAnnotationHandler
{

  /**
   * This method should implement logic to resolve correct properties, when overrides were seen.
   * @param propertiesOverrides : List of overrides from upper level properties for the given annotation namespace
   *                              The first element of the pair would be the schema PathSpec,
   *                              the second element is the overridden Object
   *                              overridden object can be an {@link com.linkedin.data.DataComplex}, or primitive type
   *                              For example, for the example schema below, the list would be
   *                              {["/f1/f2", "2rd layer"], ["/f2", "1st layer"], ["", OriginalValue]}
   *
   * <p><pre>
   * &#064;customAnnotation= {"/f1/f2" : "2rd layer" }
   * f: record rcd {
   *     &#064;customAnnotation= {"/f2" : "1rd layer" }
   *     f1: record rcd2 {
   *         &#064;customAnnotation = "OriginalValue"
   *         f2: string
   *     }
   * }
   * </pre></p>
   *
   * @param resolutionMetadata : some metadata that can help handler resolve values.
   *
   * @return ResolutionResult containing resolved data, or error messages if failed.
   *          It has the resolved properties in a map, which eventually merge to
   *          populate the "resolvedProperties" for a dataSchema.
   *          The entries will be merged as they are, to the entries in the "resolvedProperties" for the dataSchema.
   *
   *          For example in this function, if one handler returns {"customAnnotation1": "NONE"},
   *          and another handler returns {"customAnnotation2": "NONE"}
   *
   *          Should expect to see below entries in the resolvedProperties for the dataSchema.
   *          {
   *            ...
   *            "customAnnotation1": "NONE",
   *            "customAnnotation2": "NONE"
   *            ...
   *          }
   *
   * @see ResolutionResult
   *
   */
  ResolutionResult resolve(List<Pair<String, Object>> propertiesOverrides, ResolutionMetaData resolutionMetadata);

  /**
   * Getter for the annotationNamespace value that this {@link SchemaAnnotationHandler} should handle
   * @return annotationNamespace
   */
  String getAnnotationNamespace();

  /**
   *
   * Validation function to implement to validate on the DataSchema's resolvedProperties
   *
   * @param paths SchemaPathSpec for the dataSchema under validation
   * @param resolvedProperties the resolvedProperties for the schema to be validated
   * @param dataSchema the dataSchema under validation
   * @param metaData metaData to give to validator
   * @return FieldAnnotationValidationResult
   */
  AnnotationValidationResult validate(List<String> paths,
                                      Map<String, Object> resolvedProperties,
                                      DataSchema dataSchema,
                                      ValidationMetaData metaData);

  /**
   * Result the {@link #resolve(List, ResolutionMetaData)} function should return after it is called
   *
   */
  class ResolutionResult
  {
    public boolean isError()
    {
      return _isError;
    }

    public void setError(boolean error)
    {
      _isError = error;
    }

    public MessageList<Message> getMessages()
    {
      return _messages;
    }

    public void setMessages(MessageList<Message> messages)
    {
      _messages = messages;
    }

    public void addMessage(List<String> path, String format, Object... args)
    {
      _messages.add(new Message(path.toArray(), format, args));
    }

    public Map<String, Object> getResolvedResult()
    {
      return _resolvedResult;
    }

    public void setResolvedResult(Map<String, Object> resolvedResult)
    {
      _resolvedResult = resolvedResult;
    }

    boolean _isError = false;
    MessageList<Message> _messages;
    /**
     * This value stores the resolved result that will be merged to resolvedProperties of the DataSchema under traversal
     *
     * The key should be the annotation namespace, below is an example
     * <pre>
     * {
     *    "customAnnotation": <ResolvedResult>
     * }
     * </pre>
     *
     */
    Map<String, Object> _resolvedResult = Collections.emptyMap();
  }

  /**
   * Result the {@link #validate(List, Map, DataSchema, ValidationMetaData)} function should return after it is called
   *
   */
  class AnnotationValidationResult
  {
    public boolean isValid()
    {
      return _isValid;
    }

    public void setValid(boolean valid)
    {
      _isValid = valid;
    }

    public List<String> getPaths()
    {
      return _paths;
    }

    public void setPaths(List<String> paths)
    {
      _paths = paths;
    }

    public MessageList<Message> getMessages()
    {
      return _messages;
    }

    public void setMessages(MessageList<Message> messages)
    {
      _messages = messages;
    }

    public void addMessage(List<String> path, String format, Object... args)
    {
      _messages.add(new Message(path.toArray(), format, args));
    }

    boolean _isValid = true;
    List<String> _paths = new ArrayList<>();
    MessageList<Message> _messages = new MessageList<>();
  }

  /**
   * Metadata object used when each time the {@link #validate(List, Map, DataSchema, ValidationMetaData)} function is called
   *
   */
  class ValidationMetaData
  {
    public boolean isLeafDataSchema()
    {
      return _isLeafDataSchema;
    }

    public void setIsLeafDataSchema(boolean leafDataSchema)
    {
      _isLeafDataSchema = leafDataSchema;
    }

    /**
     * A boolean to tell whether the {@link #validate(List, Map, DataSchema, ValidationMetaData)}
     * function are called on a leaf data schema.
     *
     * a leaf DataSchema is a schema that doesn't have other types of DataSchema linked from it.
     * Below types are leaf DataSchemas
     * {@link com.linkedin.data.schema.PrimitiveDataSchema} ,
     * {@link com.linkedin.data.schema.EnumDataSchema} ,
     * {@link com.linkedin.data.schema.FixedDataSchema}
     *
     * Other dataSchema types, for example {@link com.linkedin.data.schema.TyperefDataSchema} could link to another DataSchema
     * so it is not a leaf DataSchema
     *
     */
    private boolean _isLeafDataSchema = false;
  }

  /**
   * Metadata object used when each time the {@link #resolve(List, ResolutionMetaData)} function is called
   *
   */
  class ResolutionMetaData
  {
    public DataSchema getDataSchemaUnderResolution()
    {
      return _dataSchemaUnderResolution;
    }

    public void setDataSchemaUnderResolution(DataSchema dataSchemaUnderResolution)
    {
      _dataSchemaUnderResolution = dataSchemaUnderResolution;
    }

    public DataSchema _dataSchemaUnderResolution;
  }
}
