/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.server.filter;


import com.linkedin.data.template.RecordTemplate;

/**
 * This interface provides information regarding the resource implementation.
 *
 * @author nshankar
 *
 */
public interface FilterResourceModel
{
  /**
   * Get the name of the target resource.
   *
   * @return Name of the resource.
   */
  String getResourceName();

  /**
   * Get the namespace of the target resource.
   *
   * @return Namespace of the resource.
   */
  String getResourceNamespace();

  /**
   * Flag indicating whether the resource is the root resource or not.
   *
   * @return true, if the resource is the root; else false.
   */
  boolean isRootResource();

  /**
   * Obtain the {@link Class} of the resource.
   *
   * @return {@link Class} of the resource.
   */
  Class<?> getResourceClass();

  /**
   * Obtain the {@link Class} of the record template.
   *
   * @return {@link Class} of the record template.
   */
  Class<? extends RecordTemplate> getValueClass();

  /**
   * Obtain the name of the key.
   *
   * @return Resource key name.
   */
  String getKeyName();

  /**
   * Obtain the ResourceModel of the parent resource, if applicable.
   *
   * @return null if this resource is the root resource; else ResourceModel corresponding to the
   *         parent resource.
   */
  FilterResourceModel getParentResourceModel();
}