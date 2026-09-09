package com.cloversecho.miq.mixin;

import com.cloversecho.miq.recipe.DailyRecipeManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Scales the food eaten by a player in Minecraft 26.1.
 *
 * <p>Since 26.1 removed {@code Player.eat}, consuming food now flows through
 * {@link Item#finishUsingItem} which reads the {@code DataComponents.FOOD}
 * (hunger/saturation) and {@code DataComponents.CONSUMABLE} (effects) of the
 * stack being used. This mixin swaps both components to desire-category-scaled
 * versions right before the vanilla consumption is applied, then restores the
 * originals afterwards so later bites always scale from the base food.
 *
 * <p>26.1 已移除 {@code Player.eat}，进食通过 {@link Item#finishUsingItem} 发生，该方法读取
 * 堆叠上的 FOOD（饱食度/饱和度）与 CONSUMABLE（效果）组件。本 Mixin 在实际进食前把两个组件
 * 替换为按欲望分类缩放后的版本，并在进食结束后恢复，确保后续每次进食都从基础食物重新缩放。
 */
@Mixin(Item.class)
public abstract class ItemEatMixin {

    /** Snapshot of the original components so they can be restored after the bite. */
    private record Components(FoodProperties food, Consumable consumable) {
    }

    /** One-shot swap in progress for the current (single-threaded) game tick, if any. */
    private static final ThreadLocal<Components> MIQ_SWAP = new ThreadLocal<>();

    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void miq$scaleStart(ItemStack stack, Level level, LivingEntity living,
                               CallbackInfoReturnable<ItemStack> cir) {
        if (!(living instanceof Player player) || MIQ_SWAP.get() != null) {
            return;
        }
        DailyRecipeManager.ModifiedFood scaled = DailyRecipeManager.buildScaled(stack, player);
        if (scaled != null) {
            MIQ_SWAP.set(new Components(stack.get(DataComponents.FOOD), stack.get(DataComponents.CONSUMABLE)));
            stack.set(DataComponents.FOOD, scaled.food());
            stack.set(DataComponents.CONSUMABLE, scaled.consumable());
        }
    }

    @Inject(method = "finishUsingItem", at = @At("RETURN"))
    private void miq$scaleEnd(ItemStack stack, Level level, LivingEntity living,
                              CallbackInfoReturnable<ItemStack> cir) {
        Components snapshot = MIQ_SWAP.get();
        if (snapshot == null) {
            return;
        }
        MIQ_SWAP.remove();
        ItemStack result = cir.getReturnValue();
        // finishUsingItem consumes from the same stack reference; restore the original components
        // only when a (non-empty) result remains, so the item never retains scaled food.
        if (result != null && !result.isEmpty()) {
            if (snapshot.food() != null) {
                result.set(DataComponents.FOOD, snapshot.food());
            } else {
                result.remove(DataComponents.FOOD);
            }
            if (snapshot.consumable() != null) {
                result.set(DataComponents.CONSUMABLE, snapshot.consumable());
            } else {
                result.remove(DataComponents.CONSUMABLE);
            }
        }
    }
}