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

package com.linkedin.restli.tools.twitter;

import com.linkedin.data.template.StringArray;
import com.linkedin.data.template.StringMap;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.Optional;
import com.linkedin.restli.server.annotations.RestLiActions;

/**
 * @author dellamag
 */
@RestLiActions(name="accounts")
public class TwitterAccountsResource
{
  @Action(name="register")
  public void register(@ActionParam("first") String first,
                                      @ActionParam("last") String last,
                                      @ActionParam("email") String email,
                                      @ActionParam("company") @Optional String company,
                                      @ActionParam("openToMarketingEmails") @Optional("true") boolean openToMarketingEmails)
  {
  }

  @Action(name="closeAccounts")
  public StringMap closeAccounts(@ActionParam("emailAddresses") StringArray emailAddresses,
                                                 @ActionParam("someFlag") boolean someFlag,
                                                 @ActionParam("options") @Optional StringMap options)
  {
    StringMap res = new StringMap();
    res.put("numClosed", "5");
    res.put("resultCode", "11");

    return new StringMap(res);
  }

  @Action(name="noArgMethod")
  public void noArgMethod()
  {
  }

  @Action(name="spamTweets")
  public void spamTweets(@ActionParam("statuses") StringArray statuses)
  {
  }

  @Action(name="primitiveResponse")
  public int primitiveResponse()
  {
    return 1;
  }
}
