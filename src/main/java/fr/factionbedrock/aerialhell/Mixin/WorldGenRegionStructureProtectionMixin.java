package fr.factionbedrock.aerialhell.Mixin;

import fr.factionbedrock.aerialhell.Util.FeatureHelper;
import fr.factionbedrock.aerialhell.Util.WorldGenStructureProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(WorldGenRegion.class)
public class WorldGenRegionStructureProtectionMixin
{
    //Aerial Hell structure pieces in the writable area of this region, computed when first needed
    @Unique private List<BoundingBox> aerialhell$protectedBoxes;

    //features can't place (or remove) blocks inside Aerial Hell structure pieces
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"), cancellable = true)
    private void preventFeatureFromEditingStructure(BlockPos pos, BlockState blockState, int updateFlags, int updateLimit, CallbackInfoReturnable<Boolean> cir)
    {
        if (!WorldGenStructureProtection.isPlacingFeature()) {return;}

        if (this.aerialhell$protectedBoxes == null)
        {
            WorldGenRegion region = (WorldGenRegion) (Object) this;
            ChunkPos center = region.getCenter();
            BoundingBox writableArea = new BoundingBox(center.getMinBlockX() - 16, region.getMinY(), center.getMinBlockZ() - 16, center.getMaxBlockX() + 16, region.getMaxY(), center.getMaxBlockZ() + 16);
            this.aerialhell$protectedBoxes = FeatureHelper.getAerialHellStructurePieceBoxes(region, writableArea);
        }

        for (BoundingBox box : this.aerialhell$protectedBoxes)
        {
            if (box.isInside(pos)) {cir.setReturnValue(false); return;}
        }
    }
}
