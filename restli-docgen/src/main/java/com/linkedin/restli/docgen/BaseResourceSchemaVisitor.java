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

/**
 * Concrete default implementation of {@link ResourceSchemaVisitior} that does nothing.
 *
 * @author dellamag
 */
public class BaseResourceSchemaVisitor implements ResourceSchemaVisitior
{
  @Override
  public void visitResourceSchema(VisitContext visitContext, ResourceSchema resourceSchema)
  {
  }

  @Override
  public void visitCollectionResource(VisitContext visitContext,
                                      CollectionSchema collectionSchema)
  {
  }

  @Override
  public void visitAssociationResource(VisitContext visitContext,
                                       AssociationSchema associationSchema)
  {
  }

  @Override
  public void visitSimpleResource(VisitContext visitContext, SimpleSchema simpleSchema)
  {
  }

  @Override
  public void visitActionSetResource(VisitContext visitContext, ActionsSetSchema actionSetSchema)
  {
  }

  @Override
  public void visitEntityResource(VisitContext visitContext,
                                  EntitySchema entitySchema)
  {
  }

  @Override
  public void visitRestMethod(VisitContext visitContext,
                              RecordTemplate parentResource,
                              RestMethodSchema restMethodSchema)
  {
  }

  @Override
  public void visitFinder(VisitContext visitContext,
                          RecordTemplate parentResource,
                          FinderSchema finderSchema)
  {
  }

  @Override
  public void visitBatchFinder(VisitContext visitContext,
      RecordTemplate parentResource,
      BatchFinderSchema batchFinderSchema)
  {
  }

  @Override
  public void visitAction(VisitContext visitContext,
                          RecordTemplate parentResource,
                          ResourceLevel resourceLevel,
                          ActionSchema actionSchema)
  {
  }

  @Override
  public void visitParameter(VisitContext visitContext,
                             RecordTemplate parentResource,
                             Object parentMethodSchema,
                             ParameterSchema parameterSchema)
  {
  }
}
