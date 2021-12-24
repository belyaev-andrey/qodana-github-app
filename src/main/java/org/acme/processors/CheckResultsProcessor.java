package org.acme.processors;


import com.contrastsecurity.sarif.Location;
import com.contrastsecurity.sarif.PhysicalLocation;
import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueBuilder;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class CheckResultsProcessor {

    private String locationsToString(List<Location> locations) {
        return locations.stream().map(
                location -> {
                    PhysicalLocation physicalLocation = location.getPhysicalLocation();
                    Region contextRegion = physicalLocation.getContextRegion();

                    return String.format("%s %d:%d\n```%s\n%s\n```",
                            physicalLocation.getArtifactLocation().getUri(),
                            contextRegion.getStartLine(),
                            contextRegion.getStartColumn(),
                            physicalLocation.getRegion().getSourceLanguage().toLowerCase(),
                            contextRegion.getSnippet().getText());
                }
        ).collect(Collectors.joining("\n"));
    }

    public GHIssue processResults(List<Result> results, GHEventPayload pushPayload) {
        String report = getReport(results);

        if (report.length() > 1) {
            GHIssueBuilder issueBuilder = pushPayload.getRepository()
                    .createIssue("Static check failed for push from: "+pushPayload.getSender().getLogin());
            issueBuilder.assignee(pushPayload.getSender());
            issueBuilder.body(report);
            try {
                return issueBuilder.create();
            } catch (IOException e) {
                throw new IllegalStateException("Cannot create issue", e);
            }
        }
        return null;
    }

    @NotNull
    String getReport(List<Result> results) {
        return results.stream()
                .filter(result -> result.getLevel() == Result.Level.WARNING || result.getLevel() == Result.Level.ERROR)
                .map(result -> String.format("%s: %s\nSource:%s",
                        result.getLevel().value().toUpperCase(),
                        result.getMessage().getText(),
                        locationsToString(result.getLocations())))
                .collect(Collectors.joining("\n"));
    }

}