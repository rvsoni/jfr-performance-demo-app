package com.shopsphere.demo.controller;

import com.shopsphere.demo.jfr.JfrRecordingHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/jfr")
public class JfrController {

    private final JfrRecordingHelper jfrRecordingHelper;

    public JfrController(JfrRecordingHelper jfrRecordingHelper) {
        this.jfrRecordingHelper = jfrRecordingHelper;
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start(
            @RequestParam(required = false, defaultValue = "manual") String scenarioId,
            @RequestParam(required = false, defaultValue = "default") String mode,
            @RequestParam(required = false, defaultValue = "profile") String profile
    ) {
        String filename = jfrRecordingHelper.startRecording(scenarioId, mode, profile);
        return ResponseEntity.ok(Map.of(
                "status", "RECORDING_STARTED",
                "recordingFile", filename,
                "directory", jfrRecordingHelper.getRecordingsDir().toString()
        ));
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stop(
            @RequestParam(required = false) String filename
    ) {
        Path path = jfrRecordingHelper.stopRecording(filename);
        return ResponseEntity.ok(Map.of(
                "status", "RECORDING_SAVED",
                "filePath", path.toString()
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "activeRecordings", jfrRecordingHelper.getActiveRecordings(),
                "completedRecordings", jfrRecordingHelper.getCompletedRecordings(),
                "recordingsDir", jfrRecordingHelper.getRecordingsDir().toString()
        ));
    }
}
