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

package com.linkedin.restli.examples.groups.server.impl;

import java.util.Random;

import com.linkedin.data.template.StringArray;
import com.linkedin.restli.examples.groups.api.Badge;
import com.linkedin.restli.examples.groups.api.Contactability;
import com.linkedin.restli.examples.groups.api.DirectoryPresence;
import com.linkedin.restli.examples.groups.api.Group;
import com.linkedin.restli.examples.groups.api.Location;
import com.linkedin.restli.examples.groups.api.NewsFormat;
import com.linkedin.restli.examples.groups.api.NonMemberPermissions;
import com.linkedin.restli.examples.groups.api.PostCategory;
import com.linkedin.restli.examples.groups.api.PreModerationType;
import com.linkedin.restli.examples.groups.api.State;
import com.linkedin.restli.examples.groups.api.Visibility;

/**
 * @author dellamag
 */
public class GroupGenerator
{
  private static final String[] NAMES = new String[] {"Genentech Mad Scientists Club",
                                                      "Extreme COBOL Web 2.0 Developers Conference",
                                                      "LinkedIn Anti-Networkers"};

  public static Group create(int groupID)
  {
    String name = NAMES[new Random().nextInt(3)];
    return create(groupID, name);
  }

  public static Group create(int groupID, String name)
  {
    Group group = new Group();
    group.setApprovalModes(1);
    group.setBadge(Badge.FEATURED);
    group.setCategoriesEnabled(PostCategory.DISCUSSION);
    group.setCategoriesForModeratorsOnly(PostCategory.DISCUSSION);
    group.setCategory(1);
    group.setContactability(Contactability.CONTACTABLE);
    group.setContactEmail("bob@example.com");
    group.setCreatedTimestamp(System.currentTimeMillis());
    group.setDescription("long description long description long description long description long description long description long description long description long description long description long description long description long description long description long description ");
    group.setDirectoryPresence(DirectoryPresence.PUBLIC);
    group.setHasEmailExport(true);
    group.setHasMemberInvites(false);
    group.setHasMemberRoster(true);
    group.setHasNetworkUpdates(true);
    group.setHasSettings(true);
    group.setHideSubgroups(false);
    group.setHomeSiteUrl("http://www.example.com");
    group.setId(groupID);
    group.setIsOpenToNonMembers(true);
    group.setLargeLogoMediaUrl("/0/0/1/skafhdsjahiuewh");
    group.setLastModifiedTimestamp(System.currentTimeMillis());
    group.setLocale("en_US");

    Location location = new Location();
    location.setCountryCode("us");
    StringArray geoPlaceCodes = new StringArray();
    geoPlaceCodes.add("1-2-3-4-5");
    location.setGeoPlaceCodes(geoPlaceCodes);
    location.setGeoPostalCode("94043");
    location.setGmtOffset(-8f);
    location.setLatitude(122.1f);
    location.setLongitude(37.4f);
    location.setPostalCode("94043");
    location.setRegionCode(37);
    location.setUsesDaylightSavings(true);
    group.setLocation(location);

    group.setMaxFeeds(100);
    group.setMaxIdentityChanges(5);
    group.setMaxMembers(2000);
    group.setMaxModerators(10);
    group.setMaxSubgroups(20);
    group.setName(name);
    group.setNewsFormat(NewsFormat.RECENT);
    group.setNonMemberPermissions(NonMemberPermissions.COMMENT_AND_POST_WITH_MODERATION);
    group.setNumIdentityChanges(5);
    group.setNumMemberFlagsToDelete(3);
    group.setOpenedToNonMembersTimestamp(System.currentTimeMillis());
    group.setOtherCategory(3);
    // group.setParentGroupId();

    StringArray preApprovedEmailDomains = new StringArray();
    preApprovedEmailDomains.add("example.com");
    preApprovedEmailDomains.add("linkedin.com");

    group.setPreApprovedEmailDomains(preApprovedEmailDomains);
    group.setPreModerateMembersWithLowConnections(true);
    group.setPreModerateNewMembersPeriodInDays(3);
    group.setPreModeration(PreModerationType.COMMENTS);
    group.setPreModerationCategories(PostCategory.JOB);
    group.setRules("No spam, please");
    group.setSharingKey("HJFD3JH98JKH3");
    group.setShortDescription("short description");
    group.setSmallLogoMediaUrl("/0/0/1/skafhdsjahiuewh");
    group.setState(State.ACTIVE);
    group.setVanityUrl(name.toLowerCase().replace(' ', '-'));
    group.setVisibility(Visibility.PUBLIC);

    return group;
  }
}
