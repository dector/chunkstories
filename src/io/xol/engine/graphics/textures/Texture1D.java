package io.xol.engine.graphics.textures;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;

import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.geometry.IllegalRenderingThreadException;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class Texture1D
{
	String name;
	TextureType type;
	int glId = -1;
	int width;
	boolean wrapping = true;
	boolean linearFiltering = true;

	public Texture1D(TextureType type)
	{
		this.type = type;

		allTextureObjects.add(new WeakReference<Texture1D>(this));
	}

	public TextureType getType()
	{
		return type;
	}

	/**
	 * Returns the OpenGL GL_TEXTURE id of this object
	 * 
	 * @return
	 */
	public int getID()
	{
		return glId;
	}
	
	public void bind()
	{
		if (!GameWindowOpenGL.isMainGLWindow())
			throw new IllegalRenderingThreadException();
		
		if(glId == -1)
			glId = glGenTextures();
		
		glBindTexture(GL_TEXTURE_1D, glId);
	}

	public synchronized void destroy()
	{
		if (glId >= 0)
		{
			glDeleteTextures(glId);
			totalTextureObjects--;
		}
		glId = -1;
	}

	// Texture modifications

	public boolean uploadTextureData(int width, ByteBuffer data)
	{
		bind();
		this.width = width;
		glTexImage1D(GL_TEXTURE_1D, 0, GL_RGBA, width, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
		
		return true;
	}
	
	/**
	 * Determines if a texture will loop arround itself or clamp to it's edges
	 * 
	 * @param on
	 */
	public void setTextureWrapping(boolean on)
	{
		if (glId < 0) // Don't bother with invalid textures
			return;
		boolean applyParameters = false;

		if (wrapping != on) // We changed something so we redo them
			applyParameters = true;

		wrapping = on;

		if (!applyParameters)
			return;
		bind();
		if (!on)
		{
			glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		}
		else
		{
			glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_REPEAT);
			glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		}
	}
	
	public void setLinearFiltering(boolean on)
	{
		if (glId < 0) // Don't bother with invalid textures
			return;
		boolean applyParameters = false;

		if (linearFiltering != on) // We changed something so we redo them
			applyParameters = true;

		linearFiltering = on;

		if (!applyParameters)
			return;
		bind();
		setFiltering();
	}

	// Private function that sets both filering scheme and mipmap usage.
	private void setFiltering()
	{
		if (linearFiltering)
		{
			glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		}
		else
		{
			glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		}
	}

	public int getWidth()
	{
		return width;
	}

	public long getVramUsage()
	{
		int surface = getWidth();
		if (type == TextureType.RGBA_8BPP)
			return surface * 4;
		if (type == TextureType.RGB_HDR)
			return surface * 4;
		if (type == TextureType.DEPTH_SHADOWMAP)
			return surface * 3;
		if (type == TextureType.DEPTH_RENDERBUFFER)
			return surface * 4;
		return surface;
	}

	public static int destroyPendingTextureObjects()
	{
		return 0;
	}

	public static int getTotalNumberOfTextureObjects()
	{
		return totalTextureObjects;
	}

	public static long getTotalVramUsage()
	{
		long vram = 0;

		//Iterates over every instance reference, removes null ones and add up valid ones
		Iterator<WeakReference<Texture1D>> i = allTextureObjects.iterator();
		while (i.hasNext())
		{
			WeakReference<Texture1D> reference = i.next();

			Texture1D object = reference.get();
			if (object != null)
				vram += object.getVramUsage();
			else
				i.remove();
		}

		return vram;
	}

	private static int totalTextureObjects = 0;
	private static BlockingQueue<WeakReference<Texture1D>> allTextureObjects = new LinkedBlockingQueue<WeakReference<Texture1D>>();
}
