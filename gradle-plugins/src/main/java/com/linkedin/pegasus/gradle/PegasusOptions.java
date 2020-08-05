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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.gradle.api.file.ConfigurableFileCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author David Hoa
 * @version $Revision: $
 */

public class PegasusOptions
{
  private PegasusOptions(ConfigurableFileCollection dataSchemaDirs) {
    this.dataSchemaDirs = dataSchemaDirs;
  }

  public static PegasusOptions create(ConfigurableFileCollection dataSchemaDirs) {
    return new PegasusOptions(dataSchemaDirs);
  }

  public Set<GenerationMode> generationModes = new HashSet<GenerationMode>(Arrays.asList(GenerationMode.PEGASUS));
  public IdlOptions idlOptions = new IdlOptions();
  public ClientOptions clientOptions = new ClientOptions();
  public RestModelOptions restModelOptions = new RestModelOptions();
  private final ConfigurableFileCollection dataSchemaDirs;

  private static final Logger _log = LoggerFactory.getLogger(PegasusOptions.class);

  /**
   * control whether or not some generation tasks will be executed
   * AVRO: generate equivalent avro schema from pdsc schema
   * PEGASUS: generate data template from pdsc schema
   */
  public static enum GenerationMode
  {
    AVRO,
    PEGASUS
  }

  /**
   * Test if a specific generation mode is turned on
   *
   * @param mode the {@link GenerationMode} to test against
   * @return If generationModes is null, return true if mode is PEGASUS.
   *         Otherwise, return true if generationModes contains the queried mode.
   */
  public boolean hasGenerationMode(GenerationMode mode)
  {
    if (generationModes == null)
    {
      throw new NullPointerException("PegasusOptions.generationModes is null. Please check your build.gradle.");
    }

    return generationModes.contains(mode);
  }

  public static class IdlItem
  {
    // Input options for pegasus IDL Generation
    public String apiName;
    public String[] packageNames;

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
      _log.warn("addIdlItem(inApiName, inPackageNames) has been deprecated, please use addIdlItem(inPackageNames) instead.");
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
    public String defaultPackage;
    public String restModelFileName;
    // will be used in the future
    public boolean keepDataTemplates = false;

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
        {
          return true;
        }
      }
      return false;
    }

    public List<ClientItem> getClientItems()
    {
      return clientOptionsList;
    }
  }

  public static class RestModelOptions
  {
    private String _restResourcesRootPath;

    public void setRestResourcesRootPath(String restResourcesRootPath)
    {
      _restResourcesRootPath = restResourcesRootPath;
    }

    public String getRestResourcesRootPath()
    {
      return _restResourcesRootPath != null ? _restResourcesRootPath : "src/main/java";
    }
  }

  /**
   * Locations to search for data schemas.
   */
  public ConfigurableFileCollection getDataSchemaDirs() {
    return dataSchemaDirs;
  }
}
