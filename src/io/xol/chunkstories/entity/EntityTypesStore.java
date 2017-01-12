package io.xol.chunkstories.entity;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.Content.EntityTypes;
import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityType;
import io.xol.chunkstories.api.mods.Asset;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.world.WorldImplementation;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class EntityTypesStore implements EntityTypes
{
	private final GameContext context;
	private final Content content;
	private final EntityComponentsStore entityComponents;
	
	private Map<Short, EntityType> entityTypesById = new HashMap<Short, EntityType>();
	private Map<String, EntityType> entityTypesByName = new HashMap<String, EntityType>();
	private Map<String, EntityType> entityTypesByClassname = new HashMap<String, EntityType>();

	public EntityTypesStore(Content content)
	{
		this.content = content;
		this.context = content.getContext();
		
		this.entityComponents = new EntityComponentsStore(context, this);
		
		this.reload();
	}
	
	public void reload()
	{
		//entitiesIds.clear();
		//entitiesTypes.clear();
		entityTypesById.clear();
		entityTypesByName.clear();
		entityTypesByClassname.clear();
		
		Iterator<Asset> i = content.modsManager().getAllAssetsByExtension("entities");
		while(i.hasNext())
		{
			Asset f = i.next();
			ChunkStoriesLogger.getInstance().log("Reading entities definitions in : " + f);
			readEntitiesDefinitions(f);
		}
		
		this.entityComponents.reload();
	}

	private void readEntitiesDefinitions(Asset f)
	{
		if (f == null)
			return;
		try
		{
			BufferedReader reader = new BufferedReader(f.reader());
			String line = "";
			while ((line = reader.readLine()) != null)
			{
				line = line.replace("\t", "");
				if (line.startsWith("#"))
				{
					// It's a comment, ignore.
				}
				else
				{
					if(line.contains(" "))
					{
						String[] split = line.split(" ");
						short id = Short.parseShort(split[0]);
						String className = split[1];
						
						String entityTypeName = className.substring(className.lastIndexOf("."));
						if(split.length >= 3)
							entityTypeName = split[2];
							
						try
						{
							Class<?> entityClass = content.modsManager().getClassByName(className);
							if(entityClass == null)
							{
								System.out.println("Entity class "+className+" does not exist in codebase.");
							}
							else if (!(Entity.class.isAssignableFrom(entityClass)))
							{
								ChunkStoriesLogger.getInstance().warning("Entity class " + entityClass + " is not implementing the Entity interface.");
							}
							else
							{
								@SuppressWarnings("rawtypes")
								Class[] types = { WorldImplementation.class, Double.TYPE, Double.TYPE , Double.TYPE  };
								@SuppressWarnings("unchecked")
								Constructor<? extends Entity> constructor = (Constructor<? extends Entity>) entityClass.getConstructor(types);
								
								if(constructor == null)
								{
									System.out.println("Entity "+className+" does not provide a valid constructor.");
								}
								else
								{
									EntityTypeLoaded addMe = new EntityTypeLoaded(entityTypeName, className, constructor, id);

									entityTypesById.put(id, addMe);
									entityTypesByName.put(entityTypeName, addMe);
									entityTypesByClassname.put(className, addMe);
									//entitiesTypes.put(id, constructor);
									//entitiesIds.put(className, id);
								}
							}

						}
						catch (NoSuchMethodException | SecurityException | IllegalArgumentException e)
						{
							e.printStackTrace();
						}
					}
				}
			}
			reader.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public EntityType getEntityTypeById(short entityId)
	{
		return entityTypesById.get(entityId);
	}
	
	public short getEntityIdByClassname(String className)
	{
		EntityType type = entityTypesByClassname.get(className);
		if(type == null)
			return -1;
		return type.getId();
	}

	class EntityTypeLoaded implements EntityType {

		public EntityTypeLoaded(String name, String className, Constructor<? extends Entity> constructor, short id)
		{
			super();
			this.name = name;
			this.className = className;
			this.constructor = constructor;
			this.id = id;
		}

		@Override
		public String toString()
		{
			return "EntityDefined [name=" + name + ", className=" + className + ", constructor=" + constructor + ", id=" + id + "]";
		}

		final String name;
		final String className;
		final Constructor<? extends Entity> constructor;
		final short id;
		
		@Override
		public String getName()
		{
			return name;
		}

		@Override
		public short getId()
		{
			return id;
		}

		@Override
		public Entity create(World world)
		{
			Object[] parameters = { world, 0d, 0d, 0d };
			try
			{
				return constructor.newInstance(parameters);
			}
			catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
			{
				//This is bad
				ChunkStoriesLogger.getInstance().log("Couldn't instanciate entity "+this+" in world "+world);
				e.printStackTrace(ChunkStoriesLogger.getInstance().getPrintWriter());
				return null;
			}
		}
		
	}

	@Override
	public EntityType getEntityTypeById(int entityId)
	{
		return entityTypesById.get(entityId);
	}

	@Override
	public EntityType getEntityTypeByName(String entityName)
	{
		return entityTypesByName.get(entityName);
	}

	@Override
	public EntityType getEntityTypeByClassname(String className)
	{
		return entityTypesByClassname.get(className);
	}

	@Override
	public Iterator<EntityType> all()
	{
		return this.entityTypesById.values().iterator();
	}

	@Override
	public Content parent()
	{
		return content;
	}

	@Override
	public EntityComponentsStore components()
	{
		return entityComponents;
	}
}
