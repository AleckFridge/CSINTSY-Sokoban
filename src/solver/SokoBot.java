/*Implemented by : Roj Friginal, Angelo Quinones, Kharl Lim, Marquus Azevedo
 * Final Update: October 23 11:00AM
 * 
 */


package solver;

import java.util.*;

public class SokoBot {

    private class Node implements Comparable<Node> {
        int playerX, playerY;
        char[][] itemsData;
        String path;
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
        

        int[] dX = { -1, 1, 0, 0 };  // Up, Down, Left, Right
        int[] dY = { 0, 0, -1, 1 };
        char[] moves = { 'u', 'd', 'l', 'r' };

        PriorityQueue<Node> openList = new PriorityQueue<>();
        Set<String> closedList = new HashSet<>();

        // Find initial position of the player
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

        // Add initial state to priority queue
        openList.add(new Node(startX, startY, itemsData, "", 0, heuristic(mapData, itemsData, width, height)));

        while (!openList.isEmpty()) {
            // Time limit check
            

            Node current = openList.poll();

            // If all crates are on targets, the puzzle is solved
            if (isSolved(mapData, current.itemsData, width, height)) {
                return current.path;
            }

            String stateKey = generateStateKey(current.itemsData, current.playerX, current.playerY);
            if (closedList.contains(stateKey)) continue;  // Skip visited states
            closedList.add(stateKey);

            // Explore all four directions
            for (int i = 0; i < 4; i++) {
                int newX = current.playerX + dX[i];
                int newY = current.playerY + dY[i];

                // Check if the move is valid
                if (isValidMove(current.playerX, current.playerY, newX, newY, current.itemsData, mapData, width, height)) {
                    char[][] newItemsData = cloneItemsData(current.itemsData);
                    movePlayer(current.playerX, current.playerY, newX, newY, newItemsData);

                    // Skip deadlock states
                    if (isDeadlock(newItemsData, mapData, width, height)) continue;

                    // Add the new state to the open list
                    Node newNode = new Node(newX, newY, newItemsData, current.path + moves[i], current.cost + 1,
                            heuristic(mapData, newItemsData, width, height));

                    openList.add(newNode);
                }
            }
        }

        return "No solution found";
    }

    // Heuristic using Manhattan distance to targets and clustering
    private int heuristic(char[][] mapData, char[][] itemsData, int width, int height) {
        int heuristicValue = 0;
        List<int[]> cratePositions = new ArrayList<>();
        List<int[]> targetPositions = new ArrayList<>();

        // Identify crate and target positions
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (itemsData[i][j] == '$') {
                    cratePositions.add(new int[]{i, j});
                }
                if (mapData[i][j] == '.') {
                    targetPositions.add(new int[]{i, j});
                }
            }
        }

        // Assign crates to nearest unoccupied targets (greedy strategy)
        boolean[] assigned = new boolean[targetPositions.size()];
        for (int[] crate : cratePositions) {
            int minDistance = Integer.MAX_VALUE;
            int nearestTarget = -1;
            for (int k = 0; k < targetPositions.size(); k++) {
                if (!assigned[k]) {
                    int distance = Math.abs(crate[0] - targetPositions.get(k)[0]) + Math.abs(crate[1] - targetPositions.get(k)[1]);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestTarget = k;
                    }
                }
            }
            if (nearestTarget != -1) {
                heuristicValue += minDistance;
                assigned[nearestTarget] = true;
            }
        }
        return heuristicValue;
    }

    // Detects if a crate is in a deadlock position
    private boolean isDeadlock(char[][] itemsData, char[][] mapData, int width, int height) {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (itemsData[i][j] == '$') {
                    if (isStuckInCorner(i, j, mapData, width, height) && mapData[i][j] != '.') {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Checks if a crate is stuck in a corner
    private boolean isStuckInCorner(int x, int y, char[][] mapData, int width, int height) {
        boolean isHorizontalBlocked = (x > 0 && mapData[x - 1][y] == '#') || (x < height - 1 && mapData[x + 1][y] == '#');
        boolean isVerticalBlocked = (y > 0 && mapData[x][y - 1] == '#') || (y < width - 1 && mapData[x][y + 1] == '#');
        return isHorizontalBlocked && isVerticalBlocked;
    }

    // Checks if all crates are on the targets
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

    // Validates whether the player can move to a new position
    private boolean isValidMove(int playerX, int playerY, int newX, int newY, char[][] itemsData, char[][] mapData,
            int width, int height) {
        if (newX < 0 || newY < 0 || newX >= height || newY >= width || mapData[newX][newY] == '#') {
            return false;
        }
        if (itemsData[newX][newY] == '$') {  // Crate in the new position
            int crateNewX = newX + (newX - playerX);
            int crateNewY = newY + (newY - playerY);
            if (crateNewX < 0 || crateNewY < 0 || crateNewX >= height || crateNewY >= width
                    || itemsData[crateNewX][crateNewY] != ' ' || mapData[crateNewX][crateNewY] == '#') {
                return false;
            }
        }
        return true;
    }

    // Moves the player and potentially pushes a crate
    private void movePlayer(int playerX, int playerY, int newX, int newY, char[][] itemsData) {
        itemsData[playerX][playerY] = ' ';
        if (itemsData[newX][newY] == '$') {
            int crateNewX = newX + (newX - playerX);
            int crateNewY = newY + (newY - playerY);
            itemsData[crateNewX][crateNewY] = '$';
        }
        itemsData[newX][newY] = '@';
    }

    // Clones the itemsData array
    private char[][] cloneItemsData(char[][] itemsData) {
        char[][] cloned = new char[itemsData.length][];
        for (int i = 0; i < itemsData.length; i++) {
            cloned[i] = itemsData[i].clone();
        }
        return cloned;
    }

    // Generates a unique state key for visited states
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
}
