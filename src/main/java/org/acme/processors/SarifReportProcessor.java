package org.acme.processors;

import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.SarifSchema210;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class SarifReportProcessor {

    public List<Result> process(File file) {
        try {
            SarifSchema210 sarifSchema210 = new ObjectMapper().readValue(file, SarifSchema210.class);
            List<Result> results = sarifSchema210.getRuns().stream()
                    .flatMap(r -> r.getResults().stream())
                    .collect(Collectors.toList());
            return results;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

}