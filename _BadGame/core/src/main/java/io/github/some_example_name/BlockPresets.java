package io.github.some_example_name;
import java.awt.Color;

public class BlockPresets {
    
    // 1. Create an Enum to hold our shape names securely
    public enum Shape {
        WEIRD_LINE, LINE, SQUARE, DOT
    }

    public static Color newColor() {
        Color[] colors = {
            new Color(255, 0, 0),
            new Color(0, 255, 0),
            new Color(255, 0, 255),
            new Color(0, 0, 255),
        };
        return colors[(int) (Math.random() * colors.length)];
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