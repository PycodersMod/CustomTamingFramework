package com.pycoder.customtamingframework.pet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Data variable storage for CTF entities.
 *
 * Each variable stores the current value, total value, total mode,
 * count mode, minimum value, lower-bound policy, reversible policy
 * and stage metadata.
 */
public final class DataVariables {
    private static final String ROOT = "CustomTamingFramework";
    private static final String KEY = "DataVariables";

    private static final String FIELD_CURRENT = "current";
    private static final String FIELD_TOTAL = "total";
    private static final String FIELD_TOTAL_MODE = "total_mode";
    private static final String FIELD_COUNT_MODE = "count_mode";
    private static final String FIELD_MINIMUM = "minimum";
    private static final String FIELD_NO_LOWER_BOUND = "no_lower_bound";
    private static final String FIELD_REVERSIBLE = "reversible";
    private static final String FIELD_STAGES = "stages";
    private static final String FIELD_STAGE_VALUE = "value";
    private static final String FIELD_STAGE_NAME = "name";
    private static final String FIELD_STAGE_INFINITE = "infinite";
    private static final String FIELD_STAGE_ENABLED = "enabled";

    private DataVariables() {
    }

    public enum TotalMode {
        CUMULATIVE("cumulative"),
        STAGE("stage");

        private final String snbtKey;

        TotalMode(String snbtKey) {
            this.snbtKey = snbtKey;
        }

        public String snbtKey() {
            return snbtKey;
        }

        public static TotalMode fromString(String value) {
            if (value != null && value.equalsIgnoreCase(STAGE.snbtKey)) {
                return STAGE;
            }
            return CUMULATIVE;
        }
    }

    public enum CountMode {
        CUMULATIVE("cumulative"),
        STAGE("stage");

        private final String snbtKey;

        CountMode(String snbtKey) {
            this.snbtKey = snbtKey;
        }

        public String snbtKey() {
            return snbtKey;
        }

        public static CountMode fromString(String value) {
            if (value != null && value.equalsIgnoreCase(STAGE.snbtKey)) {
                return STAGE;
            }
            return CUMULATIVE;
        }
    }

    public record StageEntry(String value, String name, boolean infinite, boolean enabled) {
        public StageEntry {
            value = value == null ? "" : value.trim();
            name = name == null ? "" : name.trim();
        }

        public StageEntry withValue(String next) {
            return new StageEntry(next, name, infinite, enabled);
        }

        public StageEntry withName(String next) {
            return new StageEntry(value, next, infinite, enabled);
        }

        public StageEntry withInfinite(boolean next) {
            return new StageEntry(value, name, next, enabled);
        }

        public StageEntry withEnabled(boolean next) {
            return new StageEntry(value, name, infinite, next);
        }
    }

    public static final class VariableState {
        private final String current;
        private final String total;
        private final TotalMode totalMode;
        private final CountMode countMode;
        private final String minimum;
        private final boolean noLowerBound;
        private final boolean reversible;
        private final List<StageEntry> stages;

        public VariableState(String current, String total, TotalMode totalMode, List<StageEntry> stages) {
            this(current, total, totalMode, CountMode.CUMULATIVE, "0", false, false, stages);
        }

        public VariableState(String current, String total, TotalMode totalMode, CountMode countMode,
                             String minimum, boolean noLowerBound, boolean reversible, List<StageEntry> stages) {
            this.current = current == null ? "" : current.trim();
            this.total = total == null ? "" : total.trim();
            this.totalMode = totalMode == null ? TotalMode.CUMULATIVE : totalMode;
            this.countMode = countMode == null ? CountMode.CUMULATIVE : countMode;
            this.minimum = minimum == null ? "0" : minimum.trim();
            this.noLowerBound = noLowerBound;
            this.reversible = reversible;
            this.stages = stages == null ? List.of() : List.copyOf(stages);
        }

        public String current() {
            return current;
        }

        public String total() {
            return total;
        }

        public TotalMode totalMode() {
            return totalMode;
        }

        public CountMode countMode() {
            return countMode;
        }

        public String minimum() {
            return minimum;
        }

        public boolean noLowerBound() {
            return noLowerBound;
        }

        public boolean reversible() {
            return reversible;
        }

        public List<StageEntry> stages() {
            return stages;
        }

        public VariableState withCurrent(String value) {
            return new VariableState(value, total, totalMode, countMode, minimum, noLowerBound, reversible, stages);
        }

        public VariableState withTotal(String value) {
            return new VariableState(current, value, totalMode, countMode, minimum, noLowerBound, reversible, stages);
        }

        public VariableState withMode(TotalMode mode) {
            return new VariableState(current, total, mode, countMode, minimum, noLowerBound, reversible, stages);
        }

        public VariableState withCountMode(CountMode mode) {
            return new VariableState(current, total, totalMode, mode, minimum, noLowerBound, reversible, stages);
        }

        public VariableState withMinimum(String value) {
            return new VariableState(current, total, totalMode, countMode, value, noLowerBound, reversible, stages);
        }

        public VariableState withNoLowerBound(boolean value) {
            return new VariableState(current, total, totalMode, countMode, minimum, value, reversible, stages);
        }

        public VariableState withReversible(boolean value) {
            return new VariableState(current, total, totalMode, countMode, minimum, noLowerBound, value, stages);
        }

        public VariableState withStages(List<StageEntry> nextStages) {
            return new VariableState(current, total, totalMode, countMode, minimum, noLowerBound, reversible, nextStages);
        }

        public VariableState withStage(int index, StageEntry stage) {
            List<StageEntry> next = new ArrayList<>(stages);
            if (index >= 0 && index < next.size()) {
                next.set(index, stage);
            }
            return withStages(next);
        }
    }

    public static Map<String, String> getAll(LivingEntity entity) {
        return toDisplayMap(getAllStates(entity));
    }

    public static Map<String, VariableState> getAllStates(LivingEntity entity) {
        CompoundTag root = entity.getPersistentData().getCompound(ROOT);
        CompoundTag vars = root.getCompound(KEY);
        return fromCompoundStates(vars);
    }

    public static String get(LivingEntity entity, String key) {
        VariableState state = getState(entity, key);
        return state != null ? state.current() : "";
    }

    public static String getTotal(LivingEntity entity, String key) {
        VariableState state = getState(entity, key);
        return state != null ? state.total() : "";
    }

    public static VariableState getState(LivingEntity entity, String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return getAllStates(entity).get(key);
    }

    public static String set(LivingEntity entity, String key, String value) {
        return setCurrent(entity, key, value);
    }

    public static String setCurrent(LivingEntity entity, String key, String value) {
        if (key == null || key.isBlank()) {
            return "";
        }
        Map<String, VariableState> states = new TreeMap<>(getAllStates(entity));
        if (value == null || value.isBlank()) {
            states.remove(key);
            importAll(entity, states);
            return "";
        }
        VariableState next = updateCurrent(states.get(key), value);
        states.put(key, next);
        importAll(entity, states);
        return next.current();
    }

    public static String addCurrent(LivingEntity entity, String key, int delta) {
        return addCurrent(entity, key, BigDecimal.valueOf(delta));
    }

    public static String addCurrent(LivingEntity entity, String key, BigDecimal delta) {
        if (key == null || key.isBlank()) {
            return "";
        }
        Map<String, VariableState> states = new TreeMap<>(getAllStates(entity));
        VariableState next = addCurrent(states.get(key), delta);
        states.put(key, next);
        importAll(entity, states);
        return next.current();
    }

    public static void remove(LivingEntity entity, String key) {
        setCurrent(entity, key, "");
    }

    public static void importAll(LivingEntity entity, Map<String, VariableState> data) {
        CompoundTag root = entity.getPersistentData().getCompound(ROOT).copy();
        root.put(KEY, toCompound(data));
        entity.getPersistentData().put(ROOT, root);
    }

    public static CompoundTag toCompound(LivingEntity entity) {
        return toCompound(getAllStates(entity));
    }

    public static CompoundTag toCompound(Map<String, VariableState> data) {
        CompoundTag result = new CompoundTag();
        Map<String, VariableState> sorted = data == null ? Map.of() : new TreeMap<>(data);
        for (Map.Entry<String, VariableState> entry : sorted.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            result.put(entry.getKey(), toStateCompound(sanitizeState(entry.getValue())));
        }
        return result;
    }

    public static Map<String, VariableState> fromCompoundStates(CompoundTag tag) {
        Map<String, VariableState> map = new TreeMap<>();
        if (tag == null) {
            return map;
        }
        for (String key : tag.getAllKeys()) {
            if (key == null || key.isBlank()) {
                continue;
            }
            if (tag.contains(key, Tag.TAG_COMPOUND)) {
                map.put(key, fromStateCompound(tag.getCompound(key)));
            } else {
                map.put(key, createState(tag.getString(key)));
            }
        }
        return map;
    }

    public static Map<String, String> fromCompound(CompoundTag tag) {
        return toDisplayMap(fromCompoundStates(tag));
    }

    public static Map<String, String> toDisplayMap(Map<String, VariableState> states) {
        Map<String, String> result = new TreeMap<>();
        if (states == null) {
            return result;
        }
        for (Map.Entry<String, VariableState> entry : states.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            result.put(entry.getKey(), displayString(sanitizeState(entry.getValue())));
        }
        return result;
    }

    public static Map<String, String> toDisplayMap(CompoundTag tag) {
        return toDisplayMap(fromCompoundStates(tag));
    }

    public static VariableState createState(String current) {
        String normalized = normalizeNumericText(current);
        return new VariableState(normalized, normalized, TotalMode.CUMULATIVE, CountMode.CUMULATIVE,
                "0", false, false, List.of());
    }

    public static VariableState setInitialValue(VariableState state, String initial) {
        VariableState safe = sanitizeState(state);
        String normalized = normalizeNumericText(initial);
        if (safe.totalMode() == TotalMode.STAGE) {
            VariableState next = new VariableState(normalized, safe.total(), safe.totalMode(), safe.countMode(),
                    safe.minimum(), safe.noLowerBound(), safe.reversible(), safe.stages());
            return recalculateStageTotal(next);
        }
        return new VariableState(normalized, normalized, safe.totalMode(), safe.countMode(),
                safe.minimum(), safe.noLowerBound(), safe.reversible(), safe.stages());
    }

    public static VariableState updateCurrent(VariableState state, String newCurrent) {
        VariableState safe = sanitizeState(state);
        BigDecimal nextCurrent = parseDecimal(newCurrent);
        if (safe.totalMode() == TotalMode.STAGE) {
            return applyStageMode(safe, nextCurrent);
        }
        BigDecimal oldCurrent = parseDecimal(safe.current());
        BigDecimal oldTotal = parseDecimal(safe.total());
        BigDecimal nextTotal = oldTotal;
        if (nextCurrent.compareTo(oldCurrent) > 0) {
            nextTotal = oldTotal.add(nextCurrent.subtract(oldCurrent));
        }
        return new VariableState(formatDecimal(nextCurrent), formatDecimal(nextTotal), safe.totalMode(), safe.countMode(),
                safe.minimum(), safe.noLowerBound(), safe.reversible(), safe.stages());
    }

    public static VariableState addCurrent(VariableState state, BigDecimal delta) {
        VariableState safe = sanitizeState(state);
        BigDecimal add = delta == null ? BigDecimal.ZERO : delta;
        BigDecimal nextCurrent = parseDecimal(safe.current()).add(add);
        if (safe.totalMode() == TotalMode.STAGE) {
            return applyStageMode(safe, nextCurrent);
        }
        BigDecimal oldTotal = parseDecimal(safe.total());
        BigDecimal nextTotal = oldTotal;
        if (add.compareTo(BigDecimal.ZERO) > 0) {
            nextTotal = oldTotal.add(add);
        }
        return new VariableState(formatDecimal(nextCurrent), formatDecimal(nextTotal), safe.totalMode(), safe.countMode(),
                safe.minimum(), safe.noLowerBound(), safe.reversible(), safe.stages());
    }

    public static VariableState setTotalMode(VariableState state, TotalMode mode) {
        VariableState safe = sanitizeState(state);
        TotalMode nextMode = mode == null ? TotalMode.CUMULATIVE : mode;
        VariableState next = new VariableState(safe.current(), safe.total(), nextMode, safe.countMode(),
                safe.minimum(), safe.noLowerBound(), safe.reversible(), safe.stages());
        if (nextMode == TotalMode.STAGE) {
            next = ensureStageDefaults(next);
            return recalculateStageTotal(next);
        }
        return next;
    }

    public static VariableState setCountMode(VariableState state, CountMode mode) {
        VariableState safe = sanitizeState(state);
        CountMode nextMode = mode == null ? CountMode.CUMULATIVE : mode;
        VariableState next = new VariableState(safe.current(), safe.total(), safe.totalMode(), nextMode,
                safe.minimum(), safe.noLowerBound(), safe.reversible(), safe.stages());
        if (next.totalMode() == TotalMode.STAGE) {
            return recalculateStageTotal(next);
        }
        return next;
    }

    public static VariableState setMinimum(VariableState state, String minimum) {
        VariableState safe = sanitizeState(state);
        VariableState next = new VariableState(safe.current(), safe.total(), safe.totalMode(), safe.countMode(),
                normalizeNumericText(minimum), safe.noLowerBound(), safe.reversible(), safe.stages());
        if (next.totalMode() == TotalMode.STAGE) {
            return recalculateStageTotal(next);
        }
        return next;
    }

    public static VariableState setNoLowerBound(VariableState state, boolean value) {
        VariableState safe = sanitizeState(state);
        VariableState next = new VariableState(safe.current(), safe.total(), safe.totalMode(), safe.countMode(),
                safe.minimum(), value, safe.reversible(), safe.stages());
        if (next.totalMode() == TotalMode.STAGE) {
            return recalculateStageTotal(next);
        }
        return next;
    }

    public static VariableState setReversible(VariableState state, boolean value) {
        VariableState safe = sanitizeState(state);
        VariableState next = new VariableState(safe.current(), safe.total(), safe.totalMode(), safe.countMode(),
                safe.minimum(), safe.noLowerBound(), value, safe.stages());
        if (next.totalMode() == TotalMode.STAGE) {
            return recalculateStageTotal(next);
        }
        return next;
    }

    public static VariableState setStages(VariableState state, List<StageEntry> stages) {
        VariableState safe = sanitizeState(state);
        VariableState next = new VariableState(safe.current(), safe.total(), safe.totalMode(), safe.countMode(),
                safe.minimum(), safe.noLowerBound(), safe.reversible(), sanitizeStages(stages, safe.totalMode() == TotalMode.STAGE));
        if (next.totalMode() == TotalMode.STAGE) {
            return recalculateStageTotal(next);
        }
        return next;
    }

    public static VariableState setStageValue(VariableState state, int index, String value) {
        VariableState safe = sanitizeState(state);
        List<StageEntry> stages = new ArrayList<>(safe.stages());
        if (index < 0 || index >= stages.size()) {
            return safe;
        }
        StageEntry entry = stages.get(index);
        stages.set(index, entry.withValue(normalizeNumericText(value)));
        return setStages(safe, stages);
    }

    public static VariableState setStageName(VariableState state, int index, String name) {
        VariableState safe = sanitizeState(state);
        List<StageEntry> stages = new ArrayList<>(safe.stages());
        if (index < 0 || index >= stages.size()) {
            return safe;
        }
        StageEntry entry = stages.get(index);
        stages.set(index, entry.withName(name));
        return setStages(safe, stages);
    }

    public static VariableState setInfiniteStageEnabled(VariableState state, boolean enabled) {
        VariableState safe = sanitizeState(state);
        List<StageEntry> stages = new ArrayList<>(safe.stages());
        int infiniteIndex = findInfiniteStageIndex(stages);
        if (infiniteIndex < 0) {
            stages.add(new StageEntry("", defaultInfiniteStageName(), true, enabled));
        } else {
            stages.set(infiniteIndex, stages.get(infiniteIndex).withEnabled(enabled));
        }
        return setStages(safe, stages);
    }

    public static VariableState setInfiniteStageName(VariableState state, String name) {
        VariableState safe = sanitizeState(state);
        List<StageEntry> stages = new ArrayList<>(safe.stages());
        int infiniteIndex = findInfiniteStageIndex(stages);
        if (infiniteIndex < 0) {
            stages.add(new StageEntry("", name, true, true));
        } else {
            stages.set(infiniteIndex, stages.get(infiniteIndex).withName(name));
        }
        return setStages(safe, stages);
    }

    public static VariableState addFiniteStage(VariableState state) {
        VariableState safe = sanitizeState(state);
        List<StageEntry> stages = new ArrayList<>(safe.stages());
        int infiniteIndex = findInfiniteStageIndex(stages);
        String defaultValue = stages.isEmpty() ? "0" : normalizeNumericText(lastFiniteValue(stages));
        if (!stages.isEmpty()) {
            try {
                defaultValue = formatDecimal(parseDecimal(lastFiniteValue(stages)).add(BigDecimal.ONE));
            } catch (Exception ignored) {
                defaultValue = "0";
            }
        }
        StageEntry next = new StageEntry(defaultValue, defaultStageName(countFiniteStages(stages) + 1), false, true);
        if (infiniteIndex < 0) {
            stages.add(next);
        } else {
            stages.add(infiniteIndex, next);
        }
        return setStages(safe, stages);
    }

    public static VariableState removeFiniteStage(VariableState state, int index) {
        VariableState safe = sanitizeState(state);
        List<StageEntry> stages = new ArrayList<>(safe.stages());
        List<Integer> finiteIndexes = new ArrayList<>();
        for (int i = 0; i < stages.size(); i++) {
            if (!stages.get(i).infinite()) {
                finiteIndexes.add(i);
            }
        }
        if (index < 0 || index >= finiteIndexes.size()) {
            return safe;
        }
        stages.remove((int) finiteIndexes.get(index));
        return setStages(safe, stages);
    }

    public static VariableState resetMinimum(VariableState state) {
        VariableState safe = sanitizeState(state);
        return setMinimum(safe, "0");
    }

    public static VariableState recalculateStageTotal(VariableState state) {
        VariableState safe = sanitizeState(state);
        if (safe.totalMode() != TotalMode.STAGE) {
            return safe;
        }
        return adjustStageModeTotal(safe, parseDecimal(safe.current()));
    }

    public static boolean isNumericText(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        int start = 0;
        if (trimmed.charAt(0) == '-') {
            if (trimmed.length() == 1) {
                return false;
            }
            start = 1;
        }
        int dots = 0;
        int digits = 0;
        for (int i = start; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '.') {
                dots++;
                if (dots > 1) {
                    return false;
                }
                continue;
            }
            if (c < '0' || c > '9') {
                return false;
            }
            digits++;
        }
        return digits > 0;
    }

    public static String normalizeNumericText(String value) {
        if (value == null) {
            return "0";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "0";
        }
        if (isInfinityText(trimmed)) {
            return "∞";
        }
        try {
            return formatDecimal(new BigDecimal(trimmed));
        } catch (NumberFormatException ignored) {
            return "0";
        }
    }

    public static BigDecimal parseDecimal(String value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || isInfinityText(trimmed)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(trimmed);
        } catch (NumberFormatException ignored) {
            return BigDecimal.ZERO;
        }
    }

    public static String formatDecimal(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.scale() < 0) {
            normalized = normalized.setScale(0);
        }
        return normalized.toPlainString();
    }

    private static VariableState fromStateCompound(CompoundTag compound) {
        String current = compound.contains(FIELD_CURRENT) ? compound.getString(FIELD_CURRENT) : compound.getString("value");
        String total = compound.contains(FIELD_TOTAL) ? compound.getString(FIELD_TOTAL) : current;
        TotalMode totalMode = TotalMode.fromString(compound.getString(FIELD_TOTAL_MODE));
        CountMode countMode = CountMode.fromString(compound.getString(FIELD_COUNT_MODE));
        String minimum = compound.contains(FIELD_MINIMUM) ? compound.getString(FIELD_MINIMUM) : "0";
        boolean noLowerBound = compound.contains(FIELD_NO_LOWER_BOUND) && compound.getBoolean(FIELD_NO_LOWER_BOUND);
        boolean reversible = compound.contains(FIELD_REVERSIBLE) && compound.getBoolean(FIELD_REVERSIBLE);
        List<StageEntry> stages = readStages(compound);
        VariableState state = new VariableState(current, total, totalMode, countMode, minimum, noLowerBound, reversible, stages);
        return sanitizeState(state);
    }

    private static List<StageEntry> readStages(CompoundTag compound) {
        if (!compound.contains(FIELD_STAGES, Tag.TAG_LIST)) {
            return List.of();
        }
        Tag raw = compound.get(FIELD_STAGES);
        if (!(raw instanceof ListTag list)) {
            return List.of();
        }
        List<StageEntry> stages = new ArrayList<>();
        int finiteIndex = 0;
        for (Tag element : list) {
            if (element instanceof CompoundTag entryTag) {
                boolean infinite = entryTag.contains(FIELD_STAGE_INFINITE) && entryTag.getBoolean(FIELD_STAGE_INFINITE);
                boolean enabled = !entryTag.contains(FIELD_STAGE_ENABLED) || entryTag.getBoolean(FIELD_STAGE_ENABLED);
                String value = entryTag.contains(FIELD_STAGE_VALUE) ? entryTag.getString(FIELD_STAGE_VALUE) : "";
                String name = entryTag.contains(FIELD_STAGE_NAME) ? entryTag.getString(FIELD_STAGE_NAME) : "";
                if (infinite) {
                    if (name.isBlank()) {
                        name = defaultInfiniteStageName();
                    }
                    stages.add(new StageEntry("", name, true, enabled));
                } else {
                    if (name.isBlank()) {
                        name = defaultStageName(++finiteIndex);
                    } else {
                        finiteIndex++;
                    }
                    stages.add(new StageEntry(normalizeNumericText(value), name, false, true));
                }
            } else if (element instanceof StringTag stringTag) {
                finiteIndex++;
                stages.add(new StageEntry(normalizeNumericText(stringTag.getAsString()), defaultStageName(finiteIndex), false, true));
            } else {
                finiteIndex++;
                stages.add(new StageEntry("0", defaultStageName(finiteIndex), false, true));
            }
        }
        return List.copyOf(stages);
    }

    private static CompoundTag toStateCompound(VariableState state) {
        VariableState safe = sanitizeState(state);
        CompoundTag tag = new CompoundTag();
        tag.putString(FIELD_CURRENT, safe.current());
        tag.putString(FIELD_TOTAL, safe.total());
        tag.putString(FIELD_TOTAL_MODE, safe.totalMode().snbtKey());
        tag.putString(FIELD_COUNT_MODE, safe.countMode().snbtKey());
        tag.putString(FIELD_MINIMUM, safe.minimum());
        tag.putBoolean(FIELD_NO_LOWER_BOUND, safe.noLowerBound());
        tag.putBoolean(FIELD_REVERSIBLE, safe.reversible());
        ListTag stages = new ListTag();
        for (StageEntry entry : safe.stages()) {
            CompoundTag stage = new CompoundTag();
            stage.putString(FIELD_STAGE_VALUE, entry.value());
            stage.putString(FIELD_STAGE_NAME, entry.name());
            stage.putBoolean(FIELD_STAGE_INFINITE, entry.infinite());
            stage.putBoolean(FIELD_STAGE_ENABLED, entry.enabled());
            stages.add(stage);
        }
        tag.put(FIELD_STAGES, stages);
        return tag;
    }

    private static VariableState sanitizeState(VariableState state) {
        if (state == null) {
            return createState("0");
        }
        List<StageEntry> stages = sanitizeStages(state.stages(), state.totalMode() == TotalMode.STAGE);
        VariableState safe = new VariableState(
                normalizeNumericText(state.current()),
                normalizeTotalText(state.total()),
                state.totalMode(),
                state.countMode(),
                normalizeNumericText(state.minimum()),
                state.noLowerBound(),
                state.reversible(),
                stages
        );
        if (safe.totalMode() == TotalMode.STAGE) {
            safe = adjustStageModeTotal(ensureStageDefaults(safe), parseDecimal(safe.current()));
        }
        return safe;
    }

    private static VariableState ensureStageDefaults(VariableState state) {
        List<StageEntry> stages = new ArrayList<>(state.stages());
        List<StageEntry> finite = finiteStages(stages);
        StageEntry infinite = infiniteStage(stages);
        if (finite.isEmpty()) {
            String seed = isNumericText(state.total()) ? normalizeNumericText(state.total()) : normalizeNumericText(state.current());
            if (seed.isBlank() || "0".equals(seed)) {
                seed = "0";
            }
            finite.add(new StageEntry(seed, defaultStageName(1), false, true));
        }
        if (infinite == null) {
            infinite = new StageEntry("", defaultInfiniteStageName(), true, true);
        }
        List<StageEntry> next = new ArrayList<>(finite);
        next.add(infinite);
        return new VariableState(state.current(), state.total(), state.totalMode(), state.countMode(),
                state.minimum(), state.noLowerBound(), state.reversible(), next);
    }

    private static List<StageEntry> sanitizeStages(List<StageEntry> stages, boolean ensureInfinite) {
        if (stages == null || stages.isEmpty()) {
            return List.of();
        }
        List<StageEntry> finite = new ArrayList<>();
        StageEntry infinite = null;
        for (StageEntry entry : stages) {
            if (entry == null) {
                continue;
            }
            if (entry.infinite()) {
                if (infinite == null) {
                    infinite = entry;
                }
            } else {
                finite.add(new StageEntry(normalizeNumericText(entry.value()), entry.name(), false, true));
            }
        }
        finite.sort(Comparator.comparing(stage -> parseDecimal(stage.value())));
        for (int i = 0; i < finite.size(); i++) {
            StageEntry entry = finite.get(i);
            if (entry.name().isBlank()) {
                finite.set(i, entry.withName(defaultStageName(i + 1)));
            }
        }
        if (infinite == null && ensureInfinite) {
            infinite = new StageEntry("", defaultInfiniteStageName(), true, true);
        }
        List<StageEntry> result = new ArrayList<>(finite);
        if (infinite != null) {
            String name = infinite.name().isBlank() ? defaultInfiniteStageName() : infinite.name();
            result.add(new StageEntry("", name, true, infinite.enabled()));
        }
        return List.copyOf(result);
    }

    private static VariableState applyStageMode(VariableState state, BigDecimal nextCurrent) {
        VariableState safe = ensureStageDefaults(sanitizeState(state));
        if (safe.countMode() == CountMode.STAGE) {
            return applyStageCountMode(safe, nextCurrent);
        }
        return applyStageCumulativeMode(safe, nextCurrent);
    }

    private static VariableState adjustStageModeTotal(VariableState state, BigDecimal nextCurrent) {
        VariableState safe = ensureStageDefaults(state);
        if (safe.countMode() == CountMode.STAGE) {
            return applyStageCountMode(safe, nextCurrent);
        }
        return applyStageCumulativeMode(safe, nextCurrent);
    }

    private static VariableState applyStageCumulativeMode(VariableState state, BigDecimal nextCurrent) {
        int currentStage = stageIndexByTotal(state.total(), state);
        int targetStage = stageIndexForAbsolute(nextCurrent, state);
        if (!state.reversible() && targetStage < currentStage) {
            targetStage = currentStage;
            nextCurrent = lowerBoundForStage(currentStage, state);
        }
        if (!state.noLowerBound()) {
            BigDecimal minimum = parseDecimal(state.minimum());
            if (nextCurrent.compareTo(minimum) < 0) {
                nextCurrent = minimum;
            }
        }
        String total = stageTotalForIndex(targetStage, state);
        String name = stageNameForIndex(targetStage, state);
        return new VariableState(formatDecimal(nextCurrent), total, TotalMode.STAGE, state.countMode(),
                state.minimum(), state.noLowerBound(), state.reversible(), state.stages());
    }

    private static VariableState applyStageCountMode(VariableState state, BigDecimal nextCurrent) {
        int stageIndex = stageIndexByTotal(state.total(), state);
        if (stageIndex < 0) {
            stageIndex = 0;
        }
        BigDecimal current = nextCurrent;
        List<StageEntry> finite = finiteStages(state.stages());
        boolean hasInfinite = hasInfiniteStage(state);

        if (current.compareTo(BigDecimal.ZERO) < 0) {
            if (state.reversible()) {
                while (current.compareTo(BigDecimal.ZERO) < 0 && stageIndex > 0) {
                    stageIndex--;
                    BigDecimal prevTotal = parseDecimal(stageTotalForIndex(stageIndex, state));
                    current = prevTotal.add(current);
                }
            }
            if (current.compareTo(BigDecimal.ZERO) < 0 && !state.noLowerBound()) {
                current = parseDecimal(state.minimum());
            }
        }

        String total = stageTotalForIndex(stageIndex, state);
        BigDecimal stageTotal = isInfinityText(total) ? null : parseDecimal(total);
        if (stageTotal != null && current.compareTo(stageTotal) >= 0) {
            int nextStage = stageIndex + 1;
            if (nextStage < finite.size() || (hasInfinite && nextStage == finite.size())) {
                stageIndex = nextStage;
                current = BigDecimal.ZERO;
                total = stageTotalForIndex(stageIndex, state);
            } else {
                current = stageTotal;
            }
        }

        if (isInfinityText(total) && current.compareTo(BigDecimal.ZERO) < 0 && state.reversible() && finite.size() > 0) {
            stageIndex = finite.size() - 1;
            BigDecimal prevTotal = parseDecimal(stageTotalForIndex(stageIndex, state));
            current = prevTotal.add(current);
            total = stageTotalForIndex(stageIndex, state);
        }

        if (!state.noLowerBound() && stageIndex == 0) {
            BigDecimal minimum = parseDecimal(state.minimum());
            if (current.compareTo(minimum) < 0) {
                current = minimum;
            }
        }

        return new VariableState(formatDecimal(current), total, TotalMode.STAGE, state.countMode(),
                state.minimum(), state.noLowerBound(), state.reversible(), state.stages());
    }

    private static int stageIndexByTotal(String total, VariableState state) {
        List<StageEntry> finite = finiteStages(state.stages());
        if (isInfinityText(total)) {
            return finite.size();
        }
        BigDecimal totalValue = parseDecimal(total);
        for (int i = 0; i < finite.size(); i++) {
            if (parseDecimal(finite.get(i).value()).compareTo(totalValue) == 0) {
                return i;
            }
        }
        if (state.current() != null && !state.current().isBlank()) {
            return stageIndexForAbsolute(parseDecimal(state.current()), state);
        }
        return 0;
    }

    private static int stageIndexForAbsolute(BigDecimal current, VariableState state) {
        List<StageEntry> finite = finiteStages(state.stages());
        if (finite.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < finite.size(); i++) {
            BigDecimal threshold = parseDecimal(finite.get(i).value());
            if (current.compareTo(threshold) <= 0) {
                return i;
            }
        }
        if (hasInfiniteStage(state)) {
            return finite.size();
        }
        return Math.max(0, finite.size() - 1);
    }

    private static BigDecimal lowerBoundForStage(int index, VariableState state) {
        List<StageEntry> finite = finiteStages(state.stages());
        if (index <= 0) {
            return parseDecimal(state.minimum());
        }
        int finiteIndex = Math.min(index - 1, finite.size() - 1);
        return parseDecimal(finite.get(finiteIndex).value());
    }

    private static String stageTotalForIndex(int index, VariableState state) {
        List<StageEntry> finite = finiteStages(state.stages());
        if (index < 0) {
            return finite.isEmpty() ? "0" : finite.get(0).value();
        }
        if (index < finite.size()) {
            return finite.get(index).value();
        }
        StageEntry infinite = infiniteStage(state.stages());
        if (infinite != null && infinite.enabled()) {
            return "∞";
        }
        return finite.isEmpty() ? "0" : finite.get(finite.size() - 1).value();
    }

    private static String stageNameForIndex(int index, VariableState state) {
        List<StageEntry> finite = finiteStages(state.stages());
        if (index < 0) {
            return finite.isEmpty() ? defaultStageName(1) : stageNameOrDefault(finite.get(0), 1);
        }
        if (index < finite.size()) {
            return stageNameOrDefault(finite.get(index), index + 1);
        }
        StageEntry infinite = infiniteStage(state.stages());
        if (infinite != null && infinite.enabled()) {
            return infinite.name().isBlank() ? defaultInfiniteStageName() : infinite.name();
        }
        return finite.isEmpty() ? defaultStageName(1) : stageNameOrDefault(finite.get(finite.size() - 1), finite.size());
    }

    private static String stageNameOrDefault(StageEntry entry, int index) {
        if (entry == null || entry.name().isBlank()) {
            return defaultStageName(index);
        }
        return entry.name();
    }

    private static String displayString(VariableState state) {
        VariableState safe = sanitizeState(state);
        if (safe.totalMode() == TotalMode.STAGE) {
            String name = stageNameForIndex(stageIndexByTotal(safe.total(), safe), safe);
            return safe.current() + "/" + safe.total() + "（" + name + "）";
        }
        return safe.current() + "/" + safe.total();
    }

    private static List<StageEntry> finiteStages(List<StageEntry> stages) {
        List<StageEntry> finite = new ArrayList<>();
        if (stages == null) {
            return finite;
        }
        for (StageEntry stage : stages) {
            if (stage != null && !stage.infinite()) {
                finite.add(stage);
            }
        }
        return finite;
    }

    private static int countFiniteStages(List<StageEntry> stages) {
        return finiteStages(stages).size();
    }

    private static String lastFiniteValue(List<StageEntry> stages) {
        List<StageEntry> finite = finiteStages(stages);
        if (finite.isEmpty()) {
            return "0";
        }
        return finite.get(finite.size() - 1).value();
    }

    private static int findInfiniteStageIndex(List<StageEntry> stages) {
        if (stages == null) {
            return -1;
        }
        for (int i = 0; i < stages.size(); i++) {
            StageEntry stage = stages.get(i);
            if (stage != null && stage.infinite()) {
                return i;
            }
        }
        return -1;
    }

    private static StageEntry infiniteStage(List<StageEntry> stages) {
        if (stages == null) {
            return null;
        }
        for (int i = stages.size() - 1; i >= 0; i--) {
            StageEntry stage = stages.get(i);
            if (stage != null && stage.infinite()) {
                return stage;
            }
        }
        return null;
    }

    private static boolean hasInfiniteStage(VariableState state) {
        StageEntry infinite = infiniteStage(state.stages());
        return infinite != null && infinite.enabled();
    }

    private static boolean isInfinityText(String value) {
        return value != null && value.trim().equals("∞");
    }

    private static String normalizeTotalText(String value) {
        if (value == null) {
            return "0";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "0";
        }
        if (isInfinityText(trimmed)) {
            return "∞";
        }
        try {
            return formatDecimal(new BigDecimal(trimmed));
        } catch (NumberFormatException ignored) {
            return "0";
        }
    }

    private static String defaultStageName(int index) {
        return switch (index) {
            case 1 -> "一阶";
            case 2 -> "二阶";
            case 3 -> "三阶";
            case 4 -> "四阶";
            case 5 -> "五阶";
            case 6 -> "六阶";
            case 7 -> "七阶";
            case 8 -> "八阶";
            case 9 -> "九阶";
            case 10 -> "十阶";
            default -> index + "阶";
        };
    }

    private static String defaultInfiniteStageName() {
        return "无穷大";
    }
}
