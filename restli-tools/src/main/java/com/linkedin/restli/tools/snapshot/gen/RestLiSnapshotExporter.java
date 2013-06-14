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

package com.linkedin.restli.tools.snapshot.gen;


import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.pegasus.generator.GeneratorResult;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.model.ResourceModelEncoder;
import com.linkedin.restli.internal.server.model.RestLiApiBuilder;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.server.RestLiConfig;
import com.linkedin.restli.server.util.FileClassNameScanner;
import com.linkedin.restli.tools.compatibility.CompatibilityUtil;
import com.linkedin.restli.tools.idlgen.DocletDocsProvider;
import com.sun.tools.javadoc.Main;
import org.apache.commons.io.output.NullWriter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Given a set of package names, scans all Rest.li resource classes in the packages and generate corresponding
 * snapshot (snapshot.json) files.
 *
 * @author Moira Tagle
 */

public class RestLiSnapshotExporter
{
  private static final Logger log = LoggerFactory.getLogger(RestLiSnapshotExporter.class);
  private static final DataSchemaResolver _schemaResolver;

  static
  {
    _schemaResolver = CompatibilityUtil.getDataSchemaResolver();
  }

  public GeneratorResult export(String apiName,
                                String[] classpath,
                                String[] sourcePaths,
                                String[] resourcePackages,
                                String[] resourceClasses,
                                String outdir)
    throws IOException
  {
    final RestLiConfig config = new RestLiConfig();
    if (resourcePackages != null)
    {
      config.addResourcePackageNames(resourcePackages);
    }

    Collection<String> sourceFileNames = null;
    if (resourceClasses != null || resourcePackages == null)
    {
      final Map<String, String> classFileNames = new HashMap<String, String>();
      for (String path : sourcePaths)
      {
        classFileNames.putAll(FileClassNameScanner.scan(path));
      }

      if (resourceClasses != null)
      {
        config.addResourceClassNames(resourceClasses);

        sourceFileNames = new ArrayList<String>(resourceClasses.length);
        for (String resourceClass : resourceClasses)
        {
          final String resourceFileName = classFileNames.get(resourceClass);
          if (resourceFileName == null)
          {
            log.warn("Unable to find source file for class " + resourceClass + " .  No Javadoc will be generated for it.");
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

    log.info("Executing Javadoc tool...");

    final String flatClasspath;
    if (classpath == null)
    {
      flatClasspath = System.getProperty("java.class.path");
    }
    else
    {
      flatClasspath = StringUtils.join(classpath, ":");
    }

    final PrintWriter sysoutWriter = new PrintWriter(System.out, true);
    final PrintWriter nullWriter = new PrintWriter(new NullWriter());
    final List<String> javadocArgs = new ArrayList<String>(Arrays.asList(
      "-classpath", flatClasspath,
      "-sourcepath", StringUtils.join(sourcePaths, ":")
    ));
    if (resourcePackages != null)
    {
      javadocArgs.add("-subpackages");
      javadocArgs.add(StringUtils.join(resourcePackages, ":"));
    }
    else
    {
      javadocArgs.addAll(sourceFileNames);
    }
    Main.execute(apiName, sysoutWriter, nullWriter, nullWriter, "com.linkedin.restli.tools.idlgen.RestLiDoclet", javadocArgs.toArray(new String[0]));


    log.info("Exporting snapshot files...");

    final GeneratorResult result = generateSnapshotFiles(apiName, outdir, rootResourceMap);

    log.info("Done!");

    return result;
  }

  private GeneratorResult generateSnapshotFiles(String apiName,
                                                String outdir,
                                                Map<String, ResourceModel> rootResourceMap)
    throws IOException
  {
    SnapshotResult result = new SnapshotResult();

    final File outdirFile = new File(outdir);
    if (!outdirFile.exists())
    {
      if (!outdirFile.mkdirs())
      {
        throw new RuntimeException("Output directory '" + outdir + "' could not be created!");
      }
    }
    if (!outdirFile.isDirectory())
    {
      throw new RuntimeException("Output directory '" + outdir + "' is not a directory");
    }
    if (!outdirFile.canRead() || !outdirFile.canWrite())
    {
      throw new RuntimeException("Output directory '" + outdir + "' must be writeable");
    }

    final ResourceModelEncoder encoder = new ResourceModelEncoder(new DocletDocsProvider());

    final List<ResourceSchema> rootResourceNodes = new ArrayList<ResourceSchema>();
    for (Map.Entry<String, ResourceModel> entry: rootResourceMap.entrySet())
    {
      final ResourceSchema rootResourceNode = encoder.buildResourceSchema(entry.getValue());
      rootResourceNodes.add(rootResourceNode);
    }

    for (ResourceSchema rootResourceNode: rootResourceNodes)
    {
      String fileName = rootResourceNode.getName();
      if (rootResourceNode.hasNamespace())
      {
        final String namespace = rootResourceNode.getNamespace();
        fileName = namespace + "." + fileName;
      }
      if (apiName != null && !apiName.isEmpty())
      {
        fileName = apiName + "-" + fileName;
      }

      File writtenFile = writeSnapshotFile(outdirFile, fileName, rootResourceNode);
      result.addModifiedFile(writtenFile);
      result.addTargetFile(writtenFile);
    }

    return result;
  }

  private File writeSnapshotFile(File outdirFile,
                            String fileName,
                            ResourceSchema rootResourceNode)
    throws IOException
  {
    log.info("Writing file '" + fileName + '\'');

    SnapshotGenerator generator = new SnapshotGenerator(rootResourceNode, _schemaResolver);
    return generator.writeFile(outdirFile, fileName);
  }

  private static class SnapshotResult implements GeneratorResult
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
      throw new UnsupportedOperationException("getSourceFiles is not supported for the RestliSnapshotModelExporter");
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

}
