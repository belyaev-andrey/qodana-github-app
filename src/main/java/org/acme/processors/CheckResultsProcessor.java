package org.acme.processors;


import com.contrastsecurity.sarif.Location;
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
                location ->
                        location.getPhysicalLocation().getArtifactLocation().getUri() + " " +
                        location.getPhysicalLocation().getContextRegion().getStartLine() + ":" +
                                location.getPhysicalLocation().getContextRegion().getStartColumn()
        ).collect(Collectors.joining("\n"));
    }

    public GHIssue processResults(List<Result> results, GHEventPayload pushPayload) {
        String report = getReport(results);

        if (report.length() > 1) {
            GHIssueBuilder issueBuilder = pushPayload.getRepository()
                    .createIssue("Static check failed for push from: "+pushPayload.getSender().getLogin());
            issueBuilder.assignee(pushPayload.getSender());
            issueBuilder.body(report);
            GHIssue issue = null;
            try {
                issue = issueBuilder.create();
            } catch (IOException e) {
                throw new IllegalStateException("Cannot create issue", e);
            }
            return issue;
        }
        return null;
    }

    @NotNull
    String getReport(List<Result> results) {
        return results.stream()
                .filter(result -> result.getLevel() == Result.Level.WARNING || result.getLevel() == Result.Level.ERROR)
                .map(result -> result.getLevel() + ": " + result.getMessage().getText() +
                        "\n: " + locationsToString(result.getLocations())).collect(Collectors.joining("\n"));
    }

}