package com.shopsphere.demo.jfr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JfrRecordingTest {

    @Test
    void testJfrStartAndDump(@TempDir Path tempDir) throws Exception {
        JfrRecordingHelper helper = new JfrRecordingHelper(tempDir.toString());
        String recordingName = helper.startRecording("test-scenario", "faulty", "default");

        assertThat(helper.getActiveRecordings()).contains(recordingName);

        // Do a tiny bit of work
        long sum = 0;
        for (int i = 0; i < 1000; i++) {
            sum += i;
        }

        Path dumped = helper.stopRecording(recordingName);
        assertThat(dumped).isNotNull();
        assertThat(Files.exists(dumped)).isTrue();
        assertThat(Files.size(dumped)).isGreaterThan(0);
        assertThat(helper.getCompletedRecordings()).contains(dumped.toString());
    }
}
