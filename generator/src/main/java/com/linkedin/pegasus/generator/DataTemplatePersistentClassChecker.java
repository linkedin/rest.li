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

import com.linkedin.data.schema.DataSchemaLocation;
import com.linkedin.pegasus.generator.spec.ClassTemplateSpec;
import com.sun.codemodel.JDefinedClass;
import java.io.File;
import java.util.Set;


/**
 * Implements the checker interface to decide if a template class should be persisted.
 */
public class DataTemplatePersistentClassChecker implements JavaCodeUtil.PersistentClassChecker
{
  private final boolean _generateImported;
  private final TemplateSpecGenerator _specGenerator;
  private final JavaDataTemplateGenerator _dataTemplateGenerator;
  private final Set<File> _sourceFiles;

  public DataTemplatePersistentClassChecker(boolean generateImported, TemplateSpecGenerator specGenerator,
      JavaDataTemplateGenerator dataTemplateGenerator, Set<File> sourceFiles)
  {
    _generateImported = generateImported;
    _specGenerator = specGenerator;
    _dataTemplateGenerator = dataTemplateGenerator;
    _sourceFiles = sourceFiles;
  }

  @Override
  public boolean isPersistent(JDefinedClass clazz)
  {
    if (_generateImported)
    {
      return true;
    }
    else
    {
      final ClassTemplateSpec spec = _dataTemplateGenerator.getGeneratedClasses().get(clazz);
      final DataSchemaLocation location = _specGenerator.getClassLocation(spec);
      return location == null  // assume local
          || _sourceFiles.contains(location.getSourceFile());
    }
  }
}
