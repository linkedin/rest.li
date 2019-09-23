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
    return new Group()
        .setApprovalModes(1)
        .setBadge(Badge.FEATURED)
        .setCategoriesEnabled(PostCategory.DISCUSSION)
        .setCategoriesForModeratorsOnly(PostCategory.DISCUSSION)
        .setCategory(1)
        .setContactability(Contactability.CONTACTABLE)
        .setContactEmail("bob@example.com")
        .setCreatedTimestamp(System.currentTimeMillis())
        .setDescription("long description long description long description long description long description long description long description long description long description long description long description long description long description long description long description ")
        .setDirectoryPresence(DirectoryPresence.PUBLIC)
        .setHasEmailExport(true)
        .setHasMemberInvites(false)
        .setHasMemberRoster(true)
        .setHasNetworkUpdates(true)
        .setHasSettings(true)
        .setHideSubgroups(false)
        .setHomeSiteUrl("http://www.example.com")
        .setId(groupID)
        .setIsOpenToNonMembers(true)
        .setLargeLogoMediaUrl("/0/0/1/skafhdsjahiuewh")
        .setLastModifiedTimestamp(System.currentTimeMillis())
        .setLocale("en_US")
        .setLocation(new Location()
            .setCountryCode("us")
            .setGeoPlaceCodes(new StringArray("1-2-3-4-5"))
            .setGeoPostalCode("94043")
            .setGmtOffset(-8f)
            .setLatitude(122.1f)
            .setLongitude(37.4f)
            .setPostalCode("94043")
            .setRegionCode(37)
            .setUsesDaylightSavings(true))
        .setMaxFeeds(100)
        .setMaxIdentityChanges(5)
        .setMaxMembers(2000)
        .setMaxModerators(10)
        .setMaxSubgroups(20)
        .setName(name)
        .setNewsFormat(NewsFormat.RECENT)
        .setNonMemberPermissions(NonMemberPermissions.COMMENT_AND_POST_WITH_MODERATION)
        .setNumIdentityChanges(5)
        .setNumMemberFlagsToDelete(3)
        .setOpenedToNonMembersTimestamp(System.currentTimeMillis())
        .setOtherCategory(3)
        .setPreApprovedEmailDomains(new StringArray("example.com", "linkedin.com"))
        .setPreModerateMembersWithLowConnections(true)
        .setPreModerateNewMembersPeriodInDays(3)
        .setPreModeration(PreModerationType.COMMENTS)
        .setPreModerationCategories(PostCategory.JOB)
        .setRules("No spam, please")
        .setSharingKey("HJFD3JH98JKH3")
        .setShortDescription("short description")
        .setSmallLogoMediaUrl("/0/0/1/skafhdsjahiuewh")
        .setState(State.ACTIVE)
        .setVanityUrl(name.toLowerCase().replace(' ', '-'))
        .setVisibility(Visibility.PUBLIC);
  }
}
