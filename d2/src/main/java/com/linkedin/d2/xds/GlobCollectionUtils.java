package com.linkedin.d2.xds;

public class GlobCollectionUtils
{
  private static final String D2_URI_NODE_GLOB_COLLECTION_PREFIX = "xdstp:///indis.D2URI/";
  private static final String GLOB_COLLECTION_SUFFIX = "/*";

  private GlobCollectionUtils()
  {
  }

  /**
   * Extracts the glob collection URL (e.g. xdstp:///indis.D2URI/FooCluster/*) from the given resource URN.
   */
  public static String globCollectionUrlFromResourceUrn(String resourceUrn)
  {
    return resourceUrn.substring(0, resourceUrn.lastIndexOf('/')) + GLOB_COLLECTION_SUFFIX;
  }

  /**
   * Extracts the URI from the resource URN. Resource URNs for indis.D2URIs have the format of:<pre>
   *   xdstp:///indis.D2URI/{CLUSTER}/{URL_ESCAPED_ANNOUNCER}
   * </pre>
   */
  public static String uriFromResourceUrn(String resourceUrn)
  {
    return resourceUrn.substring(resourceUrn.lastIndexOf('/'));
  }

  /**
   * Returns the glob collection URL for the cluster, which has the format:<pre>
   *   xdstp:///indis.D2URI/{CLUSTER}/*
   * </pre>
   * Note that CLUSTER in this case does not include the /d2/uris/ prefix, it is only the cluster name.
   */
  public static String globCollectionUrlForCluster(String cluster)
  {
    return D2_URI_NODE_GLOB_COLLECTION_PREFIX + cluster + GLOB_COLLECTION_SUFFIX;
  }

}
