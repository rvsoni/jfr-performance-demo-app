package com.shopsphere.demo.jfr;

import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JfrRecordingHelper {

    private static final Logger log = LoggerFactory.getLogger(JfrRecordingHelper.class);
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private final Path recordingsDir;
    private final Map<String, Recording> activeRecordings = new ConcurrentHashMap<>();
    private final List<String> completedRecordings = Collections.synchronizedList(new ArrayList<>());

    public JfrRecordingHelper(@Value("${shopsphere.recordings.directory:recordings}") String recordingsDirPath) {
        this.recordingsDir = Paths.get(recordingsDirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.recordingsDir);
        } catch (IOException e) {
            log.error("Failed to create recordings directory: {}", this.recordingsDir, e);
        }
    }

    public synchronized String startRecording(String scenarioId, String mode, String profileName) {
        try {
            String timestamp = TS_FMT.format(Instant.now());
            String filename = String.format("%s-%s-%s.jfr",
                    scenarioId != null ? scenarioId : "manual",
                    mode != null ? mode.toLowerCase() : "default",
                    timestamp);

            Configuration config;
            try {
                config = Configuration.getConfiguration(profileName != null ? profileName : "profile");
            } catch (Exception e) {
                log.warn("Could not load JFR profile '{}', falling back to 'default'", profileName);
                config = Configuration.getConfiguration("default");
            }

            Recording recording = new Recording(config);
            recording.setName(filename);
            recording.enable("jdk.ExecutionSample").withPeriod(java.time.Duration.ofMillis(10));
            recording.enable("jdk.JavaMonitorEnter").withThreshold(java.time.Duration.ofMillis(1));
            recording.enable("jdk.ObjectAllocationSample");
            recording.enable("jdk.GarbageCollection");
            recording.enable("jdk.GCPhasePause");
            recording.enable("jdk.ThreadPark").withThreshold(java.time.Duration.ofMillis(10));

            // Enable exception events if available
            try {
                recording.enable("jdk.JavaExceptionThrow");
            } catch (Exception ignored) {
            }

            recording.start();
            activeRecordings.put(filename, recording);

            log.info("[JFR-EVENT] Started JFR recording: '{}' (profile: {})", filename, profileName);
            return filename;
        } catch (IOException | ParseException e) {
            throw new RuntimeException("Failed to start JFR recording: " + e.getMessage(), e);
        }
    }

    public synchronized Path stopRecording(String recordingName) {
        Recording recording = activeRecordings.remove(recordingName);
        if (recording == null) {
            // Check if there is only one active recording
            if (activeRecordings.size() == 1) {
                var entry = activeRecordings.entrySet().iterator().next();
                recordingName = entry.getKey();
                recording = entry.getValue();
                activeRecordings.remove(recordingName);
            } else {
                throw new IllegalArgumentException("No active JFR recording named: " + recordingName);
            }
        }

        try {
            Path destination = recordingsDir.resolve(recordingName);
            recording.stop();
            recording.dump(destination);
            recording.close();
            completedRecordings.add(destination.toString());
            log.info("[JFR-EVENT] Stopped and dumped JFR recording to: '{}' (size: {} bytes)",
                    destination, Files.size(destination));
            return destination;
        } catch (IOException e) {
            throw new RuntimeException("Failed to dump JFR recording: " + e.getMessage(), e);
        }
    }

    public synchronized void stopAll() {
        for (String name : new HashSet<>(activeRecordings.keySet())) {
            try {
                stopRecording(name);
            } catch (Exception e) {
                log.error("Failed to stop recording '{}'", name, e);
            }
        }
    }

    public Path getRecordingsDir() {
        return recordingsDir;
    }

    public List<String> getActiveRecordings() {
        return new ArrayList<>(activeRecordings.keySet());
    }

    public List<String> getCompletedRecordings() {
        return new ArrayList<>(completedRecordings);
    }
}
