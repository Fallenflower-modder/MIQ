package com.cloversecho.miq.compat;

import com.cloversecho.miq.recipe.DailyRecipeManager;
import net.minecraft.world.food.FoodProperties;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import squeek.appleskin.api.event.FoodValuesEvent;
import squeek.appleskin.api.food.FoodValues;

/**
 * AppleSkin compatibility. AppleSkin asks every mod for possibly-modified hunger/saturation
 * values via {@link FoodValuesEvent} when it renders its hunger/saturation tooltip or HUD
 * overlay.
 *
 * This listener feeds AppleSkin the same desire-category-scaled values that the
 * {@code PlayerEatMixin} actually applies when eating, so the displayed hunger restore and
 * saturation match real behaviour.
 *
 * Note: on 1.20.1 the event carries {@link FoodValues} ({@code hunger} + {@code saturationModifier})
 * instead of a whole {@link FoodProperties}, so the scaled properties are converted accordingly.
 *
 * 为 AppleSkin 提供与真实进食一致的缩放食物数值，使工具提示与 HUD 叠加层显示的
 * 饱食度/饱和度与实际效果一致。1.20.1 中事件携带的是 FoodValues（hunger + saturationModifier），
 * 因此将缩放后的 FoodProperties 转换后写入。
 *
 * <p>Note: this class is only loaded/registered when AppleSkin is actually installed
 * at runtime (guarded in {@code MIQ}), so it never breaks launch without AppleSkin.
 */
public final class AppleSkinCompat {

    @SubscribeEvent
    public void onFoodValues(FoodValuesEvent event) {
        FoodProperties base = event.itemStack.getItem().getFoodProperties(event.itemStack, event.player);
        if (base == null) {
            return;
        }
        FoodProperties scaled = DailyRecipeManager.buildModifiedFoodProperties(base, event.player, event.itemStack);
        event.modifiedFoodValues = new FoodValues(scaled.getNutrition(), scaled.getSaturationModifier());
    }
}