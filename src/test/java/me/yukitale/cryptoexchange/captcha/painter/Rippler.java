package me.yukitale.cryptoexchange.captcha.painter;

import java.awt.image.BufferedImage;

public class Rippler {

    public static class AxisConfig {

        private double start;

        private double length;

        private double amplitude;

        public AxisConfig(double start, double length, double amplitude) {
            this.start = normalize(start, 2);
            this.length = normalize(length, 4);
            this.amplitude = amplitude;
        }

        private double normalize(double a, int multi) {
            double piMulti = multi * Math.PI;

            a = Math.abs(a);
            double d = Math.floor(a / piMulti);

            return a - d * piMulti;
        }

        public double getStart() {
            return start;
        }

        public double getLength() {
            return length;
        }

        public double getAmplitude() {
            return amplitude;
        }
    }

    private AxisConfig vertical;

    private AxisConfig horizontal;

    public Rippler(AxisConfig vertical, AxisConfig horizontal) {
        this.vertical = vertical;
        this.horizontal = horizontal;
    }

    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        int width = src.getWidth();
        int height = src.getHeight();

        int[] verticalDelta = calcDeltaArray(vertical, width);

        int[] horizontalDelta = calcDeltaArray(horizontal, height);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int ny = (y + verticalDelta[x] + height) % height;
                int nx = (x + horizontalDelta[ny] + width) % width;
                dest.setRGB(nx, ny, src.getRGB(x, y));
            }
        }

        return dest;
    }

    private int[] calcDeltaArray(AxisConfig axisConfig, int num) {
        int[] delta = new int[ num ];

        double start = axisConfig.getStart();
        double period = axisConfig.getLength() / num;
        double amplitude = axisConfig.getAmplitude();

        for (int fi = 0; fi < num; fi++) {
            delta[fi] = (int) Math.round(amplitude * Math.sin(start + fi * period));
        }

        return delta;
    }

    public AxisConfig getVertical() {
        return vertical;
    }

    public AxisConfig getHorizontal() {
        return horizontal;
    }
}