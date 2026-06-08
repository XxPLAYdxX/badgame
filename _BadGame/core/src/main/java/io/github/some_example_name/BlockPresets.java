package io.github.some_example_name;
import java.awt.Color;

public class BlockPresets {
    
    // 1. Create an Enum to hold our shape names securely
    public enum Shape {
        WEIRD_LINE, LINE, SQUARE, DOT
    }

    public static Color newColor() {
        // Array now contains a broader depth of secondary/tertiary colors
        Color[] colors = {
            new Color(255, 0, 0),     // 0: Red
            new Color(0, 255, 0),     // 1: Green
            new Color(0, 0, 255),     // 2: Blue
            new Color(255, 255, 0),   // 3: Yellow (Unlocked at 100 pts)
            new Color(255, 0, 255),   // 4: Purple (Unlocked at 300 pts)
            new Color(0, 255, 255),   // 5: Cyan   (Unlocked at 600 pts)
            new Color(255, 128, 0)    // 6: Orange (Unlocked at 600 pts)
        };
        
        // Bounds check to ensure index never out-scales the color palette length
        int maxRange = Math.min(Main.currentMaxColors, colors.length);
        return colors[(int) (Math.random() * maxRange)];
    }

    // 2. Function to pick a random shape name
    public static Shape getRandomShape() {
        Shape[] shapes = Shape.values();
        return shapes[(int) (Math.random() * shapes.length)];
    }

    // 3. Master function to spawn a block based on its name and color
    public static void spawnShape(Shape shape, int size, Color rc) {
        int startX = Main.WIDTH / 2;
        int startY = 500; // Top of the screen

        switch (shape) {
            case WEIRD_LINE:
                Main.newBlock(startX, startY, size, rc);
                Main.newBlock(startX + (size * 2), startY, size, rc);
                Main.newBlock(startX + (size * 4), startY, size, rc);
                Main.newBlock(startX + (size * 2), startY + (size * 2), size, rc);
                break;
            case LINE:
                Main.newBlock(startX, startY, size, rc);
                Main.newBlock(startX + (size * 2), startY, size, rc);
                Main.newBlock(startX + (size * 4), startY, size, rc);
                Main.newBlock(startX - (size * 2), startY, size, rc); 
                break;
            case SQUARE:
                Main.newBlock(startX, startY, size, rc);
                Main.newBlock(startX + (size * 2), startY, size, rc);
                Main.newBlock(startX, startY + (size * 2), size, rc);
                Main.newBlock(startX + (size * 2), startY + (size * 2), size, rc);
                break;
            case DOT:
                Main.newBlock(startX, startY, size, rc);
                Main.newBlock(startX + (size * 2), startY, size, rc);
                Main.newBlock(startX + (size * 4), startY, size, rc);
                Main.newBlock(startX + (size * 4), startY + (size * 2), size, rc);
                break;
        }
    }
}
