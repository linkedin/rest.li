package com.linkedin.intellij.pegasusplugin.test;

import com.intellij.codeInsight.completion.LightFixtureCompletionTestCase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;


/**
 * Base test fixture from which all other unit tests extend.
 */
public abstract class PdlLightTestBase extends LightFixtureCompletionTestCase {
  @NotNull
  @Override
  protected LightProjectDescriptor getProjectDescriptor() {
    return new LightProjectDescriptor() {
      @Override
      public String getModuleTypeId() {
        return ModuleTypeId.JAVA_MODULE;
      }

      @Override
      public void configureModule(Module module, ModifiableRootModel model, ContentEntry contentEntry) {
        final Library.ModifiableModel modifiableModel = model.getModuleLibraryTable().createLibrary("external-pegasus").getModifiableModel();
        final VirtualFile pegasusJar = JarFileSystem.getInstance().refreshAndFindFileByPath("src/test/resources/common/external-pegasus.jar"+"!/");
        modifiableModel.addRoot(pegasusJar, OrderRootType.CLASSES);
        modifiableModel.commit();
      }
    };
  }
}
