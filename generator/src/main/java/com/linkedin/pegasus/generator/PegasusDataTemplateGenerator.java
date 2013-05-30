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


import com.linkedin.data.schema.resolver.FileDataSchemaLocation;
import com.sun.codemodel.writer.FileCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Generates Java data templates from Pegasus Data Model schemas.
 *
 * @author Eran Leshem
 */
public class PegasusDataTemplateGenerator extends DataTemplateGenerator
{
  private static final Logger log = LoggerFactory.getLogger(PegasusDataTemplateGenerator.class);

  private final Config _config;

  public static void main(String[] args) throws IOException
  {
    if (args.length < 2)
    {
      log.error("Usage: PegasusDataTemplateGenerator targetDirectoryPath [sourceFile or sourceDirectory or schemaName]+");
      System.exit(1);
    }

    final String generateImported = System.getProperty(GENERATOR_GENERATE_IMPORTED);
    run(System.getProperty(GENERATOR_RESOLVER_PATH),
        System.getProperty(GENERATOR_DEFAULT_PACKAGE),
        generateImported == null ? null : Boolean.parseBoolean(generateImported),
        args[0],
        Arrays.copyOfRange(args, 1, args.length));
  }

  public static GeneratorResult run(String resolverPath,
                                    String defaultPackage,
                                    Boolean generateImported,
                                    String targetDirectoryPath,
                                    String[] sources) throws IOException
  {
    final Config config = new Config(resolverPath, defaultPackage, generateImported);
    final PegasusDataTemplateGenerator generator = new PegasusDataTemplateGenerator(config);

    return generator.generate(targetDirectoryPath, sources);
  }

  public PegasusDataTemplateGenerator(Config config)
  {
    super();
    _config = config;
  }

  /**
   * Parses data schema files and generates stubs for them.
   * @param sources provides the paths to schema files and/or fully qualified schema names.
   * @param targetDirectoryPath path to target root java source directory
   * @return a result that includes collection of files accessed, would have generated and actually modified.
   * @throws IOException if there are problems opening or deleting files.
   */
  private GeneratorResult generate(String targetDirectoryPath, String[] sources) throws IOException
  {
    initializeDefaultPackage();
    initSchemaResolver();

    List<File> sourceFiles = parseSources(sources);

    File targetDirectory = new File(targetDirectoryPath);
    List<File> targetFiles = targetFiles(targetDirectory);

    List<File> modifiedFiles;
    if (upToDate(sourceFiles, targetFiles))
    {
      modifiedFiles = Collections.emptyList();
      log.info("Target files are up-to-date: " + targetFiles);
    }
    else
    {
      modifiedFiles = targetFiles;
      log.info("Generating " + targetFiles.size() + " files: " + targetFiles);
      validateDefinedClassRegistration();
      getCodeModel().build(new FileCodeWriter(targetDirectory, true));
    }
    return new Result(sourceFiles, targetFiles, modifiedFiles);
  }

  @Override
  protected Config getConfig()
  {
    return _config;
  }

  @Override
  protected void parseFile(File schemaSourceFile) throws IOException
  {
    FileDataSchemaLocation schemaLocation = new FileDataSchemaLocation(schemaSourceFile);
    pushCurrentLocation(schemaLocation);
    super.parseFile(schemaSourceFile);
    popCurrentLocation();
  }
}
