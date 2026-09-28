package fr.factionbedrock.aerialhell.Util;

/*
 * Tells if the current worldgen thread is placing a feature (and not a structure).
 * While a feature is being placed, the blocks inside Aerial Hell structure pieces can't be modified (see WorldGenRegionStructureProtectionMixin).
 */
public class WorldGenStructureProtection
{
    private static final ThreadLocal<int[]> FEATURE_PLACEMENT_DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    public static void startFeaturePlacement() {FEATURE_PLACEMENT_DEPTH.get()[0]++;}

    public static void endFeaturePlacement() {FEATURE_PLACEMENT_DEPTH.get()[0]--;}

    public static boolean isPlacingFeature() {return FEATURE_PLACEMENT_DEPTH.get()[0] > 0;}
}
