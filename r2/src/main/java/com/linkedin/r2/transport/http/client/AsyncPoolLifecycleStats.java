package com.linkedin.r2.transport.http.client;

/**
 * @author Ang Xu
 * @version $Revision: $
 */
public class AsyncPoolLifecycleStats implements PoolStats.LifecycleStats
{
  private final double _createTimeAvg;
  private final long _createTime50Pct;
  private final long _createTime95Pct;
  private final long _createTime99Pct;


  public AsyncPoolLifecycleStats(double createTimeAvg,
                                 long createTime50Pct,
                                 long createTime95Pct,
                                 long createTime99Pct)
  {
    _createTimeAvg = createTimeAvg;
    _createTime50Pct = createTime50Pct;
    _createTime95Pct = createTime95Pct;
    _createTime99Pct = createTime99Pct;
  }

  @Override
  public double getCreateTimeAvg()
  {
    return _createTimeAvg;
  }

  @Override
  public long getCreateTime50Pct()
  {
    return _createTime50Pct;
  }

  @Override
  public long getCreateTime95Pct()
  {
    return _createTime95Pct;
  }

  @Override
  public long getCreateTime99Pct()
  {
    return _createTime99Pct;
  }

  @Override
  public String toString()
  {
    return "\ncreateTimeAvg: " + _createTimeAvg +
           "\ncreateTime50Pct: " + _createTime50Pct +
           "\ncreateTime95Pct: " + _createTime95Pct +
           "\ncreateTime99Pct: " + _createTime99Pct;
  }
}
