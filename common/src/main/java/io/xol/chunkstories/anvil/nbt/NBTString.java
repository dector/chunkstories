package io.xol.chunkstories.anvil.nbt;

import java.io.IOException;
import java.io.DataInputStream;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class NBTString extends NBTNamed{
	public String data;
	
	@Override
	public void feed(DataInputStream is) throws IOException {
		super.feed(is);
		
		int size = is.read() << 8;
		size+=is.read();
		
		byte[] n = new byte[size];
		try{
			is.readFully(n);
			data = new String(n, "UTF-8");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public String getText()
	{
		if(data == null)
			return "";
		return data;
	}
}