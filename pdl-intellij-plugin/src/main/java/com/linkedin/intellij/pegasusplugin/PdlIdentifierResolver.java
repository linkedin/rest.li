/*
 * Copyright 2015 Coursera Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linkedin.intellij.pegasusplugin;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.linkedin.intellij.pegasusplugin.pdsc.PdscReferenceUtils;
import com.linkedin.intellij.pegasusplugin.psi.PdlFullnameStubIndex;
import com.linkedin.intellij.pegasusplugin.psi.PdlNameStubIndex;
import com.linkedin.intellij.pegasusplugin.psi.PdlPsiFile;
import com.linkedin.intellij.pegasusplugin.psi.PdlTypeName;
import com.linkedin.intellij.pegasusplugin.psi.PdlTypeNameDeclaration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Utility methods for looking up types using an IntelliJ managed "stub index".
 */
public class PdlIdentifierResolver {
  private PdlIdentifierResolver() {
  }

  /**
   * Shortcut for {@link #findTypeDeclarationsByName(Project, PdlPsiFile, String)} with the context file being null.
   * In other words, the reference is being resolved from no particular file.
   *
   * @param project project in which to resolve the reference
   * @param name simple name of the reference
   * @return all type declarations corresponding to this simple name
   */
  public static List<PdlTypeNameDeclaration> findTypeDeclarationsByName(Project project, String name) {
    return findTypeDeclarationsByName(project, null, name);
  }

  /**
   * Finds all type declarations corresponding to a given simple name when used in a given file.
   *
   * @param project project in which to resolve the reference
   * @param context file in which this simple name is used as a reference
   * @param name simple name of the reference
   * @return all type declarations corresponding to this simple name
   */
  public static List<PdlTypeNameDeclaration> findTypeDeclarationsByName(Project project,
      @Nullable PdlPsiFile context,
      String name) {
    PdlNameStubIndex index = PdlNameStubIndex.getInstance();
    GlobalSearchScope scope = GlobalSearchScope.allScope(project);
    List<PdlTypeNameDeclaration> decls = index.get(name, project, scope)
        .stream()
        .filter(decl -> decl.isAccessibleFrom(context))
        .collect(Collectors.toList());
    return Collections.unmodifiableList(decls);
  }

  /**
   * Returns true if a given fullname refers to a resolvable type when used in a given file.
   *
   * @param context file in which this fullname is used as a reference
   * @param fullname fullname of the reference
   * @return whether this fullname is resolvable
   */
  public static boolean isResolvableTypeDeclaration(@NotNull PdlPsiFile context, PdlTypeName fullname) {
    return findTypeDeclaration(context.getProject(), context, fullname) != null;
  }

  /**
   * Finds the declaration of a type, which may either be in a PDL file's {@link PdlTypeNameDeclaration} or a
   * PDSC {@link PsiFile}.
   *
   * @param project project in which to resolve the reference
   * @param context file in which this fullname is used as a reference
   * @param fullname fullname of the reference
   * @return either a {@link PdlTypeNameDeclaration} or if the reference is to PDSC, a {@link PsiFile}.
   */
  public static PsiElement findTypeDeclaration(Project project, @Nullable PdlPsiFile context, PdlTypeName fullname) {
    PdlFullnameStubIndex index = PdlFullnameStubIndex.getInstance();
    GlobalSearchScope scope = GlobalSearchScope.allScope(project);
    Optional<PdlTypeNameDeclaration> pdlDecl = index.get(fullname.toString(), project, scope).stream().findFirst();
    if (pdlDecl.isPresent() && pdlDecl.get().isAccessibleFrom(context)) {
      return pdlDecl.get();
    } else {
      return PdscReferenceUtils.findPdscTypeDeclaration(project, fullname);
    }
  }
}
