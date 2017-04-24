package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSynchPrepared;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.client.Client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Describes a voxel change
 */
public class PacketVoxelUpdate extends PacketSynchPrepared
{
	public int x, y, z;
	public int data;
	
	@Override
	public void sendIntoBuffer(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		out.writeInt(x);
		out.writeInt(y);
		out.writeInt(z);
		out.writeInt(data);
		//No further information
		out.writeByte((byte)0x00);
	}

	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException
	{
		read(in);
		process(processor);
	}
	
	public void read(DataInputStream in) throws IOException
	{
		x = in.readInt();
		y = in.readInt();
		z = in.readInt();
		data = in.readInt();
		byte osef = in.readByte();
		assert osef == 0x00;
	}

	public void process(PacketsProcessor processor)
	{
		if(Client.world instanceof WorldClient)
		{
			Client.world.setVoxelData(x, y, z, data);
		}
	}

}
