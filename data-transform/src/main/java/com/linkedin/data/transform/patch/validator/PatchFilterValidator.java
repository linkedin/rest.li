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

package com.linkedin.data.transform.patch.validator;


import com.linkedin.data.DataMap;
import com.linkedin.data.element.DataElement;
import com.linkedin.data.schema.validator.AbstractValidator;
import com.linkedin.data.schema.validator.Validator;
import com.linkedin.data.schema.validator.ValidatorContext;
import com.linkedin.data.transform.patch.PatchConstants;
import java.util.ArrayList;


/**
 * {@link Validator} that filters validate calls to the next downstream {@link Validator}
 * based on the actual data that have been set according the patch operations {@link DataMap}.
 * <p>
 *
 * There are several modes of operation, specified by the {@code mode} argument to
 * the constructor. If the mode is {@link Mode#SET_ONLY}, actual data set (includes the
 * data and its children) will be passed to next validator.
 * If the mode is {@link Mode#PARENT_AND_SET}, then immediate parents with
 * modified data and actual data set will be passed to the next validator.
 * If the mode is {@link Mode#ANCESTOR_AND_SET}, then ancestors with modified
 * data and actual set data will be passed to the next validator.
 * <p>
 *
 * For example, if we have the following {@link DataMap},
 * <code>
 *   {
 *     "fooInt" : 2,
 *     "fooString" : "x",
 *     "bar" : {
 *       "barInt" : 3,
 *       "barString" : "y",
 *       "baz" : {
 *         "bazInt" : 4,
 *         "bazString" : "z"
 *       }
 *     }
 *   }
 * </code>
 *
 * If the patch operation is:
 * <code>
 *  {
 *    "$set" : {
 *      "fooInt" : 2
 *    }
 *  }
 * </code>
 * If the mode is {@link Mode#SET_ONLY}, then the next downstream {@link Validator}
 * will be called for "/fooInt". If the mode is {@link Mode#PARENT_AND_SET}, then the next
 * downstream {@link Validator} will be called for "" (the root {@link DataMap}).
 * and "/fooInt".
 * If the mode is {@link Mode#ANCESTOR_AND_SET}, then the next downstream {@link Validator}
 * will be called for "", "/fooInt".
 * <p>
 *
 * If the patch operation is:
 * <code>
 *   {
 *     "bar" : {
 *       "$set" : {
 *         "barInt" : 3
 *       }
 *     }
 *   }
 * </code>
 * If the mode is {@link Mode#SET_ONLY}, then the next downstream {@link Validator}
 * will be called for "/bar/barInt". If the mode is {@link Mode#PARENT_AND_SET}, then the next
 * downstream {@link Validator} will be called for "/bar" and "/bar/barInt".
 * If the mode is {@link Mode#ANCESTOR_AND_SET}, then the next downstream {@link Validator}
 * will be called for "", "/bar", "/bar/barInt".
 * <p>
 *
 * If the patch operation is:
 * <code>
 *   {
 *     "bar" : {
 *       "$set" : {
 *         "baz" : {
 *           "bazInt" : 4,
 *           "bazString" : "z"
 *         }
 *       }
 *     }
 *   }
 * </code>
 * If the mode is {@link Mode#SET_ONLY}, then the next downstream {@link Validator}
 * will be called for "/bar/baz", "/bar/baz/bazInt", "/bar/baz/bazString".
 * If the mode is {@link Mode#PARENT_AND_SET}, then the next downstream {@link Validator}
 * will also be called for "/bar", "/bar/baz", "/bar/baz/bazInt" and "/bar/baz/bazString".
 * If the mode is {@link Mode#ANCESTOR_AND_SET}, then the next downstream {@link Validator}
 * will also be called for "", "/bar", "/bar/baz", "/bar/baz/bazInt" and "/bar/baz/bazString".
 * <p>
 *
 * In some cases, the patch operation may be applied to a Data object that is not the
 * root Data object. In these cases, this validator will need to
 * know which Data object was the patch operation applied to.
 *
 * For example, the following patch operation could be applied to "/bar/baz".
 * <code>
 *   {
 *     "$set" : {
 *       "bazInt" : 4,
 *       "bazString" : "z"
 *     }
 *   }
 * </code>
 * If the mode is {@link Mode#SET_ONLY}, then the next downstream {@link Validator}
 * will be called for "/bar/baz/bazInt", "/bar/baz/bazString".
 * If the mode is {@link Mode#PARENT_AND_SET}, then the next downstream {@link Validator}
 * will also be called for "/bar/baz", "/bar/baz/bazInt" and "/bar/baz/bazString".
 * If the mode is {@link Mode#ANCESTOR_AND_SET}, then the next downstream {@link Validator}
 * will also be called for "", "/bar", "/bar/baz", "/bar/baz/bazInt" and "/bar/baz/bazString".
 */
public class PatchFilterValidator extends AbstractValidator
{
  public static enum Mode
  {
    SET_ONLY,
    PARENT_AND_SET,
    ANCESTOR_AND_SET
  }

  private final Validator _nextValidator;
  private final Object[] _patchedPath;
  private final DataMap _opMap;
  private final Mode _mode;
  private final ArrayList<Object> _path = new ArrayList<>();
  private static final Object[] _emptyPath = {};

  protected static enum Status
  {
    NONE,
    IS_SET_VALUE,
    IS_CHILD_MODIFIED,
    IS_DESCENDANT_MODIFIED
  }

  /**
   * Constructor.
   *
   * This constructor assumes that the patch has been applied to root Data object.
   *
   * @param nextValidator provides the next downstream {@link Validator}.
   * @param opMap provides the {@link DataMap} representing the patch operation.
   * @param mode provides the desired filtering mode.
   */
  public PatchFilterValidator(Validator nextValidator, DataMap opMap, Mode mode)
  {
    this(nextValidator, opMap, mode, _emptyPath);
  }

  /**
   * Constructor that includes the {@link DataElement} where the patch has been applied.
   *
   * This constructor should be used if the patch operation is not applied to the
   * root Data object. For example, a patch operation may be applied to Data object at
   * "/bar/baz" instead the root Data object.
   *
   * @param nextValidator provides the next downstream {@link Validator}.
   * @param opMap provides the {@link DataMap} representing the patch operation.
   * @param mode provides the desired filtering mode.
   * @param patchedElement provides the {@link DataElement} where the patch has been applied.
   */
  public PatchFilterValidator(Validator nextValidator, DataMap opMap, Mode mode, DataElement patchedElement)
  {
    this(nextValidator, opMap, mode, patchedElement.path());
  }

  /**
   * Constructor that includes the path (from the root Data object) where the patch has been applied.
   *
   * This constructor should be used if the patch operation is not applied to the
   * root Data object. For example, a patch operation may be applied to Data object at
   * "/bar/baz" instead the root Data object.
   *
   * @param nextValidator provides the next downstream {@link Validator}.
   * @param opMap provides the {@link DataMap} representing the patch operation.
   * @param mode provides the desired filtering mode.
   * @param patchedPath provides the path from the root Data object where the patch has been applied.
   */
  public PatchFilterValidator(Validator nextValidator, DataMap opMap, Mode mode, Object[] patchedPath)
  {
    super(opMap);
    _nextValidator = nextValidator;
    _patchedPath = patchedPath;
    _opMap = opMap;
    _mode = mode;
  }

  @Override
  public void validate(ValidatorContext context)
  {
    Status status = determineStatus(context.dataElement());
    if (status == Status.IS_SET_VALUE ||
        (status == Status.IS_CHILD_MODIFIED && (_mode == Mode.PARENT_AND_SET || _mode == Mode.ANCESTOR_AND_SET)) ||
        (status == Status.IS_DESCENDANT_MODIFIED && (_mode == Mode.ANCESTOR_AND_SET)))
    {
      _nextValidator.validate(context);
    }
  }

  protected Status determineStatus(DataElement element)
  {
    Status status = null;
    element.pathAsList(_path);
    DataMap currentOpMap = _opMap;
    int i;
    for (i = 0; i < _path.size(); i++)
    {
      Object pathComponent = _path.get(i);
      if (i < _patchedPath.length)
      {
        if (_patchedPath[i].equals(pathComponent) == false)
        {
          status = Status.NONE;
          break;
        }
        status = Status.IS_DESCENDANT_MODIFIED;
        continue;
      }
      Object nextValue = currentOpMap.get(pathComponent);
      if (nextValue == null || nextValue.getClass() != DataMap.class)
      {
        Object setValue = currentOpMap.get(PatchConstants.SET_COMMAND);
        if (setValue != null && setValue.getClass() == DataMap.class &&
            ((DataMap) setValue).get(pathComponent) != null)
        {
          status = Status.IS_SET_VALUE;
          break;
        }
        status = Status.NONE;
        break;
      }
      status = Status.IS_DESCENDANT_MODIFIED;
      currentOpMap = (DataMap) nextValue;
    }
    if (i == _path.size())
    {
      if (i >= _patchedPath.length)
      {
        for (String keys : currentOpMap.keySet())
        {
          if (keys.startsWith(PatchConstants.COMMAND_PREFIX))
          {
            status = Status.IS_CHILD_MODIFIED;
            break;
          }
        }
      }
      if (status != Status.IS_CHILD_MODIFIED && i <= _patchedPath.length && ! currentOpMap.isEmpty())
      {
        status = Status.IS_DESCENDANT_MODIFIED;
      }
    }
    else if (status == null)
    {
      status = Status.NONE;
    }
    return status;
  }
}
