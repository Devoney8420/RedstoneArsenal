package cofh.redstonearsenal.common.network;

import cofh.redstonearsenal.common.network.data.client.FluxShieldingPayload;
import cofh.redstonearsenal.common.network.packet.client.FluxShieldingPacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class PacketHandler {

    public static void registerNetworking(final RegisterPayloadHandlersEvent event) {

        final PayloadRegistrar registrar = event.registrar("1");

        // CLIENT
        registrar.playToClient(FluxShieldingPayload.TYPE, FluxShieldingPayload.STREAM_CODEC, FluxShieldingPacket.get()::handle);
    }

}
