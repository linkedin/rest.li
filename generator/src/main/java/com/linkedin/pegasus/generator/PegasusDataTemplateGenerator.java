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
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.resolver.FileDataSchemaLocation;
import com.sun.codemodel.JClass;
import com.sun.codemodel.writer.FileCodeWriter;


/**
 * Generates Java data templates from Pegasus Data Model schemas.
 *
 * @author Eran Leshem
 */
public class PegasusDataTemplateGenerator extends DataTemplateGenerator
{
  private static final Logger log = LoggerFactory.getLogger(PegasusDataTemplateGenerator.class);

  public static void main(String[] args) throws IOException
  {
    if (args.length < 2)
    {
      log.error("Usage: PegasusDataTemplateGenerator targetDirectoryPath [sourceFile or sourceDirectory or schemaName]+");
      System.exit(1);
    }

    PegasusDataTemplateGenerator generator = new PegasusDataTemplateGenerator();
    generator.run(args[0], Arrays.copyOfRange(args, 1, args.length));
  }

  /**
   * Parses data schema files and generates stubs for them.
   * @param sources provides the paths to schema files and/or fully qualified schema names.
   * @param targetDirectoryPath path to target root java source directory
   * @return a result that includes collection of files accessed, would have generated and actually modified.
   * @throws IOException if there are problems opening or deleting files.
   */
  public GeneratorResult run(String targetDirectoryPath, String sources[]) throws IOException
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
  protected void parseFile(File schemaSourceFile) throws IOException
  {
    FileDataSchemaLocation schemaLocation = new FileDataSchemaLocation(schemaSourceFile);
    pushCurrentLocation(schemaLocation);
    super.parseFile(schemaSourceFile);
    popCurrentLocation();
  }
}
