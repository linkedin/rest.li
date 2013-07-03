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

package com.linkedin.restli.tools.idlgen;


import com.linkedin.pegasus.generator.GeneratorResult;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.model.ResourceModelEncoder;
import com.linkedin.restli.internal.server.model.ResourceModelEncoder.DocsProvider;
import com.linkedin.restli.internal.server.model.RestLiApiBuilder;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestSpecCodec;
import com.linkedin.restli.server.RestLiConfig;
import com.linkedin.restli.server.util.FileClassNameScanner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.output.NullWriter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given a set of package names, scans all Rest.li resource classes in the packages and generate corresponding
 * idl (.restspec.json) files.
 *
 * @author dellamag
 */
public class RestLiResourceModelExporter
{
  private static final Logger log = LoggerFactory.getLogger(RestLiResourceModelExporter.class);
  private static final RestSpecCodec _codec = new RestSpecCodec();

  /**
   * @param apiName the name of the API
   * @param classpath classpath to to load the resources. this is purely for Javadoc Doclet {@link RestLiDoclet}
   * @param sourcePaths paths to scan for resource Java source files. this is purely for Javadoc Doclet {@link RestLiDoclet}
   * @param resourcePackages packages to scan for resources
   * @param outdir directory in which to output the IDL files
   * @return a result that includes collection of files generated and modified. Note: getSourceFiles() on the result
   * will always return an empty List as the code generation operates on classpaths and the ClassLoader and not files.
   * @throws IOException could be {@link java.io.FileNotFoundException} if unable to write the output file,
   *                     otherwise, {@link IOException} if failure happened when writing the output file
   */
  public GeneratorResult export(String apiName,
                                String[] classpath,
                                String[] sourcePaths,
                                String[] resourcePackages,
                                String outdir)
    throws IOException
  {
    return export(apiName, classpath, sourcePaths, resourcePackages, null, outdir);
  }

  /**
   * @param apiName the name of the API
   * @param classpath classpath to to load the resources. this is purely for Javadoc Doclet {@link RestLiDoclet}
   * @param sourcePaths paths to scan for resource Java source files. this is purely for Javadoc Doclet {@link RestLiDoclet}
   *                    if both resourcePackages and resourceClasses is null, all classes defined in the directories will be scanned
   * @param resourcePackages packages to scan for resources
   * @param resourceClasses specific classes as resources
   * @param outdir directory in which to output the IDL files
   * @return a result that includes collection of files generated and modified. Note: getSourceFiles() on the result
   * will always return an empty List as the code generation operates on classpaths and the ClassLoader and not files.
   * @throws IOException could be {@link java.io.FileNotFoundException} if unable to write the output file,
   *                     otherwise, {@link IOException} if failure happened when writing the output file
   */
  public GeneratorResult export(String apiName,
                                String[] classpath,
                                String[] sourcePaths,
                                String[] resourcePackages,
                                String[] resourceClasses,
                                String outdir)
      throws IOException
  {
    return export(apiName,
                  classpath,
                  sourcePaths,
                  resourcePackages,
                  resourceClasses,
                  outdir,
                  Collections.<DocsProvider>emptyList());
  }

  /**
   * @param apiName the name of the API
   * @param classpath classpath to to load the resources. this is purely for Javadoc Doclet {@link RestLiDoclet}
   * @param sourcePaths paths to scan for resource Java source files. this is purely for Javadoc Doclet {@link RestLiDoclet}
   *                    if both resourcePackages and resourceClasses is null, all classes defined in the directories will be scanned
   * @param resourcePackages packages to scan for resources
   * @param resourceClasses specific classes as resources
   * @param outdir directory in which to output the IDL files
   * @param additionalDocProviders names of additional classes in the classpath that implement DocsProvider, if empty,
   *                      only javadoc will be supported.
   * @return a result that includes collection of files generated and modified. Note: getSourceFiles() on the result
   * will always return an empty List as the code generation operates on classpaths and the ClassLoader and not files.
   * @throws IOException could be {@link java.io.FileNotFoundException} if unable to write the output file,
   *                     otherwise, {@link IOException} if failure happened when writing the output file
   */
  public GeneratorResult export(String apiName,
                                String[] classpath,
                                String[] sourcePaths,
                                String[] resourcePackages,
                                String[] resourceClasses,
                                String outdir,
                                List<DocsProvider> additionalDocProviders)
      throws IOException
  {
    final RestLiConfig config = new RestLiConfig();
    if (resourcePackages != null)
    {
      config.addResourcePackageNames(resourcePackages);
    }

    final Map<String, String> classFileNames = new HashMap<String, String>();
    for (String path : sourcePaths)
    {
      classFileNames.putAll(FileClassNameScanner.scan(path));
    }

    Collection<String> sourceFileNames = null;
    if (resourceClasses != null || resourcePackages == null)
    {
      if (resourceClasses != null)
      {
        config.addResourceClassNames(resourceClasses);

        sourceFileNames = new ArrayList<String>(resourceClasses.length);
        for (String resourceClass : resourceClasses)
        {
          final String resourceFileName = classFileNames.get(resourceClass);
          if (resourceFileName == null)
          {
            log.warn("Unable to find source file for class " + resourceClass + " .  No documentation will be generated for it.");
          }
          else
          {
            sourceFileNames.add(resourceFileName);
          }
        }
      }
      else
      {
        config.addResourceClassNames(classFileNames.keySet());
        sourceFileNames = classFileNames.values();
      }
    }

    log.info("Executing Rest.li annotation processor...");
    final RestLiApiBuilder apiBuilder = new RestLiApiBuilder(config);
    final Map<String, ResourceModel> rootResourceMap = apiBuilder.build();
    if (rootResourceMap.isEmpty())
    {
      return new Result();
    }

    // We always include the doc provider for javadoc
    DocsProvider javadocProvider = new DocletDocsProvider(apiName, classpath, sourcePaths, resourcePackages);

    DocsProvider docsProvider;
    if(additionalDocProviders == null || additionalDocProviders.isEmpty())
    {
      docsProvider = javadocProvider;
    }
    else
    {
      // dynamically load doc providers for additional language, if available
      List<DocsProvider> languageSpecificDocsProviders = new ArrayList<DocsProvider>();
      languageSpecificDocsProviders.add(javadocProvider);
      languageSpecificDocsProviders.addAll(MultiLanguageDocsProvider.loadExternalProviders(additionalDocProviders));
      docsProvider = new MultiLanguageDocsProvider(languageSpecificDocsProviders);
    }

    log.info("Registering source files with doc providers...");

    docsProvider.registerSourceFiles(classFileNames.values());

    log.info("Exporting IDL files...");

    final GeneratorResult result = generateIDLFiles(apiName, outdir, rootResourceMap, docsProvider);

    log.info("Done!");

    return result;
  }

  /**
   * @param apiName the name of the API
   * @param sourcePaths paths to scan for resource Java source files
   * @param resourcePackages packages to scan for resources
   * @param resourceClasses specific classes as resources
   * @param outdir directory in which to output the IDL files
   * @param split if true, IDL will be split into multiple files, one per root resource
   * @return a result that includes collection of files generated and modified. Note: getSourceFiles() on the result
   * will always return an empty List as the code generation operates on classpaths and the ClassLoader and not files.
   * @throws IOException could be {@link java.io.FileNotFoundException} if unable to write the output file,
   *                     otherwise, {@link IOException} if failure happened when writing the output file
   */
  @Deprecated
  public GeneratorResult export(String apiName,
                                String[] sourcePaths,
                                String[] resourcePackages,
                                String[] resourceClasses,
                                String outdir,
                                boolean split)
      throws IOException
  {
    if (!split)
    {
      throw new IllegalStateException("Resource models should always be exported in split mode");
    }

    return export(apiName, null, sourcePaths, resourcePackages, resourceClasses, outdir);
  }

  private GeneratorResult generateIDLFiles(String apiName,
                                           String outdir,
                                           Map<String, ResourceModel> rootResourceMap,
                                           DocsProvider docsProvider)
      throws IOException
  {
    Result result = new Result();

    final File outdirFile = new File(outdir);
    if (! outdirFile.exists())
    {
      if (! outdirFile.mkdirs())
      {
        throw new RuntimeException("Output directory '" + outdir + "' could not be created!");
      }
    }
    if (! outdirFile.isDirectory())
    {
      throw new RuntimeException("Output directory '" + outdir + "' is not a directory");
    }
    if (! outdirFile.canRead() || !outdirFile.canWrite())
    {
      throw new RuntimeException("Output directory '" + outdir + "' must be writeable");
    }

    final ResourceModelEncoder encoder = new ResourceModelEncoder(docsProvider);

    final List<ResourceSchema> rootResourceNodes = new ArrayList<ResourceSchema>();
    for (Entry<String, ResourceModel> entry: rootResourceMap.entrySet())
    {
      final ResourceSchema rootResourceNode = encoder.buildResourceSchema(entry.getValue());
      rootResourceNodes.add(rootResourceNode);
    }

    for (ResourceSchema rootResourceNode: rootResourceNodes)
    {
      String fileName = rootResourceNode.getName();
      if (rootResourceNode.getNamespace() != null)
      {
        final String namespace = rootResourceNode.getNamespace();
        fileName = namespace + "." + fileName;
      }
      if (apiName != null && !apiName.isEmpty())
      {
        fileName = apiName + "-" + fileName;
      }

      File writtenFile = writeIDLFile(outdirFile, fileName, rootResourceNode);
      result.addModifiedFile(writtenFile);
      result.addTargetFile(writtenFile);
    }

    return result;
  }

  class Result implements GeneratorResult
  {
    private List<File> targetFiles = new ArrayList<File>();
    private List<File> modifiedFiles = new ArrayList<File>();

    public void addTargetFile(File file)
    {
      targetFiles.add(file);
    }

    public void addModifiedFile(File file)
    {
      modifiedFiles.add(file);
    }

    @Override
    public Collection<File> getSourceFiles()
    {
      throw new UnsupportedOperationException("getSourceFiles is not supported for the RestliResourceModelExporter");
    }

    @Override
    public Collection<File> getTargetFiles()
    {
      return targetFiles;
    }

    @Override
    public Collection<File> getModifiedFiles()
    {
      return modifiedFiles;
    }
  }

  private File writeIDLFile(File outdirFile,
                            String fileName,
                            ResourceSchema rootResourceNode)
      throws IOException
  {
    fileName += RestConstants.RESOURCE_MODEL_FILENAME_EXTENSION;
    log.info("Writing file '" + fileName + '\'');
    final File file = new File(outdirFile, fileName);

    _codec.writeResourceSchema(rootResourceNode, new FileOutputStream(file));
    return file;
  }
}
