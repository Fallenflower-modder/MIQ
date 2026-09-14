package com.cloversecho.miq.mixin;

import com.cloversecho.miq.recipe.DailyRecipeManager;
import com.cloversecho.miq.util.EatContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * In 1.20.1 {@link Player#eat(Level, ItemStack)} does not hold a FoodProperties parameter:
 * hunger/saturation are applied through {@code FoodData.eat(Item, ItemStack)} and the food
 * effects through {@code LivingEntity.addEatEffect(...)}, each re-fetching the item's food
 * properties internally. This mixin computes the desire-scaled FoodProperties once at the
 * entry of {@code Player.eat} and exposes it via {@link EatContext} for the redirect mixins
 * in {@code FoodData} and {@code LivingEntity} to consume.
 *
 * 在 1.20.1 中 Player.eat(Level, ItemStack) 不再接收 FoodProperties 参数：
 * 饱食度/饱和度由 FoodData.eat(Item, ItemStack) 应用、食物效果由
 * LivingEntity.addEatEffect(...) 应用，二者内部都会重新获取食物属性。
 * 本 Mixin 在 Player.eat 入口处一次性计算缩放后的 FoodProperties，
 * 并通过 {@link EatContext} 提供给 FoodData / LivingEntity 中的重定向 Mixin 使用。
 */
@Mixin(Player.class)
public abstract class PlayerEatMixin {

    @Inject(
            method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"))
    private void miq$beginEat(Level level, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        Player self = (Player) (Object) this;
        // Use the ItemStack-aware overload so that mod-provided per-stack food properties are kept:
        // KaleidoscopeCookery's cuisine-quality ratio (nutrition/saturation/effect duration) and
        // Some Assembly Required's sandwich aggregate are both applied inside this method.
        // 使用感知 ItemStack 的 getFoodProperties 重载，保留各模组基于物品堆叠的食物属性：
        // KaleidoscopeCookery 的食物品质偏移（饱食度/饱和度/效果时长）与
        // Some Assembly Required 的三明治成分汇总都在该方法内部生效。
        FoodProperties base = stack.getItem().getFoodProperties(stack, self);
        if (base == null) {
            EatContext.clear();
            return;
        }
        EatContext.set(DailyRecipeManager.buildModifiedFoodProperties(base, self, stack));
    }

    @Inject(
            method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"))
    private void miq$endEat(Level level, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        EatContext.clear();
    }
}
