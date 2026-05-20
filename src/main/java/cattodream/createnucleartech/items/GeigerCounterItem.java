package cattodream.createnucleartech.items;

import cattodream.createnucleartech.radiation.RadiationData;
import cattodream.createnucleartech.radiation.RadiationEvents;
import cattodream.createnucleartech.radiation.RadiationMaterials;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class GeigerCounterItem extends Item {
    private static final String LAST_AUTOMATIC_READING_KEY = "CreateNuclearTechLastGeigerReadingTick";
    private static final int AUTOMATIC_READING_INTERVAL = 20;
    public static final String RADS_PER_SECOND_KEY = "CreateNuclearTechRadsPerSecond";
    public static final String FIELD_RADIATION_KEY = "CreateNuclearTechFieldRadiation";
    public static final String BODY_RADIATION_KEY = "CreateNuclearTechBodyRadiation";
    public static final String LAST_UPDATE_KEY = "CreateNuclearTechGeigerUpdatedAt";

    public GeigerCounterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            GeigerReading reading = updateReading(serverLevel, player, stack);
            double toxicity = player.getPersistentData().getDouble(RadiationEvents.TOXICITY_LEVEL_KEY);
            player.displayClientMessage(Component.literal("Chunk rad/s: " + format(reading.chunkRadiation())
                    + " | Field rad/s: " + format(reading.fieldRadiation())
                    + " | Body radiation: " + format(reading.bodyRadiation())
                    + " | Toxicity: " + format(toxicity)).withStyle(colorFor(reading.radsPerSecond())), true);
        }
        return new InteractionResultHolder<>(net.minecraft.world.InteractionResult.SUCCESS_NO_ITEM_USED, stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof Player player)) {
            return;
        }
        if (!isSelected && player.getOffhandItem() != stack) {
            return;
        }
        long gameTime = serverLevel.getGameTime();
        if (gameTime % AUTOMATIC_READING_INTERVAL != 0) {
            return;
        }
        if (player.getPersistentData().getLong(LAST_AUTOMATIC_READING_KEY) == gameTime) {
            return;
        }
        player.getPersistentData().putLong(LAST_AUTOMATIC_READING_KEY, gameTime);
        updateReading(serverLevel, player, stack);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    private static GeigerReading updateReading(ServerLevel level, Player player, ItemStack stack) {
        RadiationData radiationData = RadiationData.get(level);
        double chunkRadiation = radiationData.chunkRadiationAt(player.blockPosition());
        double fieldRadiation = radiationData.radiationAt(level, player.blockPosition());
        double carriedRadiation = carriedRadiation(player);
        double radsPerSecond = Math.max(0.0D, chunkRadiation);
        double dose = player.getPersistentData().getDouble(RadiationEvents.RADIATION_LEVEL_KEY);
        GeigerReading reading = new GeigerReading(radsPerSecond, chunkRadiation, fieldRadiation, dose);

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putDouble(RADS_PER_SECOND_KEY, reading.radsPerSecond());
        tag.putDouble("CreateNuclearTechChunkRadiation", reading.chunkRadiation());
        tag.putDouble(FIELD_RADIATION_KEY, reading.fieldRadiation());
        tag.putDouble(BODY_RADIATION_KEY, reading.bodyRadiation());
        tag.putLong(LAST_UPDATE_KEY, level.getGameTime());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return reading;
    }

    private static double carriedRadiation(Player player) {
        double total = 0.0D;
        for (ItemStack stack : player.getInventory().items) {
            total += RadiationMaterials.radiationFor(stack);
        }
        for (ItemStack stack : player.getInventory().armor) {
            total += RadiationMaterials.radiationFor(stack);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            total += RadiationMaterials.radiationFor(stack);
        }
        return total;
    }

    private static String format(double value) {
        return String.format("%.2f", value);
    }

    private static ChatFormatting colorFor(double radiation) {
        if (radiation >= 8.0D) {
            return ChatFormatting.RED;
        }
        if (radiation >= 2.0D) {
            return ChatFormatting.YELLOW;
        }
        return ChatFormatting.GREEN;
    }

    private record GeigerReading(double radsPerSecond, double chunkRadiation, double fieldRadiation, double bodyRadiation) {
    }
}
