package sanch3zss.raven.client.platform;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.config.ConfigAutoSaveScheduler;
import sanch3zss.raven.ui.notification.NotificationType;

public final class FilePicker {
    private static final AtomicBoolean OPEN = new AtomicBoolean();
    private static volatile String lastDirectory = "";

    private FilePicker() {
    }

    public static void choose(String title, String filterName, List<String> extensions, String selectedMessage,
                              Consumer<String> selected) {
        if (!OPEN.compareAndSet(false, true)) return;
        Thread picker = new Thread(() -> {
            try {
                String path = openDialog(title, filterName, extensions);
                if (path != null && !path.isEmpty()) accept(new File(path), extensions, selectedMessage, selected);
            } catch (Throwable error) {
                reportError(error);
            } finally {
                OPEN.set(false);
            }
        }, "Raven file picker");
        picker.setDaemon(true);
        picker.start();
    }

    private static String openDialog(String title, String filterName, List<String> extensions) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer patterns = stack.mallocPointer(extensions.size());
            for (String extension : extensions) {
                patterns.put(stack.UTF8("*." + extension));
            }
            patterns.flip();
            String patternList = String.join(";", extensions.stream().map(extension -> "*." + extension).toList());
            return TinyFileDialogs.tinyfd_openFileDialog(title, lastDirectory, patterns,
                    filterName + " (" + patternList + ")", false);
        }
    }

    private static boolean matches(String name, List<String> extensions) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        return extensions.stream().anyMatch(extension -> lowerName.endsWith("." + extension));
    }

    private static void accept(File file, List<String> extensions, String selectedMessage, Consumer<String> selected) {
        File directory = file.getParentFile();
        if (directory != null) {
            lastDirectory = directory.getAbsolutePath() + File.separator;
        }
        Mc.INSTANCE.getMinecraft().execute(() -> {
            if (!file.isFile() || !matches(file.getName(), extensions)) {
                Raven.INSTANCE.notificationRepository().post(NotificationType.WARNING,
                        Component.literal("Неподходящий тип файла."), 3000L);
                return;
            }
            selected.accept(file.getAbsolutePath());
            ConfigAutoSaveScheduler.scheduleAutoSave();
            Raven.INSTANCE.notificationRepository().post(NotificationType.SUCCESS,
                    Component.literal(selectedMessage), 3000L);
        });
    }

    private static void reportError(Throwable error) {
        Raven.LOGGER.error("Unable to open file picker", error);
        Mc.INSTANCE.getMinecraft().execute(() ->
                Raven.INSTANCE.notificationRepository().post(NotificationType.ERROR,
                        Component.literal("Не удалось открыть выбор файла. Проверьте лог."), 4000L));
    }
}
