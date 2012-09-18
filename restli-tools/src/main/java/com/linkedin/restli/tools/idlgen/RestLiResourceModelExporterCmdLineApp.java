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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tool that serializes a set of resource models to a RESTspec IDL file
 *
 * Usage: restliexporter -help
 *
 * @author dellamag
 */
// OptionBuilder usage requires static-access suppression :/
@SuppressWarnings("static-access")
public class RestLiResourceModelExporterCmdLineApp
{
  private static final Logger log = LoggerFactory.getLogger(RestLiResourceModelExporterCmdLineApp.class);

  private static final Options OPTIONS = new Options();
  static
  {
    OPTIONS.addOption(OptionBuilder.isRequired().withArgName("sourcepath").hasArgs()
                      .withDescription("Space-delimited list of directories in which to find resource Java source files").create("sourcepath"));
    OPTIONS.addOption(OptionBuilder.withArgName("name").hasArg()
                      .withDescription("Name of the API").create("name"));
    OPTIONS.addOption(OptionBuilder.withArgName("outdir").hasArg()
                      .withDescription("Directory in which to output the generated IDL files (default=current working dir)").create("outdir"));

    OPTIONS.addOption(new Option("split", false, "DEPRECATED! Splits IDL across multiple files, one per root resource (always true)"));

    final OptionGroup sourceGroup = new OptionGroup();
    sourceGroup.setRequired(true);
    final Option sourcePkgs =
      OptionBuilder.withArgName("resourcepackages").hasArgs()
                   .withDescription("Space-delimited list of packages to scan for resource classes")
                   .create("resourcepackages");
    // not supported yet
//    Option sourceClasses =
//      OptionBuilder.withArgName("resourceclasses").hasArgs()
//                   .withDescription("space-delimited list of resource classes to scan")
//                   .create("resourceclasses");
    sourceGroup.addOption(sourcePkgs);
//    sourceGroup.addOption(sourceClasses);
    OPTIONS.addOptionGroup(sourceGroup);
  }

  /**
   * @param args restliexporter -sourcepath sourcepath -resourcepackages packagenames [-name api_name] [-outdir outdir]
   */
  public static void main(String[] args)
  {
    BasicConfigurator.configure();

//    args = new String[] {"-name", "groups",
//                         "-resourcepackages", "com.linkedin.groups.server.rest2.impl",
//                         "-sourcepath", "src/main/java",
//                         "-outdir", "src/codegen/idl",
//                         "-split"};

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
      formatter.printHelp("restliexporter -sourcepath sourcepath -resourcepackages packagenames" +
                          "[-name api_name] [-outdir outdir]", OPTIONS);
      System.exit(0);
    }

    try
    {
      new RestLiResourceModelExporter().export(cl.getOptionValue("name"),
                                               null,
                                               cl.getOptionValues("sourcepath"),
                                               cl.getOptionValues("resourcepackages"),
                                               cl.getOptionValue("outdir", "."));
    }
    catch (Throwable e)
    {
      log.error("Error writing IDL files", e);
    }
  }
}
