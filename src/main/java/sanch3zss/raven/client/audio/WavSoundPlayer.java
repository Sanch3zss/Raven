package sanch3zss.raven.client.audio;

import java.io.*;
import java.util.*;
import javax.sound.sampled.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import sanch3zss.raven.client.chat.ChatUtil;

public class WavSoundPlayer {
    public static final WavSoundPlayer INSTANCE = new WavSoundPlayer();
    public Clip currentClip;

    public void playCustomSound(String path, float volume) {
        if (path == null || path.isBlank()) return;
        Thread playback = new Thread(() -> {
            try (AudioInputStream stream = AudioSystem.getAudioInputStream(new File(path))) {
                Clip clip = AudioSystem.getClip();
                clip.open(stream);
                setVolume(clip, volume);
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) clip.close();
                });
                clip.start();
            } catch (UnsupportedAudioFileException | LineUnavailableException | IOException error) {
                Minecraft.getInstance().execute(() ->
                        ChatUtil.addChatMessage("Не удалось воспроизвести звук: " + error.getMessage()));
            }
        }, "Raven custom sound");
        playback.setDaemon(true);
        playback.start();
    }

    public synchronized void playSound(String str, float f, boolean z) {
        new Thread(() -> {
            try {
                Clip clip = AudioSystem.getClip();
                BufferedInputStream bufferedInputStream = new BufferedInputStream(((Resource) Minecraft.getInstance().getResourceManager().getResource(Identifier.fromNamespaceAndPath("raven", "sounds/" + str + ".wav")).orElseThrow(() -> {
                    return new IOException("Sound resource not found: " + str);
                })).open());
                try {
                    AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bufferedInputStream);
                    try {
                        clip.open(audioInputStream);
                        setVolume(clip, f);
                        clip.start();
                        if (z) {
                            clip.loop(-1);
                        }
                        synchronized (this) {
                            try {
                                this.currentClip = clip;
                            } catch (Throwable th) {
                                throw th;
                            }
                        }
                        if (audioInputStream != null) {
                            audioInputStream.close();
                        }
                        bufferedInputStream.close();
                    } catch (Throwable th2) {
                        if (audioInputStream != null) {
                            try {
                                audioInputStream.close();
                            } catch (Throwable th3) {
                                th2.addSuppressed(th3);
                            }
                        }
                        throw th2;
                    }
                } catch (Throwable th4) {
                    try {
                        bufferedInputStream.close();
                    } catch (Throwable th5) {
                        th4.addSuppressed(th5);
                    }
                    throw th4;
                }
            } catch (UnsupportedAudioFileException | LineUnavailableException | IOException e) {
                ChatUtil.addChatMessage("Ошибка воспроизведения звука: " + e.getMessage());
            }
        }).start();
    }

    public synchronized void playSoundSequentially(List<String> list, float f, long j) {
        new Thread(() -> {
            for (String str : list) {
                try {
                    Clip clip = AudioSystem.getClip();
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(((Resource) Minecraft.getInstance().getResourceManager().getResource(Identifier.fromNamespaceAndPath("raven", "sounds/" + str + ".wav")).orElseThrow(() -> {
                        return new IOException("Sound resource not found: " + str);
                    })).open());
                    try {
                        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bufferedInputStream);
                        try {
                            clip.open(audioInputStream);
                            setVolume(clip, f);
                            clip.start();
                            Thread.sleep(j);
                            clip.close();
                            if (audioInputStream != null) {
                                audioInputStream.close();
                            }
                            bufferedInputStream.close();
                        } catch (Throwable th) {
                            if (audioInputStream != null) {
                                try {
                                    audioInputStream.close();
                                } catch (Throwable th2) {
                                    th.addSuppressed(th2);
                                }
                            }
                            throw th;
                        }
                    } catch (Throwable th3) {
                        try {
                            bufferedInputStream.close();
                        } catch (Throwable th4) {
                            th3.addSuppressed(th4);
                        }
                        throw th3;
                    }
                } catch (UnsupportedAudioFileException | LineUnavailableException | IOException | InterruptedException e) {
                    ChatUtil.addChatMessage("Ошибка воспроизведения звука: " + str + " - " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    public synchronized void stopSound() {
        if (this.currentClip != null) {
            this.currentClip.stop();
            this.currentClip.close();
            this.currentClip = null;
        }
    }

    public void setVolume(Clip clip, float f) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
        FloatControl control = (FloatControl) (clip.getControl(FloatControl.Type.MASTER_GAIN));
        float minimum = control.getMinimum();
        control.setValue(minimum + ((control.getMaximum() - minimum) * (f / 100.0f)));
    }
}
