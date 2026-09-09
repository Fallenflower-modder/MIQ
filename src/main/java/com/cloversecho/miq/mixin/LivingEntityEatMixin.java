package com.cloversecho.miq.mixin;

import com.cloversecho.miq.util.EatContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Redirects the {@link ItemStack#getFoodProperties(LivingEntity)} fetch inside the private
 * {@code LivingEntity.addEatEffect(ItemStack, Level, LivingEntity)} so the effects granted by a
 * food (including the MIQ very-want luck boost / don't-want hunger-nausea penalty) match the
 * desire-scaled FoodProperties computed at {@code Player.eat}. For non-player entities eating,
 * {@link EatContext} is empty and the original properties are kept.
 *
 * 重定向 LivingEntity.addEatEffect 内部对 ItemStack#getFoodProperties(LivingEntity) 的获取，
 * 使食物附带效果（含“很想吃”的幸运加成与“不想吃”的饥饿/反胃惩罚）与 Player.eat 中计算好的
 * 欲望缩放属性一致。非玩家实体进食时 EatContext 为空，保持原始属性不变。
 * 注意：1.20.1 中该方法调用的是 Forge 补丁方法 ItemStack#getFoodProperties(LivingEntity)，
 * 而非 Item#getFoodProperties()，因此 @Redirect 目标必须是前者。
 */
@Mixin(net.minecraft.world.entity.LivingEntity.class)
public abstract class LivingEntityEatMixin {

    @Redirect(
            method = "addEatEffect(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getFoodProperties(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/food/FoodProperties;", remap = false))
    private FoodProperties miq$scaleEffects(ItemStack stack, LivingEntity entity) {
        FoodProperties scaled = EatContext.get();
        return scaled != null ? scaled : stack.getFoodProperties(entity);
    }
}
