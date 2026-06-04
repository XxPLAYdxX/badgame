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

    public static boolean pieceActive = false;
    public static boolean collisionDetectedThisFrame = false;
    private BlockPresets.Shape queuedShape;
    private java.awt.Color queuedColor;
    private static boolean gameOverTriggered = false;

    // --- NEW: MENU & STATE VARIABLES ---
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

    // --- NEW: HANDLE GAME OVER LOGIC ---
    private void handleGameOver() {
        // Save new high score if achieved
        if (score > bestScore) {
            bestScore = score;
            prefs.putInteger("bestScore", bestScore);
            prefs.flush(); 
            bestScoreLabel.setText("BEST SCORE: " + bestScore);
        }
        
        restartGame();
        
        // Switch states
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

        // Load Persistent Preferences
        prefs = Gdx.app.getPreferences("SandTetrisPrefs");
        bestScore = prefs.getInteger("bestScore", 0);

        backgroundTexture = new Texture(Gdx.files.internal("background.png"));
        backgroundImage = new Image(backgroundTexture);
        backgroundImage.setFillParent(true); 
        stage.addActor(backgroundImage); 

        try {
            clickSound = Gdx.audio.newSound(Gdx.files.internal("fart.mp3"));
        } catch (Exception e) {
            clickSound = new EmptySound();
        }

        try {
            clearSound = Gdx.audio.newSound(Gdx.files.internal("clear.wav"));
        } catch (Exception e) {
            clearSound = new EmptySound();
        }
        
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

        // 1. Setup Game UI (Moved your previous rootTable logic here)
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

        // 2. Setup Menu UI
        Label titleLabel = new Label("SAND TETRIS", labelStyle);
        titleLabel.setFontScale(3.0f);
        bestScoreLabel = new Label("BEST SCORE: " + bestScore, labelStyle);
        
        // Generate a visual block for the Play Button without needing external files
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

        // Start by showing the Menu
        menuTable.setVisible(true);
        gameTable.setVisible(false);

        // Generate the very first shape
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

        // --- GAME LOGIC (ONLY RUNS IF PLAYING) ---
        if (currentState == GameState.PLAYING) {
            
            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                moveActivePiece(-3); 
            }
            if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                moveActivePiece(3);
            }
            
            if (!isPieceActive()) {
                List<Integer> bridgeBlob = getFullBridgeBlob();
                if (bridgeBlob != null) {
                    score += bridgeBlob.size(); 
                    scoreLabel.setText("SCORE: " + score);
                    for (int index : bridgeBlob) {
                        int bx = index % WIDTH;
                        int by = index / WIDTH;
                        grid[bx][by] = null;
                    }
                    clearSound.play();
                }

                BlockPresets.spawnShape(queuedShape, 12, queuedColor);
                clickSound.play();
                queuedShape = BlockPresets.getRandomShape();
                queuedColor = BlockPresets.newColor();
                setNextShapePreview(queuedShape, queuedColor);

                if (gameOverTriggered) {
                    handleGameOver(); 
                    gameOverTriggered = false; 
                }
            }

            timer += Gdx.graphics.getDeltaTime();
            timer2 += Gdx.graphics.getDeltaTime();

            if (timer >= tickRate) {
                int simulationSpeed = 5; 
                int i2 = 1;
                
                boolean isOneSecondTick = (timer2 >= 0.2f) || Gdx.input.isKeyPressed(Input.Keys.S);
                
                for (int i = 0; i < simulationSpeed; i++) {
                    i2 *= -1;
                    updateSand(i2, isOneSecondTick);
                }
                timer -= tickRate;
                
                if (timer2 >= 0.2f) {
                    timer2 -= 0.2f; 
                }
            }

            // Draw to texture
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

        // Draw whichever stage is visible
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
        List<int[]> activeParticles = new ArrayList<>();
        
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (grid[x][y] != null && grid[x][y].slowFall) {
                    activeParticles.add(new int[]{x, y});
                }
            }
        }

        for (int[] p : activeParticles) {
            int newX = p[0] + dx;
            int newY = p[1];
            if (newX < 0 || newX >= WIDTH) return; 
            if (grid[newX][newY] != null && !grid[newX][newY].slowFall) {
                return; 
            }
        }

        Sand[] savedSand = new Sand[activeParticles.size()];
        for (int i = 0; i < activeParticles.size(); i++) {
            int[] p = activeParticles.get(i);
            savedSand[i] = grid[p[0]][p[1]];
            grid[p[0]][p[1]] = null; 
        }

        for (int i = 0; i < activeParticles.size(); i++) {
            int[] p = activeParticles.get(i);
            grid[p[0] + dx][p[1]] = savedSand[i];
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
        int totalCells = WIDTH * HEIGHT;
        boolean[] visited = new boolean[totalCells];
        int[] parentMap = new int[totalCells];
        Arrays.fill(parentMap, -1);

        List<Integer> starters = new ArrayList<>();
        for (int y = 0; y < HEIGHT; y++) {
            if (grid[0][y] != null && !grid[0][y].slowFall) {
                starters.add(y * WIDTH);
            }
        }

        int finalBridgeIndex = -1;
        java.awt.Color targetColor = null;

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

                if (cx == WIDTH - 1) {
                    finalBridgeIndex = currIndex;
                    break searchLoop;
                }

                for (int ox = -1; ox <= 1; ox++) {
                    for (int oy = -1; oy <= 1; oy++) {
                        if (ox == 0 && oy == 0) continue;

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
        boolean[] blobVisited = new boolean[totalCells];

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
    }
}