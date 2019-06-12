package com.linesix.akhaten.proxy;

import com.linesix.akhaten.common.blocks.registries.BuildingBlocks;
import com.linesix.akhaten.common.blocks.registries.DimBlocks;
import com.linesix.akhaten.common.blocks.registries.MachineBlocks;
import com.linesix.akhaten.common.dimensions.Dimensions;
import com.linesix.akhaten.common.items.registries.Gadgets;
import com.linesix.akhaten.common.items.registries.Materials;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {

        DimBlocks.init();
        MachineBlocks.init();
        BuildingBlocks.init();
        Gadgets.init();
        Materials.init();

        super.preInit(event);

    }

    public void init(FMLInitializationEvent event) {

        Dimensions.registerDimensions();

        super.init(event);

    }

    public void postInit(FMLPostInitializationEvent event) {

        super.postInit(event);

    }


}
