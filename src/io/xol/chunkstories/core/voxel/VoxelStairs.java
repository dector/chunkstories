package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.BlockRenderInfo;
import io.xol.chunkstories.voxel.VoxelDefault;
import io.xol.chunkstories.voxel.models.VoxelModel;
import io.xol.chunkstories.voxel.models.VoxelModels;

public class VoxelStairs extends VoxelDefault
{

	VoxelModel[] models = new VoxelModel[8];

	public VoxelStairs(int id, String name)
	{
		super(id, name);
		for (int i = 0; i < 8; i++)
			models[i] = VoxelModels.getVoxelModel("stairs.m" + i);
	}

	@Override
	public VoxelModel getVoxelModel(BlockRenderInfo info)
	{
		int meta = info.getMetaData();
		return models[meta % 8];
	}

	@Override
	public CollisionBox[] getCollisionBoxes(BlockRenderInfo info)
	{
		int meta = VoxelFormat.meta(info.data);
		// System.out.println("kek"+meta);
		CollisionBox[] boxes = new CollisionBox[2];
		boxes[0] = new CollisionBox(1, 0.5, 1);//.translate(0.5, -1, 0.5);
		switch (meta % 4)
		{
		case 0:
			boxes[1] = new CollisionBox(0.5, 0.5, 1.0).translate(0.75, -0.5, 0.5);
			break;
		case 1:
			boxes[1] = new CollisionBox(0.5, 0.5, 1.0).translate(0.25, -0.5, 0.5);
			break;
		case 2:
			boxes[1] = new CollisionBox(1.0, 0.5, 0.5).translate(0.5, -0.5, 0.75);
			break;
		case 3:
			boxes[1] = new CollisionBox(1.0, 0.5, 0.5).translate(0.5, -0.5, 0.25);
			break;
		default:
			boxes[1] = new CollisionBox(0.5, 0.5, 1.0).translate(0.5, -0.5, 0.25);
			break;
		}
		if(meta / 4 == 0)
		{
			boxes[0].translate(0.5, -0, 0.5);
			boxes[1].translate(0.0, +1.0, 0.0);
		}
		else
		{
			boxes[0].translate(0.5, +0.5, 0.5);
			boxes[1].translate(0.0, +0.5, 0.0);
		}

		return boxes;

		/*
		 * CollisionBox box = new CollisionBox(1,0.5,1); if(bottomOrTop(data))
		 * box.translate(0.5, -1, 0.5); else box.translate(0.5, -0.5, 0.5);
		 * return new CollisionBox[] { box };
		 */
		// return super.getCollisionBoxes(data);
	}
	
	@Override
	public int getLightLevelModifier(int dataFrom, int dataTo, int side)
	{
		return super.getLightLevelModifier(dataFrom, dataTo, side);
	}
}