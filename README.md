<!--
  MIQ — Minecraft Daily Desire
  Bilingual project README (English / 中文). The two languages are kept in separate
  sections and are NOT mixed, so the file can be pasted directly into either a
  CurseForge or a Modrinth project description (remove the section in the other language).
-->

# MIQ — Minecraft Daily Desire

A Minecraft 1.21.1 NeoForge mod that makes food interesting again. Every game day,
each food in the world is secretly assigned a "desire" category. Whether that food
turns out to be something you crave, something you are willing to eat, or something
you really do not want today changes how satiating it is — and rewards or punishes
you accordingly.

## English

### Overview

Each refresh, every edible item is randomly assigned one of three categories:

- **Very want to eat** – eating it is genuinely rewarding.
- **Willing to eat** – no special effect, just the vanilla food.
- **Don't want to eat** – eating it comes with a real downside.

The category for each item is visible on the item tooltip, and a per-player chat
message announces what you are craving right now. When two players on the same
server speak different languages, each one sees the announcement and item names in
their own client language.

### Main Features

- **Daily desire assignment.** The recipe re-rolls automatically at a configurable
  moment each game day. You can also force a refresh with the in-game command.
- **Reward for cravings.** Eating a "very want to eat" food restores more hunger and
  saturation (+25% by default), lengthens any food-granted effects (+25%), and grants
  a bonus Luck effect (60 seconds).
- **Penalty for the unwanted.** Eating a "don't want to eat" food restores less hunger
  (approximate −50%), shortens food-granted effects (−25%), and has a small chance to
  briefly apply Hunger or Nausea.
- **Smarter hunger over time.** The more a food is eaten by anyone on the server since
  the last refresh, the more likely it is to be re-rolled as "don't want", and the less
  likely as "very want". This naturally discourages overeating a single item. The
  tally resets on every refresh.
- **Fixed favorites.** You can pin specific items to a fixed category from the config,
  so they are always very-want, willing, or don't-want and never randomized.
- **AppleSkin compatibility.** When AppleSkin is installed, the hunger and saturation
  bars it shows match the mod's real scaling.
- **Fully bilingual.** All in-game text, the config file, and this document are
  available in both English and Chinese.

### Basic Configuration

The configuration is generated automatically as `config/miq-common.toml` in the
game directory. It is commented in English and Chinese and is split into groups:

- `[recipe]`
  - `recipeUpdateNodes` – tick-of-day values (0 to 24000) at which the recipe is
    refreshed each game day. Default `[3000]`.
- `[desireReward]`
  - `hungerBoost` – extra hunger restore for "very want" food. Default `0.25`.
  - `effectDurationBoost` – added duration for food-granted effects. Default `0.25`.
  - `extraLuck` – whether to grant the bonus Luck effect (`true` / `false`).
  - `luckDurationSeconds` – duration of that Luck effect in seconds. Default `60`.
- `[desirePenalty]`
  - `enablePenalty` – master switch for the "don't want" category. When `false`, no food
    is ever placed there.
  - `hungerReduce` – hunger-restore reduction for "don't want" food. Default `0.5`.
  - `effectDurationReduce` – duration reduction for food-granted effects. Default `0.25`.
  - `penaltyEffectChance` – chance (0.05 = 5%) of getting a short Hunger or Nausea.
  - `hungerDurationSeconds` / `nauseaDurationSeconds` – durations of those effects.
- `[desireWeights]`
  - `weights` – three integers: very-want, willing, don't-want. Default `[1, 5, 4]`.
  - `eatCountAffinity` – how strongly eating a food pushes it toward "don't want" and
    away from "very want". `0` disables this. Default `0.2`.
- `[fixedPreferences]`
  - `fixedVeryWant` – item registry IDs always treated as "very want".
    Default `["minecraft:enchanted_golden_apple"]`.
  - `fixedWilling` – item registry IDs always treated as "willing". Default empty.
  - `fixedDontWant` – item registry IDs always treated as "don't want".
    Default `["minecraft:spider_eye", "minecraft:pufferfish"]`.

Valid item IDs look like `minecraft:cooked_porkchop`. Changes take effect on the next
recipe refresh. A food listed in a fixed group is removed from the random pool, so it
can never be swapped to another category.

### Commands

- `/miq refresh` – immediately re-roll the daily desire recipe (requires operator
  permission level 2).

### Requirement

Minecraft Java Edition 1.21.1 with NeoForge. AppleSkin is optional and recommended if
you want the tooltip to reflect the modified hunger and saturation values.

---

## 中文

### 简介

本模组让食物重新变得有意思。每个游戏日里，世界上每种食物都会被秘密打上一个
"食欲"分类。它到底是"今天想吃"的东西、可以接受的东西、还是"今天不想吃"的
东西，会直接影响它带来的饱腹感，并给你相应的奖励或惩罚。

### 主要功能

- **每日食欲分配。** 每到可配置的时刻，系统会在每个游戏日自动重新随机划分食谱，
  你也可以用游戏内指令立刻强制刷新。
- **想吃的奖励。** 吃"很想吃"的食物时，饱食度与饱和度恢复提升（默认 +25%），
  食物自带效果的时长延长（+25%），并额外获得一段幸运效果（默认 60 秒）。
- **不想吃的惩罚。** 吃"不想吃"的食物时，饱食度恢复降低（默认约 −50%），
  自带效果时长缩短（−25%），并有一定概率短暂获得饥饿或反胃效果。
- **越吃越看淡。** 从上一次刷新以来，全服玩家某种食物吃得越多，下次刷新时它就越
  容易变成"不想吃"、越难变成"很想吃"。这样能自然避免只盯着一种食物猛吃。
  统计会在每次刷新时自动清空。
- **固定喜好。** 你可以在配置里把指定物品钉在固定分类，让它们永远属于"很想吃"、
  "愿意吃"或"不想吃"，不参与随机。
- **AppleSkin 兼容。** 安装 AppleSkin 后，它显示的饱食度/饱和度条会与本模组的
  真实缩放数值一致。
- **完全双语。** 游戏内文字、配置文件以及本文档均提供中文和英文两种语言。

### 基础配置说明

配置文件会在游戏目录下自动生成：`config/miq-common.toml`。文件内含中英双语注释，
并按类别分组：

- `[recipe]`
  - `recipeUpdateNodes` – 每日食谱更新的时刻（0 到 24000 的整数）。默认 `[3000]`。
- `[desireReward]`
  - `hungerBoost` – "很想吃"食物额外提升的饱食度恢复量。默认 `0.25`。
  - `effectDurationBoost` – 食物自带效果时长的延长量。默认 `0.25`。
  - `extraLuck` – 是否额外给予幸运效果（`true` / `false`）。
  - `luckDurationSeconds` – 幸运效果时长（秒）。默认 `60`。
- `[desirePenalty]`
  - `enablePenalty` – "不想吃"分类的总开关。设为 `false` 后不会有食物被划入该分类。
  - `hungerReduce` – "不想吃"食物的饱食度恢复降低量。默认 `0.5`。
  - `effectDurationReduce` – 食物自带效果时长的缩短量。默认 `0.25`。
  - `penaltyEffectChance` – 有该概率（0.05 表示 5%）获得短暂的饥饿或反胃效果。
  - `hungerDurationSeconds` / `nauseaDurationSeconds` – 上述效果的持续时长（秒）。
- `[desireWeights]`
  - `weights` – 三个整数，依次表示：很想吃、愿意吃、不想吃。默认 `[1, 5, 4]`。
  - `eatCountAffinity` – 进食次数把某食物推向"不想吃"、拉离"很想吃"的强度。
    填 `0` 可关闭。默认 `0.2`。
- `[fixedPreferences]`
  - `fixedVeryWant` – 永远划入"很想吃"分类的物品 ID 列表。
    默认 `["minecraft:enchanted_golden_apple"]`。
  - `fixedWilling` – 永远划入"愿意吃"分类的物品 ID 列表。默认空。
  - `fixedDontWant` – 永远划入"不想吃"分类的物品 ID 列表。
    默认 `["minecraft:spider_eye", "minecraft:pufferfish"]`。

有效的物品 ID 形如 `minecraft:cooked_porkchop`。修改会在下次刷新食谱时生效。
被列入固定分组的食物会从随机池中移除，永远不会被换到其它分类。

### 游戏内指令

- `/miq refresh` – 立即重新随机生成每日欲望食谱（需要 OP 权限等级 2）。

### 运行要求

Minecraft Java 版 1.21.1 + NeoForge。AppleSkin 为可选模组，若希望工具提示能反映
被缩放后的饱食度/饱和度数值，推荐一并安装。