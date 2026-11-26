package sanch3zss.raven.client.config.repository;

import java.util.List;
import java.util.concurrent.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.render.animation.AnimatedFloat;
import sanch3zss.raven.ui.hud.impl.Notification;
import sanch3zss.raven.ui.notification.NotificationType;
import sanch3zss.raven.util.math.interpolation.Easings;

public class NotificationRepository {
    private static final int MAX_VISIBLE_NOTIFICATIONS = 5;
    public final List<Notification> notifications = new CopyOnWriteArrayList<>();

    public void post(NotificationType notificationType, Component text, long j, TimeUnit timeUnit) {
        long millis = timeUnit.toMillis(j);
        AnimatedFloat animation = new AnimatedFloat(320, Easings.EASE_IN_OUT_CUBIC);
        animation.destination(1.0f);
        this.notifications.add(new Notification(animation, notificationType, text, System.currentTimeMillis(), millis, null));
        dismissOverflow();
    }

    public void post(NotificationType notificationType, Component text, long j) {
        post(notificationType, text, j, TimeUnit.MILLISECONDS);
    }

    public void post(Component text, ItemStack itemStack, long j, TimeUnit timeUnit) {
        long millis = timeUnit.toMillis(j);
        AnimatedFloat animation = new AnimatedFloat(320, Easings.EASE_IN_OUT_CUBIC);
        animation.destination(1.0f);
        this.notifications.add(new Notification(animation, NotificationType.INFO, text, System.currentTimeMillis(), millis, itemStack.copy()));
        dismissOverflow();
    }

    public void post(Component text, ItemStack itemStack, long j) {
        post(text, itemStack, j, TimeUnit.MILLISECONDS);
    }

    public void tick() {
        this.notifications.forEach(notification -> {
            if (notification.isExpired()) {
                notification.dismiss();
            }
        });
        this.notifications.removeIf(notification -> notification.valueAnimation().destination() <= 0.0f
                && notification.valueAnimation().animatedValue() <= 0.01f);
    }

    public void animate(WeightedEngine weightedEngine) {
        this.notifications.forEach(notification -> {
            notification.valueAnimation().animate(weightedEngine);
        });
    }

    public List<Notification> getNotifications() {
        return this.notifications;
    }

    private void dismissOverflow() {
        int visibleCount = 0;
        for (Notification notification : this.notifications) {
            if (notification.valueAnimation().destination() > 0.0f) {
                visibleCount++;
            }
        }
        if (visibleCount <= MAX_VISIBLE_NOTIFICATIONS) {
            return;
        }
        for (Notification notification : this.notifications) {
            if (visibleCount <= MAX_VISIBLE_NOTIFICATIONS) {
                break;
            }
            if (notification.valueAnimation().destination() > 0.0f) {
                notification.dismiss();
                visibleCount--;
            }
        }
    }
}
