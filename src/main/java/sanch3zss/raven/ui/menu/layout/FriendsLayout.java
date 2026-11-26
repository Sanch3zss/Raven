package sanch3zss.raven.ui.menu.layout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.lwjgl.opengl.GL33;

import sanch3zss.raven.client.input.InputEventContext;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.feature.friend.FriendEntry;
import sanch3zss.raven.feature.friend.FriendManager;
import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.gl.FramebufferUtil;
import sanch3zss.raven.render.texture.ItemSpriteManager;
import sanch3zss.raven.ui.component.control.ClickableBehavior;
import sanch3zss.raven.ui.component.input.TextInputField;
import sanch3zss.raven.ui.menu.MenuStyle;

public final class FriendsLayout extends ListPageLayout<FriendEntry> {
    private final TextInputField nickField = new TextInputField(this.textFont, 13).maxLength(16);
    private final ClickableBehavior addButton = new ClickableBehavior().clickCallback(this::addFromField);
    private final Map<String, ClickableBehavior> suggestionButtons = new HashMap<>();
    private List<String> suggestions = List.of();

    public FriendsLayout() {
        super("Друзья", "Пока нет друзей, добавь первого");
    }

    @Override
    protected List<FriendEntry> entries() {
        return FriendManager.entries();
    }

    @Override
    protected String key(FriendEntry entry) {
        return entry.name();
    }

    @Override
    protected String searchText(FriendEntry entry) {
        return entry.alias() == null ? entry.name() : entry.name() + " " + entry.alias();
    }

    @Override
    protected float composerHeight() {
        return 40.0f;
    }

    @Override
    protected void drawComposer(DrawCtx drawCtx, float x, float y, float width) {
        MenuStyle.drawTextInput(drawCtx, this.nickField, this.textFont, this.nickField.text(), "Ник игрока",
                x, y + 8.0f, 170.0f, 24.0f);
        drawTextButton(drawCtx, this.addButton, "Добавить", x + 178.0f, y + 8.0f, 80.0f);
        this.suggestions = onlineSuggestions();
        float chipX = x + 270.0f;
        float limit = x + width;
        for (String name : this.suggestions) {
            float chipWidth = drawCtx.textWidthPhysical(this.textFont, name, 11) + 18.0f;
            if (chipX + chipWidth > limit) {
                break;
            }
            ClickableBehavior chip = this.suggestionButtons.computeIfAbsent(name,
                    n -> new ClickableBehavior().clickCallback(() -> addFriend(n)));
            float hover = chip.hoverAnimation().smoothAnimation();
            MenuStyle.drawSettingButton(drawCtx, chipX, y + 8.0f, chipWidth, 24.0f, 5.0f, hover);
            drawCtx.text(this.textFont, name, 11, chipX + 9.0f, textY(this.textFont, 11, y + 20.0f),
                    drawCtx.colorStack().interpolate(secondaryColor(drawCtx), textColor(drawCtx), hover));
            chip.setDimensions(chipX, y + 8.0f, chipWidth, 24.0f);
            chipX += chipWidth + 6.0f;
        }
        this.suggestionButtons.keySet().retainAll(this.suggestions);
    }

    @Override
    protected void layoutComposer(LayoutScaleContext context, float x, float y, float width) {
        this.nickField.listen(x, y + 8.0f, 170.0f, 24.0f, 158.0f, x + 6.0f, context.scaleFactor());
    }

    @Override
    protected void animateComposer(WeightedEngine engine) {
        this.nickField.animate(engine);
        this.addButton.animate(engine);
        this.suggestionButtons.values().forEach(chip -> chip.animate(engine));
    }

    @Override
    protected boolean handleComposerInput(InputEventContext context, boolean consumed) {
        boolean handled = consumed;
        handled |= this.nickField.handleInput(context, handled);
        handled |= this.addButton.handleInput(context, handled);
        for (ClickableBehavior chip : new ArrayList<>(this.suggestionButtons.values())) {
            handled |= chip.handleInput(context, handled);
        }
        return handled;
    }

    @Override
    protected void setupRow(String key, Row row) {
        row.field = new TextInputField(this.textFont, 12).maxLength(24);
        FriendManager.entries().stream().filter(entry -> entry.name().equals(key)).findFirst()
                .map(FriendEntry::alias).ifPresent(row.field::setTextSilently);
        row.field.changeText(text -> FriendManager.setAlias(key, text.trim()));
        row.primary.clickCallback(() -> FriendManager.remove(key));
    }

    @Override
    protected void drawRow(DrawCtx drawCtx, FriendEntry entry, Row row, float x, float y, float width) {
        float centerY = y + ROW_HEIGHT / 2.0f;
        PlayerInfo info = playerInfo(entry.name());
        drawHead(drawCtx, info, x, centerY - 11.0f, 22.0f);
        float nameX = x + 32.0f;
        drawFittedText(drawCtx, this.titleFont, entry.name(), 12, nameX, centerY, 150.0f, textColor(drawCtx));
        float nameWidth = Math.min(150.0f, drawCtx.textWidthPhysical(this.titleFont, entry.name(), 12));
        drawCtx.circle(nameX + nameWidth + 8.0f, centerY, 3.0f, info != null
                ? drawCtx.colorStack().computeColor(0xFF34C759)
                : drawCtx.colorStack().computeColor(0xFFFFFFFF, 0.2f));
        float deleteX = x + width - 24.0f;
        float fieldWidth = 190.0f;
        float fieldX = deleteX - 10.0f - fieldWidth;
        row.field.listen(fieldX, centerY - 11.0f, fieldWidth, 22.0f, fieldWidth - 12.0f, fieldX + 6.0f,
                drawCtx.layoutContext().scaleFactor());
        MenuStyle.drawTextInput(drawCtx, row.field, this.textFont, 12, row.field.text(), "Свой ник для друга",
                fieldX, centerY - 11.0f, fieldWidth, 22.0f, 6.0f);
        drawIconButton(drawCtx, row.primary, "d", deleteX, centerY - 12.0f, dangerColor(drawCtx));
    }

    private void drawHead(DrawCtx drawCtx, PlayerInfo info, float x, float y, float size) {
        int textureId = info == null ? 0 : skinTexture(info);
        if (textureId <= 0) {
            drawCtx.fillRoundedRect(x, y, size, size, 6.0f, MenuStyle.hoverTint(drawCtx.colorStack(), 0.08f));
            return;
        }
        var layout = drawCtx.layoutContext();
        int slot = drawCtx.drawEngine().bindTexture(textureId);
        int color = drawCtx.colorStack().white();
        float radius = layout.toPhysical(6.0f);
        var face = ItemSpriteManager.INSTANCE.defaultFaceUv;
        drawCtx.drawEngine().roundedTexture(drawCtx.matrixStack().last().pose(),
                layout.toPhysical(x), layout.toPhysical(y), layout.toPhysical(size), layout.toPhysical(size),
                radius, radius, radius, radius, face.u0(), face.v0(), face.u1(), face.v1(),
                slot, color, color, color, color);
        drawCtx.drawEngine().roundedTexture(drawCtx.matrixStack().last().pose(),
                layout.toPhysical(x), layout.toPhysical(y), layout.toPhysical(size), layout.toPhysical(size),
                radius, radius, radius, radius, 0.625f, 0.125f, 0.75f, 0.25f,
                slot, color, color, color, color);
    }

    private static int skinTexture(PlayerInfo info) {
        AbstractTexture texture = Minecraft.getInstance().getTextureManager()
                .getTexture(info.getSkin().body().texturePath());
        int textureId = texture == null ? 0 : FramebufferUtil.getTextureId(texture);
        if (textureId > 0) {
            int previousTexture = GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);
            GL33.glBindTexture(GL33.GL_TEXTURE_2D, textureId);
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MIN_FILTER, GL33.GL_NEAREST);
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MAG_FILTER, GL33.GL_NEAREST);
            GL33.glBindTexture(GL33.GL_TEXTURE_2D, previousTexture);
        }
        return textureId;
    }

    private static PlayerInfo playerInfo(String name) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection == null ? null : connection.getPlayerInfo(name);
    }

    private List<String> onlineSuggestions() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return List.of();
        }
        String query = this.nickField.text().trim().toLowerCase(Locale.ROOT);
        String self = Minecraft.getInstance().getUser().getName();
        List<String> result = new ArrayList<>();
        for (PlayerInfo info : connection.getOnlinePlayers()) {
            String name = info.getProfile().name();
            if (name.equals(self) || FriendManager.isFriend(name)
                    || !query.isEmpty() && !name.toLowerCase(Locale.ROOT).startsWith(query)) {
                continue;
            }
            result.add(name);
        }
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    private void addFromField() {
        String name = this.nickField.text().trim();
        if (name.isEmpty()) {
            this.nickField.focused(true);
            return;
        }
        addFriend(name);
        this.nickField.clearText();
    }

    private void addFriend(String name) {
        if (!FriendManager.isFriend(name)) {
            FriendManager.add(name);
        }
    }

    @Override
    public void close() {
        super.close();
        this.nickField.focused(false);
    }
}
