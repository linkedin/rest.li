/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.server.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Used to denote an injected type of {@link com.linkedin.restli.common.attachments.RestLiAttachmentReader}. If no
 * attachments are present then the corresponding {@link com.linkedin.restli.common.attachments.RestLiAttachmentReader}
 * will be null.
 *
 * NOTE: It is the responsibility of the application developer to drain all the attachments represented by
 * the {@link com.linkedin.restli.common.attachments.RestLiAttachmentReader}. Failure to absorb all attachments
 * may lead to a leak in resources on the server, notably file descriptors due to open TCP connections. This
 * may potentially cause server instability. Also further note that a response to the client may be sent by the
 * application developer before attachments are consumed. The rest.li framework therefore cannot be held responsible for
 * absorbing any request level attachments that are left untouched by the application developer.
 *
 * In cases where resource methods throw exceptions, application developers should still also absorb/drain all incoming
 * attachments. However in cases of exceptions, the rest.li framework will make an attempt to drain attachments
 * that have not yet been consumed from the incoming request. However this behavior of the rest.li framework
 * should not be relied upon.
 *
 * Lastly it should be noted that the rest.li framework is guaranteed to drop all attachments to the ground if
 * an exception occurs prior to resource method invocation. For example if an exception occurs in the rest.li framework
 * due to a bad request or an exception occurs in the request filters, then the rest.li framework is guaranteed
 * to absorb and drop all attachments to the ground.
 *
 * @author Karim Vidhani
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RestLiAttachmentsParam
{
}