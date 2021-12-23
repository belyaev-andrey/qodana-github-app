package org.acme.actions;

import io.quarkiverse.githubapp.event.Push;
import org.acme.processors.CheckResultsProcessor;
import org.acme.processors.SarifReportProcessor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.kohsuke.github.GHEventPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.Arrays;

public class CommitCodeHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CommitCodeHandler.class );

    @Inject
    SarifReportProcessor sarifReportProcessor;

    @Inject
    CheckResultsProcessor checkResultsProcessor;

    void onCodeCommitted(@Push GHEventPayload.Push pushPayload) {
        LOG.info("Pushed to repository");
        String url = pushPayload.getRepository().getHttpTransportUrl();

        try {
            Path resultDir = Files.createTempDirectory("git-result",
                        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx")));

            if (!runChecks(url, resultDir)) return;

            Arrays.stream(resultDir.toFile().listFiles())
                    .filter(file -> "qodana.sarif.json".equals(file.getName()))
                    .findFirst().map(f -> sarifReportProcessor.process(f))
                    .map(results -> checkResultsProcessor.processResults(results, pushPayload))
                    .ifPresent(issue -> LOG.error("Problems found, issue "+issue.getNumber()+" created"));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private boolean runChecks(String url, Path resultDir) {
        try (GenericContainer container = new GenericContainer("jetbrains/qodana-jvm")) {

            Path srcDir = Files.createTempDirectory("git-src",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx")));

            container.withFileSystemBind(srcDir.toFile().getAbsolutePath(), "/data/project/")
                    .withFileSystemBind(resultDir.toFile().getAbsolutePath(), "/data/results/")
                    .waitingFor(Wait.forLogMessage(".*IDEA exit code.*\\n", 1)
                            .withStartupTimeout(Duration.ofMinutes(5)));

            Git git = Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(srcDir.toFile())
                    .call();

            container.start();
            LOG.info(container.getLogs());

        } catch (IOException | GitAPIException e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
        return true;
    }


}