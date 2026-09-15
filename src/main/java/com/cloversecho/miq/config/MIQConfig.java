package com.cloversecho.miq.config;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Mod configuration (Fabric port). The generated file is {@code config/miq-common.toml}
 * and keeps the same sections, keys, defaults and bilingual comments as the NeoForge version.
 *
 * 模组配置文件（Fabric 移植版）。生成位置为 {@code config/miq-common.toml}，
 * 与 NeoForge 版本保持相同的分组、键名、默认值与中英双语注释。
 *
 * <p>The file is written with default values on first access, then lazily re-read whenever it
 * has been modified (at most once per second), so edits take effect at the next recipe refresh
 * or eat, without a restart. Malformed values never crash the game: they log a WARN and fall
 * back to the default.
 *
 * 配置文件在首次访问时按默认值生成；之后每秒钟最多检测一次文件变化并重新解析，
 * 因此修改后无需重启即可在下次刷新/进食时生效。非法值不会导致游戏崩溃，
 * 只会输出 WARN 日志并回退到默认值。
 */
public final class MIQConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("miq");

    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("miq-common.toml");

    /** Parsed values keyed by {@code section.key}. 以 section.key 为键保存的解析结果。 */
    private static final Map<String, Object> VALUES = new HashMap<>();

    private static final long RELOAD_INTERVAL_MS = 1000L;
    private static volatile long lastCheck = 0L;
    private static volatile long lastStamp = -1L;

    // ---------------------------------------------------------------------
    // Daily recipe / 每日食谱
    // ---------------------------------------------------------------------

    /**
     * Tick-of-day values (0~24000) at which the recipe is refreshed.
     * 每日食谱更新节点列表（0~24000 内的整数数组）。
     */
    public static final ConfigValue<List<Integer>> RECIPE_UPDATE_NODES = new ConfigValue<>(
            "recipe.recipeUpdateNodes", List.of(3000),
            raw -> {
                List<Integer> out = new ArrayList<>();
                for (Object o : asList(raw)) {
                    if (o instanceof Number n && n.intValue() >= 0 && n.intValue() < 24000) {
                        out.add(n.intValue());
                    }
                }
                return List.copyOf(out);
            });

    // ---------------------------------------------------------------------
    // Desire reward (very-want-to-eat) / 欲望奖励（今天很想吃）
    // ---------------------------------------------------------------------

    /** Hunger-restore boost as a fraction (0.25 = +25%). 饱食度恢复提升量。 */
    public static final ConfigValue<Double> VERY_WANT_HUNGER_BOOST = new ConfigValue<>(
            "desireReward.hungerBoost", 0.25,
            raw -> inRange(asDouble(raw), 0.0, 10.0, "desireReward.hungerBoost"));

    /** Effect-duration boost as a fraction (0.25 = +25%). 食物自带效果时长提升量。 */
    public static final ConfigValue<Double> VERY_WANT_EFFECT_DURATION_BOOST = new ConfigValue<>(
            "desireReward.effectDurationBoost", 0.25,
            raw -> inRange(asDouble(raw), 0.0, 10.0, "desireReward.effectDurationBoost"));

    /** Weighted effect table granted on 'very want', as a JSON string. 奖励效果表（JSON 字符串）。 */
    public static final ConfigValue<String> REWARD_EFFECTS = new ConfigValue<>(
            "desireReward.rewardEffects",
            "[[[{\"id\":\"minecraft:luck\",\"time\":1200,\"lvl\":1}],1]]",
            raw -> asString(raw, "desireReward.rewardEffects"));

    // ---------------------------------------------------------------------
    // Desire penalty / 欲望惩罚（今天不想吃）
    // ---------------------------------------------------------------------

    /** Master switch for the 'don't want' penalty. 欲望惩罚总开关。 */
    public static final ConfigValue<Boolean> ENABLE_PENALTY = new ConfigValue<>(
            "desirePenalty.enablePenalty", true,
            raw -> raw instanceof Boolean b ? b : true);

    /** Hunger-restore reduction as a fraction (0.5 = -50%). 饱食度恢复降低量。 */
    public static final ConfigValue<Double> DONT_WANT_HUNGER_REDUCE = new ConfigValue<>(
            "desirePenalty.hungerReduce", 0.5,
            raw -> inRange(asDouble(raw), 0.0, 1.0, "desirePenalty.hungerReduce"));

    /** Effect-duration reduction as a fraction (0.25 = -25%). 食物自带效果时长缩减量。 */
    public static final ConfigValue<Double> DONT_WANT_EFFECT_DURATION_REDUCE = new ConfigValue<>(
            "desirePenalty.effectDurationReduce", 0.25,
            raw -> inRange(asDouble(raw), 0.0, 1.0, "desirePenalty.effectDurationReduce"));

    /** Weighted effect table granted on 'don't want', as a JSON string. 惩罚效果表（JSON 字符串）。 */
    public static final ConfigValue<String> PENALTY_EFFECTS = new ConfigValue<>(
            "desirePenalty.penaltyEffects",
            "[[[{\"id\":\"minecraft:hunger\",\"time\":60,\"lvl\":1}],1],"
                    + "[[{\"id\":\"minecraft:nausea\",\"time\":60,\"lvl\":1}],1],"
                    + "[[],38]]",
            raw -> asString(raw, "desirePenalty.penaltyEffects"));

    // ---------------------------------------------------------------------
    // Desire generation weights / 欲望生成比例
    // ---------------------------------------------------------------------

    /** Weights in order (very-want, willing, don't-want). 三个整数权重：很想吃、愿意吃、不想吃。 */
    public static final ConfigValue<List<Integer>> DESIRE_WEIGHTS = new ConfigValue<>(
            "desireWeights.weights", List.of(1, 5, 4),
            raw -> {
                List<Integer> out = new ArrayList<>();
                for (Object o : asList(raw)) {
                    if (o instanceof Number n && n.intValue() > 0) {
                        out.add(n.intValue());
                    }
                }
                if (out.size() < 3) {
                    throw new IllegalArgumentException("desireWeights.weights needs at least 3 positive integers");
                }
                return List.copyOf(out);
            });

    /** Per-eat weight shift strength; 0 disables the influence. 进食次数对食谱生成的权重影响强度。 */
    public static final ConfigValue<Double> EAT_COUNT_AFFINITY = new ConfigValue<>(
            "desireWeights.eatCountAffinity", 0.2,
            raw -> inRange(asDouble(raw), 0.0, 10.0, "desireWeights.eatCountAffinity"));

    // ---------------------------------------------------------------------
    // Fixed preferences / 固定喜好分类
    // ---------------------------------------------------------------------

    /** Items always placed in the 'very want' category. 固定划入“很想吃”分类的物品 ID 列表。 */
    public static final ConfigValue<List<String>> FIXED_VERY_WANT = new ConfigValue<>(
            "fixedPreferences.fixedVeryWant", List.of("minecraft:enchanted_golden_apple"),
            MIQConfig::asStringList);

    /** Items always placed in the 'willing' category. 固定划入“愿意吃”分类的物品 ID 列表。 */
    public static final ConfigValue<List<String>> FIXED_WILLING = new ConfigValue<>(
            "fixedPreferences.fixedWilling", List.of(),
            MIQConfig::asStringList);

    /** Items always placed in the 'don't want' category. 固定划入“不想吃”分类的物品 ID 列表。 */
    public static final ConfigValue<List<String>> FIXED_DONT_WANT = new ConfigValue<>(
            "fixedPreferences.fixedDontWant", List.of("minecraft:spider_eye", "minecraft:pufferfish"),
            MIQConfig::asStringList);

    private MIQConfig() {
    }

    // ------------------------------------------------------------------
    // Value access / 取值
    // ------------------------------------------------------------------

    /** Re-reads the config file if it has changed (throttled). 若文件已变化则重新读取配置。 */
    private static void refresh() {
        long now = System.currentTimeMillis();
        if (now - lastCheck < RELOAD_INTERVAL_MS) {
            return;
        }
        lastCheck = now;
        if (!Files.exists(CONFIG_PATH)) {
            writeDefaults();
        }
        long stamp;
        try {
            stamp = Files.getLastModifiedTime(CONFIG_PATH).toMillis();
        } catch (IOException e) {
            return;
        }
        if (stamp == lastStamp) {
            return;
        }
        lastStamp = stamp;
        try {
            Map<String, Object> parsed = parse(Files.readAllLines(CONFIG_PATH, StandardCharsets.UTF_8));
            VALUES.clear();
            VALUES.putAll(parsed);
        } catch (IOException e) {
            LOGGER.warn("[MIQ] Failed to read config {}: {}", CONFIG_PATH, e.getMessage());
        }
    }

    private static void writeDefaults() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, defaultFile(), StandardCharsets.UTF_8);
            LOGGER.info("[MIQ] Generated default config at {}", CONFIG_PATH);
        } catch (IOException e) {
            LOGGER.warn("[MIQ] Failed to write default config {}: {}", CONFIG_PATH, e.getMessage());
        }
    }

    /** Builds the default config file text with bilingual comments. 生成带中英双语注释的默认配置文件。 */
    private static String defaultFile() {
        StringBuilder sb = new StringBuilder();
        section(sb, "recipe", "Daily recipe / 每日食谱",
                "Controls when the daily recipe is re-generated each game day.",
                "控制每个游戏日何时重新生成每日食谱。");
        entry(sb, "recipeUpdateNodes", List.of(3000),
                "Tick-of-day values at which the daily recipe updates (0 to 24000, integer array). ",
                "One update is fired per crossed node per game day; the update-only-once rule applies even when",
                "several nodes are skipped over in a single tick.",
                "每日食谱更新时刻（0~24000 的整数数组，允许多个）。",
                "系统会在每个游戏日的这些时刻各更新一次食谱；即使一次 tick 越过多个节点也只会触发一次更新。");

        section(sb, "desireReward", "Desire reward, applied when eating an item marked 'very want to eat'.",
                "欲望奖励参数：当食用被划分为“今天很想吃”的食物时生效。");
        entry(sb, "hungerBoost", 0.25,
                "Hunger-restore increase as a fraction (0.25 = +25%). / 饱食度恢复提升量（0.25 表示提高 25%）。");
        entry(sb, "effectDurationBoost", 0.25,
                "Boost to the duration of food-granted effects as a fraction (0.25 = +25%).",
                "/ 食用食物获得的效果时长提升量（0.25 表示延长 25%）。");
        entry(sb, "rewardEffects",
                "[[[{\"id\":\"minecraft:luck\",\"time\":1200,\"lvl\":1}],1]]",
                "Weighted effect table granted when eating a 'very want' food, as a JSON string.",
                "Format: [[[{\"id\":\"effectId\",\"time\":ticks,\"lvl\":level},...],weight],...].",
                "One entry is rolled by its weight; all effects in the chosen entry are applied.",
                "An empty effects array [] grants nothing. 'time' is in ticks (20 ticks = 1 second).",
                "Default grants Luck I for 1200 ticks (60 s) with weight 1.",
                "奖励效果表（JSON 字符串）：食用“很想吃”食物时按权重随机施加的一组效果。",
                "格式：[[[{\"id\":\"效果ID\",\"time\":刻数,\"lvl\":等级},...],权重],...]。",
                "按权重随机选出一组并施加其中所有效果；空数组 [] 表示不施加任何效果。",
                "time 单位为刻（20 刻 = 1 秒）。默认：权重 1，给予 1200 刻（60 秒）幸运 I。",
                "示例：rewardEffects = '[[[{\"id\":\"minecraft:luck\",\"time\":1200,\"lvl\":1}],1]]'");

        section(sb, "desirePenalty", "Desire penalty, applied when eating an item marked 'don't want to eat'.",
                "欲望惩罚参数：当食用被划分为“今天不想吃”的食物时生效。");
        entry(sb, "enablePenalty", true,
                "Master switch for the 'don't want to eat' penalty. When disabled, no food is put into the",
                "'don't want' category.",
                "/ 是否启用欲望惩罚。关闭后不会有食物被划分到“不想吃”列表。");
        entry(sb, "hungerReduce", 0.5,
                "Hunger-restore reduction as a fraction (0.5 = -50%). / 饱食度恢复降低量（0.5 表示降低 50%）。");
        entry(sb, "effectDurationReduce", 0.25,
                "Reduction to the duration of food-granted effects as a fraction (0.25 = -25%).",
                "/ 食用食物获得的效果时长缩减量（0.25 表示缩短 25%）。");
        entry(sb, "penaltyEffects",
                "[[[{\"id\":\"minecraft:hunger\",\"time\":60,\"lvl\":1}],1],"
                        + "[[{\"id\":\"minecraft:nausea\",\"time\":60,\"lvl\":1}],1],"
                        + "[[],38]]",
                "Weighted effect table granted when eating a 'don't want' food, as a JSON string.",
                "Same format and rules as rewardEffects; the last entry usually is an empty effects array [].",
                "Unknown effect ids only produce a WARN log line, never a crash.",
                "惩罚效果表（JSON 字符串）：食用“不想吃”食物时按权重随机施加的一组效果。",
                "格式与 rewardEffects 相同；通常最后一个条目为空数组 [] 表示“无效果”。",
                "未知的效果 ID 只输出 WARN 日志，不会导致游戏崩溃。",
                "示例：penaltyEffects = '[[[{\"id\":\"minecraft:hunger\",\"time\":60,\"lvl\":1}],1],[[{\"id\":\"minecraft:nausea\",\"time\":60,\"lvl\":1}],1],[[],38]]'");

        section(sb, "desireWeights", "Random weights used when assigning each food to a desire category.",
                "用于把每种食物随机划分到某一欲望分类的权重。");
        entry(sb, "weights", List.of(1, 5, 4),
                "Three positive integers, in order: very-want, willing, don't-want.",
                "Default [1, 5, 4] ≈ 10% / 50% / 40%. When the desire penalty is disabled,",
                "the 'don't want' weight is ignored and the remaining two are used.",
                "三个正整数，顺序为：很想吃、愿意吃、不想吃。",
                "默认 [1, 5, 4]，约等于 10% / 50% / 40%。",
                "当欲望惩罚关闭时，“不想吃”的权重会被忽略，只使用前两个。");
        entry(sb, "eatCountAffinity", 0.2,
                "Per-eat weight shift when generating the recipe. The more a food was eaten, the more it is",
                "pushed toward 'don't want' and away from 'very want'. 0 disables this influence.",
                "进食次数对食谱生成的权重影响强度：被吃得越多的食物，越容易被划到“不想吃”、",
                "越难被划到“很想吃”。0 表示关闭该影响。");

        section(sb, "fixedPreferences",
                "Item IDs that are always forced into a fixed desire category when the recipe is refreshed.",
                "固定分类配置：这些食物的物品 ID 在刷新食谱时会被强制划分到指定分类（不参与随机）。");
        entry(sb, "fixedVeryWant", List.of("minecraft:enchanted_golden_apple"),
                "Item registry IDs always placed in the 'very want' category, e.g. minecraft:enchanted_golden_apple.",
                "固定划入“很想吃”分类的物品 ID 列表。例如 minecraft:enchanted_golden_apple。");
        entry(sb, "fixedWilling", List.of(),
                "Item registry IDs always placed in the 'willing' category.",
                "固定划入“愿意吃”分类的物品 ID 列表。");
        entry(sb, "fixedDontWant", List.of("minecraft:spider_eye", "minecraft:pufferfish"),
                "Item registry IDs always placed in the 'don't want' category, e.g. minecraft:spider_eye.",
                "固定划入“不想吃”分类的物品 ID 列表。例如 minecraft:spider_eye。");
        return sb.toString();
    }

    private static void section(StringBuilder sb, String name, String... comments) {
        sb.append('\n');
        for (String c : comments) {
            sb.append('#').append(c).append('\n');
        }
        sb.append('\n');
        sb.append('[').append(name).append("]\n");
    }

    private static void entry(StringBuilder sb, String key, Object value, String... comments) {
        for (String c : comments) {
            sb.append("\t#").append(c).append('\n');
        }
        sb.append('\t').append(key).append(" = ").append(formatValue(value)).append('\n');
        sb.append('\n');
    }

    /** Formats a value as TOML. 将配置值格式化为 TOML 文本。 */
    private static String formatValue(Object value) {
        if (value instanceof String s) {
            StringBuilder sb = new StringBuilder("\"");
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\n' -> sb.append("\\n");
                    case '\t' -> sb.append("\\t");
                    default -> sb.append(c);
                }
            }
            return sb.append('"').toString();
        }
        if (value instanceof Boolean || value instanceof Number) {
            return value.toString();
        }
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(formatValue(list.get(i)));
            }
            return sb.append(']').toString();
        }
        return String.valueOf(value);
    }

    // ------------------------------------------------------------------
    // Minimal TOML parser (only what this config needs) / 精简 TOML 解析器
    // ------------------------------------------------------------------

    private static Map<String, Object> parse(List<String> lines) {
        Map<String, Object> map = new HashMap<>();
        String section = "";
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim();
                continue;
            }
            int eq = line.indexOf('=');
            if (eq < 0) {
                continue; // malformed line: skip. 格式错误的行：跳过。
            }
            String key = line.substring(0, eq).trim();
            String value = stripInlineComment(line.substring(eq + 1).trim());
            if (key.isEmpty()) {
                continue;
            }
            try {
                map.put(section.isEmpty() ? key : section + "." + key, parseValue(value));
            } catch (IllegalArgumentException ex) {
                LOGGER.warn("[MIQ] Config: skipping invalid value for '{}.{}': {}", section, key, value);
            }
        }
        return map;
    }

    /** Removes a trailing {@code #} comment, keeping '#' inside quoted strings. 去掉行尾注释。 */
    private static String stripInlineComment(String s) {
        if (s.startsWith("\"")) {
            int end = basicStringEnd(s);
            return end >= 0 ? s.substring(0, end + 1) : s;
        }
        if (s.startsWith("'")) {
            int end = s.indexOf('\'', 1);
            return end >= 0 ? s.substring(0, end + 1) : s;
        }
        int idx = s.indexOf('#');
        return idx >= 0 ? s.substring(0, idx).trim() : s.trim();
    }

    /** Index of the closing quote of a TOML basic string, honouring backslash escapes. */
    private static int basicStringEnd(String s) {
        boolean escaped = false;
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    private static Object parseValue(String s) {
        if (s.startsWith("\"")) {
            return unescapeBasic(s.substring(1, basicStringEnd(s)));
        }
        if (s.startsWith("'")) {
            int end = s.indexOf('\'', 1);
            return s.substring(1, end < 0 ? s.length() : end);
        }
        if (s.startsWith("[")) {
            String inner = s.substring(1, s.endsWith("]") ? s.length() - 1 : s.length()).trim();
            if (inner.isEmpty()) {
                return List.of();
            }
            List<Object> list = new ArrayList<>();
            for (String part : inner.split(",")) {
                String p = part.trim();
                if (!p.isEmpty()) {
                    list.add(parseValue(p));
                }
            }
            return list;
        }
        if ("true".equals(s)) {
            return Boolean.TRUE;
        }
        if ("false".equals(s)) {
            return Boolean.FALSE;
        }
        if (s.matches("-?\\d+")) {
            return Integer.valueOf(s);
        }
        if (s.matches("-?\\d+\\.\\d+([eE][+-]?\\d+)?")) {
            return Double.valueOf(s);
        }
        throw new IllegalArgumentException("Unrecognized value: " + s);
    }

    private static String unescapeBasic(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        boolean escaped = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default -> sb.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Conversion helpers / 转换辅助
    // ------------------------------------------------------------------

    private static List<?> asList(Object raw) {
        return raw instanceof List<?> list ? list : List.of();
    }

    private static double asDouble(Object raw) {
        if (raw instanceof Number n) {
            return n.doubleValue();
        }
        throw new IllegalArgumentException("not a number: " + raw);
    }

    private static double inRange(double v, double min, double max, String key) {
        if (v >= min && v <= max) {
            return v;
        }
        throw new IllegalArgumentException(key + " must be in [" + min + ", " + max + "], got " + v);
    }

    private static String asString(Object raw, String key) {
        if (raw instanceof String s) {
            return s;
        }
        throw new IllegalArgumentException(key + " must be a string, got " + raw);
    }

    private static List<String> asStringList(Object raw) {
        List<String> out = new ArrayList<>();
        for (Object o : asList(raw)) {
            if (o instanceof String s) {
                out.add(s);
            }
        }
        return List.copyOf(out);
    }

    /**
     * A lazily-resolved configuration option backed by the parsed TOML file.
     * 一个由解析后的 TOML 文件支持的、按需读取的配置项。
     */
    public static final class ConfigValue<T> {

        private final String key;
        private final T defaultValue;
        private final Function<Object, T> convert;

        private ConfigValue(String key, T defaultValue, Function<Object, T> convert) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.convert = convert;
        }

        /** Returns the current value, falling back to the default if missing or invalid. 获取当前值。 */
        public T get() {
            MIQConfig.refresh();
            Object raw = VALUES.get(key);
            if (raw == null) {
                return defaultValue;
            }
            try {
                return convert.apply(raw);
            } catch (RuntimeException ex) {
                LOGGER.warn("[MIQ] Config '{}' is invalid ({}); using default value.", key, ex.getMessage());
                return defaultValue;
            }
        }
    }
}
