/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.restli.server;


import com.linkedin.data.schema.DataSchema;
import java.util.Map;
import java.util.Set;


/**
 * An interface that provides high level information regarding the resource.
 */
public interface ResourceDefinition
{
  /**
   * Gets the name.
   *
   * @return the name
   */
  String getName();

  /**
   * Gets the namespace.
   *
   * @return the namespace
   */
  String getNamespace();

  /**
   * Gets the D2 service name. This is null unless explicitly annotated by the resource owner in the resource.
   *
   * <p>This is meant to be a hint to D2 based routing solutions, and is NOT directly used anywhere by
   * the rest.li framework, apart from enforcing that this value once set, cannot be changed for backward
   * compatibility reasons.</p>
   *
   * @return the d2 service name
   */
  String getD2ServiceName();

  /**
   * Gets the rest.li resource java class.
   *
   * @return java class for this rest.li resource.
   */
  default Class<?> getResourceClass() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns whether the resource is a root resource or not.
   *
   * @return true if the resource is a root resource; else false.
   */
  boolean isRoot();

  /**
   * Gets the parent {@link ResourceDefinition}.
   *
   * @return parent {@link ResourceDefinition} if this resource is a subresource; else null.
   */
  ResourceDefinition getParent();

  /**
   * Check whether the resource has any sub resources.
   *
   * @return true if this resource has sub-resources, false otherwise
   */
  boolean hasSubResources();

  /**
   * Gets sub resources map.
   *
   * @return the sub resources map
   */
  Map<String, ResourceDefinition> getSubResourceDefinitions();

  /**
   * Collect all the data schemas referenced by this definition into the given set.
   */
  void collectReferencedDataSchemas(Set<DataSchema> schemas);
}
