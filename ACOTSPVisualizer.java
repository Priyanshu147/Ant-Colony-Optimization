import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 *  Ant Colony Optimization — Traveling Salesman Problem (TSP) Visualizer
 *  Algorithms : ACO | Nearest Neighbor | 2-Opt | Random Tour | ACO+2-Opt
 *  Compile    : javac ACOTSPVisualizer.java
 *  Run        : java  ACOTSPVisualizer
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
public class ACOTSPVisualizer extends JFrame {

    // ─── Canvas / Layout ─────────────────────────────────────────────────────
    private static final int CW = 800, CH = 700, PW = 320;

    // ─── Color Palette (dark sci-fi theme) ───────────────────────────────────
    private static final Color C_BG      = new Color(8,  14, 26);
    private static final Color C_GRID    = new Color(22, 35, 58);
    private static final Color C_CITY    = new Color(0,  210, 255);
    private static final Color C_CITY_B  = new Color(255, 255, 255);
    private static final Color C_BEST    = new Color(0,  255, 130);
    private static final Color C_CURR    = new Color(90, 150, 255, 120);
    private static final Color C_PHER    = new Color(255, 170, 0);
    private static final Color C_ANT     = new Color(255, 70,  70);
    private static final Color C_PANEL   = new Color(14, 22, 40);
    private static final Color C_ACCENT  = new Color(0,  160, 255);
    private static final Color C_TEXT    = new Color(190, 205, 230);
    private static final Color C_DIM     = new Color(80, 105, 145);
    private static final Color C_SEC     = new Color(0,  130, 200);
    private static final Color C_GREEN   = new Color(0,  200, 100);
    private static final Color C_RED     = new Color(200, 50, 50);
    private static final Color C_PURPLE  = new Color(130, 60, 200);

    // ─── Volatile parameters (shared safely between EDT & algo thread) ────────
    private volatile int    numCities   = 25;
    private volatile int    numAnts     = 40;
    private volatile double alpha       = 1.0;   // pheromone exponent
    private volatile double beta        = 3.0;   // heuristic exponent
    private volatile double rho         = 0.50;  // evaporation rate
    private volatile double Q           = 100.0; // pheromone deposit intensity
    private volatile double initPher    = 1.0;   // initial pheromone level
    private volatile int    maxIter     = 300;   // max iterations
    private volatile int    animDelay   = 20;    // ms between animation frames

    // ─── Shared simulation state ──────────────────────────────────────────────
    private volatile double[][]  cityPos;   // normalized [0,1] x/y per city
    private volatile double[][]  dist;      // Euclidean distance matrix
    private volatile double[][]  pher;      // pheromone matrix
    private volatile int[]       bestTour;
    private volatile double      bestDist   = Double.MAX_VALUE;
    private volatile int         iteration  = 0;
    private volatile boolean     running    = false;
    private volatile String      algoName   = "ACO";

    // Ant animation state
    private volatile int[][]     antTours;  // current ant tour arrays
    private volatile int[]       antStep;   // how many steps each ant has taken

    // ─── Convergence history (for mini-chart) ────────────────────────────────
    private final List<Double> convergenceHistory = Collections.synchronizedList(new ArrayList<>());

    // ─── UI refs ──────────────────────────────────────────────────────────────
    private CanvasPanel   canvas;
    private JLabel        lbAlgo, lbIter, lbBest, lbAnts, lbTime;
    private JButton       btnStart, btnStop, btnGen, btnReset;
    private JProgressBar  progressBar;
    private Thread        algoThread;
    private long          startTime;
    private JComboBox<String> algoCombo;

    // ═════════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new ACOTSPVisualizer().setVisible(true);
        });
    }

    public ACOTSPVisualizer() {
        super("🐜  Ant Colony Optimization — Traveling Salesman Problem");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(C_BG);

        canvas = new CanvasPanel();
        canvas.setPreferredSize(new Dimension(CW, CH));

        add(canvas,            BorderLayout.CENTER);
        add(buildSidePanel(),  BorderLayout.EAST);
        add(buildStatusBar(),  BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setResizable(false);

        generateCities(numCities);

        // Repaint timer
        new javax.swing.Timer(30, e -> canvas.repaint()).start();

        // Stats update timer
        new javax.swing.Timer(200, e -> refreshStats()).start();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CITY & MATRIX SETUP
    // ══════════════════════════════════════════════════════════════════════════
    private synchronized void generateCities(int n) {
        Random rng = new Random();
        double[][] pos = new double[n][2];
        double margin = 0.07;
        for (int i = 0; i < n; i++) {
            pos[i][0] = margin + rng.nextDouble() * (1 - 2 * margin);
            pos[i][1] = margin + rng.nextDouble() * (1 - 2 * margin);
        }
        cityPos = pos;
        recomputeDistances();
        resetState();
    }

    private void recomputeDistances() {
        int n = cityPos.length;
        double[][] d = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                double dx = cityPos[i][0] - cityPos[j][0];
                double dy = cityPos[i][1] - cityPos[j][1];
                d[i][j] = Math.sqrt(dx * dx + dy * dy);
            }
        dist = d;
    }

    private void resetState() {
        int n = cityPos.length;
        double[][] p = new double[n][n];
        for (double[] row : p) Arrays.fill(row, initPher);
        pher      = p;
        bestTour  = null;
        bestDist  = Double.MAX_VALUE;
        iteration = 0;
        antTours  = null;
        antStep   = null;
        convergenceHistory.clear();
        progressBar.setValue(0);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ALGORITHM DISPATCH
    // ══════════════════════════════════════════════════════════════════════════
    private void startAlgorithm() {
        if (running) return;
        resetState();
        running   = true;
        startTime = System.currentTimeMillis();
        setButtons(false);

        algoThread = new Thread(() -> {
            try {
                switch (algoName) {
                    case "ACO"             -> runACO(false);
                    case "Nearest Neighbor"-> runNearestNeighbor();
                    case "2-Opt"           -> runTwoOpt(greedyTour());
                    case "Random Tour"     -> runRandomTour();
                    case "ACO + 2-Opt"     -> { runACO(true);
                                                if (running && bestTour != null)
                                                    runTwoOpt(bestTour.clone()); }
                }
            } finally {
                running = false;
                SwingUtilities.invokeLater(() -> setButtons(true));
            }
        }, "AlgoThread");
        algoThread.setDaemon(true);
        algoThread.start();
    }

    private void stopAlgo() {
        running = false;
        if (algoThread != null) algoThread.interrupt();
        setButtons(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ACO ALGORITHM
    // ══════════════════════════════════════════════════════════════════════════
    private void runACO(boolean quietMode) {
        int n       = cityPos.length;
        int ants    = numAnts;
        Random rng  = new Random();

        antTours = new int[ants][n];
        antStep  = new int[ants];

        for (int iter = 0; iter < maxIter && running; iter++) {
            iteration = iter + 1;
            int pct = (int)(100.0 * (iter + 1) / maxIter);
            SwingUtilities.invokeLater(() -> progressBar.setValue(pct));

            // ── Initialize all ants ──
            for (int k = 0; k < ants; k++) {
                Arrays.fill(antTours[k], -1);
                antTours[k][0] = rng.nextInt(n);
                antStep[k] = 1;
            }

            // ── Build tours step by step (animated) ──
            for (int step = 1; step < n && running; step++) {
                for (int k = 0; k < ants; k++) {
                    if (antTours[k][step - 1] < 0) continue;
                    antTours[k][step] = selectNextCity(antTours[k], step, rng);
                    antStep[k] = step + 1;
                }
                if (!quietMode) {
                    try { Thread.sleep(Math.max(1, animDelay / n)); }
                    catch (InterruptedException e) { return; }
                }
            }

            // ── Evaporate ──
            evaporatePheromones();

            // ── Deposit & track best ──
            double iterBest = Double.MAX_VALUE;
            for (int k = 0; k < ants; k++) {
                double d = tourLength(antTours[k]);
                depositPheromone(antTours[k], d);
                if (d < bestDist) {
                    bestDist = d;
                    bestTour = antTours[k].clone();
                }
                if (d < iterBest) iterBest = d;
            }
            convergenceHistory.add(bestDist);

            try { Thread.sleep(animDelay); }
            catch (InterruptedException e) { return; }
        }
        SwingUtilities.invokeLater(() -> progressBar.setValue(100));
    }

    private int selectNextCity(int[] tour, int step, Random rng) {
        int n       = cityPos.length;
        int current = tour[step - 1];

        boolean[] visited = new boolean[n];
        for (int i = 0; i < step; i++) visited[tour[i]] = true;

        double[] prob  = new double[n];
        double   total = 0;

        for (int j = 0; j < n; j++) {
            if (!visited[j] && dist[current][j] > 1e-9) {
                double ph = Math.pow(pher[current][j], alpha);
                double hn = Math.pow(1.0 / dist[current][j], beta);
                prob[j]   = ph * hn;
                total    += prob[j];
            }
        }

        if (total < 1e-12) {
            for (int j = 0; j < n; j++) if (!visited[j]) return j;
            return 0;
        }

        double r = rng.nextDouble() * total;
        double cumul = 0;
        for (int j = 0; j < n; j++) {
            if (!visited[j]) {
                cumul += prob[j];
                if (cumul >= r) return j;
            }
        }
        for (int j = 0; j < n; j++) if (!visited[j]) return j;
        return 0;
    }

    private synchronized void evaporatePheromones() {
        double[][] p = pher;
        int n = p.length;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                p[i][j] = Math.max(0.001, p[i][j] * (1.0 - rho));
    }

    private synchronized void depositPheromone(int[] tour, double length) {
        if (length < 1e-9) return;
        double deposit = Q / length;
        int n = tour.length;
        for (int i = 0; i < n; i++) {
            int a = tour[i], b = tour[(i + 1) % n];
            pher[a][b] += deposit;
            pher[b][a] += deposit;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  NEAREST NEIGHBOR
    // ══════════════════════════════════════════════════════════════════════════
    private void runNearestNeighbor() {
        int n = cityPos.length;
        bestDist = Double.MAX_VALUE;
        for (int s = 0; s < n && running; s++) {
            int[] tour = nearestNeighborFrom(s);
            double d   = tourLength(tour);
            iteration  = s + 1;
            convergenceHistory.add(bestDist < Double.MAX_VALUE ? bestDist : d);
            SwingUtilities.invokeLater(() -> progressBar.setValue(
                (int)(100.0 * iteration / n)));
            if (d < bestDist) { bestDist = d; bestTour = tour; }
            try { Thread.sleep(animDelay * 3L); } catch (InterruptedException e) { return; }
        }
        SwingUtilities.invokeLater(() -> progressBar.setValue(100));
    }

    private int[] nearestNeighborFrom(int start) {
        int n = cityPos.length;
        boolean[] visited = new boolean[n];
        int[] tour = new int[n];
        tour[0] = start;
        visited[start] = true;
        for (int step = 1; step < n; step++) {
            int    cur = tour[step - 1];
            double min = Double.MAX_VALUE;
            int    nx  = -1;
            for (int j = 0; j < n; j++) {
                if (!visited[j] && dist[cur][j] < min) { min = dist[cur][j]; nx = j; }
            }
            tour[step] = nx;
            visited[nx] = true;
        }
        return tour;
    }

    private int[] greedyTour() { return nearestNeighborFrom(0); }

    // ══════════════════════════════════════════════════════════════════════════
    //  2-OPT LOCAL SEARCH
    // ══════════════════════════════════════════════════════════════════════════
    private void runTwoOpt(int[] init) {
        int n = cityPos.length;
        int[] tour = (init != null) ? init.clone() : greedyTour();
        bestTour   = tour.clone();
        bestDist   = tourLength(tour);
        convergenceHistory.add(bestDist);

        boolean improved = true;
        int pass = 0;
        while (improved && running) {
            improved = false;
            pass++;
            iteration = pass;
            for (int i = 1; i < n - 1 && running; i++) {
                for (int k = i + 1; k < n && running; k++) {
                    if (twoOptDelta(tour, i, k) > 1e-10) {
                        reverse(tour, i, k);
                        improved = true;
                        double d = tourLength(tour);
                        if (d < bestDist) {
                            bestDist = d;
                            bestTour = tour.clone();
                            convergenceHistory.add(bestDist);
                        }
                    }
                }
            }
            int passCopy = pass;
            SwingUtilities.invokeLater(() -> progressBar.setValue(
                Math.min(99, passCopy * 8)));
            try { Thread.sleep(animDelay); } catch (InterruptedException e) { return; }
        }
        SwingUtilities.invokeLater(() -> progressBar.setValue(100));
    }

    private double twoOptDelta(int[] tour, int i, int k) {
        int n = tour.length;
        int a = tour[i - 1], b = tour[i];
        int c = tour[k],     d = tour[(k + 1) % n];
        return (dist[a][b] + dist[c][d]) - (dist[a][c] + dist[b][d]);
    }

    private void reverse(int[] tour, int i, int k) {
        while (i < k) { int t = tour[i]; tour[i] = tour[k]; tour[k] = t; i++; k--; }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  RANDOM TOUR (baseline)
    // ══════════════════════════════════════════════════════════════════════════
    private void runRandomTour() {
        int n = cityPos.length;
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        Random rng = new Random();
        bestDist = Double.MAX_VALUE;

        for (int iter = 0; iter < maxIter && running; iter++) {
            iteration = iter + 1;
            Collections.shuffle(Arrays.asList(idx), rng);
            int[] tour = new int[n];
            for (int i = 0; i < n; i++) tour[i] = idx[i];
            double d = tourLength(tour);
            if (d < bestDist) { bestDist = d; bestTour = tour.clone(); }
            convergenceHistory.add(bestDist);
            SwingUtilities.invokeLater(() -> progressBar.setValue(
                (int)(100.0 * iteration / maxIter)));
            try { Thread.sleep(animDelay); } catch (InterruptedException e) { return; }
        }
        SwingUtilities.invokeLater(() -> progressBar.setValue(100));
    }

    // ── Utility ───────────────────────────────────────────────────────────────
    private double tourLength(int[] tour) {
        if (tour == null) return Double.MAX_VALUE;
        double d = 0;
        int n = tour.length;
        for (int i = 0; i < n; i++) {
            if (tour[i] < 0 || tour[(i + 1) % n] < 0) continue;
            d += dist[tour[i]][tour[(i + 1) % n]];
        }
        return d;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CANVAS PANEL — all drawing logic
    // ══════════════════════════════════════════════════════════════════════════
    class CanvasPanel extends JPanel {
        CanvasPanel() {
            setBackground(C_BG);
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,    RenderingHints.VALUE_STROKE_PURE);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            drawGrid(g2);

            double[][] cp = cityPos;
            if (cp == null) { g2.dispose(); return; }

            drawPheromoneTrails(g2, cp);
            drawBestTour(g2, cp);
            if (running) drawAntPaths(g2, cp);
            drawCities(g2, cp);
            drawHUD(g2);
            drawConvergenceChart(g2);

            g2.dispose();
        }

        // ── Dot-grid background ───────────────────────────────────────────────
        private void drawGrid(Graphics2D g2) {
            int w = getWidth(), h = getHeight();
            g2.setColor(C_GRID);
            for (int x = 0; x < w; x += 40)
                for (int y = 0; y < h; y += 40)
                    g2.fillOval(x - 1, y - 1, 2, 2);
        }

        // ── Pheromone trails ─────────────────────────────────────────────────
        private void drawPheromoneTrails(Graphics2D g2, double[][] cp) {
            double[][] p = pher;
            if (p == null) return;
            int n = cp.length;
            double mx = 0;
            for (double[] row : p) for (double v : row) if (v > mx) mx = v;
            if (mx < 1e-9) return;

            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    double lvl = p[i][j] / mx;
                    if (lvl < 0.04) continue;
                    float a = (float) Math.min(0.88, lvl * 0.95);
                    float w = (float) (0.4 + lvl * 5.5);
                    g2.setColor(new Color(C_PHER.getRed(), C_PHER.getGreen(),
                                         C_PHER.getBlue(), (int)(a * 255)));
                    g2.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(cx(cp[i][0]), cy(cp[i][1]),
                                cx(cp[j][0]), cy(cp[j][1]));
                }
            }
        }

        // ── Best tour ─────────────────────────────────────────────────────────
        private void drawBestTour(Graphics2D g2, double[][] cp) {
            int[] bt = bestTour;
            if (bt == null) return;
            int n = bt.length;
            // Shadow
            g2.setColor(new Color(0, 255, 130, 40));
            g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < n; i++) {
                int a = bt[i], b = bt[(i + 1) % n];
                if (a < 0 || b < 0 || a >= cp.length || b >= cp.length) continue;
                g2.drawLine(cx(cp[a][0]), cy(cp[a][1]), cx(cp[b][0]), cy(cp[b][1]));
            }
            // Line
            g2.setColor(C_BEST);
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < n; i++) {
                int a = bt[i], b = bt[(i + 1) % n];
                if (a < 0 || b < 0 || a >= cp.length || b >= cp.length) continue;
                g2.drawLine(cx(cp[a][0]), cy(cp[a][1]), cx(cp[b][0]), cy(cp[b][1]));
            }
        }

        // ── Ant paths (animation) ─────────────────────────────────────────────
        private void drawAntPaths(Graphics2D g2, double[][] cp) {
            int[][] at = antTours;
            int[]   as = antStep;
            if (at == null || as == null) return;

            for (int k = 0; k < at.length; k++) {
                int steps = Math.min(as[k], at[k].length);
                if (steps < 2) continue;
                g2.setColor(C_CURR);
                g2.setStroke(new BasicStroke(1f));
                for (int s = 1; s < steps; s++) {
                    int a = at[k][s - 1], b = at[k][s];
                    if (a < 0 || b < 0 || a >= cp.length || b >= cp.length) continue;
                    g2.drawLine(cx(cp[a][0]), cy(cp[a][1]),
                                cx(cp[b][0]), cy(cp[b][1]));
                }
                // Ant head (red dot at current position)
                int head = at[k][steps - 1];
                if (head >= 0 && head < cp.length) {
                    g2.setColor(C_ANT);
                    int hx = cx(cp[head][0]), hy = cy(cp[head][1]);
                    g2.fillOval(hx - 4, hy - 4, 8, 8);
                }
            }
        }

        // ── Cities ────────────────────────────────────────────────────────────
        private void drawCities(Graphics2D g2, double[][] cp) {
            int n = cp.length;
            for (int i = 0; i < n; i++) {
                int x = cx(cp[i][0]), y = cy(cp[i][1]);
                // Glow ring
                g2.setColor(new Color(0, 210, 255, 35));
                g2.fillOval(x - 12, y - 12, 24, 24);
                // Body
                g2.setColor(C_CITY);
                g2.fillOval(x - 6, y - 6, 12, 12);
                // Border
                g2.setStroke(new BasicStroke(1.5f));
                g2.setColor(C_CITY_B);
                g2.drawOval(x - 6, y - 6, 12, 12);
                // Label
                g2.setFont(new Font("Monospaced", Font.PLAIN, 9));
                g2.setColor(C_DIM);
                g2.drawString(String.valueOf(i), x + 8, y - 4);
            }
        }

        // ── HUD overlay ───────────────────────────────────────────────────────
        private void drawHUD(Graphics2D g2) {
            int bx = 12, by = 12, bw = 230, bh = 100;
            // Background
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRoundRect(bx, by, bw, bh, 14, 14);
            g2.setColor(new Color(0, 120, 200, 80));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(bx, by, bw, bh, 14, 14);

            // Algorithm name
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.setColor(C_ACCENT);
            g2.drawString("▶  " + algoName, bx + 12, by + 22);

            g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
            g2.setColor(C_TEXT);

            String dStr = bestDist < Double.MAX_VALUE
                ? String.format("%.4f", bestDist) : "—";
            g2.drawString("Iteration  :  " + iteration, bx + 12, by + 42);
            g2.drawString("Best Dist  :  " + dStr,      bx + 12, by + 58);

            int cities = cityPos != null ? cityPos.length : 0;
            g2.drawString("Cities     :  " + cities + "  |  Ants : " + numAnts,
                          bx + 12, by + 74);

            if (running) {
                long elapsed = System.currentTimeMillis() - startTime;
                g2.setColor(C_GREEN);
                g2.drawString("Time       :  " + elapsed + " ms", bx + 12, by + 90);
            } else if (bestTour != null) {
                g2.setColor(new Color(0, 220, 100));
                g2.drawString("✓ Complete", bx + 12, by + 90);
            }
        }

        // ── Mini convergence chart ─────────────────────────────────────────────
        private void drawConvergenceChart(Graphics2D g2) {
            List<Double> hist;
            synchronized (convergenceHistory) {
                if (convergenceHistory.size() < 2) return;
                hist = new ArrayList<>(convergenceHistory);
            }

            int cw = 200, ch2 = 80;
            int ox = getWidth() - cw - 14, oy = 12;

            // Background
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRoundRect(ox, oy, cw, ch2 + 24, 12, 12);
            g2.setColor(new Color(0, 120, 200, 80));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(ox, oy, cw, ch2 + 24, 12, 12);

            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.setColor(C_ACCENT);
            g2.drawString("CONVERGENCE", ox + 10, oy + 14);

            double minV = Double.MAX_VALUE, maxV = 0;
            for (double v : hist) { if (v < minV) minV = v; if (v > maxV) maxV = v; }
            double range = maxV - minV;
            if (range < 1e-9) range = 1;

            int chartX = ox + 8, chartY = oy + 20, chartW = cw - 16, chartH = ch2;

            // Axes
            g2.setColor(C_GRID);
            g2.setStroke(new BasicStroke(0.5f));
            g2.drawLine(chartX, chartY, chartX, chartY + chartH);
            g2.drawLine(chartX, chartY + chartH, chartX + chartW, chartY + chartH);

            // Plot
            g2.setColor(C_ACCENT);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int sz = hist.size();
            int prevX = -1, prevY = -1;
            for (int i = 0; i < sz; i++) {
                int px = chartX + (int)((double) i / (sz - 1) * chartW);
                int py = chartY + chartH - (int)((hist.get(i) - minV) / range * chartH);
                if (prevX >= 0) g2.drawLine(prevX, prevY, px, py);
                prevX = px; prevY = py;
            }

            // Best label
            g2.setFont(new Font("Monospaced", Font.BOLD, 9));
            g2.setColor(C_GREEN);
            g2.drawString(String.format("%.4f", minV), chartX + 2, chartY + chartH + 14);
            g2.setColor(C_DIM);
            g2.drawString(String.format("%.4f", maxV), chartX + chartW - 48, chartY + 10);
        }

        private int cx(double nx) { return (int)(nx * getWidth()); }
        private int cy(double ny) { return (int)(ny * getHeight()); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SIDE PANEL — controls, sliders, buttons
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel buildSidePanel() {
        JPanel p = new JPanel();
        p.setBackground(C_PANEL);
        p.setPreferredSize(new Dimension(PW, CH));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(10, 12, 10, 12));

        // Title
        JLabel title = new JLabel("ACO — TSP VISUALIZER");
        title.setFont(new Font("Monospaced", Font.BOLD, 13));
        title.setForeground(C_ACCENT);
        title.setAlignmentX(LEFT_ALIGNMENT);
        title.setBorder(new EmptyBorder(0, 0, 8, 0));
        p.add(title);

        // ── Algorithm selector ────────────────────────────────────────────────
        p.add(sec("⚙  ALGORITHM"));
        String[] algos = { "ACO", "Nearest Neighbor", "2-Opt", "Random Tour", "ACO + 2-Opt" };
        algoCombo = new JComboBox<>(algos);
        styleCombo(algoCombo);
        algoCombo.addActionListener(e -> algoName = (String) algoCombo.getSelectedItem());
        p.add(algoCombo);
        p.add(vgap(6));

        // ── ACO parameters ────────────────────────────────────────────────────
        p.add(sec("🐜  ACO PARAMETERS"));
        addParam(p, "Alpha (α)  — Pheromone Weight",  1, 50,  (int)(alpha * 10),
            v -> alpha = v / 10.0,   v -> fmt1(v / 10.0));
        addParam(p, "Beta (β)   — Heuristic Weight",  1, 80,  (int)(beta * 10),
            v -> beta  = v / 10.0,   v -> fmt1(v / 10.0));
        addParam(p, "Rho (ρ)   — Evaporation Rate",   1, 99,  (int)(rho * 100),
            v -> rho   = v / 100.0,  v -> fmt2(v / 100.0));
        addParam(p, "Q  — Pheromone Intensity",        1, 500, (int) Q,
            v -> Q     = v,          v -> String.valueOf(v));
        addParam(p, "Initial Pheromone Level",         1, 100, (int)(initPher * 10),
            v -> initPher = v / 10.0, v -> fmt1(v / 10.0));
        p.add(vgap(4));

        // ── Simulation settings ───────────────────────────────────────────────
        p.add(sec("🔢  SIMULATION"));
        addParam(p, "Number of Ants",        2,  200, numAnts,   v -> numAnts   = v, String::valueOf);
        addParam(p, "Max Iterations",       10,  800, maxIter,   v -> maxIter   = v, String::valueOf);
        addParam(p, "Number of Cities",      5,   80, numCities, v -> numCities = v, String::valueOf);
        addParam(p, "Anim Speed (ms delay)", 1,  200, animDelay, v -> animDelay = v, String::valueOf);
        p.add(vgap(8));

        // ── Buttons ───────────────────────────────────────────────────────────
        p.add(sec("▶  CONTROLS"));
        JPanel bGrid = new JPanel(new GridLayout(2, 2, 6, 6));
        bGrid.setBackground(C_PANEL);
        bGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        bGrid.setAlignmentX(LEFT_ALIGNMENT);

        btnGen   = btn("🗺  New Map",  new Color(40, 65, 110),  e -> { stopAlgo(); generateCities(numCities); });
        btnStart = btn("▶  Start",    new Color(20, 110, 65),  e -> startAlgorithm());
        btnStop  = btn("⏹  Stop",     new Color(130, 30, 30),  e -> stopAlgo());
        btnReset = btn("↺  Reset",    new Color(70, 45, 115),  e -> { stopAlgo(); generateCities(numCities); });

        bGrid.add(btnGen); bGrid.add(btnStart);
        bGrid.add(btnStop); bGrid.add(btnReset);
        p.add(bGrid);
        p.add(vgap(8));

        // ── Progress bar ──────────────────────────────────────────────────────
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Idle");
        progressBar.setBackground(new Color(22, 34, 58));
        progressBar.setForeground(C_ACCENT);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        progressBar.setAlignmentX(LEFT_ALIGNMENT);
        p.add(progressBar);
        p.add(vgap(10));

        // ── Legend ────────────────────────────────────────────────────────────
        p.add(buildLegend());
        p.add(Box.createVerticalGlue());

        // ── Info box ──────────────────────────────────────────────────────────
        p.add(buildInfoBox());

        setButtons(true);
        return p;
    }

    private JPanel buildLegend() {
        JPanel p = new JPanel();
        p.setBackground(new Color(10, 16, 30));
        p.setLayout(new GridLayout(4, 1, 1, 1));
        p.setBorder(new CompoundBorder(
            new LineBorder(new Color(30, 50, 90), 1),
            new EmptyBorder(6, 8, 6, 8)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.add(legRow(C_PHER,  "Pheromone trails (width = intensity)"));
        p.add(legRow(C_BEST,  "Best tour found"));
        p.add(legRow(C_CURR,  "Current ant paths"));
        p.add(legRow(C_ANT,   "Ant head positions"));
        return p;
    }

    private JPanel legRow(Color c, String txt) {
        JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        r.setBackground(new Color(10, 16, 30));
        JLabel dot = new JLabel("●"); dot.setForeground(c);
        dot.setFont(new Font("Monospaced", Font.BOLD, 13));
        JLabel lb = new JLabel(txt); lb.setForeground(C_DIM);
        lb.setFont(new Font("SansSerif", Font.PLAIN, 10));
        r.add(dot); r.add(lb);
        return r;
    }

    private JPanel buildInfoBox() {
        JPanel p = new JPanel();
        p.setBackground(new Color(8, 14, 28));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new CompoundBorder(
            new LineBorder(new Color(25, 45, 80), 1),
            new EmptyBorder(6, 8, 6, 8)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        p.setAlignmentX(LEFT_ALIGNMENT);
        String[] tips = {
            "α  — Higher: more pheromone-driven",
            "β  — Higher: more distance-driven",
            "ρ  — Higher: faster evaporation",
            "Q  — Higher: stronger deposits"
        };
        for (String t : tips) {
            JLabel l = new JLabel(t);
            l.setForeground(new Color(70, 100, 140));
            l.setFont(new Font("Monospaced", Font.PLAIN, 9));
            p.add(l);
        }
        return p;
    }

    // ── Status bar ────────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 4));
        p.setBackground(new Color(5, 9, 18));
        p.setBorder(new MatteBorder(1, 0, 0, 0, new Color(20, 35, 65)));
        lbAlgo = stat("Algorithm: ACO");
        lbIter  = stat("Iter: 0");
        lbBest  = stat("Best: —");
        lbAnts  = stat("Ants: " + numAnts);
        lbTime  = stat("Time: —");
        p.add(lbAlgo); p.add(lbIter); p.add(lbBest); p.add(lbAnts); p.add(lbTime);
        return p;
    }

    private void refreshStats() {
        lbAlgo.setText("Algorithm: " + algoName);
        lbIter.setText("Iter: " + iteration);
        lbBest.setText("Best: " + (bestDist < Double.MAX_VALUE
            ? String.format("%.4f", bestDist) : "—"));
        lbAnts.setText("Ants: " + numAnts + " | Cities: " + numCities);
        if (running)
            lbTime.setText("Time: " + (System.currentTimeMillis() - startTime) + "ms");
        else
            lbTime.setText(bestTour != null ? "✓ Done" : "Idle");

        progressBar.setString(running
            ? (algoName + "  " + progressBar.getValue() + "%")
            : (bestTour != null ? "Complete" : "Idle"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UI HELPERS
    // ══════════════════════════════════════════════════════════════════════════
    private void addParam(JPanel parent, String label, int min, int max, int init,
                          IntConsumer onChange, java.util.function.Function<Integer,String> fmt) {
        JPanel row = new JPanel(new BorderLayout(0, 0));
        row.setBackground(C_PANEL);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel lb = new JLabel(label);
        lb.setForeground(C_TEXT);
        lb.setFont(new Font("SansSerif", Font.PLAIN, 10));

        JLabel val = new JLabel(fmt.apply(init));
        val.setForeground(C_ACCENT);
        val.setFont(new Font("Monospaced", Font.BOLD, 11));
        val.setPreferredSize(new Dimension(52, 14));
        val.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(C_PANEL);
        top.add(lb, BorderLayout.WEST);
        top.add(val, BorderLayout.EAST);

        JSlider sl = new JSlider(min, max, init);
        sl.setBackground(C_PANEL);
        sl.setForeground(C_DIM);
        sl.setBorder(null);
        sl.addChangeListener(e -> {
            onChange.accept(sl.getValue());
            val.setText(fmt.apply(sl.getValue()));
        });

        row.add(top, BorderLayout.NORTH);
        row.add(sl,  BorderLayout.CENTER);
        parent.add(row);
        parent.add(vgap(2));
    }

    private JLabel sec(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(C_SEC);
        l.setFont(new Font("Monospaced", Font.BOLD, 11));
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(6, 0, 3, 0));
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        return l;
    }

    private JButton btn(String text, Color bg, ActionListener al) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 11));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);
        b.addActionListener(al);
        return b;
    }

    private JLabel stat(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(C_TEXT);
        l.setFont(new Font("Monospaced", Font.PLAIN, 11));
        return l;
    }

    private void styleCombo(JComboBox<?> cb) {
        cb.setBackground(new Color(22, 34, 62));
        cb.setForeground(C_TEXT);
        cb.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        cb.setAlignmentX(LEFT_ALIGNMENT);
    }

    private Component vgap(int h) {
        return Box.createRigidArea(new Dimension(0, h));
    }

    private void setButtons(boolean idle) {
        SwingUtilities.invokeLater(() -> {
            btnStart.setEnabled(idle);
            btnStop.setEnabled(!idle);
            btnGen.setEnabled(idle);
            btnReset.setEnabled(idle);
        });
    }

    private static String fmt1(double v) { return String.format("%.1f", v); }
    private static String fmt2(double v) { return String.format("%.2f", v); }
}
