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
import com.linkedin.data.schema.resolver.SchemaDirectory;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Generate Java data template files from Pegasus Data Model schema files.
 *
 * @author Karthik B
 */
public class DataTemplateGeneratorCmdLineApp
{
  private static final Logger LOGGER = LoggerFactory.getLogger(DataTemplateGeneratorCmdLineApp.class);
  private static final Options OPTIONS = new Options();

  static
  {
    OPTIONS.addOption("h", "help", false, "Show help.");
    OptionBuilder.withArgName("Generate imported schemas");
    OptionBuilder.withLongOpt("generateImported");
    OptionBuilder.hasArgs(0);
    OptionBuilder.withDescription("Specifies whether to generate classes for externally resolved schemas.");
    OPTIONS.addOption(OptionBuilder.create('i'));
    OptionBuilder.withArgName("Generate lower case path");
    OptionBuilder.withLongOpt("generateLowercasePath");
    OptionBuilder.hasArgs(0);
    OptionBuilder.withDescription("Specifies if generated directories should be created in lower case.");
    OPTIONS.addOption(OptionBuilder.create('l'));
    OptionBuilder.withArgName("Generate field mask");
    OptionBuilder.withLongOpt("generateFieldMask");
    OptionBuilder.hasArgs(0);
    OptionBuilder.withDescription("Specifies if field mask classes should be generated for templates.");
    OPTIONS.addOption(OptionBuilder.create('m'));
    OptionBuilder.withArgName("Target directory");
    OptionBuilder.withLongOpt("targetDir");
    OptionBuilder.hasArgs(1);
    OptionBuilder.isRequired();
    OptionBuilder.withDescription("Target directory in which the classes should be generated.");
    OPTIONS.addOption(OptionBuilder.create('d'));
    OptionBuilder.withArgName("Resolver Path/ArgFile");
    OptionBuilder.withLongOpt("resolverPath");
    OptionBuilder.hasArgs(1);
    OptionBuilder.withDescription("Resolver path for loading data schemas. This can also be an arg file with path written per "
        + "line in the file. Use the syntax @[filename] for this arg when using the arg file.");
    OPTIONS.addOption(OptionBuilder.create('p'));
    OptionBuilder.withArgName("Root path");
    OptionBuilder.withLongOpt("rootPath");
    OptionBuilder.hasArgs(1);
    OptionBuilder.withDescription("Root path used to generate the relative location for including in java doc.");
    OPTIONS.addOption(OptionBuilder.create('t'));
    OptionBuilder.withArgName("Default package");
    OptionBuilder.withLongOpt("defaultPackage");
    OptionBuilder.hasArgs(1);
    OptionBuilder.withDescription("Default package to use when a PDL schema has no namespace.");
    OPTIONS.addOption(OptionBuilder.create('n'));
    OptionBuilder.withArgName("Resolver schema directories");
    OptionBuilder.withLongOpt("resolverSchemaDirectories");
    OptionBuilder.hasArgs(1);
    OptionBuilder.withValueSeparator(',');
    OptionBuilder.withDescription("Comma separated list of schema directory names within the resolver path to use for"
        + "resolving schemas. Optional, defaults to 'pegasus'.");
    OPTIONS.addOption(OptionBuilder.create('r'));
  }

  private static void help()
  {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(120,
        DataTemplateGeneratorCmdLineApp.class.getSimpleName(),
        "Command should be followed by one or more source files to process.",
        OPTIONS,
        "[sources]+          List of source files or directories to process, specified at the end. Source file list can also be "
            + "provided as a single arg file, specified as @<arg filename>. The file should list source files/directories one per line.",
        true);
  }

  public static void main(String[] args)
      throws IOException
  {
    try
    {
      final CommandLineParser parser = new GnuParser();
      CommandLine cl = parser.parse(OPTIONS, args);

      if (cl.hasOption('h'))
      {
        help();
        System.exit(0);
      }
      final boolean generateImported = Boolean.parseBoolean(cl.getOptionValue('i', "true"));
      final boolean generateLowercasePath = Boolean.parseBoolean(cl.getOptionValue('l', "true"));
      final boolean generateFieldMask = Boolean.parseBoolean(cl.getOptionValue('m', "true"));
      final String targetDirectory = cl.getOptionValue('d');
      final String defaultPackage = cl.getOptionValue('n');
      String resolverPath = cl.getOptionValue('p');
      if (resolverPath!= null && ArgumentFileProcessor.isArgFile(resolverPath))
      {
        // The resolver path is an arg file, prefixed with '@' and containing the actual resolverPath
        resolverPath = ArgumentFileProcessor.getContentsAsArray(resolverPath)[0];
      }
      LOGGER.debug("Resolver Path: " + resolverPath);
      final String rootPath = cl.getOptionValue('t');
      String[] resolverSchemaDirectories = cl.getOptionValues('r');
      String[] sources = cl.getArgs();
      if (sources.length == 1 && ArgumentFileProcessor.isArgFile(sources[0]))
      {
        // Using argFile, prefixed with '@' and containing one absolute path per line
        // Consume the argFile and populate the sources array
        sources = ArgumentFileProcessor.getContentsAsArray(sources[0]);
      }
      else if (sources.length == 0)
      {
        help();
        System.exit(0);
      }


      DataTemplateGeneratorCmdLineApp.run(resolverPath,
          defaultPackage,
          rootPath,
          generateImported,
          targetDirectory,
          sources,
          generateLowercasePath,
          generateFieldMask,
          resolverSchemaDirectories);
    }
    catch (ParseException | IOException e)
    {
      LOGGER.error("Encountered error while generating template classes: " + e.getMessage());
      help();
      System.exit(1);
    }
  }

  private static void run(String resolverPath, String defaultPackage, String rootPath, final boolean generateImported,
      String targetDirectoryPath, String[] sources, boolean generateLowercasePath, boolean generateFieldMask,
      String[] resolverSchemaDirectories)
      throws IOException
  {
    final DataSchemaParser.Builder schemaParserBuilder = new DataSchemaParser.Builder(resolverPath);
    if (resolverSchemaDirectories != null)
    {
      schemaParserBuilder.setResolverDirectories(Arrays.stream(resolverSchemaDirectories)
          .map(directory -> (SchemaDirectory) () -> directory)
          .collect(Collectors.toList()));
    }
    final DataSchemaParser schemaParser =  schemaParserBuilder.build();
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

    final JavaCodeUtil.PersistentClassChecker checker = new DataTemplatePersistentClassChecker(
        generateImported, specGenerator, dataTemplateGenerator, parseResult.getSourceFiles());

    final File targetDirectory = new File(targetDirectoryPath);
    final List<File> targetFiles = JavaCodeUtil.targetFiles(
        targetDirectory, dataTemplateGenerator.getCodeModel(),
        JavaCodeUtil.classLoaderFromResolverPath(schemaParser.getResolverPath()), checker, generateLowercasePath);

    if (FileUtil.upToDate(parseResult.getSourceFiles(), targetFiles))
    {
      LOGGER.info("Target files are up-to-date: " + targetFiles);
    }
    else
    {
      LOGGER.info("Generating " + targetFiles.size() + " files");
      LOGGER.debug("Files: "+ targetFiles);
      validateDefinedClassRegistration(dataTemplateGenerator.getCodeModel(),
          dataTemplateGenerator.getGeneratedClasses().keySet());
      targetDirectory.mkdirs();
      dataTemplateGenerator.getCodeModel().build(
          new CaseSensitiveFileCodeWriter(targetDirectory, true, generateLowercasePath));
    }
  }

  /**
   * Validates that all JDefinedClass instances in the code model have been properly registered.
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
          throw new IllegalStateException(
              "Attempting to generate unregistered class: '" + currentClass.fullName() + "'");
        }
      }
    }
  }
}
