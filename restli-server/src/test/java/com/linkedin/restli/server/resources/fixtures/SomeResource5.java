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

package com.linkedin.restli.server.resources.fixtures;

import javax.inject.Inject;
import javax.inject.Named;

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.server.TestRecord;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResource;
import com.linkedin.restli.server.resources.fixtures.SomeDependency1;
import com.linkedin.restli.server.resources.fixtures.SomeResource1;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

@RestLiCollection(name="someResource5",
                  keyName="key")
public class SomeResource5 extends SomeResource1 implements CollectionResource<String, TestRecord>
{
  @Inject @Named("dep1")
  private SomeDependency1 _dependency1;

  @Inject @Named("dep3")
  private SomeDependency1 _dependency3;

  public SomeDependency1 getDerivedDependency1()
  {
    return _dependency1;
  }

  public SomeDependency1 getDependency3()
  {
    return _dependency3;
  }
}
