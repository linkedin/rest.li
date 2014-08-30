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

  /**
   * This is a sample Javadoc comment for an action. This method below takes in parameters that
   * specify what accounts to close
   *
   * @return    
   *                  
   * a map that contains details about account closures.     This return description here is intentionally  
   *  long     and poorly spaced in   between   
   *      so that I can       make sure it shows up correctly in the restspec.json          
   *
   *
   * @param emailAddresses Array of email addresses
   * @param someFlag flag for some custom behavior
   * @param options a map specifying some custom options
   *
   */
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

  /**
   * This is a another sample Javadoc comment for an action. This semi-poorly written java doc only has one character in the
   * return description and uses a mixture of upper and lower case letters in the @return tag
   *
   * @ReTuRn ^
   */
  @Action(name="spamTweets")
  public void spamTweets(@ActionParam("statuses") StringArray statuses)
  {
  }

  /**
   * This is a another sample Javadoc comment for an action. This poorly written java doc neglects to mention a return
   * parameter description
   *
   * @return
   */
  @Action(name="primitiveResponse")
  public int primitiveResponse()
  {
    return 1;
  }
}
