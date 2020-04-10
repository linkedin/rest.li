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


import com.linkedin.data.schema.generator.AbstractGenerator;
import com.linkedin.internal.tools.ArgumentFileProcessor;
import com.linkedin.restli.internal.tools.AdditionalDocProvidersUtil;

import com.linkedin.restli.internal.tools.RestLiToolsUtils;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class RestLiSnapshotExporterCmdLineApp
{
  private static final Logger log = LoggerFactory.getLogger(
      RestLiSnapshotExporterCmdLineApp.class);

  private static final Options OPTIONS = new Options();

  static
  {
    OPTIONS.addOption(OptionBuilder.isRequired().withArgName("sourcepath").hasArgs()
                          .withDescription(
                              "Space-delimited list of directories in which to find resource Java source files\nIf neither -resourcepackages nor -resourcepackages is provided, all classes defined in the directories will be scanned").create(
            "sourcepath"));
    OPTIONS.addOption(OptionBuilder.withArgName("name").hasArg()
                          .withDescription("Name of the API").create("name"));
    OPTIONS.addOption(OptionBuilder.withArgName("outdir").hasArg()
                          .withDescription("Directory in which to output the generated Snapshot files (default=current working dir)").create("outdir"));
    OPTIONS.addOption(OptionBuilder.withArgName("loadAdditionalDocProviders")
                          .withDescription("Will load any additional DocProviders if available on the classpath.")
                          .create("loadAdditionalDocProviders"));

    final OptionGroup sourceGroup = new OptionGroup();
    final Option sourcePkgs =
        OptionBuilder.withArgName("resourcepackages").hasArgs()
            .withDescription("Space-delimited list of packages to scan for resource classes")
            .create("resourcepackages");
    final Option sourceClasses =
        OptionBuilder.withArgName("resourceclasses").hasArgs()
            .withDescription("space-delimited list of resource classes to scan")
            .create("resourceclasses");
    sourceGroup.addOption(sourcePkgs);
    sourceGroup.addOption(sourceClasses);
    OPTIONS.addOptionGroup(sourceGroup);
  }

  /**
   * @param args restliexporter -sourcepath sourcepath -resourcepackages packagenames [-name api_name] [-outdir outdir]
   */
  public static void main(String[] args)
  {
    CommandLine cl = null;
    try
    {
      final CommandLineParser parser = new GnuParser();
      cl = parser.parse(OPTIONS, args);
    }
    catch (ParseException e)
    {
      System.err.println("Invalid arguments: " + e.getMessage());
      final HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("restliexporter -sourcepath sourcepath [-resourcepackages packagenames] [-resourceclasses classnames]" +
                              "[-name api_name] [-outdir outdir]", OPTIONS);
      System.exit(0);
    }

    String resolverPath = null;
    try
    {
      resolverPath = RestLiToolsUtils.getResolverPathFromSystemProperty();
    } catch (IOException e)
    {
      System.err.println("Cannot read resolver path: " + e.getMessage());
      System.exit(255);
    }

    try
    {
      final RestLiSnapshotExporter exporter = new RestLiSnapshotExporter();
      exporter.setResolverPath(resolverPath);
      exporter.export(cl.getOptionValue("name"),
                      null,
                      cl.getOptionValues("sourcepath"),
                      cl.getOptionValues("resourcepackages"),
                      cl.getOptionValues("resourceClasses"),
                      cl.getOptionValue("outdir", "."),
                      AdditionalDocProvidersUtil.findDocProviders(log, cl.hasOption("loadAdditionalDocProviders")));
    }
    catch (Throwable e)
    {
      log.error("Error writing Snapshot files", e);
      System.out.println("Error writing Snapshot files:\n" + e);
      System.exit(1);
    }
  }
}
