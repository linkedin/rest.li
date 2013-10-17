package com.linkedin.restli.common;


/**
 * @author kparikh
 */
public class OperationNameGenerator
{
  /**
   * Builds the operation string for a method
   * @param method
   * @param methodName
   * @return
   */
  public static String generate(ResourceMethod method, String methodName)
  {
    String operation = method.toString();
    final String ACTION_AND_FINDER_SEPARATOR = ":";

    switch (method)
    {
      case ACTION:
        operation += (ACTION_AND_FINDER_SEPARATOR + methodName);
        break;
      case FINDER:
        operation += (ACTION_AND_FINDER_SEPARATOR + methodName);
        break;
    }
    return operation;
  }
}
