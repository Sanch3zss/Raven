package sanch3zss.raven.client.service;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Psapi;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.ui.notification.NotificationType;

public final class MemoryTrimmer {
    private static final long MB = 1024L * 1024L;
    private static final long BASE_COOLDOWN = TimeUnit.MINUTES.toMillis(3);
    private static final long MAX_COOLDOWN = TimeUnit.MINUTES.toMillis(30);
    private static final Set<String> PROTECTED = Set.of("dwm.exe", "audiodg.exe", "csrss.exe",
            "explorer.exe", "lsass.exe", "winlogon.exe", "smss.exe", "wininit.exe", "services.exe");

    private ScheduledFuture<?> task;
    private int pressuredChecks;
    private long cooldown = BASE_COOLDOWN;
    private long nextTrimAt;

    public void start(ScheduledExecutorService executor) {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) return;
        this.task = executor.scheduleWithFixedDelay(this::check, 30L, 5L, TimeUnit.SECONDS);
    }

    private void check() {
        try {
            WinBase.MEMORYSTATUSEX memory = memoryStatus();
            long total = memory.ullTotalPhys.longValue();
            long available = memory.ullAvailPhys.longValue();
            long threshold = Math.clamp(total * 15L / 100L, 1024L * MB, 2560L * MB);
            if (available >= threshold) {
                this.pressuredChecks = 0;
                return;
            }
            if (++this.pressuredChecks < 3 || System.currentTimeMillis() < this.nextTrimAt) return;
            this.pressuredChecks = 0;
            trimOtherProcesses();
            long freed = Math.max(0L, memoryStatus().ullAvailPhys.longValue() - available) / MB;
            this.cooldown = freed >= 256L ? BASE_COOLDOWN : Math.min(MAX_COOLDOWN, this.cooldown * 2L);
            this.nextTrimAt = System.currentTimeMillis() + this.cooldown;
            Raven.LOGGER.info("Memory trim freed {} MB, next check in {} min", freed, this.cooldown / 60000L);
            if (freed >= 256L) {
                Minecraft.getInstance().execute(() -> Raven.INSTANCE.notificationRepository().post(NotificationType.INFO,
                        Component.literal("Освобождено " + freed + " МБ оперативной памяти"), 3L, TimeUnit.SECONDS));
            }
        } catch (Throwable error) {
            Raven.LOGGER.warn("Memory trimming disabled", error);
            if (this.task != null) this.task.cancel(false);
        }
    }

    private static WinBase.MEMORYSTATUSEX memoryStatus() {
        WinBase.MEMORYSTATUSEX memory = new WinBase.MEMORYSTATUSEX();
        Kernel32.INSTANCE.GlobalMemoryStatusEx(memory);
        return memory;
    }

    private static void trimOtherProcesses() {
        int[] pids = new int[4096];
        IntByReference needed = new IntByReference();
        if (!Psapi.INSTANCE.EnumProcesses(pids, pids.length * 4, needed)) return;
        int self = Kernel32.INSTANCE.GetCurrentProcessId();
        int count = needed.getValue() / 4;
        for (int i = 0; i < count; i++) {
            int pid = pids[i];
            if (pid == 0 || pid == 4 || pid == self) continue;
            WinNT.HANDLE process = Kernel32.INSTANCE.OpenProcess(0x1000 | 0x0100, false, pid);
            if (process == null) continue;
            try {
                if (!PROTECTED.contains(imageName(process))) {
                    WorkingSet.INSTANCE.EmptyWorkingSet(process);
                }
            } finally {
                Kernel32.INSTANCE.CloseHandle(process);
            }
        }
    }

    private static String imageName(WinNT.HANDLE process) {
        char[] buffer = new char[1024];
        IntByReference length = new IntByReference(buffer.length);
        if (!Kernel32.INSTANCE.QueryFullProcessImageName(process, 0, buffer, length)) return "";
        String path = new String(buffer, 0, length.getValue());
        return path.substring(path.lastIndexOf('\\') + 1).toLowerCase(Locale.ROOT);
    }

    private interface WorkingSet extends StdCallLibrary {
        WorkingSet INSTANCE = Native.load("psapi", WorkingSet.class, W32APIOptions.DEFAULT_OPTIONS);

        boolean EmptyWorkingSet(WinNT.HANDLE process);
    }
}
