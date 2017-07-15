package io.xol.engine.sound;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL11.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.ALC11.*;
import static org.lwjgl.openal.EXTEfx.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALUtil;
import org.lwjgl.system.MemoryUtil;

import io.xol.chunkstories.api.client.ClientSoundManager;
import io.xol.chunkstories.api.exceptions.SoundEffectNotFoundException;
import org.joml.Vector3fc;

import io.xol.chunkstories.api.sound.SoundEffect;
import io.xol.chunkstories.api.sound.SoundSource;
import io.xol.chunkstories.tools.ChunkStoriesLoggerImplementation;
import io.xol.engine.sound.ogg.SoundDataOggSample;
import io.xol.engine.sound.sources.ALBufferedSoundSource;
import io.xol.engine.sound.sources.ALSoundSource;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ALSoundManager implements ClientSoundManager
{
	protected Queue<ALSoundSource> playingSoundSources = new ConcurrentLinkedQueue<ALSoundSource>();
	Random rng;

	Thread contextThread;
	// Are we allowed to use EFX effects
	public static boolean efxOn = false;
	
	private AtomicBoolean shutdownState = new AtomicBoolean(false);

	int[] auxEffectsSlotsId;
	SoundEffect[] auxEffectsSlots;
	
	private long device;
	private long context;

	public ALSoundManager()
	{
		rng = new Random();
		try
		{
			device = alcOpenDevice((ByteBuffer)null);
	        if (device == MemoryUtil.NULL) {
	            throw new IllegalStateException("Failed to open the default device.");
	        }

	        ALCCapabilities deviceCaps = ALC.createCapabilities(device);

	        System.out.println("OpenALC10: " + deviceCaps.OpenALC10);
	        System.out.println("OpenALC11: " + deviceCaps.OpenALC11);
	        System.out.println("caps.ALC_EXT_EFX = " + deviceCaps.ALC_EXT_EFX);

	        if (deviceCaps.OpenALC11) {
	            List<String> devices = ALUtil.getStringList(MemoryUtil.NULL, ALC_ALL_DEVICES_SPECIFIER);
	            if (devices == null) {
	                //checkALCError(MemoryUtil.NULL);
	            } else {
	                for (int i = 0; i < devices.size(); i++) {
	                    System.out.println(i + ": " + devices.get(i));
	                }
	            }
	        }

	        String defaultDeviceSpecifier = alcGetString(MemoryUtil.NULL, ALC_DEFAULT_DEVICE_SPECIFIER);
	        System.out.println("Default device: " + defaultDeviceSpecifier);

	        context = alcCreateContext(device, (IntBuffer)null);
	        alcMakeContextCurrent(context);
	        //alcSetThreadContext(context);
	       
	        AL.createCapabilities(deviceCaps);

			
			alDistanceModel(AL_LINEAR_DISTANCE_CLAMPED);
			String alVersion = alGetString(AL_VERSION);
			String alExtensions = alGetString(AL_EXTENSIONS);
			contextThread = Thread.currentThread();
			ChunkStoriesLoggerImplementation.getInstance().info("OpenAL context successfully created, version = " + alVersion);
			ChunkStoriesLoggerImplementation.getInstance().info("OpenAL Extensions avaible : " + alExtensions);
			efxOn = false;//EFXUtil.isEfxSupported();
			ChunkStoriesLoggerImplementation.getInstance().info("EFX extension support : " + (efxOn ? "yes" : "no"));
			if (efxOn)
			{
				//Reset error
				alGetError();
				List<Integer> auxSlotsIds = new ArrayList<Integer>();
				while (true)
				{
					int generated_id = alGenAuxiliaryEffectSlots();
					int error = alGetError();
					if (error != AL_NO_ERROR)
						break;
					auxSlotsIds.add(generated_id);
				}
				auxEffectsSlotsId = new int[auxSlotsIds.size()];
				int j = 0;
				for (int i : auxSlotsIds)
				{
					auxEffectsSlotsId[j] = i;
					j++;
				}
				auxEffectsSlots = new SoundEffect[auxSlotsIds.size()];
				ChunkStoriesLoggerImplementation.getInstance().info(auxEffectsSlots.length + " avaible auxiliary effects slots.");
			}

			Runtime.getRuntime().addShutdownHook(new Thread()
			{
				@Override
				public void run()
				{
		            shutdown();
				}
			});
		}
		catch (Exception e)
		{
			System.out.println("Failed to start sound system !");
			e.printStackTrace();
		}
	}

	public void destroy()
	{
		for (SoundSource ss : playingSoundSources)
			ss.stop();

		shutdown();
	}
	
	private void shutdown() {
		if(shutdownState.compareAndSet(false, true)) {
	        alcDestroyContext(context);
	        alcCloseDevice(device);
			System.out.println("OpenAL properly shut down.");
		}
	}

	public void update()
	{
		int result;
		if ((result = alGetError()) != AL_NO_ERROR)
			System.out.println("error at iter :" + SoundDataOggSample.getALErrorString(result));
		removeUnplayingSources();
		Iterator<ALSoundSource> i = playingSoundSources.iterator();
		while (i.hasNext())
		{
			ALSoundSource soundSource = i.next();
			soundSource.update(this);
		}
	}

	public float x, y, z;

	@Override
	public void setListenerPosition(float x, float y, float z, Vector3fc lookAt, Vector3fc up)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		FloatBuffer posScratch = MemoryUtil.memAllocFloat(3).put(new float[] { x, y, z });
		posScratch.flip();
		alListenerfv(AL_POSITION, posScratch);
		//AL10.alListener(AL10.AL_VELOCITY, xxx);
		

		FloatBuffer rotScratch = MemoryUtil.memAllocFloat(6).put(new float[] { lookAt.x(), lookAt.y(), lookAt.z(), up.x(), up.y(), up.z() });
		rotScratch.flip();
		alListenerfv(AL_ORIENTATION, rotScratch);
		//FloatBuffer listenerOri = BufferUtils.createFloatBuffer(6).put(new float[] { 0.0f, 0.0f, -1.0f,  0.0f, 1.0f, 0.0f });
	}

	//long countTo9223372036854775808 = 0L;

	public void addSoundSource(ALSoundSource soundSource)
	{
		soundSource.play();
		//countTo9223372036854775808++;
		//soundSource.soundSourceUUID = countTo9223372036854775808;
		playingSoundSources.add(soundSource);
	}

	@Override
	public SoundSource playSoundEffect(String soundEffect, float x, float y, float z, float pitch, float gain, float attStart, float attEnd)
	{
		try
		{
			ALSoundSource ss = new ALSoundSource(soundEffect, x, y, z, false, false, pitch, gain, attStart, attEnd);
			addSoundSource(ss);
			return ss;
		}
		catch (SoundEffectNotFoundException e)
		{
			System.out.println("Sound not found "+soundEffect);
		}
		return new DummySound();
	}

	@Override
	public void stopAnySound(String sfx)
	{
		Iterator<ALSoundSource> i = playingSoundSources.iterator();
		while (i.hasNext())
		{
			ALSoundSource soundSource = i.next();
			if (soundSource.soundData.getName().indexOf(sfx) != -1)
			{
				soundSource.stop();
				i.remove();
			}
		}
	}

	@Override
	public void stopAnySound()
	{
		for (SoundSource ss : playingSoundSources)
			ss.stop();
		playingSoundSources.clear();
	}

	int removeUnplayingSources()
	{
		int j = 0;
		Iterator<ALSoundSource> i = playingSoundSources.iterator();
		while (i.hasNext())
		{
			SoundSource soundSource = i.next();
			if (soundSource.isDonePlaying())
			{
				soundSource.stop();
				i.remove();
				j++;
			}
		}
		return j;
	}

	@Override
	public SoundSource playSoundEffect(String soundEffect)
	{
		return playSoundEffect(soundEffect, 0, 0, 0, 1, 1);
	}

	@Override
	public SoundSource playMusic(String musicName, float x, float y, float z, float pitch, float gain, boolean ambient, float attStart, float attEnd)
	{
		try
		{
			ALSoundSource ss = new ALBufferedSoundSource(musicName, x, y, z, false, ambient, pitch, gain, attStart, attEnd);
			addSoundSource(ss);
			return ss;
		}
		catch (SoundEffectNotFoundException e)
		{
			System.out.println("Music not found "+musicName);
		}
		return null;
	}

	@Override
	public int getMaxEffectsSlots()
	{
		return this.auxEffectsSlots.length;
	}

	@Override
	public boolean setEffectForSlot(int slot, SoundEffect effect)
	{
		if (auxEffectsSlots.length <= 0)
			return false;
		else if (slot >= 0 && slot < auxEffectsSlots.length)
		{
			auxEffectsSlots[slot] = effect;
			return true;
		}
		else
			return false;
	}

	public int getSlotForEffect(SoundEffect effect)
	{
		for (int i = 0; i < auxEffectsSlots.length; i++)
		{
			if (auxEffectsSlots[i].equals(effect))
				return i;
		}
		return -1;
	}

	@Override
	public Iterator<SoundSource> getAllPlayingSounds()
	{
		return new Iterator<SoundSource>()
		{
			Iterator<ALSoundSource> i = playingSoundSources.iterator();

			@Override
			public boolean hasNext()
			{
				return i.hasNext();
			}

			@Override
			public SoundSource next()
			{
				return i.next();
			}
		};
	}

	@Override
	public SoundSource replicateServerSoundSource(String soundName, float x, float y, float z, boolean loop, boolean isAmbient, float pitch, float gain, float attenuationStart, float attenuationEnd, boolean buffered, long UUID) {
		try {
			ALSoundSource soundSource = null;
				
			if (buffered)
				soundSource = new ALBufferedSoundSource(soundName, x, y, z, loop, isAmbient, pitch, gain, attenuationStart, attenuationEnd);
			else
				soundSource = new ALSoundSource(soundName, x, y, z, loop, isAmbient, pitch, gain, attenuationStart, attenuationEnd);
			
			//Match the UUIDs
			soundSource.setUUID(UUID);
			addSoundSource(soundSource);
			
			return soundSource;
		}
		catch (SoundEffectNotFoundException e)
		{
			System.out.println("Sound not found "+soundName);
			return null;
		}
	}
}