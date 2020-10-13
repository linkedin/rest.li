package com.linkedin.intellij.pegasusplugin.pdsc;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Query;
import com.linkedin.intellij.pegasusplugin.psi.PdlTypeName;
import java.util.ArrayList;
import java.util.Collection;
import org.jetbrains.annotations.Nullable;


public class PdscReferenceUtils {

  private PdscReferenceUtils() {
  }

  /**
   * Find all pdsc types by finding all files with the .pdsc file extension and inferring their
   * namespace by their relative path to the nearest source root.
   *
   * @param project provides the project to find find files for.
   * @return a collection of pegasus type names.
   */
  public static Collection<PdlTypeName> findPdscFiles(Project project, GlobalSearchScope scope) {
    ArrayList<PdlTypeName> results = new ArrayList<>();
    Collection<VirtualFile> pdscFiles = FilenameIndex.getAllFilesByExt(project, "pdsc", scope);
    PsiManager manager = PsiManager.getInstance(project);
    for (VirtualFile pdscFile : pdscFiles) {

      PsiFile pdscPsiFile = manager.findFile(pdscFile);
      if (pdscPsiFile != null) {
        PsiDirectory directory = pdscPsiFile.getContainingDirectory();
        if (directory != null) {
          PsiPackage sourceSetPackage = JavaDirectoryService.getInstance().getPackage(directory);
          String typeName = pdscFile.getNameWithoutExtension();
          if (sourceSetPackage != null) {
            String namespace = sourceSetPackage.getQualifiedName();

            // Schemas in JARs should be packaged under the 'pegasus' root directory.
            if (isJarFile(pdscFile)) {
              // If the schema is in the correct directory then strip the namespace, else ignore it.
              if (namespace.startsWith("pegasus.")) {
                namespace = namespace.replaceFirst("pegasus\\.", "");
              } else {
                continue;
              }
            }

            results.add(new PdlTypeName(namespace, typeName));
          }
        }
      }
    }
    return results;
  }

  /**
   * Attempt to find a PDSC file where the file path and file name match the given pegasus type fullname.
   */
  @Nullable
  public static PsiFile findPdscTypeDeclaration(Project project, PdlTypeName fullname) {
    String filename = fullname.getName() + ".pdsc";
    Query<VirtualFile> directories =
        DirectoryIndex.getInstance(project).getDirectoriesByPackageName(fullname.getNamespace(), false);
    for (VirtualFile file: directories) {
      VirtualFile child = file.findChild(filename);
      if (child != null) {
        return PsiManager.getInstance(project).findFile(child);
      }
    }

    Query<VirtualFile> jarDirectories =
        DirectoryIndex.getInstance(project).getDirectoriesByPackageName("pegasus." + fullname.getNamespace(), false);
    for (VirtualFile file: jarDirectories) {
      VirtualFile child = file.findChild(filename);
      if (child != null && isJarFile(child)) {
        return PsiManager.getInstance(project).findFile(child);
      }
    }

    return null;
  }

  public static boolean isJarFile(VirtualFile file) {
    return file.getFileSystem().getProtocol().equals(StandardFileSystems.JAR_PROTOCOL);
  }
}
