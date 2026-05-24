/*
   Copyright (c) 2024 LinkedIn Corp.

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

package com.linkedin.d2.balancer;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Opt-in allowlist annotation for direct use of {@link D2ClientBuilder}.
 *
 * <p>Annotating a class or method with this annotation suppresses the Error Prone
 * {@code RestrictedApiChecker} warning that is raised when constructing or calling
 * {@link D2ClientBuilder#build()} directly.</p>
 *
 * <p><b>This annotation should only be used as a temporary migration aid.</b>
 * All usages should eventually be migrated to {@code D2ClientFactory} (container).
 * See <a href="go/onboardindis">go/onboardindis</a> for migration instructions.</p>
 *
 * <p>To track remaining usages, search for {@code cu:AllowRawD2ClientBuilder} in Jarvis.</p>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface AllowRawD2ClientBuilder
{
  /**
   * Required justification for why this class/method needs direct D2ClientBuilder access.
   * Helps reviewers assess whether the exception is legitimate.
   */
  String reason();
}
