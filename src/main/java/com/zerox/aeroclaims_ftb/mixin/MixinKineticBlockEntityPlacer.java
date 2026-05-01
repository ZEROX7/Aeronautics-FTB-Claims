package com.zerox.aeroclaims_ftb.mixin;

import com.zerox.aeroclaims_ftb.protect.IPlacerTracked;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(value = KineticBlockEntity.class, remap = false)
public class MixinKineticBlockEntityPlacer implements IPlacerTracked {

    @Unique
    private UUID aeroclaims_ftb$placerUUID;

    @Override
    public UUID aeroclaims_ftb$getPlacerUUID() {
        return aeroclaims_ftb$placerUUID;
    }

    @Override
    public void aeroclaims_ftb$setPlacerUUID(UUID uuid) {
        this.aeroclaims_ftb$placerUUID = uuid;
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void aeroclaims_ftb$onWrite(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if (aeroclaims_ftb$placerUUID != null) {
            compound.putUUID("aeroclaims_ftb:Placer", aeroclaims_ftb$placerUUID);
        }
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void aeroclaims_ftb$onRead(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if (compound.hasUUID("aeroclaims_ftb:Placer")) {
            aeroclaims_ftb$placerUUID = compound.getUUID("aeroclaims_ftb:Placer");
        }
    }
}
