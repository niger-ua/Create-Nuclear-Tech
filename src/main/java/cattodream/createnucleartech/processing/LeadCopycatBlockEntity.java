package cattodream.createnucleartech.processing;

import cattodream.createnucleartech.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LeadCopycatBlockEntity extends BlockEntity {
    private BlockState copiedState = Blocks.AIR.defaultBlockState();

    public LeadCopycatBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModRegistry.LEAD_COPYCAT_ENTITY.get(), pos, blockState);
    }

    public BlockState copiedState() {
        return copiedState;
    }

    public void setCopiedState(BlockState copiedState) {
        if (copiedState.is(ModRegistry.LEAD_COPYCAT.get()) || copiedState.hasBlockEntity()) {
            return;
        }
        this.copiedState = copiedState;
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            if (state.hasProperty(LeadCopycatBlock.COPIED)) {
                level.setBlock(worldPosition, state.setValue(LeadCopycatBlock.COPIED, !copiedState.isAir()), 3);
            } else {
                level.sendBlockUpdated(worldPosition, state, state, 3);
            }
        }
    }

    public ItemStack extractCopiedStack() {
        if (copiedState.isAir()) {
            return ItemStack.EMPTY;
        }
        Block copiedBlock = copiedState.getBlock();
        ItemStack stack = new ItemStack(copiedBlock);
        setCopiedState(Blocks.AIR.defaultBlockState());
        return stack;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("CopiedState", NbtUtils.writeBlockState(copiedState));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("CopiedState")) {
            copiedState = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("CopiedState"));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
