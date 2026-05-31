package io.github.some_example_name;

public class BlockPresets{
    public static void Line(int size){
        Main.newBlock(Main.WIDTH/2, 400, 5);
        Main.newBlock(Main.WIDTH/2+10, 400, 5);
        Main.newBlock(Main.WIDTH/2+20, 400, 5);
        Main.newBlock(Main.WIDTH/2+10, 400+10, 5);
    }
}