package com.linkedin.d2.balancer.dualread;

public interface DualReadModeProvider
{
  enum DualReadMode
  {
    OLD_LB_ONLY,
    DUAL_READ,
    NEW_LB_ONLY
  }

  DualReadMode getDualReadMode();

  default DualReadMode getDualReadMode(String d2ServiceName) {
    return getDualReadMode();
  };
}
