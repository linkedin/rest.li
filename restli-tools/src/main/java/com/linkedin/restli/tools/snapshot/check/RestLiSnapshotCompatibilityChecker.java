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
      compatValue = CompatibilityLevel.EQUIVALENT.name();
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
      result &= checker.check(prevTarget, currTarget, compat);
      allSummaries.append(checker.getInfoMap().createSummary(prevTarget, currTarget));

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
   * @param prevRestspecPath previously existing snapshot file
   * @param currRestspecPath current snapshot file
   * @param compatLevel compatibility level which affects the return value
   * @return true if the check result conforms the compatibility level requirement
   *         e.g. false if backwards compatible changes are found but the level is equivalent
   */
  public boolean check(String prevRestspecPath, String currRestspecPath, CompatibilityLevel compatLevel)
  {
    if (compatLevel == CompatibilityLevel.OFF)
    {
      // skip check entirely.
      return true;
    }

    Stack<Object> path = new Stack<Object>();
    path.push("");

    FileInputStream prevSnapshotFile = null;
    FileInputStream currSnapshotFile = null;

    try
    {
      prevSnapshotFile = new FileInputStream(prevRestspecPath);
    }
    catch (FileNotFoundException e)
    {
      _infoMap.addInfo(CompatibilityInfo.Type.RESOURCE_NEW, path, currRestspecPath);
    }

    try
    {
      currSnapshotFile = new FileInputStream(currRestspecPath);
    }
    catch (FileNotFoundException e)
    {
      _infoMap.addInfo(CompatibilityInfo.Type.RESOURCE_MISSING, path, prevRestspecPath);
    }

    if (prevSnapshotFile == null || currSnapshotFile == null)
    {
      return _infoMap.isCompatible(compatLevel);
    }

    Snapshot prevSnapshot = null;
    Snapshot currSnapshot = null;
    try
    {
      prevSnapshot = new Snapshot(prevSnapshotFile);
      currSnapshot = new Snapshot(currSnapshotFile);
    }
    catch (IOException e)
    {
      _infoMap.addInfo(CompatibilityInfo.Type.OTHER_ERROR, path, e.getMessage());
    }

    if (prevSnapshot == null || currSnapshot == null)
    {
      return _infoMap.isCompatible(compatLevel);
    }

    DataSchemaResolver prevResolver = createResolverFromSnapshot(prevSnapshot, _resolverPath);
    DataSchemaResolver currResolver = createResolverFromSnapshot(currSnapshot, _resolverPath);

    ResourceCompatibilityChecker checker = new ResourceCompatibilityChecker(prevSnapshot.getResourceSchema(), prevResolver,
                                                                            currSnapshot.getResourceSchema(), currResolver);
    boolean check = checker.check(compatLevel);
    _infoMap.addAll(checker.getInfoMap());
    return check;
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

  private static DataSchemaResolver createResolverFromSnapshot(Snapshot snapshot, String resolverPath)
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

  public CompatibilityInfoMap getInfoMap()
  {
    return _infoMap;
  }

  private String _resolverPath;

  private final CompatibilityInfoMap _infoMap = new CompatibilityInfoMap();

}
