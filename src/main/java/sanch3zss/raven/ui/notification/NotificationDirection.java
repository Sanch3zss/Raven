package sanch3zss.raven.ui.notification;

import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.client.i18n.Lang;

public enum NotificationDirection implements DisplayNamed {
    AUTO(Lang.WIDGET_NOTIFICATIONS_DIRECTION_AUTO),
    UPWARD(Lang.WIDGET_NOTIFICATIONS_DIRECTION_UPWARD),
    DOWNWARD(Lang.WIDGET_NOTIFICATIONS_DIRECTION_DOWNWARD);

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    NotificationDirection(Translation translation) {
        this.displayName = translation;
    }
}
