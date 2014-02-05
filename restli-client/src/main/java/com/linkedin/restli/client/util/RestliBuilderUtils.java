/**
 * $Id: $
 */

package com.linkedin.restli.client.util;


import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.internal.AllProtocolVersions;
import com.linkedin.restli.internal.common.URLEscaper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This class holds methods useful when manipulating Client Builders.
 *
 * @author David Hoa
 * @version $Revision: $
 */

public class RestliBuilderUtils
{
  /**
   * Get the static original resource name from the client builder.
   *
   * @param type to get the primary resource name from. Has to be a master client builder, which is
   *             the builder that corresponds directly to the restli resource, and from which
   *             sub-builders are created.
   * @return name of the resource
   * @throws Runtime exception if unable to get the resource name from the builder
   */
  public static String getPrimaryResourceName(Class<?> type)
  {
    try
    {
      Method resourceMethod = type.getDeclaredMethod("getPrimaryResource");
      return (String)resourceMethod.invoke(null);
    }
    catch (IllegalAccessException e)
    {
      throw new RuntimeException("Error evaluating resource name for class: " + type.getName(), e);
    }
    catch (NoSuchMethodException e)
    {
      throw new RuntimeException("Class missing resource field: " + type.getName(), e);
    }
    catch (InvocationTargetException e)
    {
      throw new RuntimeException("Unable to instantiate class: " + type.getName(), e);
    }
  }

  /**
   * Depending on whether or not this is a complex resource key, return either a full (key + params)
   * string representation of the key (which for ComplexResource includes both key and params), or
   * simply key.toString() (which for ComplexResourceKey only uses key part).
   */
  public static String keyToString(Object key, URLEscaper.Escaping escaping, ProtocolVersion version)
  {
    String result;
    version = (version == null) ? AllProtocolVersions.DEFAULT_PROTOCOL_VERSION : version;
    if (key == null)
    {
      result = null;
    }
    else if (key instanceof ComplexResourceKey)
    {
      result = ((ComplexResourceKey<?,?>)key).toStringFull(escaping);
    }
    else if (key instanceof CompoundKey)
    {
      result = key.toString(); // already escaped
    }
    else
    {
      result = URLEscaper.escape(DataTemplateUtil.stringify(key), escaping);
    }
    return result;
  }
}
