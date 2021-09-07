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
import com.linkedin.internal.tools.ArgumentFileProcessor;
import com.linkedin.pegasus.generator.CaseSensitiveFileCodeWriter;
import com.linkedin.pegasus.generator.CodeUtil;
import com.linkedin.pegasus.generator.DataTemplatePersistentClassChecker;
import com.linkedin.pegasus.generator.DefaultGeneratorResult;
import com.linkedin.pegasus.generator.GeneratorResult;
import com.linkedin.pegasus.generator.JavaCodeGeneratorBase;
import com.linkedin.pegasus.generator.JavaCodeUtil;
import com.linkedin.restli.internal.common.RestliVersion;
import com.linkedin.restli.internal.tools.RestLiToolsUtils;
import com.linkedin.restli.restspec.ResourceEntityType;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Generate request builder from idl file to java source file.
 *
 * @author Keren Jin
 */
public class RestRequestBuilderGenerator
{
  static final String GENERATOR_REST_GENERATE_DATATEMPLATES = "generator.rest.generate.datatemplates";
  static final String GENERATOR_REST_GENERATE_VERSION = "generator.rest.generate.version";
  public static final String GENERATOR_REST_GENERATE_LOWERCASE_PATH = "generator.rest.generate.lowercase.path";
  private static final String GENERATOR_REST_GENERATE_DEPRECATED_VERSION = "generator.rest.generate.deprecated.version";
  /**
   * The system property that specifies whether to generate classes for externally resolved schemas
   */
  static final String GENERATOR_GENERATE_IMPORTED = "generator.generate.imported";
  private static final Logger _log = LoggerFactory.getLogger(RestRequestBuilderGenerator.class);

  /**
   * @param args Usage: RestRequestBuilderGenerator targetDirectoryPath sourceFilePaths
   *
   * TODO refactor arg processing to eliminate use of sysprops in favor of proper CLI arguments;
   *  possibly using commons-cli or jcommander
   *
   * @throws IOException if there are problems opening or deleting files
   */
  public static void main(String[] args)
      throws IOException
  {
    String[] sources = new String[0];

    if (args.length < 2)
    {
      _log.error("Usage: RestRequestBuilderGenerator targetDirectoryPath [sourceFile or sourceDirectory]+");
      System.exit(1);
    }
    else if (args.length == 2 && ArgumentFileProcessor.isArgFile(args[1]))
    {
      // The second argument is an argFile, prefixed with '@' and containing one absolute path per line
      // Consume the argFile and populate the sources array
      sources = ArgumentFileProcessor.getContentsAsArray(args[1]);
    }
    else
    {
      sources = Arrays.copyOfRange(args, 1, args.length);
    }

    final String generateImported = System.getProperty(GENERATOR_GENERATE_IMPORTED);
    final String generateDataTemplates = System.getProperty(GENERATOR_REST_GENERATE_DATATEMPLATES);
    final String versionString = System.getProperty(GENERATOR_REST_GENERATE_VERSION);
    final String generateLowercasePath = System.getProperty(GENERATOR_REST_GENERATE_LOWERCASE_PATH);
    final RestliVersion version = RestliVersion.lookUpRestliVersion(new Version(versionString));
    if (version == null)
    {
      throw new IllegalArgumentException("Unrecognized version: " + versionString);
    }

    final RestliVersion deprecatedByVersion = findDeprecatedVersion();
    String resolverPath = RestLiToolsUtils.getResolverPathFromSystemProperty();

    RestRequestBuilderGenerator.run(resolverPath,
                                    System.getProperty(JavaCodeGeneratorBase.GENERATOR_DEFAULT_PACKAGE),
                                    System.getProperty(JavaCodeGeneratorBase.ROOT_PATH),
                                    generateImported == null ? true : Boolean.parseBoolean(generateImported),
                                    generateDataTemplates == null ? true : Boolean.parseBoolean(generateDataTemplates),
                                    version,
                                    deprecatedByVersion,
                                    args[0],
                                    sources,
                                    generateLowercasePath == null ? true : Boolean.parseBoolean(generateLowercasePath));
  }

  public static RestliVersion findDeprecatedVersion()
  {
    final String deprecatedByVersionString = System.getProperty(GENERATOR_REST_GENERATE_DEPRECATED_VERSION);
    if (deprecatedByVersionString == null)
    {
      return null;
    }

    try
    {
      return RestliVersion.lookUpRestliVersion(new Version(deprecatedByVersionString));
    }
    catch (IllegalArgumentException ignored)
    {
      return null;
    }
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
    return run(resolverPath,
               defaultPackage,
               null,
               generateImported,
               generateDataTemplates,
               version,
               deprecatedByVersion,
               targetDirectoryPath,
               sources);
  }

  public static GeneratorResult run(String resolverPath,
                                    String defaultPackage,
                                    String rootPath,
                                    final boolean generateImported,
                                    final boolean generateDataTemplates,
                                    RestliVersion version,
                                    RestliVersion deprecatedByVersion,
                                    String targetDirectoryPath,
                                    String[] sources)
      throws IOException
  {
    return run(resolverPath,
               defaultPackage,
               rootPath,
               generateImported,
               generateDataTemplates,
               version,
               deprecatedByVersion,
               targetDirectoryPath,
               sources,
               true);
  }

  /**
   * @param generateLowercasePath true, files are generated with a lower case path; false, files are generated as spec specifies.
   */
  public static GeneratorResult run(String resolverPath,
                                    String defaultPackage,
                                    String rootPath,
                                    final boolean generateImported,
                                    final boolean generateDataTemplates,
                                    RestliVersion version,
                                    RestliVersion deprecatedByVersion,
                                    String targetDirectoryPath,
                                    String[] sources,
                                    boolean generateLowercasePath)
      throws IOException
  {
    final RestSpecParser parser = new RestSpecParser();
    final JavaRequestBuilderGenerator generator = new JavaRequestBuilderGenerator(resolverPath, defaultPackage, generateDataTemplates, version, deprecatedByVersion, rootPath);
    final ClassLoader classLoader = JavaCodeUtil.classLoaderFromResolverPath(resolverPath);

    final RestSpecParser.ParseResult parseResult = parser.parseSources(sources);

    final StringBuilder message = new StringBuilder();
    for (CodeUtil.Pair<ResourceSchema, File> pair : parseResult.getSchemaAndFiles())
    {
      ResourceSchema resourceSchema = pair.first;

      // Skip unstructured data resources for client generation
      if (resourceSchema == null || ResourceEntityType.UNSTRUCTURED_DATA == resourceSchema.getEntityType())
      {
        continue;
      }

      try
      {
        final JDefinedClass clazz = generator.generate(resourceSchema, pair.second, rootPath);
      }
      catch (Exception e)
      {
        _log.error("Failed to generate request builders for schema: " + resourceSchema.getName(), e);
        message.append(e.getMessage()).append("\n");
      }
    }

    if (message.length() > 0)
    {
      throw new IOException(message.toString());
    }

    final DataTemplatePersistentClassChecker dataTemplateChecker =
        new DataTemplatePersistentClassChecker(generateImported, generator.getSpecGenerator(),
            generator.getJavaDataTemplateGenerator(), Collections.<File>emptySet());
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
    final List<File> targetFiles = JavaCodeUtil.targetFiles(targetDirectory, requestBuilderCodeModel, classLoader, checker, generateLowercasePath);
    targetFiles.addAll(JavaCodeUtil.targetFiles(targetDirectory, dataTemplateCodeModel, classLoader, checker, generateLowercasePath));

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
      _log.debug("Files: " + targetFiles);
      requestBuilderCodeModel.build(new CaseSensitiveFileCodeWriter(targetDirectory, true, generateLowercasePath));
      dataTemplateCodeModel.build(new CaseSensitiveFileCodeWriter(targetDirectory, true, generateLowercasePath));
    }
    return new DefaultGeneratorResult(parseResult.getSourceFiles(), targetFiles, modifiedFiles);
  }
}
