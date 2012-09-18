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

package com.linkedin.data.schema;


/**
 * An object that has a fully scoped name which consists of a namespace and name.
 */
public interface Named
{
  /**
   * Get the name.
   *
   * @return the name.
   */
  String getName();

  /**
   * Get the namespace.
   */
  String getNamespace();

  /**
   * Get the fully scoped name.
   */
  String getFullName();
}
