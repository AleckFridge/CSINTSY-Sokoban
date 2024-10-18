package solver;

import java.util.*;

public class SokoBot {

    private class Node implements Comparable<Node> {
        int playerX, playerY;
        char[][] itemsData;
        String path; // the path taken to reach this node
        int cost, heuristic;

        public Node(int playerX, int playerY, char[][] itemsData, String path, int cost, int heuristic) {
            this.playerX = playerX;
            this.playerY = playerY;
            this.itemsData = itemsData;
            this.path = path;
            this.cost = cost;
            this.heuristic = heuristic;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.cost + this.heuristic, other.cost + other.heuristic);
        }
    }

    
    public String solveSokobanPuzzle(int width, int height, char[][] mapData, char[][] itemsData) {
        int[] dX = { -1, 1, 0, 0 }; // Up, Down, Left, Right
        int[] dY = { 0, 0, -1, 1 };
        char[] moves = { 'u', 'd', 'l', 'r' };

        PriorityQueue<Node> openList = new PriorityQueue<>();
        Set<String> closedList = new HashSet<>();

        // Find the initial position of the player
        int startX = -1, startY = -1;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (itemsData[i][j] == '@') {
                    startX = i;
                    startY = j;
                    break;
                }
            }
        }

        // Initial node with starting player position and crates
        openList.add(new Node(startX, startY, itemsData, "", 0, heuristic(mapData, itemsData, width, height)));

        while (!openList.isEmpty()) {
            Node current = openList.poll();

            // Check if the puzzle is solved (all crates are on targets)
            if (isSolved(mapData, current.itemsData, width, height)) {
                return current.path;
            }

            String stateKey = generateStateKey(current.itemsData, current.playerX, current.playerY);
            if (closedList.contains(stateKey)) continue; // Skip already visited states
            closedList.add(stateKey);

            // Try moving in all directions
            for (int i = 0; i < 4; i++) {
                int newX = current.playerX + dX[i];
                int newY = current.playerY + dY[i];

                if (isValidMove(current.playerX, current.playerY, newX, newY, current.itemsData, mapData, width, height)) {
                    char[][] newItemsData = cloneItemsData(current.itemsData);
                    movePlayer(current.playerX, current.playerY, newX, newY, newItemsData);

                    if (isDeadlock(newItemsData, mapData, width, height)) {
                        continue; // Skip deadlock positions
                    }

                    Node newNode = new Node(newX, newY, newItemsData, current.path + moves[i], current.cost + 1,
                            heuristic(mapData, newItemsData, width, height));

                    openList.add(newNode);
                }
            }
        }

        return "No solution found";
    }

    // Enhanced Heuristic: Manhattan Distance + Deadlock detection
    private int heuristic(char[][] mapData, char[][] itemsData, int width, int height) {
        int heuristicValue = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (itemsData[i][j] == '$') { // crate
                    heuristicValue += findNearestTargetDistance(i, j, mapData, width, height);
                }
            }
        }
        return heuristicValue;
    }

    // Deadlock detection: checks if a crate is in a position where it can't be pushed to a target
    private boolean isDeadlock(char[][] itemsData, char[][] mapData, int width, int height) {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (itemsData[i][j] == '$') { // crate
                    // Check if the crate is stuck in a corner with no target nearby
                    if (isStuckInCorner(i, j, mapData, width, height) && mapData[i][j] != '.') {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Check if a crate is stuck in a corner
    private boolean isStuckInCorner(int x, int y, char[][] mapData, int width, int height) {
        boolean isHorizontalBlocked = (x > 0 && mapData[x - 1][y] == '#') || (x < height - 1 && mapData[x + 1][y] == '#');
        boolean isVerticalBlocked = (y > 0 && mapData[x][y - 1] == '#') || (y < width - 1 && mapData[x][y + 1] == '#');
        return isHorizontalBlocked && isVerticalBlocked;
    }

    // Check if all crates are on targets
    private boolean isSolved(char[][] mapData, char[][] itemsData, int width, int height) {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (mapData[i][j] == '.' && itemsData[i][j] != '$') {
                    return false;
                }
            }
        }
        return true;
    }

    // Check if a move is valid
    private boolean isValidMove(int playerX, int playerY, int newX, int newY, char[][] itemsData, char[][] mapData,
            int width, int height) {
        if (newX < 0 || newY < 0 || newX >= height || newY >= width || mapData[newX][newY] == '#') {
            return false;
        }
        if (itemsData[newX][newY] == '$') { // Crate in the new position
            int crateNewX = newX + (newX - playerX); // Push the crate
            int crateNewY = newY + (newY - playerY);
            if (crateNewX < 0 || crateNewY < 0 || crateNewX >= height || crateNewY >= width
                    || itemsData[crateNewX][crateNewY] != ' ' || mapData[crateNewX][crateNewY] == '#') {
                return false;
            }
        }
        return true;
    }

    // Move the player and potentially push a crate
    private void movePlayer(int playerX, int playerY, int newX, int newY, char[][] itemsData) {
        itemsData[playerX][playerY] = ' '; // Empty the old player position
        if (itemsData[newX][newY] == '$') { // Push the crate
            int crateNewX = newX + (newX - playerX);
            int crateNewY = newY + (newY - playerY);
            itemsData[crateNewX][crateNewY] = '$'; // Move the crate
        }
        itemsData[newX][newY] = '@'; // Move player to new position
    }

    // Clone the itemsData array
    private char[][] cloneItemsData(char[][] itemsData) {
        char[][] cloned = new char[itemsData.length][];
        for (int i = 0; i < itemsData.length; i++) {
            cloned[i] = itemsData[i].clone();
        }
        return cloned;
    }

    // Generate a unique state key for visited states
    private String generateStateKey(char[][] itemsData, int playerX, int playerY) {
        StringBuilder key = new StringBuilder();
        key.append(playerX).append(",").append(playerY).append(";");
        for (int i = 0; i < itemsData.length; i++) {
            for (int j = 0; j < itemsData[i].length; j++) {
                key.append(itemsData[i][j]);
            }
        }
        return key.toString();
    }

    // Find the Manhattan distance to the nearest target
    private int findNearestTargetDistance(int crateX, int crateY, char[][] mapData, int width, int height) {
        int minDistance = Integer.MAX_VALUE;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (mapData[i][j] == '.') { // Target
                    int distance = Math.abs(i - crateX) + Math.abs(j - crateY);
                    minDistance = Math.min(minDistance, distance);
                }
            }
        }
        return minDistance;
    }
}
