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

package com.linkedin.restli.server.resources;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.restli.internal.server.RestLiInternalException;
import com.linkedin.restli.internal.server.model.AnnotationSet;

/**
 * Simple DI adapter that reads JSR-330 annotations off declared fields in a class and
 * auto-wires dependencies via injection of fields and/or constructor arguments.
 *
 * @author dellamag
 */
public class Jsr330Adapter
{
  private static final Logger log = LoggerFactory.getLogger(InjectResourceFactory.class);

  private final BeanProvider _beanProvider;

  private final Map<Class<?>, InjectableConstructor> _constructorParameterDependencies = new HashMap<>();

  private final Map<Class<?>, Object[]> _constructorParameterBindings = new HashMap<>();

  //map of field dependency declarations (unbound dependencies) for each bean class
  //bean class => InjectableFields, i.e., (Field => DependencyDecl)
  private final Map<Class<?>, InjectableFields> _fieldDependencyDeclarations = new HashMap<>();

  //map of bound field dependencies for each bean class
  //bean class => BeanDependencies, i.e., (Field => Object)
  private final Map<Class<?>, BeanDependencies> _fieldDependencyBindings = new HashMap<>();

  public Jsr330Adapter(final Collection<Class<?>> managedBeans,
                       final BeanProvider beanProvider)
  {
    _beanProvider = beanProvider;

    // pick off field annotations
    scan(managedBeans);
    validate();
  }

  public <T> T getBean(final Class<T> beanClass)
  {
    BeanDependencies deps = _fieldDependencyBindings.get(beanClass);

    if (deps == null)
    {
      throw new RestLiInternalException("Could not find bean of class '" + beanClass.getName() + "'");
    }

    try
    {
      Constructor<?> constructor = _constructorParameterDependencies.get(beanClass).getConstructor();
      Object[] arguments = _constructorParameterBindings.get(beanClass);
      @SuppressWarnings("unchecked")
      T bean = (T)constructor.newInstance(arguments);

      for (Entry<Field, Object> fieldDep : deps.iterator())
      {
        Field f = fieldDep.getKey();
        f.setAccessible(true);
        f.set(bean, fieldDep.getValue());
      }

      return bean;
    }
    catch (Throwable t)
    {
      throw new RestLiInternalException(String.format("Error initializing bean %s", beanClass.getName()), t);
    }
  }


  private void scan(final Collection<Class<?>> managedBeans)
  {
    for (Class<?> beanClazz : managedBeans)
    {
      log.debug("Scanning class " + beanClazz.getName());

      scanInjectableConstructors(beanClazz);

      scanInjectableFields(beanClazz);
    }
  }

  private void scanInjectableConstructors(Class<?> beanClazz)
  {
    int annotatedConstructors = 0;
    for (Constructor<?> constructor : beanClazz.getConstructors())
    {
      Inject injectAnnotation = constructor.getAnnotation(Inject.class);
      if (injectAnnotation != null)
      {
        ++annotatedConstructors;
        if (annotatedConstructors > 1)
        {
          throw new RestLiInternalException("Found multiple constructors annotated with @Inject in "
                                                    + "class '" + beanClazz.getCanonicalName() +
                                                    "'.  At most one constructor can be annotated "
                                                    +"with @Inject.");
        }

        Class<?>[] parameters = constructor.getParameterTypes();
        Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();

        List<DependencyDecl> parameterDecls = new ArrayList<>(parameters.length);

        for (int i = 0; i < parameters.length; ++i)
        {
          Class<?> parameter = parameters[i];
          AnnotationSet annotations = new AnnotationSet(parameterAnnotations[i]);
          Named namedAnno = annotations.get(Named.class);

          parameterDecls.add(new DependencyDecl(parameter,
                                                namedAnno != null ? namedAnno.value() : null));
        }

        constructor.setAccessible(true);
        _constructorParameterDependencies.put(beanClazz,
                                new InjectableConstructor(constructor, parameterDecls));
      }
    }
    if (annotatedConstructors == 0)
    {
      try
      {
        Constructor<?> defaultConstructor = beanClazz.getConstructor();
        defaultConstructor.setAccessible(true);
        _constructorParameterDependencies.put(beanClazz,
                                              new InjectableConstructor(defaultConstructor,
                                                                     Collections.<DependencyDecl>emptyList()));
      }
      catch (NoSuchMethodException e)
      {
        throw new RestLiInternalException(
                String.format("No injectable constructor defined for class %s.  Classes must define"
                                      +" either a default constructor or a constructor annotated "
                                      + "with @Inject.", beanClazz.getName()), e);
      }

    }
  }

  private void scanInjectableFields(Class<?> beanClazz)
  {
    InjectableFields fieldDecls = new InjectableFields();

    List<Field> fieldsToScan =
        new ArrayList<>(Arrays.asList(beanClazz.getDeclaredFields()));
    Class<?> superclazz = beanClazz.getSuperclass();
    while (superclazz != Object.class)
    {
      fieldsToScan.addAll(Arrays.asList(superclazz.getDeclaredFields()));
      superclazz = superclazz.getSuperclass();
    }

    for (Field field : fieldsToScan)
    {
      log.debug("  Scanning field " + field.getName());
      if (field.getAnnotations().length > 0)
      {
        // prefer more specific Named fields
        Named namedAnno = field.getAnnotation(Named.class);
        if (namedAnno != null)
        {
          log.debug("    Using @Named: " + namedAnno.value());
          fieldDecls.add(field, field.getType(), namedAnno.value());
        }
        else
        {
          log.debug("    Using @Inject");
          Inject injectAnno = field.getAnnotation(Inject.class);
          if (injectAnno != null)
          {
            fieldDecls.add(field, field.getType(), null);
          }
        }
      }
    }
    _fieldDependencyDeclarations.put(beanClazz, fieldDecls);
  }

  public void validate()
  {
    bindConstructorParameterDependencies();
    bindFieldDependencies();
  }

  private void bindConstructorParameterDependencies()
  {
    for (Entry<Class<?>, InjectableConstructor> beanConstructor : _constructorParameterDependencies.entrySet())
    {
      Class<?> beanClazz = beanConstructor.getKey();
      List<DependencyDecl> dependencies = beanConstructor.getValue().getParameterDecls();
      Object[] bindings = new Object[dependencies.size()];
      int idx = 0;
      for (DependencyDecl dependency : dependencies)
      {
        String dependencyTarget = "constructor '" + beanConstructor.getValue().getConstructor() + "' parameter index " + idx;
        bindings[idx] = resolveDependency(dependency, beanClazz, dependencyTarget);
        ++idx;
      }
      _constructorParameterBindings.put(beanClazz, bindings);
    }
  }

  private void bindFieldDependencies()
  {
    for (Entry<Class<?>, InjectableFields> beanFields : _fieldDependencyDeclarations.entrySet())
    {
      BeanDependencies deps = new BeanDependencies();

      for (Entry<Field,DependencyDecl> depDecl : beanFields.getValue().iterator())
      {
        DependencyDecl decl = depDecl.getValue();
        Class<?> beanClazz = beanFields.getKey();
        String dependencyTarget = "field '" + depDecl.getKey() + "'";

        Object resolvedBean = resolveDependency(decl, beanClazz, dependencyTarget);
        deps.add(depDecl.getKey(), resolvedBean);
      }
      _fieldDependencyBindings.put(beanFields.getKey(), deps);
    }
  }

  private Object resolveDependency(DependencyDecl decl, Class<?> beanClazz,
                                   String dependencyTarget)
  {
    log.debug("Resolving bean for class " + beanClazz + ", " + dependencyTarget);
    Object resolvedBean;
    if (decl.hasBeanName())
    {
      resolvedBean = _beanProvider.getBean(decl.getBeanName());
      if (resolvedBean == null)
      {
        throw new RestLiInternalException("Expected to find bean with name '" + decl.getBeanName() + "', but did not find such bean." +
            " This bean needs to be injected into class '" + beanClazz + "', " + dependencyTarget + ".");
      }
    }
    else
    {
      Map<String, ?> matchingBeans = _beanProvider.getBeansOfType(decl.getBeanType());
      if (matchingBeans.size() != 1)
      {
        throw new RestLiInternalException("Expected to find exactly 1 bean of type '" + decl.getBeanType() +
                                          "', but found " + matchingBeans.size() + ". You can use the @Named " +
                                          "annotation to further qualify ambiguous dependencies");
      }

      resolvedBean = matchingBeans.values().iterator().next();
    }
    return resolvedBean;
  }


  protected static class BeanDependencies
  {
    Map<Field, Object> _dependencyMap = new HashMap<>();

    public void add(final Field field, final Object bean)
    {
      _dependencyMap.put(field, bean);
    }

    public Iterable<Entry<Field, Object>> iterator()
    {
      return _dependencyMap.entrySet();
    }

    public Object get(final Field field)
    {
      return _dependencyMap.get(field);
    }
  }

  protected static class InjectableConstructor
  {
    private final Constructor<?> _constructor;
    private final List<DependencyDecl> _parameterDecls;

    public InjectableConstructor(Constructor<?> constructor, List<DependencyDecl> parameterDecls)
    {
      _constructor = constructor;
      _parameterDecls = parameterDecls;
    }

    public Constructor<?> getConstructor()
    {
      return _constructor;
    }

    public List<DependencyDecl> getParameterDecls()
    {
      return _parameterDecls;
    }
  }

  protected static class InjectableFields
  {
    Map<Field, DependencyDecl> _fieldMap = new HashMap<>();

    public Iterable<Entry<Field, DependencyDecl>> iterator()
    {
      return _fieldMap.entrySet();
    }

    public void add(final Field field, final Class<?> type, final String beanName)
    {
      _fieldMap.put(field, new DependencyDecl(type, beanName));
    }

    public DependencyDecl get(final Field field)
    {
      return _fieldMap.get(field);
    }
  }

  protected static class DependencyDecl
  {
    private final Class<?> _beanType;
    private final String _beanName;

    public DependencyDecl(final Class<?> beanType, final String beanName)
    {
      super();
      _beanType = beanType;
      _beanName = beanName;
    }

    public Class<?> getBeanType()
    {
      return _beanType;
    }

    public boolean hasBeanName()
    {
      return _beanName != null;
    }

    public String getBeanName()
    {
      return _beanName;
    }
  }
}
