package com.up.diplobot;

import java.awt.Canvas;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;

/**
 *
 * @author Ricky
 */
public class DrawableWindow {

    Frame f;
    Canvas c;
    BiConsumer<Rectangle, Graphics> con;
    Thread renderer;
    public BufferedImage buffer;
    
    public DrawableWindow(int width, int height, BiConsumer<Rectangle, Graphics> con) {
        this.con = con;
        f = new Frame();
        c = new Canvas() {
            
            @Override
            public void paint(Graphics g) {
                buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                con.accept(c.getBounds(), buffer.getGraphics());
                g.drawImage(buffer, 0, 0, null);
            }
            
            @Override
            public void update(Graphics g) {
                paint(g);
            }

//            @Override
//            public void resize(int width, int height) {
//                buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
//            }
            
        };
        f.add(c);
        f.setSize(width, height);
        f.setVisible(true);
        f.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
            
        });
//        renderer = new Thread(() -> {
//            while (true) {
//                c.repaint();
//            }
//        });
//        renderer.start();
    }
    
    public Canvas getCanvas() {
        return c;
    }
}
