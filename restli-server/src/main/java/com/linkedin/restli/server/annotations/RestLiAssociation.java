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

package com.linkedin.restli.server.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates an association resource. This is a collection resource representing an association
 * between two or more entities (e.g. groups and members). The array of {@literal @}AssociationCollection
 * describes the different 'sides' of the association e.g. GroupMemberships for a group vs.
 * GroupMemberships for a member. URI syntax is PAL-like:
 *
 * <ul>
 * <li>/group-member-association/groupId={groupId} => ResourceCollection of GroupMembership for a given group
 * <li>/group-member-association/memberId={memberId} => ResourceCollection of GroupMemberships for a given member
 * <li>/group-member-association/groupId={groupId}&memberID={memberId} => Resource representing a
 *          single GroupMembership for a given group and member
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RestLiAssociation
{
  /** The parent resource class.  Optional - if not specified, this resource will be a
   * root resource */
  Class<?> parent() default RestAnnotations.ROOT.class;

  /** Path is only set for root resources. The path of subresources is implied by the resource hierarchy */
  String name();

  /** The namespace of the resource, used to qualify the IDL name*/
  String namespace() default "";

  /** An ordered list of associative keys used in this association (required) */
  Key[] assocKeys();
}
