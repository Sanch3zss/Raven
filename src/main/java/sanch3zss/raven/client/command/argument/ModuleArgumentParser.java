package sanch3zss.raven.client.command.argument;

import java.util.List;
import java.util.stream.Collectors;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.feature.module.Module;

public class ModuleArgumentParser implements ArgumentParser<Module> {
    @Override
    public Module parse(String str) {
        return Raven.INSTANCE.moduleRepository().getModules().stream().filter(value -> !value.isBlocked()).filter(value -> {
            return value.getName().replaceAll("\\s", "").equalsIgnoreCase(str);
        }).findFirst().orElseThrow(() -> {
            return new TranslatedException(Translation.clearText(Lang.TYPE_MODULE_NOT_FOUND.effective().replace("{input}", str)));
        });
    }

    @Override
    public List<String> getSuggestions(String str) {
        String lowerCase = str.toLowerCase();
        return (List) Raven.INSTANCE.moduleRepository().getModules().stream().filter(value -> !value.isBlocked()).map(value -> {
            return value.getName().replaceAll("\\s", "");
        }).filter(str2 -> {
            return str2.replaceAll("\\s", "").toLowerCase().startsWith(lowerCase);
        }).collect(Collectors.toList());
    }

    @Override
    public String getName() {
        return "module";
    }
}
