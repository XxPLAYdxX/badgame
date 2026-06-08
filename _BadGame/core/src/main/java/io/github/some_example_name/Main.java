package io.github.some_example_name;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class Main extends ApplicationAdapter {
    // --- PATHFINDING OPTIMIZATION VARIABLES ---
    private static final int TOTAL_CELLS = 500 * 600; // WIDTH * HEIGHT
    private static boolean[] visited = new boolean[TOTAL_CELLS];
    private static int[] parentMap = new int[TOTAL_CELLS];
    private static boolean[] blobVisited = new boolean[TOTAL_CELLS];
    private float lineCheckTimer = 0f;
    public static int WIDTH = 500;
    public static int HEIGHT = 600;

    private static int UI_WIDTH = 854;  
    private static int UI_HEIGHT = 640; 
    
    private Stage stage;
    private Pixmap pixmap;
    private Texture texture;
    
    private Texture backgroundTexture;
    private Image backgroundImage;
    private BitmapFont font;
    private Label scoreLabel;
    private int score = 0;

    private Pixmap nextPixmap;
    private Texture nextTexture;
    private Image nextShapeImage;
    private final int PREVIEW_SIZE = 120; 
    
    private static Sand[][] grid; 

    private float timer = 0f;
    private float timer2 = 0f;
    private final float tickRate = 0.016f; 

    // --- MOVEMENT VARIABLES (DAS/ARR) ---
    private float dasTimer = 0f;
    private float arrTimer = 0f;
    private int currentDir = 0;
    private final float DAS_DELAY = 0.15f; 
    private final float ARR_RATE = 0.02f;  

    public static boolean pieceActive = false;
    public static boolean collisionDetectedThisFrame = false;
    private BlockPresets.Shape queuedShape;
    private java.awt.Color queuedColor;
    private static boolean gameOverTriggered = false;

    // --- MENU & STATE VARIABLES ---
    private enum GameState { MENU, PLAYING }
    private GameState currentState = GameState.MENU;
    private Preferences prefs;
    private int bestScore = 0;
    
    private Table menuTable;
    private Table gameTable;
    private Label bestScoreLabel;
    private Pixmap btnPixmap;
    private Texture btnTexture;

    private Sound clickSound;
    private Sound clearSound;
    private Sound gameoverSound;

    private void restartGame() {
        for (int x = 0; x < WIDTH; x++) {
            Arrays.fill(grid[x], null);
        }
        score = 0;
        scoreLabel.setText("SCORE: 0");
    }

    private void handleGameOver() {
        if (score > bestScore) {
            bestScore = score;
            prefs.putInteger("bestScore", bestScore);
            prefs.flush(); 
            bestScoreLabel.setText("BEST SCORE: " + bestScore);
        }
        
        restartGame();
        
        currentState = GameState.MENU;
        gameTable.setVisible(false);
        menuTable.setVisible(true);
    }

    @Override
    public void create() {
        grid = new Sand[WIDTH][HEIGHT];
        pixmap = new Pixmap(WIDTH, HEIGHT, Pixmap.Format.RGBA8888);
        texture = new Texture(pixmap);

        stage = new Stage(new FitViewport(UI_WIDTH, UI_HEIGHT));
        Gdx.input.setInputProcessor(stage);

        prefs = Gdx.app.getPreferences("SandTetrisPrefs");
        bestScore = prefs.getInteger("bestScore", 0);

        backgroundTexture = new Texture(Gdx.files.internal("background.png"));
        backgroundImage = new Image(backgroundTexture);
        backgroundImage.setFillParent(true); 
        stage.addActor(backgroundImage); 

        // Load sounds safely
        try { clickSound = Gdx.audio.newSound(Gdx.files.internal("fart.mp3")); } catch (Exception e) {}
        try { clearSound = Gdx.audio.newSound(Gdx.files.internal("clear.wav")); } catch (Exception e) {}
        
        font = new BitmapFont(); 
        font.getData().setScale(2.0f); 
        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        scoreLabel = new Label("SCORE: 0", labelStyle);
        
        nextPixmap = new Pixmap(PREVIEW_SIZE, PREVIEW_SIZE, Pixmap.Format.RGBA8888);
        nextTexture = new Texture(nextPixmap);
        nextShapeImage = new Image(nextTexture);

        // --- UI TABLES SETUP ---
        gameTable = new Table();
        gameTable.setFillParent(true);
        stage.addActor(gameTable);

        menuTable = new Table();
        menuTable.setFillParent(true);
        stage.addActor(menuTable);

        // Game UI
        Image sandGameWidget = new Image(texture);
        gameTable.center();
        gameTable.add(sandGameWidget).size(WIDTH, HEIGHT).pad(20);
        
        Table sidePanel = new Table();
        sidePanel.top();
        sidePanel.add(scoreLabel).padTop(60).expandX().center();
        sidePanel.row(); 
        Label nextHeadingLabel = new Label("NEXT:", labelStyle);
        sidePanel.add(nextHeadingLabel).padTop(40).center();
        sidePanel.row(); 
        sidePanel.add(nextShapeImage).size(PREVIEW_SIZE, PREVIEW_SIZE).padTop(10).center();
        gameTable.add(sidePanel).width(UI_WIDTH - WIDTH - 40).fillY().top();

        // Menu UI
        Label titleLabel = new Label("SAND TETRIS", labelStyle);
        titleLabel.setFontScale(3.0f);
        bestScoreLabel = new Label("BEST SCORE: " + bestScore, labelStyle);
        
        btnPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        btnPixmap.setColor(new Color(0.2f, 0.8f, 0.2f, 1f));
        btnPixmap.fill();
        btnTexture = new Texture(btnPixmap);
        
        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.up = new TextureRegionDrawable(new TextureRegion(btnTexture));
        btnStyle.font = font;
        
        TextButton playButton = new TextButton("PLAY", btnStyle);
        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                currentState = GameState.PLAYING;
                menuTable.setVisible(false);
                gameTable.setVisible(true);
            }
        });

        menuTable.add(titleLabel).padBottom(60).row();
        menuTable.add(playButton).size(200, 60).padBottom(30).row();
        menuTable.add(bestScoreLabel);

        menuTable.setVisible(true);
        gameTable.setVisible(false);

        queuedShape = BlockPresets.getRandomShape();
        queuedColor = BlockPresets.newColor();
        setNextShapePreview(queuedShape, queuedColor);
    }

    public void setNextShapePreview(BlockPresets.Shape shape, java.awt.Color shapeColor) {
        nextPixmap.setColor(new Color(0f, 0f, 0f, 0.6f)); 
        nextPixmap.fill();
        nextPixmap.setColor(Color.LIGHT_GRAY);
        nextPixmap.drawRectangle(0, 0, PREVIEW_SIZE, PREVIEW_SIZE);

        int argb = shapeColor.getRGB();
        int gdxColor = (argb << 8) | argb >>> 24;
        nextPixmap.setColor(gdxColor);

        int b = 15; 
        int cx = PREVIEW_SIZE / 2;
        int cy = PREVIEW_SIZE / 2;

        switch(shape) {
            case WEIRD_LINE:
                nextPixmap.fillRectangle(cx - b, cy, b, b);
                nextPixmap.fillRectangle(cx, cy, b, b);
                nextPixmap.fillRectangle(cx + b, cy, b, b);
                nextPixmap.fillRectangle(cx, cy - b, b, b);
                break;
            case LINE:
                nextPixmap.fillRectangle(cx - b*2, cy, b, b);
                nextPixmap.fillRectangle(cx - b, cy, b, b);
                nextPixmap.fillRectangle(cx, cy, b, b);
                nextPixmap.fillRectangle(cx + b, cy, b, b);
                break;
            case SQUARE:
                nextPixmap.fillRectangle(cx - b, cy - b, b*2, b*2);
                break;
            case DOT:
                nextPixmap.fillRectangle(cx - b/2, cy - b/2, b, b);
                break;
        }
        nextTexture.draw(nextPixmap, 0, 0);
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1);

        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            if (Gdx.graphics.isFullscreen()) {
                Gdx.graphics.setWindowedMode(WIDTH, HEIGHT);
            } else {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            }
        }

        if (currentState == GameState.PLAYING) {
            
            // 1. ALWAYS check for completed lines
            lineCheckTimer += Gdx.graphics.getDeltaTime();
            if (lineCheckTimer >= 0.2f) {
                lineCheckTimer = 0f; // Reset timer
                List<Integer> bridgeBlob = getFullBridgeBlob();
                if (bridgeBlob != null) {
                    score += bridgeBlob.size(); 
                    scoreLabel.setText("SCORE: " + score);
                    for (int index : bridgeBlob) {
                        int bx = index % WIDTH;
                        int by = index / WIDTH;
                        grid[bx][by] = null;
                    }
                    if (clearSound != null) clearSound.play();
                }
            }

            // 2. Handle Player Input (DAS / Smooth Holding)
            int inputDir = 0;
            if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                rotateActivePiece();
            }
            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                inputDir = -1;
            } else if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                inputDir = 1;
            }

            if (inputDir != 0) {
                if (currentDir != inputDir) {
                    moveActivePiece(inputDir * 4); 
                    currentDir = inputDir;
                    dasTimer = 0f;
                    arrTimer = 0f;
                } else {
                    dasTimer += Gdx.graphics.getDeltaTime();
                    if (dasTimer >= DAS_DELAY) {
                        arrTimer += Gdx.graphics.getDeltaTime();
                        if (arrTimer >= ARR_RATE) {
                            moveActivePiece(inputDir * 4); 
                            arrTimer = 0f; 
                        }
                    }
                }
            } else {
                currentDir = 0; 
            }
            
            // 3. Spawning logic
            if (!isPieceActive()) {
                BlockPresets.spawnShape(queuedShape, 12, queuedColor);
                if (clickSound != null) clickSound.play();
                queuedShape = BlockPresets.getRandomShape();
                queuedColor = BlockPresets.newColor();
                setNextShapePreview(queuedShape, queuedColor);

                if (gameOverTriggered) {
                    handleGameOver(); 
                    gameOverTriggered = false; 
                }
            }

            // 4. Physics Engine
            timer += Gdx.graphics.getDeltaTime();
            timer2 += Gdx.graphics.getDeltaTime();

            if (timer >= tickRate) {
                int simulationSpeed = 5; 
                int i2 = 1;
                
                boolean isOneSecondTick = (timer2 >= 0.2f) || (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN));
                
                for (int i = 0; i < simulationSpeed; i++) {
                    i2 *= -1;
                    updateSand(i2, isOneSecondTick);
                }
                timer -= tickRate;
                
                if (timer2 >= 0.2f) {
                    timer2 -= 0.2f; 
                }
            }

            // 5. Draw to texture
            pixmap.setColor(Color.BLACK);
            pixmap.fill(); 
            for (int x = 0; x < WIDTH; x++) {
                for (int y = 0; y < HEIGHT; y++) {
                    if (grid[x][y] != null) {
                        int argb = grid[x][y].color.getRGB();
                        pixmap.drawPixel(x, HEIGHT - y - 1, (argb << 8) | argb >>> 24);
                    }
                }
            }
            texture.draw(pixmap, 0, 0);
        }

        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    public static void newBlock(int X, int Y, int size, java.awt.Color color) {
        for (int x = -size; x <= size; x++) {
            for (int y = -size; y <= size; y++) {
                int drawX = X + x;
                int drawY = Y + y;

                if (drawX >= 0 && drawX < WIDTH && drawY >= 0 && drawY < HEIGHT) {
                    if (grid[drawX][drawY] != null && !grid[drawX][drawY].slowFall) {
                        gameOverTriggered = true;
                    }

                    Sand a = new Sand();
                    a.slowFall = true;
                    a.color = color;
                    grid[drawX][drawY] = a;
                }
            }
        }
    }

    private void moveActivePiece(int dx) {
        int steps = Math.abs(dx);
        int sign = dx > 0 ? 1 : -1;

        List<int[]> activeParticles = new ArrayList<>();
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (grid[x][y] != null && grid[x][y].slowFall) {
                    activeParticles.add(new int[]{x, y});
                }
            }
        }

        for (int s = 0; s < steps; s++) {
            boolean blocked = false;
            
            for (int[] p : activeParticles) {
                int newX = p[0] + sign;
                int newY = p[1];
                if (newX < 0 || newX >= WIDTH) { blocked = true; break; }
                if (grid[newX][newY] != null && !grid[newX][newY].slowFall) { blocked = true; break; }
            }

            if (blocked) break; 

            Sand[] savedSand = new Sand[activeParticles.size()];
            for (int i = 0; i < activeParticles.size(); i++) {
                int[] p = activeParticles.get(i);
                savedSand[i] = grid[p[0]][p[1]];
                grid[p[0]][p[1]] = null; 
            }

            for (int i = 0; i < activeParticles.size(); i++) {
                int[] p = activeParticles.get(i);
                grid[p[0] + sign][p[1]] = savedSand[i];
                p[0] += sign; 
            }
        }
    }

    private void updateSand(int dir, boolean onesecond) {
        boolean shouldDeactivatePiece = false;
        
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (grid[x][y] != null && grid[x][y].slowFall) {
                    if (y == 0 || (grid[x][y - 1] != null && !grid[x][y - 1].slowFall)) {
                        shouldDeactivatePiece = true;
                        break;
                    }
                }
            }
            if (shouldDeactivatePiece) break;
        }

        if (shouldDeactivatePiece) {
            for (int x = 0; x < WIDTH; x++) {
                for (int y = 0; y < HEIGHT; y++) {
                    if (grid[x][y] != null && grid[x][y].slowFall) {
                        grid[x][y].slowFall = false;
                    }
                }
            }
        }

        for (int y = 0; y < HEIGHT - 1; y++) {
            for (int xx = 0; xx < WIDTH; xx++) {
                int x = xx;
                if (dir == 1) {
                    x = WIDTH - xx - 1; 
                }
                
                if (grid[x][y + 1] != null) { 
                    if (grid[x][y + 1].slowFall && !onesecond) {
                        continue; 
                    }

                    if (grid[x][y] == null) {
                        grid[x][y] = grid[x][y + 1];       
                        grid[x][y + 1] = null;
                        
                        if (y == 0 || (y > 0 && grid[x][y - 1] != null && !grid[x][y - 1].slowFall)) {
                            grid[x][y].slowFall = false;
                        }
                    }
                    else {
                        if (!grid[x][y].slowFall) {
                            grid[x][y + 1].slowFall = false;
                        }
                        
                        if (!grid[x][y + 1].slowFall || onesecond) {
                            boolean canGoLeft = x > 0 && grid[x - 1][y] == null;
                            boolean canGoRight = x < WIDTH - 1 && grid[x + 1][y] == null;
                            
                            if (canGoLeft) {
                                grid[x - 1][y] = grid[x][y + 1];
                                grid[x][y + 1] = null; 
                                
                                if (y == 0 || (y > 0 && grid[x - 1][y - 1] != null && !grid[x - 1][y - 1].slowFall)) {
                                    grid[x - 1][y].slowFall = false;
                                }
                            } else if (canGoRight) {
                                grid[x + 1][y] = grid[x][y + 1];
                                grid[x][y + 1] = null;
                                
                                if (y == 0 || (y > 0 && grid[x + 1][y - 1] != null && !grid[x + 1][y - 1].slowFall)) {
                                    grid[x + 1][y].slowFall = false;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isPieceActive() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (grid[x][y] != null && grid[x][y].slowFall) {
                    return true; 
                }
            }
        }
        return false; 
    }

    public static List<Integer> getFullBridgeBlob() {
        // Quickly wipe the global arrays clean instead of creating new ones
        Arrays.fill(visited, false);
        Arrays.fill(parentMap, -1);
        Arrays.fill(blobVisited, false);

        List<Integer> starters = new ArrayList<>();
        for (int y = 0; y < HEIGHT; y++) {
            if (grid[0][y] != null && !grid[0][y].slowFall) {
                starters.add(y * WIDTH);
            }
        }

        int finalBridgeIndex = -1;
        java.awt.Color targetColor = null;
        int gapTolerance = 20; 

        searchLoop:
        for (int startIndex : starters) {
            if (visited[startIndex]) continue;

            int startY = startIndex / WIDTH;
            targetColor = grid[0][startY].color;
            
            Queue<Integer> queue = new LinkedList<>();
            queue.add(startIndex);
            visited[startIndex] = true;

            while (!queue.isEmpty()) {
                int currIndex = queue.poll();
                int cx = currIndex % WIDTH;
                int cy = currIndex / WIDTH;

                if (cx >= WIDTH - gapTolerance - 1) {
                    finalBridgeIndex = currIndex;
                    break searchLoop;
                }

                for (int ox = 1; ox <= gapTolerance; ox++) { // Start at 1 (always move right)
                    for (int oy = -gapTolerance; oy <= gapTolerance; oy++) {
                        int nx = cx + ox;
                        int ny = cy + oy;

                        if (nx >= 0 && nx < WIDTH && ny >= 0 && ny < HEIGHT) {
                            int ni = nx + ny * WIDTH;
                            Sand neighbor = grid[nx][ny];

                            if (neighbor != null && !neighbor.slowFall && !visited[ni] && neighbor.color.equals(targetColor)) {
                                visited[ni] = true;
                                parentMap[ni] = currIndex;
                                queue.add(ni);
                            }
                        }
                    }
                }
            }
        }

        if (finalBridgeIndex == -1) return null;

        List<Integer> fullBlob = new ArrayList<>();
        Queue<Integer> expansionQueue = new LinkedList<>();

        int trace = finalBridgeIndex;
        while (trace != -1) {
            expansionQueue.add(trace);
            blobVisited[trace] = true;
            trace = parentMap[trace];
        }

        while (!expansionQueue.isEmpty()) {
            int currIndex = expansionQueue.poll();
            fullBlob.add(currIndex);

            int cx = currIndex % WIDTH;
            int cy = currIndex / WIDTH;

            for (int ox = -1; ox <= 1; ox++) {
                for (int oy = -1; oy <= 1; oy++) {
                    if (ox == 0 && oy == 0) continue;
                    int nx = cx + ox;
                    int ny = cy + oy;

                    if (nx >= 0 && nx < WIDTH && ny >= 0 && ny < HEIGHT) {
                        int ni = nx + ny * WIDTH;
                        Sand neighbor = grid[nx][ny];

                        if (neighbor != null && !neighbor.slowFall && !blobVisited[ni] && neighbor.color.equals(targetColor)) {
                            blobVisited[ni] = true;
                            expansionQueue.add(ni);
                        }
                    }
                }
            }
        }
        return fullBlob;
    }

    @Override
    public void dispose() {
        stage.dispose();
        texture.dispose();
        pixmap.dispose();
        if (nextTexture != null) nextTexture.dispose();
        if (nextPixmap != null) nextPixmap.dispose();
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (font != null) font.dispose();
        if (btnTexture != null) btnTexture.dispose();
        if (btnPixmap != null) btnPixmap.dispose();
        
        if (clickSound != null) clickSound.dispose();
        if (clearSound != null) clearSound.dispose();
        if (gameoverSound != null) gameoverSound.dispose();
    }
    private void rotateActivePiece() {
        List<int[]> activeParticles = new ArrayList<>();
        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;

        // 1. Gather all falling particles and find their bounding box
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (grid[x][y] != null && grid[x][y].slowFall) {
                    activeParticles.add(new int[]{x, y});
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (activeParticles.isEmpty()) return;

        // 2. Calculate the center pivot point
        float cx = (minX + maxX) / 2f;
        float cy = (minY + maxY) / 2f;

        List<int[]> newPositions = new ArrayList<>();
        boolean blocked = false;

        // 3. Calculate new rotated coordinates
        for (int[] p : activeParticles) {
            // Find position relative to the center
            float rx = p[0] - cx;
            float ry = p[1] - cy;

            // Apply 90-degree clockwise rotation math
            int newX = Math.round(cx + ry);
            int newY = Math.round(cy - rx);

            // Check bounds (don't rotate out of the screen)
            if (newX < 0 || newX >= WIDTH || newY < 0 || newY >= HEIGHT) {
                blocked = true; 
                break;
            }
            
            // Check collisions (don't rotate into settled sand)
            if (grid[newX][newY] != null && !grid[newX][newY].slowFall) {
                blocked = true; 
                break;
            }

            newPositions.add(new int[]{newX, newY});
        }

        if (blocked) return; // Abort rotation if it doesn't fit

        // 4. Apply the rotation safely
        Sand[] savedSand = new Sand[activeParticles.size()];
        
        // First, clear all old positions from the grid
        for (int i = 0; i < activeParticles.size(); i++) {
            int[] p = activeParticles.get(i);
            savedSand[i] = grid[p[0]][p[1]];
            grid[p[0]][p[1]] = null; 
        }

        // Then, stamp them into their new rotated positions
        for (int i = 0; i < newPositions.size(); i++) {
            int[] newP = newPositions.get(i);
            grid[newP[0]][newP[1]] = savedSand[i]; 
        }
    }
}
