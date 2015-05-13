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


import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaLocation;
import com.linkedin.data.schema.generator.AbstractGenerator;
import com.linkedin.data.schema.resolver.FileDataSchemaLocation;
import com.linkedin.pegasus.generator.spec.ClassTemplateSpec;
import com.linkedin.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.writer.FileCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Generate Java data template files from Pegasus Data Model schema files.
 *
 * @author Eran Leshem
 */
public class PegasusDataTemplateGenerator
{
  /**
   * The system property that specifies whether to generate classes for externally resolved schemas
   */
  public static final String GENERATOR_GENERATE_IMPORTED = "generator.generate.imported";

  private static final Logger _log = LoggerFactory.getLogger(PegasusDataTemplateGenerator.class);

  public static class DataTemplatePersistentClassChecker implements JavaCodeUtil.PersistentClassChecker
  {
    private final boolean _generateImported;
    private final TemplateSpecGenerator _specGenerator;
    private final JavaDataTemplateGenerator _dataTemplateGenerator;
    private final Set<File> _sourceFiles;

    public DataTemplatePersistentClassChecker(boolean generateImported,
                                              TemplateSpecGenerator specGenerator,
                                              JavaDataTemplateGenerator dataTemplateGenerator,
                                              Set<File> sourceFiles)
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

  public static void main(String[] args)
      throws IOException
  {
    if (args.length < 2)
    {
      _log.error("Usage: PegasusDataTemplateGenerator targetDirectoryPath [sourceFile or sourceDirectory or schemaName]+");
      System.exit(1);
    }

    final String generateImportedProperty = System.getProperty(PegasusDataTemplateGenerator.GENERATOR_GENERATE_IMPORTED);
    final boolean generateImported = generateImportedProperty == null ? true : Boolean.parseBoolean(generateImportedProperty);
    PegasusDataTemplateGenerator.run(System.getProperty(AbstractGenerator.GENERATOR_RESOLVER_PATH),
                                     System.getProperty(JavaCodeGeneratorBase.GENERATOR_DEFAULT_PACKAGE),
                                     generateImported,
                                     args[0],
                                     Arrays.copyOfRange(args, 1, args.length));
  }

  public static GeneratorResult run(String resolverPath, String defaultPackage, final boolean generateImported, String targetDirectoryPath, String[] sources)
      throws IOException
  {
    final DataSchemaParser schemaParser = new DataSchemaParser(resolverPath);
    final TemplateSpecGenerator specGenerator = new TemplateSpecGenerator(schemaParser.getSchemaResolver());
    final JavaDataTemplateGenerator dataTemplateGenerator = new JavaDataTemplateGenerator(defaultPackage);

    for (DataSchema predefinedSchema : JavaDataTemplateGenerator.PredefinedJavaClasses.keySet())
    {
      specGenerator.registerDefinedSchema(predefinedSchema);
    }

    final DataSchemaParser.ParseResult parseResult = schemaParser.parseSources(sources);

    for (CodeUtil.Pair<DataSchema, File> pair : parseResult.getSchemaAndFiles())
    {
      final DataSchemaLocation location = new FileDataSchemaLocation(pair.second);
      specGenerator.generate(pair.first, location);
    }
    for (CodeUtil.Pair<DataSchema, String> pair : parseResult.getSchemaAndNames())
    {
      specGenerator.generate(pair.first);
    }
    for (ClassTemplateSpec spec : specGenerator.getGeneratedSpecs())
    {
      dataTemplateGenerator.generate(spec);
    }

    final JavaCodeUtil.PersistentClassChecker checker = new DataTemplatePersistentClassChecker(generateImported, specGenerator, dataTemplateGenerator, parseResult.getSourceFiles());

    final File targetDirectory = new File(targetDirectoryPath);
    final List<File> targetFiles = JavaCodeUtil.targetFiles(targetDirectory, dataTemplateGenerator.getCodeModel(), JavaCodeUtil.classLoaderFromResolverPath(schemaParser.getResolverPath()), checker);

    final List<File> modifiedFiles;
    if (FileUtil.upToDate(parseResult.getSourceFiles(), targetFiles))
    {
      modifiedFiles = Collections.emptyList();
      _log.info("Target files are up-to-date: " + targetFiles);
    }
    else
    {
      modifiedFiles = targetFiles;
      _log.info("Generating " + targetFiles.size() + " files: " + targetFiles);
      validateDefinedClassRegistration(dataTemplateGenerator.getCodeModel(), dataTemplateGenerator.getGeneratedClasses().keySet());
      dataTemplateGenerator.getCodeModel().build(new FileCodeWriter(targetDirectory, true));
    }
    return new DefaultGeneratorResult(parseResult.getSourceFiles(), targetFiles, modifiedFiles);
  }

  /**
   * Validates that all JDefinedClass instances in the code model have been properly registered. See {@link TemplateSpecGenerator#registerClassTemplateSpec}.
   */
  private static void validateDefinedClassRegistration(JCodeModel codeModel, Collection<JDefinedClass> classes)
  {
    for (Iterator<JPackage> packageIterator = codeModel.packages(); packageIterator.hasNext(); )
    {
      final JPackage currentPackage = packageIterator.next();
      for (Iterator<JDefinedClass> classIterator = currentPackage.classes(); classIterator.hasNext(); )
      {
        final JDefinedClass currentClass = classIterator.next();
        if (!classes.contains(currentClass))
        {
          throw new IllegalStateException("Attempting to generate unregistered class: '" + currentClass.fullName() + "'");
        }
      }
    }
  }
}
