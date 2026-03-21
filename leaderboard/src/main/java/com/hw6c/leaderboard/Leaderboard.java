package com.hw6c.leaderboard;

import java.io.IOException;
import java.io.File;
import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

class SubmissionResults {
    String submissionName;
    long totalTime; // Total time taken across all puzzles.
    int correctPuzzles; // Count of correctly solved puzzles.
    long totalMemoryUsed; // Sum of peak memory per puzzle.

    // Formatted string which gets displayed on the leaderboard.
    String displayString;

    public SubmissionResults(String submissionName) {
        this.submissionName = submissionName;
        this.totalTime = 0;
        this.correctPuzzles = 0;
        this.totalMemoryUsed = 0;
    }

    public void addPuzzleResult(long timeTaken, boolean isCorrect, long memoryUsed) {
        totalTime += timeTaken;
        if (isCorrect) {
            correctPuzzles++;
        }
        totalMemoryUsed += memoryUsed;
    }

    public void format(int totalPuzzles, boolean memEnabled) {
        // Total time (in milliseconds).
        long time = totalTime / 1_000_000;
        String fmtStr = "%d %s, %d/%d correct puzzles";

        if (memEnabled) {
            fmtStr += ", %d KB memory";
        }

        String timeUnits;
        if (time > 1000) {
            timeUnits = "seconds";
            time = time / 1000;
        } else {
            timeUnits = "milliseconds";
        }

        displayString = String.format(fmtStr, time, timeUnits, correctPuzzles, totalPuzzles, totalMemoryUsed / 1024);
    }

    public String getDisplayString() {
        return displayString;
    }
}

public class Leaderboard extends Canvas {

    private static final long serialVersionUID = -1713296820692453235L;
    
	// GUI Settings
	private final int FONT_SIZE		= 24;
    private final int ENTRY_HEIGHT  = FONT_SIZE * 2;
    private final int Y_SPACING     = ENTRY_HEIGHT + 10;
    private final int BG_PADDING    = 10;
    private final int X_SEP         = 300;

    // Colors
    private final Color BG_COL = new Color(255, 255, 255);
    private final Color GOLD    = new Color(255, 215, 0);
    private final Color SILVER  = new Color(192, 192, 192);
    private final Color BRONZE  = new Color(205, 127, 50);

    int totalPuzzles = 0;
    boolean showMemory = true;
    String title;

    ArrayList<SubmissionResults> resultsList;

    public Leaderboard() {
        resultsList = new ArrayList<>();
    }

    public Leaderboard(String title) {
        this.title = title;
        resultsList = new ArrayList<>();
    }

    /**
     * Format a submission result and add it to the leaderboard.
     * @param result
     */
    public void addSubmissionResult(SubmissionResults result) {
        result.format(totalPuzzles, showMemory);
        resultsList.add(result);
    }

    public void sortResults() {
        // Sort by total time (ascending), then by correct puzzles (descending)
        resultsList.sort((a, b) -> {
            // Correct puzzles is most important, check first
            if (a.correctPuzzles != b.correctPuzzles) {
                return Integer.compare(b.correctPuzzles, a.correctPuzzles); // More correct puzzles is better
            }
            // If correct puzzles are the same, sort by total time (less time is better)
            if (a.totalTime != b.totalTime) {
                return Long.compare(a.totalTime, b.totalTime);
            }

            // If total time is also the same, sort by memory usage (less is better)
            if (a.totalMemoryUsed != b.totalMemoryUsed) {
                return Long.compare(a.totalMemoryUsed, b.totalMemoryUsed);
            }

            // Finally, if everything is the same, sort alphabetically by submission name
            return a.submissionName.compareTo(b.submissionName);
        });
    }

    @Override
    public void paint(Graphics g) {
        g.setFont(new Font("Arial", Font.PLAIN, 24));

        // Draw the results on the canvas
        int rank = 1;
        int yOffset = 50;
        int xOffset = 50;

        // Draw the title at the top
		g.setColor(Color.BLACK);
        g.drawString(title, xOffset, yOffset);
        yOffset += Y_SPACING; // Move down for the next entry

        // Use an alternating color background for each entry
        for (SubmissionResults result : resultsList) {
            // Reset xOffset for each entry
            xOffset = 50;

            switch (rank) {
                case 1 -> g.setColor(GOLD); // Gold for 1st place
                case 2 -> g.setColor(SILVER); // Silver for 2nd place
                case 3 -> g.setColor(BRONZE); // Bronze for 3rd place
                default -> {
                    g.setColor(BG_COL);
                }
            }

            // Rounded rectangle background for the entry
            g.fillRoundRect(xOffset - BG_PADDING, yOffset - (BG_PADDING * 2), getWidth() - xOffset, ENTRY_HEIGHT, 20, 20);

            g.setColor(Color.BLACK); // Text color

            // First, draw the name.
            String rankText = String.format("%d. %s", rank, result.submissionName);
            g.drawString(rankText, xOffset, yOffset + (Y_SPACING / 4));

            // Next, draw the time, correctness, memory usage to the right.
            xOffset += X_SEP;
            g.drawString(result.getDisplayString(), xOffset, yOffset + (Y_SPACING / 4));
            yOffset += Y_SPACING; // Move down for the next entry
            rank++;
        }
    }
	
    /**
     * Create a string from the contents of a file.
     * @param f The file to read.
     * @return The contents of the file as a string.
     */
	public String readStringFromFile(File f) {
		try {
			byte[] bytes = Files.readAllBytes(f.toPath());
			String content;
			if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) {
				content = new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
			} else if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF) {
				content = new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
			} else {
				content = new String(bytes, StandardCharsets.UTF_8);
			}
			return content.replace("\r\n", "\n").replace("\r", "\n");
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

    /**
     * Compare two outputs for equivalence (regardless of order or whitespace).
     * @param out1
     * @param out2
     * @return true if the outputs are equivalent, false otherwise.
     */
	public boolean areOutputsEquivalent(String out1, String out2) {
		Set<String> s1 = new HashSet<String>();
		Set<String> s2 = new HashSet<String>();
		
		StringBuilder sb = new StringBuilder();
		BufferedReader br1 = new BufferedReader(new StringReader(out1));
		BufferedReader br2 = new BufferedReader(new StringReader(out2));
		
		String line;
		
		try {
			while ((line = br1.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.charAt(0) == '-') {
					if (sb.length() == 0) {
						continue;
					}
					
					s1.add(sb.toString());
					sb.setLength(0);
					continue;
				}
				
				sb.append(line);
				sb.append("\n");
			}
			
			sb.setLength(0);
			while ((line = br2.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.charAt(0) == '-') {
					if (sb.length() == 0) {
						continue;
					}
					
					s2.add(sb.toString());
					sb.setLength(0);
					continue;
				}
				
				sb.append(line);
				sb.append("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return s1.equals(s2);
	}

    public void setTotalPuzzles(int totalPuzzles) {
        this.totalPuzzles = totalPuzzles;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setShowMemory(boolean showMemory) {
        this.showMemory = showMemory;
    }
}