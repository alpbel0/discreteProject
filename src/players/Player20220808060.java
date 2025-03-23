package players;

import game.*;

import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;

public class Player20220808060 extends Player {
    private final Random random;
    
    // Game state tracking
    private int moveCount = 0;
    private int totalCellsVisited = 1; // Start with 1 for the initial position
    private int boardSize;
    private boolean[][] accessibilityMatrix;
    
    // Strategy parameters
    private static final double ENDGAME_THRESHOLD = 0.5; // When 50% of cells are visited, switch to endgame strategy
    private static final int MAX_DEPTH = 8; // Maximum simulation depth for move evaluation

    public Player20220808060(Board board) {
        super(board);
        this.random = new Random();
        this.boardSize = board.getSize();
        this.accessibilityMatrix = new boolean[boardSize][boardSize];
        initializeAccessibilityMatrix();
    }
    
    /**
     * Initialize the accessibility matrix based on the current board state.
     * This helps identify which parts of the board are more accessible.
     */
    private void initializeAccessibilityMatrix() {
        // Initially, all cells are potentially accessible
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                accessibilityMatrix[i][j] = !board.isVisited(i, j);
            }
        }
    }
    
    /**
     * Update accessibility matrix after a move is made
     */
    private void updateAccessibilityMatrix(int row, int col) {
        accessibilityMatrix[row][col] = false;
        totalCellsVisited++;
    }

    @Override
    public Move nextMove() {
        List<Move> possibleMoves = board.getPossibleMoves();
        
        if (possibleMoves.isEmpty()) {
            return null;
        }
        
        moveCount++;
        
        // If only one move is available, take it
        if (possibleMoves.size() == 1) {
            Move onlyMove = possibleMoves.get(0);
            updateAccessibilityMatrix(onlyMove.getRow(), onlyMove.getCol());
            return onlyMove;
        }
        
        // Determine if we're in the endgame
        boolean isEndgame = (double) totalCellsVisited / (boardSize * boardSize) >= ENDGAME_THRESHOLD;
        
        // Use a different strategy based on game phase
        if (isEndgame) {
            return selectEndgameMove(possibleMoves);
        } else {
            return selectOptimalMove(possibleMoves);
        }
    }
    
    /**
     * Main move selection algorithm for the mid-game.
     * Uses a sophisticated evaluation function that considers:
     * - Future mobility (number of moves available after this move)
     * - Board partitioning (avoid cutting the board into isolated regions)
     * - Accessibility of different board regions
     * - Long-term consequences through simulation
     */
    private Move selectOptimalMove(List<Move> possibleMoves) {
        Move bestMove = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        
        // Create a simulation environment for each possible move
        for (Move move : possibleMoves) {
            // Create a deep copy of the board for simulation
            BoardSimulation simulation = new BoardSimulation(board);
            
            // Simulate this move
            simulation.applyMove(move);
            
            // Calculate a score for this move using multiple factors
            double score = evaluateMove(move, simulation);
            
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        
        // Update accessibility matrix for the chosen move
        updateAccessibilityMatrix(bestMove.getRow(), bestMove.getCol());
        return bestMove;
    }
    
    /**
     * Specialized move selection for the endgame.
     * In the endgame, we focus on maximizing the number of remaining moves
     * and avoiding dead-ends as much as possible.
     */
    private Move selectEndgameMove(List<Move> possibleMoves) {
        Move bestMove = null;
        int bestSurvivalDepth = -1;
        
        for (Move move : possibleMoves) {
            // Calculate how many more moves we can make after this one (survival depth)
            int survivalDepth = calculateSurvivalDepth(move, MAX_DEPTH);
            
            if (survivalDepth > bestSurvivalDepth) {
                bestSurvivalDepth = survivalDepth;
                bestMove = move;
            }
        }
        
        updateAccessibilityMatrix(bestMove.getRow(), bestMove.getCol());
        return bestMove;
    }
    
    /**
     * Calculate how many more moves we can make after a given move before the game ends.
     * This is done through recursive simulation up to maxDepth.
     */
    private int calculateSurvivalDepth(Move initialMove, int maxDepth) {
        // Create a board simulation
        BoardSimulation simulation = new BoardSimulation(board);
        simulation.applyMove(initialMove);
        
        return simulateMovesRecursively(simulation, 1, maxDepth);
    }
    
    /**
     * Recursive simulation to determine maximum possible depth of moves
     */
    private int simulateMovesRecursively(BoardSimulation simulation, int currentDepth, int maxDepth) {
        // If we've reached maximum simulation depth or there are no more moves,
        // return the current depth
        List<Move> possibleNextMoves = simulation.getPossibleMoves();
        if (currentDepth >= maxDepth || possibleNextMoves.isEmpty()) {
            return currentDepth;
        }
        
        // Try each possible next move and find the one that lets us go deepest
        int maxReachableDepth = currentDepth;
        for (Move nextMove : possibleNextMoves) {
            // Create a new simulation for this branch
            BoardSimulation nextSimulation = simulation.copy();
            nextSimulation.applyMove(nextMove);
            
            // Recursively simulate from here
            int depthFromHere = simulateMovesRecursively(nextSimulation, currentDepth + 1, maxDepth);
            maxReachableDepth = Math.max(maxReachableDepth, depthFromHere);
            
            // If we've already found a path to the maximum depth, no need to search further
            if (maxReachableDepth >= maxDepth) {
                break;
            }
        }
        
        return maxReachableDepth;
    }
    
    /**
     * Comprehensive move evaluation function that considers multiple factors:
     * 1. Future Mobility: How many moves will be available after this move
     * 2. Connectivity: How well the board remains connected after this move
     * 3. Area Control: Access to different regions of the board
     * 4. Strategic Positioning: Position relative to board centers and edges
     */
    private double evaluateMove(Move move, BoardSimulation simulation) {
        // Get basic statistics about this move
        List<Move> futureMoves = simulation.getPossibleMoves();
        int futureMoveCount = futureMoves.size();
        
        // Heavily penalize moves that lead to immediate game over
        if (futureMoveCount == 0) {
            return -1000;
        }
        
        // 1. Mobility Score: More possible future moves is better
        double mobilityScore = futureMoveCount * 10;
        
        // 2. Connectivity Score: Avoid isolating regions of the board
        double connectivityScore = evaluateConnectivity(simulation) * 20;
        
        // 3. Area Control: Prefer moves that maintain access to different board quadrants
        double areaScore = evaluateAreaControl(simulation) * 30;
        
        // 4. Strategic Positioning: Evaluate position relative to board centers and edges
        double positionScore = evaluatePosition(move) * 10;
        
        // 5. Future potential: Run a short simulation from this position
        double futureScore = evaluateFuturePotential(simulation, 3) * 15;
        
        // Combine all factors with appropriate weights
        return mobilityScore + connectivityScore + areaScore + positionScore + futureScore;
    }
    
    /**
     * Evaluates how well the board remains connected after a move.
     * We want to avoid creating isolated regions that can't be accessed.
     */
    private double evaluateConnectivity(BoardSimulation simulation) {
        // Implementation of flood fill algorithm to check connectivity
        boolean[][] visited = new boolean[boardSize][boardSize];
        int playerRow = simulation.getPlayerRow();
        int playerCol = simulation.getPlayerCol();
        
        // Use BFS to find all reachable cells from the current position
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{playerRow, playerCol});
        visited[playerRow][playerCol] = true;
        
        int reachableCells = 0;
        int[][] directions = {{-1,0}, {1,0}, {0,-1}, {0,1}, {-1,-1}, {-1,1}, {1,-1}, {1,1}};
        
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            reachableCells++;
            
            // Try all 8 directions
            for (int[] dir : directions) {
                // Check all possible step sizes (1-9)
                for (int step = 1; step <= 9; step++) {
                    int newRow = cell[0] + dir[0] * step;
                    int newCol = cell[1] + dir[1] * step;
                    
                    // If this cell is within bounds, not visited, and not already deleted
                    if (isInBounds(newRow, newCol) && !visited[newRow][newCol] && 
                            !simulation.isVisited(newRow, newCol)) {
                        visited[newRow][newCol] = true;
                        queue.add(new int[]{newRow, newCol});
                    }
                }
            }
        }
        
        // Count how many cells are still not visited in the board
        int unvisitedCells = 0;
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                if (!simulation.isVisited(i, j) && !visited[i][j]) {
                    unvisitedCells++;
                }
            }
        }
        
        // If there are unreachable cells, penalize this move
        return 1.0 - ((double) unvisitedCells / (boardSize * boardSize));
    }
    
    /**
     * Evaluates how well we maintain access to different quadrants of the board.
     */
    private double evaluateAreaControl(BoardSimulation simulation) {
        // Divide the board into quadrants
        int halfSize = boardSize / 2;
        
        // Calculate accessibility to each quadrant
        double q1Access = calculateQuadrantAccessibility(simulation, 0, 0, halfSize, halfSize);
        double q2Access = calculateQuadrantAccessibility(simulation, 0, halfSize, halfSize, boardSize);
        double q3Access = calculateQuadrantAccessibility(simulation, halfSize, 0, boardSize, halfSize);
        double q4Access = calculateQuadrantAccessibility(simulation, halfSize, halfSize, boardSize, boardSize);
        
        // Return average accessibility across all quadrants
        return (q1Access + q2Access + q3Access + q4Access) / 4.0;
    }
    
    /**
     * Calculates how accessible a particular quadrant of the board is
     */
    private double calculateQuadrantAccessibility(BoardSimulation simulation, 
            int startRow, int startCol, int endRow, int endCol) {
        int totalCells = (endRow - startRow) * (endCol - startCol);
        int accessibleCells = 0;
        
        for (int i = startRow; i < endRow; i++) {
            for (int j = startCol; j < endCol; j++) {
                if (!simulation.isVisited(i, j)) {
                    accessibleCells++;
                }
            }
        }
        
        return (double) accessibleCells / totalCells;
    }
    
    /**
     * Evaluates strategic value of a position on the board.
     * In general, central positions are more valuable in the early/mid-game.
     */
    private double evaluatePosition(Move move) {
        int row = move.getRow();
        int col = move.getCol();
        
        // Calculate distance from center (Manhattan distance)
        int centerRow = boardSize / 2;
        int centerCol = boardSize / 2;
        int distanceFromCenter = Math.abs(row - centerRow) + Math.abs(col - centerCol);
        
        // Convert to a 0-1 scale where 1 is at center, 0 is at corner
        double normalizedDistance = 1.0 - ((double) distanceFromCenter / (boardSize + boardSize));
        
        // Early in the game, central positions are more valuable
        if (moveCount < boardSize) {
            return normalizedDistance;
        } 
        // Mid-game, slightly prefer central positions
        else if (moveCount < boardSize * 2) {
            return normalizedDistance * 0.7;
        }
        // Late game, position doesn't matter as much
        else {
            return 0.5;
        }
    }
    
    /**
     * Evaluates long-term potential by simulating a few moves ahead
     */
    private double evaluateFuturePotential(BoardSimulation initialSimulation, int depth) {
        if (depth <= 0) {
            return 0;
        }
        
        List<Move> possibleMoves = initialSimulation.getPossibleMoves();
        if (possibleMoves.isEmpty()) {
            return 0;
        }
        
        double bestFutureValue = 0;
        
        // Look at up to 3 best moves (to save computation)
        int movsToEvaluate = Math.min(possibleMoves.size(), 3);
        
        // Sort moves by immediate future move count
        possibleMoves.sort((m1, m2) -> {
            BoardSimulation sim1 = initialSimulation.copy();
            sim1.applyMove(m1);
            
            BoardSimulation sim2 = initialSimulation.copy();
            sim2.applyMove(m2);
            
            return Integer.compare(sim2.getPossibleMoves().size(), sim1.getPossibleMoves().size());
        });
        
        // Evaluate only the top few moves
        for (int i = 0; i < movsToEvaluate; i++) {
            Move nextMove = possibleMoves.get(i);
            BoardSimulation nextSimulation = initialSimulation.copy();
            nextSimulation.applyMove(nextMove);
            
            // Calculate immediate value of this move
            int futureMoveCount = nextSimulation.getPossibleMoves().size();
            double immediateValue = futureMoveCount * 0.1;
            
            // Recursive evaluation with decreased depth
            double futureValue = evaluateFuturePotential(nextSimulation, depth - 1) * 0.7;
            
            // Combine immediate and future value
            double totalValue = immediateValue + futureValue;
            
            bestFutureValue = Math.max(bestFutureValue, totalValue);
        }
        
        return bestFutureValue;
    }
    
    /**
     * Utility method to check if a cell is within board bounds
     */
    private boolean isInBounds(int row, int col) {
        return row >= 0 && row < boardSize && col >= 0 && col < boardSize;
    }
    
    /**
     * Internal class to represent a simulation of the board state.
     * This allows us to efficiently simulate moves without modifying the actual board.
     */
    private class BoardSimulation {
        private int size;
        private int[][] grid;
        private boolean[][] visited;
        private int playerRow;
        private int playerCol;
        
        public BoardSimulation(Board originalBoard) {
            this.size = originalBoard.getSize();
            this.grid = new int[size][size];
            this.visited = new boolean[size][size];
            this.playerRow = originalBoard.getPlayerRow();
            this.playerCol = originalBoard.getPlayerCol();
            
            // Copy the grid state
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    grid[i][j] = originalBoard.getValueAt(i, j);
                    visited[i][j] = originalBoard.isVisited(i, j);
                }
            }
        }
        
        // Create a deep copy of this simulation for branching simulations
        public BoardSimulation copy() {
            BoardSimulation copy = new BoardSimulation(size, grid, visited, playerRow, playerCol);
            return copy;
        }
        
        // Constructor for creating a copy
        private BoardSimulation(int size, int[][] originalGrid, boolean[][] originalVisited, int playerRow, int playerCol) {
            this.size = size;
            this.grid = new int[size][size];
            this.visited = new boolean[size][size];
            this.playerRow = playerRow;
            this.playerCol = playerCol;
            
            // Deep copy arrays
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    this.grid[i][j] = originalGrid[i][j];
                    this.visited[i][j] = originalVisited[i][j];
                }
            }
        }
        
        public List<Move> getPossibleMoves() {
            List<Move> moves = new ArrayList<>();
            int[][] directions = {
                {-1, 0}, {1, 0}, {0, -1}, {0, 1},  // N, S, W, E
                {-1, -1}, {-1, 1}, {1, -1}, {1, 1} // NW, NE, SW, SE
            };
            
            for (int[] dir : directions) {
                int firstRow = playerRow + dir[0];
                int firstCol = playerCol + dir[1];
                
                // Check if the first cell in this direction is valid
                if (!isInBounds(firstRow, firstCol) || visited[firstRow][firstCol]) {
                    continue;
                }
                
                int stepSize = grid[firstRow][firstCol]; // Get step size from first cell
                int targetRow = playerRow + (dir[0] * stepSize);
                int targetCol = playerCol + (dir[1] * stepSize);
                
                // Ensure the target cell is in bounds and not visited
                if (!isInBounds(targetRow, targetCol) || visited[targetRow][targetCol]) {
                    continue;
                }
                
                // Check if all intermediate cells are available
                if (isPathClear(playerRow, playerCol, targetRow, targetCol, dir[0], dir[1])) {
                    moves.add(new Move(targetRow, targetCol));
                }
            }
            
            return moves;
        }
        
        // Apply a move to the simulation
        public void applyMove(Move move) {
            int targetRow = move.getRow();
            int targetCol = move.getCol();
            
            // Calculate direction
            int rowDir = Integer.signum(targetRow - playerRow);
            int colDir = Integer.signum(targetCol - playerCol);
            
            // Mark all cells along the path as visited
            int tempRow = playerRow;
            int tempCol = playerCol;
            
            while (tempRow != targetRow || tempCol != targetCol) {
                tempRow += rowDir;
                tempCol += colDir;
                
                if (isInBounds(tempRow, tempCol)) {
                    visited[tempRow][tempCol] = true;
                    grid[tempRow][tempCol] = 0;
                }
            }
            
            // Update player position
            playerRow = targetRow;
            playerCol = targetCol;
        }
        
        // Helper method to check if path is clear
        private boolean isPathClear(int startRow, int startCol, int targetRow, int targetCol, int rowStep, int colStep) {
            int row = startRow + rowStep;
            int col = startCol + colStep;
            
            while (row != targetRow || col != targetCol) {
                if (!isInBounds(row, col) || visited[row][col]) {
                    return false;
                }
                row += rowStep;
                col += colStep;
            }
            
            return true;
        }
        
        // Utility methods
        public int getPlayerRow() { return playerRow; }
        public int getPlayerCol() { return playerCol; }
        public boolean isVisited(int row, int col) { return visited[row][col]; }
        public int getSize() { return size; }
    }
}