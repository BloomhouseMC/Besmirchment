package dev.mrsterner.besmirchment.common.packet;

import dev.mrsterner.besmirchment.common.Besmirchment;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class SparklePacket {
    public static final Identifier ID = Besmirchment.id("familiar_ability");

    public static void send(LivingEntity entity){
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(entity.getId());
        if (entity instanceof ServerPlayerEntity){
            ServerPlayNetworking.send((ServerPlayerEntity) entity, ID, buf);
        }
        PlayerLookup.tracking(entity).forEach(player -> ServerPlayNetworking.send(player, ID, buf));
    }

    @Environment(EnvType.CLIENT)
    public static void handle(MinecraftClient client, ClientPlayNetworkHandler networkHandler, PacketByteBuf buf, PacketSender sender) {
        int id = buf.readInt();
        client.execute(() -> {
            ClientWorld world = client.world;
            if (world != null) {
                Entity entity = world.getEntityById(id);
                if (entity != null) {
                    entity.getWorld().addParticle(ParticleTypes.END_ROD, entity.getParticleX(1), entity.getRandomBodyY(), entity.getParticleZ(1), 0, 0, 0);
                }
            }
        });
    }
}
