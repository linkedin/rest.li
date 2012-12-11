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
package com.linkedin.pegasus.gradle;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David Hoa
 * @version $Revision: $
 */

public class PegasusOptions
{
  public Mode mode;
  public IdlOptions idlOptions = new IdlOptions();
  public ClientOptions clientOptions = new ClientOptions();

  public static enum Mode
  {
    AVRO,
    PEGASUS
  }

  public static class IdlItem
  {
    // Input options for pegasus IDL Generation
    String apiName;
    String[] packageNames;

    public IdlItem(String inApiName, List<String> inPackageNames)
    {
      apiName = inApiName;
      packageNames = inPackageNames.toArray(new String[0]);
    }
  }

  public static class IdlOptions
  {
    private List<IdlItem> _idlOptionsList = new ArrayList<IdlItem>();

    public void addIdlItem(String inApiName, List<String> inPackageNames)
    {
      IdlItem newItem = new IdlItem(inApiName, inPackageNames);
      _idlOptionsList.add(newItem);
    }

    public void addIdlItem(List<String> inPackageNames)
    {
      IdlItem newItem = new IdlItem("", inPackageNames);
      _idlOptionsList.add(newItem);
    }

    public List<IdlItem> getIdlItems()
    {
      return _idlOptionsList;
    }
  }

  public static class ClientItem
  {
    // Input options for pegasus Client Stub (Builder) generation
    String defaultPackage;
    String restModelFileName;
    // will be used in the future
    boolean keepDataTemplates = false;

    public ClientItem(String inRestModelFileName, String inDefaultPackage, boolean inKeepDataTemplates)
    {
      restModelFileName = inRestModelFileName;
      defaultPackage = inDefaultPackage;
      keepDataTemplates = inKeepDataTemplates;
    }
  }

  public static class ClientOptions
  {
    private List<ClientItem> clientOptionsList = new ArrayList<ClientItem>();

    public void addClientItem(String inRestModelFileName, String inDefaultPackage, boolean inKeepDataTemplates)
    {
      ClientItem newItem = new ClientItem(inRestModelFileName, inDefaultPackage, inKeepDataTemplates);
      clientOptionsList.add(newItem);
    }

    public void addClientItem(String inRestModelFileName)
    {
      ClientItem newItem = new ClientItem(inRestModelFileName, "", false);
      clientOptionsList.add(newItem);
    }

    public boolean hasRestModelFileName(String fileName)
    {
      for (ClientItem item : clientOptionsList)
      {
        if (item.restModelFileName.equals(fileName))
          return true;
      }
      return false;
    }

    public List<ClientItem> getClientItems()
    {
      return clientOptionsList;
    }
  }
}
