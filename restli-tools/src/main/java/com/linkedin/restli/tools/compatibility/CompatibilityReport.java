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

    String header = String.format("[Rest Spec: %b\t|\tModel: %b]", _infoMap.isRestSpecCompatible(_compatibilityLevel),
                                  _infoMap.isModelCompatible(_compatibilityLevel));

    return Arrays.asList(header, restSpecCompat, restSpecIncompat, modelCompat, modelIncompat)
        .stream()
        .filter(it -> !it.isEmpty())
        .collect(Collectors.joining("\n")) + '\n';
  }
}
