package cofh.redstonearsenal.common.network.data.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static cofh.lib.util.constants.ModIds.ID_REDSTONE_ARSENAL;

public record FluxShieldingPayload(int curCharges, int maxCharges) implements CustomPacketPayload {

    public static final Type<FluxShieldingPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ID_REDSTONE_ARSENAL, "flux_shielding_packet"));
    public static final StreamCodec<FriendlyByteBuf, FluxShieldingPayload> STREAM_CODEC = StreamCodec.ofMember(FluxShieldingPayload::write, FluxShieldingPayload::new);

    public FluxShieldingPayload(final FriendlyByteBuf buf) {

        this(buf.readInt(), buf.readInt());
    }

    private void write(FriendlyByteBuf buf) {

        buf.writeInt(curCharges);
        buf.writeInt(maxCharges);
    }

    @Override
    public Type<FluxShieldingPayload> type() {

        return TYPE;
    }

}
