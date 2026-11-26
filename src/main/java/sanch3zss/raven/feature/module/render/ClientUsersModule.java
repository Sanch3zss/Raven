package sanch3zss.raven.feature.module.render;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.server.RavenUserService;
import sanch3zss.raven.ui.menu.layout.ModuleTab;

@Aliases(aliases = {"ClientUsers", "Raven Users", "Users"})
public final class ClientUsersModule extends Module {
    public static final FontDescription.Resource ICON_FONT = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("raven", "client_icon"));

    private final RavenUserService service;

    public ClientUsersModule() {
        this(Raven.INSTANCE.ravenUserService);
    }

    public ClientUsersModule(RavenUserService service) {
        super(ModuleTab.VISUALS, "ClientUsers");
        this.service = service;
    }

    public boolean shouldMark(String username) {
        return isState() && this.service.contains(username);
    }

    public Component decorate(Component name) {
        int accent = Raven.INSTANCE.theme().palette().accent().argb() & 0xFFFFFF;
        return Component.empty()
                .append(Component.literal("a")
                        .setStyle(Style.EMPTY.withFont(ICON_FONT).withColor(accent)))
                .append(Component.literal(" "))
                .append(name.copy());
    }
}
