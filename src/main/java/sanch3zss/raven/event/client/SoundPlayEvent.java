package sanch3zss.raven.event.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import sanch3zss.raven.event.CancellableEvent;

public class SoundPlayEvent extends CancellableEvent {
    public final SoundInstance soundInstance;
    public float volumeMultiplier = 1.0f;

    public SoundInstance getSoundInstance() {
        return this.soundInstance;
    }

    public float getVolumeMultiplier() {
        return this.volumeMultiplier;
    }

    public void setVolumeMultiplier(float f) {
        this.volumeMultiplier = f;
    }

    public SoundPlayEvent(SoundInstance soundInstance) {
        this.soundInstance = soundInstance;
    }
}
