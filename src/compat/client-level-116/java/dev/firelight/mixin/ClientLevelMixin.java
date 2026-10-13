package dev.firelight.mixin;

import dev.firelight.client.FlameTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {
    @Inject(method = "sendBlockUpdated", at = @At("HEAD"))
    private void firelight$blockUpdated(BlockPos pos, BlockState before, BlockState after, int flags, CallbackInfo ci) {
        FlameTracker.blockChanged(pos, before, after);
    }

    @Inject(method = "onChunkLoaded", at = @At("HEAD"))
    private void firelight$chunkLoaded(int x, int z, CallbackInfo ci) {
        FlameTracker.chunkChanged((ClientLevel) (Object) this, x, z);
    }

    @Inject(method = "unload", at = @At("HEAD"))
    private void firelight$chunkUnloaded(LevelChunk chunk, CallbackInfo ci) {
        FlameTracker.chunkChanged((ClientLevel) (Object) this, chunk.getPos().x, chunk.getPos().z);
    }
}
