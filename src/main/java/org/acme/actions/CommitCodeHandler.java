package org.acme.actions;

import io.quarkiverse.githubapp.event.Push;
import org.acme.processors.CheckResultsProcessor;
import org.acme.processors.SarifReportProcessor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.github.GHEventPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.Arrays;

public class CommitCodeHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CommitCodeHandler.class);

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

            Path srcDir = Files.createTempDirectory("git-src",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx")));

            Path pluginsDir = ConfigProvider.getConfig()
                    .getOptionalValue("qodana.plugins.dir", String.class)
                    .map(s -> new File(s).toPath())
                    .orElse(null);//Dirty but fair

            if (!runChecks(url, srcDir, resultDir, pluginsDir)) return;

            Arrays.stream(resultDir.toFile().listFiles())
                    .filter(file -> "qodana.sarif.json".equals(file.getName()))
                    .findFirst().map(f -> sarifReportProcessor.process(f))
                    .map(results -> checkResultsProcessor.processResults(results, pushPayload))
                    .ifPresent(issue -> LOG.error("Problems found, issue " + issue.getNumber() + " created"));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    boolean runChecks(String url, Path srcDir, Path resultDir, @Nullable Path pluginsDir) {

        LOG.info(String.format(
                "Starting qodana to analyze \n\tgit: %s \n\tcheckout dir: %s \n\tresults dir: %s \n\tplugins dir %s",
                url, srcDir, resultDir, pluginsDir));

        try (GenericContainer container = new GenericContainer("jetbrains/qodana-jvm")) {

            container.withFileSystemBind(srcDir.toFile().getAbsolutePath(), "/data/project/")
                    .withFileSystemBind(resultDir.toFile().getAbsolutePath(), "/data/results/")
                    .waitingFor(Wait.forLogMessage(".*IDEA exit code.*\\n", 1)
                            .withStartupTimeout(Duration.ofMinutes(5)));

            if (pluginsDir != null) {
                String pluginName = pluginsDir.getFileName().toString();
                container.withFileSystemBind(pluginsDir.toFile().getAbsolutePath(), "/opt/idea/plugins/" + pluginName);
            }

            Git git = Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(srcDir.toFile())
                    .call();

            container.start();
            LOG.info(container.getLogs());

        } catch (GitAPIException e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
        return true;
    }


}