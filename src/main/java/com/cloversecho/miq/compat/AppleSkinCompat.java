package com.cloversecho.miq.compat;

import com.cloversecho.miq.recipe.DailyRecipeManager;
import net.neoforged.bus.api.SubscribeEvent;
import squeek.appleskin.api.event.FoodValuesEvent;

/**
 * AppleSkin compatibility. AppleSkin asks every mod for a possibly-modified
 * {@link net.minecraft.world.food.FoodProperties} via {@link FoodValuesEvent}
 * when it renders its hunger/saturation tooltip or HUD overlay.
 *
 * This listener feeds AppleSkin the same desire-category-scaled values that the
 * {@code PlayerEatMixin} actually applies when eating, so the displayed hunger
 * restore and saturation match real behaviour.
 *
 * 为 AppleSkin 提供与真实进食一致的缩放 FoodProperties，使工具提示与 HUD 叠加层
 * 显示的饱食度/饱和度与实际效果一致。
 *
 * <p>Note: this class is only loaded/registered when AppleSkin is actually installed
 * at runtime (guarded in {@code MIQ}), so it never breaks launch without AppleSkin.
 */
public final class AppleSkinCompat {

    @SubscribeEvent
    public void onFoodValues(FoodValuesEvent event) {
        event.modifiedFoodProperties = DailyRecipeManager.buildModifiedFoodProperties(
                event.defaultFoodProperties, event.player, event.itemStack);
    }
}