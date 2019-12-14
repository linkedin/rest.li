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

import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaConstants;
import com.linkedin.data.schema.DataSchemaTraverse;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import com.linkedin.data.schema.annotation.DataSchemaRichContextTraverser.SchemaVisitor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This SchemaAnnotationProcessor is for processing annotations in {@link DataSchema}.
 *
 * The processor is expected to take {@link SchemaAnnotationHandler} as arguments, use them with {@link SchemaVisitor} to
 * traverse the schema and call the {@link SchemaVisitor#callbackOnContext(DataSchemaRichContextTraverser.TraverserContext, DataSchemaTraverse.Order)}
 * on the {@link SchemaAnnotationHandler}
 *
 * If the schema annotation is annotated using syntax rule that uses pathSpec as path for overriding fields,
 * Then the {@link PathSpecBasedSchemaAnnotationVisitor} can be used to parse such rules
 * And in this case,  what users would need to implement is a {@link SchemaAnnotationHandler} that uses {@link PathSpecBasedSchemaAnnotationVisitor}.
 *
 * also @see {@link PathSpecBasedSchemaAnnotationVisitor} for overriding annotation using pathspec
 * also @see {@link SchemaAnnotationHandler} for what to implement as resolution logic
 * also @see {@link PegasusSchemaAnnotationHandlerImpl} as the default handler implementation
 *
 */
public class SchemaAnnotationProcessor
{
  private static final Logger LOG = LoggerFactory.getLogger(SchemaAnnotationProcessor.class);

  /**
   * This function creates  {@link DataSchemaRichContextTraverser} and use it to wrap {@link SchemaVisitor} to visit the {@link DataSchema}
   *
   * Note {@link SchemaAnnotationHandler}'s #resolve() and #validate() function are supposed to be called by {@link SchemaVisitor}
   *
   * For the given {@link DataSchema}, it will first invoke each {@link SchemaAnnotationHandler#resolve}
   * by using the {@link SchemaVisitor} returned by {@link SchemaAnnotationHandler#getVisitor()}
   *
   * then it uses {@link SchemaAnnotationValidationVisitor} to invoke each {@link SchemaAnnotationHandler#validate} to validate resolved schema annotation.
   *
   * It will abort in case of unexpected exceptions.
   * Otherwise will aggregate error messages after all handlers' processing, to the final {@link SchemaAnnotationProcessResult}
   *
   * @param handlers the handlers that can resolve the annotation on the dataSchema and validate them
   * @param dataSchema the dataSchema to be processed
   * @param options additional options to help schema annotation processing
   * @return result after process
   */
  public static SchemaAnnotationProcessResult process(List<SchemaAnnotationHandler> handlers,
                                                      DataSchema dataSchema, AnnotationProcessOption options)
  {

    SchemaAnnotationProcessResult processResult = new SchemaAnnotationProcessResult();
    // passed in dataSchema is not changed after processing, this variable stores dynamically constructed dataSchema after each handler.
    processResult.setResultSchema(dataSchema);
    StringBuilder errorMsgBuilder = new StringBuilder();


    // resolve
    boolean hasResolveError = false;
    for (SchemaAnnotationHandler schemaAnnotationHandler: handlers)
    {
      LOG.debug("DEBUG:  starting resolving schema annotations using \"{}\" handler", schemaAnnotationHandler.getAnnotationNamespace());
      DataSchema schemaToProcess = processResult.getResultSchema();
      SchemaVisitor visitor = schemaAnnotationHandler.getVisitor();
      DataSchemaRichContextTraverser traverser = new DataSchemaRichContextTraverser(visitor);
      try
      {
        traverser.traverse(schemaToProcess);
      }
      catch (Exception e)
      {
        throw new IllegalStateException(String.format("Annotation resolution processing failed at \"%s\" handler",
                                                      schemaAnnotationHandler.getAnnotationNamespace()), e);
      }
      DataSchemaRichContextTraverser.VisitorTraversalResult handlerTraverseResult = visitor.getVisitorTraversalResult();
      if (!handlerTraverseResult.isTraversalSuccessful())
      {
        hasResolveError = true;
        String errorMsgs = handlerTraverseResult.formatToErrorMessage();
        errorMsgBuilder.append(String.format("Annotation processing encountered errors during resolution in \"%s\" handler. \n",
                                             schemaAnnotationHandler.getAnnotationNamespace()));
        errorMsgBuilder.append(errorMsgs);
      }

      if (handlerTraverseResult.isTraversalSuccessful() || options.forcePopulateDataSchemaToResult())
      {
        DataSchema visitorConstructedSchema = handlerTraverseResult.getConstructedSchema();
        if (visitorConstructedSchema != null)
        {
          // will update the processResult with the constructed dataSchema from the visitor.
          processResult.setResultSchema(visitorConstructedSchema);
        }
      }
    }
    processResult.setResolutionSuccess(!hasResolveError);
    // early terminate if resolution failed
    if (!processResult.isResolutionSuccess())
    {
      errorMsgBuilder.append("Annotation resolution processing failed at at least one of the handlers.\n");
      processResult.setErrorMsgs(errorMsgBuilder.toString());
      return processResult;
    }

    // validate
    boolean hasValidationError = false;
    for (SchemaAnnotationHandler schemaAnnotationHandler: handlers)
    {
      LOG.debug("DEBUG:  starting validating using \"{}\" handler", schemaAnnotationHandler.getAnnotationNamespace());
      SchemaAnnotationValidationVisitor validationVisitor = new SchemaAnnotationValidationVisitor(schemaAnnotationHandler);
      DataSchemaRichContextTraverser traverserBase = new DataSchemaRichContextTraverser(validationVisitor);
      try {
        traverserBase.traverse(processResult.getResultSchema());
      }
      catch (Exception e)
      {
        throw new IllegalStateException(String.format("Annotation validation failed in \"%s\" handler.",
                                                      schemaAnnotationHandler.getAnnotationNamespace()), e);
      }
      DataSchemaRichContextTraverser.VisitorTraversalResult handlerTraverseResult = validationVisitor.getVisitorTraversalResult();
      if (!handlerTraverseResult.isTraversalSuccessful())
      {
        hasValidationError = true;
        String errorMsgs = handlerTraverseResult.formatToErrorMessage();
        errorMsgBuilder.append(String.format("Annotation validation process failed in \"%s\" handler. \n",
                                             schemaAnnotationHandler.getAnnotationNamespace()));
        errorMsgBuilder.append(errorMsgs);
      }
    }
    processResult.setValidationSuccess(!hasValidationError);
    processResult.setErrorMsgs(errorMsgBuilder.toString());
    return processResult;
  }


  /**
   * Util function to get the resolvedProperties of the field specified by the PathSpec from the dataSchema.
   * If want to directly access the resolved properties of a dataSchema, could use an empty pathSpec.
   *
   * If the path specified is invalid for the given dataSchema, or the dataSchema is null,
   * will throw {@link IllegalArgumentException}
   *
   * @param pathSpec the pathSpec to search
   * @param dataSchema the dataSchema to start searching from
   * @return the resolvedProperties map
   */
  public static Map<String, Object> getResolvedPropertiesByPath(String pathSpec, DataSchema dataSchema)
  {
    if (dataSchema == null)
    {
      throw new IllegalArgumentException("Invalid data schema input");
    }

    if (pathSpec == null || (!pathSpec.isEmpty() && !PathSpec.validatePathSpecString(pathSpec)))
    {
      throw new IllegalArgumentException(String.format("Invalid inputs: PathSpec %s", pathSpec));
    }
    DataSchema dataSchemaToPath = findDataSchemaByPath(dataSchema, pathSpec);
    return dataSchemaToPath.getResolvedProperties();
  }

  private static DataSchema findDataSchemaByPath(DataSchema dataSchema, String pathSpec)
  {
    List<String> paths = new ArrayList<>(Arrays.asList(pathSpec.split(Character.toString(PathSpec.SEPARATOR))));
    paths.remove("");
    DataSchema currentSchema = dataSchema;
    for (String pathSegment: paths)
    {
      String errorMsg = String.format("Could not find path segment \"%s\" in PathSpec \"%s\"", pathSegment, pathSpec);
      if (currentSchema != null)
      {
        currentSchema = currentSchema.getDereferencedDataSchema();
        switch (currentSchema.getType())
        {
          case RECORD:
            RecordDataSchema recordDataSchema = (RecordDataSchema) currentSchema;
            RecordDataSchema.Field field = recordDataSchema.getField(pathSegment);
            if (field == null)
            {
              throw new IllegalArgumentException(errorMsg);
            }
            currentSchema = field.getType();
            break;
          case UNION:
            UnionDataSchema unionDataSchema = (UnionDataSchema) currentSchema;
            DataSchema unionSchema = unionDataSchema.getTypeByMemberKey(pathSegment);
            if (unionSchema == null)
            {
              throw new IllegalArgumentException(errorMsg);
            }
            currentSchema = unionSchema;
            break;
          case MAP:
            if (pathSegment.equals(PathSpec.WILDCARD))
            {
              currentSchema = ((MapDataSchema) currentSchema).getValues();
            }
            else if (pathSegment.equals((DataSchemaConstants.MAP_KEY_REF)))
            {
              currentSchema = ((MapDataSchema) currentSchema).getKey();
            }
            else
            {
              throw new IllegalArgumentException(errorMsg);
            }
            break;
          case ARRAY:
            if (pathSegment.equals(PathSpec.WILDCARD))
            {
              currentSchema = ((ArrayDataSchema) currentSchema).getItems();
            }
            else
            {
              throw new IllegalArgumentException(errorMsg);
            }
            break;
          default:
            //illegal state
            break;
        }
      }
    }

    // Remaining schema could be TypeRef
    currentSchema = currentSchema.getDereferencedDataSchema();

    return currentSchema;
  }


  /**
   * Process result returned by {@link #process(List, DataSchema, AnnotationProcessOption)}
   */
  public static class SchemaAnnotationProcessResult
  {
    SchemaAnnotationProcessResult()
    {
    }

    public DataSchema getResultSchema()
    {
      return _resultSchema;
    }

    public void setResultSchema(DataSchema resultSchema)
    {
      _resultSchema = resultSchema;
    }

    public boolean hasError()
    {
      return !(_resolutionSuccess && _validationSuccess);
    }

    public boolean isResolutionSuccess()
    {
      return _resolutionSuccess;
    }

    void setResolutionSuccess(boolean resolutionSuccess)
    {
      _resolutionSuccess = resolutionSuccess;
    }

    public boolean isValidationSuccess()
    {
      return _validationSuccess;
    }

    public void setValidationSuccess(boolean validationSuccess)
    {
      _validationSuccess = validationSuccess;
    }

    public String getErrorMsgs()
    {
      return errorMsgs;
    }

    public void setErrorMsgs(String errorMsgs)
    {
      this.errorMsgs = errorMsgs;
    }

    DataSchema _resultSchema;
    boolean _resolutionSuccess = false;
    boolean _validationSuccess = false;
    String errorMsgs;
  }

  /***
   * Additional options to pass to help processing schema annotations
   */
  public static class AnnotationProcessOption
  {
    public boolean forcePopulateDataSchemaToResult()
    {
      return _forcePopulateResultSchema;
    }

    public void setForcePopulateResultSchema(boolean forcePopulateResultSchema)
    {
      _forcePopulateResultSchema = forcePopulateResultSchema;
    }

    /**
     * By default, when {@link SchemaAnnotationProcessor} is processing a dataSchema using a handler,
     * and if there has been errors,
     * it will not populate the DataSchema processed by current handler to its {@link SchemaAnnotationProcessResult}
     *
     * By setting this variable to true, {@link SchemaAnnotationProcessor} will update the {@link SchemaAnnotationProcessResult}
     * using the dataSchema processed by current handler, even there has been errors.
     */
    boolean _forcePopulateResultSchema = false;

  }
}
