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

package com.linkedin.restli.tools.idlgen;


import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.DocumentationTool;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class RestLiDoclet implements Doclet
{
  private static RestLiDoclet _currentDocLet = null;

  private final DocInfo _docInfo;
  private final DocletEnvironment _docEnv;
  private final Elements _elements;


  public static synchronized RestLiDoclet generateDoclet(String programName,
                                                         PrintWriter errWriter,
                                                         PrintWriter warnWriter,
                                                         PrintWriter noticeWriter,
                                                         String flatClassPath,
                                                         List<String> analyzedSourceFiles
                                                         )
  {
    noticeWriter.println("Generating Javadoc for " + programName);

    DocumentationTool docTool = ToolProvider.getSystemDocumentationTool();
    StandardJavaFileManager fileManager = docTool.getStandardFileManager(null, null, null);
    Iterable<? extends JavaFileObject> fileObjects = fileManager.getJavaFileObjectsFromPaths(
            analyzedSourceFiles.stream().map(Paths::get).collect(Collectors.toList()));




    // Set up the Javadoc task options
    List<String> taskOptions = new ArrayList<>();
    taskOptions.add("-classpath");
    taskOptions.add(flatClassPath);

    // Create and run the Javadoc task
    DocumentationTool.DocumentationTask task = docTool.getTask(errWriter,
                                                               fileManager,
                                                               new DiagnosticListener<JavaFileObject>() {
                                                                 @Override
                                                                 public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
                                                                     switch (diagnostic.getKind()) {
                                                                        case ERROR:
                                                                          errWriter.println(diagnostic.getMessage(Locale.getDefault()));
                                                                          break;
                                                                        case WARNING:
                                                                          warnWriter.println(diagnostic.getMessage(Locale.getDefault()));
                                                                          break;
                                                                        case NOTE:
                                                                          noticeWriter.println(diagnostic.getMessage(Locale.getDefault()));
                                                                          break;
                                                                     }
                                                                 }
                                                               },
                                                               RestLiDoclet.class,
                                                               taskOptions,
                                                               fileObjects);

    boolean success = task.call();
    if (!success) {
      throw new IllegalArgumentException("Javadoc generation failed");
    }

    return _currentDocLet;
  }

  private RestLiDoclet(DocInfo docInfo, DocletEnvironment docEnv)
  {
    _docInfo = docInfo;
    _docEnv = docEnv;
    _elements = docEnv.getElementUtils();
  }

  public TypeElement getClassDoc(Class<?> resourceClass)
  {
    return _docInfo.getClassDoc(resourceClass.getCanonicalName());
  }

  public ExecutableElement getMethodDoc(Method method)
  {
    final MethodIdentity methodId = MethodIdentity.create(method);
    return _docInfo.getMethodDoc(methodId);
  }

  public List<String> getDeprecatedTags(Element element) {
    List<String> deprecatedTags = new ArrayList<>();
    Deprecated deprecatedAnnotation = element.getAnnotation(Deprecated.class);
    if (deprecatedAnnotation != null) {
      String deprecatedComment = _elements.getDocComment(element);
      deprecatedTags.add(deprecatedComment);
    }
    return deprecatedTags;
  }

  public Map<String, String> getParamTags(ExecutableElement method) {
    Map<String, String> paramTags = new HashMap<>();
    for (VariableElement parameter : method.getParameters()) {
      String paramName = parameter.getSimpleName().toString();
      String paramComment = _elements.getDocComment(parameter);
      if (paramComment != null) {
        paramTags.put(paramName, paramComment);
      }
    }
    return paramTags;
  }

  public com.sun.source.doctree.DocCommentTree getDocCommentTree(Method method) {
    TypeElement typeElement = getClassDoc(method.getDeclaringClass());
    if (typeElement == null) {
      return null;
    }

    for (Element element : typeElement.getEnclosedElements()) {
      if (element.getSimpleName().toString().equals(method.getName())) {
        return _docEnv.getDocTrees().getDocCommentTree(element);
      }
    }

    return null;
  }

  @Override
  public void init(Locale locale,
                   Reporter reporter) {
    // no-ops
  }

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public Set<? extends Option> getSupportedOptions() {
    return Set.of();
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }

  @Override
  public boolean run(DocletEnvironment docEnv) {
    final DocInfo docInfo = new DocInfo();

    // Iterate through the TypeElements (class and interface declarations)
    for (Element element : docEnv.getIncludedElements()) {
      if (element instanceof TypeElement) {
        TypeElement typeElement = (TypeElement) element;
        docInfo.setClassDoc(typeElement.getQualifiedName().toString(), typeElement);

        // Iterate through the methods of the TypeElement
        for (Element enclosedElement : typeElement.getEnclosedElements()) {
          if (enclosedElement instanceof ExecutableElement) {
            ExecutableElement methodElement = (ExecutableElement) enclosedElement;
            docInfo.setMethodDoc(MethodIdentity.create(methodElement), methodElement);
          }
        }
      }
    }

    _currentDocLet = new RestLiDoclet(docInfo, docEnv);

    return true;
  }

  public String getDocComment(Element element) {
    return _elements.getDocComment(element);
  }

  private static class DocInfo {
    public TypeElement getClassDoc(String className) {
      return classNameToClassDoc.get(className);
    }

    public ExecutableElement getMethodDoc(MethodIdentity methodId) {
      return methodIdToMethodDoc.get(methodId);
    }

    public void setClassDoc(String className, TypeElement classDoc) {
      classNameToClassDoc.put(className, classDoc);
    }

    public void setMethodDoc(MethodIdentity methodId, ExecutableElement methodDoc) {
      methodIdToMethodDoc.put(methodId, methodDoc);
    }

    private final Map<String, TypeElement> classNameToClassDoc = new HashMap<>();
    private final Map<MethodIdentity, ExecutableElement> methodIdToMethodDoc = new HashMap<>();
  }

  private static class MethodIdentity {
    public static MethodIdentity create(Method method) {
      final List<String> parameterTypeNames = new ArrayList<>();

      // type parameters are not included in identity because of differences between reflection and Doclet:
      // e.g. for Collection<Void>:
      //   reflection Type.toString() -> Collection<Void>
      //   Doclet Type.toString() -> Collection
      for (Class<?> paramClass : method.getParameterTypes()) {
        parameterTypeNames.add(paramClass.getCanonicalName());
      }

      return new MethodIdentity(method.getDeclaringClass().getName() + "." + method.getName(), parameterTypeNames);
    }

    public static MethodIdentity create(ExecutableElement method) {
      final List<String> parameterTypeNames = new ArrayList<>();
      for (VariableElement param : method.getParameters()) {
        TypeMirror type = param.asType();
        parameterTypeNames.add(type.toString());
      }

      return new MethodIdentity(method.getEnclosingElement().toString() + "." + method.getSimpleName().toString(),
                                parameterTypeNames);
    }

    private MethodIdentity(String methodQualifiedName, List<String> parameterTypeNames) {
      this.methodQualifiedName = methodQualifiedName;
      this.parameterTypeNames = parameterTypeNames;
    }

    @Override
    public int hashCode() {
      return new HashCodeBuilder(17, 29)
              .append(methodQualifiedName)
              .append(parameterTypeNames)
              .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (obj == null) {
        return false;
      }

      if (getClass() != obj.getClass()) {
        return false;
      }

      final MethodIdentity other = (MethodIdentity) obj;
      return new EqualsBuilder()
              .append(methodQualifiedName, other.methodQualifiedName)
              .append(parameterTypeNames, other.parameterTypeNames)
              .isEquals();
    }

    private final String methodQualifiedName;
    private final List<String> parameterTypeNames;
  }
}
