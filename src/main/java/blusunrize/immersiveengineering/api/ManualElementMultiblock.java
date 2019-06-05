/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.api;

import blusunrize.immersiveengineering.api.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.api.crafting.IngredientStack;
import blusunrize.lib.manual.ManualInstance;
import blusunrize.lib.manual.ManualUtils;
import blusunrize.lib.manual.SpecialManualElements;
import blusunrize.lib.manual.gui.GuiButtonManualNavigation;
import blusunrize.lib.manual.gui.GuiManual;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ManualElementMultiblock extends SpecialManualElements
{
	private IMultiblock multiblock;

	private boolean canTick = true;
	private boolean showCompleted = false;
	private int tick = 0;

	private float scale = 50f;
	private float transX = 0;
	private float transY = 0;
	private float rotX = 0;
	private float rotY = 0;
	private List<String> componentTooltip;
	private MultiblockRenderInfo renderInfo;
	private MultiblockBlockAccess blockAccess;
	private int yOffTotal;

	public ManualElementMultiblock(ManualInstance manual, IMultiblock multiblock)
	{
		super(manual);
		this.multiblock = multiblock;
		renderInfo = new MultiblockRenderInfo(multiblock);
		float diagLength = (float)Math.sqrt(renderInfo.structureHeight*renderInfo.structureHeight+
				renderInfo.structureWidth*renderInfo.structureWidth+
				renderInfo.structureLength*renderInfo.structureLength);
		blockAccess = new MultiblockBlockAccess(renderInfo);
		transX = 60+renderInfo.structureWidth/2F;
		transY = 35+diagLength/2;
		rotX = 25;
		rotY = -45;
		scale = multiblock.getManualScale();
		yOffTotal = (int)(transY+scale*diagLength/2);
	}

	@Override
	public void onOpened(GuiManual gui, int x, int y, List<GuiButton> pageButtons)
	{
		int yOff = 0;
		if(multiblock.getStructureManual()!=null)
		{
			boolean canRenderFormed = multiblock.canRenderFormedStructure();

			yOff = (int)(transY+scale*Math.sqrt(renderInfo.structureHeight*renderInfo.structureHeight+renderInfo.structureWidth*renderInfo.structureWidth+renderInfo.structureLength*renderInfo.structureLength)/2);
			pageButtons.add(new GuiButtonManualNavigation(gui, 100, x+4, (int)transY-(canRenderFormed?11: 5), 10, 10, 4));
			if(canRenderFormed)
				pageButtons.add(new GuiButtonManualNavigation(gui, 103, x+4, (int)transY+1, 10, 10, 6));
			if(this.renderInfo.structureHeight > 1)
			{
				pageButtons.add(new GuiButtonManualNavigation(gui, 101, x+4, (int)transY-(canRenderFormed?14: 8)-16, 10, 16, 3));
				pageButtons.add(new GuiButtonManualNavigation(gui, 102, x+4, (int)transY+(canRenderFormed?14: 8), 10, 16, 2));
			}
		}

		IngredientStack[] totalMaterials = this.multiblock.getTotalMaterials();
		if(totalMaterials!=null)
		{
			componentTooltip = new ArrayList<>();
			componentTooltip.add(I18n.format("desc.immersiveengineering.info.reqMaterial"));
			int maxOff = 1;
			boolean hasAnyItems = false;
			boolean[] hasItems = new boolean[totalMaterials.length];
			for(int ss = 0; ss < totalMaterials.length; ss++)
				if(totalMaterials[ss]!=null)
				{
					IngredientStack req = totalMaterials[ss];
					int reqSize = req.inputSize;
					for(int slot = 0; slot < ManualUtils.mc().player.inventory.getSizeInventory(); slot++)
					{
						ItemStack inSlot = ManualUtils.mc().player.inventory.getStackInSlot(slot);
						if(!inSlot.isEmpty()&&req.matchesItemStackIgnoringSize(inSlot))
							if((reqSize -= inSlot.getCount()) <= 0)
								break;
					}
					if(reqSize <= 0)
					{
						hasItems[ss] = true;
						if(!hasAnyItems)
							hasAnyItems = true;
					}
					maxOff = Math.max(maxOff, (""+req.inputSize).length());
				}
			for(int ss = 0; ss < totalMaterials.length; ss++)
				if(totalMaterials[ss]!=null)
				{
					IngredientStack req = totalMaterials[ss];
					int indent = maxOff-(""+req.inputSize).length();
					String sIndent = "";
					if(indent > 0)
						for(int ii = 0; ii < indent; ii++)
							sIndent += "0";
					String s = hasItems[ss]?(TextFormatting.GREEN+TextFormatting.BOLD.toString()+"\u2713"+TextFormatting.RESET+" "): hasAnyItems?("   "): "";
					s += TextFormatting.GRAY+sIndent+req.inputSize+"x "+TextFormatting.RESET;
					ItemStack example = req.getExampleStack();
					if(!example.isEmpty())
						s += example.getRarity().color+example.getDisplayName();
					else
						s += "???";
					componentTooltip.add(s);
				}
		}
		super.onOpened(gui, x, yOff, pageButtons);
	}

	@Override
	public void render(GuiManual gui, int x, int y, int mouseX, int mouseY)
	{
		boolean openBuffer = false;
		int stackDepth = GL11.glgetInt(GL11.GL_MODELVIEW_STACK_DEPTH);
		try
		{
			if(multiblock.getStructureManual()!=null)
			{
				if(!canTick)
				{
//					renderInfo.reset();
//				renderInfo.setShowLayer(9);
					//LAYER CACHING!!
				}
				else if(++tick%20==0)
					renderInfo.step();

				int structureLength = renderInfo.structureLength;
				int structureWidth = renderInfo.structureWidth;
				int structureHeight = renderInfo.structureHeight;

				GlStateManager.enableRescaleNormal();
				GlStateManager.pushMatrix();
				RenderHelper.disableStandardItemLighting();
				//			GL11.glEnable(GL11.GL_DEPTH_TEST);
				//			GL11.glDepthFunc(GL11.GL_ALWAYS);
				//			GL11.glDisable(GL11.GL_CULL_FACE);
				int i = 0;
				ItemStack highlighted = ItemStack.EMPTY;

				final BlockRendererDispatcher blockRender = Minecraft.getInstance().getBlockRendererDispatcher();

				float f = (float)Math.sqrt(structureHeight*structureHeight+structureWidth*structureWidth+structureLength*structureLength);

				GlStateManager.translate(transX, transY, Math.max(structureHeight, Math.max(structureWidth, structureLength)));
				GlStateManager.scale(scale, -scale, 1);
				GlStateManager.rotate(rotX, 1, 0, 0);
				GlStateManager.rotate(90+rotY, 0, 1, 0);

				GlStateManager.translate((float)structureLength/-2f, (float)structureHeight/-2f, (float)structureWidth/-2f);

				GlStateManager.disableLighting();

				if(Minecraft.isAmbientOcclusionEnabled())
					GlStateManager.shadeModel(GL11.GL_SMOOTH);
				else
					GlStateManager.shadeModel(GL11.GL_FLAT);

				gui.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
				int idx = 0;
				if(showCompleted&&multiblock.canRenderFormedStructure())
					multiblock.renderFormedStructure();
				else
					for(int h = 0; h < structureHeight; h++)
						for(int l = 0; l < structureLength; l++)
							for(int w = 0; w < structureWidth; w++)
							{
								BlockPos pos = new BlockPos(l, h, w);
								if(!blockAccess.isAirBlock(pos))
								{
									GlStateManager.translate(l, h, w);
									boolean b = multiblock.overwriteBlockRender(renderInfo.data[h][l][w], idx++);
									GlStateManager.translate(-l, -h, -w);
									if(!b)
									{
										IBlockState state = blockAccess.getBlockState(pos);
										Tessellator tessellator = Tessellator.getInstance();
										BufferBuilder buffer = tessellator.getBuffer();
										buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
										openBuffer = true;
										blockRender.renderBlock(state, pos, blockAccess, buffer);
										tessellator.draw();
										openBuffer = false;
									}
								}
							}

				GlStateManager.popMatrix();

				RenderHelper.disableStandardItemLighting();
				GlStateManager.disableRescaleNormal();

				GlStateManager.enableBlend();
				RenderHelper.disableStandardItemLighting();

				if(componentTooltip!=null)
				{
					manual.fontRenderer.setUnicodeFlag(false);
					manual.fontRenderer.drawString("?", 116, yOffTotal/2-4, manual.getTextColour(), false);
					if(mouseX >= 116&&mouseX < 122&&mouseY >= yOffTotal/2-4&&mouseY < yOffTotal/2+4)
						gui.drawHoveringText(componentTooltip, mouseX, mouseY, manual.fontRenderer);
				}
			}

		} catch(Exception e)
		{
			e.printStackTrace();
		}
		if(openBuffer)
			try
			{
				Tessellator.getInstance().draw();
			} catch(Exception e)
			{
			}
		int newStackDepth = GL11.glgetInt(GL11.GL_MODELVIEW_STACK_DEPTH);
		while(newStackDepth > stackDepth)
		{
			GlStateManager.popMatrix();
			newStackDepth--;
		}
	}

	@Override
	public void mouseDragged(int x, int y, int clickX, int clickY, int mouseX, int mouseY, int lastX, int lastY, GuiButton button)
	{
		if((clickX >= 40&&clickX < 144&&mouseX >= 20&&mouseX < 164)&&(clickY >= 30&&clickY < 130&&mouseY >= 30&&mouseY < 180))
		{
			int dx = mouseX-lastX;
			int dy = mouseY-lastY;
			rotY = rotY+(dx/104f)*80;
			rotX = rotX+(dy/100f)*80;
		}
	}

	@Override
	public void buttonPressed(GuiManual gui, GuiButton button)
	{
		if(button.id==100)
		{
			canTick = !canTick;
			((GuiButtonManualNavigation)button).type = ((GuiButtonManualNavigation)button).type==4?5: 4;
		}
		else if(button.id==101)
		{
			this.renderInfo.setShowLayer(Math.min(renderInfo.showLayer+1, renderInfo.structureHeight-1));
		}
		else if(button.id==102)
		{
			this.renderInfo.setShowLayer(Math.max(renderInfo.showLayer-1, -1));
		}
		else if(button.id==103)
			showCompleted = !showCompleted;
		super.buttonPressed(gui, button);
	}

	@Override
	public boolean listForSearch(String searchTag)
	{
		return false;
	}

	@Override
	public int getPixelsTaken()
	{
		return yOffTotal;
	}

	static class MultiblockBlockAccess implements IBlockAccess
	{
		private final MultiblockRenderInfo data;
		private final IBlockState[][][] structure;

		MultiblockBlockAccess(MultiblockRenderInfo data)
		{
			this.data = data;
			final int[] index = {0};//Nasty workaround, but IDEA suggested it =P
			this.structure = Arrays.stream(data.data).map(layer -> {
				return Arrays.stream(layer).map(row -> {
					return Arrays.stream(row).map(itemstack -> {
						return convert(index[0]++, itemstack);
					}).collect(Collectors.toList()).toArray(new IBlockState[0]);
				}).collect(Collectors.toList()).toArray(new IBlockState[0][]);
			}).collect(Collectors.toList()).toArray(new IBlockState[0][][]);
		}

		private IBlockState convert(int index, ItemStack itemstack)
		{
			if(itemstack==null)
				return Blocks.AIR.getDefaultState();
			IBlockState state = data.multiblock.getBlockstateFromStack(index, itemstack);
			if(state!=null)
				return state;
			return Blocks.AIR.getDefaultState();
		}

		@Nullable
		@Override
		public TileEntity getTileEntity(BlockPos pos)
		{
			return null;
		}

		@Override
		public int getCombinedLight(BlockPos pos, int lightValue)
		{
			// full brightness always
			return 15<<20|15<<4;
		}

		@Override
		public IBlockState getBlockState(BlockPos pos)
		{
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();

			if(y >= 0&&y < structure.length)
				if(x >= 0&&x < structure[y].length)
					if(z >= 0&&z < structure[y][x].length)
					{
						int index = y*(data.structureLength*data.structureWidth)+x*data.structureWidth+z;
						if(index <= data.getLimiter())
							return structure[y][x][z];
					}
			return Blocks.AIR.getDefaultState();
		}

		@Override
		public boolean isAirBlock(BlockPos pos)
		{
			return getBlockState(pos).getBlock()==Blocks.AIR;
		}

		@Override
		public Biome getBiome(BlockPos pos)
		{
			World world = Minecraft.getInstance().world;
			if(world!=null)
				return world.getBiome(pos);
			else
				return Biomes.BIRCH_FOREST;
		}

		@Override
		public int getStrongPower(BlockPos pos, EnumFacing direction)
		{
			return 0;
		}

		@Override
		public WorldType getWorldType()
		{

			World world = Minecraft.getInstance().world;
			if(world!=null)
				return world.getWorldType();
			else
				return WorldType.DEFAULT;
		}

		@Override
		public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default)
		{
			return false;
		}
	}

	//Stolen back from boni's StructureInfo
	static class MultiblockRenderInfo
	{
		public IMultiblock multiblock;
		public ItemStack[][][] data;
		int blockCount = 0;
		int[] countPerLevel;
		int structureHeight = 0;
		int structureLength = 0;
		int structureWidth = 0;
		int showLayer = -1;

		private int blockIndex = -1;
		private int maxBlockIndex;

		MultiblockRenderInfo(IMultiblock multiblock)
		{
			this.multiblock = multiblock;
			init(multiblock.getStructureManual());
			maxBlockIndex = blockIndex = structureHeight*structureLength*structureWidth;
		}

		public void init(ItemStack[][][] structure)
		{
			data = structure;
			structureHeight = structure.length;
			structureWidth = 0;
			structureLength = 0;

			countPerLevel = new int[structureHeight];
			blockCount = 0;
			for(int h = 0; h < structure.length; h++)
			{
				if(structure[h].length > structureLength)
					structureLength = structure[h].length;
				int perLvl = 0;
				for(int l = 0; l < structure[h].length; l++)
				{
					if(structure[h][l].length > structureWidth)
						structureWidth = structure[h][l].length;
					for(ItemStack ss : structure[h][l])
						if(ss!=null&&!ss.isEmpty())
							perLvl++;
				}
				countPerLevel[h] = perLvl;
				blockCount += perLvl;
			}
		}

		void setShowLayer(int layer)
		{
			showLayer = layer;
			if(layer < 0)
				reset();
			else
				blockIndex = (layer+1)*(structureLength*structureWidth)-1;
		}

		public void reset()
		{
			blockIndex = maxBlockIndex;
		}

		void step()
		{
			int start = blockIndex;
			do
			{
				if(++blockIndex >= maxBlockIndex)
					blockIndex = 0;
			}
			while(isEmpty(blockIndex)&&blockIndex!=start);
		}

		private boolean isEmpty(int index)
		{
			int y = index/(structureLength*structureWidth);
			int r = index%(structureLength*structureWidth);
			int x = r/structureWidth;
			int z = r%structureWidth;

			ItemStack stack = data[y][x][z];
			return stack==null||stack.isEmpty();
		}

		int getLimiter()
		{
			return blockIndex;
		}
	}
}