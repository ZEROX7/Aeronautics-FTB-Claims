package com.mapter.aeroclaims_ftb.mixin;

import com.mapter.aeroclaims_ftb.claim.Claim;
import com.mapter.aeroclaims_ftb.claim.ClaimManager;
import com.mapter.aeroclaims_ftb.config.aeroclaims_ftbConfig;
import com.mapter.aeroclaims_ftb.protect.CreateProtectionHelper;
import com.mapter.aeroclaims_ftb.protect.IPlacerTracked;
import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.UUID;

@Mixin(value = BlockBreakingKineticBlockEntity.class, remap = false)
public class MixinBlockBreakingKineticBlockEntity {

    @Shadow(remap = false)
    protected BlockPos breakingPos;

    @ModifyVariable(
            method = "tick",
            remap = false,
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
            ),
            name = "stateToBreak"
    )
    private BlockState aeroclaims_ftb$protectClaimedBlock(BlockState original) {
        BlockBreakingKineticBlockEntity self = (BlockBreakingKineticBlockEntity) (Object) this;
        Level level = self.getLevel();

        if (level == null || level.isClientSide() || breakingPos == null) {
            return original;
        }
        if (!aeroclaims_ftbConfig.KINETIC_BLOCK_PROTECTION.get()) {
            return original;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return original;
        }

        Claim claim = ClaimManager.getClaimAt(serverLevel, breakingPos);
        if (claim == null || !claim.isActive()) {
            return original;
        }

        UUID placerUUID = null;
        if (self instanceof IPlacerTracked tracked) {
            placerUUID = tracked.aeroclaims_ftb$getPlacerUUID();
        }

        if (CreateProtectionHelper.isBreakingAllowedForPlacer(placerUUID, serverLevel, claim)) {
            return original;
        }

        return Blocks.BEDROCK.defaultBlockState();
    }
}
