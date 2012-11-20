package com.linkedin.restli.docgen;


import com.linkedin.restli.restspec.ResourceSchema;


/**
 * @author Keren Jin
 */
public class ResourceSchemaUtil
{
  public static String getFullName(ResourceSchema schema)
  {
    if (!schema.hasNamespace())
    {
      return schema.getName();
    }
    final String namespace = schema.getNamespace();
    if (namespace.isEmpty())
    {
      return schema.getName();
    }
    else
    {
      return namespace + "." + schema.getName();
    }
  }
}
