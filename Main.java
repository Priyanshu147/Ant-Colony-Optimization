import java.util.Arrays;


public class Main {
    /**
     * The main method of the program.
     * @param args The command-line arguments (not used in this program).
     */
    public static void main(String[] args) {

        // Constants for controlling the algorithm parameters and input file
        // 1 for brute-force, 2 for ant-colony-optimization
        int chosenMethod = 2;
        // 1 for printing shortest path, 2 for printing pheromones
        int whichPrint = 2;

        final int ITERATION_COUNT = 100;
        final int ANT_PER_ITERATION = 50;
        final double DEGRADATION_FACTOR = 0.8;
        final double ALPHA = 1.1;
        final double BETA = 1.6;
        final double INITIAL_PHEROMONE_INTENSITY = 0.01;
        final double Q_VALUE = 0.0001;

        final String FILE_NAME = "input01.txt";

        // Record the starting time of the algorithm
        long startingTime = System.currentTimeMillis();

        // Read nodes from the input file and create the distance matrix
        Node.readNodesFromFile(FILE_NAME);
        Node.createDistanceMatrix();

        // Choose the method for solving the TSP and execute it
        if (chosenMethod == 1) {
            bruteForce();
        } else if (chosenMethod == 2) {
            Pheromone.fullIteration(ITERATION_COUNT, ANT_PER_ITERATION, DEGRADATION_FACTOR,
            ALPHA, BETA, INITIAL_PHEROMONE_INTENSITY, Q_VALUE, whichPrint);
        }

        // Record the finishing time of the algorithm
        long finishingTime = System.currentTimeMillis();
        // Print the shortest path found and the time taken by the algorithm
        printShortestPath(chosenMethod, finishingTime - startingTime);
    }

    /**
     * Prints information about the shortest path found and the time taken by the algorithm.
     * @param chosenMethod The chosen method for solving the TSP (1 for Brute-Force, 2 for Ant Colony Optimization).
     * @param time The time taken by the algorithm in milliseconds.
     */
    public static void printShortestPath(int chosenMethod, long time) {
        if (chosenMethod == 1) {
            System.out.println("Method: Brute-Force Method");
        } else if (chosenMethod == 2) {
            System.out.println("Method: Ant Colony Optimization Method");
        }

        System.out.printf("Shortest Distance: %.5f\n", Node.getShortestDistance());

        System.out.println("Shortest Path: " + pathRedesign(Node.getShortestPath()));

        System.out.printf("Time it takes to find the shortest path: %.2f seconds.", time / 1000.0);
    }

 
    private static String pathRedesign(Integer[] shortestPath) {
        int startingIndex = 0;
        for (int i = 0; i < Node.getNumberOfNodes(); i++) {
            if (shortestPath[i] == 0) {
                startingIndex = i;
                break;
            }
        }

        Integer[] redesignedArray = new Integer[Node.getNumberOfNodes() + 1];
        for (int i = startingIndex; i < Node.getNumberOfNodes() + startingIndex; i++) {
            redesignedArray[i - startingIndex] = shortestPath[i % Node.getNumberOfNodes()] + 1;
        }
        redesignedArray[Node.getNumberOfNodes()] = 1;
        return Arrays.toString(redesignedArray);
    }

    /**
     * Solves the TSP using the Brute-Force Method.
     */
    public static void bruteForce() {
        // Generate the default order of nodes
        Integer[] defaultOrder = new Integer[Node.getNumberOfNodes() - 1];
        for (int i = 0; i < Node.getNumberOfNodes() - 1; i++) {
            defaultOrder[i] = i + 1;
        }

        // Permute the default order to find the shortest path
        permute(defaultOrder, 0);

        // Redesign and set the shortest path to start and end at node 1
        Integer[] shortestPath = new Integer[Node.getNumberOfNodes()];
        System.arraycopy(Node.getShortestPath(), 0, shortestPath, 1, Node.getShortestPath().length);
        shortestPath[0] = 0;
        Node.setShortestPath(shortestPath);

        // Set up the canvas for visualization and draw the shortest path
        Node.canvasSetup();
        Node.drawLines();
        Node.drawPoints();
        StdDraw.show();
    }

    /**
     * Recursively permutes the given array to find all possible orders of nodes.
     * @param arr The array of node indices to permute.
     * @param k The current index of the array being permuted.
     */
    private static void permute(Integer[] arr, int k) {
        if (k == Node.getNumberOfNodes() - 1) {
            // Calculate the distance of the current permutation and update the shortest path if necessary
            double distance = 0.0;
            for (int i = 0; i < arr.length - 1; i++) {
                distance += Node.getDistanceMatrix()[arr[i]][arr[i + 1]];
            }
            distance += Node.getDistanceMatrix()[0][arr[0]];
            distance += Node.getDistanceMatrix()[0][arr[arr.length - 1]];

            if (distance < Node.getShortestDistance()) {
                Node.setShortestDistance(distance);
                Node.setShortestPath(arr.clone());
            }
        } else {
            // Swap elements at index k with each element at indices >= k
            for (int i = k; i < arr.length; i++) {
                Integer temp = arr[i];
                arr[i] = arr[k];
                arr[k] = temp;
                permute(arr, k + 1);
                arr[k] = arr[i];
                arr[i] = temp;
            }
        }
    }
}
