package com.faboslav.friendsandfoes.forge.datagen;

import com.faboslav.friendsandfoes.common.FriendsAndFoes;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Source: https://github.com/BluSunrize/ImmersiveEngineering/blob/1.20.1/src/datagen/java/blusunrize/immersiveengineering/data/IEDataGenerator.java
@Mod.EventBusSubscriber(modid = FriendsAndFoes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class StructureNbtUpdaterDatagen
{
	@SubscribeEvent
	public static void gatherData(GatherDataEvent event) {
		ExistingFileHelper exHelper = event.getExistingFileHelper();
		DataGenerator gen = event.getGenerator();
		final var output = gen.getPackOutput();

		if (event.includeServer()) {
			gen.addProvider(true, new StructureNbtUpdater("structures", FriendsAndFoes.MOD_ID, exHelper, output));
		}
	}
}
