/*
   Copyright (c) 2015 LinkedIn Corp.

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
import java.util.Collections;


/**
 * @author Keren Jin
 */
public class DefaultGeneratorResult implements GeneratorResult
{
   private final Collection<File> _sourceFiles;
   private final Collection<File> _targetFiles;
   private final Collection<File> _modifiedFiles;

   public DefaultGeneratorResult(Collection<File> sourceFiles, Collection<File> targetFiles, Collection<File> modifiedFiles)
   {
      _sourceFiles = Collections.unmodifiableCollection(sourceFiles);
      _targetFiles = Collections.unmodifiableCollection(targetFiles);
      _modifiedFiles = Collections.unmodifiableCollection(modifiedFiles);
   }

   @Override
   public Collection<File> getSourceFiles()
   {
      return _sourceFiles;
   }

   @Override
   public Collection<File> getTargetFiles()
   {
      return _targetFiles;
   }

   @Override
   public Collection<File> getModifiedFiles()
   {
      return _modifiedFiles;
   }
}
