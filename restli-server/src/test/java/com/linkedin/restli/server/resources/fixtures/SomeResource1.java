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

package com.linkedin.restli.server.resources.fixtures;

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.server.TestRecord;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.server.resources.fixtures.SomeDependency1;
import com.linkedin.restli.server.resources.fixtures.SomeDependency2;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author dellamag
 */
@RestLiCollection(name="someResource1",
                  keyName="key")
public class SomeResource1 extends CollectionResourceTemplate<String,TestRecord>
{
  @Inject @Named("dep1")
  private SomeDependency1 _dependency1;
  @Inject
  private SomeDependency2 _dependency2;
  private SomeDependency2 _nonInjectedDependency;

  public SomeDependency1 getDependency1()
  {
    return _dependency1;
  }

  public SomeDependency2 getDependency2()
  {
    return _dependency2;
  }

  public SomeDependency2 getNonInjectedDependency()
  {
    return _nonInjectedDependency;
  }
}
