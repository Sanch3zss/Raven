package sanch3zss.raven.ui.hud.impl;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screens.ChatScreen;
import org.joml.Matrix4f;

import sanch3zss.raven.client.*;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.media.MediaPlaybackService;
import sanch3zss.raven.client.media.LyricsService;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.render.engine.DrawEngine;
import sanch3zss.raven.ui.hud.HudGlassPanel;
import sanch3zss.raven.render.animation.AnimatedFloat;
import sanch3zss.raven.ui.hud.DragRenderContext;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.hud.Draggable;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.setting.BooleanSetting;
import sanch3zss.raven.ui.setting.KeybindSetting;
import sanch3zss.raven.util.math.MathUtil;
import sanch3zss.raven.util.math.interpolation.Easings;

public final class MediaPlayerWidget extends Draggable {
    private static final Pattern LYRIC_WORD = Pattern.compile("\\S+");
    private final MediaPlaybackService mediaService = new MediaPlaybackService();
    private final LyricsService lyricsService = new LyricsService();
    private final BooleanSetting showLyrics = new BooleanSetting(Translation.clearText("Текст песни"));
    private final KeybindSetting nextTrackKey = new KeybindSetting(Translation.clearText("Следующий трек"));
    private final KeybindSetting previousTrackKey = new KeybindSetting(Translation.clearText("Предыдущий трек"));
    private final AnimatedFloat fadeAnimation = new AnimatedFloat(200, Easings.EASE_IN_OUT_QUAD);
    private final AnimatedFloat progressAnimation = new AnimatedFloat(350, Easings.EASE_OUT_QUAD);
    private final AnimatedFloat lyricsAnimation = new AnimatedFloat(320, Easings.EASE_OUT_CUBIC);
    private final AnimatedFloat[] controlPulses = {
            new AnimatedFloat(180, Easings.EASE_OUT_QUAD),
            new AnimatedFloat(180, Easings.EASE_OUT_QUAD),
            new AnimatedFloat(180, Easings.EASE_OUT_QUAD)};
    private final MsdfFont textFont = Fonts.ZENITH_SEMIBOLD.get();
    private final MsdfFont sourceFont = Fonts.MEDIA_PLAYER.get();
    private final MsdfFont controlFont = Fonts.PLAYER.get();
    private GlTexture artworkTexture;
    private byte[] displayedArtwork = new byte[0];
    private long artworkRetryAt;
    private MediaPlaybackService.Snapshot displayedSnapshot;
    private long optimisticPlaybackAt;
    private String lyricTrackKey = "";
    private String currentLyric;
    private String previousLyric;
    private List<LyricWord> lyricWords = List.of();
    private double currentLyricStart = -1.0;
    private float previousLyricShift;
    private float lastLyricShift;
    private float lastLyricReveal;
    private long lineChangedAt;
    private float smoothedShift = Float.NaN;
    private long lastFrameAt;
    private String shownTitle;
    private String shownArtist;
    private String previousTitle;
    private String previousArtist;
    private long trackChangedAt;

    public MediaPlayerWidget(BooleanSupplier visibility) {
        super("MediaPlayer", visibility);
        this.x = 604.0f;
        this.y = 405.0f;
        addSettings(this.showLyrics, this.nextTrackKey, this.previousTrackKey);
        this.nextTrackKey.consumer(action -> {
            if (action == KeyAction.PRESS && Mc.INSTANCE.getCurrentScreen() == null) {
                nextTrack();
            }
        });
        this.previousTrackKey.consumer(action -> {
            if (action == KeyAction.PRESS && Mc.INSTANCE.getCurrentScreen() == null) {
                previousTrack();
            }
        });
    }

    @Override
    public void layout(DragRenderContext context) {
    }

    @Override
    public void render(DragRenderContext context) {
        MediaPlaybackService.Snapshot snapshot = this.displayedSnapshot;
        if (!isVisible() || this.fadeAnimation.isZero() || snapshot == null) {
            return;
        }
        updateArtwork(snapshot.artwork());
        DrawEngine drawEngine = context.drawEngine();
        Matrix4f matrix = context.matrixStack().last().pose();
        ColorStack colors = drawEngine.colorStack();
        ThemePalette palette = context.theme().palette();
        float alpha = this.fadeAnimation.animatedValue();
        float coverX = this.x + 3.5f;
        float coverY = this.y + (34.0f - 27.0f) / 2.0f;
        float contentX = this.x + 34.0f;
        float contentRight = this.x + 112.0f - 4.0f;
        long now = System.currentTimeMillis();
        SourceIcons icons = resolveIcons(snapshot);
        LyricsService.Cue lyric = this.showLyrics.isValue()
                ? this.lyricsService.cueAt(snapshot, now) : null;
        updateLyrics(snapshot, lyric);

        colors.push();
        colors.alpha(alpha);
        drawGlassPanel(context, this.x, this.y, 112.0f,
                34.0f + 10.0f * this.lyricsAnimation.animatedValue(),
                5.0f, 30.0f);
        renderCover(drawEngine, matrix, colors, palette, coverX, coverY, icons.primary());

        float iconReserve = icons.primary() == 0 ? 0.0f : 10.0f;
        float titleWidth = Math.max(0.0f, contentRight - contentX - iconReserve);
        float trackBlend = trackBlend(snapshot, now);
        if (trackBlend < 1.0f) {
            float leave = 3.0f * trackBlend;
            drawFadingText(drawEngine, matrix, this.previousTitle, contentX, this.y + 4.5f - leave,
                    titleWidth, 7.0f, colors.computeColor(palette.text().tone(50).argb(), 1.0f - trackBlend));
            drawFadingText(drawEngine, matrix, this.previousArtist, contentX, this.y + 12.7f - leave,
                    contentRight - contentX, 5.2f,
                    colors.computeColor(palette.text().tone(200).argb(), 0.82f * (1.0f - trackBlend)));
        }
        float enter = 3.0f * (1.0f - trackBlend);
        drawFadingText(drawEngine, matrix, snapshot.title(), contentX, this.y + 4.5f + enter,
                titleWidth, 7.0f, colors.computeColor(palette.text().tone(50).argb(), trackBlend));
        drawFadingText(drawEngine, matrix, snapshot.artist(), contentX, this.y + 12.7f + enter,
                contentRight - contentX, 5.2f,
                colors.computeColor(palette.text().tone(200).argb(), 0.82f * trackBlend));
        renderSourceIcons(drawEngine, matrix, colors, palette, icons, contentRight);
        renderControls(drawEngine, matrix, colors, palette, snapshot);
        if (this.lyricsAnimation.animatedValue() > 0.01f) {
            renderLyrics(context, palette, lyric);
        }

        float barX = contentX;
        float barWidth = contentRight - contentX;
        float barY = this.y + 28.0f;
        if (snapshot.isStream()) {
            drawEngine.msdfFont(matrix, this.textFont, "LIVE", contentX, this.y + 20.3f,
                    4.6f, 0.0f, colors.computeColor(palette.accent().argb()));
            String liveLabel = "ЭФИР";
            drawEngine.msdfFont(matrix, this.textFont, liveLabel,
                    contentRight - this.textFont.getWidth(liveLabel, 4.6f), this.y + 20.3f,
                    4.6f, 0.0f, colors.computeColor(palette.text().tone(200).argb(), 0.78f));
            drawBarBackground(context, barX, barY, barWidth, 2.5f, 1.25f);
            float segmentWidth = barWidth * 0.28f;
            float phase = (now % 1600L) / 1600.0f;
            float segmentX = barWidth * phase;
            float firstWidth = Math.min(segmentWidth, barWidth - segmentX);
            int start = palette.gradientAccent(segmentX / barWidth);
            int end = palette.gradientAccent((segmentX + firstWidth) / barWidth);
            drawEngine.roundedHorizontalGradient(matrix, barX + segmentX, barY,
                    firstWidth, 2.5f, 1.25f,
                    colors.horizontalGradient(start, end, end, 0.0f, 1.0f));
            float wrappedWidth = segmentWidth - firstWidth;
            if (wrappedWidth > 0.0f) {
                int wrappedStart = palette.gradientAccent(0.0f);
                int wrappedEnd = palette.gradientAccent(wrappedWidth / barWidth);
                drawEngine.roundedHorizontalGradient(matrix, barX, barY,
                        wrappedWidth, 2.5f, 1.25f,
                        colors.horizontalGradient(wrappedStart, wrappedEnd, wrappedEnd, 0.0f, 1.0f));
            }
        } else {
            double position = snapshot.interpolatedPosition(now);
            float ratio = snapshot.duration() <= 0L ? 0.0f
                    : MathUtil.clamp((float) (position / snapshot.duration()), 0.0f, 1.0f);
            this.progressAnimation.destination(ratio);
            String elapsed = formatTime((long) position);
            String duration = formatTime(snapshot.duration());
            drawEngine.msdfFont(matrix, this.textFont, elapsed, contentX, this.y + 20.3f,
                    4.6f, 0.0f, colors.computeColor(palette.text().tone(200).argb(), 0.78f));
            drawEngine.msdfFont(matrix, this.textFont, duration,
                    contentRight - this.textFont.getWidth(duration, 4.6f), this.y + 20.3f,
                    4.6f, 0.0f, colors.computeColor(palette.text().tone(200).argb(), 0.78f));
            drawBarBackground(context, barX, barY, barWidth, 2.5f, 1.25f);
            float filledWidth = barWidth * this.progressAnimation.animatedValue();
            if (filledWidth > 0.0f) {
                int start = palette.gradientAccent(0.0f);
                int end = palette.gradientAccent(0.5f);
                drawEngine.roundedHorizontalGradient(matrix, barX, barY,
                        filledWidth, 2.5f, 1.25f,
                        colors.horizontalGradient(start, end, end, 0.0f, 1.0f));
            }
        }
        colors.pop();
    }

    private void renderControls(DrawEngine drawEngine, Matrix4f matrix, ColorStack colors,
                                ThemePalette palette, MediaPlaybackService.Snapshot snapshot) {
        float center = this.x + 34.0f + (112.0f - 34.0f - 4.0f) / 2.0f;
        int digit = this.textFont.glyphIndex('0');
        float ascent = this.textFont.metrics.ascent() == 0.0f
                ? this.textFont.metrics.lineHeight() : this.textFont.metrics.ascent();
        float rowY = this.y + 20.3f
                + (ascent - this.textFont.bearingY[digit]) * 4.6f;
        float rowHeight = this.textFont.planeHeight[digit] * 4.6f;
        int tint = colors.computeColor(palette.text().tone(50).argb());
        drawControl(drawEngine, matrix, "d", center - 12.0f, rowY, rowHeight, 4.6f * controlPop(0),
                colors.interpolate(tint, colors.computeColor(palette.accent().argb()), this.controlPulses[0].animatedValue()));
        drawControl(drawEngine, matrix, snapshot.playing() ? "a" : "b",
                center, rowY, rowHeight, 6.0f * controlPop(1),
                colors.interpolate(tint, colors.computeColor(palette.accent().argb()), this.controlPulses[1].animatedValue()));
        drawControl(drawEngine, matrix, "c", center + 12.0f, rowY, rowHeight, 4.6f * controlPop(2),
                colors.interpolate(tint, colors.computeColor(palette.accent().argb()), this.controlPulses[2].animatedValue()));
    }

    private float controlPop(int index) {
        return 1.0f + 0.22f * this.controlPulses[index].animatedValue();
    }

    private float trackBlend(MediaPlaybackService.Snapshot snapshot, long now) {
        if (!snapshot.title().equals(this.shownTitle) || !snapshot.artist().equals(this.shownArtist)) {
            if (this.shownTitle != null) {
                this.previousTitle = this.shownTitle;
                this.previousArtist = this.shownArtist;
                this.trackChangedAt = now;
            }
            this.shownTitle = snapshot.title();
            this.shownArtist = snapshot.artist();
        }
        if (this.previousTitle == null) {
            return 1.0f;
        }
        float blend = Easings.EASE_OUT_CUBIC.ease(MathUtil.clamp((now - this.trackChangedAt) / 420.0f, 0.0f, 1.0f));
        if (blend >= 1.0f) {
            this.previousTitle = null;
            this.previousArtist = null;
        }
        return blend;
    }

    private void updateLyrics(MediaPlaybackService.Snapshot snapshot, LyricsService.Cue cue) {
        String trackKey = this.lyricsService.trackKey(snapshot);
        if (!trackKey.equals(this.lyricTrackKey)) {
            this.lyricTrackKey = trackKey;
            this.currentLyric = null;
            this.previousLyric = null;
            this.lyricWords = List.of();
            this.currentLyricStart = -1.0;
            this.lastLyricShift = 0.0f;
            this.lastLyricReveal = 0.0f;
        }
        if (cue == null) {
            this.lyricsAnimation.destination(0.0f);
            return;
        }
        if (cue.start() != this.currentLyricStart || !cue.text().equals(this.currentLyric)) {
            this.previousLyric = this.lyricsAnimation.animatedValue() > 0.8f
                    ? this.currentLyric : null;
            this.previousLyricShift = this.lastLyricShift;
            this.currentLyric = cue.text();
            this.lyricWords = layoutWords(cue);
            this.currentLyricStart = cue.start();
            this.lineChangedAt = System.currentTimeMillis();
            this.smoothedShift = Float.NaN;
        }
        this.lyricsAnimation.destination(1.0f);
    }

    private void renderLyrics(DragRenderContext context, ThemePalette palette,
                              LyricsService.Cue cue) {
        DrawEngine drawEngine = context.drawEngine();
        Matrix4f matrix = context.matrixStack().last().pose();
        ColorStack colors = drawEngine.colorStack();
        float opacity = this.lyricsAnimation.animatedValue();
        float textX = this.x + 6.0f;
        float textWidth = 112.0f - 12.0f;
        float textY = this.y + 34.0f + (1.0f - opacity) * 3.0f;
        colors.push();
        colors.alpha(opacity);
        drawEngine.beginScissor(matrix, textX, this.y + 34.0f,
                textWidth, 10.0f * opacity);
        float blend = cue == null || this.previousLyric == null ? 1.0f : lineRoll();
        if (this.previousLyric != null && blend < 1.0f) {
            drawEngine.msdfFont(matrix, this.textFont, this.previousLyric,
                    textX + this.previousLyricShift,
                    textY - blend * 6.0f, 5.8f, 0.0f,
                    colors.computeColor(palette.text().tone(50).argb(), 1.0f - blend));
        }
        if (this.currentLyric != null) {
            float fullWidth = this.textFont.getWidth(this.currentLyric, 5.8f);
            float overflow = Math.max(0.0f, fullWidth - textWidth);
            float reveal = cue == null ? this.lastLyricReveal : lyricReveal(cue, fullWidth);
            float shift = cue == null ? this.lastLyricShift : overflow == 0.0f
                    ? (textWidth - fullWidth) / 2.0f
                    : -MathUtil.clamp(reveal - textWidth * 0.46f, 0.0f, overflow);
            long now = System.currentTimeMillis();
            float delta = Math.min(0.1f, (now - this.lastFrameAt) / 1000.0f);
            this.smoothedShift = Float.isNaN(this.smoothedShift) ? shift
                    : this.smoothedShift + (shift - this.smoothedShift) * (1.0f - (float) Math.exp(-delta * 9.0f));
            this.lastFrameAt = now;
            shift = this.smoothedShift;
            this.lastLyricShift = shift;
            this.lastLyricReveal = reveal;
            float currentY = textY + (1.0f - blend) * 6.0f;
            drawEngine.msdfFont(matrix, this.textFont, this.currentLyric, textX + shift,
                    currentY, 5.8f, 0.0f,
                    colors.computeColor(palette.text().tone(200).argb(), blend * 0.84f));
            if (reveal > 0.0f) {
                drawEngine.beginScissor(matrix, textX + shift, this.y + 34.0f,
                        reveal, 10.0f * opacity);
                drawEngine.msdfFont(matrix, this.textFont, this.currentLyric, textX + shift,
                        currentY, 5.8f, 0.0f,
                        colors.computeColor(palette.text().tone(50).argb(), blend));
                drawEngine.endScissor();
            }
            if (reveal > 0.0f && reveal < fullWidth) {
                for (int i = 0; i < 4; i++) {
                    drawEngine.beginScissor(matrix, textX + shift + reveal + i * 1.2f, this.y + 34.0f,
                            1.2f, 10.0f * opacity);
                    drawEngine.msdfFont(matrix, this.textFont, this.currentLyric, textX + shift,
                            currentY, 5.8f, 0.0f,
                            colors.computeColor(palette.text().tone(50).argb(), blend * (0.8f - i * 0.2f)));
                    drawEngine.endScissor();
                }
            }
        }
        drawEngine.endScissor();
        colors.pop();
    }

    private List<LyricWord> layoutWords(LyricsService.Cue cue) {
        List<LyricWord> words = new ArrayList<>();
        if (!cue.words().isEmpty()) {
            for (LyricsService.Word timed : cue.words()) {
                String word = cue.text().substring(timed.from(), timed.to());
                float x = this.textFont.getWidth(cue.text().substring(0, timed.from()), 5.8f);
                float width = this.textFont.getWidth(word, 5.8f);
                words.add(new LyricWord(x, width, timed.start(), timed.end()));
            }
        } else {
            float fullWidth = Math.max(1.0f, this.textFont.getWidth(cue.text(), 5.8f));
            double duration = Math.max(0.05, cue.end() - cue.start());
            Matcher matcher = LYRIC_WORD.matcher(cue.text());
            while (matcher.find()) {
                float x = this.textFont.getWidth(cue.text().substring(0, matcher.start()), 5.8f);
                float width = this.textFont.getWidth(matcher.group(), 5.8f);
                words.add(new LyricWord(x, width,
                        cue.start() + duration * x / fullWidth,
                        cue.start() + duration * (x + width) / fullWidth));
            }
        }
        return List.copyOf(words);
    }

    private float lyricReveal(LyricsService.Cue cue, float fullWidth) {
        for (int i = this.lyricWords.size() - 1; i >= 0; i--) {
            LyricWord word = this.lyricWords.get(i);
            if (cue.position() < word.start()) continue;
            float progress = (float) Math.clamp((cue.position() - word.start())
                    / Math.max(0.01, word.end() - word.start()), 0.0, 1.0);
            return i == this.lyricWords.size() - 1 && cue.position() >= word.end()
                    ? fullWidth : word.x() + word.width() * progress;
        }
        return 0.0f;
    }

    private float lineRoll() {
        float phase = MathUtil.clamp((System.currentTimeMillis() - this.lineChangedAt) / 340.0f, 0.0f, 1.0f);
        return Easings.EASE_OUT_CUBIC.ease(phase);
    }

    private void drawControl(DrawEngine drawEngine, Matrix4f matrix, String glyph,
                             float centerX, float rowY, float rowHeight, float size, int tint) {
        char character = glyph.charAt(0);
        drawEngine.msdfFont(matrix, this.controlFont, glyph,
                this.controlFont.centeredGlyphX(character, size, centerX - 5.0f, 10.0f),
                this.controlFont.centeredGlyphY(character, size, rowY, rowHeight),
                size, 0.0f, tint);
    }

    private void drawGlassPanel(DragRenderContext context, float x, float y,
                                float width, float height, float radius,
                                float shadowRadius) {
        DrawEngine drawEngine = context.drawEngine();
        ColorStack colors = drawEngine.colorStack();
        int accent = context.theme().palette().accentStart().argb();
        int tint = mixRgb(MenuStyle.surfaceColor(context.theme(), 0xFF111217), accent, 0.05f);
        int blurTexture = Raven.INSTANCE.widgetStack().hudBlurTexture();
        HudGlassPanel.drawShadows(drawEngine, context.matrixStack().last().pose(),
                x, y, width, height, radius, shadowRadius,
                colors.computeColor(tint, 20.5f));
        if (blurTexture != 0) {
            drawEngine.roundedGlassPanel(context.matrixStack().last().pose(),
                    x, y, width, height, radius, 0.0f, 0.80f,
                    colors.computeColor(tint, 1.0f),
                    0, blurTexture);
        } else {
            drawEngine.roundedRectangle(context.matrixStack().last().pose(),
                    x, y, width, height, radius, colors.computeColor(tint, 0.96f));
        }
    }

    private void drawBarBackground(DragRenderContext context, float x, float y, float width,
                                   float height, float radius) {
        DrawEngine drawEngine = context.drawEngine();
        ColorStack colors = drawEngine.colorStack();
        drawEngine.roundedRectangle(context.matrixStack().last().pose(),
                x, y, width, height, radius,
                colors.computeColor(10.0f / 255.0f, 255, 255, 255));
    }

    private void renderCover(DrawEngine drawEngine, Matrix4f matrix, ColorStack colors,
                             ThemePalette palette, float x, float y, char fallbackGlyph) {
        if (this.artworkTexture == null) {
            if (fallbackGlyph != 0) {
                float size = 16.0f;
                String glyph = String.valueOf(fallbackGlyph);
                drawEngine.msdfFont(matrix, this.sourceFont, glyph,
                        this.sourceFont.centeredGlyphX(fallbackGlyph, size, x, 27.0f),
                        this.sourceFont.centeredGlyphY(fallbackGlyph, size, y, 27.0f),
                        size, 0.0f, colors.computeColor(palette.accent().argb()));
            }
            return;
        }
        try {
            int textureId = this.artworkTexture.textureWithSTB();
            float u0 = 0.0f;
            float v0 = 0.0f;
            float u1 = 1.0f;
            float v1 = 1.0f;
            if (this.artworkTexture.width() > this.artworkTexture.height()) {
                float visible = this.artworkTexture.height() / (float) this.artworkTexture.width();
                u0 = (1.0f - visible) / 2.0f;
                u1 = 1.0f - u0;
            } else if (this.artworkTexture.height() > this.artworkTexture.width()) {
                float visible = this.artworkTexture.width() / (float) this.artworkTexture.height();
                v0 = (1.0f - visible) / 2.0f;
                v1 = 1.0f - v0;
            }
            int color = colors.computeColor(0xFFFFFFFF);
            drawEngine.roundedTexture(matrix, x, y, 27.0f, 27.0f,
                    5.0f, 5.0f, 5.0f, 5.0f,
                    u0, v0, u1, v1, drawEngine.bindTexture(textureId),
                    color, color, color, color);
        } catch (Throwable error) {
            Raven.LOGGER.warn("Failed to draw media artwork, retrying", error);
            this.artworkTexture.free();
            this.artworkTexture = null;
            this.displayedArtwork = new byte[0];
            this.artworkRetryAt = System.currentTimeMillis() + 1000L;
        }
    }

    private void renderSourceIcons(DrawEngine drawEngine, Matrix4f matrix, ColorStack colors,
                                   ThemePalette palette, SourceIcons icons, float right) {
        if (icons.primary() == 0) {
            return;
        }
        int color = colors.computeColor(palette.accent().argb());
        String primary = String.valueOf(icons.primary());
        float primarySize = 7.5f;
        float iconBoxWidth = 8.0f;
        float x = this.sourceFont.centeredGlyphX(icons.primary(), primarySize,
                right - iconBoxWidth, iconBoxWidth);
        float titleY = this.y + 4.5f;
        float iconY = this.sourceFont.centeredGlyphY(icons.primary(), primarySize,
                titleY, this.textFont.getHeight(7.0f));
        drawEngine.msdfFont(matrix, this.sourceFont, primary, x, iconY,
                primarySize, 0.0f, color);
        if (icons.badge() != 0) {
            String badge = String.valueOf(icons.badge());
            float badgeSize = 4.5f;
            float badgeWidth = this.sourceFont.getWidth(badge, badgeSize);
            float badgeX = right - badgeWidth + 1.0f;
            float badgeY = this.y + 4.8f;
            drawEngine.roundedRectangle(matrix, badgeX - 1.0f, this.y + 0.8f,
                    badgeWidth + 2.0f, 5.8f, 1.6f,
                    colors.computeColor(MenuStyle.surfaceColor(Raven.INSTANCE.theme(), 0xFF111217), 0.94f));
            drawEngine.msdfFont(matrix, this.sourceFont, badge, badgeX, badgeY,
                    badgeSize, 0.0f, color);
        }
    }

    private void updateArtwork(byte[] artwork) {
        byte[] next = artwork == null ? new byte[0] : artwork;
        if (Arrays.equals(this.displayedArtwork, next) || System.currentTimeMillis() < this.artworkRetryAt) {
            return;
        }
        if (this.artworkTexture != null) {
            this.artworkTexture.free();
            this.artworkTexture = null;
        }
        this.displayedArtwork = Arrays.copyOf(next, next.length);
        if (next.length == 0) {
            return;
        }
        ByteBuffer buffer = ByteBuffer.allocateDirect(next.length);
        buffer.put(next).flip();
        this.artworkTexture = new GlTexture(buffer).wrapX(33071).wrapY(33071)
                .minFilter(9987).generateMipMap(true).anisotropy(4.0f);
    }

    private static MediaPlaybackService.Snapshot editingPreview() {
        return new MediaPlaybackService.Snapshot("Название трека", "Исполнитель", "spotify",
                new byte[0], 83L, 213L, false, System.currentTimeMillis());
    }

    private static SourceIcons resolveIcons(MediaPlaybackService.Snapshot snapshot) {
        String owner = normalize(snapshot.owner());
        String metadata = normalize(snapshot.title() + " " + snapshot.artist() + " " + snapshot.owner());
        char browser = browserIcon(owner);
        char service = serviceIcon(metadata);
        if (browser != 0) {
            return new SourceIcons(browser, service != browser ? service : 0);
        }
        return new SourceIcons(service, (char) 0);
    }

    private static char browserIcon(String value) {
        if (value.contains("brave")) return 'b';
        if (value.contains("edge") || value.contains("msedge")) return 'c';
        if (value.contains("opera")) return 'e';
        if (value.contains("firefox")) return 'l';
        if (value.contains("yandexbrowser") || value.contains("yandex browser")) return 'j';
        if (value.contains("chrome") || value.contains("chromium")) return 'a';
        return 0;
    }

    private static char serviceIcon(String value) {
        if (value.contains("yandex music") || value.contains("яндекс музыка") || value.contains("yandexmusic")) return 'd';
        if (value.contains("soundcloud")) return 'f';
        if (value.contains("spotify")) return 'g';
        if (value.contains("telegram")) return 'h';
        if (value.contains("twitch")) return 'i';
        if (value.contains("youtube")) return 'k';
        return 0;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private void drawFadingText(DrawEngine drawEngine, Matrix4f matrix, String text,
                                float x, float y, float maxWidth, float size, int color) {
        if (text == null || text.isEmpty() || maxWidth <= 0.0f) {
            return;
        }
        if (this.textFont.getWidth(text, size) <= maxWidth) {
            drawEngine.msdfFont(matrix, this.textFont, text, x, y, size, 0.0f, color);
            return;
        }
        int end = text.length();
        while (end > 0 && this.textFont.getWidth(text.substring(0, end), size) > maxWidth) {
            end--;
        }
        if (end == 0) {
            return;
        }
        int fadeStart = end;
        while (fadeStart > 0
                && this.textFont.getWidth(text.substring(fadeStart, end), size) < 8.0f) {
            fadeStart--;
        }
        String solid = text.substring(0, fadeStart);
        String fading = text.substring(fadeStart, end);
        drawEngine.msdfFont(matrix, this.textFont, solid, x, y, size, 0.0f, color);
        drawEngine.msdfFontHorizontalGradient(matrix, this.textFont, fading,
                x + this.textFont.getWidth(solid, size), y, size, color, color & 0x00FFFFFF);
    }

    private static String formatTime(long value) {
        long totalSeconds = Math.max(0L, value);
        long hours = totalSeconds / 3600L;
        long minutes = totalSeconds / 60L % 60L;
        long seconds = totalSeconds % 60L;
        return hours > 0L
                ? String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
                : String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
    }

    private static int mixRgb(int base, int tint, float amount) {
        float inverse = 1.0f - amount;
        int red = Math.round(((base >> 16) & 0xFF) * inverse + ((tint >> 16) & 0xFF) * amount);
        int green = Math.round(((base >> 8) & 0xFF) * inverse + ((tint >> 8) & 0xFF) * amount);
        int blue = Math.round((base & 0xFF) * inverse + (tint & 0xFF) * amount);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    @Override
    public boolean click(MouseButtonInput2 input, boolean hovered) {
        if (!hovered || input.intercepted() || !input.press() || !input.isLeftButtonPressed()
                || this.displayedSnapshot == null) return false;
        float localX = (input.mouseX() - this.x) / (hudScale() * baseScale());
        float localY = (input.mouseY() - this.y) / (hudScale() * baseScale());
        if (localY < 17.0f || localY > 28.0f) return false;
        float center = 34.0f + (112.0f - 34.0f - 4.0f) / 2.0f;
        if (Math.abs(localX - (center - 12.0f)) <= 5.0f) {
            previousTrack();
            return true;
        }
        if (Math.abs(localX - center) <= 5.0f) {
            this.mediaService.control(MediaPlaybackService.Control.PLAY_PAUSE);
            MediaPlaybackService.Snapshot snapshot = this.displayedSnapshot;
            long now = System.currentTimeMillis();
            this.displayedSnapshot = new MediaPlaybackService.Snapshot(snapshot.title(), snapshot.artist(),
                    snapshot.owner(), snapshot.artwork(), (long) snapshot.interpolatedPosition(now),
                    snapshot.duration(), !snapshot.playing(), now);
            this.optimisticPlaybackAt = now;
            this.controlPulses[1].set(1.0f);
            return true;
        }
        if (Math.abs(localX - (center + 12.0f)) <= 5.0f) {
            nextTrack();
            return true;
        }
        return false;
    }

    private void previousTrack() {
        this.mediaService.control(MediaPlaybackService.Control.PREVIOUS);
        this.controlPulses[0].set(1.0f);
    }

    private void nextTrack() {
        this.mediaService.control(MediaPlaybackService.Control.NEXT);
        this.controlPulses[2].set(1.0f);
    }

    @Override
    public boolean cursor(MouseMoveInput input, boolean hovered) {
        return hovered && !input.intercepted();
    }

    @Override
    public void animate(WeightedEngine engine) {
        this.fadeAnimation.animate(engine);
        this.progressAnimation.animate(engine);
        this.lyricsAnimation.animate(engine);
        for (AnimatedFloat pulse : this.controlPulses) {
            pulse.destination(0.0f);
            pulse.animate(engine);
        }
    }

    @Override
    public void update() {
        this.mediaService.update();
        MediaPlaybackService.Snapshot current = this.mediaService.current();
        boolean editing = Mc.INSTANCE.getCurrentScreen() instanceof ChatScreen;
        if (current != null) {
            if (this.optimisticPlaybackAt == 0L
                    || this.displayedSnapshot == null
                    || !current.title().equals(this.displayedSnapshot.title())
                    || current.playing() == this.displayedSnapshot.playing()
                    || System.currentTimeMillis() - this.optimisticPlaybackAt > 3000L) {
                this.displayedSnapshot = current;
                this.optimisticPlaybackAt = 0L;
            }
            if (this.showLyrics.isValue()) this.lyricsService.update(current);
        } else if (editing) {
            this.displayedSnapshot = editingPreview();
        }
        this.fadeAnimation.destination(current != null || editing ? 1.0f : 0.0f);
        if (current == null && !editing && this.fadeAnimation.isZero()) {
            this.displayedSnapshot = null;
        }
    }

    public boolean isRendering() {
        return !this.fadeAnimation.isZero() && this.displayedSnapshot != null;
    }

    @Override
    public float width() {
        return 112.0f;
    }

    @Override
    public float height() {
        return 34.0f + 10.0f * this.lyricsAnimation.animatedValue();
    }

    @Override
    protected float baseScale() {
        return 1.75f;
    }

    private record SourceIcons(char primary, char badge) {
    }

    private record LyricWord(float x, float width, double start, double end) {
    }
}
