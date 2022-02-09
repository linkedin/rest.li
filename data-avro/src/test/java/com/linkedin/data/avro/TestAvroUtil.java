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

package com.linkedin.data.avro;

class TestAvroUtil
{
  static String namespaceProcessor(String text)
  {
    if (text.contains("##NS"))
    {
      final AvroAdapter avroAdapter = AvroAdapterFinder.getAvroAdapter();

      if (avroAdapter.jsonUnionMemberHasFullName())
        text = text.replaceAll("##NS\\(([^\\)]+)\\)", "$1");
      else
        text = text.replaceAll("##NS\\([^\\)]+\\)", "");
    }
    return text;
  }

  static String serializedEnumValueProcessor(String text)
  {
    if (text.contains("##Q_START") && text.contains("##Q_END"))
    {
      final AvroAdapter avroAdapter = AvroAdapterFinder.getAvroAdapter();

      if (avroAdapter.jsonUnionMemberHasFullName())
      {
        return text.replaceAll("##Q_START", "\"").replaceAll("##Q_END", "\"");
      }
      else
      {
        return text.replaceAll("##Q_START", "").replaceAll("##Q_END", "");
      }
    }
    return text;
  }
}
