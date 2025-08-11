import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * A class representing a node in the TSP graph.
 */
public class Node {
    private static ArrayList<Node> nodeList = new ArrayList<>();
    private static double[][] distanceMatrix;
    private static int numberOfNodes = 0;
    private int nodeId;
    private double x;
    private double y;
    private Node previousNode = null;
    private static Integer[] shortestPath;
    private static double shortestDistance = Float.MAX_VALUE;

    /**
     * Constructs a new Node with given coordinates.
     * @param x The x-coordinate of the node.
     * @param y The y-coordinate of the node.
     */
    Node(double x, double y) {
        this.x = x;
        this.y = y;
        nodeId = numberOfNodes;
        numberOfNodes++;
    }

    /**
     * Reads nodes from a file and adds them to the node list.
     * @param fileName The name of the file containing node coordinates.
     */
    public static void readNodesFromFile(String fileName) {
        try {
            File file = new File(fileName);
            Scanner input = new Scanner(file);

            while (input.hasNextLine()) {
                String[] line = input.nextLine().split(",");
                Node node = new Node(Double.parseDouble(line[0]), Double.parseDouble(line[1]));
                nodeList.add(node);
            }
        } catch (FileNotFoundException ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Creates the distance matrix based on node coordinates.
     */
    public static void createDistanceMatrix() {
        distanceMatrix = new double[numberOfNodes][numberOfNodes];

        for (int i = 0; i < numberOfNodes; i++) {
            for (int j = 0; j < numberOfNodes; j++) {
                if (i != j) {
                    double distance = calculateDistance(nodeList.get(i), nodeList.get(j));
                    distanceMatrix[i][j] = distance;
                    distanceMatrix[j][i] = distance;
                } else {
                    distanceMatrix[i][j] = 0.0;
                    distanceMatrix[j][i] = 0.0;
                }
            }
        }
    }

    /**
     * Calculates the Euclidean distance between two nodes.
     * @param node1 The first node.
     * @param node2 The second node.
     * @return The Euclidean distance between the two nodes.
     */
    public static double calculateDistance(Node node1, Node node2) {
        return Math.sqrt(Math.pow(node1.x - node2.x, 2) + Math.pow(node1.y - node2.y, 2));
    }

    /**
     * Sets up the canvas for visualization.
     */
    public static void canvasSetup() {
        final int CANVAS_WIDTH = 600;
        final int CANVAS_HEIGHT = 600;

        StdDraw.setCanvasSize(CANVAS_WIDTH, CANVAS_HEIGHT);
        StdDraw.setXscale(0, 1.0);
        StdDraw.setYscale(0, 1.0);
        StdDraw.enableDoubleBuffering();
    }

    /**
     * Draws the pheromone trails on the canvas.
     */
    public static void drawPheromones() {
        Node node1;
        Node node2;
        StdDraw.setPenColor(StdDraw.BLACK);

        for (int i = 0; i < numberOfNodes - 1; i++) {
            for (int j = i + 1; j < numberOfNodes; j++) {
                node1 = nodeList.get(i);
                node2 = nodeList.get(j);
                StdDraw.setPenRadius(Pheromone.getPheromoneIntensities()[i][j] * 3);
                StdDraw.line(node1.x, node1.y, node2.x, node2.y);

            }
        }
    }

    /**
     * Draws the lines representing the shortest path on the canvas.
     */
    public static void drawLines() {
        Node node1;
        Node node2;
        StdDraw.setPenColor(StdDraw.BLACK);
        StdDraw.setPenRadius(0.01);

        for (int i = 0; i < numberOfNodes; i++) {
            if (i == numberOfNodes - 1) {
                node1 = nodeList.get(shortestPath[i]);
                node2 = nodeList.get(shortestPath[0]);
            } else {
                node1 = nodeList.get(shortestPath[i]);
                node2 = nodeList.get(shortestPath[i + 1]);
            }
            StdDraw.line(node1.x, node1.y, node2.x, node2.y);
        }
    }

    /**
     * Draws the points representing the nodes on the canvas.
     */
    public static void drawPoints() {
        Node node;
        for (int i = 0; i < numberOfNodes; i++) {
            node = nodeList.get(i);
            if (i == 0) {
                StdDraw.setPenColor(StdDraw.PRINCETON_ORANGE);
            } else {
                StdDraw.setPenColor(StdDraw.LIGHT_GRAY);
            }
            StdDraw.filledCircle(node.x, node.y, 0.02);
            StdDraw.setPenColor(StdDraw.BLACK);
            StdDraw.text(node.x, node.y, String.valueOf(node.getNodeId() + 1));
        }
    }

    /**
     * Gets the list of nodes.
     * @return The list of nodes.
     */
    public static ArrayList<Node> getNodeList() {
        return nodeList;
    }

    /**
     * Gets the distance matrix.
     * @return The distance matrix.
     */
    public static double[][] getDistanceMatrix() {
        return distanceMatrix;
    }

    /**
     * Gets the number of nodes.
     * @return The number of nodes.
     */
    public static int getNumberOfNodes() {
        return numberOfNodes;
    }

    /**
     * Gets the node ID.
     * @return The node ID.
     */
    public int getNodeId() {
        return nodeId;
    }

    /**
     * Gets the x-coordinate of the node.
     * @return The x-coordinate.
     */
    public double getX() {
        return x;
    }

    /**
     * Gets the y-coordinate of the node.
     * @return The y-coordinate.
     */
    public double getY() {
        return y;
    }

    /**
     * Gets the previous node.
     * @return The previous node.
     */
    public Node getPreviousNode() {
        return previousNode;
    }

    /**
     * Sets the distance matrix.
     * @param distanceMatrix The distance matrix to set.
     */
    public static void setDistanceMatrix(double[][] distanceMatrix) {
        Node.distanceMatrix = distanceMatrix;
    }

    /**
     * Sets the previous node.
     * @param previousNode The previous node to set.
     */
    public void setPreviousNode(Node previousNode) {
        this.previousNode = previousNode;
    }

    /**
     * Gets the shortest path.
     * @return The shortest path.
     */
    public static Integer[] getShortestPath() {
        return shortestPath;
    }

    /**
     * Gets the shortest distance.
     * @return The shortest distance.
     */
    public static double getShortestDistance() {
        return shortestDistance;
    }

    /**
     * Sets the shortest path.
     * @param shortestPath The shortest path to set.
     */
    public static void setShortestPath(Integer[] shortestPath) {
        Node.shortestPath = shortestPath;
    }

    /**
     * Sets the shortest distance.
     * @param shortestDistance The shortest distance to set.
     */
    public static void setShortestDistance(double shortestDistance) {
        Node.shortestDistance = shortestDistance;
    }
}
