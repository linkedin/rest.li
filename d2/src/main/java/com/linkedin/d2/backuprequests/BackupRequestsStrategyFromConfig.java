/*
   Copyright (c) 2017 LinkedIn Corp.

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
package com.linkedin.d2.backuprequests;

import java.util.Map;
import java.util.Optional;


/**
 * This class contains optional instance of {@link TrackingBackupRequestsStrategy} and configuration
 * that was used to create it. It is used to create new instance of {@code TrackingBackupRequestsStrategy}
 * only if configuration has changed. We want to avoid re-creating backup strategies as much as possible
 * because strategies require warm up.
 *
 * @author Jaroslaw Odzga (jodzga@linkedin.com)
 *
 */
public class BackupRequestsStrategyFromConfig
{

  private final Optional<TrackingBackupRequestsStrategy> _strategy;
  private final Map<String, Object> _config;

  public BackupRequestsStrategyFromConfig(Map<String, Object> config)
  {
    _strategy = config == null ? Optional.empty() : Optional.ofNullable(BackupRequestsStrategyFactory.create(config));
    _config = config;
  }

  public Optional<TrackingBackupRequestsStrategy> getStrategy()
  {
    return _strategy;
  }

  /**
   * If passed in config is different than current then this method will create new instance
   * of BackupRequestsStrategyWithConfig. If passed in config is identical to the current one
   * this object is returned. Null parameter is acceptable.
   * This method is thread safe.
   * @param config new config, may be null
   * @return new instance of BackupRequestsStrategyFromConfig if new config is different than
   * current one, returns {@code this} otherwise
   */
  public BackupRequestsStrategyFromConfig update(Map<String, Object> config)
  {
    if (config == null)
    {
      if (_config != null)
      {
        return new BackupRequestsStrategyFromConfig(config);
      } else
      {
        return this;
      }
    } else if (!config.equals(_config))
    {
      return new BackupRequestsStrategyFromConfig(config);
    } else
    {
      return this;
    }
  }

  @Override
  public String toString()
  {
    return "BackupRequestsStrategyFromConfig [strategy=" + _strategy + ", config=" + _config + "]";
  }
}
