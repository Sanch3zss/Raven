package sanch3zss.raven.feature.module.misc;

import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.event.network.ChatSendEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.*;

@Aliases(aliases = {"Protect Name", "Name Protect", "Hide name", "Obfuscation"})
public class NameProtectModule extends Module {
    public final TextFieldSetting changedName;

    public NameProtectModule() {
        super(ModuleTab.MISC, "NameProtect");
        this.changedName = new TextFieldSetting(Lang.NAMEPROTECT_CHANGEDNAME).setMax(16).setPlaceholder(Lang.NAMEPROTECT_CHANGEDNAME_PLACEHOLDER).setText("sanch3zss");
        addSettings(this.changedName);
        register(ChatSendEvent.class, event -> {
            Mc mc = Mc.INSTANCE;
            if (isState() && mc.isWorldLoaded()) {
                String originalText = event.getOriginalText();
                String strReplace = originalText.replace(mc.getSession().getName(), this.changedName.getText());
                if (strReplace.equals(originalText)) {
                    return;
                }
                event.setChangedText(strReplace);
                event.cancel();
            }
        });
    }

    public String replace(String str) {
        if (!isState()) {
            return str;
        }
        return str.replace(Mc.INSTANCE.getSession().getName(), this.changedName.getText());
    }
}
