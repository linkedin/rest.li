package com.linkedin.restli.server.resources.fixtures;

public interface ConstructorArgResource {
  SomeDependency1 getDependency1();

  SomeDependency2 getDependency2();

  SomeDependency2 getNonInjectedDependency();
}
