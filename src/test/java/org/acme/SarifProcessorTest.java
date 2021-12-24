package org.acme;

import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.SarifSchema210;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SarifProcessorTest {

    @Test
    public void testParseSarif() throws IOException {
        URL resource = getClass().getResource("qodana.sarif.json");
        SarifSchema210 sarifSchema210 = new ObjectMapper().readValue(resource, SarifSchema210.class);
        assertEquals(1, sarifSchema210.getRuns().size());
        assertNotNull(sarifSchema210.getRuns().get(0).getResults());

        List<Result> results = sarifSchema210.getRuns().get(0).getResults().stream()
                .filter(result -> result.getLevel() == Result.Level.WARNING)
                .collect(Collectors.toList());
        assertEquals(2, results.size());
    }

}