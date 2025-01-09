package com.linkedin.d2.xds;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.annotation.Nullable;

public class GlobCollectionUtils
{
  private static final String D2_URIS_PREFIX = "/d2/uris/";
  private static final String D2_URI_NODE_GLOB_COLLECTION_PREFIX = "xdstp:///indis.D2URI/";
  private static final String GLOB_COLLECTION_SUFFIX = "/*";

  private GlobCollectionUtils()
  {
  }

  public static class D2UriIdentifier
  {
    private final String _clusterResourceName;
    private final String _uriName;

    private D2UriIdentifier(String clusterResourceName, String name)
    {
      _clusterResourceName = clusterResourceName;
      _uriName = name;
    }

    /**
     * Returns the cluster resource's name, i.e. {@code /d2/uris/{CLUSTER_NAME}}.
     */
    public String getClusterResourceName()
    {
      return _clusterResourceName;
    }

    /**
     * Returns the name of the URI within that cluster.
     */
    public String getUriName()
    {
      return _uriName;
    }

    /**
     * Parses the given resource name into a {@link D2UriIdentifier}. URNs for {@code indis.D2URIs} have the format of:
     * <pre>
     *   xdstp:///indis.D2URI/{CLUSTER}/{NAME}
     * </pre>
     *
     * @return The parse {@link D2UriIdentifier} if the given resource name is a valid URN, otherwise {@code null}.
     */
    @Nullable
    public static D2UriIdentifier parse(String resourceName)
    {
      if (!resourceName.startsWith(D2_URI_NODE_GLOB_COLLECTION_PREFIX))
      {
        return null;
      }

      int lastIndex = resourceName.lastIndexOf('/');
      if (lastIndex == -1)
      {
        return null;
      }

      String clusterName = resourceName.substring(D2_URI_NODE_GLOB_COLLECTION_PREFIX.length(), lastIndex);

      return new D2UriIdentifier(D2_URIS_PREFIX + clusterName, resourceName.substring(lastIndex + 1));
    }
  }

  /**
   * Returns the glob collection URL for the cluster, which has the format:<pre>
   *   xdstp:///indis.D2URI/{CLUSTER_NAME}/*
   * </pre>
   *
   * @param clusterPath The full path to the cluster, including the "/d2/uris/" prefix. This matches the resource name
   *                    for the D2URIMap type.
   */
  public static String globCollectionUrlForClusterResource(String clusterPath)
  {
    return D2_URI_NODE_GLOB_COLLECTION_PREFIX +
        clusterPath.substring(clusterPath.lastIndexOf('/') + 1) +
        GLOB_COLLECTION_SUFFIX;
  }

  public static String globCollectionUrn(String clusterName, String uri)
  {
    try
    {
      return D2_URI_NODE_GLOB_COLLECTION_PREFIX + clusterName + "/" + URLEncoder.encode(uri, "UTF-8");
    }
    catch (UnsupportedEncodingException e)
    {
      // Note that this is impossible. It is only thrown if the charset isn't recognized, and UTF-8 is known to be
      // supported.
      throw new RuntimeException(e);
    }

  }
}
