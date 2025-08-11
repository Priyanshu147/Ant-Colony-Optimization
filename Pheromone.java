import java.util.Random;

/**
 * A class representing the pheromone trail for Ant Colony Optimization (ACO) in solving the Traveling Salesman Problem (TSP).
 */
public class Pheromone {
    private static double[][] pheromoneIntensities = new double[Node.getNumberOfNodes()][Node.getNumberOfNodes()];
    private static double qValue;
    private static double alpha;
    private static double beta;
    private static double degradationFactor;
    private static double initialPheromoneIntensity;

    /**
     * Degrades the pheromone intensities over time.
     */
    private static void degradePheromones() {
        for (int i = 0; i < Node.getNumberOfNodes(); i++) {
            for (int j = 0; j < Node.getNumberOfNodes(); j++) {
                pheromoneIntensities[i][j] = pheromoneIntensities[i][j] * degradationFactor;
            }
        }

    }

    /**
     * Gets the initial pheromone intensity.
     * @return The initial pheromone intensity.
     */
    public static double getInitialPheromoneIntensity() {
        return initialPheromoneIntensity;
    }

    /**
     * Runs a full iteration of the Ant Colony Optimization algorithm to find the shortest path.
     * @param iterationCount The number of iterations to perform.
     * @param antPerIteration The number of ants to use per iteration.
     * @param degradationFactor The rate at which pheromones degrade.
     * @param alpha The alpha parameter for pheromone influence.
     * @param beta The beta parameter for distance influence.
     * @param initialPheromoneIntensity The initial pheromone intensity.
     * @param qValue The Q value for pheromone update.
     * @param whichPrint To decide whether to print the shortest path or pheromones.
     */
    public static void fullIteration(int iterationCount, int antPerIteration, double degradationFactor,
                                     double alpha, double beta, double initialPheromoneIntensity, double qValue,
                                     int whichPrint) {
        Pheromone.degradationFactor = degradationFactor;
        Pheromone.alpha = alpha;
        Pheromone.beta = beta;
        Pheromone.qValue = qValue;
        Pheromone.initialPheromoneIntensity = initialPheromoneIntensity;

        // Initialize pheromone intensities
        for (int i = 0; i < Node.getNumberOfNodes(); i++) {
            for (int j = 0; j < Node.getNumberOfNodes(); j++) {
                pheromoneIntensities[i][j] = initialPheromoneIntensity;
            }
        }
        Random rand = new Random();

        // Perform iterations
        for (int i = 0; i < iterationCount; i++) {
            for (int j = 0; j < antPerIteration; j++) {
                oneTraversal(rand.nextInt(Node.getNumberOfNodes()));
                resetPreviousNodes();
            }
            degradePheromones();
        }

        // Visualize results
        Node.canvasSetup();
        if (whichPrint == 2) {
            Node.drawPheromones();
            Node.drawPoints();
        } else if (whichPrint == 1) {
            Node.drawLines();
            Node.drawPoints();
        }

        StdDraw.show();
    }

    /**
     * Resets the previous nodes for all nodes in the node list.
     */
    private static void resetPreviousNodes() {
        for (Node node: Node.getNodeList()) {
            node.setPreviousNode(null);
        }
    }

    /**
     * Decides the next node to visit based on pheromone trail and node distances.
     * @param previousNode The previous node visited.
     * @param visited An array indicating whether nodes have been visited.
     * @return The index of the next node to visit.
     */
    private static int decidePath(Node previousNode, boolean[] visited) {
        double[] edgeValues = new double[Node.getNumberOfNodes()];
        for (int i = 0; i < Node.getNumberOfNodes(); i++) {
            if (!visited[i] && i != previousNode.getNodeId()) {
                edgeValues[i] = calculateEdgeValue(previousNode, Node.getNodeList().get(i));
            }
        }

        double denominator = 0.0;
        for (int i = 0; i < Node.getNumberOfNodes(); i++) {
            denominator += edgeValues[i];
        }

        for (int i = 0; i < Node.getNumberOfNodes(); i++) {
            edgeValues[i] = edgeValues[i] / denominator;
        }

        double randomDouble = Math.random();
        double probabilitySum = 0.0;
        for (int i = 0; i < Node.getNumberOfNodes(); i++) {
            if (probabilitySum < randomDouble && probabilitySum + edgeValues[i] > randomDouble) {
                return i;
            } else {
                probabilitySum += edgeValues[i];
            }
        }
        return 0;
    }

    /**
     * Calculates the value of an edge based on pheromone trail and distance.
     * @param previousNode The node the ant is coming from.
     * @param node The node the ant is going to.
     * @return The edge value.
     */
    private static double calculateEdgeValue(Node previousNode, Node node) {
        return Math.pow(pheromoneIntensities[previousNode.getNodeId()][node.getNodeId()], alpha) /
                Math.pow(Node.getDistanceMatrix()[previousNode.getNodeId()][node.getNodeId()], beta);
    }

    /**
     * Performs a single traversal of the graph by an ant, updating pheromone trail and shortest path.
     * @param startingNode The starting node for the traversal.
     */
    private static void oneTraversal(Integer startingNode) {
        boolean[] visited = new boolean[Node.getNumberOfNodes()];
        visited[startingNode] = true;
        int numberOfVisited = 1;

        Node previousNode = Node.getNodeList().get(startingNode);
        Node nextNode = null;
        double totalDistance = 0.0;
        while (numberOfVisited < Node.getNumberOfNodes()) {
            int nextIndex = decidePath(previousNode, visited);
            nextNode = Node.getNodeList().get(nextIndex);
            visited[nextIndex] = true;
            nextNode.setPreviousNode(previousNode);
            totalDistance += Node.getDistanceMatrix()[nextNode.getNodeId()][previousNode.getNodeId()];
            previousNode = nextNode;
            numberOfVisited++;
        }
        totalDistance += Node.calculateDistance(nextNode, Node.getNodeList().get(startingNode));

        double delta = qValue / totalDistance;
        Integer[] path = backtracking(nextNode);
        updatePheromones(delta, path);

        if (totalDistance < Node.getShortestDistance()) {
            Node.setShortestDistance(totalDistance);
            Node.setShortestPath(path);
        }
    }

    /**
     * Updates pheromone intensities based on the pheromone update rule.
     * @param delta The amount of pheromone to deposit.
     * @param path The path taken by the ant.
     */
    private static void updatePheromones(double delta, Integer[] path) {
        for (int i = 0; i < path.length; i++) {
            int start = path[i];
            int destination = 0;
            if (i == path.length - 1) {
                destination = path[0];
            } else {
                destination = path[i + 1];
            }
            pheromoneIntensities[start][destination] += delta;
            pheromoneIntensities[destination][start] += delta;
        }

    }

    public static double[][] getPheromoneIntensities() {
        return pheromoneIntensities;
    }

    private static Integer[] backtracking(Node nextNode) {
        int index = Node.getNumberOfNodes() - 1;
        Integer[] orderList = new Integer[Node.getNumberOfNodes()];
        while (nextNode != null) {
            orderList[index] = nextNode.getNodeId();
            index--;
            nextNode = nextNode.getPreviousNode();
        }

        return orderList;
    }
}
