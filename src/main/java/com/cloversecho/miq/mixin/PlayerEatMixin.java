package com.cloversecho.miq.mixin;

import com.cloversecho.miq.recipe.DailyRecipeManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Scales the {@link FoodProperties} used in {@link Player#eat(Level, ItemStack, FoodProperties)}
 * so hunger restore, saturation and the granted effects reflect the item's daily desire category.
 *
 * 在 {@link Player#eat} 入口将传入的 FoodProperties 替换为按欲望分类缩放后的版本，
 * 从而影响饱食度恢复、饱和度与食物自带效果的时长。
 */
@Mixin(Player.class)
public abstract class PlayerEatMixin {

    @ModifyVariable(
            method = "eat",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private FoodProperties miq$scaleFood(FoodProperties foodProperties, Level level, ItemStack stack) {
        Player self = (Player) (Object) this;
        return DailyRecipeManager.buildModifiedFoodProperties(foodProperties, self, stack);
    }
}