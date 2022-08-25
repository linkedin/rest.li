/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.d2.discovery.stores.zk.acl;

import java.util.List;
import com.linkedin.pegasus.org.apache.zookeeper.data.ACL;

/**
 * Provide Zookeeper {@link ACL} information for authentication and authorization
 *
 * Two types of information will be provided:
 *
 * 1. AuthScheme and AuthInfo: only 'digest' scheme needs this. Zkclient needs to use this info to authenticate
 *    itself to zookeeper server
 *
 * 2. ACL list: a list of ACLs for the zonode. Zookeeper supports combination of different schemes. For example,
 *    ["world:anyone:read", "digest:admin:adminpass:admin", "hostname:linkedin.com:write"]
 */
public interface ZKAclProvider {
  /**
   * provide {@link ACL} list for the znode
   *
   * @return list of ACLs
   */
  List<ACL> getACL();

  /**
   * provide zookeeper authentication scheme. ZK client uses this scheme for authentication
   */
  String getAuthScheme();

  /**
   * provide zookeeper authentication Data in byte array. Use together with scheme to authenticate the client
   */
  byte[] getAuthInfo();
}
