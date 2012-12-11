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
package com.linkedin.pegasus.gradle

import org.gradle.api.Project

/**
 * Created: 01/11/2012
 * @author Szczepan Faber
 */
class Clock {

  static Closure measuredAction(Project project, String displayName, Closure action) {
    return { measure(project, displayName, action) }
  }

  static Object measure(Project project, String displayName, Closure action) {
    def before = System.currentTimeMillis()
    def out = action()
    def took = System.currentTimeMillis() - before
    if (took > 50) {
      project.logger.debug("${project.path} ${displayName} took $took ms.")
    }
    out
  }
}
