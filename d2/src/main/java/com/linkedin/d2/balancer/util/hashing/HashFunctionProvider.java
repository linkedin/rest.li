/*
   Copyright (c) 2018 LinkedIn Corp.

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


package com.linkedin.d2.balancer.util.hashing;

import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.r2.message.Request;
import java.net.URI;


/**
 * Provide the HashFunction with the given URI.
 *
 * Depending on hashMethod and hashConfig definitions, different services use different hashFunctions. This class
 * presents the hashFunction corresponding to the given uri.
 *
 * @throws ServiceUnavailableException if the service does not exist
 */


public interface HashFunctionProvider
{
  HashFunction<Request> getHashFunction(URI uri) throws ServiceUnavailableException;
}
