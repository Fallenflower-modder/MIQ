package com.cloversecho.miq.compat;

import com.cloversecho.miq.recipe.DailyRecipeManager;
import com.cloversecho.miq.recipe.DesireCategory;
import net.minecraft.world.item.ItemStack;
import someassemblyrequired.item.sandwich.SandwichContents;

/**
 * Some Assembly Required compatibility.
 *
 * A {@link DailyRecipeManager.StackDesireResolver} that derives the desire category of a whole SAR
 * sandwich by aggregating the daily desire categories of its ingredients, so a single sandwich can
 * be treated as "today I really want / am willing / don't want" as a whole.
 *
 * Aggregation rule (as specified):
 * <ul>
 *   <li>if #VERY_WANT >= #DON'T_WANT  -> whole sandwich is VERY_WANT</li>
 *   <li>else (#DON'T_WANT > #VERY_WANT): if #WILLING >= #DON'T_WANT -> WILLING, otherwise DON'T_WANT</li>
 * </ul>
 * Ingredients that are not part of today's recipe (category {@code null}) are ignored. If no ingredient
 * has a category (e.g. an empty/non-food sandwich), {@code null} is returned so the caller falls back
 * to the plain item lookup.
 *
 * Because this re-reads the current recipe each call, the sandwich's desire category automatically
 * updates whenever the daily recipe refreshes (no cached value).
 *
 * 为 Some Assembly Required 提供兼容：把 SAR 三明治视为整体，按其配料（每种食物的每日欲望分类）
 * 的数量聚合出整个三明治的分类。聚合规则：
 * 想吃 >= 不想吃 -> 整体想吃；否则（不想吃 > 想吃）：愿意吃 >= 不想吃 -> 整体愿意吃，否则整体不想吃。
 * 每类以外的配料（不在今日食谱中）忽略；所有配料都无分类时返回 null 以回退到物品查询。
 * 每次调用实时读取当前食谱，因此三明治的食欲会随每日食谱刷新而自动变化。
 *
 * <p>Note: this resolver is only installed when SAR is actually loaded at runtime (guarded in {@code MIQ}),
 * so it never breaks launch without SAR.
 */
public final class SarCompat {

    /** A resolver installed to {@link DailyRecipeManager} when SAR is present. */
    public static final DailyRecipeManager.StackDesireResolver RESOLVER = SarCompat::resolve;

    private SarCompat() {
    }

    /** Returns the aggregated desire category of a SAR sandwich, or {@code null} if it has no determinable desire. */
    private static DesireCategory resolve(ItemStack stack) {
        if (!stack.is(someassemblyrequired.registry.ModItems.SANDWICH.get())) {
            return null;
        }
        SandwichContents contents = SandwichContents.get(stack);
        if (contents.isEmpty()) {
            return null;
        }

        int very = 0;
        int willing = 0;
        int dont = 0;
        for (ItemStack ingredient : contents) {
            if (ingredient == null || ingredient.isEmpty()) {
                continue;
            }
            DesireCategory category = DailyRecipeManager.getCategory(ingredient.getItem());
            if (category == null) {
                continue;
            }
            switch (category) {
                case VERY_WANT -> very++;
                case WILLING -> willing++;
                case DON_T_WANT -> dont++;
            }
        }

        if (very + willing + dont == 0) {
            return null;
        }
        if (very >= dont) {
            return DesireCategory.VERY_WANT;
        }
        return willing >= dont ? DesireCategory.WILLING : DesireCategory.DON_T_WANT;
    }
}