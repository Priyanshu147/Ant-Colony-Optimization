# Ant Colony Optimization — Traveling Salesman Problem (TSP)

A Java implementation of the **Ant Colony Optimization (ACO)** algorithm to solve the **Traveling Salesman Problem (TSP)**, with an optional **Brute-Force** method for small instances. Results are visualized using `StdDraw`.

---

## Table of Contents

- [Overview](#overview)
- [Project Structure](#project-structure)
- [Algorithm](#algorithm)
  - [Ant Colony Optimization](#ant-colony-optimization)
  - [Brute-Force](#brute-force)
- [Parameters](#parameters)
- [Input Format](#input-format)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Compile](#compile)
  - [Run](#run)
- [Configuration](#configuration)
- [Visualization](#visualization)
- [Example Output](#example-output)

---

## Overview

The **Traveling Salesman Problem** asks: given a set of cities with known coordinates, what is the shortest possible route that visits each city exactly once and returns to the starting city?

This project offers two approaches:

| Method | Description | Best For |
|--------|-------------|----------|
| **Ant Colony Optimization** | Probabilistic, bio-inspired metaheuristic | Large instances |
| **Brute-Force** | Exhaustive permutation search | Small instances (≤ 11 nodes) |

---

## Project Structure

```
Ant-Colony-Optimization/
├── Main.java          # Entry point; controls method selection and parameters
├── Node.java          # Node representation, distance matrix, and visualization helpers
├── Pheromone.java     # ACO logic: pheromone update, traversal, and path selection
├── StdDraw.java       # Graphics library for 2D visualization (Princeton StdLib)
├── input01.txt        # Sample input — 10 nodes
├── input02.txt        # Sample input — 11 nodes
├── input03.txt        # Sample input — 12 nodes
├── input04.txt        # Sample input — 13 nodes
└── input05.txt        # Sample input — 29 nodes
```

---

## Algorithm

### Ant Colony Optimization

ACO is inspired by the foraging behavior of ants. Ants deposit **pheromones** on paths they travel; shorter, better paths accumulate more pheromone over time and become increasingly attractive to future ants.

The probability that ant *k* moves from node *i* to node *j* is:

```
P(i → j) = [τ(i,j)^α / d(i,j)^β] / Σ [τ(i,k)^α / d(i,k)^β]
```

Where:
- `τ(i,j)` — pheromone intensity on edge (i, j)
- `d(i,j)` — Euclidean distance between nodes i and j
- `α` — controls pheromone influence
- `β` — controls distance influence

After each iteration, pheromone trails evaporate (degrade) and are reinforced by successful ants:

```
τ(i,j) ← τ(i,j) × ρ  (evaporation)
τ(i,j) ← τ(i,j) + ΔQ / L  (reinforcement, where L is tour length)
```

### Brute-Force

Generates all permutations of nodes and evaluates every possible tour to find the globally optimal solution. Practical only for very small inputs due to O(n!) time complexity.

---

## Parameters

All parameters are defined as constants at the top of `Main.java`:

| Parameter | Default | Description |
|-----------|---------|-------------|
| `chosenMethod` | `2` | `1` = Brute-Force, `2` = Ant Colony Optimization |
| `whichPrint` | `2` | `1` = draw shortest path, `2` = draw pheromone trails |
| `ITERATION_COUNT` | `100` | Number of ACO iterations |
| `ANT_PER_ITERATION` | `50` | Number of ants deployed per iteration |
| `DEGRADATION_FACTOR` | `0.8` | Pheromone evaporation rate (ρ); range (0, 1) |
| `ALPHA` | `1.1` | Pheromone importance (α) |
| `BETA` | `1.6` | Distance importance (β) |
| `INITIAL_PHEROMONE_INTENSITY` | `0.01` | Starting pheromone value on all edges |
| `Q_VALUE` | `0.0001` | Pheromone deposit constant (Q) |
| `FILE_NAME` | `"input01.txt"` | Input file to load |

---

## Input Format

Each input file contains one city per line in **comma-separated** `x,y` coordinate format. Coordinates should be normalized to the range `[0.0, 1.0]`.

```
0.4575,0.6875
0.6413,0.7188
0.6400,0.5850
...
```

To use your own data, create a `.txt` file in the same format and update `FILE_NAME` in `Main.java`.

---

## Getting Started

### Prerequisites

- **Java JDK 8** or higher
- No external libraries required (`StdDraw.java` is included)

### Compile

From the project root directory:

```bash
javac *.java
```

### Run

```bash
java Main
```

---

## Configuration

To change the method or parameters, edit the constants near the top of `Main.java`:

```java
int chosenMethod = 2;          // 1 = Brute-Force, 2 = ACO
int whichPrint   = 2;          // 1 = shortest path, 2 = pheromone trails

final int    ITERATION_COUNT             = 100;
final int    ANT_PER_ITERATION           = 50;
final double DEGRADATION_FACTOR          = 0.8;
final double ALPHA                       = 1.1;
final double BETA                        = 1.6;
final double INITIAL_PHEROMONE_INTENSITY = 0.01;
final double Q_VALUE                     = 0.0001;

final String FILE_NAME = "input01.txt";
```

---

## Visualization

A `StdDraw` window opens after the algorithm finishes, displaying one of two views depending on `whichPrint`:

| `whichPrint` | Display |
|---|---|
| `1` | The shortest tour found, drawn as connected lines between nodes |
| `2` | Pheromone intensity map — thicker lines indicate stronger trails |

Nodes are drawn as circles. The **starting node** (node 1) is highlighted in orange; all other nodes are light grey.

---

## Example Output

```
Method: Ant Colony Optimization Method
Shortest Distance: 2.81543
Shortest Path: [1, 4, 8, 10, 9, 7, 3, 6, 5, 2, 11, 1]
Time it takes to find the shortest path: 0.23 seconds.
```
