package com.linkedin.intellij.pegasusplugin;

import com.intellij.navigation.GotoClassContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.linkedin.intellij.pegasusplugin.psi.PdlFullnameStubIndex;
import com.linkedin.intellij.pegasusplugin.psi.PdlTypeName;
import com.linkedin.intellij.pegasusplugin.psi.PdlTypeNameDeclaration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Provides "Navigate->Class.." lookup support for PDL schema types.
 */
public class PdlChooseByNameContributor implements GotoClassContributor {
  @NotNull
  @Override
  public String[] getNames(Project project, boolean includeNonProjectItems) {
    PdlFullnameStubIndex index = PdlFullnameStubIndex.getInstance();
    Collection<String> matchArray = index.getAllKeys(project).stream().map(fullname ->
        new PdlTypeName(fullname).getName()
    ).sorted().collect(Collectors.toList());
    return matchArray.toArray(new String[matchArray.size()]);
  }

  @NotNull
  @Override
  public NavigationItem[] getItemsByName(String name, String pattern, Project project, boolean includeNonProjectItems) {
    List<PdlTypeNameDeclaration> matches = PdlIdentifierResolver.findTypeDeclarationsByName(project, new PdlTypeName(name).getName());

    PdlTypeNameDeclaration[] matchArray = matches.toArray(new PdlTypeNameDeclaration[matches.size()]);
    Arrays.sort(matchArray, DeclComparator.INSTANCE);
    return matchArray;
  }

  private static class DeclComparator implements Comparator<PdlTypeNameDeclaration> {
    public static final DeclComparator INSTANCE = new DeclComparator();

    public int compare(PdlTypeNameDeclaration lhs, PdlTypeNameDeclaration rhs) {
      if (lhs == rhs) {
        return 0;
      } else {
        return lhs.getFullname().toString().compareTo(rhs.getFullname().toString());
      }
    }
  }

  @Nullable
  @Override
  public String getQualifiedName(NavigationItem item) {
    if (item instanceof PdlTypeNameDeclaration) {
      PdlTypeNameDeclaration decl = (PdlTypeNameDeclaration) item;
      return decl.getFullname().toString();
    }
    return null;
  }

  private static final String SEPARATOR = ".";
  @Nullable
  @Override
  public String getQualifiedNameSeparator() {
    return SEPARATOR;
  }
}
