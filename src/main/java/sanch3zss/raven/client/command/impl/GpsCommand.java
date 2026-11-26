package sanch3zss.raven.client.command.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.io.IOException;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.TranslatedException;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.command.ClientCommand;
import sanch3zss.raven.client.command.CommandContext;
import sanch3zss.raven.client.config.LocalPreferencesFile;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.event.render.Render2DEvent;
import sanch3zss.raven.client.chat.ChatUtil;
import sanch3zss.raven.render.engine.DrawEngine;
import sanch3zss.raven.render.engine.ScreenResolution;
import sanch3zss.raven.render.font.Fonts;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.util.math.MathUtil;

public class GpsCommand implements ClientCommand {
    private static final float SCALE = 0.4f;
    private static final int TEXTURE_SIZE = 85;
    private final GlTexture arrowTexture = new GlTexture(new ClasspathResource("/textures/arrow.png"))
            .generateMipMap(true)
            .minFilter(9987)
            .magFilter(9729)
            .wrapX(33071)
            .wrapY(33071)
            .anisotropy(16.0f);
    private int gpsX;
    private int gpsZ;
    private String gpsName = "";
    private boolean enabled;

    public GpsCommand() {
        LocalPreferencesFile preferences = Raven.INSTANCE.configManager().menuStateConfig();
        if (preferences.hasGpsTarget()) {
            this.gpsX = preferences.gpsX();
            this.gpsZ = preferences.gpsZ();
            this.gpsName = preferences.gpsName();
            this.enabled = true;
        }
        Raven.INSTANCE.eventDispatcher().register(Render2DEvent.class, this::renderGps);
    }

    @Override
    public String getName() {
        return "gps";
    }

    @Override
    public Translation getDescription() {
        return Lang.COMMAND_GPS_DESC;
    }

    @Override
    public String getUsage() {
        return ".gps <x> <z>\n.gps off";
    }

    @Override
    public List<String> getAliases() {
        return List.of();
    }

    @Override
    public void execute(CommandContext commandContext) throws TranslatedException {
        String[] args = commandContext.args();
        if (!Mc.INSTANCE.isWorldLoaded()) {
            throw new TranslatedException(Lang.COMMAND_WORLD_NOT_LOADED);
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("off")) {
            enabled = false;
            Raven.INSTANCE.configManager().menuStateConfig().disableGps();
            saveGps();
            ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_GPS_DISABLED.effective()).withStyle(ChatFormatting.GRAY));
            return;
        }
        if (args.length != 2) {
            throw new TranslatedException(Translation.clearText(Lang.COMMAND_INVALID_ARG_COUNT.effective()
                    .replace("{usage}", getUsage())));
        }
        setTarget(parseCoordinate(args[0], Mc.INSTANCE.getPlayer().getX()),
                parseCoordinate(args[1], Mc.INSTANCE.getPlayer().getZ()), "");
        ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_GPS_ENABLED.effective()
                .replace("{x}", String.valueOf(gpsX))
                .replace("{z}", String.valueOf(gpsZ))).withStyle(ChatFormatting.GRAY));
    }

    public void setTarget(int x, int z, String name) {
        this.gpsX = x;
        this.gpsZ = z;
        this.gpsName = name == null ? "" : name;
        this.enabled = true;
        Raven.INSTANCE.configManager().menuStateConfig().setGpsTarget(x, z, this.gpsName);
        saveGps();
    }

    private void saveGps() {
        try {
            Raven.INSTANCE.configManager().saveMenuState();
        } catch (IOException exception) {
            Raven.LOGGER.error("Failed to save GPS target", exception);
        }
    }

    private int parseCoordinate(String value, double current) throws TranslatedException {
        if (value.equals("~")) {
            return (int) Math.floor(current);
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new TranslatedException(Translation.clearText(Lang.COMMAND_INVALID_COORDINATE.effective()
                    .replace("{input}", value)));
        }
    }

    private void renderGps(Render2DEvent event) {
        Mc mc = Mc.INSTANCE;
        if (!enabled || !event.isPost() || !mc.isWorldLoaded()) {
            return;
        }
        Vec3 cameraPosition = mc.getCamera().position();
        double x = (gpsX - cameraPosition.x) * 0.01d;
        double z = (gpsZ - cameraPosition.z) * 0.01d;
        double yaw = Math.toRadians(mc.getCamera().yRot());
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        float angle = (float) Math.toDegrees(Math.atan2(-((z * cos) - (x * sin)), -((x * cos) + (z * sin))));
        double distance = new Vec3(gpsX, mc.getPlayer().getY(), gpsZ)
                .subtract(mc.getPlayer().position()).horizontalDistance();
        if (distance <= 0.0d) {
            return;
        }
        ScreenResolution resolution = ScreenResolution.resolution();
        float centerX = resolution.screenWidth() / 2.0f;
        float centerY = mc.getWindow().getGuiScaledHeight() / 4.0f;
        int halfSize = TEXTURE_SIZE / 2;
        PoseStack matrices = event.matrixStack();
        Matrix4f screenMatrix = new Matrix4f(matrices.last().pose());
        DrawEngine drawEngine = Raven.INSTANCE.drawEngine();
        drawEngine.begin();
        matrices.pushPose();
        matrices.translate(centerX, centerY, 0.0f);
        matrices.pushPose();
        matrices.scale(SCALE, SCALE, 1.0f);
        matrices.mulPose(Axis.ZP.rotationDegrees(angle));
        drawEngine.texture(matrices.last().pose(), -halfSize, -halfSize, TEXTURE_SIZE, TEXTURE_SIZE,
                drawEngine.bindTexture(arrowTexture.textureWithSTB()), -1);
        matrices.popPose();
        Matrix4f distanceMatrix = screenMatrix.translate(centerX, centerY, 0.0f);
        drawEngine.msdfFontHorizontalC(distanceMatrix, Fonts.INTER_EXTRA_BOLD.get(),
                "%sm".formatted(MathUtil.decimalFormat(distance, 0)), 0.0f, 23.0f * SCALE, 12.0f, 0.0f, -1);
        matrices.popPose();
        drawEngine.end();
    }

    @Override
    public List<String> getSuggestions(String[] args, int index) {
        return index == 0 ? List.of("off", "~", "<x>")
                : index == 1 ? List.of("~", "<z>") : List.of();
    }
}
