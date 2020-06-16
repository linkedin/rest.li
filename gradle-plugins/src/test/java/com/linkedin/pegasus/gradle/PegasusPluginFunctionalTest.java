package com.linkedin.pegasus.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;
import static org.junit.Assert.*;


public class PegasusPluginFunctionalTest {
    private static final String MAIN_SYNC_SCHEMAS = ":mainSyncSchemas";
    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    @Test
    public void testApplyPluginToProject() throws IOException {
        // Given: Default project with PegasusPlugin.
        setupProject(testProjectDir);

        // When: Run the build.
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withPluginClasspath()
                .withArguments("tasks")
                .build();

        // Then: Validate plugin was applied successfully.
        assertNotNull(result.getOutput());
    }

    @Test
    public void testMainSyncSchemas_whenTaskIsUpToDate() throws IOException {
        // Given: Default project with PegasusPlugin and one schema file.
        setupProject(testProjectDir);
        File schemasDir = createSchemaDirectory(testProjectDir);
        String schemaFilename = "A.pdsc";
        new File(schemasDir, schemaFilename).createNewFile();

        // When: Run the mainSyncSchemas task.
        GradleRunner runner = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments(MAIN_SYNC_SCHEMAS)
                .withPluginClasspath();

        // Then: Validate task was a success and file exists in build directory's mainSchemas folder.
        String mainSchemasDir = testProjectDir.getRoot() + File.separator + "build" + File.separator + "mainSchemas" + File.separator;
        BuildResult result = runner.build();
        File buildSchema = new File(mainSchemasDir + schemaFilename);
        assertTrue(result.task(MAIN_SYNC_SCHEMAS).getOutcome() == SUCCESS);
        assertTrue(buildSchema.exists());

        // When: Rerun the task.
        result = runner.build();

        // Then: Validate task was up-to-date and file exists in the build directory's mainSchemas folder.
        assertTrue(result.task(MAIN_SYNC_SCHEMAS).getOutcome() == UP_TO_DATE);
        assertTrue(buildSchema.exists());
    }

    @Test
    public void testMainSyncSchemas_whenFileBecomesStale() throws IOException {
        // Given: Default project with PegasusPlugin and two schema files.
        setupProject(testProjectDir);
        File schemasDir = createSchemaDirectory(testProjectDir);
        String schemaFilename1 = "A.pdsc";
        new File(schemasDir, schemaFilename1).createNewFile();
        String schemaFilename2 = "B.pdsc";
        File schemaFile2 = new File(schemasDir, schemaFilename2);
        schemaFile2.createNewFile();

        // When: Run the mainSyncSchemas task.
        GradleRunner runner = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments(MAIN_SYNC_SCHEMAS)
                .withPluginClasspath();

        // Then: Validate task was a success and both files were moved to the build directory's mainSchemas folder.
        String mainSchemasDir = testProjectDir.getRoot() + File.separator + "build" + File.separator + "mainSchemas" + File.separator;
        BuildResult result = runner.build();
        File buildSchema1 = new File(mainSchemasDir + schemaFilename1);
        File buildSchema2 = new File(mainSchemasDir + schemaFilename2);
        assertTrue(result.task(MAIN_SYNC_SCHEMAS).getOutcome() == SUCCESS);
        assertTrue(buildSchema1.exists());
        assertTrue(buildSchema2.exists());

        // When: Delete one of the schemas and rerun the task.
        assertTrue(schemaFile2.delete());
        result = runner.build();

        // Then: Validate task was a success and stale file gets removed from build directory's mainSchemas folder.
        assertTrue(result.task(MAIN_SYNC_SCHEMAS).getOutcome() == SUCCESS);
        assertTrue(buildSchema1.exists());
        assertFalse(buildSchema2.exists());
    }

    /**
     * Write file to disk with specified content.
     */
    private void writeFile(File destination, String content) throws IOException {
        try (BufferedWriter output = new BufferedWriter(new FileWriter(destination))) {
            output.write(content);
        }
    }

    /**
     * Set up project that applies the pegasus plugin.
     */
    private void setupProject(TemporaryFolder temporaryFolder) throws IOException {
        File buildFile = temporaryFolder.newFile("build.gradle");
        String buildFileContent = "plugins { id 'pegasus' }\n";
        writeFile(buildFile, buildFileContent);
    }

    /**
     * Create pegasus schema src directory.
     */
    private File createSchemaDirectory(TemporaryFolder temporaryFolder) {
        File schemasDir = new File(temporaryFolder.getRoot() + File.separator + "src" + File.separator + "main" + File.separator + "pegasus");
        schemasDir.mkdirs();
        return schemasDir;
    }
}
