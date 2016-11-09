package com.linkedin.restli.tools.compatibility;


import com.linkedin.restli.tools.idlcheck.CompatibilityInfo;
import com.linkedin.restli.tools.idlcheck.CompatibilityLevel;

import java.util.Arrays;
import java.util.stream.Collectors;


public class CompatibilityReport
{
  private final CompatibilityInfoMap _infoMap;
  private final CompatibilityLevel _compatibilityLevel;

  public CompatibilityReport(CompatibilityInfoMap infoMap, CompatibilityLevel compatibilityLevel)
  {
    _infoMap = infoMap;
    _compatibilityLevel = compatibilityLevel;
  }

  /**
   * Returns a report of the compatibility info map based on the compatibility level. Each line is prefixed with
   * a line type enclosed in []. Supported report line types are:
   * <ul>
   *   <li>[RS-C] - String describing a restspec change that is backward compatible.</li>
   *   <li>[RS-I] - String describing a restspec change that is backward incompatible.</li>
   *   <li>[MD-C] - String describing a model(PDSC) change that is backward compatible.</li>
   *   <li>[MD-C] - String describing a model(PDSC) change that is backward incompatible.</li>
   *   <li>[RS-COMPAT] - Boolean indicating if the full compatibility check was restspec backward compatible for the provided compatibilityLevel</li>
   *   <li>[MD-COMPAT] - Boolean indicating if the full compatibility check was model backward compatible for the provided compatibilityLevel</li>
   * </ul>
   *
   * This report will then be parsed by {@code CompatibilityLogChecker} to provide results to the pegasus gradle plugin.
   */
  public String createReport()
  {
    String restSpecCompat = _infoMap.getRestSpecInfo(CompatibilityInfo.Level.COMPATIBLE)
        .stream()
        .map(it -> "[RS-C]:" + it)
        .collect(Collectors.joining("\n"));

    String restSpecIncompat = _infoMap.getRestSpecInfo(CompatibilityInfo.Level.INCOMPATIBLE)
        .stream()
        .map(it -> "[RS-I]:" + it)
        .collect(Collectors.joining("\n"));

    String modelCompat = _infoMap.getModelInfo(CompatibilityInfo.Level.COMPATIBLE)
        .stream()
        .map(it -> "[MD-C]:" + it)
        .collect(Collectors.joining("\n"));

    String modelIncompat = _infoMap.getModelInfo(CompatibilityInfo.Level.INCOMPATIBLE)
        .stream()
        .map(it -> "[MD-I]:" + it)
        .collect(Collectors.joining("\n"));

    String restSpecIsCompat = String.format("[RS-COMPAT]: %b", _infoMap.isRestSpecCompatible(_compatibilityLevel));

    String modelIsCompat = String.format("[MD-COMPAT]: %b", _infoMap.isModelCompatible(_compatibilityLevel));

    return Arrays.asList(restSpecIsCompat, modelIsCompat, restSpecCompat, restSpecIncompat, modelCompat, modelIncompat)
        .stream()
        .filter(it -> !it.isEmpty())
        .collect(Collectors.joining("\n")) + '\n';
  }
}
