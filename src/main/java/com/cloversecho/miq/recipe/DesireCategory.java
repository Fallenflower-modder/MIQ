package com.cloversecho.miq.recipe;

/**
 * The daily desire category a food is assigned to.
 * 每日食谱为每种食物划分的“进食欲望”分类。
 */
public enum DesireCategory {
    /** Today I really want to eat this. / 今天很想吃。 */
    VERY_WANT,
    /** Today eating this is acceptable. / 今天愿意吃。 */
    WILLING,
    /** Today I don't want to eat this. / 今天不想吃。 */
    DON_T_WANT
}