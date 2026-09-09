package com.cloversecho.miq.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * Persists the current daily recipe between server restarts.
 * Stored under {@code data/<save>/miq_daily_recipe.dat} together with the world.
 *
 * 将当前每日食谱持久化存档，随世界一起保存在
 * {@code data/<存档>/miq_daily_recipe.dat}，以便重启后恢复当日食谱。
 */
public final class RecipeSaveData extends SavedData {

    /** NBT id used by the nearest DataStorage. 存档键名。 */
    public static final String ID = "miq_daily_recipe";

    /** The recipe loaded from or to be written to disk. 存档中保存的食谱。 */
    private Map<Item, DesireCategory> recipe = new HashMap<>();

    public RecipeSaveData() {
    }

    /** Returns the raw (unmodifiable intent) category map. 获取存档中的食谱。 */
    public Map<Item, DesireCategory> getRecipe() {
        return recipe;
    }

    /** Stores a snapshot of the given recipe. 写入一份食谱快照。 */
    public void setRecipe(Map<Item, DesireCategory> recipe) {
        this.recipe = new HashMap<>(recipe);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag keys = new ListTag();
        ListTag cats = new ListTag();
        for (Map.Entry<Item, DesireCategory> e : recipe.entrySet()) {
            keys.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(e.getKey()).toString()));
            cats.add(StringTag.valueOf(e.getValue().name()));
        }
        tag.put("keys", keys);
        tag.put("cats", cats);
        return tag;
    }

    public static RecipeSaveData load(CompoundTag tag, HolderLookup.Provider registries) {
        RecipeSaveData data = new RecipeSaveData();
        ListTag keys = tag.getList("keys", Tag.TAG_STRING);
        ListTag cats = tag.getList("cats", Tag.TAG_STRING);
        Map<Item, DesireCategory> map = new HashMap<>();
        for (int i = 0; i < keys.size() && i < cats.size(); i++) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(keys.getString(i)));
            DesireCategory cat = parseCategory(cats.getString(i));
            if (item != null && cat != null) {
                map.put(item, cat);
            }
        }
        data.recipe = map;
        return data;
    }

    private static DesireCategory parseCategory(String name) {
        for (DesireCategory category : DesireCategory.values()) {
            if (category.name().equals(name)) {
                return category;
            }
        }
        return null;
    }

    public static SavedData.Factory<RecipeSaveData> factory() {
        return new SavedData.Factory<>(RecipeSaveData::new, RecipeSaveData::load);
    }
}