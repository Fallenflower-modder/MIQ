package com.cloversecho.miq.compat;

import com.cloversecho.miq.recipe.DailyRecipeManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import squeek.appleskin.api.AppleSkinApi;
import squeek.appleskin.api.event.FoodValuesEvent;

/**
 * AppleSkin compatibility (Fabric). AppleSkin asks every mod for a possibly-modified
 * {@link net.minecraft.world.food.FoodProperties} via {@link FoodValuesEvent}
 * when it renders its hunger/saturation tooltip or HUD overlay.
 *
 * This entrypoint feeds AppleSkin the same desire-category-scaled values that the
 * {@code PlayerEatMixin} actually applies when eating, so the displayed hunger
 * restore and saturation match real behaviour.
 *
 * Registered through the custom {@code "appleskin"} Fabric entrypoint, so this class
 * is only loaded when AppleSkin is installed (and only on the client, where AppleSkin
 * invokes its entrypoints), and never breaks launch without AppleSkin.
 *
 * 为 AppleSkin 提供兼容：把与真实进食一致的缩放 FoodProperties 提供给 AppleSkin 的
 * FoodValuesEvent，使其工具提示与 HUD 叠加层显示的饱食度/饱和度与实际效果一致。
 *
 * <p>通过自定义 {@code "appleskin"} Fabric 入口点注册：仅在装有 AppleSkin 时才会被加载
 * （且仅在客户端被 AppleSkin 调用），因此没有 AppleSkin 也能正常启动。
 */
@Environment(EnvType.CLIENT)
public final class AppleSkinCompat implements AppleSkinApi {

    @Override
    public void registerEvents() {
        FoodValuesEvent.EVENT.register(event ->
                event.modifiedFoodComponent = DailyRecipeManager.buildModifiedFoodProperties(
                        event.defaultFoodComponent, event.player, event.itemStack));
    }
}
