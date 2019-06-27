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

package com.linkedin.restli.docgen;

import com.linkedin.data.template.GetMode;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.restspec.ActionSchema;
import com.linkedin.restli.restspec.ActionsSetSchema;
import com.linkedin.restli.restspec.AssociationSchema;
import com.linkedin.restli.restspec.BatchFinderSchema;
import com.linkedin.restli.restspec.CollectionSchema;
import com.linkedin.restli.restspec.FinderSchema;
import com.linkedin.restli.restspec.RestMethodSchema;
import com.linkedin.restli.restspec.ServiceErrorSchema;
import com.linkedin.restli.restspec.ServiceErrorsSchema;
import com.linkedin.restli.restspec.SimpleSchema;
import com.linkedin.restli.restspec.SuccessStatusesSchema;
import com.linkedin.restli.server.ResourceLevel;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Schema visitor which keeps track of all service errors and success statuses defined within a resource.
 * Also keeps track of whether either of these are encountered specifically for REST methods.
 *
 * @author Evan Williams
 */
public class ServiceErrorGatheringVisitor extends BaseResourceSchemaVisitor
{
  // Ordered maps keeping track of service errors with unique codes (no guarantee on parameters field)
  private LinkedHashMap<String, ServiceErrorSchema> _serviceErrors;
  private LinkedHashMap<String, ServiceErrorSchema> _resourceLevelServiceErrors;

  // Flags indicating whether service errors or success statuses have been encountered for REST methods
  private boolean _restMethodsHaveSuccessStatuses;
  private boolean _restMethodsHaveServiceErrors;

  public ServiceErrorGatheringVisitor()
  {
    _serviceErrors = new LinkedHashMap<>();
    _resourceLevelServiceErrors = new LinkedHashMap<>();
    _restMethodsHaveSuccessStatuses = false;
    _restMethodsHaveServiceErrors = false;
  }

  /**
   * Gets all unique service errors defined in a resource. "Unique" determined by the uniqueness of the code field.
   * Also, there's no guarantee on parameter data, since two service errors with different parameters may be considered
   * equal based on the code field alone.
   *
   * @return collection of service errors
   */
  public Collection<ServiceErrorSchema> getServiceErrors()
  {
    return _serviceErrors.values();
  }

  /**
   * Gets all service errors defined at the resource level for a resource.
   *
   * @return collection of service errors
   */
  public Collection<ServiceErrorSchema> getResourceLevelServiceErrors()
  {
    return _resourceLevelServiceErrors.values();
  }

  /**
   * Returns true if there's at least one success status defined for at least one REST method in a resource.
   */
  public boolean doRestMethodsHaveSuccessStatuses()
  {
    return _restMethodsHaveSuccessStatuses;
  }

  /**
   * Returns true if there's at least one service error defined for at least one REST method in a resource.
   */
  public boolean doRestMethodsHaveServiceErrors()
  {
    return _restMethodsHaveServiceErrors;
  }

  @Override
  public void visitCollectionResource(VisitContext visitContext,
                                      CollectionSchema collectionSchema)
  {
    checkServiceErrors(collectionSchema, true);
  }

  @Override
  public void visitAssociationResource(VisitContext visitContext,
                                       AssociationSchema associationSchema)
  {
    checkServiceErrors(associationSchema, true);
  }

  @Override
  public void visitSimpleResource(VisitContext visitContext,
                                  SimpleSchema simpleSchema)
  {
    checkServiceErrors(simpleSchema, true);
  }

  @Override
  public void visitActionSetResource(VisitContext visitContext,
                                     ActionsSetSchema actionSetSchema)
  {
    checkServiceErrors(actionSetSchema, true);
  }

  @Override
  public void visitRestMethod(VisitContext visitContext,
                              RecordTemplate parentResource,
                              RestMethodSchema restMethodSchema)
  {
    final boolean hasServiceErrors = checkServiceErrors(restMethodSchema, false);
    final boolean hasSuccessStatuses = checkSuccessStatuses(restMethodSchema);

    if (hasSuccessStatuses)
    {
      _restMethodsHaveSuccessStatuses = true;
    }
    if (hasServiceErrors)
    {
      _restMethodsHaveServiceErrors = true;
    }
  }

  @Override
  public void visitFinder(VisitContext visitContext,
                          RecordTemplate parentResource,
                          FinderSchema finderSchema)
  {
    checkServiceErrors(finderSchema, false);
  }

  @Override
  public void visitBatchFinder(VisitContext visitContext,
                               RecordTemplate parentResource,
                               BatchFinderSchema batchFinderSchema)
  {
    checkServiceErrors(batchFinderSchema, false);
  }

  @Override
  public void visitAction(VisitContext visitContext,
                          RecordTemplate parentResource,
                          ResourceLevel resourceLevel,
                          ActionSchema actionSchema)
  {
    checkServiceErrors(actionSchema, false);
  }

  /**
   * Given a record which includes {@link ServiceErrorsSchema}, collects all the defined service errors and returns
   * true if any service errors are encountered.
   *
   * @param record record which includes {@link ServiceErrorsSchema}
   * @param isResourceLevel true if checking from the context of a resource
   * @return true if any service error is encountered
   */
  private boolean checkServiceErrors(RecordTemplate record, boolean isResourceLevel)
  {
    final ServiceErrorsSchema serviceErrorsSchema = new ServiceErrorsSchema(record.data());
    if (serviceErrorsSchema.hasServiceErrors())
    {
      final Map<String, ServiceErrorSchema> serviceErrorMap = serviceErrorsSchema.getServiceErrors()
          .stream()
          .collect(Collectors.toMap(
              ServiceErrorSchema::getCode,
              Function.identity()
          ));
      _serviceErrors.putAll(serviceErrorMap);
      if (isResourceLevel)
      {
        _resourceLevelServiceErrors.putAll(serviceErrorMap);
      }
      return true;
    }
    return false;
  }

  /**
   * Given a record which includes {@link SuccessStatusesSchema}, returns true if any success statuses are encountered.
   *
   * @param record record which includes {@link SuccessStatusesSchema}
   * @return true if any success status is encountered
   */
  private boolean checkSuccessStatuses(RecordTemplate record)
  {
    final SuccessStatusesSchema successStatusesSchema = new SuccessStatusesSchema(record.data());
    return successStatusesSchema.hasSuccess() && !successStatusesSchema.getSuccess(GetMode.DEFAULT).isEmpty();
  }
}
