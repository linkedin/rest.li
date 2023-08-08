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


import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.DeprecatedTree;
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


/**
 * Custom Javadoc processor that merges documentation into the restspec.json. The embedded Javadoc
 * generator is basically a commandline tool wrapper and it runs in complete isolation from the rest
 * of the application. Due to the fact that the Javadoc tool instantiates RestLiDoclet, we cannot
 * cleanly integrate the output into the {@link RestLiResourceModelExporter} tool. Thus, we're just
 * dumping the docs into a static Map which can be accessed by {@link RestLiResourceModelExporter}.
 *
 * This class supports multiple runs of Javadoc Doclet API {@link DocumentationTool}.
 * Each run will be assigned an unique "Doclet ID", returned by
 * {@link #generateDoclet(String, java.io.PrintWriter, java.io.PrintWriter, java.io.PrintWriter, String, List<String>)}.
 * The Doclet ID should be subsequently used to initialize {@link DocletDocsProvider}.
 *
 * This class is thread-safe. However, #generateJavadoc() will be synchronized.
 *
 * @author dellamag
 */
public class RestLiDoclet implements Doclet
{
  private static RestLiDoclet _currentDocLet = null;
  private final DocInfo _docInfo;
  private final DocletEnvironment _docEnv;

  /**
   * Generate Javadoc and return the generated RestLiDoclet instance.
   * This method is synchronized.
   *
   * @param programName Name of the program (for error messages).
   * @param errWriter PrintWriter to receive error messages.
   * @param warnWriter PrintWriter to receive warning messages.
   * @param noticeWriter PrintWriter to receive notice messages.
   * @param flatClassPath Flat path to classes to be used.
   * @param sourceFiles List of Java source files to be analyzed.
   * @return the generated RestLiDoclet instance.
   * @throws IllegalArgumentException if Javadoc fails to generate docs.
   */
  public static synchronized RestLiDoclet generateDoclet(String programName,
      PrintWriter errWriter,
      PrintWriter warnWriter,
      PrintWriter noticeWriter,
      String flatClassPath,
      List<String> sourceFiles
  )
  {
    noticeWriter.println("Generating Javadoc for " + programName);

    DocumentationTool docTool = ToolProvider.getSystemDocumentationTool();
    StandardJavaFileManager fileManager = docTool.getStandardFileManager(null, null, null);
    Iterable<? extends JavaFileObject> fileObjects = fileManager.getJavaFileObjectsFromPaths(
        sourceFiles.stream().map(Paths::get).collect(Collectors.toList()));

    // Set up the Javadoc task options
    List<String> taskOptions = new ArrayList<>();
    taskOptions.add("-classpath");
    taskOptions.add(flatClassPath);

    // Create and run the Javadoc task
    DocumentationTool.DocumentationTask task = docTool.getTask(errWriter,
        fileManager, diagnostic -> {
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
        },
        RestLiDoclet.class,
        taskOptions,
        fileObjects);

    boolean success = task.call();
    if (!success)
    {
      throw new IllegalArgumentException("Javadoc generation failed");
    }

    return _currentDocLet;
  }

  /**
   * Entry point for Javadoc Doclet.
   *
   * @param docEnv {@link DocletEnvironment} passed in by Javadoc
   * @return is successful or not
   */
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

  @Override
  public void init(Locale locale, Reporter reporter) {
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

  private RestLiDoclet(DocInfo docInfo, DocletEnvironment docEnv)
  {
    _docInfo = docInfo;
    _docEnv = docEnv;
  }

  /**
   * The reason why we create a public empty constructor is because JavadocTaskImpl in JDK 11 requires it when using reflection.
   * Otherwise, there will be NoSuchMethodException: com.linkedin.restli.tools.idlgen.RestLiDoclet.<init>()
   */
  public RestLiDoclet() {
    _docInfo = null;
    _docEnv = null;
  }

  /**
   * Query Javadoc {@link TypeElement} for the specified resource class.
   *
   * @param resourceClass resource class to be queried
   * @return corresponding {@link TypeElement}
   */
  public TypeElement getClassDoc(Class<?> resourceClass)
  {
    return _docInfo.getClassDoc(resourceClass.getCanonicalName());
  }

  /**
   * Query Javadoc {@link ExecutableElement} for the specified Java method.
   *
   * @param method Java method to be queried
   * @return corresponding {@link ExecutableElement}
   */
  public ExecutableElement getMethodDoc(Method method)
  {
    final MethodIdentity methodId = MethodIdentity.create(method);
      return _docInfo.getMethodDoc(methodId);
  }

  private static class DocInfo
  {
    public TypeElement getClassDoc(String className) {
      return _classNameToClassDoc.get(className);
    }

    public ExecutableElement getMethodDoc(MethodIdentity methodId) {
      return _methodIdToMethodDoc.get(methodId);
    }

    public void setClassDoc(String className, TypeElement classDoc) {
      _classNameToClassDoc.put(className, classDoc);
    }

    public void setMethodDoc(MethodIdentity methodId, ExecutableElement methodDoc) {
      _methodIdToMethodDoc.put(methodId, methodDoc);
    }

    private final Map<String, TypeElement> _classNameToClassDoc = new HashMap<>();
    private final Map<MethodIdentity, ExecutableElement> _methodIdToMethodDoc = new HashMap<>();
  }

  private static class MethodIdentity
  {
    public static MethodIdentity create(Method method)
    {
      final List<String> parameterTypeNames = new ArrayList<>();

      // type parameters are not included in identity because of differences between reflection and Doclet:
      // e.g. for Collection<Void>:
      //   reflection Type.toString() -> Collection<Void>
      //   Doclet Type.toString() -> Collection
      for (Class<?> paramClass: method.getParameterTypes())
      {
        parameterTypeNames.add(paramClass.getCanonicalName());
      }

      return new MethodIdentity(method.getDeclaringClass().getName() + "." + method.getName(), parameterTypeNames);
    }

    public static MethodIdentity create(ExecutableElement method)
    {
      final List<String> parameterTypeNames = new ArrayList<>();
      for (VariableElement param : method.getParameters()) {
        TypeMirror type = param.asType();
        parameterTypeNames.add(DocletHelper.getCanonicalName(type.toString()));
      }

      return new MethodIdentity(method.getEnclosingElement().toString() + "." + method.getSimpleName().toString(),
          parameterTypeNames);
    }

    private MethodIdentity(String methodQualifiedName, List<String> parameterTypeNames)
    {
      _methodQualifiedName = methodQualifiedName;
      _parameterTypeNames = parameterTypeNames;
    }

    @Override
    public int hashCode()
    {
      return new HashCodeBuilder(17, 29).
          append(_methodQualifiedName).
          append(_parameterTypeNames).
          toHashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
      if (this == obj)
      {
        return true;
      }

      if (obj == null)
      {
        return false;
      }

      if (getClass() != obj.getClass())
      {
        return false;
      }

      final MethodIdentity other = (MethodIdentity) obj;
      return new EqualsBuilder().
          append(_methodQualifiedName, other._methodQualifiedName).
          append(_parameterTypeNames, other._parameterTypeNames).
          isEquals();
    }

    private final String _methodQualifiedName;
    private final List<String> _parameterTypeNames;
  }

  /**
   * Get the list of deprecated tags for the specified element.
   *
   * @param element {@link Element} to be queried
   * @return list of deprecated tags for the specified element
   */
  public List<String> getDeprecatedTags(Element element) {
    List<String> deprecatedTags = new ArrayList<>();
    DocCommentTree docCommentTree = getDocCommentTreeForElement(element);
    if (docCommentTree == null) {
      return deprecatedTags;
    }
    for (DocTree docTree :docCommentTree.getBlockTags()) {
      if (docTree.getKind() == DocTree.Kind.DEPRECATED) {
        DeprecatedTree deprecatedTree = (DeprecatedTree) docTree;
        String deprecatedComment = deprecatedTree.getBody().toString();
        deprecatedTags.add(deprecatedComment);
      }
    }
    return deprecatedTags;
  }

  /**
   * Get the map from param name to param comment for the specified executableElement.
   *
   * @param executableElement {@link ExecutableElement} to be queried
   * @return map from param name to param comment for the specified executableElement
   */
  public Map<String, String> getParamTags(ExecutableElement executableElement) {
    Map<String, String> paramTags = new HashMap<>();
    DocCommentTree docCommentTree = getDocCommentTreeForElement(executableElement);
    if (docCommentTree == null) {
      return paramTags;
    }
    for (DocTree docTree : docCommentTree.getBlockTags()) {
      if (docTree.getKind() == DocTree.Kind.PARAM) {
        ParamTree paramTree = (ParamTree) docTree;
        String paramName = paramTree.getName().toString();
        String paramComment = paramTree.getDescription().toString();
        if (paramComment != null) {
          paramTags.put(paramName, paramComment);
        }
      }
    }
    return paramTags;
  }

  /**
   * Get the {@link DocCommentTree} for the specified element.
   *
   * @param element {@link Element} to be queried
   * @return {@link DocCommentTree} for the specified element
   */
  public DocCommentTree getDocCommentTreeForElement(Element element) {
    return element == null ? null : _docEnv.getDocTrees().getDocCommentTree(element);
  }

  /**
   * Get the Doc Comment string for the specified element.
   *
   * @param element {@link Element} to be queried
   * @return Doc Comment string for the specified element
   */
  public String getDocCommentStrForElement(Element element) {
    DocCommentTree docCommentTree = getDocCommentTreeForElement(element);
    return docCommentTree == null ? null : docCommentTree.getFullBody().toString();
  }

  /**
   * Get the {@link DocCommentTree} for the specified method.
   *
   * @param method {@link Method} to be queried
   * @return {@link DocCommentTree} for the specified method
   */
  public DocCommentTree getDocCommentTreeForMethod(Method method) {
    TypeElement typeElement = getClassDoc(method.getDeclaringClass());
    if (typeElement == null) {
      return null;
    }
    for (Element element : typeElement.getEnclosedElements()) {
      if (element.getSimpleName().toString().equals(method.getName())) {
        return getDocCommentTreeForElement(element);
      }
    }
    return null;
  }
}