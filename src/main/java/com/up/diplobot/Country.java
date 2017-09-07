package com.up.diplobot;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;

/**
 *
 * @author Ricky
 */
public enum Country {
    AUSTRIA("Austria", Color.RED), ENGLAND("England", Color.BLUE), FRANCE("France", Color.CYAN), GERMANY("Germany", Color.BLACK), ITALY("Italy", Color.GREEN), RUSSIA("Russia", Color.WHITE), TURKEY("Turkey", Color.YELLOW);
    
    public String name;
    public Color c;
    HashMap<Unit.UnitType, BufferedImage> cunits = new HashMap<>();

    private Country(String name, Color c) {
        this.name = name;
        this.c = c;
        for (Unit.UnitType ut : Unit.UnitType.values()) {
            BufferedImage i = new BufferedImage(ut.getImage().getWidth(), ut.getImage().getHeight(), BufferedImage.TYPE_INT_ARGB);
            applyMaskToImage(i, ut.getImage());
            cunits.put(ut, i);
        }
    }
    
    private void applyMaskToImage(BufferedImage image, BufferedImage mask) {
        int width = image.getWidth();
        int height = image.getHeight();

        int[] imagePixels = image.getRGB(0, 0, width, height, null, 0, width);
        int[] maskPixels = mask.getRGB(0, 0, width, height, null, 0, width);

        for (int i = 0; i < imagePixels.length; i++) {
            imagePixels[i] = c.getRGB() & (maskPixels[i] & 0xFF000000) | (c.getRGB() & 0xFFFFFF);
        }

        image.setRGB(0, 0, width, height, imagePixels, 0, width);
    }
    
    public BufferedImage getCountryUnitImage(Unit.UnitType ut) {
        return cunits.get(ut);
    }

    @Override
    public String toString() {
        return name;
    }
    
}
