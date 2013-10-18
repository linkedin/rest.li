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

package com.linkedin.restli.tools.snapshot.check;


import com.linkedin.data.schema.DataSchemaLocation;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.generator.AbstractGenerator;
import com.linkedin.restli.tools.compatibility.CompatibilityInfoMap;
import com.linkedin.restli.tools.compatibility.CompatibilityUtil;
import com.linkedin.restli.tools.compatibility.ResourceCompatibilityChecker;
import com.linkedin.restli.tools.idlcheck.CompatibilityInfo;
import com.linkedin.restli.tools.idlcheck.CompatibilityLevel;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Stack;

/**
 * Check Compatibility between pairs of Snapshots (snapshot.json files). The results are categorized into types of errors found.
 *
 * @author Moira Tagle
 */

public class RestLiSnapshotCompatibilityChecker
{
  public static void main(String[] args)
  {
    final Options options = new Options();
    options.addOption("h", "help", false, "Print help");
    options.addOption(OptionBuilder.withArgName("compatibility_level")
                        .withLongOpt("compat")
                        .hasArg()
                        .withDescription("Compatibility level " + listCompatLevelOptions())
                        .create('c'));
    final String cmdLineSyntax = RestLiSnapshotCompatibilityChecker.class.getCanonicalName() + " [pairs of <prevRestspecPath currRestspecPath>]";

    final CommandLineParser parser = new PosixParser();
    final CommandLine cmd;

    try
    {
      cmd = parser.parse(options, args);
    }
    catch (ParseException e)
    {
      new HelpFormatter().printHelp(cmdLineSyntax, options, true);
      System.exit(1);
      return; // to suppress IDE warning
    }

    final String[] targets = cmd.getArgs();
    if (cmd.hasOption('h') || targets.length < 2 || targets.length % 2 != 0)
    {
      new HelpFormatter().printHelp(cmdLineSyntax, options, true);
      System.exit(1);
    }

    final String compatValue;
    if (cmd.hasOption('c'))
    {
      compatValue = cmd.getOptionValue('c');
    }
    else
    {
      compatValue = CompatibilityLevel.DEFAULT.name();
    }

    final CompatibilityLevel compat;
    try
    {
      compat = CompatibilityLevel.valueOf(compatValue.toUpperCase());
    }
    catch (IllegalArgumentException e)
    {
      new HelpFormatter().printHelp(cmdLineSyntax, options, true);
      System.exit(1);
      return;
    }

    final StringBuilder allSummaries = new StringBuilder();
    boolean result = true;
    final String resolverPath = System.getProperty(AbstractGenerator.GENERATOR_RESOLVER_PATH);
    final RestLiSnapshotCompatibilityChecker checker = new RestLiSnapshotCompatibilityChecker();
    checker.setResolverPath(resolverPath);

    for (int i = 1; i < targets.length; i += 2)
    {
      String prevTarget = targets[i - 1];
      String currTarget = targets[i];
      CompatibilityInfoMap infoMap = checker.check(prevTarget, currTarget, compat);
      result &= infoMap.isCompatible(compat);
      allSummaries.append(infoMap.createSummary(prevTarget, currTarget));

    }

    if (compat != CompatibilityLevel.OFF && allSummaries.length() > 0)
    {
      System.out.println(allSummaries);
    }

    System.exit(result ? 0 : 1);
  }

  public void setResolverPath(String resolverPath)
  {
    _resolverPath = resolverPath;
  }

  /**
   * Check backwards compatibility between two snapshot (snapshot.json) files.
   *
   * @param prevSnapshotPath previously existing snapshot file
   * @param currSnapshotPath current snapshot file
   * @param compatLevel compatibility level which affects the return value
   * @return true if the check result conforms the compatibility level requirement
   *         e.g. false if backwards compatible changes are found but the level is equivalent
   */
  public CompatibilityInfoMap check(String prevSnapshotPath, String currSnapshotPath, CompatibilityLevel compatLevel)
  {
    return checkCompatibility(prevSnapshotPath, currSnapshotPath, compatLevel, false);
  }

  /**
   * Check backwards compatibility between a idl (restspec.json) and a snapshot (snapshot.json) file.
   *
   * @param prevRestSpecPath previously existing idl file
   * @param currSnapshotPath current snapshot file
   * @param compatLevel compatibility level which affects the return value
   * @return true if the check result conforms the compatibility level requirement
   *         only restspec related compatibilities are reported
   *         e.g. false if backwards compatible changes are found but the level is equivalent
   */
  public CompatibilityInfoMap checkRestSpecVsSnapshot(String prevRestSpecPath, String currSnapshotPath, CompatibilityLevel compatLevel)
  {
    return checkCompatibility(prevRestSpecPath, currSnapshotPath, compatLevel, true);
  }

  private CompatibilityInfoMap checkCompatibility(String prevRestModelPath, String currRestModelPath, CompatibilityLevel compatLevel, boolean isAgainstRestSpec)
  {
    final CompatibilityInfoMap infoMap = new CompatibilityInfoMap();
    if (compatLevel == CompatibilityLevel.OFF)
    {
      // skip check entirely.
      return infoMap;
    }

    final Stack<Object> path = new Stack<Object>();
    path.push("");

    FileInputStream prevSnapshotFile = null;
    FileInputStream currSnapshotFile = null;

    try
    {
      prevSnapshotFile = new FileInputStream(prevRestModelPath);
    }
    catch (FileNotFoundException e)
    {
      infoMap.addRestSpecInfo(CompatibilityInfo.Type.RESOURCE_NEW, path, currRestModelPath);
    }

    try
    {
      currSnapshotFile = new FileInputStream(currRestModelPath);
    }
    catch (FileNotFoundException e)
    {
      infoMap.addRestSpecInfo(CompatibilityInfo.Type.RESOURCE_MISSING, path, prevRestModelPath);
    }

    if (prevSnapshotFile == null || currSnapshotFile == null)
    {
      return infoMap;
    }

    AbstractSnapshot prevSnapshot = null;
    AbstractSnapshot currSnapshot = null;
    try
    {
      if (isAgainstRestSpec)
      {
        prevSnapshot = new RestSpec(prevSnapshotFile);
      }
      else
      {
        prevSnapshot = new Snapshot(prevSnapshotFile);
      }

      currSnapshot = new Snapshot(currSnapshotFile);
    }
    catch (IOException e)
    {
      infoMap.addRestSpecInfo(CompatibilityInfo.Type.OTHER_ERROR, path, e.getMessage());
    }

    if (prevSnapshot == null || currSnapshot == null)
    {
      return infoMap;
    }

    final DataSchemaResolver currResolver = createResolverFromSnapshot(currSnapshot, _resolverPath);
    final DataSchemaResolver prevResolver;
    if (isAgainstRestSpec)
    {
      prevResolver = currResolver;
    }
    else
    {
      prevResolver = createResolverFromSnapshot(prevSnapshot, _resolverPath);
    }

    final ResourceCompatibilityChecker checker = new ResourceCompatibilityChecker(prevSnapshot.getResourceSchema(), prevResolver,
                                                                                  currSnapshot.getResourceSchema(), currResolver);
    checker.check(compatLevel);
    infoMap.addAll(checker.getInfoMap());

    return infoMap;
  }

  private static String listCompatLevelOptions()
  {
    final StringBuilder options = new StringBuilder("<");
    for (CompatibilityLevel compatLevel: CompatibilityLevel.values())
    {
      options.append(compatLevel.name().toLowerCase()).append("|");
    }
    options.replace(options.length() - 1, options.length(), ">");

    return options.toString();
  }

  private static DataSchemaResolver createResolverFromSnapshot(AbstractSnapshot snapshot, String resolverPath)
  {
    final DataSchemaResolver resolver = CompatibilityUtil.getDataSchemaResolver(resolverPath);

    for(Map.Entry<String, NamedDataSchema> entry: snapshot.getModels().entrySet())
    {
      Name name = new Name(entry.getKey());
      NamedDataSchema schema = entry.getValue();
      resolver.bindNameToSchema(name, schema, DataSchemaLocation.NO_LOCATION);
    }

    return resolver;
  }

  private String _resolverPath;

}
