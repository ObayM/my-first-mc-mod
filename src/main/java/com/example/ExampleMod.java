package com.example;

import com.example.entity.VoidWraithEntity;
import com.example.item.LaserWandItem;
import com.example.item.VoidstrikeScepterItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleMod implements ModInitializer {
	public static final String MOD_ID = "modid";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final ResourceKey<CreativeModeTab> COMBAT_TAB =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("combat"));

	private static final ResourceKey<CreativeModeTab> INGREDIENTS_TAB =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("ingredients"));

	private static final ResourceKey<CreativeModeTab> SPAWN_EGGS_TAB =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("spawn_eggs"));

	private static final ResourceKey<Item> LASER_WAND_KEY =
			ResourceKey.create(Registries.ITEM, id("laser_wand"));

	public static final Item LASER_WAND = Registry.register(
		BuiltInRegistries.ITEM,
		LASER_WAND_KEY,
		new LaserWandItem(new Item.Properties().stacksTo(1).setId(LASER_WAND_KEY))
	);

	private static final ResourceKey<Item> VOIDSTRIKE_SCEPTER_KEY =
			ResourceKey.create(Registries.ITEM, id("voidstrike_scepter"));

	public static final Item VOIDSTRIKE_SCEPTER = Registry.register(
		BuiltInRegistries.ITEM,
		VOIDSTRIKE_SCEPTER_KEY,
		new VoidstrikeScepterItem(new Item.Properties().stacksTo(1).setId(VOIDSTRIKE_SCEPTER_KEY))
	);

	private static final Identifier ENDERMAN_LOOT_TABLE = Identifier.withDefaultNamespace("entities/enderman");
	private static final float VOID_SHARD_DROP_CHANCE = 0.08f;

	private static final ResourceKey<Item> VOID_SHARD_KEY =
			ResourceKey.create(Registries.ITEM, id("void_shard"));

	public static final Item VOID_SHARD = Registry.register(
		BuiltInRegistries.ITEM,
		VOID_SHARD_KEY,
		new Item(new Item.Properties().setId(VOID_SHARD_KEY))
	);

	private static final ResourceKey<EntityType<?>> VOID_WRAITH_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, id("void_wraith"));

	public static final EntityType<VoidWraithEntity> VOID_WRAITH = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		VOID_WRAITH_KEY,
		EntityType.Builder.of(VoidWraithEntity::new, MobCategory.MONSTER)
			.sized(0.4f, 0.8f)
			.build(VOID_WRAITH_KEY)
	);

	private static final ResourceKey<Item> VOID_WRAITH_SPAWN_EGG_KEY =
			ResourceKey.create(Registries.ITEM, id("void_wraith_spawn_egg"));

	public static final Item VOID_WRAITH_SPAWN_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		VOID_WRAITH_SPAWN_EGG_KEY,
		new SpawnEggItem(new Item.Properties().setId(VOID_WRAITH_SPAWN_EGG_KEY).spawnEgg(VOID_WRAITH))
	);

	@Override
	public void onInitialize() {
		FabricDefaultAttributeRegistry.register(VOID_WRAITH, VoidWraithEntity.createAttributes());

		CreativeModeTabEvents.modifyOutputEvent(COMBAT_TAB).register(entries -> {
			entries.accept(LASER_WAND);
			entries.accept(VOIDSTRIKE_SCEPTER);
		});
		CreativeModeTabEvents.modifyOutputEvent(INGREDIENTS_TAB).register(entries -> {
			entries.accept(VOID_SHARD);
		});
		CreativeModeTabEvents.modifyOutputEvent(SPAWN_EGGS_TAB).register(entries -> {
			entries.accept(VOID_WRAITH_SPAWN_EGG);
		});

		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			if (table.is(ENDERMAN_LOOT_TABLE) && context.getRandom().nextFloat() < VOID_SHARD_DROP_CHANCE) {
				drops.add(new ItemStack(VOID_SHARD));
			}
		});

		LOGGER.info("Hello Fabric world!");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
