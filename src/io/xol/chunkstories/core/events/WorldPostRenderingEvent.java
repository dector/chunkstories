package io.xol.chunkstories.core.events;

import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.world.World;

import io.xol.chunkstories.renderer.WorldRenderer;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class WorldPostRenderingEvent extends Event
{
	// Every event class has to have this
	
	static EventListeners listeners = new EventListeners();
	
	@Override
	public EventListeners getListeners()
	{
		return listeners;
	}
	
	public static EventListeners getListenersStatic()
	{
		return listeners;
	}
	
	// Specific event code

	private World world;
	private WorldRenderer worldRenderer;
	private RenderingInterface renderingInterface;
	
	public WorldPostRenderingEvent(World world, WorldRenderer worldRenderer, RenderingInterface renderingInterface)
	{
		super();
		this.world = world;
		this.worldRenderer = worldRenderer;
		this.renderingInterface = renderingInterface;
	}
	
	public World getWorld()
	{
		return world;
	}

	public WorldRenderer getWorldRenderer()
	{
		return worldRenderer;
	}

	public RenderingInterface getRenderingInterface()
	{
		return renderingInterface;
	}
}