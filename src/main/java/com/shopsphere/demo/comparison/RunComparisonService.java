package com.shopsphere.demo.comparison;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@Service
public class RunComparisonService {

    private final ObjectMapper objectMapper;

    public RunComparisonService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void saveResult(RunResult result, Path outputPath) throws IOException {
        File file = outputPath.toFile();
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        objectMapper.writeValue(file, result);
    }

    public RunResult loadResult(Path inputPath) throws IOException {
        return objectMapper.readValue(inputPath.toFile(), RunResult.class);
    }

    public RunComparison compareRuns(Path beforeJsonPath, Path afterJsonPath) throws IOException {
        RunResult before = loadResult(beforeJsonPath);
        RunResult after = loadResult(afterJsonPath);
        return RunComparison.compare(before, after);
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
