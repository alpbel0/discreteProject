package game;

import java.io.*;
import java.lang.reflect.Field;

public class Tester {
    private static final boolean ENABLE_SNAPSHOTS = false;

    public static void main(String[] args) {
        if (false) {
            System.err.println("Usage: java game.Tester <studentID>");
            return;
        }

        String studentID = "20220808060"; // Student ID

        double totalPercentage = 0;
        int totalTests = 0;

        // 3 farklı board boyutu için döngü (1: 10x10, 2: 25x25, 3: 50x50)
        for (int boardS = 1; boardS <= 3; boardS++) {
            // 5 farklı board numarası için döngü
            System.out.println("-------------------");
            for (int boardNum = 1; boardNum <= 5; boardNum++) {
                // Board dosya yolunu oluştur
                String boardFile = getBoardFile(boardS, boardNum);

                // Eğer dosya mevcutsa testi çalıştır
                if (boardFile != null) {
                    double boardScore = runTest(boardFile, studentID);
                    totalPercentage += boardScore;
                    totalTests++;
                }
                System.out.println();
            }
        }
        System.out.println("*");
        // Ortalama skoru hesapla
        double averagePercentage = totalPercentage / totalTests;
        System.out.println("Average Score: " + String.format("%.2f%%", averagePercentage));

        try (PrintWriter writer = new PrintWriter(new FileWriter("results/test_results.txt", true))) {
            writer.println("-------------------");
            writer.println("Student ID: " + studentID);
            writer.println("Average Score: " + String.format("%.2f%%", averagePercentage));
            writer.println("-------------------\n");
        } catch (IOException e) {
            System.err.println("Error writing test results: " + e.getMessage());
        }
        

    }

    private static String getBoardFile(int boardS, int boardNum) {
        String boardFile = "";

        // Dosya yollarını oluştur
        if (boardS == 1) {
            boardFile = "boards/board_10x10_" + boardNum + ".dat";
        } else if (boardS == 2) {
            boardFile = "boards/board_25x25_" + boardNum + ".dat";
        } else if (boardS == 3) {
            boardFile = "boards/board_50x50_" + boardNum + ".dat";
        }

        File file = new File(boardFile);
        // Dosyanın var olup olmadığını kontrol et
        if (!file.exists()) {
            System.err.println("Board file not found: " + boardFile);
            return null;
        }

        return boardFile;
    }

    private static double runTest(String boardFile, String studentID) {
        BoardData data;
        try {
            data = loadBoardData(boardFile);
        } catch (IOException e) {
            System.err.println("Error loading board data: " + e.getMessage());
            return 0;
        }

        if (data == null) {
            System.err.println("Error: Board data is empty or corrupted.");
            return 0;
        }

        Board board = new Board(data.size, data.grid, data.startRow, data.startCol);

        Player player = null;
        try {
            player = Referee.initializePlayer(studentID, board);
            if (player == null) {
                System.out.println(studentID + " 1");
                return 0;
            }
        } catch (Exception e) {
            System.err.println("Error initializing player " + studentID + ": " + e.getMessage());
            System.out.println(studentID + " 1");
            return 0;
        }

        int finalScore = Referee.playGame(player, studentID, boardFile, ENABLE_SNAPSHOTS);

        int deletedCount = 0;
        try {
            Field visitedField = board.getClass().getDeclaredField("visited");
            visitedField.setAccessible(true);
            boolean[][] visited = (boolean[][]) visitedField.get(board);

            for (int i = 0; i < visited.length; i++) {
                for (int j = 0; j < visited[0].length; j++) {
                    if (visited[i][j]) {
                        deletedCount++;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Could not access deleted squares: " + e.getMessage());
        }

        int boardSize = data.size;
        int totalCells = boardSize * boardSize;
        double percentageVisited = (double) deletedCount / totalCells * 100;
        System.out.println(boardFile);
        System.out.println("Percentage visited: " + String.format("%.2f%%", percentageVisited));

        return percentageVisited;
    }

    private static BoardData loadBoardData(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists() || file.length() == 0) {
            throw new IOException("Board file " + filename + " is missing or empty.");
        }

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            int size = Integer.parseInt(br.readLine().trim());
            String[] start = br.readLine().split("\\s+");
            int startRow = Integer.parseInt(start[0]);
            int startCol = Integer.parseInt(start[1]);
            int[][] grid = new int[size][size];

            for (int r = 0; r < size; r++) {
                String[] rowVals = br.readLine().split("\\s+");
                for (int c = 0; c < size; c++) {
                    grid[r][c] = Integer.parseInt(rowVals[c]);
                }
            }

            return new BoardData(size, grid, startRow, startCol);
        }
    }

    static class BoardData {
        int size;
        int[][] grid;
        int startRow, startCol;

        BoardData(int s, int[][] g, int r, int c) {
            size = s;
            grid = g;
            startRow = r;
            startCol = c;
        }
    }
}