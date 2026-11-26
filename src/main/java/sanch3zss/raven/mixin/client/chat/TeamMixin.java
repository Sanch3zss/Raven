package sanch3zss.raven.mixin.client.chat;

import java.util.Map;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringUtil;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

import sanch3zss.raven.feature.server.ServerUtil;

@Mixin({PlayerTeam.class})
public class TeamMixin {

    @Unique
    private static final Map<String, Component> map = Map.ofEntries(Map.entry("ꔀ", buildPrefix("PLAYER", -6184543)), Map.entry("ꔁ", buildPrefix("MEDIA", -21420)), Map.entry("ꔄ", buildPrefix("HERO", -11118858)), Map.entry("ꔅ", buildPrefix("YT", -50116)), Map.entry("ꔈ", buildPrefix("TITAN", -148)), Map.entry("ꔉ", buildPrefix("HELPER", -9699494)), Map.entry("ꔒ", buildPrefix("AVENGER", -9699494)), Map.entry("ꔓ", buildPrefix("ML.MODER", -11118858)), Map.entry("ꔖ", buildPrefix("OVERLORD", -44205)), Map.entry("ꔗ", buildPrefix("MODER", -11118858)), Map.entry("ꔠ", buildPrefix("MAGISTER", -21420)), Map.entry("ꔡ", buildPrefix("MODER+", -6919697)), Map.entry("ꔤ", buildPrefix("IMPERATOR", -44205)), Map.entry("ꔥ", buildPrefix("ST.MODER", -6919697)), Map.entry("ꔨ", buildPrefix("DRAGON", -6919697)), Map.entry("ꔩ", buildPrefix("GL.MODER", -6919697)), Map.entry("ꔲ", buildPrefix("BULL", -9699494)), Map.entry("ꔳ", buildPrefix("ML.ADMIN", -44205)), Map.entry("ꔶ", buildPrefix("TIGER", -21420)), Map.entry("ꔷ", buildPrefix("ADMIN", -44205)), Map.entry("ꔸ", buildPrefix("ADMIN", -44205)), Map.entry("ꕀ", buildPrefix("HYDRA", -9699494)), Map.entry("ꕁ", buildPrefix("GOD", -26071)), Map.entry("ꕄ", buildPrefix("DRACULA", -44205)), Map.entry("ꕅ", buildPrefix("VAMPIRE", -44205)), Map.entry("ꕈ", buildPrefix("COBRA", -9699494)), Map.entry("ꕉ", buildPrefix("PEGAS", -148)), Map.entry("ꕒ", buildPrefix("RABBIT", -11118858)), Map.entry("ꕖ", buildPrefix("BUNNY", -11118858)), Map.entry("ꕠ", buildPrefix("D.HELPER", -9699494)));

    @WrapOperation(method = {"setPlayerPrefix"}, at = {@At(value = "FIELD", target = "Lnet/minecraft/world/scores/PlayerTeam;playerPrefix:Lnet/minecraft/network/chat/Component;", opcode = 181)})
    private void setPrefixHook(PlayerTeam team, Component text, Operation<Void> operation) {
        Object[] objArr = new Object[2];
        objArr[0] = team;
        objArr[1] = ServerUtil.isConnectedToServer("reallyworld") ? map.getOrDefault(StringUtil.stripColor(text.getString().replace("●", "").replace(" ", "")), text) : text;
        operation.call(objArr);
    }

    @Unique
    private static Component buildPrefix(String str, int i) {
        return Component.literal(" §c●§f ").append(Component.literal(str + " ").setStyle(Style.EMPTY.withColor(i)));
    }
}
