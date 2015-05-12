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

package com.linkedin.pegasus.generator;


import java.io.File;
import java.util.Collection;


/**
 * Result from running the generator, it is used to return data about the files accessed, would have been modified, and actually modified.
 */
public interface GeneratorResult
{
  /**
   * Return the source files accessed and parsed.
   *
   * @return the source files accessed and parsed.
   */
  public Collection<File> getSourceFiles();

  /**
   * Return the target files that would have been generated if they did not exist or were stale.
   *
   * @return the target files that would have been generated if they did not exist or were stale.
   */
  public Collection<File> getTargetFiles();

  /**
   * Return the files that have been modified or written during this run.
   *
   * @return the files that have been modified or written during this run.
   */
  public Collection<File> getModifiedFiles();
}
