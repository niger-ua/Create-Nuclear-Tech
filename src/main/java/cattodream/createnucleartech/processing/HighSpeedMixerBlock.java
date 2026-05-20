package cattodream.createnucleartech.processing;

import cattodream.createnucleartech.ModRegistry;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlock;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class HighSpeedMixerBlock extends MechanicalMixerBlock {
    public HighSpeedMixerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<MechanicalMixerBlockEntity> getBlockEntityClass() {
        return (Class<MechanicalMixerBlockEntity>) (Class<?>) HighSpeedMixerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MechanicalMixerBlockEntity> getBlockEntityType() {
        return ModRegistry.HIGH_SPEED_MIXER_ENTITY.get();
    }
}
