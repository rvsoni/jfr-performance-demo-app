package com.shopsphere.demo.comparison;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ComparisonCli {

    public static void main(String[] args) {
        Map<String, String> params = parseArgs(args);

        String beforeFile = params.get("before");
        String afterFile = params.get("after");
        String outputFile = params.get("output");

        if (beforeFile == null || afterFile == null) {
            System.err.println("Usage: java -cp <jar> com.shopsphere.demo.comparison.ComparisonCli --before=<before.json> --after=<after.json> [--output=<report.md>]");
            System.exit(1);
        }

        try {
            RunComparisonService service = new RunComparisonService();
            RunComparison comparison = service.compareRuns(Paths.get(beforeFile), Paths.get(afterFile));

            System.out.println(comparison.markdownReport());

            if (outputFile != null) {
                Path outPath = Paths.get(outputFile);
                if (outPath.getParent() != null) {
                    Files.createDirectories(outPath.getParent());
                }
                Files.writeString(outPath, comparison.markdownReport());
                System.out.printf("Report saved to: %s%n", outputFile);
            }
        } catch (Exception e) {
            System.err.println("Comparison failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--") && arg.contains("=")) {
                String[] parts = arg.substring(2).split("=", 2);
                map.put(parts[0].trim().toLowerCase(), parts[1].trim());
            }
        }
        return map;
    }
}
