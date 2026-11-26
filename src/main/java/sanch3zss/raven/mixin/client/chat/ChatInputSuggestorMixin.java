package sanch3zss.raven.mixin.client.chat;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.commands.SharedSuggestionProvider;
import com.mojang.brigadier.*;
import com.mojang.brigadier.suggestion.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import sanch3zss.raven.client.Raven;

@Mixin({CommandSuggestions.class})
public abstract class ChatInputSuggestorMixin {

    @Shadow
    @Final
    Minecraft minecraft;

    @Shadow
    @Final
    EditBox input;

    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow
    private CommandSuggestions.SuggestionsList suggestions;

    @Shadow
    private ParseResults<SharedSuggestionProvider> currentParse;

    @Shadow
    boolean keepSuggestions;

    @Shadow
    private boolean allowSuggestions;

    @Shadow
    public abstract void showSuggestions(boolean z);

    @Inject(method = {"updateCommandInfo"}, at = {@At("HEAD")}, cancellable = true)
    private void onRefresh(CallbackInfo callbackInfo) {
        String text = this.input.getValue();
        if (text.startsWith(Raven.INSTANCE.commandDispatcher().getPrefix())) {
            callbackInfo.cancel();
            refreshCustomCommands(text);
        }
    }

    @Unique
    private void refreshCustomCommands(String str) {
        if (this.currentParse != null && !this.currentParse.getReader().getString().equals(str)) {
            this.currentParse = null;
        }
        if (!this.keepSuggestions) {
            this.input.setSuggestion((String) null);
            this.suggestions = null;
        }
        int prefixLength = Raven.INSTANCE.commandDispatcher().getPrefix().length();
        int cursor = Math.max(prefixLength, Math.min(this.input.getCursorPosition(), str.length()));
        String beforeCursor = str.substring(0, cursor);
        List<String> suggestions = Raven.INSTANCE.commandDispatcher().getSuggestions(
                beforeCursor.substring(prefixLength));
        if (cursor == str.length()) {
            suggestions.stream().filter(suggestion -> suggestion.startsWith("<"))
                    .findFirst().ifPresent(this.input::setSuggestion);
        }
        List<String> completions = suggestions.stream()
                .filter(suggestion -> !suggestion.startsWith("<")).toList();
        if (completions.isEmpty()) {
            this.pendingSuggestions = Suggestions.empty();
            return;
        }
        this.pendingSuggestions = buildSuggestions(beforeCursor, completions, prefixLength);
        if (this.allowSuggestions && Boolean.TRUE.equals(this.minecraft.options.autoSuggestions().get())) {
            this.pendingSuggestions.thenRun(() -> {
                if (this.pendingSuggestions.isDone()) {
                    showSuggestions(false);
                }
            });
        }
    }

    @Unique
    private CompletableFuture<Suggestions> buildSuggestions(String beforeCursor, List<String> list,
                                                            int prefixLength) {
        int start = beforeCursor.length();
        while (start > prefixLength && !Character.isWhitespace(beforeCursor.charAt(start - 1))) {
            start--;
        }
        SuggestionsBuilder suggestionsBuilder = new SuggestionsBuilder(beforeCursor, start);
        for (String suggestion : list) {
            suggestionsBuilder.suggest(suggestion);
        }
        return suggestionsBuilder.buildFuture();
    }
}
