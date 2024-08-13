package me.yukitale.cryptoexchange.captcha.painter;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Random;

public class CaptchaPainter {

    private final int width = 250;
    private final int height = 150;

    private final Random random = new Random();

    public BufferedImage draw(Font font, Color fGround, String text) {
        BufferedImage image = createImage();

        Graphics2D g2 = (Graphics2D) image.getGraphics();

        configureGraphics(g2, font, fGround);

        draw(g2, text);

        g2.dispose();

        return postProcess(image);
    }

    private BufferedImage createImage() {
        return new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    }

    private void configureGraphics(Graphics2D g2, Font font, Color fGround) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g2.setColor(fGround);
        g2.setBackground(Color.WHITE);
        g2.setFont(font);

        g2.clearRect(0, 0, width, height);
    }

    private void draw(Graphics2D g, String text) {
        GlyphVector vector = g.getFont().createGlyphVector(g.getFontRenderContext(), text);

        transform(vector);

        Rectangle bounds = vector.getPixelBounds(null, 0, height);
        float bw = (float) bounds.getWidth();
        float bh = (float) bounds.getHeight();

        float wr = width / bw * (random.nextFloat() / 20 + 0.89f) * 1;
        float hr = height / bh * (random.nextFloat() / 20 + 0.68f) * 1;
        g.translate((width - bw * wr) / 2, (height - bh * hr) / 2);
        g.scale(wr, hr);

        float bx = (float) bounds.getX();
        float by = (float) bounds.getY();
        
        g.draw(vector.getOutline(Math.signum(random.nextFloat() - 0.5f) * 1 * width / 200 - bx, Math.signum(random.nextFloat() - 0.5f) * 1 * height / 70 + height - by));
        
        g.drawGlyphVector(vector, -bx, height - by);
    }

    private void transform(GlyphVector v) {
        int glyphNum = v.getNumGlyphs();

        Point2D prePos = null;
        Rectangle2D preBounds = null;

        double rotateCur = (random.nextDouble() - 0.5) * Math.PI / 8;
        double rotateStep = Math.signum(rotateCur) * (random.nextDouble() * 3 * Math.PI / 8 / glyphNum);

        for (int fi = 0; fi < glyphNum; fi++) {
            AffineTransform tr = AffineTransform.getRotateInstance(rotateCur);
            if (random.nextDouble() < 0.25) {
                rotateStep *= -1;
            }
            rotateCur += rotateStep;
            v.setGlyphTransform(fi, tr);

            Point2D pos = v.getGlyphPosition(fi);
            Rectangle2D bounds = v.getGlyphVisualBounds(fi).getBounds2D();
            Point2D newPos;
            if (prePos == null) {
                newPos = new Point2D.Double(pos.getX() - bounds.getX(), pos.getY());
            } else {
                newPos = new Point2D.Double(preBounds.getMaxX() + pos.getX() - bounds.getX() - Math.min(preBounds.getWidth(), bounds.getWidth()) * (random.nextDouble() / 20 + 0.27), pos.getY());
            }
            v.setGlyphPosition(fi, newPos);
            prePos = newPos;
            preBounds = v.getGlyphVisualBounds(fi).getBounds2D();
        }
    }

    private BufferedImage postProcess(BufferedImage img) {
        Rippler.AxisConfig vertical = new Rippler.AxisConfig(random.nextDouble() * 2 * Math.PI, (1 + random.nextDouble() * 2) * Math.PI, img.getHeight() / 10.0);
        Rippler.AxisConfig horizontal = new Rippler.AxisConfig(random.nextDouble() * 2 * Math.PI, (2 + random.nextDouble() * 2) * Math.PI, img.getWidth() / 100.0);
        Rippler op = new Rippler(vertical, horizontal);

        float[] blurArray = new float[9];
        fillBlurArray(blurArray);
        ConvolveOp cp = new ConvolveOp(new Kernel( 3, 3, blurArray), ConvolveOp.EDGE_NO_OP, null);

        img = op.filter(img, createImage());

        return cp.filter(img, createImage());
    }

    private void fillBlurArray(float[] array) {
        float sum = 0;
        for (int fi = 0; fi < array.length; fi++) {
            array[fi] = random.nextFloat();
            sum += array[fi];
        }
        for (int fi = 0; fi < array.length; fi++) {
            array[fi] /= sum;
        }
    }
}
