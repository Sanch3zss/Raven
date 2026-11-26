package sanch3zss.raven.render.texture;

import java.util.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import sanch3zss.raven.render.texture.SpriteRegion;

public class ItemSpriteTextures {
    private String type;
    private Map sprites;
    private boolean glint;

    public ItemSpriteTextures() {
    }

    public ItemSpriteTextures(String str, Map map, boolean z) {
        this.type = str;
        this.sprites = map;
        this.glint = z;
    }

    public static ItemSpriteTextures of(String str, Map map, boolean z, int i) {
        HashMap hashMap = new HashMap();
        for (Object obj : map.entrySet()) {
            Map.Entry entry = (Map.Entry) obj;
            hashMap.put((Direction) entry.getKey(), SpriteRegion.of((TextureAtlasSprite) entry.getValue(), i));
        }
        return new ItemSpriteTextures(str, hashMap, z);
    }

    public Map sprites() {
        return this.sprites;
    }

    public String type() {
        return this.type;
    }
}
