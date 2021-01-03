/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.util.compat;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.api.IEEnums.IOSideConfig;
import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.api.energy.immersiveflux.IFluxProvider;
import blusunrize.immersiveengineering.api.energy.immersiveflux.IFluxReceiver;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
//import blusunrize.immersiveengineering.common.blocks.TileEntityMultiblockPart;
import blusunrize.immersiveengineering.common.blocks.metal.SheetmetalTankTileEntity;
//import blusunrize.immersiveengineering.common.blocks.metal.TileEntityTeslaCoil;
import com.google.common.base.Function;
import mcjty.theoneprobe.Tools;
import mcjty.theoneprobe.api.*;
import mcjty.theoneprobe.config.Config;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.World;
import net.minecraft.util.Direction;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.InterModComms;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * @author BluSunrize - 12.10.2016
 */
public class OneProbeHelper extends IECompatModule implements Function<ITheOneProbe, Void>
{
	@Override
	public void preInit()
	{
		Supplier<Function<ITheOneProbe, Void>> callbackClass = () -> this;
		InterModComms.sendTo("theoneprobe", "getTheOneProbe", callbackClass);
	}

	@Override
	public void registerRecipes()
	{
	}

	@Override
	public void init()
	{

	}

	@Override
	public void postInit()
	{

	}

	@Nullable
	@Override
	public Void apply(@Nullable ITheOneProbe input)
	{
		EnergyInfoProvider energyInfo = new EnergyInfoProvider();
		input.registerProvider(energyInfo);
		input.registerProbeConfigProvider(energyInfo);
		input.registerProvider(new ProcessProvider());
		input.registerProvider(new SideConfigProvider());
		input.registerProvider(new FluidInfoProvider());
//		input.registerBlockDisplayOverride(new MultiblockDisplayOverride());
		return null;
	}

	public static class FluidInfoProvider implements IProbeInfoProvider
	{

		@Override
		public String getID()
		{
			return ImmersiveEngineering.MODID+":"+"FluidInfo";
		}

		@Override
		public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, PlayerEntity player, World world, BlockState blockState, IProbeHitData data)
		{
			TileEntity te = world.getTileEntity(data.getPos());
			if(te instanceof SheetmetalTankTileEntity)
			{
				SheetmetalTankTileEntity master = ((SheetmetalTankTileEntity)te).master();
				int current = master.tank.getFluidAmount();
				int max = master.tank.getCapacity();

				if(current > 0)
				{
					probeInfo.progress(current, max,
							probeInfo.defaultProgressStyle()
									.suffix("mB")
									.numberFormat(NumberFormat.COMPACT));
				}
			}
		}
	}


	public static class EnergyInfoProvider implements IProbeInfoProvider, IProbeConfigProvider
	{

		@Override
		public String getID()
		{
			return ImmersiveEngineering.MODID+":"+"EnergyInfo";
		}

		@Override
		public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, PlayerEntity player, World world, BlockState blockState, IProbeHitData data)
		{
			TileEntity te = world.getTileEntity(data.getPos());
			int cur = 0;
			int max = 0;
			if(te instanceof IFluxReceiver)
			{
				cur = ((IFluxReceiver)te).getEnergyStored(null);
				max = ((IFluxReceiver)te).getMaxEnergyStored(null);
			}
			else if(te instanceof IFluxProvider)
			{
				cur = ((IFluxProvider)te).getEnergyStored(null);
				max = ((IFluxProvider)te).getMaxEnergyStored(null);
			}
			if(max > 0)
			{
				probeInfo.progress(cur, max,
						probeInfo.defaultProgressStyle()
								.suffix("IF")
								.filledColor(Lib.COLOUR_I_ImmersiveOrange)
								.alternateFilledColor(0xff994f20)
								.borderColor(Lib.COLOUR_I_ImmersiveOrangeShadow)
								.numberFormat(NumberFormat.COMPACT));
			}
		}

		@Override
		public void getProbeConfig(IProbeConfig config, PlayerEntity player, World world, Entity entity, IProbeHitEntityData data)
		{
		}

		@Override
		public void getProbeConfig(IProbeConfig config, PlayerEntity player, World world, BlockState blockState, IProbeHitData data)
		{
			TileEntity te = world.getTileEntity(data.getPos());
			if(te instanceof IFluxReceiver||te instanceof IFluxProvider)
				config.setRFMode(0);
		}
	}

	public static class ProcessProvider implements IProbeInfoProvider
	{

		@Override
		public String getID()
		{
			return ImmersiveEngineering.MODID+":"+"ProcessInfo";
		}

		@Override
		public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, PlayerEntity player, World world, BlockState blockState, IProbeHitData data)
		{
			TileEntity te = world.getTileEntity(data.getPos());
			if(te instanceof IEBlockInterfaces.IProcessTile)
			{
				int[] curTicks = ((IEBlockInterfaces.IProcessTile)te).getCurrentProcessesStep();
				int[] maxTicks = ((IEBlockInterfaces.IProcessTile)te).getCurrentProcessesMax();
				int h = Math.max(4, (int)Math.ceil(12/(float)curTicks.length));
				for(int i = 0; i < curTicks.length; i++)
					if(maxTicks[i] > 0)
					{
						float f = curTicks[i]/(float)maxTicks[i]*100;
						probeInfo.progress((int)f, 100, probeInfo.defaultProgressStyle().showText(h >= 10).suffix("%").height(h));
					}
			}
		}
	}

	public static class SideConfigProvider implements IProbeInfoProvider
	{

		@Override
		public String getID()
		{
			return ImmersiveEngineering.MODID+":"+"SideConfigInfo";
		}

		@Override
		public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, PlayerEntity player, World world, BlockState blockState, IProbeHitData data)
		{
			TileEntity te = world.getTileEntity(data.getPos());
			if(te instanceof IEBlockInterfaces.IConfigurableSides&&data.getSideHit()!=null)
			{
				boolean flip = player.isSneaking();
				Direction side = flip ? data.getSideHit().getOpposite() : data.getSideHit();
				IOSideConfig config = ((IEBlockInterfaces.IConfigurableSides)te).getSideConfig(side);
				
				String direction = I18n.format(Lib.DESC_INFO+"blockSide." + (flip?"opposite": "facing")) + ": ";
				String connection = I18n.format(Lib.DESC_INFO+"blockSide.io." + config.getString());
				probeInfo.text(new StringTextComponent(direction + connection));
			}
		}
	}

//	public static class MultiblockDisplayOverride implements IBlockDisplayOverride
//	{
//		@Override
//		public boolean overrideStandardInfo(ProbeMode mode, IProbeInfo probeInfo, PlayerEntity player, World world, BlockState blockState, IProbeHitData data)
//		{
//			TileEntity te = world.getTileEntity(data.getPos());
//			if(te instanceof TileEntityMultiblockPart)
//			{
//				ItemStack stack = new ItemStack(blockState.getBlock(), 1, blockState.getBlock().getMetaFromState(blockState));
//				if(Tools.show(mode, Config.getRealConfig().getShowModName()))
//				{
//					probeInfo.horizontal()
//							.item(stack)
//							.vertical()
//							.itemLabel(stack)
//							.text(TextStyleClass.MODNAME+ImmersiveEngineering.MODNAME);
//				}
//				else
//				{
//					probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
//							.item(stack)
//							.itemLabel(stack);
//				}
//				return true;
//			}
//			return false;
//		}
//	}
}