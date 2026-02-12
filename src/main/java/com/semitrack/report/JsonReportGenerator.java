package com.semitrack.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.semitrack.model.WaferTestResult;

import java.util.List;

/**
 * Serialises the result list to a pretty-printed JSON string on stdout.
 *
 * In production this would write to a file or POST to a monitoring endpoint.
 * Kept simple here so the system can be run and inspected without additional
 * infrastructure.
 */
public class JsonReportGenerator implements ReportGenerator {

    private final ObjectMapper mapper;

    public JsonReportGenerator() {
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public void generate(List<WaferTestResult> results) {
        try {
            String json = mapper.writeValueAsString(results);
            System.out.println("[JsonReport] Output:");
            System.out.println(json);
        } catch (Exception e) {
            System.err.println("[JsonReport] Serialisation failed: " + e.getMessage());
        }
    }

    @Override
    public String name() { return "JsonReportGenerator"; }
}
