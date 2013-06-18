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
import com.linkedin.restli.restspec.CollectionSchema;
import com.linkedin.restli.restspec.EntitySchema;
import com.linkedin.restli.restspec.FinderSchema;
import com.linkedin.restli.restspec.ParameterSchema;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestMethodSchema;
import com.linkedin.restli.restspec.SimpleSchema;
import com.linkedin.restli.server.ResourceLevel;
import org.apache.log4j.Logger;

/**
 * @author dellamag
 */
public class LoggingResourceSchemaVisitor implements ResourceSchemaVisitior
{
  @Override
  public void visitAction(VisitContext visitContext,
                          RecordTemplate parentResource,
                          ResourceLevel resourceLevel,
                          ActionSchema actionSchema)
  {
    _logger.info("Visiting action: "+ actionSchema.getName());
    _logger.info("resource schema: " + visitContext.getResourcePath());
    _logger.info("resource level: " + resourceLevel);
  }

  @Override
  public void visitFinder(VisitContext visitContext,
                          RecordTemplate parentResource,
                          FinderSchema finderSchema)
  {
    _logger.info("Visiting finder: " + finderSchema.getName());
    _logger.info("resourcePath: " + visitContext.getResourcePath());
  }

  @Override
  public void visitParameter(VisitContext visitContext,
                             RecordTemplate parentResource,
                             Object parentMethodSchema,
                             ParameterSchema parameterSchema)
  {
    _logger.info("Visiting parameter: " + parameterSchema.getName());
    _logger.info("resourcePath: " + visitContext.getResourcePath());
  }

  @Override
  public void visitRestMethod(VisitContext visitContext,
                              RecordTemplate parentResource,
                              RestMethodSchema restMethodSchema)
  {
    _logger.info("Visiting restMethod: " + restMethodSchema.getMethod());
    _logger.info("resourcePath: " + visitContext.getResourcePath());
  }

  @Override
  public void visitActionSetResource(VisitContext visitContext,
                                     ActionsSetSchema actionSet)
  {
    _logger.info("Visiting actionSet for resource: " + visitContext.getResourcePath());
  }

  @Override
  public void visitAssociationResource(VisitContext visitContext,
                                       AssociationSchema associationSchema)
  {
    _logger.info("Visiting association for resource: " + visitContext.getResourcePath());
  }

  @Override
  public void visitCollectionResource(VisitContext visitContext,
                                      CollectionSchema collectionSchema)
  {
    _logger.info("Visiting collection for resource: " + visitContext.getResourcePath());
  }

  @Override
  public void visitSimpleResource(VisitContext visitContext,
                                      SimpleSchema collectionSchema)
  {
    _logger.info("Visiting simple for resource: " + visitContext.getResourcePath());
  }

  @Override
  public void visitEntityResource(VisitContext visitContext,
                                  EntitySchema entitySchema)
  {
    _logger.info("Visiting entity for resource: " + visitContext.getResourcePath());
  }

  @Override
  public void visitResourceSchema(VisitContext visitContext, ResourceSchema resourceSchema)
  {
    _logger.info("Visiting resource: " + visitContext.getResourcePath());
  }

  private final Logger _logger = Logger.getLogger(getClass());
}
