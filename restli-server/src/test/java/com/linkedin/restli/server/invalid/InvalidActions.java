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

package com.linkedin.restli.server.invalid;

import java.util.Date;

import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.RestLiActions;


/**
 * @author dellamag
 */
public class InvalidActions
{
  @RestLiActions(name="foo")
  public static class ActionUnannotatedParameters
  {
    @Action(name="action")
    public String action(@ActionParam("p1") String p1, String p2) {return null;}
  }

  // this is covered in more detail by InvalidResources as well (actions shares this code)
  @RestLiActions(name="foo")
  public static class ActionInvalidParameterTypes
  {
    @Action(name="action")
    public String action(@ActionParam("p1") Object p1) {return null;}
  }

  @RestLiActions(name="foo")
  public static class ActionNameConflict
  {
    @Action(name="action")
    public void action1() {}

    @Action(name="action")
    public void action2() {}
  }

  @RestLiActions(name="foo")
  public static class ActionInvalidReturnType
  {
    @Action(name="action")
    public Date action() {return null;}
  }

  @RestLiActions(name="foo")
  public class ActionInvalidReturnType2
  {
    @Action(name="action")
    public Object action() {return null;}
  }
  
  @RestLiActions(name = "foo")
  public class ActionInvalidBytesParam
  {
    @Action(name="action")
    public void action(@ActionParam("p1") byte[] foo)
    {

    }
  }
}
