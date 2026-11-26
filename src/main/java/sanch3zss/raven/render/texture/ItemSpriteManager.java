package sanch3zss.raven.render.texture;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.PlayerHeadItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.TippedArrowItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.resource.*;
import sanch3zss.raven.client.service.ActionSequenceController;
import sanch3zss.raven.event.network.WorldLoadEvent;
import sanch3zss.raven.event.player.PlayerInitEvent;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.texture.*;

public enum ItemSpriteManager {
    INSTANCE;

    public final UvBounds defaultFaceUv = new UvBounds(0.125f, 0.125f, 0.25f, 0.25f);
    public final Map<Object, GlTexture> textureCache = new HashMap<>();
    public final Map<String, ItemSpriteTextures> spriteCache = new HashMap<>();

    public final ConcurrentMap<GameProfile, Identifier> skinCache = new ConcurrentHashMap<>();
    public final Set<GameProfile> pendingProfiles = ConcurrentHashMap.newKeySet();
    public final Map<Item, int[]> tintCache = new HashMap<>();
    public Map<Identifier, Resource> itemResources = null;

    ItemSpriteManager() {
        Raven.INSTANCE.eventDispatcher().register(WorldLoadEvent.class, event -> {
            clearCaches();
            ActionSequenceController.reset();
        });
        Raven.INSTANCE.eventDispatcher().register(PlayerInitEvent.class, event -> {
            if (Mc.INSTANCE.isWorldLoaded()) {
                clearCaches();
                ActionSequenceController.reset();
            }
        });
    }

    public boolean canRender(ItemStack itemStack) {
        ItemSpriteTextures spriteTextures;
        return itemStack != null && !itemStack.isEmpty() && (spriteTextures = getSpriteTextures(itemStack)) != null && spriteTextures.sprites() != null && !spriteTextures.sprites().isEmpty() && itemStack.getItem() != Items.AIR;
    }

    public UvBounds getFaceUV(LivingEntityRenderer<?, ?, ?> livingEntityRenderer) {
        if (livingEntityRenderer == null) {
            return this.defaultFaceUv;
        }
        EntityModel model = livingEntityRenderer.getModel();
        if (model != null) {
            for (Object partObj : model.allParts()) {
                if (partObj instanceof ModelPart part) {
                    UvBounds faceUv = computePartFaceUv(part);
                    if (faceUv != null) {
                        return faceUv;
                    }
                }
            }
        }
        return this.defaultFaceUv;
    }

    public GlTexture getOrCreateTexture(Object obj) {
        if (obj == null) {
            return null;
        }
        return this.textureCache.computeIfAbsent(obj, obj2 -> {
            if (obj2 instanceof TextureAtlasSprite) {
                return new GlTexture(new IdentifierResource(((TextureAtlasSprite) obj2).atlasLocation()));
            }
            if (!(obj2 instanceof AbstractTexture)) {
                return null;
            }
            GlTexture texture = new GlTexture(new TextureResource((AbstractTexture) obj2));
            texture.minFilter(9728);
            texture.magFilter(9728);
            return texture;
        });
    }

    public String getCacheKey(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return "empty";
        StringBuilder key = new StringBuilder(BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString());
        if (itemStack.hasFoil() || !((ItemEnchantments) itemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)).isEmpty()) {
            key.append("_glint");
        }
        int color = DyedItemColor.getOrDefault(itemStack, -1);
        if (color != -1) key.append("_color").append(color);
        if (itemStack.getItem() instanceof PotionItem) {
            key.append("_potion").append(((PotionContents) itemStack.getOrDefault(
                    DataComponents.POTION_CONTENTS, PotionContents.EMPTY)).getColor());
        }
        ResolvableProfile profile = (ResolvableProfile) itemStack.get(DataComponents.PROFILE);
        if (profile != null) {
            key.append("_profile").append(profile.hashCode());
        }
        return key.toString();
    }

    public ItemSpriteTextures getSpriteTextures(ItemStack itemStack) {
        String cacheKey = getCacheKey(itemStack);
        return this.spriteCache.computeIfAbsent(cacheKey, k -> {
            TextureAtlasSprite particleSprite = getParticleSprite(itemStack);
            if (particleSprite == null) {
                return null;
            }
            boolean z = itemStack.hasFoil() || !((ItemEnchantments) itemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)).isEmpty();
            int color = DyedItemColor.getOrDefault(itemStack, -1);
            Item item = itemStack.getItem();

            if (item instanceof TippedArrowItem) {
                return new ItemSpriteTextures("Default", Map.of(Direction.UP, SpriteRegion.of(getParticleSprite(new ItemStack(Items.ARROW)), -1), Direction.DOWN, SpriteRegion.of(particleSprite, ((PotionContents) itemStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)).getColor())), false);
            }

            if (item instanceof PotionItem) {
                Function<Identifier, TextureAtlasSprite> spriteAtlas = Mc.INSTANCE.getMinecraft().getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS)::getSprite;
                String str = (item instanceof LingeringPotionItem) ? "lingering_" : (item instanceof SplashPotionItem ? "splash_" : "");
                LinkedHashMap<Direction, SpriteRegion> potionLayers = new LinkedHashMap<>();
                potionLayers.put(Direction.DOWN, SpriteRegion.of(
                        spriteAtlas.apply(Identifier.withDefaultNamespace("item/" + str + "potion")), -1));
                potionLayers.put(Direction.UP, SpriteRegion.of(particleSprite,
                        ((PotionContents) itemStack.getOrDefault(
                                DataComponents.POTION_CONTENTS,
                                PotionContents.EMPTY)).getColor()));
                return new ItemSpriteTextures("Default", potionLayers, false);
            }

            if (item instanceof ShieldItem) {
                TextureAtlasSprite sprite;
                try {
                    sprite = Mc.INSTANCE.getMinecraft().getAtlasManager().getAtlasOrThrow(Sheets.SHIELD_SHEET).getSprite(Identifier.withDefaultNamespace("entity/shield_base_nopattern"));
                    return new ItemSpriteTextures("Shield", Map.of(Direction.UP, new SpriteRegion(sprite.atlasLocation(), sprite.getU0() + 0.0025f, sprite.getV0() + 0.0025f, sprite.getU1() - 0.1f, sprite.getV1() - 0.08f, -1)), z);
                } catch (IllegalArgumentException e) {
                    try {
                        sprite = Mc.INSTANCE.getMinecraft().getAtlasManager().getAtlasOrThrow(Identifier.withDefaultNamespace("shield_patterns")).getSprite(Identifier.withDefaultNamespace("entity/shield_base_nopattern"));
                        return new ItemSpriteTextures("Shield", Map.of(Direction.UP, new SpriteRegion(sprite.atlasLocation(), sprite.getU0() + 0.0025f, sprite.getV0() + 0.0025f, sprite.getU1() - 0.1f, sprite.getV1() - 0.08f, -1)), z);
                    } catch (IllegalArgumentException e2) {
                        return ItemSpriteTextures.of("Default", Map.of(Direction.DOWN, particleSprite), z, color);
                    }
                }
            }

            if (item instanceof SpawnEggItem) {
                int[] tints = this.tintCache.computeIfAbsent(item, item2 -> {
                    try {
                        JsonElement jsonElement = JsonParser.parseReader(getItemResources().get(Identifier.parse("items/" + BuiltInRegistries.ITEM.getKey(item2).getPath() + ".json")).openAsReader()).getAsJsonObject().get("model").getAsJsonObject().get("tints");
                        return new int[]{jsonElement.getAsJsonArray().get(1).getAsJsonObject().get("value").getAsInt(), jsonElement.getAsJsonArray().get(0).getAsJsonObject().get("value").getAsInt()};
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                return new ItemSpriteTextures("Default", Map.of(Direction.DOWN, SpriteRegion.of(Mc.INSTANCE.getMinecraft().getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS).getSprite(Identifier.withDefaultNamespace("item/spawn_egg_overlay")), tints[0]), Direction.UP, SpriteRegion.of(particleSprite, tints[1])), z);
            }

            if (item instanceof PlayerHeadItem) {
                ResolvableProfile profileComponent = (ResolvableProfile) itemStack.get(DataComponents.PROFILE);
                HashMap<Direction, SpriteRegion> map = new HashMap<>();
                PlayerSkin steve = profileComponent == null ? DefaultPlayerSkin.getDefaultSkin() : (PlayerSkin) ((Optional) Mc.INSTANCE.getSkinProvider().get(profileComponent.partialProfile()).getNow(Optional.empty())).orElse(null);
                if (steve == null) {
                    return null;
                }
                Identifier identifierTexture = steve.body().texturePath();
                map.put(Direction.UP, SpriteRegion.of(identifierTexture, 0.125f, 0.0f, 0.25f, 0.125f, color));
                map.put(Direction.WEST, SpriteRegion.of(identifierTexture, 0.0f, 0.125f, 0.125f, 0.25f, color));
                map.put(Direction.EAST, SpriteRegion.of(identifierTexture, 0.125f, 0.125f, 0.25f, 0.25f, color));
                return new ItemSpriteTextures("3D", map, z);
            }

            if (item instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                if (block instanceof ShulkerBoxBlock) {
                    return ItemSpriteTextures.of("3D", Map.of(Direction.UP, particleSprite, Direction.WEST, particleSprite, Direction.EAST, particleSprite), z, color);
                }
                HashMap<Direction, TextureAtlasSprite> map2 = new HashMap<>();
                for (Direction direction : Direction.values()) {
                    map2.put(direction, particleSprite);
                }
                if (!map2.isEmpty()) {
                    return ItemSpriteTextures.of("3D", map2, z, color);
                }
            }

            return ItemSpriteTextures.of("Default", Map.of(Direction.DOWN, particleSprite), z, color);
        });
    }

    public Map<Identifier, Resource> getItemResources() {
        if (this.itemResources == null) {
            this.itemResources = FileToIdConverter.json("items").listMatchingResources(Mc.INSTANCE.getResourceManager());
        }
        return this.itemResources;
    }

    public TextureAtlasSprite getParticleSprite(ItemStack itemStack) {
        ItemStackRenderState itemRenderState = new ItemStackRenderState();
        Mc.INSTANCE.getItemModelManager().appendItemLayers(itemRenderState, itemStack, ItemDisplayContext.GUI, Mc.INSTANCE.getWorld(), null, 0);
        net.minecraft.client.resources.model.sprite.Material.Baked material = itemRenderState.pickParticleMaterial(Mc.INSTANCE.getWorld() != null ? Mc.INSTANCE.getWorld().getRandom() : RandomSource.create());
        return material != null ? material.sprite() : null;
    }

    public UvBounds computePartFaceUv(ModelPart modelPart) {
        UvBounds largerUv = null;
        for (ModelPart.Cube cuboid : modelPart.cubes) {
            largerUv = pickLargerUv(largerUv, selectMedianUv(computeSideUv(cuboid, Direction.NORTH), computeSideUv(cuboid, Direction.SOUTH), computeSideUv(cuboid, Direction.WEST), computeSideUv(cuboid, Direction.EAST)));
        }
        return largerUv;
    }

    public UvBounds computeSideUv(ModelPart.Cube cuboid, Direction direction) {
        ModelPart.Polygon quad;
        int id = direction.get3DDataValue();
        if (id < 0 || id >= cuboid.polygons.length || (quad = cuboid.polygons[id]) == null || quad.vertices() == null || quad.vertices().length == 0) {
            return null;
        }
        float fMin = Float.POSITIVE_INFINITY;
        float fMin2 = Float.POSITIVE_INFINITY;
        float fMax = Float.NEGATIVE_INFINITY;
        float fMax2 = Float.NEGATIVE_INFINITY;
        for (ModelPart.Vertex vertex : quad.vertices()) {
            fMin = Math.min(fMin, vertex.u());
            fMin2 = Math.min(fMin2, vertex.v());
            fMax = Math.max(fMax, vertex.u());
            fMax2 = Math.max(fMax2, vertex.v());
        }
        if (fMin == Float.POSITIVE_INFINITY || fMin2 == Float.POSITIVE_INFINITY) {
            return null;
        }
        return new UvBounds(fMin, fMin2, fMax, fMax2);
    }

    public UvBounds selectMedianUv(UvBounds... arr) {
        ArrayList<UvBounds> arrayList = new ArrayList<>();
        for (UvBounds uvBounds2 : arr) {
            if (uvBounds2 != null) {
                arrayList.add(uvBounds2);
            }
        }
        if (arrayList.isEmpty()) {
            return null;
        }
        if (arrayList.size() == 1) {
            return arrayList.get(0);
        }
        arrayList.sort(Comparator.comparing(UvBounds::u0));
        return arrayList.size() >= 4 ? arrayList.get(1) : arrayList.get(arrayList.size() / 2);
    }

    public UvBounds pickLargerUv(UvBounds uvBounds2, UvBounds uvBounds) {
        if (uvBounds == null) {
            return uvBounds2;
        }
        if (uvBounds2 == null) {
            return uvBounds;
        }
        float fU1 = (uvBounds2.u1() - uvBounds2.u0()) * (uvBounds2.v1() - uvBounds2.v0());
        float fU2 = (uvBounds.u1() - uvBounds.u0()) * (uvBounds.v1() - uvBounds.v0());
        if (fU2 != fU1 && fU2 <= fU1) {
            return uvBounds2;
        }
        return uvBounds;
    }

    public TextureAtlasSprite getFirstParticleSprite(ItemStackRenderState itemRenderState) {
        if (itemRenderState.activeLayerCount == 0) {
            return null;
        }
        net.minecraft.client.resources.model.sprite.Material.Baked material = itemRenderState.pickParticleMaterial(RandomSource.create());
        return material != null ? material.sprite() : null;
    }

    public void clearCaches() {
        this.textureCache.values().forEach(GlTexture::reload);
        this.textureCache.clear();
        this.spriteCache.clear();
        this.tintCache.clear();
        this.itemResources = null;
    }
}
