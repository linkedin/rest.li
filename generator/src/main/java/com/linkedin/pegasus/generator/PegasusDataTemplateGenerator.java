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
import com.linkedin.internal.tools.ArgumentFileProcessor;
import com.linkedin.pegasus.generator.spec.ClassTemplateSpec;
import com.linkedin.util.FileUtil;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JPackage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generate Java data template files from Pegasus Data Model schema files.
 *
 * @author Eran Leshem
 * @deprecated Use {@link DataTemplateGeneratorCmdLineApp} instead.
 */
@Deprecated
public class PegasusDataTemplateGenerator
{
  /**
   * The system property that specifies whether to generate classes for externally resolved schemas
   */
  public static final String GENERATOR_GENERATE_IMPORTED = "generator.generate.imported";
  public static final String GENERATOR_GENERATE_LOWERCASE_PATH = "generator.generate.lowercase.path";
  public static final String GENERATOR_GENERATE_FIELD_MASK = "generator.generate.field.mask";

  private static final Logger _log = LoggerFactory.getLogger(PegasusDataTemplateGenerator.class);

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
    final String generateLowercasePathProperty = System.getProperty(PegasusDataTemplateGenerator.GENERATOR_GENERATE_LOWERCASE_PATH);
    final boolean generateLowercasePath = generateLowercasePathProperty == null ?  true : Boolean.parseBoolean(generateLowercasePathProperty);
    final String generateFieldMaskProperty = System.getProperty(PegasusDataTemplateGenerator.GENERATOR_GENERATE_FIELD_MASK);
    final boolean generateFieldMask = Boolean.parseBoolean(generateFieldMaskProperty);
    String resolverPath = System.getProperty(AbstractGenerator.GENERATOR_RESOLVER_PATH);
    if (resolverPath != null && ArgumentFileProcessor.isArgFile(resolverPath))
    {
      // The resolver path is an arg file, prefixed with '@' and containing the actual resolverPath
      String[] argFileContents = ArgumentFileProcessor.getContentsAsArray(resolverPath);
      resolverPath = argFileContents.length > 0 ? argFileContents[0] : null;
    }
    _log.debug("Resolver Path: " + resolverPath);
    String[] schemaFiles = Arrays.copyOfRange(args, 1, args.length);
    PegasusDataTemplateGenerator.run(resolverPath,
                                     System.getProperty(JavaCodeGeneratorBase.GENERATOR_DEFAULT_PACKAGE),
                                     System.getProperty(JavaCodeGeneratorBase.ROOT_PATH),
                                     generateImported,
                                     args[0],
                                     schemaFiles,
                                     generateLowercasePath,
                                     generateFieldMask);
  }

  public static GeneratorResult run(String resolverPath, String defaultPackage, String rootPath, final boolean generateImported,
      String targetDirectoryPath, String[] sources, boolean generateLowercasePath, boolean generateFieldMask)
      throws IOException
  {
    final DataSchemaParser schemaParser = new DataSchemaParser.Builder(resolverPath).build();
    final TemplateSpecGenerator specGenerator = new TemplateSpecGenerator(schemaParser.getSchemaResolver());
    JavaDataTemplateGenerator.Config config = new JavaDataTemplateGenerator.Config();
    config.setDefaultPackage(defaultPackage);
    config.setRootPath(rootPath);
    config.setFieldMaskMethods(generateFieldMask);

    for (DataSchema predefinedSchema : JavaDataTemplateGenerator.PredefinedJavaClasses.keySet())
    {
      specGenerator.registerDefinedSchema(predefinedSchema);
    }

    final DataSchemaParser.ParseResult parseResult = schemaParser.parseSources(sources);

    for (Map.Entry<DataSchema, DataSchemaLocation> entry : parseResult.getSchemaAndLocations().entrySet())
    {
      specGenerator.generate(entry.getKey(), entry.getValue());
    }
    config.setProjectionMaskApiChecker(new ProjectionMaskApiChecker(
        specGenerator, parseResult.getSourceFiles(),
        JavaCodeUtil.classLoaderFromResolverPath(schemaParser.getResolverPath())));
    final JavaDataTemplateGenerator dataTemplateGenerator = new JavaDataTemplateGenerator(config);
    for (ClassTemplateSpec spec : specGenerator.getGeneratedSpecs())
    {
      dataTemplateGenerator.generate(spec);
    }

    final JavaCodeUtil.PersistentClassChecker checker = new DataTemplatePersistentClassChecker(generateImported, specGenerator, dataTemplateGenerator, parseResult.getSourceFiles());

    final File targetDirectory = new File(targetDirectoryPath);
    final List<File> targetFiles = JavaCodeUtil.targetFiles(targetDirectory, dataTemplateGenerator.getCodeModel(), JavaCodeUtil.classLoaderFromResolverPath(schemaParser.getResolverPath()), checker, generateLowercasePath);

    final List<File> modifiedFiles;
    if (FileUtil.upToDate(parseResult.getSourceFiles(), targetFiles))
    {
      modifiedFiles = Collections.emptyList();
      _log.info("Target files are up-to-date: " + targetFiles);
    }
    else
    {
      modifiedFiles = targetFiles;
      _log.info("Generating " + targetFiles.size() + " files");
      _log.debug("Files: "+ targetFiles);
      validateDefinedClassRegistration(dataTemplateGenerator.getCodeModel(), dataTemplateGenerator.getGeneratedClasses().keySet());
      targetDirectory.mkdirs();
      dataTemplateGenerator.getCodeModel().build(new CaseSensitiveFileCodeWriter(targetDirectory, true, generateLowercasePath));
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
