package io.xol.chunkstories.world.summary;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.world.chunk.WorldUser;
import io.xol.chunkstories.api.world.heightmap.RegionSummaries;
import io.xol.chunkstories.api.world.heightmap.RegionSummary;
import io.xol.chunkstories.world.WorldImplementation;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class WorldRegionSummariesHolder implements RegionSummaries
{
	private final WorldImplementation world;
	private final int worldSize;
	
	private Map<Long, RegionSummaryImplementation> summaries = new ConcurrentHashMap<Long, RegionSummaryImplementation>();
	private Semaphore dontDeleteWhileCreating = new Semaphore(1);

	public WorldRegionSummariesHolder(WorldImplementation world)
	{
		this.world = world;
		this.worldSize = world.getSizeInChunks() * 32;
	}

	long index(int x, int z)
	{
		x /= 256;
		z /= 256;
		int s = world.getSizeInChunks() / 8;
		return x * s + z;
	}

	@Override
	public RegionSummaryImplementation aquireRegionSummary(WorldUser worldUser, int regionX, int regionZ)
	{
		RegionSummaryImplementation summary;
		
		regionX %= worldSize;
		regionZ %= worldSize;
		if (regionX < 0)
			regionX += worldSize;
		if (regionZ < 0)
			regionZ += worldSize;

		long i = index(regionX * 256, regionZ * 256);
		
		dontDeleteWhileCreating.acquireUninterruptibly();
		if (summaries.containsKey(i))
			summary = summaries.get(i);
		else
		{
			summary = new RegionSummaryImplementation(this, regionX, regionZ);
			summaries.put(i, summary);
		}
		dontDeleteWhileCreating.release();
		
		return summary.registerUser(worldUser) ? summary : null;
	}

	@Override
	public RegionSummaryImplementation aquireRegionSummaryChunkCoordinates(WorldUser worldUser, int chunkX, int chunkZ)
	{
		return aquireRegionSummary(worldUser, chunkX / 8, chunkZ / 8);
	}

	@Override
	public RegionSummaryImplementation aquireRegionSummaryWorldCoordinates(WorldUser worldUser, int worldX, int worldZ)
	{
		worldX = sanitizeHorizontalCoordinate(worldX);
		worldZ = sanitizeHorizontalCoordinate(worldZ);
		return aquireRegionSummary(worldUser, worldX / 256, worldZ / 256);
	}

	@Override
	public RegionSummaryImplementation aquireRegionSummaryLocation(WorldUser worldUser, Location location)
	{
		return aquireRegionSummary(worldUser, (int)location.getX(), (int)location.getZ());
	}
	
	@Override
	public RegionSummary getRegionSummary(int regionX, int regionZ)
	{
		return getRegionSummaryWorldCoordinates(regionX * 256, regionZ * 256);
	}

	@Override
	public RegionSummary getRegionSummaryChunkCoordinates(int chunkX, int chunkZ)
	{
		return getRegionSummaryWorldCoordinates(chunkX * 32, chunkZ * 32);
	}

	@Override
	public RegionSummary getRegionSummaryLocation(Location location)
	{
		return getRegionSummaryWorldCoordinates((int)location.getX(), (int)location.getZ());
	}

	public RegionSummaryImplementation getRegionSummaryWorldCoordinates(int worldX, int worldZ)
	{
		worldX = sanitizeHorizontalCoordinate(worldX);
		worldZ = sanitizeHorizontalCoordinate(worldZ);

		long i = index(worldX, worldZ);
		if (summaries.containsKey(i))
		{
			RegionSummaryImplementation summary = summaries.get(i);
			if(!summary.isLoaded())
				return summary;
			else
				return summary;
		}

		return null;
	}

	public int getHeightMipmapped(int x, int z, int level)
	{
		x %= worldSize;
		z %= worldSize;
		if (x < 0)
			x += worldSize;
		if (z < 0)
			z += worldSize;
		RegionSummaryImplementation cs = getRegionSummaryWorldCoordinates(x, z);
		if (cs == null)
			return 0;
		return cs.getHeightMipmapped(x % 256, z % 256, level);
	}

	public int getDataMipmapped(int x, int z, int level)
	{
		x %= worldSize;
		z %= worldSize;
		if (x < 0)
			x += worldSize;
		if (z < 0)
			z += worldSize;
		RegionSummaryImplementation cs = getRegionSummaryWorldCoordinates(x, z);
		if (cs == null)
			return 0;
		return cs.getDataMipmapped(x % 256, z % 256, level);
	}

	public int getHeightAtWorldCoordinates(int x, int z)
	{
		x %= worldSize;
		z %= worldSize;
		if (x < 0)
			x += worldSize;
		if (z < 0)
			z += worldSize;
		RegionSummaryImplementation cs = getRegionSummaryWorldCoordinates(x, z);
		if (cs == null)
			return 0;
		return cs.getHeight(x % 256, z % 256);
	}

	public int getDataAtWorldCoordinates(int x, int z)
	{
		x %= worldSize;
		z %= worldSize;
		if (x < 0)
			x += worldSize;
		if (z < 0)
			z += worldSize;
		RegionSummaryImplementation cs = getRegionSummaryWorldCoordinates(x, z);
		return cs.getVoxelData(x % 256, z % 256);
	}

	public void updateOnBlockPlaced(int x, int y, int z, int id)
	{
		x %= worldSize;
		z %= worldSize;
		if (x < 0)
			x += worldSize;
		if (z < 0)
			z += worldSize;
		RegionSummaryImplementation cs = getRegionSummaryWorldCoordinates(x, z);
		cs.updateOnBlockModification(x % 256, y, z % 256, id);
	}

	public int countSummaries()
	{
		return summaries.size();
	}

	public void saveAllLoadedSummaries()
	{
		for (RegionSummaryImplementation cs : summaries.values())
		{
			cs.saveSummary();
		}
	}

	public void setHeightAndId(int x, int z, int y, int id)
	{
		x %= worldSize;
		z %= worldSize;
		if (x < 0)
			x += worldSize;
		if (z < 0)
			z += worldSize;
		RegionSummaryImplementation cs = getRegionSummaryWorldCoordinates(x, z);
		cs.setHeightAndId(x % 256, y, z % 256, id);
	}

	public void unloadsUselessData()
	{
		dontDeleteWhileCreating.acquireUninterruptibly();
		
		Iterator<RegionSummaryImplementation> i = summaries.values().iterator();
		while(i.hasNext())
		{
			RegionSummaryImplementation summary = i.next();
			if(summary.unloadsIfUnused())
				System.out.println("unloaded unused summary "+summary);
		}
		
		dontDeleteWhileCreating.release();
	}
	
	/*public void removeFurther(int pCX, int pCZ, int distanceInChunks)
	{
		int rx = pCX / 8;
		int rz = pCZ / 8;

		int distInRegions = distanceInChunks / 8;
		int s = world.getSizeInChunks() / 8;
		synchronized(summaries)
		{
			Iterator<Entry<Long, RegionSummaryImplementation>> iterator = summaries.entrySet().iterator();
			while (iterator.hasNext())
			{
				Entry<Long, RegionSummaryImplementation> entry = iterator.next();
				long l = entry.getKey();
				int lx = (int) (l / s);
				int lz = (int) (l % s);

				int dx = LoopingMathHelper.moduloDistance(rx, lx, s);
				int dz = LoopingMathHelper.moduloDistance(rz, lz, s);
				// System.out.println("Chunk Summary "+lx+":"+lz+" is "+dx+":"+dz+" away from camera max:"+distInRegions+" total:"+summaries.size());
				if (dx > distInRegions || dz > distInRegions)
				{
					summaries.get(l).unloadSummary();
					iterator.remove();
				}
			}
		}
	}*/
	
	public void destroy()
	{
		for(RegionSummaryImplementation cs : summaries.values())
		{
			cs.unloadSummary();
		}
		summaries.clear();
	}

	WorldImplementation getWorld()
	{
		return world;
	}

	boolean removeSummary(RegionSummaryImplementation regionSummary)
	{
		return summaries.remove(this.index(regionSummary.getRegionX() * 256, regionSummary.getRegionZ() * 256)) != null;
	}
	
	private int sanitizeHorizontalCoordinate(int coordinate)
	{
		coordinate = coordinate % (world.getSizeInChunks() * 32);
		if (coordinate < 0)
			coordinate += world.getSizeInChunks() * 32;
		return coordinate;
	}
}