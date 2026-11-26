package sanch3zss.raven.client.resource;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.*;
import java.nio.channels.*;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

public class TextureResource implements ResourceSource {
    public final AbstractTexture texture;

    @Override
    public InputStream stream() {
        if ((this.texture) instanceof SimpleTexture resourceTexture ) {
            Identifier id = resourceTexture.resourceId();
            try {
                System.out.println("loaded resource texture");
                return openResourceStream(id);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load ResourceTexture: " + String.valueOf(id), e);
            }
        }
        DynamicTexture nativeImageBackedTexture = (DynamicTexture) (this.texture);
        if (!(nativeImageBackedTexture instanceof DynamicTexture)) {
            return null;
        }
        DynamicTexture nativeImageBackedTexture2 = nativeImageBackedTexture;
        try {
            System.out.println("loaded native image texture");
            return encodeNativeImage((NativeImage) Objects.requireNonNull(nativeImageBackedTexture2.getPixels()));
        } catch (IOException e2) {
            throw new IllegalStateException("Failed to process NativeImageBackedTexture", e2);
        }
    }

    public InputStream encodeNativeImage(NativeImage nativeImage) throws IOException {
        if (!nativeImage.format().supportedByStb()) {
            throw new UnsupportedOperationException("Image format is not writeable: " + String.valueOf(nativeImage.format()));
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        if (writeImage(nativeImage, Channels.newChannel(byteArrayOutputStream))) {
            return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        }
        throw new IOException("Failed to write NativeImage to PNG: " + String.valueOf(nativeImage));
    }

    public boolean writeImage(NativeImage nativeImage, WritableByteChannel writableByteChannel) throws IOException {
        return nativeImage.writeToChannel(writableByteChannel);
    }

    public InputStream openResourceStream(Identifier identifier) throws IOException {
        return ((Resource) Minecraft.getInstance().getResourceManager().getResource(identifier).orElseThrow(() -> {
            return new IllegalStateException("Resource not found: " + String.valueOf(identifier));
        })).open();
    }

    public TextureResource(AbstractTexture abstractTexture) {
        this.texture = abstractTexture;
    }
}
