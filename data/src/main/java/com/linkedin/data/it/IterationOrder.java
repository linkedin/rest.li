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

package com.linkedin.data.it;


/*
 * The iteration order used by the ObjectIterator to determine how to traverse the DataComplex object graph. Note that
 * this only guarantees the order in which root is visited with respect to its children. It makes no guarantee that
 * the same exact preorder or postorder traversal is guaranteed everytime since we could have a DataMap as a
 * node whose key/value pairs are not iterated through in a deterministic order.
 */
public enum IterationOrder
{
  /*
   * The root node is visited before its children. If the root node is an instance of DataComplex, then the children
   * are called in the iteration order of the underlying collection.
   */
  PRE_ORDER,
  /*
   * The root node is visited after its children. If the root node is an instance of DataComplex, then the children
   * are called in the iteration order of the underlying collection.
   */
  POST_ORDER
}

