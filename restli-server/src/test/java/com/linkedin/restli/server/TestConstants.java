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

package com.linkedin.restli.server;

public interface TestConstants
{
  String TESTNG_GROUP_PRIORITY_1 = "priority_1";
  String TESTNG_GROUP_PRIORITY_2 = "priority_2";
  String TESTNG_GROUP_PRIORITY_3 = "priority_3";
  String TESTNG_GROUP_REGRESSION = "regression";
  String TESTNG_GROUP_BACKEND = "backend";

  String TESTNG_GROUP_UNIT = "unit";
  String TESTNG_GROUP_INTEGRATION = "integration";
  String TESTNG_GROUP_NEGATIVE_TESTS = "negative_tests";
  String TESTNG_GROUP_REVISE_TESTS = "revise_tests";
  String TESTNG_GROUP_KNOWN_ISSUE = "known_issue";
  String TESTNG_GROUP_NOT_IMPLEMENTED = "not_implemented";

  String TESTNG_GROUP_REST_FRAMEWORK_EXAMPLE = "rest-framework-example";

  TestRecordTemplateClass.Foo FOO_1 = TestRecordTemplateClass.Foo.createFoo("foo1_key", "foo1_value");
  TestRecordTemplateClass.Foo FOO_2 = TestRecordTemplateClass.Foo.createFoo("foo2_key", "foo2_value");
  TestRecordTemplateClass.Bar MD_1 = TestRecordTemplateClass.Bar.createBar("md1_key", "md1_value");
  TestRecordTemplateClass.Bar MD_2 = TestRecordTemplateClass.Bar.createBar("md2_key", "md2_value");
}
