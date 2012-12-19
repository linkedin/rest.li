package com.linkedin.data.template;

public interface HasTyperefInfo
{
  /**
   * Return the {@link TyperefInfo} of the enclosing typeref that provides the name of the class.
   *
   * @return return the {@link TyperefInfo} of the enclosing typeref that provides the name of the class.
   */
  TyperefInfo typerefInfo();
}
