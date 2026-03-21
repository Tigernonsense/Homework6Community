package com.hw6c.leaderboard;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.swing.JFrame;
import java.util.HashMap;

public class Main {
    static final int INITIAL_WINDOW_WIDTH   = 1024;
    static final int INITIAL_WINDOW_HEIGHT  = 600;

    // Keys expected in the config file.
    static final String TITLE_KEY = "title";
    static final String EXECUTABLES_DIR_KEY = "executablesDir";
    static final String PUZZLES_DIR_KEY = "puzzlesDir";
    static final String EXPECTED_OUTPUTS_DIR_KEY = "expectedOutputsDir";

    static HashMap<String, String> readConfig(String path) {
        File configFile = new File(path);

        if (!configFile.exists()) {
            System.out.println("Config file not found: " + path);
            return null;
        }

        String configDir = configFile.getParent();

        HashMap<String, String> configMap = new HashMap<>();

        try {
            String configContent = new String(Files.readAllBytes(configFile.toPath()), StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(new StringReader(configContent));

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Skip empty lines and comments
                }

                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    // Remove surrounding quotes if present
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }

                    // Check if the value is a relative path and adjust
                    if (value.startsWith("./") || value.startsWith(".\\")) {
                        value = configDir + File.separator + value.substring(2);
                    }
                        
                    configMap.put(key, value);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return configMap;
    }

     public static void main(String[] args) {
        // Program run as:
        // java -jar leaderboard.jar /path/to/contest/leaderboard.cfg
        // Where leaderboard.cfg will contain the paths/settings.
        if (args.length < 1) {
            System.out.println("Usage: java -jar leaderboard.jar /path/to/contest/leaderboard.cfg");
            return;
        }

        boolean trackMemoryUsage = true;

        // Optional arguments:
        // --disable-memory-tracker:  Disables creation of the memory poller thread.
        // --timeout=SECONDS: Specify the timeout, otherwise use the default of 10 seconds.
        for (String arg: args) {
            if (arg.equals("--disable-memory-tracker")) {
                trackMemoryUsage = false;
            }

            if (arg.startsWith("--timeout=")) {
                String timeoutStr = arg.substring("--timeout=".length());
                try {
                    long timeoutSeconds = Long.parseLong(timeoutStr);
                    ProcessTimer.setMaxTimeMs(timeoutSeconds * 1000);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid timeout value: " + timeoutStr);
                    return;
                }
            }
        }

        // Conditionally initialize operating system context.
        if (trackMemoryUsage) {
            ProcessTimer.initOSContext();
        }

        HashMap<String, String> config = readConfig(args[0]);
        if (config == null) {
            System.out.println("Failed to read config file.");
            return;
        }

        Leaderboard leaderboard = new Leaderboard(config.get(TITLE_KEY));
        leaderboard.setShowMemory(trackMemoryUsage);

        // Iterate over executables and puzzles
        File executablesDir = new File(config.get(EXECUTABLES_DIR_KEY));
        File puzzlesDir = new File(config.get(PUZZLES_DIR_KEY));
        File expectedOutputsDir = new File(config.get(EXPECTED_OUTPUTS_DIR_KEY));

        File[] executables = executablesDir.listFiles();
        File[] puzzles = puzzlesDir.listFiles();
        File[] expectedOutputs = expectedOutputsDir.listFiles();

        // Sort puzzles and expected outputs to ensure they are in the same order
        if (puzzles != null) {
            java.util.Arrays.sort(puzzles);
        } else {
            System.out.println("No puzzles found in " + config.get(PUZZLES_DIR_KEY));
            return;
        }

        if (expectedOutputs != null) {
            java.util.Arrays.sort(expectedOutputs);
        } else {
            System.out.println("No expected outputs found in " + config.get(EXPECTED_OUTPUTS_DIR_KEY));
            return;
        }

        if (executables == null) {
            System.out.println("No executables found in " + config.get(EXECUTABLES_DIR_KEY));
            return;
        }

        leaderboard.setTotalPuzzles(puzzles.length);

        for (File executable : executables) {
            if (executable.isFile() && executable.canExecute()) {

				// Extract name from executable (drop .exe/.out)
				int dotPos = executable.getName().lastIndexOf('.');
				if (dotPos == -1)
					dotPos = executable.getName().length();

                // Create a submission entry for this executable
                SubmissionResults submissionResult = new SubmissionResults(executable.getName().substring(0, dotPos));
				int puzzleIndex = 0;
                for (File puzzle : puzzles) {
                    try {
                        ProcessTimer timer = new ProcessTimer(executable.getAbsolutePath(), new String[]{ puzzle.getAbsolutePath(), "all" });
                        timer.setMemoryTrackerEnabled(trackMemoryUsage);
                        timer.start();
						
						String processOutput = timer.getOutput();
						String expected = leaderboard.readStringFromFile(expectedOutputs[puzzleIndex]);
						boolean matching = !timer.timedOut() && leaderboard.areOutputsEquivalent(expected, processOutput);
						
                        // Outputs were actually different, not just a timeout.
						if (!matching && !timer.timedOut()) {
							System.out.println("EXPECTED: ");
							System.out.println(expected);
							System.out.println("\nACTUAL: ");
							System.out.println(processOutput);
						}
						
						submissionResult.addPuzzleResult(timer.getElapsedTime(), matching, timer.getMemoryUsed()); // Assuming correctness is always true for now
                    
						puzzleIndex++;
					} catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                leaderboard.addSubmissionResult(submissionResult);
            } else if (executable.isFile()) {
                // Ignore files starting with . because these are Unix hidden files.
                if (executable.getName().startsWith(".")) {
                    continue;
                }

                // This is a non-executable file which is a placeholder for a contender who hasn't submitted a solution YET.
                SubmissionResults noSubmissionResult = new SubmissionResults(executable.getName());
                leaderboard.addSubmissionResult(noSubmissionResult);
            }
        }

        // Sort the results before displaying
        leaderboard.sortResults();

        // Now, create the leaderboard GUI
        JFrame frame = new JFrame(leaderboard.getTitle());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(INITIAL_WINDOW_WIDTH, INITIAL_WINDOW_HEIGHT);
        frame.add(leaderboard);
        frame.setVisible(true);
    }
}
