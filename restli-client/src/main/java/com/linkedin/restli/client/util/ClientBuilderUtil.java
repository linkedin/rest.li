/**
 * $Id: $
 */

package com.linkedin.restli.client.util;

/**
 * @author David Hoa
 * @version $Revision: $
 */

public class ClientBuilderUtil
{
  /**
   * addSuffixToBaseName will mutate a base name with a suffix in a known fashion.
   *
   * @param baseName
   * @param suffix
   * @return
   */
  public static String addSuffixToBaseName(String baseName, String suffix)
  {
    StringBuilder strBuilder = new StringBuilder();
    strBuilder.append(baseName);
    if (suffix != null)
    {
      strBuilder.append("-").append(suffix);
    }
    return strBuilder.toString();
  }
}
