import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;

public class SnakeGame extends JPanel implements ActionListener, KeyListener {
    public class Tile {
        int x;
        int y;

        Tile(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    int boardWidth;
    int boardHeight;
    int tileSize = 50;

    // snake
    Tile snakeHead;
    ArrayList<Tile> snakeBody;

    // food
    Tile food;
    Random random;

    // game logic
    int velocityX;
    int velocityY;
    Timer gameLoop;

    boolean gameOver = false;

    JButton homeButton;

    boolean[][] obstacleGrid; // Grid to track obstacles

    // Best score
    int bestScore = 0;

    // Sound Clips
    Clip eatFoodClip;
    Clip gameOverClip;
    Clip collisionClip;

    SnakeGame(int boardWidth, int boardHeight) {
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        setPreferredSize(new Dimension(this.boardWidth, this.boardHeight));
        setBackground(Color.black);
        addKeyListener(this);
        setFocusable(true);

        obstacleGrid = new boolean[boardWidth / tileSize][boardHeight / tileSize];

        snakeHead = new Tile(0, 5); // Initialize snake head with provided coordinates
        snakeBody = new ArrayList<Tile>();

        food = new Tile(10, 10);
        random = new Random();
        placeFood(); // Now placeFood() is called after snakeHead is initialized

        velocityX = 1;
        velocityY = 0;

        // game timer
        gameLoop = new Timer(130, this); // how long it takes to start timer, milliseconds gone between frames
        gameLoop.start();

        homeButton = new JButton("Home");
        homeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                returnToHomePage();
            }
        });
        add(homeButton);
        // playBackgroundMusic();

        // Load Sound Clips
        loadSoundClips();
    }

    private void returnToHomePage() {
        JFrame currentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        currentFrame.dispose();
        new App();
    }

    private void loadSoundClips() {
        try {
            // Load eat food sound
            eatFoodClip = AudioSystem.getClip();
            AudioInputStream eatFoodInputStream = AudioSystem.getAudioInputStream(new File("05Newfood.wav"));
            eatFoodClip.open(eatFoodInputStream);

            // Load game over sound
            gameOverClip = AudioSystem.getClip();
            AudioInputStream gameOverInputStream = AudioSystem.getAudioInputStream(new File("02.Gameover.wav"));
            gameOverClip.open(gameOverInputStream);

            // Load collision sound
            collisionClip = AudioSystem.getClip();
            AudioInputStream collisionInputStream = AudioSystem.getAudioInputStream(new File("01Collide.wav"));
            collisionClip.open(collisionInputStream);
        } catch (Exception ex) {
            System.out.println("Error loading sound clips: " + ex.getMessage());
        }
    }

    private void playSound(Clip clip) {
        if (clip.isRunning()) {
            clip.stop();
        }
        clip.setFramePosition(0);
        clip.start();
    }

    public void draw(Graphics g) {
        // Grid Lines
        for (int i = 0; i < boardWidth / tileSize; i++) {
            // (x1, y1, x2, y2)
            g.drawLine(i * tileSize, 0, i * tileSize, boardHeight);
            g.drawLine(0, i * tileSize, boardWidth, i * tileSize);
        }

        g.setColor(Color.gray);
        // Draw obstacles
        for (int i = 0; i < obstacleGrid.length; i++) {
            for (int j = 0; j < obstacleGrid[0].length; j++) {
                if (obstacleGrid[i][j]) {
                    g.fill3DRect(i * tileSize, j * tileSize, tileSize, tileSize, true);
                }
            }
        }
        // Food
        g.setColor(Color.red);
        g.fill3DRect(food.x * tileSize, food.y * tileSize, tileSize, tileSize, true);

        // Draw the snake body
        g.setColor(Color.green); // Set color to green for the snake
        for (int i = 0; i < snakeBody.size(); i++) {
            Tile snakePart = snakeBody.get(i);
            g.fill3DRect(snakePart.x * tileSize, snakePart.y * tileSize, tileSize, tileSize, true);
        }

        // Draw the snake head
        g.setColor(new Color(0, 194, 0));
        g.fill3DRect(snakeHead.x * tileSize, snakeHead.y * tileSize, tileSize, tileSize, true);

        if (!gameLoop.isRunning() && !gameOver) {
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.setColor(Color.red);
            FontMetrics fm = g.getFontMetrics();
            int xPaused = (boardWidth - fm.stringWidth("Game Paused")) / 2;
            int yPaused = boardHeight / 2 - 30;
            g.drawString("Game Paused", xPaused, yPaused);

            int xPressSpace = (boardWidth - fm.stringWidth("Press Space")) / 2;
            int yPressSpace = yPaused + 40;
            g.drawString("Press Space", xPressSpace, yPressSpace);
        }
        // Score
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        if (gameOver) {
            // Game over panel
            int panelWidth = 300;
            int panelHeight = 200;
            int panelX = (boardWidth - panelWidth) / 2;
            int panelY = (boardHeight - panelHeight) / 2;

            // Draw light gray panel
            g.setColor(Color.gray);
            g.fillRect(panelX, panelY, panelWidth, panelHeight);

            Font gameoverFont = new Font("Arial", Font.BOLD, 40);
            g.setFont(gameoverFont);
            g.setColor(Color.red);
            // Calculate the position to center the text
            FontMetrics fm = g.getFontMetrics();
            int xGameOver = (boardWidth - fm.stringWidth("Game Over")) / 2;
            int yGameOver = (boardHeight / 2) - 30; // Position above the center
            g.drawString("Game Over", xGameOver, yGameOver);

            // Best Score
            g.setColor(Color.green);
            String bestScoreString = "Best Score: " + bestScore;
            int xBestScore = (boardWidth - fm.stringWidth(bestScoreString)) / 2;
            int yBestScore = boardHeight / 2 + 10; // Center of the frame
            g.drawString(bestScoreString, xBestScore, yBestScore);

            // Score
            String scoreString = "Score: " + snakeBody.size();
            int xScore = (boardWidth - fm.stringWidth(scoreString)) / 2;
            int yScore = (boardHeight / 2) + 50; // Position below the center
            g.drawString(scoreString, xScore, yScore);
        } else {
            // Score
            g.setColor(Color.green);
            g.drawString("Score: " + String.valueOf(snakeBody.size()), tileSize - 26, tileSize);
            // Best Score
            g.setColor(Color.green);
            g.drawString("Best Score: " + bestScore, tileSize - 26, tileSize - 20);
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void placeFood() {
        do {
            int foodX = random.nextInt(boardWidth / tileSize);
            int foodY = random.nextInt(boardHeight / tileSize);

            // Check if the food position is not occupied by the snake or inside an obstacle
            boolean foodOccupied = false;
            for (Tile snakePart : snakeBody) {
                if (snakePart.x == foodX && snakePart.y == foodY) {
                    foodOccupied = true;
                    break;
                }
            }

            if (foodOccupied || (snakeHead.x == foodX && snakeHead.y == foodY) || obstacleGrid[foodX][foodY]) {
                // Food position is occupied, generate new position
                continue;
            }

            // Food position is valid, place the food
            food.x = foodX;
            food.y = foodY;
            break;
        } while (true);
    }

    public void move() {
        // Eat food
        if (collision(snakeHead, food)) {
            snakeBody.add(new Tile(food.x, food.y));
            placeFood();
            playSound(eatFoodClip); // Play sound when snake eats food
        }

        // Move snake body
        for (int i = snakeBody.size() - 1; i >= 0; i--) {
            Tile snakePart = snakeBody.get(i);
            if (i == 0) { // right before the head
                snakePart.x = snakeHead.x;
                snakePart.y = snakeHead.y;
            } else {
                Tile prevSnakePart = snakeBody.get(i - 1);
                snakePart.x = prevSnakePart.x;
                snakePart.y = prevSnakePart.y;
            }
        }

        // Calculate new head position
        int newHeadX = snakeHead.x + velocityX;
        int newHeadY = snakeHead.y + velocityY;

        // Check if the new head position is out of bounds
        if (newHeadX < 0)
            newHeadX = boardWidth / tileSize - 1;
        else if (newHeadX >= boardWidth / tileSize)
            newHeadX = 0;
        if (newHeadY < 0)
            newHeadY = boardHeight / tileSize - 1;
        else if (newHeadY >= boardHeight / tileSize)
            newHeadY = 0;

        // Check if the new head position is hitting an obstacle
        if (obstacleGrid[newHeadX][newHeadY]) {
            // Game over if hitting an obstacle
            gameOver = true;
            if (collision(snakeHead, food)) {
                playSound(eatFoodClip);
            } else {
                playSound(collisionClip);
            }
            return;
        }

        // Move the snake head
        snakeHead.x = newHeadX;
        snakeHead.y = newHeadY;

        // Game over conditions
        for (int i = 0; i < snakeBody.size(); i++) {
            Tile snakePart = snakeBody.get(i);

            // Collide with snake head
            if (collision(snakeHead, snakePart)) {
                gameOver = true;
                playSound(collisionClip);
            }
        }
    }

    public boolean collision(Tile tile1, Tile tile2) {
        return tile1.x == tile2.x && tile1.y == tile2.y;
    }

    @Override
    public void actionPerformed(ActionEvent e) { // called every x milliseconds by gameLoop timer
        move();
        repaint();
        if (gameOver) {
            gameLoop.stop();
            updateBestScore();
            playSound(gameOverClip);
        }
    }

    private long lastKeyPressTime = 0;
    private static final long MOVEMENT_COOLDOWN = 130;

    private boolean canMove() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastKeyPressTime) >= MOVEMENT_COOLDOWN;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (gameOver) {
                resetGame();
            } else {
                if (gameLoop.isRunning()) {
                    gameLoop.stop();
                } else {
                    gameLoop.start();
                }
                repaint();
            }
        } else if (!gameOver && canMove()) {
            if (e.getKeyCode() == KeyEvent.VK_UP && velocityY != 1) {
                velocityX = 0;
                velocityY = -1;
                lastKeyPressTime = System.currentTimeMillis();
            } else if (e.getKeyCode() == KeyEvent.VK_DOWN && velocityY != -1) {
                velocityX = 0;
                velocityY = 1;
                lastKeyPressTime = System.currentTimeMillis();
            } else if (e.getKeyCode() == KeyEvent.VK_LEFT && velocityX != 1) {
                velocityX = -1;
                velocityY = 0;
                lastKeyPressTime = System.currentTimeMillis();
            } else if (e.getKeyCode() == KeyEvent.VK_RIGHT && velocityX != -1) {
                velocityX = 1;
                velocityY = 0;
                lastKeyPressTime = System.currentTimeMillis();
            }
        }
    }

    private void resetGame() {
        snakeHead = new Tile(1, 5);
        snakeBody.clear();
        placeFood();
        velocityX = 1;
        velocityY = 0;
        gameOver = false;
        gameLoop.restart();
    }

    // not needed
    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    // Method to retrieve the best score from previous tries
    int getBestScore() {
        // You can use any method to store and retrieve the best score,
        // such as saving it to a file or using a database. For simplicity,
        // I'll just return the best score variable in this example.
        return bestScore;
    }

    // Method to update the best score
    void updateBestScore() {
        if (snakeBody.size() > bestScore) {
            bestScore = snakeBody.size();
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Snake Game");
        SnakeGame game = new SnakeGame(600, 600);
        frame.add(game);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        // Display the best score when the game starts
        JOptionPane.showMessageDialog(frame, "Best Score: " + game.getBestScore(), "Best Score", JOptionPane.INFORMATION_MESSAGE);
    }

}
 




