package io.xol.chunkstories.tools;

import java.io.File;

import io.xol.chunkstories.world.World;
import io.xol.chunkstories.world.WorldInfo;
import io.xol.chunkstories.world.generator.BlankWorldAccessor;
import io.xol.chunkstories.world.io.IOTasksImmediate;

public class WorldTool extends World
{
	public WorldTool(String worldDir)
	{
		super(new WorldInfo(new File(worldDir+"/info.txt"), new File(worldDir).getName()));
		client = false;
		
		ioHandler = new IOTasksImmediate(this);
		//ioHandler.start();
	}

	public WorldTool(File csWorldDir)
	{
		this(csWorldDir.getAbsolutePath());
	}

	public WorldTool(String csWorldName, String string, BlankWorldAccessor blankWorldAccessor, WorldSize size)
	{
		super(csWorldName, string, blankWorldAccessor, size);
	}
}