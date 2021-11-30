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
package com.linkedin.restli.tools.snapshot.gen;

import com.linkedin.restli.internal.tools.RestLiToolsUtils;
import java.io.File;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tool that encodes pegasus schemas to pegasusSchemaSnapshot files
 *
 * @author Yingjie Bi
 */
public class PegasusSchemaSnapshotGenerationCmdLineApp
{
  private static final Logger _logger = LoggerFactory.getLogger(
      PegasusSchemaSnapshotGenerationCmdLineApp.class);

  private static final Options _options = new Options();

  static
  {
    _options.addOption(OptionBuilder.withLongOpt("help")
        .withDescription("Print help")
        .create('h'));
  }

  public static void main(String[] args) throws Exception
  {
    final CommandLineParser parser = new GnuParser();
    CommandLine cl = parser.parse(_options, args);

    if (cl.hasOption('h'))
    {
      help();
      System.exit(0);
    }

    String[] cliArgs = cl.getArgs();
    if (cliArgs.length != 3)
    {
      _logger.error("Invalid arguments");
      help();
      System.exit(1);
    }

    String resolverPath = RestLiToolsUtils.readArgFromFileIfNeeded(cliArgs[0]);
    String inputPath = cliArgs[1];
    String outputPath = cliArgs[2];

    try
    {
      File outputDir = new File(outputPath);
      if (!outputDir.exists())
      {
        if (!outputDir.mkdirs())
        {
          throw new RuntimeException("Output directory '" + outputDir + "' could not be created!");
        }
      }
      if (!outputDir.isDirectory())
      {
        throw new RuntimeException("Output directory '" + outputDir + "' is not a directory");
      }
      if (!outputDir.canRead() || !outputDir.canWrite())
      {
        throw new RuntimeException("Output directory '" + outputDir + "' must be readable and writeable");
      }

      PegasusSchemaSnapshotExporter exporter = new PegasusSchemaSnapshotExporter();
      exporter.export(resolverPath, inputPath, outputDir);

    }
    catch (Exception e)
    {
      _logger.error("Error while generate pegasus schema snapshot: " + e.getMessage());
      System.exit(1);
    }
  }



  private static void help()
  {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(120,
        PegasusSchemaSnapshotGenerationCmdLineApp.class.getSimpleName(),
        "[resolverPath], [inputPath], [pegasusSchemaSnapshotDirectory]",
        _options,
        "",
        true);
  }
}
