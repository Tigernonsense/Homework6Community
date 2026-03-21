"""
PIPS Puzzle Visualizer Module

For the PIPS contest--
Take a puzzle (.txt) file and draw a colored grid with the requirements
and all the dominoes at the bottom.
"""

import tkinter as tk

# Grid constants
GRID_BLOCKED    = -2
GRID_EMPTY      = -1

# UI constants
CANVAS_WIDTH    = 800
CANVAS_HEIGHT   = 500
CELL_SIZE       = 50
FONT            = ("Arial", 16)
DOMINO_WIDTH    = CELL_SIZE
DOMINO_HEIGHT   = DOMINO_WIDTH / 2
DOMINO_MARGIN   = DOMINO_WIDTH / 4
DOMINO_XSEP     = 10
DOMINO_YSEP     = 50
DOM_LINE_SIZE   = 10
DOM_XOFFSET     = 20

class Grid:
    def __init__(self, width, height):
        self.width = width
        self.height = height
        self.grid_size = width * height
        self.grid = [ GRID_BLOCKED for _ in range(self.grid_size) ]
    
    def get(self, idx):
        return self.grid[idx]
    
    def get(self, row, col):
        return self.grid[row * self.width + col]
    
    def set(self, idx, val):
        self.grid[idx] = val

    def set(self, row, col, val):
        self.grid[row * self.width + col] = val

    def print_grid(self):
        for row in range(self.height):
            for col in range(self.width):
                print(self.get(row, col), end=" ")
            print("")

class Requirement:
    def __init__(self, requirement_str, id):
        self.req_str = requirement_str
        self.id = id

class Domino:
    def __init__(self, left, right):
        self.left = left
        self.right = right

def read_puzzle_file(file_path):
    with open(file_path, "r") as f:
        lines = f.readlines()

    dominoes = []
    requirements = []

    i = 0
    while i < len(lines):
        line = lines[i].strip()

        if line.startswith("Grid"):
            # Parse grid dimensions
            i += 1
            dimensions = lines[i].strip().split(sep="x")
            grid = Grid(int(dimensions[1]), int(dimensions[0]))
            i += 1

            # Line by line for actual grid data
            row = 0
            while i < len(lines) and lines[i].strip() != "":
                vals = lines[i].strip().split(" ")
                for col in range(grid.width):
                    val = GRID_BLOCKED
                    if vals[col] == ".":
                        val = GRID_BLOCKED
                    elif vals[col] == "*":
                        val = GRID_EMPTY
                    else:
                        val = int(vals[col])
                    grid.set(row, col, val)
                row += 1
                i += 1

        elif line.startswith("Dominoes"):
            # Parse dominoes
            i += 1
            while i < len(lines) and lines[i].strip() != "":
                dom_val = lines[i].strip().strip("|").split("|")
                dominoes.append(Domino(dom_val[0], dom_val[1]))
                i += 1

        elif line.startswith("Requirements"):
            # Parse requirements
            i += 1
            req_id = 0
            while i < len(lines) and lines[i].strip() != "":
                parts = lines[i].strip().split(":")
                if len(parts) == 2:
                    requirements.append(Requirement(parts[1].strip(), req_id))
                    req_id += 1
                i += 1

        else:
            i += 1

    #grid.print_grid()
    return grid, dominoes, requirements

def visualize_puzzle(grid, dominoes, requirements):
    def get_color(index):
        """Generate a color based on the index."""
        colors = ["#FF556E", "#59D6FF", "#90EE90", "#FFD700", "#FF9267", "#9370DB"]
        return colors[index % len(colors)]

    # Create the Tkinter window and canvas
    root = tk.Tk()
    root.title("PIPS Puzzle Visualizer")
    root.resizable(False, False)
    canvas = tk.Canvas(root, width=CANVAS_WIDTH, height=CANVAS_HEIGHT, bg="white")
    canvas.pack()

    # Draw the grid
    for r in range(grid.height):
        for c in range(grid.width):
            cell_value = grid.get(r, c)
            if cell_value >= 0:
                color = get_color(int(cell_value))
                canvas.create_rectangle(
                    c * CELL_SIZE, r * CELL_SIZE,
                    (c + 1) * CELL_SIZE, (r + 1) * CELL_SIZE,
                    fill=color, outline="black"
                )
                canvas.create_text(
                    (c + 0.5) * CELL_SIZE, (r + 0.5) * CELL_SIZE,
                    text=requirements[cell_value].req_str, font=FONT
                )
            elif cell_value == GRID_EMPTY:
                canvas.create_rectangle(
                    c * CELL_SIZE, r * CELL_SIZE,
                    (c + 1) * CELL_SIZE, (r + 1) * CELL_SIZE,
                    fill="#FFE79F", outline="black"
                )

    # Draw dominoes at the bottom
    domino_start_y = grid.height * CELL_SIZE + 20
    for i, domino in enumerate(dominoes):
        x1 = (i % DOM_LINE_SIZE) * (CELL_SIZE + DOMINO_XSEP) + DOM_XOFFSET
        y1 = domino_start_y + (DOMINO_YSEP * (i // DOM_LINE_SIZE))
        x2 = x1 + DOMINO_WIDTH
        y2 = y1 + DOMINO_HEIGHT

        canvas.create_rectangle(x1, y1, x2, y2, fill="white", outline="black")
        canvas.create_text(x1 + DOMINO_MARGIN, (y1 + y2) / 2, text=str(domino.left), font=FONT)
        canvas.create_text(x2 - DOMINO_MARGIN, (y1 + y2) / 2, text=str(domino.right), font=FONT)

    root.mainloop()

# Test cases for this module.
if __name__ == "__main__":
    import os

    puzzles_dir = "./puzzles/"

    for entry in os.scandir(puzzles_dir):
        if entry.is_file():
            grid, dominoes, requirements = read_puzzle_file(puzzles_dir + entry.name)
            visualize_puzzle(grid, dominoes, requirements)
