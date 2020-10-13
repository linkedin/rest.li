package com.linkedin.intellij.pegasusplugin.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndexKey;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;


/**
 * Create a IntelliJ managed "stub index" of identifiers by their unqualified name.
 *
 * Primarily used to find all types for a given unqualified name.
 */
public class PdlNameStubIndex extends StringStubIndexExtension<PdlTypeNameDeclaration> {
  public static final StubIndexKey KEY = StubIndexKey.createIndexKey("NAME");

  private static final PdlNameStubIndex INSTANCE = new PdlNameStubIndex();

  public static PdlNameStubIndex getInstance() {
    return INSTANCE;
  }

  @NotNull
  @Override
  public StubIndexKey<String, PdlTypeNameDeclaration> getKey() {
    return KEY;
  }

  @Override
  public Collection<PdlTypeNameDeclaration> get(@NotNull String s, @NotNull Project project,
      @NotNull GlobalSearchScope scope) {
    return super.get(s, project, scope);
  }
}
