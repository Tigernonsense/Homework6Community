package com.rotor.leaderboard;

import java.io.IOException;
import java.util.Arrays;
import java.io.BufferedReader;
import java.lang.StringBuilder;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

public class ProcessTimer {
    // Since many of these processes will be really short, frequent polls are necessary for accurate memory measurement.
    // Since this will be run in a separate thread, the overhead is fine.
    private static final long MEMORY_POLL_INTERVAL_MS = 1;

    private static long maxTimeMs = 10_000;

    private static SystemInfo sysInfo = null;
    private static OperatingSystem operatingSystem = null;

    private long startTime  = 0;
    private long endTime    = 0;
    private long memoryUsed = 0;
    private boolean useMemoryTracker = true;

    // Tracks if the last test case ran timed out, since puzzles obviously don't get credit for these.
    private boolean timedOut = false;
    
    String command;
    String[] args;    

	StringBuilder outputSB;

    public static void initOSContext() {
        sysInfo = new SystemInfo();
        operatingSystem = sysInfo.getOperatingSystem();
    }

    public ProcessTimer(String command, String[] args) throws IOException {
        this.command = command;
        this.args = args;
		
		this.outputSB = new StringBuilder();
    }

    public void start() {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(command);
        pb.command().addAll(Arrays.asList(args));
        pb.redirectErrorStream(true);
        timedOut = false;

        try {
            Process process = pb.start();
            startTime = System.nanoTime();

			Thread outputDrainer = new Thread(() -> {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
					String line;
					while ((line = reader.readLine()) != null) {
						outputSB.append(line);
						outputSB.append("\n");
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			outputDrainer.start();

            Thread memoryPoller = null;
            if (useMemoryTracker) {
                int pid = (int) process.pid();
                memoryPoller = new Thread(() -> {
                    while (process.isAlive()) {
                        long currentMemory = readResidentBytes(pid);
                        if (currentMemory > memoryUsed) {
                            memoryUsed = currentMemory;
                        }

                        try {
                            Thread.sleep(MEMORY_POLL_INTERVAL_MS);
                        } catch (InterruptedException e) {
                            // Thread interrupted, likely because the process has finished
                            break;
                        }
                    }
                });
                memoryPoller.start();
            }

            boolean finished = process.waitFor(maxTimeMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                timedOut = true;
                process.destroy();
                if (!process.waitFor(200, TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                    process.waitFor();
                }
            }

            endTime = System.nanoTime();

            if (memoryPoller != null) {
                memoryPoller.interrupt();
                memoryPoller.join();
            }

			outputDrainer.join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public long getElapsedTime() {
        return endTime - startTime;
    }
	
	public String getOutput() {
		return outputSB.toString();
	}

    public long getMemoryUsed() {
        return memoryUsed;
    }

    public boolean timedOut() {
        return timedOut;
    }

    public void setMemoryTrackerEnabled(boolean val) {
        useMemoryTracker = val;
    }

    private static long readResidentBytes(int pid) {
        if (operatingSystem == null) {
            return 0;
        }

        OSProcess osProcess = operatingSystem.getProcess(pid);
        if (osProcess == null) {
            return 0; // Process already exited.
        }
        
        return osProcess.getResidentSetSize();
    }

    public static void setMaxTimeMs(long maxTimeMs) {
        ProcessTimer.maxTimeMs = maxTimeMs;
    }
}