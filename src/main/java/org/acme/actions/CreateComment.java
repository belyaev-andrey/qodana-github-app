package org.acme.actions;

import io.quarkiverse.githubapp.event.Issue;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHIssue;

import java.io.IOException;
import java.time.LocalDateTime;

public class CreateComment {

    private static final Logger LOG = Logger.getLogger( CreateComment.class );

    void onOpen(@Issue.Opened GHEventPayload.Issue issuePayload) throws IOException {
        LOG.info("Issue is opened");
        GHIssue issue = issuePayload.getIssue();
        issue.comment("Issue "+issue.getNumber()+" is opened on "+ LocalDateTime.now()+"");
    }

    void onEdited (@Issue.Edited GHEventPayload.Issue issuePayload) throws IOException {
        LOG.info("Issue is edited");
        GHIssue issue = issuePayload.getIssue();
        issue.comment("Issue "+issue.getNumber()+
                " is edited on "+ LocalDateTime.now()+" by "+issue.getUser().getLogin());
    }
}