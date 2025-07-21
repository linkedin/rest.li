/**
 * $Id: $
 */

package com.linkedin.d2.discovery.util;

import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.zkfs.ZKFSUtil;
import com.linkedin.d2.discovery.stores.zk.SymlinkUtil;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author David Hoa
 * @version $Revision: $
 */

public class D2Utils
{
  private static final Logger LOG = LoggerFactory.getLogger(D2Utils.class);
  private static final String RAW_D2_CLIENT_BASE_PATH = "/d2/rawD2ClientBuilders";
  private static final String USR_DIR_SYS_PROPERTY = "user.dir";
  private static final String SPARK_APP_NAME = "spark.app.name";
  private static final String APP_NAME = "com.linkedin.app.name";
  private static final String SAMZA_CONTAINER_NAME = "samza.container.name";
  private static final String USER_DIR_EXPORT_CONTENT_PREFIX = "/export/content/lid/apps/";
  // This is needed to avoid creating Zookeeper node for testing and dev environments
  private static final Set<String> USR_DIRS_TO_EXCLUDE = Stream.of(
      "/dev-",
      "/dev/",
      "/multiproduct-post-commit-mpdep/" // post-commit runs
  ).collect(Collectors.toSet());

  // Keeping the max threshold to 10K, this would ensure that we accidentally won't create more than max ZK tracking nodes.
  public static final int RAW_D2_CLIENT_MAX_TRACKING_NODE = 1000;

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

  public static Boolean isAppToExclude()
  {
    String userDir = System.getProperties().getProperty(USR_DIR_SYS_PROPERTY);
    return userDir != null && USR_DIRS_TO_EXCLUDE.stream().anyMatch(userDir::contains);
  }

  // ZK don't allow / in the node name, we are replacing / with -, This name would be unique for each app.
  // for example: export-content-lid-apps-indis-canary-install nodeName is being used.
  public static String getAppIdentityName()
  {
    Properties properties = System.getProperties();
    if (properties.getProperty(SPARK_APP_NAME) != null)
    {
      return properties.getProperty(SPARK_APP_NAME);
    }

    // for samza jobs using the app name
    if (properties.getProperty(APP_NAME) != null)
    {
      return properties.getProperty(APP_NAME);
    }

    // for samza jobs using the container name
    if (properties.getProperty(SAMZA_CONTAINER_NAME) != null)
    {
      return properties.getProperty(SAMZA_CONTAINER_NAME);
    }

    // Process user.dir property to identify the app
    String userDir = properties.getProperty(USR_DIR_SYS_PROPERTY);
    if (userDir.startsWith(USER_DIR_EXPORT_CONTENT_PREFIX))
    {
      // sample: /export/content/lid/apps/seas-cloud-searcher/11ed246acf2e0be26bd44b29fb620df45ca14481
      int slashAfterAppName = userDir.indexOf('/', USER_DIR_EXPORT_CONTENT_PREFIX.length());
      if (slashAfterAppName > 0)
      {
        // if there is a slash after the app name, we will use the part before the slash as the identifier
        // e.g. /export/content/lid/apps/seas-cloud-searcher/11ed246acf2e0be26bd44b29fb620df45ca14481 becomes
        // /export/content/lid/apps/seas-cloud-searcher
        userDir = userDir.substring(0, slashAfterAppName);
      }
      userDir = userDir.replace("/", "-");
    }
    else {
      // sample usr.dir:
      // /grid/g/tmp/yarn/usercache/seascloud/appcache/application_1747631859816_3737754/container_e42_1747631859816_3737754_01_000011
      List<String> parts = Arrays.stream(userDir.split("/"))
          .collect(Collectors.toList());
      if (parts.size() > 2)
      {
        // remove the last and second last parts, which are usually random strings
        parts.remove(parts.size() - 1);
        parts.remove(parts.size() - 1);
      }
      userDir = String.join("-", parts.toArray(new String[0]));
    }

    LOG.info("user.dir for raw D2 Client usages: {}", userDir);
    return userDir.substring(1); // remove the leading slash
  }

  public static String getRawClientTrackingPath()
  {
    return RAW_D2_CLIENT_BASE_PATH + "/" + getAppIdentityName();
  }

  public static String getRawClientTrackingBasePath()
  {
    return RAW_D2_CLIENT_BASE_PATH;
  }
}
