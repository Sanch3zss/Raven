package sanch3zss.raven.client.media;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.ptr.IntByReference;
import dev.redstones.mediaplayerinfo.*;

import sanch3zss.raven.client.Raven;

public final class MediaPlaybackService {
    private static final long POLL_INTERVAL_MS = 400L;
    private final AtomicBoolean polling = new AtomicBoolean();
    private volatile Snapshot current;
    private volatile long nextPollAt;
    private String selectedOwner = "";
    private String selectedTitle = "";
    private String selectedArtist = "";
    private long lastSessionAt;
    private byte[] rawArtwork = new byte[0];
    private byte[] croppedArtwork = new byte[0];
    private String spotifyKey = "";
    private String spotifyArtists;

    public void update() {
        long now = System.currentTimeMillis();
        if (now < this.nextPollAt || !this.polling.compareAndSet(false, true)) {
            return;
        }
        this.nextPollAt = now + POLL_INTERVAL_MS;
        Raven.INSTANCE.executor().execute(this::poll);
    }

    public Snapshot current() {
        return this.current;
    }

    public void control(Control control) {
        Raven.INSTANCE.executor().execute(() -> {
            try {
                IMediaSession session = selectSession(MediaPlayerInfo.Instance.getMediaSessions(), true);
                if (session == null) return;
                switch (control) {
                    case PLAY_PAUSE -> session.playPause();
                    case NEXT -> session.next();
                    case PREVIOUS -> session.previous();
                }
                this.nextPollAt = 0L;
            } catch (Throwable error) {
                Raven.LOGGER.debug("Unable to control operating-system media session", error);
            }
        });
    }

    private void poll() {
        try {
            List<IMediaSession> sessions = MediaPlayerInfo.Instance.getMediaSessions();
            IMediaSession session = selectSession(sessions, false);
            if (session == null) {
                if (System.currentTimeMillis() - this.lastSessionAt > 1500L) {
                    this.current = null;
                }
                return;
            }
            MediaInfo media = session.getMedia();
            long capturedAt = System.currentTimeMillis();
            String owner = clean(session.getOwner(), "");
            String title = clean(media.getTitle(), "Без названия");
            String artist = clean(media.getArtist(), "Неизвестный исполнитель");
            if (owner.toLowerCase(Locale.ROOT).contains("spotify")) {
                artist = spotifyArtists(artist, title);
            }
            long rawPosition = Math.max(0L, media.getPosition());
            Snapshot previous = this.current;
            boolean sameTrack = previous != null && owner.equals(this.selectedOwner)
                    && title.equals(previous.title()) && artist.equals(previous.artist());
            long duration = Math.max(0L, media.getDuration());
            if (sameTrack && Math.abs(duration - previous.duration()) <= 2L) {
                duration = previous.duration();
            }
            double position = rawPosition;
            if (previous != null && media.getPlaying() && previous.playing()
                    && sameTrack
                    && Math.abs(previous.interpolatedPosition(capturedAt) - rawPosition) <= 1.5) {
                position = previous.interpolatedPosition(capturedAt);
            }
            long wholeSeconds = (long) position;
            long positionCapturedAt = capturedAt - Math.round((position - wholeSeconds) * 1000.0);
            this.selectedOwner = owner;
            this.selectedTitle = clean(media.getTitle(), "");
            this.selectedArtist = clean(media.getArtist(), "");
            this.lastSessionAt = capturedAt;
            byte[] artwork = media.getArtworkPng();
            if (artwork != null && artwork.length > 0) {
                artwork = readArtwork(artwork, owner.toLowerCase(Locale.ROOT).contains("spotify"));
            }
            if ((artwork == null || artwork.length == 0) && sameTrack) {
                artwork = previous.artwork();
            }
            this.current = new Snapshot(
                    title,
                    artist,
                    owner,
                    artwork == null || artwork.length == 0 ? new byte[0] : Arrays.copyOf(artwork, artwork.length),
                    wholeSeconds,
                    duration,
                    media.getPlaying(),
                    positionCapturedAt);
        } catch (Throwable error) {
            if (System.currentTimeMillis() - this.lastSessionAt > 1500L) {
                this.current = null;
            }
            Raven.LOGGER.debug("Unable to read operating-system media session", error);
        } finally {
            this.polling.set(false);
        }
    }

    private String spotifyArtists(String artist, String title) {
        String key = artist + "\n" + title;
        if (key.equals(this.spotifyKey) && this.spotifyArtists != null) {
            return this.spotifyArtists;
        }
        this.spotifyKey = key;
        this.spotifyArtists = null;
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return artist;
        }
        String suffix = " - " + title;
        String prefix = artist.toLowerCase(Locale.ROOT);
        User32.INSTANCE.EnumWindows((window, data) -> {
            char[] buffer = new char[512];
            int length = User32.INSTANCE.GetWindowText(window, buffer, buffer.length);
            String windowTitle = new String(buffer, 0, Math.max(0, length));
            if (!windowTitle.endsWith(suffix)) return true;
            String artists = windowTitle.substring(0, windowTitle.length() - suffix.length()).strip();
            if (!artists.toLowerCase(Locale.ROOT).startsWith(prefix)) return true;
            IntByReference pid = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(window, pid);
            boolean spotify = ProcessHandle.of(pid.getValue())
                    .flatMap(process -> process.info().command())
                    .map(command -> command.toLowerCase(Locale.ROOT).endsWith("spotify.exe"))
                    .orElse(false);
            if (!spotify) return true;
            this.spotifyArtists = artists;
            return false;
        }, null);
        return this.spotifyArtists != null ? this.spotifyArtists : artist;
    }

    private byte[] readArtwork(byte[] png, boolean spotify) {
        if (Arrays.equals(png, this.rawArtwork)) {
            return this.croppedArtwork;
        }
        byte[] result = new byte[0];
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
            if (image != null) {
                result = png;
                BufferedImage cover = spotify ? cropSpotifyCover(image) : image;
                if (cover != image) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    ImageIO.write(cover, "png", out);
                    result = out.toByteArray();
                }
            }
        } catch (Exception error) {
            Raven.LOGGER.debug("Unable to read media artwork", error);
        }
        if (result.length > 0) {
            this.rawArtwork = png;
            this.croppedArtwork = result;
        }
        return result;
    }

    private static BufferedImage cropSpotifyCover(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        int background = image.getRGB(0, 0);
        int top = -1;
        int bottom = -1;
        int left = w;
        int right = -1;
        for (int y = 0; y < h; y++) {
            int rowLeft = -1;
            int rowRight = -1;
            for (int x = 0; x < w; x++) {
                if (differs(image.getRGB(x, y), background)) {
                    if (rowLeft < 0) rowLeft = x;
                    rowRight = x;
                }
            }
            if (rowLeft < 0) {
                if (top >= 0) break;
                continue;
            }
            if (top < 0) top = y;
            bottom = y;
            left = Math.min(left, rowLeft);
            right = Math.max(right, rowRight);
        }
        if (top < 0) {
            return image;
        }
        int size = right - left + 1;
        if (size < w / 4 || (size >= w - 2 && bottom - top >= h - 3)) {
            return image;
        }
        return image.getSubimage(left, top, size, Math.min(size, h - top));
    }

    private static boolean differs(int a, int b) {
        int dr = Math.abs((a >> 16 & 0xFF) - (b >> 16 & 0xFF));
        int dg = Math.abs((a >> 8 & 0xFF) - (b >> 8 & 0xFF));
        int db = Math.abs((a & 0xFF) - (b & 0xFF));
        int da = Math.abs((a >>> 24) - (b >>> 24));
        return dr + dg + db + da > 24;
    }

    private static boolean hasUsefulMetadata(IMediaSession session) {
        try {
            MediaInfo media = session.getMedia();
            return media != null && (!clean(media.getTitle(), "").isEmpty()
                    || !clean(media.getArtist(), "").isEmpty());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private IMediaSession selectSession(List<IMediaSession> sessions, boolean forControl) {
        if (sessions == null) {
            return null;
        }
        IMediaSession fallback = null;
        IMediaSession playing = null;
        IMediaSession sameOwner = null;
        IMediaSession selected = null;
        for (IMediaSession session : sessions) {
            if (!hasUsefulMetadata(session)) {
                continue;
            }
            if (fallback == null) fallback = session;
            try {
                MediaInfo media = session.getMedia();
                String owner = clean(session.getOwner(), "");
                boolean exact = !owner.isEmpty() && owner.equals(this.selectedOwner)
                        && clean(media.getTitle(), "").equals(this.selectedTitle)
                        && clean(media.getArtist(), "").equals(this.selectedArtist);
                if (exact) {
                    selected = session;
                    if (forControl || media.getPlaying()) return session;
                }
                if (media.getPlaying()) {
                    if (playing == null) playing = session;
                    if (owner.equals(this.selectedOwner) && !owner.isEmpty()) {
                        if (sameOwner == null) sameOwner = session;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        if (sameOwner != null) return sameOwner;
        if (!forControl && !this.selectedOwner.isEmpty()
                && System.currentTimeMillis() - this.lastSessionAt < 1200L) {
            return null;
        }
        return playing != null ? playing : selected != null ? selected : fallback;
    }

    public enum Control {
        PLAY_PAUSE, NEXT, PREVIOUS
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.strip();
    }

    public record Snapshot(String title, String artist, String owner, byte[] artwork,
                           long position, long duration, boolean playing, long capturedAt) {
        public double interpolatedPosition(long now) {
            double result = position + (playing ? Math.max(0L, now - capturedAt) / 1000.0 : 0.0);
            return duration > 0L ? Math.min(result, duration) : result;
        }

        public boolean isStream() {
            return duration <= 0L;
        }
    }
}
