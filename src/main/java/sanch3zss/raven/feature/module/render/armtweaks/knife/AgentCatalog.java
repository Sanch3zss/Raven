package sanch3zss.raven.feature.module.render.armtweaks.knife;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import sanch3zss.raven.client.Raven;

final class AgentCatalog {
    static final String ROOT = "/assets/raven/knives/cs2/agents/";
    private static AgentCatalog instance;
    private final Map<String, Agent> agents = new HashMap<>();
    private final Map<String, Identifier> textures = new HashMap<>();
    private final Set<String> failed = new HashSet<>();

    private AgentCatalog() {
        try (InputStream input = AgentCatalog.class.getResourceAsStream(ROOT + "agents.json")) {
            if (input == null) return;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                JsonObject json = entry.getValue().getAsJsonObject();
                agents.put(entry.getKey(), new Agent(json.get("hands").getAsString(),
                        json.has("sleeves") ? json.get("sleeves").getAsString() : null, json.get("arm").getAsString(),
                        json.has("body") ? json.get("body").getAsString() : null));
            }
        } catch (Exception exception) {
            Raven.LOGGER.error("Failed to load CS2 agents", exception);
        }
    }

    static AgentCatalog get() {
        if (instance == null) instance = new AgentCatalog();
        return instance;
    }

    Agent agent(String id) {
        return id == null ? null : agents.get(id);
    }

    Identifier texture(String material) {
        Identifier texture = textures.get(material);
        if (texture != null || failed.contains(material)) return texture;
        try (InputStream input = AgentCatalog.class.getResourceAsStream(ROOT + "textures/" + material + ".jpg")) {
            if (input == null) throw new IllegalStateException(material);
            BufferedImage sheet = ImageIO.read(input);
            int width = sheet.getWidth() / 2;
            int height = sheet.getHeight();
            NativeImage image = new NativeImage(width * 2, height, false);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    image.setPixel(x, y, 0xFF000000 | sheet.getRGB(x, y));
                    int detail = sheet.getRGB(x + width, y);
                    float nx = (detail >> 16 & 255) / 127.5f - 1.0f;
                    float ny = (detail >> 8 & 255) / 127.5f - 1.0f;
                    float nz = (float) Math.sqrt(Math.max(0.0f, 1.0f - nx * nx - ny * ny));
                    int encodedZ = Math.round((nz * 0.5f + 0.5f) * 255.0f);
                    image.setPixel(x + width, y, (detail & 255) << 24 | detail & 0xFFFF00 | encodedZ);
                }
            }
            texture = Identifier.fromNamespaceAndPath("raven", "agent/" + material);
            PaintedTexture uploaded = new PaintedTexture(texture.toString(), image);
            image.close();
            Minecraft.getInstance().getTextureManager().register(texture, uploaded);
            textures.put(material, texture);
            return texture;
        } catch (Exception exception) {
            failed.add(material);
            Raven.LOGGER.error("Failed to load CS2 agent texture {}", material, exception);
            return null;
        }
    }

    record Agent(String hands, String sleeves, String arm, String body) {
    }
}
