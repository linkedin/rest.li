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

package com.linkedin.restli.docgen;

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.restspec.ActionSchema;
import com.linkedin.restli.restspec.ActionsSetSchema;
import com.linkedin.restli.restspec.AssociationSchema;
import com.linkedin.restli.restspec.BatchFinderSchema;
import com.linkedin.restli.restspec.CollectionSchema;
import com.linkedin.restli.restspec.EntitySchema;
import com.linkedin.restli.restspec.FinderSchema;
import com.linkedin.restli.restspec.ParameterSchema;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestMethodSchema;
import com.linkedin.restli.restspec.SimpleSchema;
import com.linkedin.restli.server.ResourceLevel;
import java.util.List;

/**
 * Visits various features of a REST resource hierarchy. The hierarchy is formed
 * from disparate resource types, each of which has common method types, such as
 * rest methods, finders and actions.
 *
 * @author dellamag
 */
public interface ResourceSchemaVisitior
{
  /**
   * Callback function when the visitor visits a {@link ResourceSchema}.
   *
   * @param visitContext hierarchy of all parent resource schemas (root is the first element)
   * @param resourceSchema resource being visited
   */
  void visitResourceSchema(VisitContext visitContext,
                           ResourceSchema resourceSchema);

  /**
   * Callback function when the visitor visits a {@link CollectionSchema}.
   *
   * @param visitContext hierarchy of all parent resource schemas (root is the first element)
   * @param collectionSchema collection being visited
   */
  void visitCollectionResource(VisitContext visitContext,
                               CollectionSchema collectionSchema);

  /**
   * Callback function when the visitor visits a {@link AssociationSchema}.
   *
   * @param visitContext hierarchy of all parent resource schemas (root is the first element)
   * @param associationSchema association being visited
   */
  void visitAssociationResource(VisitContext visitContext,
                                AssociationSchema associationSchema);

  /**
   * Callback function when the visitor visits a {@link SimpleSchema}.
   *
   * @param visitContext hierarchy of all parent resource schemas (root is the first element)
   * @param simpleSchema simple being visited
   */
  void visitSimpleResource(VisitContext visitContext, SimpleSchema simpleSchema);

  /**
   * Callback function when the visitor visits a {@link ActionsSetSchema}.
   *
   * @param visitContext hierarchy of all parent resource schemas (root is the first element)
   * @param actionSetSchema action set being visited
   */
  void visitActionSetResource(VisitContext visitContext,
                              ActionsSetSchema actionSetSchema);

  /**
   * Callback function when the visitor visits a {@link EntitySchema}.
   *
   * @param visitContext hierarchy of all parent resource schemas (root is the first element)
   * @param entitySchema entity being visited
   */
  void visitEntityResource(VisitContext visitContext,
                           EntitySchema entitySchema);

  /**
   * Callback function when the visitor visits a {@link RestMethodSchema}.
   *
   * @param visitContext hierarchy of all parent resource schemas (root is the first element)
   * @param parentResource can be any of {@link CollectionSchema}, {@link ActionsSetSchema} or {@link EntitySchema}
   * @param restMethodSchema REST method being visited, e.g. GET, POST, BATCH_GET, etc
   */
  void visitRestMethod(VisitContext visitContext,
                       RecordTemplate parentResource,
                       RestMethodSchema restMethodSchema);

  /**
   * Callback function when the visitor visits a {@link FinderSchema}.
   *
   * @param visitContext hierarchy of all parent resource schemas (root is the first element)
   * @param parentResource can be any of {@link CollectionSchema}, {@link ActionsSetSchema} or {@link EntitySchema}
   * @param finderSchema finder being visited
   */
  void visitFinder(VisitContext visitContext,
                   RecordTemplate parentResource,
                   FinderSchema finderSchema);

  /**
   * Callback function when the visitor visits a {@link BatchFinderSchema}.
   *
   * @param visitContext hierarchy of all parent resource schemas (root is the first element)
   * @param parentResource can be any of {@link CollectionSchema}, {@link ActionsSetSchema} or {@link EntitySchema}
   * @param batchFinderSchema batchfinder being visited
   */
  void visitBatchFinder(VisitContext visitContext,
                        RecordTemplate parentResource,
                        BatchFinderSchema batchFinderSchema);

  /**
   * Callback function when the visitor visits a {@link ActionSchema}.
   *
   * @param visitContext hierarchy of all parent resource schemas (root is the first element)
   * @param parentResource can be any of {@link CollectionSchema}, {@link ActionsSetSchema} or {@link EntitySchema}
   * @param resourceLevel {@link ResourceLevel} of the action
   * @param actionSchema action being visited
   */
  void visitAction(VisitContext visitContext,
                   RecordTemplate parentResource,
                   ResourceLevel resourceLevel,
                   ActionSchema actionSchema);

  /**
   * Callback function when the visitor visits a {@link ParameterSchema}.
   *
   * @param visitContext hierarchy of all parent resource schemas (root is the first element)
   * @param parentResource can be any of {@link CollectionSchema}, {@link ActionsSetSchema} or {@link EntitySchema}
   * @param parentMethodSchema can be any of {@link RestMethodSchema}, {@link ActionSchema} or {@link FinderSchema}
   * @param parameterSchema parameter to be visited
   */
  void visitParameter(VisitContext visitContext,
                      RecordTemplate parentResource,
                      Object parentMethodSchema,
                      ParameterSchema parameterSchema);

  /**
   * Context data passed between visit callbacks.
   */
  static class VisitContext
  {
    /**
     * @param resourceSchemaHierarchy list of {@link ResourceSchema} that the visitor has traversed
     * @param resourcePath path of the resource
     */
    public VisitContext(List<ResourceSchema> resourceSchemaHierarchy,
                        String resourcePath)
    {
      _resourceSchemaHierarchy = resourceSchemaHierarchy;
      _resourcePath = resourcePath;
    }

    /**
     * @return list of {@link ResourceSchema} that the visitor has traversed
     */
    public List<ResourceSchema> getResourceSchemaHierarchy()
    {
      return _resourceSchemaHierarchy;
    }

    /**
     * @return path of the resource
     */
    public String getResourcePath()
    {
      return _resourcePath;
    }

    /**
     * @return the most recently visited {@link ResourceSchema}
     */
    public ResourceSchema getParentSchema()
    {
      return _resourceSchemaHierarchy.get(_resourceSchemaHierarchy.size() - 1);
    }

    private final List<ResourceSchema> _resourceSchemaHierarchy;
    private final String _resourcePath;
  }
}
