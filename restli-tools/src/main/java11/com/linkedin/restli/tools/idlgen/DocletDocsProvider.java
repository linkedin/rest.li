/*
   Copyright (c) 2023 LinkedIn Corp.

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


import com.linkedin.restli.internal.server.model.ResourceModelEncoder.DocsProvider;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.QueryParam;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import org.apache.commons.io.output.NullWriter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;


/**
 * This file is using Java 11 APIs to implement the same logic as its Java 8 counterpart located in
 * restli-tools/src/main/java/com/linkedin/restli/tools/idlgen/DocletDocsProvider.java
 *
 * Specialized {@link DocsProvider} whose documentation comes from the Javadoc Doclet {@link RestLiDoclet}.
 *
 * @author Yan Zhou
 */
public class DocletDocsProvider implements DocsProvider {
  private static final Logger log = LoggerFactory.getLogger(DocletDocsProvider.class);

  private final String _apiName;
  private final String[] _classpath;
  private final String[] _sourcePaths;
  private final String[] _resourcePackages;

  private RestLiDoclet _doclet;

  public DocletDocsProvider(String apiName,
      String[] classpath,
      String[] sourcePaths,
      String[] resourcePackages) {
    _apiName = apiName;
    _classpath = classpath;
    _sourcePaths = sourcePaths;
    _resourcePackages = resourcePackages;
  }

  @Override
  public Set<String> supportedFileExtensions() {
    return Collections.singleton(".java");
  }

  /**
   * Recursively collect all Java file paths under the sourcePaths if packageNames is null or empty. Else, only
   * collect the Java file paths whose package name starts with packageNames.
   *
   * @param sourcePaths source paths to be queried
   * @param packageNames target package names to be matched
   * @return list of Java file paths
   */
  public static List<String> collectSourceFiles(List<String> sourcePaths, List<String> packageNames) throws IOException {
    List<String> sourceFiles = new ArrayList<>();
    for (String sourcePath : sourcePaths) {
      Path basePath = Paths.get(sourcePath);
      if (!Files.exists(basePath)) {
        continue;
      }
      Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
          if (file.toString().endsWith(".java")) {
            if (packageNames == null || packageNames.isEmpty()) {
              sourceFiles.add(file.toString());
            } else {
              String packageName = basePath.relativize(file.getParent()).toString().replace('/', '.');
              for (String targetPackageName : packageNames) {
                if (packageName.startsWith(targetPackageName)) {
                  sourceFiles.add(file.toString());
                  break;
                }
              }
            }
          }
          return FileVisitResult.CONTINUE;
        }
      });
    }
    return sourceFiles;
  }

  @Override
  public void registerSourceFiles(Collection<String> sourceFileNames) {
    log.debug("Executing Javadoc tool...");
    final String flatClasspath;
    if (_classpath == null) {
      flatClasspath = System.getProperty("java.class.path");
    }
    else {
      flatClasspath = StringUtils.join(_classpath, ":");
    }

    final PrintWriter sysoutWriter = new PrintWriter(System.out, true);
    final PrintWriter nullWriter = new PrintWriter(new NullWriter());

    List<String> sourceFiles;
    try {
      sourceFiles = collectSourceFiles(Arrays.asList(_sourcePaths),
          _resourcePackages == null ? null : Arrays.asList(_resourcePackages));
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to collect source files", e);
    }

    _doclet = RestLiDoclet.generateDoclet(_apiName,
        sysoutWriter,
        nullWriter,
        nullWriter,
        flatClasspath,
        sourceFiles
    );
  }

  @Override
  public String getClassDoc(Class<?> resourceClass) {
    final TypeElement doc = _doclet.getClassDoc(resourceClass);
    if (doc == null) {
      return null;
    }
    return buildDoc(_doclet.getDocCommentStrForElement(doc));
  }

  public String getClassDeprecatedTag(Class<?> resourceClass) {
    TypeElement typeElement = _doclet.getClassDoc(resourceClass);
    if (typeElement == null) {
      return null;
    }
    return formatDeprecatedTags(typeElement);
  }

  private String formatDeprecatedTags(Element element) {
    List<String> deprecatedTags = _doclet.getDeprecatedTags(element);
    if (!deprecatedTags.isEmpty()) {
      StringBuilder deprecatedText = new StringBuilder();
      for (int i = 0; i < deprecatedTags.size(); i++) {
        deprecatedText.append(deprecatedTags.get(i));
        if (i < deprecatedTags.size() - 1) {
          deprecatedText.append(" ");
        }
      }
      return deprecatedText.toString();
    } else {
      return null;
    }
  }
  @Override
  public String getMethodDoc(Method method) {
    final ExecutableElement doc = _doclet.getMethodDoc(method);
    if (doc == null) {
      return null;
    }

    return buildDoc(_doclet.getDocCommentStrForElement(doc));
  }

  @Override
  public String getMethodDeprecatedTag(Method method) {
    final ExecutableElement doc = _doclet.getMethodDoc(method);
    if (doc == null) {
      return null;
    }
    return formatDeprecatedTags(doc);
  }


  @Override
  public String getParamDoc(Method method, String name) {
    final ExecutableElement methodDoc = _doclet.getMethodDoc(method);

    if (methodDoc == null) {
      return null;
    }
    Map<String, String> paramTags = _doclet.getParamTags(methodDoc);
    for (VariableElement parameter : methodDoc.getParameters()) {
      for (AnnotationMirror annotationMirror : parameter.getAnnotationMirrors()) {
        if (isQueryParamAnnotation(annotationMirror) || isActionParamAnnotation(annotationMirror)) {
          for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet()) {
            if ("value".equals(entry.getKey().getSimpleName().toString()) && name.equals(entry.getValue().getValue())) {
              return paramTags.get(parameter.getSimpleName().toString());
            }
          }
        }
      }
    }

    return null;
  }

  @Override
  public String getReturnDoc(Method method) {
    ExecutableElement methodElement = _doclet.getMethodDoc(method);
    if (methodElement != null) {
      for (DocTree docTree : _doclet.getDocCommentTreeForMethod(method).getBlockTags()) {
        if (!docTree.toString().toLowerCase().startsWith("@return")) {
          continue;
        }
        DocTree.Kind kind = docTree.getKind();
        if (kind == DocTree.Kind.RETURN) {
          ReturnTree returnTree = (ReturnTree) docTree;
           return buildDoc(DocletHelper.convertDocTreeListToStr(returnTree.getDescription()));
        } else if (kind == DocTree.Kind.UNKNOWN_BLOCK_TAG) {
          UnknownBlockTagTree unknownBlockTagTree = (UnknownBlockTagTree) docTree;
          return buildDoc(DocletHelper.convertDocTreeListToStr(unknownBlockTagTree.getContent()));
        }
      }
    }
    return null;
  }

  private static String buildDoc(String docText) {
    if (docText != null && !docText.isEmpty()) {
      return docText;
    }
    return null;
  }

  private static boolean isQueryParamAnnotation(AnnotationMirror annotationMirror) {
    return QueryParam.class.getCanonicalName().equals(annotationMirror.getAnnotationType().toString());
  }

  private static boolean isActionParamAnnotation(AnnotationMirror annotationMirror) {
    return ActionParam.class.getCanonicalName().equals(annotationMirror.getAnnotationType().toString());
  }
}