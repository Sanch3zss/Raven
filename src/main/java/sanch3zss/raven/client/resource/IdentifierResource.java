package sanch3zss.raven.client.resource;

import java.io.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

public class IdentifierResource implements ResourceSource {
    public final Identifier identifier;

    public IdentifierResource(Identifier identifier) {
        this.identifier = identifier;
    }

    @Override
    public InputStream stream() {
        try {
            return ((Resource) Minecraft.getInstance().getResourceManager().getResource(this.identifier).orElseThrow(() -> {
                return new IllegalStateException("Can't find resource for " + String.valueOf(this.identifier));
            })).open();
        } catch (IOException e) {
            throw new IllegalStateException("Error reading resource for " + String.valueOf(this.identifier), e);
        }
    }
}
