package sanch3zss.raven.client.audio;

import java.util.List;
import java.util.function.Consumer;
import sanch3zss.raven.client.platform.FilePicker;

public final class CustomSoundPicker {
    private CustomSoundPicker() {
    }

    public static void choose(Consumer<String> selected) {
        FilePicker.choose("Выбрать звук WAV", "WAV-файлы", List.of("wav"), "Звук выбран.", selected);
    }
}
