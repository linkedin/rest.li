package com.linkedin.darkcluster;

import java.util.function.Supplier;

import com.linkedin.common.util.Notifier;

public class DoNothingNotifier implements Notifier
{
  @Override
  public void notify(RuntimeException ex)
  {

  }

  @Override
  public void notify(Supplier<RuntimeException> supplier)
  {

  }
}
