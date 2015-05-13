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

package com.linkedin.restli.tools.clientgen;


import com.linkedin.common.Version;
import com.linkedin.data.schema.generator.AbstractGenerator;
import com.linkedin.pegasus.generator.CodeUtil;
import com.linkedin.pegasus.generator.DefaultGeneratorResult;
import com.linkedin.pegasus.generator.GeneratorResult;
import com.linkedin.pegasus.generator.JavaCodeGeneratorBase;
import com.linkedin.pegasus.generator.JavaCodeUtil;
import com.linkedin.pegasus.generator.PegasusDataTemplateGenerator;
import com.linkedin.restli.internal.common.RestliVersion;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.writer.FileCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Generate request builder from idl file to java source file.
 *
 * @author Keren Jin
 */
public class RestRequestBuilderGenerator
{
  private static final String GENERATOR_REST_GENERATE_DATATEMPLATES = "generator.rest.generate.datatemplates";
  private static final String GENERATOR_REST_GENERATE_VERSION = "generator.rest.generate.version";
  private static final Logger _log = LoggerFactory.getLogger(RestRequestBuilderGenerator.class);

  /**
   * @param args Usage: RestRequestBuilderGenerator targetDirectoryPath sourceFilePaths
   *
   * @throws IOException if there are problems opening or deleting files
   */
  public static void main(String[] args)
      throws IOException
  {
    if (args.length < 2)
    {
      _log.error("Usage: RestRequestBuilderGenerator targetDirectoryPath [sourceFile or sourceDirectory]+");
      System.exit(1);
    }

    final String generateImported = System.getProperty(PegasusDataTemplateGenerator.GENERATOR_GENERATE_IMPORTED);
    final String generateDataTemplates = System.getProperty(GENERATOR_REST_GENERATE_DATATEMPLATES);
    final String versionString = System.getProperty(GENERATOR_REST_GENERATE_VERSION);
    final RestliVersion version = RestliVersion.lookUpRestliVersion(new Version(versionString));
    if (version == null)
    {
      throw new IllegalArgumentException("Unrecognized version: " + versionString);
    }

    RestRequestBuilderGenerator.run(System.getProperty(AbstractGenerator.GENERATOR_RESOLVER_PATH),
                                    System.getProperty(JavaCodeGeneratorBase.GENERATOR_DEFAULT_PACKAGE),
                                    generateImported == null ? true : Boolean.parseBoolean(generateImported),
                                    generateDataTemplates == null ? true : Boolean.parseBoolean(generateDataTemplates),
                                    version,
                                    null,
                                    args[0],
                                    Arrays.copyOfRange(args, 1, args.length));
  }

  public static GeneratorResult run(String resolverPath,
                                    String defaultPackage,
                                    final boolean generateImported,
                                    final boolean generateDataTemplates,
                                    RestliVersion version,
                                    RestliVersion deprecatedByVersion,
                                    String targetDirectoryPath,
                                    String[] sources)
      throws IOException
  {
    final RestSpecParser parser = new RestSpecParser();
    final JavaRequestBuilderGenerator generator = new JavaRequestBuilderGenerator(resolverPath, defaultPackage, generateDataTemplates, version, deprecatedByVersion);
    final ClassLoader classLoader = JavaCodeUtil.classLoaderFromResolverPath(resolverPath);

    final RestSpecParser.ParseResult parseResult = parser.parseSources(sources);

    final StringBuilder message = new StringBuilder();
    for (CodeUtil.Pair<ResourceSchema, File> pair : parseResult.getSchemaAndFiles())
    {
      try
      {
        final JDefinedClass clazz = generator.generate(pair.first, pair.second);
      }
      catch (Exception e)
      {
        message.append(e.getMessage() + "\n");
      }
    }

    if (message.length() > 0)
    {
      throw new IOException(message.toString());
    }

    final PegasusDataTemplateGenerator.DataTemplatePersistentClassChecker dataTemplateChecker =
        new PegasusDataTemplateGenerator.DataTemplatePersistentClassChecker(generateImported,
                                                                            generator.getSpecGenerator(),
                                                                            generator.getJavaDataTemplateGenerator(),
                                                                            Collections.<File>emptySet());
    final JavaCodeUtil.PersistentClassChecker checker = new JavaCodeUtil.PersistentClassChecker()
    {
      @Override
      public boolean isPersistent(JDefinedClass clazz)
      {
        if (generateDataTemplates || generator.isGeneratedArrayClass(clazz))
        {
          try
          {
            Class.forName(clazz.fullName(), false, classLoader);
          }
          catch (ClassNotFoundException e)
          {
            return true;
          }
        }

        return dataTemplateChecker.isPersistent(clazz);
      }
    };

    final JCodeModel requestBuilderCodeModel = generator.getCodeModel();
    final JCodeModel dataTemplateCodeModel = generator.getJavaDataTemplateGenerator().getCodeModel();

    final File targetDirectory = new File(targetDirectoryPath);
    final List<File> targetFiles = JavaCodeUtil.targetFiles(targetDirectory, requestBuilderCodeModel, classLoader, checker);
    targetFiles.addAll(JavaCodeUtil.targetFiles(targetDirectory, dataTemplateCodeModel, classLoader, checker));

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
      requestBuilderCodeModel.build(new FileCodeWriter(targetDirectory, true));
      dataTemplateCodeModel.build(new FileCodeWriter(targetDirectory, true));
    }
    return new DefaultGeneratorResult(parseResult.getSourceFiles(), targetFiles, modifiedFiles);
  }
}
