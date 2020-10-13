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

package com.linkedin.intellij.pegasusplugin.psi;

import com.intellij.navigation.NavigationItem;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.StubBasedPsiElement;
import org.jetbrains.annotations.Nullable;


/**
 * Models a type declaration identifier.
 */
public interface PdlTypeNameDeclarationInterface extends PsiNameIdentifierOwner, NavigationItem, StubBasedPsiElement<PdlTypeNameDeclarationStub> {
  String getName();
  PdlTypeName getFullname();
  PdlPsiFile getPdlFile();
  String getSchemaTypeName();
  boolean isDeprecated();

  @Nullable
  PdlNamedTypeDeclaration getNamedTypeDeclaration();

  /**
   * Returns true if this type declaration can be referenced from the given file. Currently in Pegasus, there is no
   * guarantee that non-root types can be referenced from other files. Thus, those are "inaccessible" from another file.
   * If the provided file is null, then the result is always true.
   *
   * @param pdlFile file from which this type would be referenced
   * @return whether this type is accessible from a given file
   */
  boolean isAccessibleFrom(@Nullable PdlPsiFile pdlFile);
}
