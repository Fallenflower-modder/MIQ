package com.cloversecho.miq.mixin;

import com.cloversecho.miq.util.EatContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Redirects the {@link ItemStack#getFoodProperties(LivingEntity)} fetch inside
 * {@code FoodData.eat(Item, ItemStack, LivingEntity)} so the hunger restore and saturation applied
 * when a player eats use the desire-scaled FoodProperties computed at {@code Player.eat}.
 *
 * Note: in 1.20.1 {@code Player.eat} calls the 3-arg overload
 * {@code FoodData.eat(Item, ItemStack, LivingEntity)}, which fetches food properties through
 * {@code ItemStack#getFoodProperties(LivingEntity)} (a Forge-patched method), not the old
 * {@code Item#getFoodProperties()}.
 *
 * 重定向 FoodData.eat(Item, ItemStack, LivingEntity) 内部对
 * ItemStack#getFoodProperties(LivingEntity) 的获取，使玩家进食时应用的饱食度恢复与饱和度
 * 使用 Player.eat 中计算好的欲望缩放属性。
 * 注意：1.20.1 中 Player.eat 调用的是三参数重载 FoodData.eat(Item, ItemStack, LivingEntity)，
 * 内部通过 Forge 补丁方法 ItemStack#getFoodProperties(LivingEntity) 获取属性。
 */
@Mixin(net.minecraft.world.food.FoodData.class)
public abstract class FoodDataEatMixin {

    @Redirect(
            remap = false,
            method = "eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getFoodProperties(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/food/FoodProperties;", remap = false))
    private FoodProperties miq$scaleNutrition(ItemStack stack, LivingEntity entity) {
        FoodProperties scaled = EatContext.get();
        return scaled != null ? scaled : stack.getFoodProperties(entity);
    }
}
