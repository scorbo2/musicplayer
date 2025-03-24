package ca.corbett.musicplayer.ui;

import ca.corbett.extras.gradient.GradientConfig;
import ca.corbett.extras.gradient.GradientUtil;
import ca.corbett.extras.image.ImageTextUtil;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class AudioPanelIdleAnimation {

    private static AnimationThread thread;

    public static void go() {
        if (thread != null) {
            return;
        }
        thread = new AnimationThread();
        new Thread(thread).start();
    }

    public static void stop() {
        if (thread == null) {
            return;
        }
        thread.stop();
        thread = null;
    }

    private static class AnimationThread implements Runnable {

        public static final int WIDTH = 400;
        public static final int HEIGHT = 225;
        private boolean isRunning;
        private final GradientConfig gradient1;
        private final GradientConfig gradient2;
        int x = 0;

        public AnimationThread() {
            gradient1 = new GradientConfig();
            gradient1.setColor1(Color.BLACK);
            gradient1.setColor2(Color.BLUE);
            gradient1.setGradientType(GradientUtil.GradientType.HORIZONTAL_LINEAR);
            gradient2 = new GradientConfig();
            gradient2.setColor1(Color.BLUE);
            gradient2.setColor2(Color.BLACK);
            gradient2.setGradientType(GradientUtil.GradientType.HORIZONTAL_LINEAR);
        }

        public void stop() {
            isRunning = false;
        }

        @Override
        public void run() {
            isRunning = true;
            while (isRunning) {
                BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                graphics.setColor(Color.BLACK);
                graphics.fillRect(0, 0, WIDTH, HEIGHT);
                GradientUtil.fill(gradient1, graphics, 0, 180, x, 8);
                GradientUtil.fill(gradient2, graphics, x, 180, WIDTH - x, 8);
                graphics.dispose();

                ImageTextUtil.drawText(image, "R E A D Y", 100, new Font(Font.MONOSPACED, Font.PLAIN, 12), ImageTextUtil.TextAlign.CENTER, Color.BLUE, 0f, Color.BLUE, null, new Rectangle(60, 50, WIDTH - 120, 80));
                AudioPanel.getInstance().setIdleImage(image);

                x++;

                try {
                    Thread.sleep(80);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
