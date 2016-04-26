package io.xol.chunkstories.content;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.xol.chunkstories.entity.EntitiesList;
import io.xol.chunkstories.gui.GameplayScene;
import io.xol.chunkstories.input.KeyBinds;
import io.xol.chunkstories.item.ItemsList;
import io.xol.chunkstories.net.packets.PacketsProcessor;
import io.xol.chunkstories.voxel.VoxelTextures;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.voxel.models.VoxelModels;
import io.xol.chunkstories.world.generator.WorldGenerators;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.model.ModelLibrary;
import io.xol.engine.model.animation.BVHLibrary;
import io.xol.engine.shaders.ShadersLibrary;
import io.xol.engine.sound.library.SoundsLibrary;
import io.xol.engine.textures.TexturesHandler;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class GameData
{
	public static void reload()
	{
		buildModsFileSystem();
		VoxelTextures.buildTextureAtlas();
		VoxelModels.resetAndLoadModels();
		ItemsList.reload();
		VoxelTypes.loadVoxelTypes();
		EntitiesList.reload();
		PacketsProcessor.loadPacketsTypes();
		WorldGenerators.loadWorldGenerators();
	}

	public static void reloadClientContent()
	{
		KeyBinds.loadKeyBinds();
		TexturesHandler.reloadAll();
		SoundsLibrary.clean();
		ModelLibrary.reloadAllModels();
		BVHLibrary.reloadAllAnimations();
		ShadersLibrary.reloadAllShaders();
		if(XolioWindow.getInstance().getCurrentScene() instanceof GameplayScene)
		{
			((GameplayScene)XolioWindow.getInstance().getCurrentScene()).worldRenderer.sky.reloadSky();
		}
	}
	
	static ConcurrentHashMap<String, Deque<File>> fileSystem = new ConcurrentHashMap<String, Deque<File>>();

	static Set<String> mods = new HashSet<String>();
	static boolean allModsEnabled = false;
	
	public static void setEnabledMods(String... modsEnabled)
	{
		//Build a set of required mods
		mods.clear();
		allModsEnabled = false;
		for (String s : modsEnabled)
		{
			//System.out.println("MODS"+s);
			if(s.equals("*"))
				allModsEnabled = true;
			else
				mods.add(s);
		}
		//System.out.println("MODS"+allModsEnabled);
	}
	
	/**
	 * Creates and fill the fileSystem hashmap of deque of files
	 * @param modsEnabled
	 */
	private static void buildModsFileSystem()
	{
		fileSystem.clear();
		//Get the mods/ dir
		File modsDir = new File(GameDirectory.getGameFolderPath() + "/mods/");
		if (!modsDir.exists())
			modsDir.mkdirs();
		//Load needed mods by order of priority
		for (File f : modsDir.listFiles())
		{
			System.out.println("FFFF"+allModsEnabled);
			if (allModsEnabled || mods.contains(f.getName()))
			{
				System.out.println("FFFF"+f.getAbsolutePath());
				if (f.isDirectory())
					recursiveScan(f, f);
			}
		}
		//Load vanilla ressources (lowest priority)
		for(File f : new File(GameDirectory.getGameFolderPath() + "/res/").listFiles())
				recursiveScan(f, new File(GameDirectory.getGameFolderPath() + "/"));
	}

	private static void recursiveScan(File directory, File modsDir)
	{
		//Special case for initial res/ folder
		if(!directory.isDirectory())
		{
			File f = directory;
			String filteredName = f.getAbsolutePath();
			//Remove game dir path
			filteredName = "."+filteredName.replace(modsDir.getAbsolutePath(), "");
			filteredName = filteredName.replace('\\', '/');
			//Remove mod path
			if(!fileSystem.containsKey(filteredName))
			{
				fileSystem.remove(filteredName);
				fileSystem.put(filteredName, new ArrayDeque<File>());
				//System.out.println("Found override for ressource : " + filteredName + " in modDir : " + modsDir);
			}
			fileSystem.get(filteredName).addLast(f);
			//fileSystem.put(filteredName, f);
			return;
		}
		//We just list dem files
		for (File f : directory.listFiles())
		{
			if (f.isDirectory())
				recursiveScan(f, modsDir);
			else
			{
				String filteredName = f.getAbsolutePath();
				//Remove game dir path
				filteredName = "."+filteredName.replace(modsDir.getAbsolutePath(), "");
				filteredName = filteredName.replace('\\', '/');
				//Remove mod path
				if(!fileSystem.containsKey(filteredName))
				{
					fileSystem.remove(filteredName);
					fileSystem.put(filteredName, new ArrayDeque<File>());
					//System.out.println("Found override for ressource : " + filteredName + " in modDir : " + modsDir);
				}
				fileSystem.get(filteredName).addLast(f);
				//fileSystem.put(filteredName, f);
			}
		}
	}

	public static Iterator<Entry<String, Deque<File>>> getAllUniqueEntries()
	{
		return fileSystem.entrySet().iterator();
	}
	
	public static Iterator<Deque<File>> getAllUniqueFilesLocations()
	{
		return fileSystem.values().iterator();
	}

	/**
	 * Gets the location of a certain texture file ( checks mods/ first and then various directories )
	 * @param textureName
	 * @return hopefully a valid file ( null if it doesn't seem to exist )
	 */
	public static File getTextureFileLocation(String textureName)
	{
		if(fileSystem.containsKey(textureName))
			return fileSystem.get(textureName).getFirst();
		
		if (textureName.endsWith(".png"))
			textureName = textureName.substring(0, textureName.length() - 4);
		File checkTexturesFolder = new File("./res/textures/" + textureName + ".png");
		if (checkTexturesFolder.exists())
			return checkTexturesFolder;
		File checkResFolder = new File("./res/" + textureName + ".png");
		if (checkResFolder.exists())
			return checkResFolder;
		File checkRootFolder = new File("./" + textureName + ".png");
		if (checkRootFolder.exists())
			return checkRootFolder;
		File checkGameFolder = new File(GameDirectory.getGameFolderPath() + "/" + textureName + ".png");
		if (checkGameFolder.exists())
			return checkGameFolder;
		return null;
	}

	/**
	 * Gets the most prioritary instance of the file this file system has (or null if it doesn't exist)
	 * @param fileName
	 * @return
	 */
	public static File getFileLocation(String fileName)
	{
		if(fileSystem.containsKey(fileName))
			return fileSystem.get(fileName).getFirst();
		File checkRootFolder = new File("./" + fileName);
		if (checkRootFolder.exists())
			return checkRootFolder;
		File checkGameFolder = new File(GameDirectory.getGameFolderPath() + "/" + fileName);
		if (checkGameFolder.exists())
			return checkGameFolder;
		return null;
	}

	/**
	 * Returns all the instances of a file in the various mods, sorted by mod priority
	 * @return
	 */
	public static Deque<File> getAllFileInstances(String fileName)
	{
		if(fileSystem.containsKey(fileName))
			return fileSystem.get(fileName);
		return null;
	}
}