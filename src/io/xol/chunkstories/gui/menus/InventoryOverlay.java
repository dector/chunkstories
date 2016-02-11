package io.xol.chunkstories.gui.menus;

import org.lwjgl.input.Mouse;

import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.entity.inventory.Inventory;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.net.packets.Packet06InventoryMoveItemPile;
import io.xol.chunkstories.world.WorldLocalClient;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.gui.GuiDrawer;
import io.xol.engine.textures.TexturesHandler;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class InventoryOverlay extends Overlay
{
	Inventory[] inventories;
	InventoryDrawer[] drawers;

	public static ItemPile selectedItem;

	public InventoryOverlay(OverlayableScene scene, Overlay parent, Inventory[] inventories)
	{
		super(scene, parent);
		this.inventories = inventories;
		this.drawers = new InventoryDrawer[inventories.length];
		for (int i = 0; i < drawers.length; i++)
			drawers[i] = new InventoryDrawer(inventories[i]);
	}

	public void drawToScreen(int x, int y, int w, int h)
	{
		int totalWidth = 0;
		for (Inventory inv : inventories)
			totalWidth += 2 + inv.width;
		totalWidth -= 2;
		int widthAccumulation = 0;
		for (int i = 0; i < drawers.length; i++)
		{
			int thisWidth = inventories[i].width;
			drawers[i].drawInventoryCentered(XolioWindow.frameW / 2 - totalWidth * 24 + thisWidth * 24 + widthAccumulation * 48, XolioWindow.frameH / 2, 2, false, 4);
			widthAccumulation += 1 + thisWidth;
		}

		if (selectedItem != null)
		{
			int slotSize = 24 * 2;
			int textureId = TexturesHandler.getTextureID(selectedItem.getTextureName());
			int width = slotSize * selectedItem.item.getSlotsWidth();
			int height = slotSize * selectedItem.item.getSlotsHeight();
			GuiDrawer.drawBoxWindowsSpaceWithSize(Mouse.getX() - width / 2, Mouse.getY() - height / 2, width, height, 0, 1, 1, 0, textureId, true, true, null);
		}
	}

	public boolean handleKeypress(int k)
	{
		if (k == FastConfig.EXIT_KEY)
			this.mainScene.changeOverlay(parent);
		return true;
	}

	public boolean onClick(int posx, int posy, int button)
	{
		for (int i = 0; i < drawers.length; i++)
		{
			if (drawers[i].isOverCloseButton())
				this.mainScene.changeOverlay(parent);
			else
			{

				int[] c = drawers[i].getSelectedSlot();
				if (c == null)
					continue;
				else
				{
					int x = c[0];
					int y = c[1];
					if (selectedItem == null)
					{
						selectedItem = inventories[i].getItem(x, y);
						//selectedItemInv = inventory;
					}
					else
					{
						if (Client.world instanceof WorldLocalClient)
							selectedItem = selectedItem.moveTo(inventories[i], x, y);
						else
						{
							Packet06InventoryMoveItemPile packetMove = new Packet06InventoryMoveItemPile(true);
							packetMove.from = selectedItem.inventory;
							packetMove.oldX = selectedItem.x;
							packetMove.oldY = selectedItem.y;
							packetMove.to = inventories[i];
							packetMove.newX = x;
							packetMove.newY = y;
							packetMove.itemPile = selectedItem;
							Client.connection.sendPacket(packetMove);
							selectedItem = selectedItem.moveTo(inventories[i], x, y);
						}

					}
					return true;
				}
			}
		}
		if(selectedItem != null)
		{
			Packet06InventoryMoveItemPile packetMove = new Packet06InventoryMoveItemPile(true);
			packetMove.from = selectedItem.inventory;
			packetMove.oldX = selectedItem.x;
			packetMove.oldY = selectedItem.y;
			packetMove.to = null;
			packetMove.newX = 0;
			packetMove.newY = 0;
			packetMove.itemPile = selectedItem;
			Client.connection.sendPacket(packetMove);
			selectedItem = null;//selectedItem.moveTo(inventories[i], x, y);
		}
		return true;

	}
}
