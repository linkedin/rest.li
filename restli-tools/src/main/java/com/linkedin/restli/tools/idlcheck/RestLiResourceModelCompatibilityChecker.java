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

package com.linkedin.restli.tools.idlcheck;

import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.SchemaParserFactory;
import com.linkedin.data.schema.generator.AbstractGenerator;
import com.linkedin.data.schema.resolver.DefaultDataSchemaResolver;
import com.linkedin.data.schema.resolver.FileDataSchemaResolver;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestSpecCodec;
import com.linkedin.restli.tools.compatibility.CompatibilityInfoMap;
import com.linkedin.restli.tools.compatibility.ResourceCompatibilityChecker;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Stack;


/**
 * Check backwards compatibility between pairs of idl (.restspec.json) files. The check result messages are categorized.
 *
 * @author Keren Jin
 */
public class RestLiResourceModelCompatibilityChecker
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
    final String cmdLineSyntax = RestLiResourceModelCompatibilityChecker.class.getCanonicalName() + " [pairs of <prevRestspecPath currRestspecPath>]";

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
    for (int i = 1; i < targets.length; i += 2)
    {
      final RestLiResourceModelCompatibilityChecker checker = new RestLiResourceModelCompatibilityChecker();
      checker.setResolverPath(System.getProperty(AbstractGenerator.GENERATOR_RESOLVER_PATH));

      String prevTarget = targets[i - 1];
      String currTarget = targets[i];
      result &= checker.check(prevTarget, currTarget, compat);
      allSummaries.append(checker.getMap().createSummary(prevTarget, currTarget));
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
   * Check backwards compatibility between two idl (.restspec.json) files.
   *
   * @param prevRestspecPath previously existing idl file
   * @param currRestspecPath current idl file
   * @param compatLevel compatibility level which affects the return value
   * @return true if the check result conforms the compatibility level requirement
   *         e.g. false if backwards compatible changes are found but the level is equivalent
   */
  public boolean check(String prevRestspecPath, String currRestspecPath, CompatibilityLevel compatLevel)
  {
    _prevRestspecPath = prevRestspecPath;
    _currRestspecPath = currRestspecPath;
    
    Stack<Object> path = new Stack<Object>();
    path.push("");

    ResourceSchema prevRec = null;
    ResourceSchema currRec = null;

    try
    {
      prevRec = _codec.readResourceSchema(new FileInputStream(prevRestspecPath));
    }
    catch (FileNotFoundException e)
    {
      _map.addInfo(CompatibilityInfo.Type.RESOURCE_NEW, path, currRestspecPath);
    }
    catch (IOException e)
    {
      _map.addInfo(CompatibilityInfo.Type.OTHER_ERROR, path, e.getMessage());
    }

    try
    {
      currRec = _codec.readResourceSchema(new FileInputStream(currRestspecPath));
    }
    catch (FileNotFoundException e)
    {
      _map.addInfo(CompatibilityInfo.Type.RESOURCE_MISSING, path, prevRestspecPath);
    }
    catch (Exception e)
    {
      _map.addInfo(CompatibilityInfo.Type.OTHER_ERROR, path, e.getMessage());
    }

    if (prevRec == null || currRec == null)
    {
      return _map.isCompatible(compatLevel);
    }

    final DataSchemaResolver resolver;
    if (_resolverPath == null)
    {
      resolver = new DefaultDataSchemaResolver();
    }
    else
    {
      resolver = new FileDataSchemaResolver(SchemaParserFactory.instance(), _resolverPath);
    }

    ResourceCompatibilityChecker checker = new ResourceCompatibilityChecker(prevRec, resolver, currRec, resolver);
    boolean check = checker.check(compatLevel);
    _map.addAll(checker.getMap());
    return check;
  }

  /**
   * Check backwards compatibility between two idl (.restspec.json) files.
   *
   * @param prevRestspecPath previously existing idl file
   * @param currRestspecPath current idl file
   * @return true if the previous idl file is equivalent to the current one
   */
  public boolean check(String prevRestspecPath, String currRestspecPath)
  {
    return check(prevRestspecPath, currRestspecPath, CompatibilityLevel.EQUIVALENT);
  }

  /**
   * @return check results in the backwards incompatibility category.
   *         empty collection if called before checking any files
   */
  public Collection<CompatibilityInfo> getIncompatibles()
  {
    return _map.getIncompatibles();
  }

  /**
   * @return check results in the backwards compatibility category.
   *         empty collection if called before checking any files
   */
  @Deprecated
  public Collection<CompatibilityInfo> getCompatibles()
  {
    return _map.getCompatibles();
  }

  /**
   * @return summary message about the check result, including all categories
   *         empty string if called before checking any files
   */
  @Deprecated
  public String getSummary()
  {
    return _map.createSummary(_prevRestspecPath, _currRestspecPath);
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

  public CompatibilityInfoMap getMap()
  {
    return _map;
  }

  private String _prevRestspecPath;
  private String _currRestspecPath;
  private String _resolverPath;
  private static final RestSpecCodec _codec = new RestSpecCodec();
  private static final Logger log = LoggerFactory.getLogger(RestLiResourceModelCompatibilityChecker.class);

  private final CompatibilityInfoMap _map = new CompatibilityInfoMap();
}
