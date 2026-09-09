package com.cloversecho.miq.util;

import net.minecraft.world.food.FoodProperties;

/**
 * Thread-confined holder for the desire-scaled {@link FoodProperties} of the eat currently
 * in progress.
 *
 * In 1.20.1 the eating flow is split across {@code Player.eat}, {@code FoodData.eat} and
 * {@code LivingEntity.addEatEffect}; each of the latter two re-fetches the item's food
 * properties through {@link net.minecraft.world.item.Item#getFoodProperties()}. To make both
 * the hunger/saturation restore and the food effects use one single scaled properties object
 * (including the rolled don't-want penalty), the {@code Player.eat} mixin computes it once at
 * HEAD and the two redirect mixins consume it here. Game logic runs on a single thread, so a
 * plain static field is safe.
 *
 * 在 1.20.1 中进食流程被拆分为 Player.eat / FoodData.eat / LivingEntity.addEatEffect 三段，
 * 后两者都会通过 Item#getFoodProperties() 重新获取食物属性。为了让饱食度/饱和度与食物效果
 * 都使用同一份缩放后的 FoodProperties（含一次性掷骰的“不想吃”惩罚效果），由 Player.eat 的
 * Mixin 在入口处统一计算并暂存于此，两个重定向 Mixin 在此取用。游戏逻辑单线程执行，静态字段安全。
 */
public final class EatContext {

    private static FoodProperties activeScaled;

    private EatContext() {
    }

    /** Stores the scaled properties for the ongoing eat (null-safe). 保存本次进食的缩放属性。 */
    public static void set(FoodProperties foodProperties) {
        activeScaled = foodProperties;
    }

    /** Returns the scaled properties, or {@code null} if no eat is in progress. 返回暂存的缩放属性。 */
    public static FoodProperties get() {
        return activeScaled;
    }

    /** Clears the context at the end of the eat. 进食结束时清空上下文。 */
    public static void clear() {
        activeScaled = null;
    }
}
