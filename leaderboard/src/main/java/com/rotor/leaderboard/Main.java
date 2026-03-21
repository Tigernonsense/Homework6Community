package com.rotor.leaderboard;

import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;

public class Main {
    static final int INITIAL_WINDOW_WIDTH   = 1024;
    static final int INITIAL_WINDOW_HEIGHT  = 600;

     public static void main(String[] args) {
        // Program run as:
        // java -jar PipsLeaderboard.jar <executables> <puzzles> <outputs>
        // executables contains all of the programs being compared,
        // puzzles contains all of the puzzles being tested,
        // outputs is the expected output for each puzzle to ensure correctness.
        if (args.length < 3) {
            System.out.println("Usage: java -jar PipsLeaderboard.jar <executables> <puzzles> <outputs>");
            return;
        }

        boolean trackMemoryUsage = true;

        // Arguments:
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

        Leaderboard leaderboard = new Leaderboard("Pips Solver Leaderboard");
        leaderboard.setShowMemory(trackMemoryUsage);

        // Iterate over executables and puzzles
        File executablesDir = new File(args[0]);
        File puzzlesDir = new File(args[1]);
        File expectedOutputsDir = new File(args[2]);

        File[] executables = executablesDir.listFiles();
        File[] puzzles = puzzlesDir.listFiles();
        File[] expectedOutputs = expectedOutputsDir.listFiles();

        // Sort puzzles and expected outputs to ensure they are in the same order
        if (puzzles != null) {
            java.util.Arrays.sort(puzzles);
        } else {
            System.out.println("No puzzles found in " + args[1]);
            return;
        }

        if (expectedOutputs != null) {
            java.util.Arrays.sort(expectedOutputs);
        } else {
            System.out.println("No expected outputs found in " + args[2]);
            return;
        }

        if (executables == null) {
            System.out.println("No executables found in " + args[0]);
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
