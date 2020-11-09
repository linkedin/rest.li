/*
 * Copyright (c) 2020 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.restli.tools.snapshot.check;

import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.annotation.ExtensionSchemaAnnotationHandler;
import com.linkedin.data.schema.annotation.SchemaAnnotationHandler;
import com.linkedin.data.schema.compatibility.AnnotationCompatibilityChecker;
import com.linkedin.data.schema.compatibility.CompatibilityChecker;
import com.linkedin.data.schema.compatibility.CompatibilityMessage;
import com.linkedin.data.schema.compatibility.CompatibilityOptions;
import com.linkedin.data.schema.compatibility.CompatibilityResult;
import com.linkedin.data.schema.grammar.PdlSchemaParser;
import com.linkedin.data.schema.resolver.DefaultDataSchemaResolver;
import com.linkedin.restli.internal.tools.ClassJarPathUtil;
import com.linkedin.restli.tools.compatibility.CompatibilityInfoMap;
import com.linkedin.restli.tools.compatibility.CompatibilityReport;
import com.linkedin.restli.tools.idlcheck.CompatibilityLevel;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Check Compatibility between pairs of Pegasus Schema Snapshots (.pdl files).
 *
 * @author Yingjie Bi
 */
public class PegasusSchemaSnapshotCompatibilityChecker
{

  private static final Options _options = new Options();
  private static final Logger _logger = LoggerFactory.getLogger(
      PegasusSchemaSnapshotCompatibilityChecker.class);
  private final CompatibilityInfoMap _infoMap = new CompatibilityInfoMap();
  private static List<SchemaAnnotationHandler> _handlers = new ArrayList<>();

  private static final String PDL = ".pdl";


  static
  {
    _options.addOption(OptionBuilder.withLongOpt("help")
        .withDescription("Print help")
        .create('h'));
    _options.addOption(OptionBuilder.withArgName("compatibility_level")
        .withLongOpt("compatLevel")
        .hasArg()
        .withDescription("Compatibility level " + listCompatLevelOptions())
        .create("cl"));
    _options.addOption(OptionBuilder.withArgName("compatibilityOption_mode")
        .withLongOpt("compatMode")
        .hasArg()
        .withDescription("CompatibilityOption Mode " + listCompatModeOptions())
        .create("cm"));
    _options.addOption(OptionBuilder.withArgName("compatibility_report")
        .withLongOpt("report")
        .hasArg()
        .withDescription("Write the compatibility report into the provided file at the end of the execution.")
        .isRequired()
        .create("report"));
    _options.addOption(OptionBuilder.withArgName("annotation_handler_jarPaths")
        .withLongOpt("handlerJarPath")
        .hasArgs()
        .withDescription("path of the jars which contains the annotation handlers")
        .create("jar"));
    _options.addOption(OptionBuilder.withArgName("handler-classNames")
        .withLongOpt("handlerClassName")
        .hasArgs()
        .withDescription("class names of the handlers string, class names are separated by ':'.")
        .create("className"));
    _options.addOption(OptionBuilder.withArgName("extensionSchema")
        .withLongOpt("extensionSchema")
        .withDescription("Indicates check pegasus extension schema annotation, if this option is provided.")
        .create('e'));
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

    if (cliArgs.length != 2)
    {
      _logger.error("Invalid arguments!");
      help();
      System.exit(1);
    }

    String prevSnapshotDir = cliArgs[0];
    String currSnapshotDir = cliArgs[1];

    List<String> prevSnapshotAndCurrSnapshotPairs = getMatchingPrevAndCurrSnapshotPairs(prevSnapshotDir, currSnapshotDir);

    CompatibilityLevel compatLevel = null;
    if (cl.hasOption("cl"))
    {
      try
      {
        compatLevel = CompatibilityLevel.valueOf(cl.getOptionValue("cl").toUpperCase());
      }
      catch (IllegalArgumentException e)
      {
        _logger.error("Invalid compatibilityLevel: " + cl.getOptionValue("cl") + e.getMessage());
        help();
        System.exit(1);
      }
    }
    else
    {
      compatLevel = CompatibilityLevel.DEFAULT;
    }

    CompatibilityOptions.Mode compatMode = null;
    if (cl.hasOption("cm"))
    {
      try
      {
        compatMode = CompatibilityOptions.Mode.valueOf(cl.getOptionValue("cm").toUpperCase());
      }
      catch (IllegalArgumentException e)
      {
        _logger.error("Invalid compatibilityOption Mode: " + cl.getOptionValue("cm") + e.getMessage());
        help();
        System.exit(1);
      }
    }
    else
    {
      compatMode = CompatibilityOptions.Mode.SCHEMA;
    }

    if (cl.hasOption('e'))
    {
      _handlers.add(new ExtensionSchemaAnnotationHandler());
    }

    if (cl.hasOption("jar") && cl.hasOption("className"))
    {
     String handlerJarPaths = cl.getOptionValue("jar");
     String classNames = cl.getOptionValue("className");
     try
     {
       _handlers = ClassJarPathUtil.getAnnotationHandlers(handlerJarPaths, classNames);
     }
     catch (IllegalStateException e)
     {
       _logger.error("Error while doing schema compatibility check, could not get SchemaAnnotationHandler classes: " + e.getMessage());
       System.exit(1);
     }

    }

    PegasusSchemaSnapshotCompatibilityChecker compatibilityChecker = new PegasusSchemaSnapshotCompatibilityChecker();
    for (int i = 1; i < prevSnapshotAndCurrSnapshotPairs.size(); i += 2)
    {
      String prevSnapshot = prevSnapshotAndCurrSnapshotPairs.get(i-1);
      String currentSnapshot = prevSnapshotAndCurrSnapshotPairs.get(i);
      compatibilityChecker.checkPegasusSchemaCompatibility(prevSnapshot, currentSnapshot, compatMode, compatLevel);
    }

    if (cl.hasOption("report"))
    {
      File reportFile = new File(cl.getOptionValue("report"));
      String compatibilityReport = new CompatibilityReport(compatibilityChecker._infoMap, compatLevel).createReport();
      Files.write(reportFile.toPath(), compatibilityReport.getBytes(StandardCharsets.UTF_8));
      System.exit(0);
    }

    System.exit(compatibilityChecker._infoMap.isModelCompatible(compatLevel) ? 0 : 1);
  }

  /**
   * Check backwards compatibility between a pegasusSchemaSnapshot (.pdl) and a pegasusSchemaSnapshot (.pdl) file.
   *
   * @param prevPegasusSchemaPath previously existing snapshot file
   * @param currentPegasusSchemaPath current snapshot file
   * @param compatMode compatibilityOptions mode which defines the compatibility check mode.
   * @return CompatibilityInfoMap which contains information whether the given two files are compatible or not.
   */
  public CompatibilityInfoMap checkPegasusSchemaCompatibility(String prevPegasusSchemaPath, String currentPegasusSchemaPath,
      CompatibilityOptions.Mode compatMode, CompatibilityLevel compatLevel)
  {
    boolean newSchemaCreated = false;
    boolean preSchemaRemoved = false;

    DataSchema preSchema = null;
    try
    {
      preSchema = parseSchema(new File(prevPegasusSchemaPath));
    }
    catch(FileNotFoundException e)
    {
      newSchemaCreated = true;
    }

    DataSchema currSchema = null;
    try
    {
      currSchema = parseSchema(new File(currentPegasusSchemaPath));
    }
    catch(FileNotFoundException e)
    {
      preSchemaRemoved = true;
    }

    if (newSchemaCreated && !preSchemaRemoved)
    {
      constructCompatibilityMessage(CompatibilityMessage.Impact.NEW_SCHEMA_ADDED,
          "New schema %s is created.", currentPegasusSchemaPath);
    }
    if (!newSchemaCreated && preSchemaRemoved)
    {
      constructCompatibilityMessage(CompatibilityMessage.Impact.BREAK_OLD_CLIENTS,
          "Schema %s is removed.", prevPegasusSchemaPath);
    }

    if (preSchema == null || currSchema == null)
    {
      return _infoMap;
    }

    CompatibilityOptions compatibilityOptions = new CompatibilityOptions().setMode(compatMode).setAllowPromotions(true);
    CompatibilityResult result = CompatibilityChecker.checkCompatibility(preSchema, currSchema, compatibilityOptions);

    if (!result.getMessages().isEmpty())
    {
      result.getMessages().forEach(message -> _infoMap.addModelInfo(message, compatLevel));
    }

    if (!_handlers.isEmpty())
    {
      List<SchemaAnnotationHandler.AnnotationCompatibilityResult> annotationCompatibilityResults =
          AnnotationCompatibilityChecker.checkPegasusSchemaAnnotation(preSchema, currSchema, _handlers);
      for (SchemaAnnotationHandler.AnnotationCompatibilityResult annotationResult: annotationCompatibilityResults)
      {
        if (!annotationResult.getMessages().isEmpty())
        {
          annotationResult.getMessages().forEach(message -> _infoMap.addAnnotation(message));
        }
      }
    }

    return _infoMap;
  }

  private void constructCompatibilityMessage(CompatibilityMessage.Impact impact, String format, Object... args)
  {
    CompatibilityMessage message = new CompatibilityMessage(new Object[]{}, impact, format, args);
    _infoMap.addModelInfo(message);
  }

  private DataSchema parseSchema(File schemaFile) throws FileNotFoundException
  {
    PdlSchemaParser parser = new PdlSchemaParser(new DefaultDataSchemaResolver());
    parser.parse(new FileInputStream(schemaFile));
    if (parser.hasError())
    {
      throw new RuntimeException(parser.errorMessage() + " Error while parsing file: " + schemaFile.toString());
    }

    List<DataSchema> topLevelDataSchemas = parser.topLevelDataSchemas();
    if (topLevelDataSchemas.size() != 1)
    {
      throw new RuntimeException("Could not parse schema : " + schemaFile.getAbsolutePath() + " The size of top level schemas is not 1.");
    }
    DataSchema topLevelDataSchema = topLevelDataSchemas.get(0);
    if (!(topLevelDataSchema instanceof NamedDataSchema))
    {
      throw new RuntimeException("Invalid schema : " + schemaFile.getAbsolutePath() + ", the schema is not a named schema.");
    }
    return topLevelDataSchema;
  }

  private static String listCompatLevelOptions()
  {
    StringJoiner stringJoiner = new StringJoiner("|", "<", ">");
    Stream.of(CompatibilityLevel.values()).forEach(e -> stringJoiner.add(e.name()));
    return stringJoiner.toString();
  }

  private static String listCompatModeOptions()
  {
    StringJoiner stringJoiner = new StringJoiner("|", "<", ">");
    Stream.of(CompatibilityOptions.Mode.values()).forEach(e -> stringJoiner.add(e.name()));
    return stringJoiner.toString();
  }

  private static void help()
  {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(120,
        PegasusSchemaSnapshotCompatibilityChecker.class.getSimpleName(),
        "[compatibility_level], [compatibilityOption_mode], [report], [prevSnapshotDir], [currSnapshotDir], "
            + "[annotation_handler_jarPaths], [handler-classNames], [extensionSchema]",
        _options,
        "",
        true);
  }

  /**
   * Generate a file pair list, the same snapshot names of prevSnapshot and currSnapshot will be grouped together.
   *
   * @param prevSnapshotDir
   * @param currSnapshotDir
   * @return filePairList List<String>
   */
  static List<String> getMatchingPrevAndCurrSnapshotPairs(String prevSnapshotDir, String currSnapshotDir)
  {
    Map<String, String> prevFilesMap = createMapFromFiles(prevSnapshotDir);
    Map<String, String> currFilesMap = createMapFromFiles(currSnapshotDir);
    List<String> filePairs = new ArrayList<>();

    currFilesMap.forEach((filename, absolutePath) ->
    {
      if (prevFilesMap.containsKey(filename))
      {
        filePairs.add(prevFilesMap.get(filename));
        filePairs.add(absolutePath);
        prevFilesMap.remove(filename);
      }
      else
      {
        filePairs.add("");
        filePairs.add(absolutePath);
      }
    });

    prevFilesMap.forEach((filename, absolutePath) ->
    {
      filePairs.add(absolutePath);
      filePairs.add("");
    });

    return filePairs;
  }

  /**
   * Create a map for all the files under snapshot directory.
   * The key is the file name, the value is the absolutePath of the file
   * @param snapshotFileDir
   * @return filesMap Map<String, String>
   */
  static Map<String, String> createMapFromFiles(String snapshotFileDir)
  {
    try (Stream<Path> paths = Files.walk(Paths.get(snapshotFileDir)))
    {
      return paths
          .filter(path -> path.toString().endsWith(PDL))
          .map(path -> path.toFile())
          .collect(Collectors.toMap(File::getName, File:: getAbsolutePath, (first, second) -> first));
    }
    catch (IOException e)
    {
      _logger.error ("Error while reading snapshot directory: " + snapshotFileDir);
      System.exit(1);
    }
    return null;
  }
}
