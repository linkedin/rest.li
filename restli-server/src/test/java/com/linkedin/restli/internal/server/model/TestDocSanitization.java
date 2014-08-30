/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.internal.server.model;


import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Karim Vidhani
 */
public class TestDocSanitization
{
  //We have to explicitly test this way using tabs because intellij converts
  //the tabs in our Javadoc tests to white spaces due to the LinkedIn coding style plugin
  @Test
  public void testSanitizeDoc()
  {
    final String docToSanitize =
        "\t\n\n\t  \t   \n  " + "This is a sample    doc to \t   sanitize. We \n\t\n \t \nshould be able " +
            "    to  \t \t\t\t\t\t sanitize this doc and remove all     \t\t \t   \t this unnecessary white space \n" +
            " \t\t   while preserving the new line characters in between the words" +
            "\n\t\t\n   \t\n. I hope it works\n  \t";
    final String sanitizedDoc = "This is a sample doc to sanitize. We\n\n\nshould be able " +
        "to sanitize this doc and remove all this unnecessary white space\nwhile preserving the new line " +
        "characters in between the words\n\n\n. I hope it works";
    Assert.assertEquals(ResourceModelEncoder.sanitizeDoc(docToSanitize), sanitizedDoc,
        "Sanitized doc should match what we expect!");
  }
}
