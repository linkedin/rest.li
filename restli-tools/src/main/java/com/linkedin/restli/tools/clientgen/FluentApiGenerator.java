/*
   Copyright (c) 2020 LinkedIn Corp.

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

import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.resolver.MultiFormatDataSchemaResolver;
import com.linkedin.internal.tools.ArgumentFileProcessor;
import com.linkedin.pegasus.generator.CodeUtil;
import com.linkedin.pegasus.generator.TemplateSpecGenerator;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.common.RestliVersion;
import com.linkedin.restli.internal.server.RestLiInternalException;
import com.linkedin.restli.restspec.ResourceEntityType;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.tools.clientgen.builderspec.BuilderSpec;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.Log4JLogChute;
import org.apache.velocity.runtime.resource.loader.JarResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Generate fluent api client bindings from idl file to java source file.
 *
 * @author Karthik Balasubramanian
 */
public class FluentApiGenerator
{
  private static final Logger LOGGER = LoggerFactory.getLogger(FluentApiGenerator.class);
  private static final Options OPTIONS = new Options();
  private static final Map<ResourceMethod, String> BUILDER_BASE_MAP = new HashMap<>();
  private static final String API_TEMPLATE_DIR = "apiVmTemplates";

  static {
    BUILDER_BASE_MAP.put(ResourceMethod.GET, "get");
    BUILDER_BASE_MAP.put(ResourceMethod.DELETE, "delete");
    BUILDER_BASE_MAP.put(ResourceMethod.UPDATE, "update");
    BUILDER_BASE_MAP.put(ResourceMethod.CREATE, "create");
    BUILDER_BASE_MAP.put(ResourceMethod.PARTIAL_UPDATE, "partialUpdate");
    BUILDER_BASE_MAP.put(ResourceMethod.GET_ALL, "getAll");
    BUILDER_BASE_MAP.put(ResourceMethod.BATCH_GET, "batchGet");
    BUILDER_BASE_MAP.put(ResourceMethod.BATCH_UPDATE, "batchUpdate");
    BUILDER_BASE_MAP.put(ResourceMethod.BATCH_PARTIAL_UPDATE, "batchPartialUpdate");
    BUILDER_BASE_MAP.put(ResourceMethod.BATCH_DELETE, "batchDelete");
    BUILDER_BASE_MAP.put(ResourceMethod.BATCH_CREATE, "batchCreate");
  }

  public static void main(String[] args) throws Exception
  {
    OPTIONS.addOption("h", "help", false, "Show help.");
    OptionBuilder.withLongOpt("targetDir");
    OptionBuilder.hasArgs(1);
    OptionBuilder.isRequired();
    OptionBuilder.withDescription("Target directory in which the classes should be generated.");
    OPTIONS.addOption(OptionBuilder.create('t'));
    OptionBuilder.withLongOpt("resolverPath");
    OptionBuilder.hasArgs(1);
    OptionBuilder.isRequired();
    OptionBuilder.withDescription("Resolver path for loading data schemas");
    OPTIONS.addOption(OptionBuilder.create('p'));
    OPTIONS.addOption("r", "rootPath", true, "Root path to use for generating relative path for source location");

    try
    {
      final CommandLineParser parser = new GnuParser();
      CommandLine cl = parser.parse(OPTIONS, args);

      if (cl.hasOption('h'))
      {
        help();
        System.exit(0);
      }
      String targetDirectory = cl.getOptionValue('t');
      String resolverPath = cl.getOptionValue('p');
      if (ArgumentFileProcessor.isArgFile(resolverPath))
      {
        resolverPath = ArgumentFileProcessor.getContentsAsArray(resolverPath)[0];
      }
      String[] sources = cl.getArgs();
      if (sources.length == 1 && ArgumentFileProcessor.isArgFile(sources[0]))
      {
        // Using argFile, prefixed with '@' and containing one absolute path per line
        // Consume the argFile and populate the sources array
        sources = ArgumentFileProcessor.getContentsAsArray(sources[0]);
      }

      FluentApiGenerator.run(resolverPath, cl.getOptionValue('r'), targetDirectory, sources);
    }
    catch (ParseException e)
    {
      LOGGER.error("Invalid arguments: " + e.getMessage());
      System.exit(1);
    }

  }

  static void run(String resolverPath, String rootPath, String targetDirectoryPath, String[] sources)
      throws IOException
  {
    final RestSpecParser parser = new RestSpecParser();
    final DataSchemaResolver schemaResolver = MultiFormatDataSchemaResolver.withBuiltinFormats(resolverPath);


    final RequestBuilderSpecGenerator specGenerator = new RequestBuilderSpecGenerator(schemaResolver,
        new TemplateSpecGenerator(schemaResolver), RestliVersion.RESTLI_2_0_0, BUILDER_BASE_MAP);

    final RestSpecParser.ParseResult parseResult = parser.parseSources(sources);

    final StringBuilder message = new StringBuilder();
    for (CodeUtil.Pair<ResourceSchema, File> pair : parseResult.getSchemaAndFiles())
    {
      ResourceSchema resourceSchema = pair.first;
      // Skip unstructured data resources for client generation
      if (resourceSchema != null && ResourceEntityType.UNSTRUCTURED_DATA == resourceSchema.getEntityType())
      {
        continue;
      }

      try
      {
        specGenerator.generate(resourceSchema, pair.second);
      }
      catch (Exception e)
      {
        message.append(e.getMessage()).append("\n");
      }
    }

    if (message.length() > 0)
    {
      throw new IOException(message.toString());
    }

    Set<BuilderSpec> allBuilders = specGenerator.getBuilderSpec();
    VelocityEngine velocityEngine = initVelocityEngine();
    final File targetDirectory = new File(targetDirectoryPath);
    for (BuilderSpec spec : allBuilders)
    {
      File packageDir = new File(targetDirectory, spec.getNamespace().toLowerCase().replace('.', File.separatorChar));
      packageDir.mkdirs();
      File file = new File(packageDir, CodeUtil.capitalize(spec.getResource().getName()) + ".java");
      try (FileWriter writer = new FileWriter(file))
      {
        VelocityContext context = new VelocityContext();
        context.put("spec", spec);
        context.put("className", CodeUtil.capitalize(spec.getResource().getName()));
        if (spec.getResource().hasCollection())
        {
          // TODO: Implement.
          velocityEngine.mergeTemplate(API_TEMPLATE_DIR + "/collection.vm", context, writer);
        }
      }
      catch (Exception e)
      {
        LOGGER.error("Error generating fluent client apis", e);
        System.exit(1);
      }
    }
  }

  private static VelocityEngine initVelocityEngine()
  {
    final URL templateDirUrl = FluentApiGenerator.class.getClassLoader().getResource(API_TEMPLATE_DIR);
    if (templateDirUrl == null)
    {
      throw new RestLiInternalException("Unable to find the Velocity template resources");
    }

    StringBuilder configName;
    VelocityEngine velocity;
    if ("jar".equals(templateDirUrl.getProtocol()))
    {
      velocity = new VelocityEngine();

      // config Velocity to use the jar resource loader
      // more detail in Velocity user manual
      velocity.setProperty(VelocityEngine.RESOURCE_LOADER, "jar");

      configName = new StringBuilder("jar.").append(VelocityEngine.RESOURCE_LOADER).append(".class");
      velocity.setProperty(configName.toString(), JarResourceLoader.class.getName());

      configName = new StringBuilder("jar.").append(VelocityEngine.RESOURCE_LOADER).append(".path");

      // fix for Velocity 1.5: jar URL needs to be ended with "!/"
      final String normalizedUrl = templateDirUrl.toString().substring(0, templateDirUrl.toString().length() - API_TEMPLATE_DIR
          .length());
      velocity.setProperty(configName.toString(), normalizedUrl);
    }
    else if ("file".equals(templateDirUrl.getProtocol()))
    {
      velocity = new VelocityEngine();

      final String resourceDirPath = new File(templateDirUrl.getPath()).getParent();
      configName = new StringBuilder("file.").append(VelocityEngine.RESOURCE_LOADER).append(".path");
      velocity.setProperty(configName.toString(), resourceDirPath);
    }
    else
    {
      throw new IllegalArgumentException("Unsupported template path scheme");
    }

    velocity.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, Log4JLogChute.class.getName());
    velocity.setProperty(Log4JLogChute.RUNTIME_LOG_LOG4J_LOGGER, FluentApiGenerator.class.getName());

    try
    {
      velocity.init();
    }
    catch (Exception e)
    {
      throw new RestLiInternalException(e);
    }
    return velocity;
  }

  private static void help()
  {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(120,
        FluentApiGenerator.class.getSimpleName(),
        "",
        OPTIONS,
        "[sources]+",
        true);
  }
}
