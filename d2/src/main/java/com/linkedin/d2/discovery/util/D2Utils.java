/**
 * $Id: $
 */

package com.linkedin.d2.discovery.util;

import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.zkfs.ZKFSUtil;
import com.linkedin.d2.discovery.stores.zk.SymlinkUtil;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;


/**
 * @author David Hoa
 * @version $Revision: $
 */

public class D2Utils
{
  // A set of system properties to be excluded as they are lengthy, not needed, etc.
  private static final Set<String> SYSTEM_PROPS_TO_EXCLUDE = Stream.of(
      "jdk.debug",
      "line.separator",
      "java.class.path",
      "java.vm.inputarguments"
  ).collect(Collectors.toSet());
  /**
   * addSuffixToBaseName will mutate a base name with a suffix in a known fashion.
   *
   * @param baseName original string (can be cluster name or service name) to mutate
   * @param suffix string to append in a known fashion
   * @return new string that is a combination of baseName and suffix
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

  /**
   * addMasterToBaseName will append the Master suffix to a passed in base name.
   *
   * @param baseName original string (can be cluster name or service name)
   * @return baseName + "Master"
   */
  public static String addMasterToBaseName(String baseName)
  {
    return baseName + PropertyKeys.MASTER_SUFFIX;
  }

  public static String getSymlinkNameForMaster(String clusterName)
  {
    return SymlinkUtil.SYMLINK_PREFIX + clusterName + PropertyKeys.MASTER_SUFFIX;
  }

  public static String getServicePathAsChildOfCluster(String clusterName, String serviceName, @Nonnull String basePath)
  {
    return ZKFSUtil.clusterPath(basePath) + "/" + clusterName + "/" + serviceName;
  }

  /**
   * System properties could include properties set by LinkedIn, Java, Zookeeper, and more. It will be logged or saved
   * on a znode to reveal identities of apps that are using hard-coded D2ClientBuilder.
   * @return A string of system properties.
   */
  public static String getSystemProperties()
  {
    StringBuilder properties = new StringBuilder();
    System.getProperties().forEach((k, v) -> {
      if (!SYSTEM_PROPS_TO_EXCLUDE.contains(k.toString())) {
        properties.append(k).append(" = ").append(v).append("\n");
      }
    });
    return properties.toString();
  }

  public static String getUserDir(){
    // TODO: check if ZK path can have / in it.
    return System.getProperties().getProperty("user.dir");
  }
}
