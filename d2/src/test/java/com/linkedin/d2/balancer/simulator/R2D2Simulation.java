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

package com.linkedin.d2.balancer.simulator;

/**
 * R2D2 runs brings echo clients and echo servers up and down randomly, and makes calls to
 * them.
 */
public class R2D2Simulation
{
  public static void main(String[] args) throws Exception
  {
    new R2D2Server().run();
    new R2D2Client("localhost:2181").run();
  }
}
