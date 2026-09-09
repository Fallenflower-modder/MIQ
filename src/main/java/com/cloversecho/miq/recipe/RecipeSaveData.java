package com.cloversecho.miq.recipe;

import com.cloversecho.miq.MIQ;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persists the current daily recipe between server restarts.
 * With Minecraft 26.1 the saved-data API moved to a {@link Codec}-based
 * {@link SavedDataType}, so the recipe is (de)serialised as a string map
 * ({@code item id -> desire category name}).
 *
 * 将当前每日食谱持久化存档。26.1 中存档系统改为基于 {@link Codec} 的
 * {@link SavedDataType}，因此食谱以“物品 id -> 欲望分类名”的字符串映射序列化。
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

    private static Map<Item, DesireCategory> decodeMap(Map<String, String> raw) {
        Map<Item, DesireCategory> result = new HashMap<>();
        for (Map.Entry<String, String> e : raw.entrySet()) {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(e.getKey()));
            DesireCategory cat = parseCategory(e.getValue());
            if (item != null && item != Items.AIR && cat != null) {
                result.put(item, cat);
            }
        }
        return result;
    }

    private static Map<String, String> encodeMap(Map<Item, DesireCategory> recipe) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<Item, DesireCategory> e : recipe.entrySet()) {
            out.put(BuiltInRegistries.ITEM.getKey(e.getKey()).toString(), e.getValue().name());
        }
        return out;
    }

    private static DesireCategory parseCategory(String name) {
        for (DesireCategory category : DesireCategory.values()) {
            if (category.name().equals(name)) {
                return category;
            }
        }
        return null;
    }

    /** Codec used by the saved-data storage to read/write the recipe. */
    public static final Codec<RecipeSaveData> CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .xmap(RecipeSaveData::decodeMap, RecipeSaveData::encodeMap)
                    .xmap(map -> {
                        RecipeSaveData data = new RecipeSaveData();
                        data.recipe = map;
                        return data;
                    }, data -> data.recipe);

    /** Saved-data type registered under the world storage. */
    public static final SavedDataType<RecipeSaveData> TYPE =
            new SavedDataType<>(Identifier.fromNamespaceAndPath(MIQ.MODID, "daily_recipe"),
                    RecipeSaveData::new, CODEC);
}