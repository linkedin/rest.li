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
import com.linkedin.restli.restspec.FinderSchema;
import com.linkedin.restli.restspec.RestMethodSchema;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.util.ChainedIterator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Picks off REST methods actions, finders and actions from a set of {@link com.linkedin.restli.restspec.ResourceSchema}
 *
 * @author dellamag, Keren Jin
 */
public class MethodGatheringResourceSchemaVisitor extends BaseResourceSchemaVisitor
{
  /**
   * @param resourceNames name of ResourceSchemas whose methods are picked off
   */
  public MethodGatheringResourceSchemaVisitor(String... resourceNames)
  {
    for (String resourceName : resourceNames)
    {
      _resourceNames.add(resourceName);
    }
  }

  /**
   * @return REST methods from the target ResourceSchemas
   */
  public Set<RestMethodSchema> getRestMethods()
  {
    return _restMethods;
  }

  /**
   * @return finders from the target ResourceSchemas
   */
  public Set<FinderSchema> getFinders()
  {
    return _finders;
  }

  /**
   * @return collection-level actions from the target ResourceSchemas
   */
  public Set<ActionSchema> getCollectionActions()
  {
    return _collectionActions;
  }

  /**
   * @return entity-level actions from the target ResourceSchemas
   */
  public Set<ActionSchema> getEntityActions()
  {
    return _entityActions;
  }

  /**
   * @return combination of all REST methods, finders and actions from the target ResourceSchemas
   */
  public Iterable<? extends RecordTemplate> getAllMethods()
  {
    return new Iterable<RecordTemplate>()
    {
      private final Iterator<RecordTemplate> _itr = getAllMethodsIterator();
      @Override
      public Iterator<RecordTemplate> iterator()
      {
        return _itr;
      }
    };
  }

  /**
   * @return iterator to visit all the REST methods, finders and actions from the target ResourceSchemas
   */
  @SuppressWarnings("unchecked")
  public Iterator<RecordTemplate> getAllMethodsIterator()
  {
    return new ChainedIterator<RecordTemplate>(_restMethods.iterator(),
                                               _finders.iterator(),
                                               _collectionActions.iterator(),
                                               _entityActions.iterator());
  }

  @Override
  public void visitAction(VisitContext context,
                          RecordTemplate parentResource,
                          ResourceLevel resourceLevel,
                          ActionSchema actionSchema)
  {
    if (!isTargetResourcePath(context))
    {
      return;
    }

    if (resourceLevel == ResourceLevel.COLLECTION)
    {
      _collectionActions.add(actionSchema);
    }
    else
    {
      _entityActions.add(actionSchema);
    }
  }

  @Override
  public void visitFinder(VisitContext context,
                          RecordTemplate parentResource,
                          FinderSchema finderSchema)
  {
    if (isTargetResourcePath(context))
    {
      _finders.add(finderSchema);
    }
  }

  @Override
  public void visitRestMethod(VisitContext context,
                              RecordTemplate parentResource,
                              RestMethodSchema restMethodSchema)
  {
    if (isTargetResourcePath(context))
    {
      _restMethods.add(restMethodSchema);
    }
  }

  private boolean isTargetResourcePath(VisitContext visitContext)
  {
    return _resourceNames.contains(visitContext.getResourcePath());
  }

  private final Set<String> _resourceNames = new HashSet<String>();
  private final Set<RestMethodSchema> _restMethods = new HashSet<RestMethodSchema>();
  private final Set<FinderSchema> _finders = new HashSet<FinderSchema>();
  private final Set<ActionSchema> _collectionActions = new HashSet<ActionSchema>();
  private final Set<ActionSchema> _entityActions = new HashSet<ActionSchema>();
}
