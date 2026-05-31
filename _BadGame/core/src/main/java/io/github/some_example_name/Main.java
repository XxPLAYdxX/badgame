package io.github.some_example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class Main extends ApplicationAdapter {
    public static int WIDTH = 640;
    public static int HEIGHT = 480;

    private static int UI_WIDTH = 854;  // 640 is 75% of 854
    private static int UI_HEIGHT = 640; // 480 is 75% of 640
    
    // --- SCENE2D UI VARIABLES ---
    private Stage stage;
    private Pixmap pixmap;
    private Texture texture;
    
    private static Sand[][] grid; 

    private float timer = 0f;
    private final float tickRate = 0.016f; 

    @Override
    public void create() {
        grid = new Sand[WIDTH][HEIGHT];
        pixmap = new Pixmap(WIDTH, HEIGHT, Pixmap.Format.RGBA8888);
        texture = new Texture(pixmap);

        // 1. Create a Stage with a ScreenViewport (UI doesn't stretch, it just gets more space)
        stage = new Stage(new FitViewport(WIDTH, HEIGHT));
        
        // CRITICAL: Tell LibGDX to send all mouse/keyboard input to the Stage!
        Gdx.input.setInputProcessor(stage);

        // 2. Create a Layout Table and make it fill the whole screen
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        // 3. Wrap our Sand Texture in a Scene2D Image Widget
        Image sandGameWidget = new Image(texture);
        
        // 4. Add an Input Listener directly to the Widget!
        sandGameWidget.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                // x and y are strictly local to the widget. 0,0 is the bottom left of the sand box!
                BlockPresets.Line(10);

                return true; // Must return true to tell LibGDX we want to keep listening for drags
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                //spawnSandBrush((int) x, (int) y);
            }
        });

        // 5. Add the game widget to the center of our UI table
        // Later, you can add buttons to the left or right of this cell!
        rootTable.add(sandGameWidget).size(WIDTH, HEIGHT).center();
    }

    @Override
    public void resize(int width, int height) {
        // Tell the stage UI to update its layout when you resize or go fullscreen
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1); // Dark gray background so you can see the game window bounds

        // --- FULLSCREEN TOGGLE ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            if (Gdx.graphics.isFullscreen()) {
                Gdx.graphics.setWindowedMode(WIDTH, HEIGHT);
            } else {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            }
        }

        // --- SAND PHYSICS ---
        timer += Gdx.graphics.getDeltaTime();
        if (timer >= tickRate) {
            int simulationSpeed = 5; 
            int i2 = 1;
            for (int i = 0; i < simulationSpeed; i++) {
                i2*=-1;
                updateSand(i2);
            }
            timer -= tickRate;
        }

        // --- DRAW TO PIXMAP ---
        pixmap.setColor(Color.BLACK);
        pixmap.fill(); 
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (grid[x][y]!=null) {
                    int argb = grid[x][y].color.getRGB();
                    pixmap.drawPixel(x,HEIGHT-y-1, (argb & 0x00FFFFFF << 8) | argb >>> 24);
                }
            }
        }

        // Update the texture data so the Scene2D Image widget has the newest frame
        texture.draw(pixmap, 0, 0);

        // --- RENDER UI STAGE ---
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    // Extracted the brush logic into its own method to be called by the InputListener
    public static void newBlock(int X, int Y, int size) {
        for (int x = -size; x <= size; x++) {
            for (int y = -size; y <= size; y++) {
                int drawX = X + x;
                int drawY = Y + y;

                if (drawX >= 0 && drawX < WIDTH && drawY >= 0 && drawY < HEIGHT) {
                    grid[drawX][drawY] = new Sand();
                }
            }
        }
    }

    private void updateSand(int dir) {
        for (int y = 0; y < HEIGHT - 1; y++) {
            for (int xx = 0; xx < WIDTH; xx++) {
                int x = xx;
                if(dir == 1){
                   x=WIDTH-xx-1; 
                }
                if (grid[x][y + 1] != null) { 
                    if (grid[x][y] == null) {
                        grid[x][y] = grid[x][y + 1];       
                        grid[x][y + 1] = null;   
                    } 
                    else {
                        boolean canGoLeft = x > 0 && grid[x - 1][y] == null;
                        boolean canGoRight = x < WIDTH - 1 && grid[x + 1][y] == null;
                        
                        if (canGoLeft) {
                            grid[x - 1][y] = grid[x][y + 1];
                            grid[x][y + 1] = null;
                        } else if (canGoRight) {
                            grid[x + 1][y] = grid[x][y + 1];
                            grid[x][y + 1] = null;
                        }else if (canGoRight) {
                            grid[x + 1][y] = grid[x][y + 1];
                            grid[x][y + 1] = null;
                        } else if (canGoLeft) {
                            grid[x - 1][y] = grid[x][y + 1];
                            grid[x][y + 1] = null;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void dispose() {
        stage.dispose();
        texture.dispose();
        pixmap.dispose();
    }
}