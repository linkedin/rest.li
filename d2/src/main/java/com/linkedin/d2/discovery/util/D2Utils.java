/**
 * $Id: $
 */

package com.linkedin.d2.discovery.util;

/**
 * @author David Hoa
 * @version $Revision: $
 */

public class D2Utils
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
    if (suffix != null && !suffix.isEmpty())
    {
      strBuilder.append("-").append(suffix);
    }
    return strBuilder.toString();
  }
}
