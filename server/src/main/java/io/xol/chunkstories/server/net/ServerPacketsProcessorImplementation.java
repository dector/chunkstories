package io.xol.chunkstories.server.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.xol.chunkstories.api.client.net.ClientPacketsProcessor;
import io.xol.chunkstories.api.exceptions.net.IllegalPacketException;
import io.xol.chunkstories.api.exceptions.net.UnknowPacketException;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketSynch;
import io.xol.chunkstories.api.net.PacketsProcessor;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.api.server.ServerPacketsProcessor;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.net.PacketTypeDeclared;
import io.xol.chunkstories.net.PacketsProcessorActual;
import io.xol.chunkstories.net.PacketsProcessorCommon;
import io.xol.chunkstories.net.PacketsProcessorCommon.PendingSynchPacket;
import io.xol.chunkstories.server.Server;
import io.xol.chunkstories.server.ServerPlayer;

public class ServerPacketsProcessorImplementation extends PacketsProcessorCommon implements ServerPacketsProcessor {

	final Server server;
	
	public ServerPacketsProcessorImplementation(Server server) {
		super(server);
		this.server = server;
	}

	@Override
	public ServerInterface getContext() {
		return server;
	}

	@Override
	public WorldMaster getWorld() {
		return server.getWorld();
	}
	
	public UserPacketsProcessor forConnection(UserConnection connection)
	{
		return new UserPacketsProcessor(connection);
	}
	
	/*public PlayerPacketsProcessor forPlayer(ServerPlayer player)
	{
		return new PlayerPacketsProcessor(player);
	}*/

	public class UserPacketsProcessor implements PacketsProcessorActual, ServerPacketsProcessor {
		
		final UserConnection connection;
		final Queue<PendingSynchPacket> pendingSynchPackets = new ConcurrentLinkedQueue<PendingSynchPacket>();
		
		public UserPacketsProcessor(UserConnection connection) {
			this.connection = connection;
		}
		
		public UserConnection getConnection()
		{
			return connection;
		}
		
		public PlayerPacketsProcessor toPlayer(ServerPlayer player) {
			return new PlayerPacketsProcessor(player);
		}
		
		@Override
		public WorldMaster getWorld() {
			return ServerPacketsProcessorImplementation.this.getWorld();
		}

		@Override
		public ServerInterface getContext() {
			return ServerPacketsProcessorImplementation.this.getContext();
		}

		public void sendPacketHeader(DataOutputStream out, Packet packet) throws UnknowPacketException, IOException {
			ServerPacketsProcessorImplementation.this.sendPacketHeader(out, packet);
		}

		public Packet getPacket(DataInputStream in) throws IOException, UnknowPacketException, IllegalPacketException {
			//return ServerPacketsProcessorImplementation.this.getPacket(in);
			while (true)
			{
				int firstByte = in.readByte();
				int packetType = 0;
				//If it is under 127 unsigned it's a 1-byte packet [0.firstByte(1.7)]
				if ((firstByte & 0x80) == 0)
					packetType = firstByte;
				else
				{
					//It's a 2-byte packet [0.firstByte(1.7)][secondByte(0.8)]
					int secondByte = in.readByte();
					secondByte = secondByte & 0xFF;
					packetType = secondByte | (firstByte & 0x7F) << 8;
				}
				Packet packet = ((PacketTypeDeclared)store.getPacketTypeById(packetType)).createNew(this instanceof ClientPacketsProcessor);

				//When we get a packetSynch
				if (packet instanceof PacketSynch)
				{
					//Read it's meta
					int packetSynchLength = in.readInt();

					//Read it entirely
					byte[] bufferedIncommingPacket = new byte[packetSynchLength];
					in.readFully(bufferedIncommingPacket);

					//Queue result
					pendingSynchPackets.add(new PendingSynchPacket(packet, bufferedIncommingPacket));
					
					//Skip this packet ( don't return it )
					continue;
				}

				if (packet == null)
					throw new UnknowPacketException(packetType);
				else
					return packet;
			}
			//System.out.println("could not find packut");
			//throw new EOFException();
			
		}

		@Override
		public PendingSynchPacket getPendingSynchPacket() {
			return pendingSynchPackets.poll();
		}
	}
	
	public class PlayerPacketsProcessor extends UserPacketsProcessor implements ServerPlayerPacketsProcessor {
		final ServerPlayer player;
		
		public PlayerPacketsProcessor(ServerPlayer player) {
			super(player.getPlayerConnection());
			this.player = player;
		}

		@Override
		public Player getPlayer() {
			return player;
		}
	}
}