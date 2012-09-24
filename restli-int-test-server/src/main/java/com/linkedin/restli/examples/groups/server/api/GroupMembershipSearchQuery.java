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

package com.linkedin.restli.examples.groups.server.api;


import com.linkedin.restli.examples.groups.api.MembershipLevel;
import com.linkedin.restli.examples.groups.api.MembershipSortOrder;


public class GroupMembershipSearchQuery
{
  public  static final int WILDCARD = -1;

  private final int           _groupID;
  private final int           _start;
  private final int           _count;

  private MembershipLevel     _membershipLevel;
  private String              _firstName;
  private String              _lastName;
  private String              _emailAddress;
  private MembershipSortOrder _sortOrder;

  public GroupMembershipSearchQuery(int groupID, int start, int count)
  {
    _groupID = groupID;
    _start = start;
    _count = count;
    _sortOrder = MembershipSortOrder.LAST_NAME_ASC;
  }

  public int getGroupID()
  {
    return _groupID;
  }

  public int getStart()
  {
    return _start;
  }

  public int getCount()
  {
    return _count;
  }

  public MembershipLevel getMembershipLevel()
  {
    return _membershipLevel;
  }

  public void setMembershipLevel(MembershipLevel membershipLevel)
  {
    _membershipLevel = membershipLevel;
  }

  public String getFirstName()
  {
    return _firstName;
  }

  public void setFirstName(String firstName)
  {
    _firstName = firstName;
  }

  public String getLastName()
  {
    return _lastName;
  }

  public void setLastName(String lastName)
  {
    _lastName = lastName;
  }

  public String getEmailAddress()
  {
    return _emailAddress;
  }

  public void setEmailAddress(String emailAddress)
  {
    _emailAddress = emailAddress;
  }

  public MembershipSortOrder getSortOrder()
  {
    return _sortOrder;
  }

  public void setSortOrder(MembershipSortOrder sortOrder)
  {
    _sortOrder = sortOrder;
  }
}
