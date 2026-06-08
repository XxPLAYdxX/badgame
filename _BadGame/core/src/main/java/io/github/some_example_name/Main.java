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
    private static final int TOTAL_CELLS = 500 * 600; 
    private static int[] visited = new int[TOTAL_CELLS];
    private static int currentGeneration = 1; 
    private static int[] parentMap = new int[TOTAL_CELLS];
    private static int[] blobVisited = new int[TOTAL_CELLS];
    private static int blobGeneration = 1;
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
    
    // --- MEMORY OPTIMIZATION: Primitive Arrays ---
    private static int[][] gridColors; 
    private static boolean[][] gridActive;
    
    // --- CPU OPTIMIZATION: Active Piece Tracker ---
    private static List<int[]> activeParticles = new ArrayList<>();

    private float timer = 0f;
    private float timer2 = 0f;
    
    // --- PROGRESSIVE DIFFICULTY VARIABLES ---
    private float tickRate = 0.016f; 
    public static int currentMaxColors = 3; 

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

    private void restartGame() {
        for (int x = 0; x < WIDTH; x++) {
            Arrays.fill(gridColors[x], 0);
            Arrays.fill(gridActive[x], false);
        }
        activeParticles.clear();
        score = 0;
        tickRate = 0.016f;
        currentMaxColors = 3;
        scoreLabel.setText("SCORE: 0");
        gameOverTriggered = false;
    }

    private void updateDifficulty() {
        if (score < 30) {
            tickRate = 0.016f;
            currentMaxColors = 3; 
        } else if (score < 150) {
            tickRate = 0.012f; 
            currentMaxColors = 4; 
        } else if (score < 600) {
            tickRate = 0.009f; 
            currentMaxColors = 5; 
        } else {
            tickRate = 0.006f; 
            currentMaxColors = 6; 
        }
    }

    private float getDropDelay() {
        if (score < 100) return 0.20f;
        if (score < 300) return 0.14f;
        if (score < 600) return 0.09f;
        return 0.05f; 
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
        gridColors = new int[WIDTH][HEIGHT];
        gridActive = new boolean[WIDTH][HEIGHT];
        
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

        try { clickSound = Gdx.audio.newSound(Gdx.files.internal("fart.mp3")); } catch (Exception e) {}
        try { clearSound = Gdx.audio.newSound(Gdx.files.internal("clear.wav")); } catch (Exception e) {}
        
        font = new BitmapFont(); 
        font.getData().setScale(2.0f); 
        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        scoreLabel = new Label("SCORE: 0", labelStyle);
        
        nextPixmap = new Pixmap(PREVIEW_SIZE, PREVIEW_SIZE, Pixmap.Format.RGBA8888);
        nextTexture = new Texture(nextPixmap);
        nextShapeImage = new Image(nextTexture);

        gameTable = new Table();
        gameTable.setFillParent(true);
        stage.addActor(gameTable);

        menuTable = new Table();
        menuTable.setFillParent(true);
        stage.addActor(menuTable);

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
            
            // 1. Line Clear Engine
            lineCheckTimer += Gdx.graphics.getDeltaTime();
            if (lineCheckTimer >= 0.2f) {
                lineCheckTimer = 0f;
                List<Integer> bridgeBlob = getFullBridgeBlob();
                if (bridgeBlob != null) {
                    score += 10 + (bridgeBlob.size() / 300); 
                    scoreLabel.setText("SCORE: " + score);
                    
                    updateDifficulty(); 

                    for (int index : bridgeBlob) {
                        int bx = index % WIDTH;
                        int by = index / WIDTH;
                        gridColors[bx][by] = 0; 
                        gridActive[bx][by] = false;
                    }
                    if (clearSound != null) clearSound.play();
                }
            }

            // 2. Input Parsing
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
            
            // 3. Spawning Matrix
            if (activeParticles.isEmpty()) {
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

            // 4. Physics Tick Updates
            timer += Gdx.graphics.getDeltaTime();
            if (timer >= tickRate) {
                int simulationSpeed = 5; 
                int i2 = 1;
                for (int i = 0; i < simulationSpeed; i++) {
                    i2 *= -1;
                    updateSand(i2);
                }
                timer -= tickRate;
            }

            // Dedicated Controlled Gravity Loop (With Snappy Soft Drop Logic)
            timer2 += Gdx.graphics.getDeltaTime();
            boolean isSoftDropping = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
            float currentDropDelay = isSoftDropping ? 0.015f : getDropDelay();
            
            if (timer2 >= currentDropDelay) {
                // If soft dropping, fall by 8 horizontal grid rows per tick instead of 1
                moveActivePieceDown(isSoftDropping ? 8 : 1);
                timer2 = 0f;
            }

            // 5. Blit Render Screen
            pixmap.setColor(Color.BLACK);
            pixmap.fill(); 
            for (int x = 0; x < WIDTH; x++) {
                for (int y = 0; y < HEIGHT; y++) {
                    if (gridColors[x][y] != 0) {
                        pixmap.drawPixel(x, HEIGHT - y - 1, gridColors[x][y]);
                    }
                }
            }
            texture.draw(pixmap, 0, 0);
        }

        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    public static void newBlock(int X, int Y, int size, java.awt.Color color) {
        int argb = color.getRGB();
        int gdxColor = (argb << 8) | argb >>> 24;
        
        for (int x = -size; x <= size; x++) {
            for (int y = -size; y <= size; y++) {
                int drawX = X + x;
                int drawY = Y + y;

                if (drawX >= 0 && drawX < WIDTH && drawY >= 0 && drawY < HEIGHT) {
                    if (gridColors[drawX][drawY] != 0 && !gridActive[drawX][drawY]) {
                        gameOverTriggered = true;
                    }

                    gridColors[drawX][drawY] = gdxColor;
                    gridActive[drawX][drawY] = true;
                    activeParticles.add(new int[]{drawX, drawY});
                }
            }
        }
    }

    private void moveActivePiece(int dx) {
        if (activeParticles.isEmpty()) return;
        
        int steps = Math.abs(dx);
        int sign = dx > 0 ? 1 : -1;

        for (int s = 0; s < steps; s++) {
            boolean blocked = false;
            
            for (int[] p : activeParticles) {
                int newX = p[0] + sign;
                int newY = p[1];
                if (newX < 0 || newX >= WIDTH) { blocked = true; break; }
                if (gridColors[newX][newY] != 0 && !gridActive[newX][newY]) { blocked = true; break; }
            }

            if (blocked) break; 

            int[] savedColors = new int[activeParticles.size()];
            for (int i = 0; i < activeParticles.size(); i++) {
                int[] p = activeParticles.get(i);
                savedColors[i] = gridColors[p[0]][p[1]];
                gridColors[p[0]][p[1]] = 0; 
                gridActive[p[0]][p[1]] = false;
            }

            for (int i = 0; i < activeParticles.size(); i++) {
                int[] p = activeParticles.get(i);
                p[0] += sign; 
                gridColors[p[0]][p[1]] = savedColors[i];
                gridActive[p[0]][p[1]] = true;
            }
        }
    }

    private void moveActivePieceDown(int dy) {
        if (activeParticles.isEmpty()) return;

        // Perform programmatic sub-stepping loop to handle high velocity descent cleanly
        for (int s = 0; s < dy; s++) {
            boolean blocked = false;
            for (int[] p : activeParticles) {
                int newX = p[0];
                int newY = p[1] - 1;
                if (newY < 0) { blocked = true; break; }
                if (gridColors[newX][newY] != 0 && !gridActive[newX][newY]) { blocked = true; break; }
            }

            if (blocked) {
                for (int[] p : activeParticles) {
                    gridActive[p[0]][p[1]] = false;
                }
                activeParticles.clear();
                return; // Cease execution loop if tracking block hits an environmental stop
            }

            int[] savedColors = new int[activeParticles.size()];
            for (int i = 0; i < activeParticles.size(); i++) {
                int[] p = activeParticles.get(i);
                savedColors[i] = gridColors[p[0]][p[1]];
                gridColors[p[0]][p[1]] = 0;
                gridActive[p[0]][p[1]] = false;
            }

            for (int i = 0; i < activeParticles.size(); i++) {
                int[] p = activeParticles.get(i);
                p[1] -= 1;
                gridColors[p[0]][p[1]] = savedColors[i];
                gridActive[p[0]][p[1]] = true;
            }
        }
    }

    private void updateSand(int dir) {
        for (int y = 0; y < HEIGHT - 1; y++) {
            for (int xx = 0; xx < WIDTH; xx++) {
                int x = xx;
                if (dir == 1) { x = WIDTH - xx - 1; }
                
                if (gridColors[x][y + 1] == 0) continue;
                if (gridActive[x][y + 1]) continue; 

                if (gridColors[x][y] == 0) {
                    gridColors[x][y] = gridColors[x][y + 1];       
                    gridColors[x][y + 1] = 0;
                }
                else {
                    boolean canGoLeft = x > 0 && gridColors[x - 1][y] == 0 && !gridActive[x - 1][y];
                    boolean canGoRight = x < WIDTH - 1 && gridColors[x + 1][y] == 0 && !gridActive[x + 1][y];
                    
                    if (canGoLeft && canGoRight) {
                        if (dir == 1) {
                            gridColors[x - 1][y] = gridColors[x][y + 1];
                            gridColors[x][y + 1] = 0;
                        } else {
                            gridColors[x + 1][y] = gridColors[x][y + 1];
                            gridColors[x][y + 1] = 0;
                        }
                    } else if (canGoLeft) {
                        gridColors[x - 1][y] = gridColors[x][y + 1];
                        gridColors[x][y + 1] = 0;
                    } else if (canGoRight) {
                        gridColors[x + 1][y] = gridColors[x][y + 1];
                        gridColors[x][y + 1] = 0;
                    }
                }
            }
        }
    }

public static List<Integer> getFullBridgeBlob() {
    currentGeneration++;
    blobGeneration++;

    List<Integer> starters = new ArrayList<>();
    for (int y = 0; y < HEIGHT; y++) {
        if (gridColors[0][y] != 0 && !gridActive[0][y]) {
            starters.add(y * WIDTH);
        }
    }

    int finalBridgeIndex = -1;
    int targetColor = 0;
    int gapTolerance = 20; 

    searchLoop:
    for (int startIndex : starters) {
        if (visited[startIndex] == currentGeneration) continue;

        int startY = startIndex / WIDTH;
        targetColor = gridColors[0][startY];
        
        Queue<Integer> queue = new LinkedList<>();
        queue.add(startIndex);
        visited[startIndex] = currentGeneration;
        
        // FIX: Explicitly set the root node's parent to -1 so the backtracking loop knows where to stop!
        parentMap[startIndex] = -1; 

        while (!queue.isEmpty()) {
            int currIndex = queue.poll();
            int cx = currIndex % WIDTH;
            int cy = currIndex / WIDTH;

            if (cx >= WIDTH - gapTolerance - 1) {
                finalBridgeIndex = currIndex;
                break searchLoop;
            }

            for (int ox = 1; ox <= gapTolerance; ox++) { 
                for (int oy = -gapTolerance; oy <= gapTolerance; oy++) {
                    int nx = cx + ox;
                    int ny = cy + oy;

                    if (nx >= 0 && nx < WIDTH && ny >= 0 && ny < HEIGHT) {
                        int ni = nx + ny * WIDTH;
                        
                        if (gridColors[nx][ny] != 0 && !gridActive[nx][ny] 
                            && visited[ni] != currentGeneration 
                            && gridColors[nx][ny] == targetColor) {
                            
                            visited[ni] = currentGeneration;
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
    // This loop will now safely terminate when it hits -1 instead of spinning forever on 0
    while (trace != -1) {
        expansionQueue.add(trace);
        blobVisited[trace] = blobGeneration;
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

                    if (gridColors[nx][ny] != 0 && !gridActive[nx][ny] 
                        && blobVisited[ni] != blobGeneration 
                        && gridColors[nx][ny] == targetColor) {
                        
                        blobVisited[ni] = blobGeneration;
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
    }
    
    private void rotateActivePiece() {
        if (activeParticles.isEmpty()) return;
        
        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;

        for (int[] p : activeParticles) {
            if (p[0] < minX) minX = p[0];
            if (p[0] > maxX) maxX = p[0];
            if (p[1] < minY) minY = p[1];
            if (p[1] > maxY) maxY = p[1];
        }

        float cx = (minX + maxX) / 2f;
        float cy = (minY + maxY) / 2f;

        List<int[]> newPositions = new ArrayList<>();
        boolean blocked = false;

        for (int[] p : activeParticles) {
            float rx = p[0] - cx;
            float ry = p[1] - cy;

            int newX = Math.round(cx + ry);
            int newY = Math.round(cy - rx);

            if (newX < 0 || newX >= WIDTH || newY < 0 || newY >= HEIGHT) {
                blocked = true; 
                break;
            }
            
            if (gridColors[newX][newY] != 0 && !gridActive[newX][newY]) {
                blocked = true; 
                break;
            }

            newPositions.add(new int[]{newX, newY});
        }

        if (blocked) return; 

        int[] savedColors = new int[activeParticles.size()];
        
        for (int i = 0; i < activeParticles.size(); i++) {
            int[] p = activeParticles.get(i);
            savedColors[i] = gridColors[p[0]][p[1]];
            gridColors[p[0]][p[1]] = 0; 
            gridActive[p[0]][p[1]] = false;
        }

        for (int i = 0; i < newPositions.size(); i++) {
            int[] newP = newPositions.get(i);
            gridColors[newP[0]][newP[1]] = savedColors[i]; 
            gridActive[newP[0]][newP[1]] = true;
            
            activeParticles.get(i)[0] = newP[0];
            activeParticles.get(i)[1] = newP[1];
        }
    }
}
