package io.xol.chunkstories.core.particles;

import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.lalgb.Vector3f;
import io.xol.engine.model.RenderingContext;

import io.xol.chunkstories.api.particles.ParticleData;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.renderer.lights.DefferedLight;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ParticleMuzzleFlash extends ParticleType
{
	public ParticleMuzzleFlash(int id, String name)
	{
		super(id, name);
	}

	public class MuzzleData extends ParticleData {

		public int timer = 1;
		
		public MuzzleData(float x, float y, float z)
		{
			super(x, y, z);
		}
	}

	@Override
	public ParticleData createNew(World world, float x, float y, float z)
	{
		return new MuzzleData(x, y, z);
	}

	@Override
	public float getBillboardSize()
	{
		return 0.0f;
	}

	@Override
	public Texture2D getTexture()
	{
		return TexturesHandler.nullTexture();
	}

	@Override
	public void forEach_Rendering(RenderingContext renderingContext, ParticleData data)
	{
		renderingContext.addLight(new DefferedLight(new Vector3f(1.0f, 181f/255f, 79/255f),
				new Vector3f((float) data.x, (float) data.y, (float) data.z),
				15f + (float) Math.random() * 5f));
	}

	@Override
	public void forEach_Physics(World world, ParticleData data)
	{
		((MuzzleData)data).timer--;
		if(((MuzzleData)data).timer < 0)
			data.destroy();
	}
}