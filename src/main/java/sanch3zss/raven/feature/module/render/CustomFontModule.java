package sanch3zss.raven.feature.module.render;

import java.util.List;

import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.font.CustomFonts;
import sanch3zss.raven.client.platform.FilePicker;
import sanch3zss.raven.event.client.ClientTickEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.setting.SettingUnit;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.*;

@Aliases(aliases = {"Custom Font", "Font", "TTF Font", "Шрифт"})
public class CustomFontModule extends Module {
    private final TextFieldSetting fontPath;
    private final NumberSetting fontSize;

    public CustomFontModule() {
        super(ModuleTab.VISUALS, "CustomFont");
        this.fontPath = new TextFieldSetting(Translation.clearText("Путь к шрифту"))
                .setText("").visible(() -> false);
        ButtonSetting chooseFont = new ButtonSetting(Translation.clearText("Шрифт"))
                .setButtonName(Translation.clearText("Выбрать шрифт"))
                .setRunnable(() -> FilePicker.choose("Выбрать шрифт", "Шрифты", List.of("ttf", "otf"),
                        "Шрифт выбран.", this.fontPath::setText));
        ButtonSetting resetFont = new ButtonSetting(Translation.clearText(""))
                .setButtonName(Translation.clearText("Сбросить"))
                .setRunnable(() -> this.fontPath.setText(""));
        this.fontSize = new NumberSetting(Translation.clearText("Размер шрифта"))
                .currentValue(100.0f).range(60.0f, 140.0f).step(1.0f).unit(SettingUnit.PERCENTS);
        addSettings(this.fontSize, chooseFont, resetFont, this.fontPath);
        register(ClientTickEvent.class, event ->
                CustomFonts.INSTANCE.update(isState(), this.fontPath.getText(), this.fontSize.currentValue() / 100.0f));
    }
}
