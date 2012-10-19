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


import com.linkedin.data.DataMap;
import com.linkedin.data.message.Message;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.RecordDataSchema.Field;
import com.linkedin.data.schema.SchemaParserFactory;
import com.linkedin.data.schema.resolver.DefaultDataSchemaResolver;
import com.linkedin.data.schema.resolver.FileDataSchemaResolver;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.template.GetMode;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.WrappingArrayTemplate;
import com.linkedin.restli.restspec.ActionSchema;
import com.linkedin.restli.restspec.ActionsSetSchema;
import com.linkedin.restli.restspec.AssocKeySchema;
import com.linkedin.restli.restspec.AssociationSchema;
import com.linkedin.restli.restspec.CollectionSchema;
import com.linkedin.restli.restspec.EntitySchema;
import com.linkedin.restli.restspec.FinderSchema;
import com.linkedin.restli.restspec.IdentifierSchema;
import com.linkedin.restli.restspec.MetadataSchema;
import com.linkedin.restli.restspec.ParameterSchema;
import com.linkedin.restli.restspec.ParameterSchemaArray;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestMethodSchema;
import com.linkedin.restli.restspec.RestSpecCodec;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
      result &= checker.check(targets[i - 1], targets[i], compat);
      allSummaries.append(checker.getSummary());
    }

    if (compat != CompatibilityLevel.OFF && !result)
    {
      System.out.println(allSummaries);
    }

    System.exit(result ? 0 : 1);
  }

  /**
   * Create new {@link RestLiResourceModelCompatibilityChecker}.
   */
  public RestLiResourceModelCompatibilityChecker()
  {
    final String resolverPath = System.getProperty(GENERATOR_RESOLVER_PATH);
    if (resolverPath != null)
    {
      _schemaResolver = new FileDataSchemaResolver(SchemaParserFactory.instance(), resolverPath);
    }
    else
    {
      _schemaResolver = new DefaultDataSchemaResolver();
    }

    _info.put(CompatibilityInfo.Level.UNABLE_TO_CHECK, new ArrayList<CompatibilityInfo>());
    _info.put(CompatibilityInfo.Level.INCOMPATIBLE, new ArrayList<CompatibilityInfo>());
    _info.put(CompatibilityInfo.Level.COMPATIBLE, new ArrayList<CompatibilityInfo>());
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

    _path = new ArrayList<Object>();
    _path.add("");

    ResourceSchema prevRec = null;
    ResourceSchema currRec = null;

    try
    {
      prevRec = _codec.readResourceSchema(new FileInputStream(prevRestspecPath));
    }
    catch (FileNotFoundException e)
    {
      addInfo(CompatibilityInfo.Type.FILE_NOT_FOUND, prevRestspecPath);
    }
    catch (IOException e)
    {
      addInfo(CompatibilityInfo.Type.OTHER_ERROR, e.getMessage());
    }

    try
    {
      currRec = _codec.readResourceSchema(new FileInputStream(currRestspecPath));
    }
    catch (FileNotFoundException e)
    {
      addInfo(CompatibilityInfo.Type.FILE_NOT_FOUND, currRestspecPath);
    }
    catch (Exception e)
    {
      addInfo(CompatibilityInfo.Type.OTHER_ERROR, e.getMessage());
    }

    if (prevRec == null || currRec == null)
    {
      return isCompatible(compatLevel);
    }

    // validate RecordTemplate against schema before checking compatibility
    final ValidationOptions valOptions = new ValidationOptions();
    // use two lines to ensure both get called
    boolean valResult = validateData(prevRec.data(), prevRec.schema(), valOptions);
    valResult &= validateData(currRec.data(), currRec.schema(), valOptions);
    if (!valResult)
    {
      return isCompatible(compatLevel);
    }

    checkRecordTemplate(prevRec, currRec);

    _path.remove(_path.size() - 1);

    return isCompatible(compatLevel);
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
   * @return check results in the unable to check category.
   *         empty collection if called before checking any files
   */
  public List<CompatibilityInfo> getUnableToChecks()
  {
    return Collections.unmodifiableList(_info.get(CompatibilityInfo.Level.UNABLE_TO_CHECK));
  }

  /**
   * @return check results in the backwards incompatibility category.
   *         empty collection if called before checking any files
   */
  public List<CompatibilityInfo> getIncompatibles()
  {
    return Collections.unmodifiableList(_info.get(CompatibilityInfo.Level.INCOMPATIBLE));
  }

  /**
   * @return check results in the backwards compatibility category.
   *         empty collection if called before checking any files
   */
  public List<CompatibilityInfo> getCompatibles()
  {
    return Collections.unmodifiableList(_info.get(CompatibilityInfo.Level.COMPATIBLE));
  }

  /**
   * @return summary message about the check result, including all categories
   *         empty string if called before checking any files
   */
  public String getSummary()
  {
    final StringBuilder summaryMessage = new StringBuilder();

    createSummaryForInfo(getUnableToChecks(), "Unable to checks", summaryMessage);
    createSummaryForInfo(getIncompatibles(), "Incompatible changes", summaryMessage);
    createSummaryForInfo(getCompatibles(), "Compatible changes", summaryMessage);

    if (summaryMessage.length() != 0)
    {
      summaryMessage.insert(0, new StringBuilder().append("\nidl compatibility report between published \"").append(_prevRestspecPath).append("\" and current \"").append(_currRestspecPath).append("\":\n").toString());
    }

    return summaryMessage.toString();
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

  private static void createSummaryForInfo(List<CompatibilityInfo> info,
                                           String description,
                                           StringBuilder summaryMessage)
  {
    if (info.isEmpty())
    {
      return;
    }

    summaryMessage.append(description + ":\n");
    int issueIndex = 1;
    final Iterator<CompatibilityInfo> iter = info.iterator();
    while (iter.hasNext())
    {
      summaryMessage.append("  ").append(issueIndex).append(") ").append(iter.next().toString()).append("\n");
      ++issueIndex;
    }
  }

  private boolean isCompatible(CompatibilityLevel compatLevel)
  {
    final List<CompatibilityInfo> unableToChecks = getUnableToChecks();
    final List<CompatibilityInfo> incompatibles = getIncompatibles();
    final List<CompatibilityInfo> compatibles = getCompatibles();

    return ((unableToChecks.isEmpty() || compatLevel.ordinal() < CompatibilityLevel.BACKWARDS.ordinal()) &&
        (incompatibles.isEmpty()  || compatLevel.ordinal() < CompatibilityLevel.BACKWARDS.ordinal()) &&
        (compatibles.isEmpty()    || compatLevel.ordinal() < CompatibilityLevel.EQUIVALENT.ordinal()));
  }

  private boolean validateData(DataMap object, DataSchema schema, ValidationOptions options)
  {
    final ValidationResult valResult = ValidateDataAgainstSchema.validate(object, schema, options);
    if (valResult.isValid())
    {
      return true;
    }

    final Collection<Message> valErrorMessages = valResult.getMessages();
    for (final Message message : valErrorMessages)
    {
      addInfo(message);
    }

    return false;
  }

  private void addInfo(CompatibilityInfo.Type infoType, Object... parameters)
  {
    _info.get(infoType.getLevel()).add(new CompatibilityInfo(_path, infoType, parameters));
  }

  private void addInfo(Object pathTail, CompatibilityInfo.Type infoType, Object... parameters)
  {
    _path.add(pathTail);
    _info.get(infoType.getLevel()).add(new CompatibilityInfo(_path, infoType, parameters));
    _path.remove(_path.size() - 1);
  }

  private void addInfo(Message message)
  {
    final CompatibilityInfo.Type infoType = CompatibilityInfo.Type.OTHER_ERROR;
    _info.get(infoType.getLevel()).add(new CompatibilityInfo(Arrays.asList(message.getPath()),
                                                             infoType,
                                                             message.toString()));
  }

  private boolean isOptionalityCompatible(Field field, Object leader, Object follower)
  {
    /*
    compatible:
    leader   : follower : is optional
    null     : null     : true
    null     : not null : true
    not null : not null : true
    not null : not null : false

    incompatible:
    leader   : follower : is optional
    not null : null     : true
    null     : null     : false (caught by validator)
    null     : not null : false (caught by validator)
    not null : null     : false (caught by validator)
    */

    final boolean isLeaderNull = (leader == null);
    final boolean isFollowerNull = (follower == null);
    final boolean isCompatible = !(!isLeaderNull && isFollowerNull && field.getOptional());

    if (isCompatible && isLeaderNull != isFollowerNull)
    {
      addInfo(CompatibilityInfo.Type.OPTIONAL_VALUE, field.getName());
    }

    return isCompatible;
  }

  /**
   * @return true if the check passes, even with compatible changes.
   *         false if the check fails, i.e. incompatible changes are detected
   */
  private boolean checkEqualSingleValue(Field field, Object prevData, Object currData)
  {
    assert (field != null);

    if (!isOptionalityCompatible(field, prevData, currData))
    {
      addInfo(CompatibilityInfo.Type.VALUE_WRONG_OPTIONALITY, field.getName());
      return false;
    }

    // if both prev and curr are null, they are considered equal
    // if prev is null and curr is not null, it has to be a optional field, which is compatible
    // if both prev and curr are not null, they are compatible iff they are equal

    if (prevData != null && !prevData.equals(currData))
    {
      addInfo(field.getName(), CompatibilityInfo.Type.VALUE_NOT_EQUAL, prevData, currData);
      return false;
    }

    return true;
  }

  /**
   * @return whether the optionality check passes
   */
  private boolean checkArrayContainment(Field field,
                                        List<? extends Object> container,
                                        List<? extends Object> containee)
  {
    assert (field != null);

    if (!isOptionalityCompatible(field, containee, container))
    {
      addInfo(CompatibilityInfo.Type.VALUE_WRONG_OPTIONALITY, field.getName());
      return false;
    }

    if (containee == null)
    {
      return true;
    }

    final boolean isContained = container.containsAll(containee);
    if (isContained)
    {
      if (container.size() > containee.size())
      {
        final List<? extends Object> diff = new ArrayList<Object>(container);
        diff.removeAll(containee);
        addInfo(field.getName(), CompatibilityInfo.Type.SUPERSET, diff);
      }

      return true;
    }
    else
    {
      addInfo(field.getName(), CompatibilityInfo.Type.ARRAY_NOT_CONTAIN, containee);
      return false;
    }
  }

  /**
   * @return whether the optionality check passes
   */
  private boolean checkType(Object pathTail, String prevType, String currType, boolean allowNull)
  {
    if (prevType == null && currType == null && allowNull)
    {
      return true;
    }

    if (prevType == null || currType == null)
    {
      addInfo(pathTail, CompatibilityInfo.Type.TYPE_MISSING);
      return false;
    }

    try
    {
      final DataSchema prevSchema = RestSpecCodec.textToSchema(prevType, _schemaResolver);
      final DataSchema currSchema = RestSpecCodec.textToSchema(currType, _schemaResolver);

      // TODO: check compatible but different type pairs, e.g. Long (64-bit) vs Integer (32-bit)
      if (!prevSchema.equals(currSchema))
      {
        addInfo(pathTail, CompatibilityInfo.Type.TYPE_INCOMPATIBLE, prevType, currType);
        return false;
      }

      return true;
    }
    catch (IllegalArgumentException e)
    {
      addInfo(pathTail, CompatibilityInfo.Type.TYPE_UNKNOWN, e.getMessage());
      return false;
    }
  }

  /**
   * @return whether the optionality check passes
   */
  private boolean checkParameterOptionality(Field field, Boolean prevOptional, Boolean currOptional)
  {
    assert (field != null);

    // optional parameter is compatible in the opposite way of other fields
    // here curr is the leader, prev is the follower
    if (!isOptionalityCompatible(field, currOptional, prevOptional))
    {
      addInfo(CompatibilityInfo.Type.VALUE_WRONG_OPTIONALITY, field.getName());
      return false;
    }

    if (currOptional == null)
    {
      return true;
    }

    final boolean isCompatible = (prevOptional || !currOptional);
    if (!isCompatible)
    {
      addInfo(field.getName(), CompatibilityInfo.Type.PARAMETER_WRONG_OPTIONALITY);
      return false;
    }

    // previous optional and currently required should be the only case different yet compatible
    if (prevOptional && !currOptional)
    {
      addInfo(CompatibilityInfo.Type.OPTIONAL_PARAMETER);
    }
    else
    {
      assert(prevOptional == currOptional);
    }

    return true;
  }

  private <T extends RecordTemplate> void checkRecordTemplate(T prevRec, T currRec)
  {
    final Class<?> prevClass = prevRec.getClass();
    if (prevClass == ResourceSchema.class)
    {
      checkResourceSchema((ResourceSchema) prevRec, (ResourceSchema) currRec);
    }
    else if (prevClass == CollectionSchema.class)
    {
      checkCollectionSchema((CollectionSchema) prevRec, (CollectionSchema) currRec);
    }
    else if (prevClass == IdentifierSchema.class)
    {
      checkIdentifierSchema((IdentifierSchema) prevRec, (IdentifierSchema) currRec);
    }
    else if (prevClass == FinderSchema.class)
    {
      checkFinderSchema((FinderSchema) prevRec, (FinderSchema) currRec);
    }
    else if (prevClass == ParameterSchema.class)
    {
      checkParameterSchema((ParameterSchema) prevRec, (ParameterSchema) currRec);
    }
    else if (prevClass == MetadataSchema.class)
    {
      checkMetadataSchema((MetadataSchema) prevRec, (MetadataSchema) currRec);
    }
    else if (prevClass == ActionSchema.class)
    {
      checkActionSchema((ActionSchema) prevRec, (ActionSchema) currRec);
    }
    else if (prevClass == EntitySchema.class)
    {
      checkEntitySchema((EntitySchema) prevRec, (EntitySchema) currRec);
    }
    else if (prevClass == AssociationSchema.class)
    {
      checkAssociationSchema((AssociationSchema) prevRec, (AssociationSchema) currRec);
    }
    else if (prevClass == AssocKeySchema.class)
    {
      checkAssocKeySchema((AssocKeySchema) prevRec, (AssocKeySchema) currRec);
    }
    else if (prevClass == ActionsSetSchema.class)
    {
      checkActionsSetSchema((ActionsSetSchema) prevRec, (ActionsSetSchema) currRec);
    }
    else if (prevClass == RestMethodSchema.class)
    {
      checkRestMethodSchema((RestMethodSchema) prevRec, (RestMethodSchema) currRec);
    }
    else
    {
      addInfo(CompatibilityInfo.Type.OTHER_ERROR, "Unknown schema type: \"" + prevRec.getClass() + "\"");
    }
  }

  /**
   * @return whether the optionality check passes. it does not cover the result of RecordTemplate check
   */
  private <T extends RecordTemplate> boolean checkComplexField(Field field, T prevRec, T currRec)
  {
    assert (field != null);

    if (!isOptionalityCompatible(field, prevRec, currRec))
    {
      addInfo(CompatibilityInfo.Type.VALUE_WRONG_OPTIONALITY, field.getName());
      return false;
    }

    if (prevRec != null)
    {
      _path.add(field.getName());
      checkRecordTemplate(prevRec, currRec);
      _path.remove(_path.size() - 1);
    }

    return true;
  }

  /**
   * @return whether the basic check passes. also true if there is missing array element
   */
  private <T extends WrappingArrayTemplate<? extends RecordTemplate>>
  boolean checkComplexArrayField(Field field, String keyName, T prevArray, T currArray, HashMap<String, Integer> currRemainder)
  {
    assert (field != null);
    assert (currRemainder != null);

    if (!isOptionalityCompatible(field, prevArray, currArray))
    {
      addInfo(CompatibilityInfo.Type.VALUE_WRONG_OPTIONALITY, field.getName());
      return false;
    }

    if (prevArray == null)
    {
      return true;
    }

    assert (prevArray.getClass() == currArray.getClass());

    _path.add(field.getName());

    for (int i = 0; i < currArray.size(); ++i)
    {
      currRemainder.put(currArray.get(i).data().getString(keyName), i);
    }

    for (int i = 0; i < prevArray.size(); ++i)
    {
      // find prev and curr element with same key name
      final RecordTemplate prevElement = prevArray.get(i);
      final String prevKey = prevElement.data().getString(keyName);
      final Integer currIndex = currRemainder.get(prevKey);

      if (currIndex == null)
      {
        addInfo(CompatibilityInfo.Type.ARRAY_MISSING_ELEMENT, prevKey);
      }
      else
      {
        final RecordTemplate currElement = currArray.get(currIndex);
        currRemainder.remove(prevKey);

        _path.add(prevKey);
        checkRecordTemplate(prevElement, currElement);
        _path.remove(_path.size() - 1);
      }
    }

    _path.remove(_path.size() - 1);

    // all missing element errors have been recorded, avoid duplicate errors by returning true
    return true;
  }

  private <T extends WrappingArrayTemplate<? extends RecordTemplate>>
  boolean checkComplexArrayField(Field field, String keyName, T prevArray, T currArray)
  {
    return checkComplexArrayField(field, keyName, prevArray, currArray, new HashMap<String, Integer>());
  }

  /**
   * @return whether the check passes
   */
  private <T extends WrappingArrayTemplate<? extends RecordTemplate>>
  boolean checkEqualComplexArrayField(Field field, String keyName, T prevArray, T currArray)
  {
    final HashMap<String, Integer> currRemainder = new HashMap<String, Integer>();

    // if prev has more than curr, array missing element
    // this should catch it
    if (!checkComplexArrayField(field, keyName, prevArray, currArray, currRemainder))
    {
      return false;
    }

    // if prev has less than curr, the remainder will contain the extra current elements
    if (!currRemainder.isEmpty())
    {
      addInfo(field.getName(), CompatibilityInfo.Type.ARRAY_NOT_EQUAL, prevArray);
      return false;
    }

    return true;
  }

  /**
   * @return whether the check passes
   */
  private boolean checkParameterArrayField(Field field, ParameterSchemaArray prevArray, ParameterSchemaArray currArray)
  {
    final HashMap<String, Integer> currRemainder = new HashMap<String, Integer>();

    if (!checkComplexArrayField(field, "name", prevArray, currArray, currRemainder))
    {
      return false;
    }

    _path.add(field.getName());

    boolean result = true;
    for (int paramIndex : currRemainder.values())
    {
      // all parameters only appear in curr must not be required
      final ParameterSchema param = currArray.get(paramIndex);
      if (param.hasOptional() && param.isOptional())
      {
        addInfo(CompatibilityInfo.Type.PARAMETER_NEW_OPTIONAL, param.getName());
      }
      else
      {
        addInfo(CompatibilityInfo.Type.PARAMETER_NEW_REQUIRED, param.getName());
        result = false;
      }
    }

    _path.remove(_path.size() - 1);

    // all parameter errors have been recorded, avoid duplicate errors by returning true
    return result;
  }

  private void checkResourceSchema(ResourceSchema prevRec, ResourceSchema currRec)
  {
    checkEqualSingleValue(prevRec.schema().getField("name"),
                          prevRec.getName(GetMode.DEFAULT),
                          currRec.getName(GetMode.DEFAULT));

    checkEqualSingleValue(prevRec.schema().getField("namespace"),
                          prevRec.getNamespace(GetMode.DEFAULT),
                          currRec.getNamespace(GetMode.DEFAULT));

    checkEqualSingleValue(prevRec.schema().getField("path"),
                          prevRec.getPath(GetMode.DEFAULT),
                          currRec.getPath(GetMode.DEFAULT));

    checkEqualSingleValue(prevRec.schema().getField("schema"),
                          prevRec.getSchema(GetMode.DEFAULT),
                          currRec.getSchema(GetMode.DEFAULT));

    checkComplexField(prevRec.schema().getField("collection"), prevRec.getCollection(), currRec.getCollection());

    checkComplexField(prevRec.schema().getField("association"), prevRec.getAssociation(), currRec.getAssociation());

    checkComplexField(prevRec.schema().getField("actionsSet"), prevRec.getActionsSet(), currRec.getActionsSet());
  }

  private void checkCollectionSchema(CollectionSchema prevRec, CollectionSchema currRec)
  {
    checkComplexField(prevRec.schema().getField("identifier"),
                      prevRec.getIdentifier(GetMode.DEFAULT),
                      currRec.getIdentifier(GetMode.DEFAULT));

    checkArrayContainment(prevRec.schema().getField("supports"),
                          currRec.getSupports(GetMode.DEFAULT),
                          prevRec.getSupports(GetMode.DEFAULT));

    checkComplexArrayField(prevRec.schema().getField("methods"),
                           "method",
                           prevRec.getMethods(GetMode.DEFAULT),
                           currRec.getMethods(GetMode.DEFAULT));

    checkComplexArrayField(prevRec.schema().getField("finders"),
                           "name",
                           prevRec.getFinders(GetMode.DEFAULT),
                           currRec.getFinders(GetMode.DEFAULT));

    checkComplexArrayField(prevRec.schema().getField("actions"),
                           "name",
                           prevRec.getActions(GetMode.DEFAULT),
                           currRec.getActions(GetMode.DEFAULT));

    checkComplexField(prevRec.schema().getField("entity"),
                      prevRec.getEntity(GetMode.DEFAULT),
                      currRec.getEntity(GetMode.DEFAULT));
  }

  private void checkIdentifierSchema(IdentifierSchema prevRec, IdentifierSchema currRec)
  {
    checkEqualSingleValue(prevRec.schema().getField("name"),
                          prevRec.getName(GetMode.DEFAULT),
                          currRec.getName(GetMode.DEFAULT));

    // type in IdentifierSchema currently can only be named type, thus guaranteed to be string
    checkType("type", prevRec.getType(GetMode.DEFAULT), currRec.getType(GetMode.DEFAULT), false);

    checkType("params", prevRec.getParams(GetMode.DEFAULT), currRec.getParams(GetMode.DEFAULT), true);
  }

  private void checkFinderSchema(FinderSchema prevRec, FinderSchema currRec)
  {
    checkEqualSingleValue(prevRec.schema().getField("name"),
                          prevRec.getName(GetMode.DEFAULT),
                          currRec.getName(GetMode.DEFAULT));

    checkParameterArrayField(prevRec.schema().getField("parameters"),
                             prevRec.getParameters(GetMode.DEFAULT),
                             currRec.getParameters(GetMode.DEFAULT));

    checkComplexField(prevRec.schema().getField("metadata"),
                      prevRec.getMetadata(GetMode.DEFAULT),
                      currRec.getMetadata(GetMode.DEFAULT));

    checkEqualSingleValue(prevRec.schema().getField("assocKey"),
                          prevRec.getAssocKey(GetMode.DEFAULT),
                          currRec.getAssocKey(GetMode.DEFAULT));

    checkEqualSingleValue(prevRec.schema().getField("assocKeys"),
                          prevRec.getAssocKeys(GetMode.DEFAULT),
                          currRec.getAssocKeys(GetMode.DEFAULT));
  }

  private void checkParameterSchema(ParameterSchema prevRec, ParameterSchema currRec)
  {
    checkEqualSingleValue(prevRec.schema().getField("name"),
                          prevRec.getName(GetMode.DEFAULT),
                          currRec.getName(GetMode.DEFAULT));

    if (prevRec.hasItems() || currRec.hasItems())
    {
      checkEqualSingleValue(prevRec.schema().getField("type"), prevRec.getType(), currRec.getType());

      checkType("items", prevRec.getItems(), currRec.getItems(), true);
    }
    else
    {
      checkType("type", prevRec.getType(), currRec.getType(), false);
    }

    checkParameterOptionality(prevRec.schema().getField("optional"),
                              prevRec.isOptional(GetMode.DEFAULT),
                              currRec.isOptional(GetMode.DEFAULT));

    checkEqualSingleValue(prevRec.schema().getField("default"),
                          prevRec.getDefault(GetMode.DEFAULT),
                          currRec.getDefault(GetMode.DEFAULT));
  }

  private void checkMetadataSchema(MetadataSchema prevRec, MetadataSchema currRec)
  {
    checkType("type", prevRec.getType(), currRec.getType(), false);
  }

  private void checkActionSchema(ActionSchema prevRec, ActionSchema currRec)
  {
    checkEqualSingleValue(prevRec.schema().getField("name"),
                          prevRec.getName(GetMode.DEFAULT),
                          currRec.getName(GetMode.DEFAULT));

    checkParameterArrayField(prevRec.schema().getField("parameters"),
                             prevRec.getParameters(GetMode.DEFAULT),
                             currRec.getParameters(GetMode.DEFAULT));

    checkType("returns", prevRec.getReturns(), currRec.getReturns(), true);

    checkArrayContainment(prevRec.schema().getField("throws"),
                          prevRec.getThrows(GetMode.DEFAULT),
                          currRec.getThrows(GetMode.DEFAULT));
  }

  private void checkEntitySchema(EntitySchema prevRec, EntitySchema currRec)
  {
    checkEqualSingleValue(prevRec.schema().getField("path"),
                          prevRec.getPath(GetMode.DEFAULT),
                          currRec.getPath(GetMode.DEFAULT));

    checkComplexArrayField(prevRec.schema().getField("actions"),
                           "name",
                           prevRec.getActions(GetMode.DEFAULT),
                           currRec.getActions(GetMode.DEFAULT));

    checkComplexArrayField(prevRec.schema().getField("subresources"),
                           "name",
                           prevRec.getSubresources(GetMode.DEFAULT),
                           currRec.getSubresources(GetMode.DEFAULT));
  }

  private void checkAssociationSchema(AssociationSchema prevRec, AssociationSchema currRec)
  {
    checkEqualComplexArrayField(prevRec.schema().getField("assocKeys"),
                                "name",
                                prevRec.getAssocKeys(GetMode.DEFAULT),
                                currRec.getAssocKeys(GetMode.DEFAULT));

    checkArrayContainment(prevRec.schema().getField("supports"),
                          currRec.getSupports(GetMode.DEFAULT),
                          prevRec.getSupports(GetMode.DEFAULT));

    checkComplexArrayField(prevRec.schema().getField("methods"),
                           "method",
                           prevRec.getMethods(GetMode.DEFAULT),
                           currRec.getMethods(GetMode.DEFAULT));

    checkComplexArrayField(prevRec.schema().getField("finders"),
                           "name",
                           prevRec.getFinders(GetMode.DEFAULT),
                           currRec.getFinders(GetMode.DEFAULT));

    checkComplexArrayField(prevRec.schema().getField("actions"),
                           "name",
                           prevRec.getActions(GetMode.DEFAULT),
                           currRec.getActions(GetMode.DEFAULT));

    checkComplexField(prevRec.schema().getField("entity"),
                      prevRec.getEntity(GetMode.DEFAULT),
                      currRec.getEntity(GetMode.DEFAULT));
  }

  private void checkAssocKeySchema(AssocKeySchema prevRec, AssocKeySchema currRec)
  {
    checkEqualSingleValue(prevRec.schema().getField("name"),
                          prevRec.getName(GetMode.DEFAULT),
                          currRec.getName(GetMode.DEFAULT));

    checkType("type", prevRec.getType(GetMode.DEFAULT), currRec.getType(GetMode.DEFAULT), false);
  }

  private void checkActionsSetSchema(ActionsSetSchema prevRec, ActionsSetSchema currRec)
  {
    checkComplexArrayField(prevRec.schema().getField("actions"),
                           "name",
                           prevRec.getActions(GetMode.DEFAULT),
                           currRec.getActions(GetMode.DEFAULT));
  }

  private void checkRestMethodSchema(RestMethodSchema prevRec, RestMethodSchema currRec)
  {
    checkEqualSingleValue(prevRec.schema().getField("method"),
                          prevRec.getMethod(GetMode.DEFAULT),
                          currRec.getMethod(GetMode.DEFAULT));

    checkParameterArrayField(prevRec.schema().getField("parameters"),
                             prevRec.getParameters(GetMode.DEFAULT),
                             currRec.getParameters(GetMode.DEFAULT));
  }

  private static final String GENERATOR_RESOLVER_PATH = "generator.resolver.path";
  private static final RestSpecCodec _codec = new RestSpecCodec();
  private static final Logger log = LoggerFactory.getLogger(RestLiResourceModelCompatibilityChecker.class);

  private final DataSchemaResolver _schemaResolver;
  private final Map<CompatibilityInfo.Level, List<CompatibilityInfo>> _info = new HashMap<CompatibilityInfo.Level, List<CompatibilityInfo>>();
  private String _prevRestspecPath;
  private String _currRestspecPath;

  private List<Object> _path;
}
