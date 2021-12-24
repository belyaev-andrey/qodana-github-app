package org.acme.actions;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

public class QodanaAnalyzerTest {

    private static Path srcDir;
    private static Path resultDir;
    private static Path pluginsDir;

    @BeforeClass
    public static void prepareFolders() throws IOException {
        srcDir = Files.createDirectory(new File("target/git-test"+ ThreadLocalRandom.current().nextLong()).toPath(),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx")));

        resultDir = Files.createDirectory(new File("target/git-test-result"+ThreadLocalRandom.current().nextLong()).toPath(),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx")));

        pluginsDir = null; //TODO load plugins if needed
    }

    @Test
    public void testAnalyzerStartup() throws IOException {
        CommitCodeHandler commitCodeHandler = new CommitCodeHandler();

        commitCodeHandler.runChecks("https://github.com/belyaev-andrey/bot-playground.git", srcDir, resultDir, pluginsDir);

        assertTrue(resultDir.toFile().exists());
        assertNotNull(resultDir.toFile().listFiles());

        Arrays.stream(resultDir.toFile().listFiles())
                .filter(file -> "qodana.sarif.json".equals(file.getName()))
                .findFirst().orElseThrow(AssertionError::new);
    }

    @AfterClass
    public static void removeFolders() throws IOException {
        srcDir.toFile().deleteOnExit();
        resultDir.toFile().deleteOnExit();
    }

}