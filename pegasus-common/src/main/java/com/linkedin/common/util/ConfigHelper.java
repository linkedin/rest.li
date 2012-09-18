/*
   Copyright (c) 2012 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

/**
 * $Id: ConfigHelper.java 33147 2007-11-18 22:29:12Z dmccutch $ */
package com.linkedin.common.util;

/**
 * This class contains static methods to help with config. Mostly it is to be
 * used by the inner <code>Config</code> classes created for dependency injection.
 *
 * @author Yan Pujante
 */
public class ConfigHelper
{
  public static final String MODULE = ConfigHelper.class.getName();

  public static final Integer UNDEFINED_INTEGER = null;
  public static final Long UNDEFINED_LONG = null;
  public static final String UNDEFINED_STRING = null;
  public static final Boolean UNDEFINED_BOOLEAN = null;

  /**
   * Exception thrown when parameter is missing from config.. it is a
   * runtime exception */
  public static class MissingConfigParameterException extends IllegalArgumentException
  {
    private static final long serialVersionUID = 1L;

    public MissingConfigParameterException()
    {
      super();
    }

    public MissingConfigParameterException(final String msg)
    {
      super(msg);
    }
  }

  /**
   * Constructor
   */
  private ConfigHelper()
  {
  }

  /**
   * Given an object object, checks if it is defined. If not throws an exception.
   * If yes returns the value.
   *
   * @param o
   * @return the object itself after checking that it is defined
   * @throws MissingConfigParameterException if it is not defined
   */
  public static Object getRequiredObject(final Object o) throws MissingConfigParameterException
  {
    if (o == null)
    {
      throw new MissingConfigParameterException("required Object has not been defined");
    }

    return o;
  }

  /**
   * The templatized version of the call.. to avoid downcast all the time!
   *
   * @param o
   * @return the object itself after checking that it is defined
   * @throws MissingConfigParameterException if it is not defined
   */
  public static <T> T getRequired(final T o) throws MissingConfigParameterException
  {
    if (o == null)
    {
      throw new MissingConfigParameterException("required Object has not been defined");
    }

    return o;
  }
}
