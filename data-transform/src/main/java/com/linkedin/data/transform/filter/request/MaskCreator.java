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
 * $Id: $
 */

package com.linkedin.data.transform.filter.request;

import com.linkedin.data.schema.PathSpec;

import java.util.Arrays;
import java.util.Collection;


/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class MaskCreator
{
  /**
   * Create a positive mask for each of the given paths.
   *
   * @param paths the paths that should be in the mask
   * @return a {@link MaskTree}
   */
  public static MaskTree createPositiveMask(PathSpec... paths)
  {
    return createPositiveMask(Arrays.asList(paths));
  }

  /**
   * Create a positive mask for each of the given paths.
   *
   * @param paths the paths that should be in the mask
   * @return a {@link MaskTree}
   */
  public static MaskTree createPositiveMask(Collection<PathSpec> paths)
  {
    return createMaskTree(paths, MaskOperation.POSITIVE_MASK_OP);
  }

  /**
   * Create a negative mask for each of the given paths.
   *
   * @param paths the paths that should be in the mask
   * @return a {@link MaskTree}
   */
  public static MaskTree  createNegativeMask(PathSpec... paths)
  {
    return createNegativeMask(Arrays.asList(paths));
  }

  /**
   * Create a negative mask for each of the given paths.
   *
   * @param paths the paths that should be in the mask
   * @return a {@link MaskTree}
   */
  public static MaskTree createNegativeMask(Collection<PathSpec> paths)
  {
    return createMaskTree(paths, MaskOperation.NEGATIVE_MASK_OP);
  }

  private static MaskTree createMaskTree(Collection<PathSpec> paths, MaskOperation op)
  {
    MaskTree maskTree = new MaskTree();
    for (PathSpec path : paths)
    {
      maskTree.addOperation(path, op);
    }
    return maskTree;
  }
}
