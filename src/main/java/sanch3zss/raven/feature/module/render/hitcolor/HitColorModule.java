package sanch3zss.raven.feature.module.render.hitcolor;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.event.client.ClientTickEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.setting.SettingUnit;
import sanch3zss.raven.mixin.accessor.OverlayTextureAccessor;
import sanch3zss.raven.render.theme.ColorUtil;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.BooleanSetting;
import sanch3zss.raven.ui.setting.ColorSetting;
import sanch3zss.raven.ui.setting.NumberSetting;

@Aliases(aliases = {"Hit Color", "Damage Color", "Hurt Color", "Custom Hit Color", "Hurt Tint"})
public class HitColorModule extends Module {
    private static final int ROWS = 8;
    private static final int VANILLA_RED = 0xB2FF0000;
    private final ColorSetting color;
    private final NumberSetting strength;
    private final BooleanSetting armor;
    private final int[] uploaded = new int[ROWS];

    public HitColorModule() {
        super(ModuleTab.VISUALS, "HitColor");
        this.color = new ColorSetting(Translation.clearText("Цвет")).value(0xFFFF3B5C);
        this.color.setSecondColor(0xFFFFA24D);
        this.strength = new NumberSetting(Translation.clearText("Насыщенность"))
                .currentValue(30.0f).range(5.0f, 90.0f).step(1.0f).unit(SettingUnit.PERCENTS);
        this.armor = new BooleanSetting(Translation.clearText("Закрашивать броню")).setValue(true);
        addSettings(this.color, this.strength, this.armor);
        Arrays.fill(this.uploaded, VANILLA_RED);
        register(ClientTickEvent.class, event -> updateOverlay());
    }

    public int overlayRow(float progress) {
        return Math.round(Math.max(0.0f, Math.min(1.0f, progress)) * (ROWS - 1));
    }

    public boolean tintsArmor(Object state) {
        return isState() && this.armor.isValue() && state instanceof LivingEntityRenderState living && living.hasRedOverlay;
    }

    private void updateOverlay() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameRenderer == null) return;
        int[] rows = new int[ROWS];
        for (int row = 0; row < ROWS; row++) {
            rows[row] = isState() ? overlayColor(row / (float) (ROWS - 1)) : VANILLA_RED;
        }
        if (Arrays.equals(rows, this.uploaded)) return;
        DynamicTexture texture = ((OverlayTextureAccessor) minecraft.gameRenderer.overlayTexture()).raven$texture();
        NativeImage pixels = texture.getPixels();
        if (pixels == null) return;
        for (int row = 0; row < ROWS; row++) {
            for (int x = 0; x < 16; x++) pixels.setPixel(x, row, rows[row]);
        }
        texture.upload();
        System.arraycopy(rows, 0, this.uploaded, 0, ROWS);
    }

    private int overlayColor(float position) {
        int tint = this.color.getGradientColor(position);
        float amount = this.strength.currentValue() / 100.0f * ColorUtil.alpha(tint) / 255.0f;
        int alpha = Math.round((1.0f - amount) * 255.0f);
        return alpha << 24 | tint & 0xFFFFFF;
    }
}
