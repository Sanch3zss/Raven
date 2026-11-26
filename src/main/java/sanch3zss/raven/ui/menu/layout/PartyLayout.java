package sanch3zss.raven.ui.menu.layout;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.input.InputEventContext;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.feature.module.render.party.PartyService;
import sanch3zss.raven.feature.module.render.party.PartyService.Member;
import sanch3zss.raven.feature.module.render.party.PartyService.Position;
import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.ui.component.control.ClickableBehavior;
import sanch3zss.raven.ui.component.input.TextInputField;
import sanch3zss.raven.ui.menu.MenuStyle;

public final class PartyLayout extends ListPageLayout<Member> {
    private final TextInputField codeField = new TextInputField(this.textFont, 13).maxLength(6);
    private final ClickableBehavior createButton = new ClickableBehavior().clickCallback(() -> party().create());
    private final ClickableBehavior joinButton = new ClickableBehavior().clickCallback(this::join);
    private final ClickableBehavior copyButton = new ClickableBehavior().clickCallback(this::copyCode);
    private final ClickableBehavior leaveButton = new ClickableBehavior().clickCallback(() -> party().leave());
    private final ClickableBehavior disbandButton = new ClickableBehavior().clickCallback(() -> party().disband());
    private long copiedAt;

    public PartyLayout() {
        super("Party", "Создай Party или войди по коду, участники появятся здесь");
    }

    private static PartyService party() {
        return Raven.INSTANCE.partyService();
    }

    @Override
    protected List<Member> entries() {
        return List.copyOf(party().members());
    }

    @Override
    protected String key(Member entry) {
        return entry.id();
    }

    @Override
    protected boolean searchable() {
        return false;
    }

    @Override
    protected float composerHeight() {
        return 40.0f;
    }

    @Override
    protected void drawComposer(DrawCtx drawCtx, float x, float y, float width) {
        PartyService party = party();
        String code = party.code();
        float centerY = y + 20.0f;
        if (code == null) {
            drawTextButton(drawCtx, this.createButton, "Создать Party", x, y + 8.0f, 120.0f);
            drawCtx.text(this.textFont, "или", 11, x + 132.0f, textY(this.textFont, 11, centerY),
                    secondaryColor(drawCtx));
            MenuStyle.drawTextInput(drawCtx, this.codeField, this.textFont, this.codeField.text(), "Код Party",
                    x + 158.0f, y + 8.0f, 120.0f, 24.0f);
            drawTextButton(drawCtx, this.joinButton, "Войти", x + 286.0f, y + 8.0f, 80.0f);
            if (party.isPending()) {
                drawStatus(drawCtx, "Подключение...", x + width, centerY);
            }
            return;
        }
        drawCtx.text(this.textFont, "Код", 11, x, textY(this.textFont, 11, centerY), secondaryColor(drawCtx));
        drawCtx.text(this.titleFont, code, 13, x + 26.0f, textY(this.titleFont, 13, centerY), textColor(drawCtx));
        float codeWidth = drawCtx.textWidthPhysical(this.titleFont, code, 13);
        boolean copied = System.currentTimeMillis() - this.copiedAt < 1500L;
        drawTextButton(drawCtx, this.copyButton, copied ? "Скопирован" : "Копировать", x + 36.0f + codeWidth,
                y + 8.0f, 100.0f);
        float right = x + width;
        drawTextButton(drawCtx, this.leaveButton, "Выйти", right - 80.0f, y + 8.0f, 80.0f);
        if (party.isOwner()) {
            right -= 88.0f;
            drawTextButton(drawCtx, this.disbandButton, "Распустить", right - 100.0f, y + 8.0f, 100.0f);
            right -= 100.0f;
        } else {
            right -= 80.0f;
        }
        boolean connected = party.isConnected();
        drawStatus(drawCtx, connected ? "Подключение активно" : "Переподключение...", right - 14.0f, centerY);
    }

    private void drawStatus(DrawCtx drawCtx, String label, float right, float centerY) {
        float labelWidth = drawCtx.textWidthPhysical(this.textFont, label, 11);
        drawCtx.text(this.textFont, label, 11, right - labelWidth, textY(this.textFont, 11, centerY),
                secondaryColor(drawCtx));
    }

    @Override
    protected void layoutComposer(LayoutScaleContext context, float x, float y, float width) {
        this.codeField.listen(x + 158.0f, y + 8.0f, 120.0f, 24.0f, 108.0f, x + 164.0f, context.scaleFactor());
    }

    @Override
    protected void animateComposer(WeightedEngine engine) {
        this.codeField.animate(engine);
        this.createButton.animate(engine);
        this.joinButton.animate(engine);
        this.copyButton.animate(engine);
        this.leaveButton.animate(engine);
        this.disbandButton.animate(engine);
    }

    @Override
    protected boolean handleComposerInput(InputEventContext context, boolean consumed) {
        boolean handled = consumed;
        if (party().code() == null) {
            handled |= this.codeField.handleInput(context, handled);
            handled |= this.createButton.handleInput(context, handled);
            handled |= this.joinButton.handleInput(context, handled);
            return handled;
        }
        handled |= this.copyButton.handleInput(context, handled);
        handled |= this.leaveButton.handleInput(context, handled);
        if (party().isOwner()) {
            handled |= this.disbandButton.handleInput(context, handled);
        }
        return handled;
    }

    @Override
    protected void setupRow(String key, Row row) {
    }

    @Override
    protected void drawRow(DrawCtx drawCtx, Member entry, Row row, float x, float y, float width) {
        float centerY = y + ROW_HEIGHT / 2.0f;
        String name = entry.id().equals(party().ownId()) ? entry.name() + " (ты)" : entry.name();
        drawFittedText(drawCtx, this.titleFont, name, 12, x, centerY, 212.0f, textColor(drawCtx));
        drawFittedText(drawCtx, this.textFont, entry.owner() ? "Владелец" : "Участник", 11, x + 230.0f, centerY,
                100.0f, entry.owner() ? accentColor(drawCtx) : secondaryColor(drawCtx));
        String status = entry.connected() ? "В игре" : "Вернётся в течение "
                + Math.max(0L, (entry.expiresAt() - System.currentTimeMillis() + 999L) / 1000L) + " сек";
        drawFittedText(drawCtx, this.textFont, status, 11, x + 340.0f, centerY, 170.0f, secondaryColor(drawCtx));
        String distance = distance(entry);
        if (distance != null) {
            float distanceWidth = drawCtx.textWidthPhysical(this.textFont, distance, 11);
            drawCtx.text(this.textFont, distance, 11, x + width - distanceWidth, textY(this.textFont, 11, centerY),
                    secondaryColor(drawCtx));
        }
    }

    private static String distance(Member member) {
        LocalPlayer player = Minecraft.getInstance().player;
        Position position = party().position(member.id());
        if (player == null || position == null
                || !PartyService.normalizeServer(position.server()).equals(PartyService.server())
                || !position.dimension().equals(PartyService.dimension())) {
            return null;
        }
        double distance = Math.sqrt(player.distanceToSqr(position.interpolatedX(), position.interpolatedY(),
                position.interpolatedZ()));
        return Math.round(distance) + " м";
    }

    private void join() {
        String code = this.codeField.text().trim();
        if (!code.matches("[A-Za-z0-9]{6}")) {
            this.codeField.focused(true);
            return;
        }
        party().join(code);
        this.codeField.clearText();
        this.codeField.focused(false);
    }

    private void copyCode() {
        String code = party().code();
        if (code != null) {
            Minecraft.getInstance().keyboardHandler.setClipboard(code);
            this.copiedAt = System.currentTimeMillis();
        }
    }

    @Override
    public void close() {
        super.close();
        this.codeField.focused(false);
    }
}
