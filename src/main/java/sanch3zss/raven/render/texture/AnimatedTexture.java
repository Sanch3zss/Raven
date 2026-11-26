package sanch3zss.raven.render.texture;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.*;

public final class AnimatedTexture implements AutoCloseable {
    private final int width;
    private final int height;
    private final List<Frame> frames;
    private final long durationMs;

    public AnimatedTexture(int width, int height, List<DecodedFrame> decodedFrames) {
        this.width = width;
        this.height = height;
        this.frames = new ArrayList<>(decodedFrames.size());
        long duration = 0L;
        for (DecodedFrame frame : decodedFrames) {
            duration += Math.max(20, frame.delayMs());
            this.frames.add(new Frame(toBuffer(frame.image()), Math.max(20, frame.delayMs()), duration));
        }
        this.durationMs = Math.max(1L, duration);
    }

    public int texture(long elapsedMs) {
        if (this.frames.isEmpty()) {
            return 0;
        }
        long position = this.frames.size() == 1 ? 0L : Math.floorMod(elapsedMs, this.durationMs);
        Frame selected = this.frames.getLast();
        for (Frame frame : this.frames) {
            if (position < frame.endsAtMs) {
                selected = frame;
                break;
            }
        }
        if (selected.texture == null) {
            selected.texture = new GlTexture(selected.rgba.duplicate())
                    .setDimensions(this.width, this.height)
                    .wrapX(33071).wrapY(33071);
        }
        return selected.texture.texture();
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public boolean animated() {
        return this.frames.size() > 1;
    }

    @Override
    public void close() {
        for (Frame frame : this.frames) {
            if (frame.texture != null) {
                frame.texture.free();
                frame.texture = null;
            }
        }
    }

    public static BufferedImage scaled(BufferedImage source, int width, int height) {
        if (source.getWidth() == width && source.getHeight() == height) {
            return source;
        }
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return result;
    }

    private static ByteBuffer toBuffer(BufferedImage image) {
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        ByteBuffer result = ByteBuffer.allocateDirect(pixels.length * 4);
        for (int pixel : pixels) {
            result.put((byte) (pixel >> 16));
            result.put((byte) (pixel >> 8));
            result.put((byte) pixel);
            result.put((byte) (pixel >> 24));
        }
        return result.flip();
    }

    public record DecodedFrame(BufferedImage image, int delayMs) {
    }

    private static final class Frame {
        private final ByteBuffer rgba;
        private final int delayMs;
        private final long endsAtMs;
        private GlTexture texture;

        private Frame(ByteBuffer rgba, int delayMs, long endsAtMs) {
            this.rgba = rgba;
            this.delayMs = delayMs;
            this.endsAtMs = endsAtMs;
        }
    }
}
