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

package com.linkedin.data.schema.compatibility;


/**
 * Specifies how compatibility checks should be performed.
 *
 * <p>
 * Two schemas are compatible if one of the following is true:
 * <ul>
 *   <li>Both schemas are arrays and their item schemas are also compatible.
 *   <li>Both schemas are maps and their value schemas are also compatible.
 *   <li>Both schemas are enums and they have same set of symbols.
 *   <li>Both schemas are fixed and they have the same sizes.
 *   <li>Both schemas are records and they have the same set of required fields,
 *       and the schemas of fields with the same name are also compatible.
 *       Fields are matched by name, i.e. order does not matter.
 *   <li>Both schemas are unions and they have the same set of members with matching
 *       schema and alias (if the members are aliased) for each member.
 *   <li>Both schemas are primitive and non-numeric (i.e. bytes, string, boolean),
 *       their types are the same.
 *   <li>Both schemas are numeric, numeric promotion is enabled
 *       and the older type is promotable to the newer type. An int is promotable to
 *       a long, a float or a double, a long is promotable to a float or a double,
 *       and a float is promotable to a double.
 *   <li>Both schemas are numeric and numeric promotion is not enabled and
 *       their types are the same.
 * </ul>
 *
 * If {@link Mode#DATA} is selected, typeref's are dereferenced and
 * only the terminal referenced schema is compared.
 * <p>
 * If {@link Mode#SCHEMA} is selected, typeref's must also be compatible, i.e.
 * both schemas are typerefs and their referenced schemas are also compatible.
 * <p>
 * If {@link #isCheckNames()} is set, then the names of the named schemas
 * must also be the same. The named schemas include record, enum, fixed, and typeref.
 * <p>
 * If {@link #isAllowPromotions()} is set, then numeric type promotions are allowed.
 * <p>
 */
public class CompatibilityOptions
{
  /**
   * Provides the compatibility check mode.
   * <p>
   * The {@link #DATA} mode checks if the data representation,
   * e.g. in-memory as Data objects or serialized to JSON,
   * are compatible.
   * <p>
   * The {@link #SCHEMA} mode is more strict than {@link #DATA}
   * mode. Not only must the data representation be compatible,
   * typeref's are included in the compatibility check and the
   * typeref's must also be compatible.
   */
  public enum Mode
  {
    /**
     * Check whether the data representation is compatible, excludes
     * checking typeref compatibility.
     */
    DATA,
    /**
     * Check whether the schema is compatible, includes checking
     * typeref compatibility.
     */
    SCHEMA,

    /**
     * Check whether the schema is compatible for extension schemas, allowing adding required record field.
     */
    EXTENSION
  }

  /**
   * Default constructor, sets {@link Mode} to {@link Mode#DATA} and
   * {@link #isCheckNames()} to true, {@link #isAllowPromotions()} to false.
   */
  public CompatibilityOptions()
  {
    _mode = Mode.DATA;
    _checkNames = true;
    _allowPromotions = false;
  }

  /**
   * Get the compatibility mode.
   *
   * @return the compatibility mode.
   */
  public Mode getMode()
  {
    return _mode;
  }

  /**
   * Set the compatibility mode.
   *
   * @param mode provides the compatibility mode to set to.
   * @return this instance.
   */
  public CompatibilityOptions setMode(Mode mode)
  {
    _mode = mode;
    return this;
  }

  /**
   * Whether the names of corresponding named schemas should be the same.
   *
   * @return whether the names of corresponding named schemas should be the same.
   */
  public boolean isCheckNames()
  {
    return _checkNames;
  }

  /**
   * Sets whether the names of corresponding named schemas should be the same.
   *
   * @param checkNames provides whether the names of corresponding named schemas should be the same.
   * @return this instance.
   */
  public CompatibilityOptions setCheckNames(boolean checkNames)
  {
    _checkNames = checkNames;
    return this;
  }

  /**
   * Whether numeric promotions are allowed.
   *
   * @return whether numeric promotions are allowed.
   */
  public boolean isAllowPromotions()
  {
    return _allowPromotions;
  }

  /**
   * Sets whether numeric promotions are allowed.
   *
   * @param allowPromotions provides whether numeric promotions are allowed.
   * @return this instance.
   */
  public CompatibilityOptions setAllowPromotions(boolean allowPromotions)
  {
    _allowPromotions = allowPromotions;
    return this;
  }

  private Mode _mode;
  private boolean _checkNames;
  private boolean _allowPromotions;
}
