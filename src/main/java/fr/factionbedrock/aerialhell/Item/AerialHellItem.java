package fr.factionbedrock.aerialhell.Item;

import fr.factionbedrock.aerialhell.Item.Ability.*;
import fr.factionbedrock.aerialhell.Item.Material.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.ItemAbilities;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

// For Items with AerialHellToolMaterials : inspired of vanilla Item class, with custom AerialhellItem.Properties properties
// For Tools : Inspired of vanilla AxeItem, HoeItem and ShovelItem, but only takes Item.Properties as constructor parameter.
// AxeItem, HoeItem and ShovelItem interaction abilities are all managed.
// To make the item behave like a tool, call properties.sword(...), properties.pickaxe(...), properties.axe(...), properties.hoe(...), properties.shovel(...) in properties before passing them to the constructor.
// To manage AxeItem, HoeItem and ShovelItem interaction abilities, think about calling .useInteraction(...) if you want your tool to be able to strip, flatten or till.
public class AerialHellItem extends WithInformationItem
{
	public final int maxUseDuration;
	public final ItemUseAnimation itemUseAnimation;
	@Nullable public final AbilitySelector abilitySelector;
	public final List<UseInteractionType> useInteractionToolTypes;

	public AerialHellItem(AerialHellItem.Properties properties)
	{
		super(properties);
		this.maxUseDuration = properties.maxUseDuration;
		this.itemUseAnimation = properties.itemUseAnimation;
		this.abilitySelector = properties.abilitySelector;
		this.useInteractionToolTypes = properties.useInteractionTypes;
	}

	//applying tick (passive) tool ability modules
	@Override public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot)
	{
		if (this.abilitySelector != null && entity instanceof LivingEntity itemOwner && entity.tickCount % 10 == 0)
		{
			@Nullable UsingItemUseSituationInfo usingItemUseSituationInfo = new UsingItemUseSituationInfo(itemOwner instanceof Player player && player.isUsingItem() ? player.getTicksUsingItem() : 0);
			this.abilitySelector.tryUseAbility(new AbilityUseSituation.Tick(stack, itemOwner, slot, usingItemUseSituationInfo));
		}
	}

	//applying use tool ability modules
	@Override public InteractionResult use(Level level, Player player, InteractionHand hand)
	{
		ItemStack heldItemStack = player.getItemInHand(hand);
		boolean used = false;
		if (this.abilitySelector != null) {used = this.abilitySelector.tryUseAbility(new AbilityUseSituation.OnUse(heldItemStack, player, hand.asEquipmentSlot()));}
		if (used && this.maxUseDuration != 0) {player.startUsingItem(hand);}
		return used ? InteractionResult.CONSUME : super.use(level, player, hand);
	}

	@Override public int getUseDuration(ItemStack itemStack, LivingEntity user)
	{
		int vanillaUseDuration = super.getUseDuration(itemStack, user);
		return vanillaUseDuration > 0 ? vanillaUseDuration : this.maxUseDuration;
	}

	@Override public ItemUseAnimation getUseAnimation(ItemStack itemStack)
	{
		ItemUseAnimation vanillaUseAnimation = super.getUseAnimation(itemStack);
		return vanillaUseAnimation != ItemUseAnimation.NONE ? vanillaUseAnimation : this.itemUseAnimation;
	}

	//applying releaseUsing tool ability modules
	@Override public boolean releaseUsing(ItemStack itemStack, Level level, LivingEntity itemOwner, int remainingTime)
	{
		int ticksUsed = this.getUseDuration(itemStack, itemOwner) - remainingTime;
		boolean used = false;
		if (this.abilitySelector != null) {used = this.abilitySelector.tryUseAbility(new AbilityUseSituation.OnReleaseUsing(itemStack, itemOwner, new UsingItemUseSituationInfo(ticksUsed)));}
		return used;
	}

	//applying onDealDamage (semi-passive) tool ability modules
	//enemy entity (stored in damageInfo) is taking damage from item owner
	public void onDealDamage(ItemStack itemStack, LivingEntity itemOwner, @Nullable EquipmentSlot slot, DamageUseSituationInfo damageInfo)
	{
		if (this.abilitySelector != null) {this.abilitySelector.tryUseAbility(new AbilityUseSituation.OnDealDamage(itemStack, itemOwner, slot, damageInfo));}
	}

	//applying onTakeDamage (semi-passive) tool ability modules
	//item owner is taking damage from enemy attacker (stored in damageInfo)
	public void onTakeDamage(ItemStack itemStack, LivingEntity itemOwner, @Nullable EquipmentSlot slot, DamageUseSituationInfo damageInfo)
	{
		if (this.abilitySelector != null) {this.abilitySelector.tryUseAbility(new AbilityUseSituation.OnTakeDamage(itemStack, itemOwner, slot, damageInfo));}
	}

	//applying onMining (semi-passive) tool ability modules
	public void onMining(ItemStack itemStack, LivingEntity itemOwner, MiningUseSituationInfo miningInfo)
	{
		if (this.abilitySelector != null) {this.abilitySelector.tryUseAbility(new AbilityUseSituation.OnMining(itemStack, itemOwner, miningInfo));}
	}

	@Override public void appendAbilityDescriptionHoverText(Player player, Item.TooltipContext context, Consumer<Component> tooltipAdder)
	{
		if (!player.level().isClientSide() || this.abilitySelector == null) {return;}

		//context.player().isShiftKeyDown() do not work here because there is a screen open
		boolean shiftDown = Minecraft.getInstance().hasShiftDown();
		List<String> descIds = this.abilitySelector.getAbilitiesDescIds();

		boolean hasAbilityDetail = false;
		for (String descId : descIds)
		{
			if (Language.getInstance().has("ability.aerialhell."+descId+".desc"))
			{
				hasAbilityDetail = true;
				break;
			}
		}

		if (!shiftDown)
		{
			if (hasAbilityDetail) {tooltipAdder.accept(Component.translatable("ability.aerialhell.shift_key_up").withStyle(ChatFormatting.DARK_GRAY));}
			return;
		}

		for (String descId : descIds)
		{
			if (descId.isEmpty()) {continue;}
			this.appendOptionalDescriptionHoverText(context, tooltipAdder, "ability.aerialhell."+descId+".desc", ChatFormatting.GRAY);
			this.appendOptionalDescriptionHoverText(context, tooltipAdder, "ability.aerialhell."+descId+".desc_2", ChatFormatting.GRAY);
			this.appendOptionalDescriptionHoverText(context, tooltipAdder, "ability.aerialhell."+descId+".desc_3", ChatFormatting.GRAY);
			this.appendOptionalDescriptionHoverText(context, tooltipAdder, "ability.aerialhell."+descId+".desc_4", ChatFormatting.GRAY);
			this.appendOptionalDescriptionHoverText(context, tooltipAdder, "ability.aerialhell."+descId+".desc_5", ChatFormatting.GRAY);
			this.appendOptionalDescriptionHoverText(context, tooltipAdder, "ability.aerialhell."+descId+".desc_6", ChatFormatting.GRAY);
			this.appendOptionalDescriptionHoverText(context, tooltipAdder, "ability.aerialhell."+descId+".condition.desc", ChatFormatting.GRAY);
			this.appendOptionalDescriptionHoverText(context, tooltipAdder, "ability.aerialhell."+descId+".cooldown.desc", ChatFormatting.GRAY);
		}
	}

	//Since 26.3, vanilla axes, hoes and shovels interactions (strip, scrape, wax off, till, flatten) are data-driven block transformers
	//(minecraft:axe, minecraft:hoe and minecraft:shovel, completed by neoforge:transformables data map).
	//An item can only hold one minecraft:block_transformer component, so the transformers matching useInteractionToolTypes are applied here.
	@Override public InteractionResult useOn(UseOnContext context)
	{
		InteractionResult result = InteractionResult.PASS;
		if (this.useInteractionToolTypes.contains(UseInteractionType.AXE))
		{
			result = useBlockTransformerOn(context, BlockTransformers.AXE);
			if (result != InteractionResult.PASS) {return result;}
		}

		if (this.useInteractionToolTypes.contains(UseInteractionType.HOE))
		{
			result = useBlockTransformerOn(context, BlockTransformers.HOE);
			if (result != InteractionResult.PASS) {return result;}
		}

		if (this.useInteractionToolTypes.contains(UseInteractionType.SHOVEL))
		{
			result = useBlockTransformerOn(context, BlockTransformers.SHOVEL);
			if (result != InteractionResult.PASS) {return result;}
		}

		return result;
	}

	private static InteractionResult useBlockTransformerOn(UseOnContext context, ResourceKey<BlockTransformer> transformerKey)
	{
		Optional<Holder.Reference<BlockTransformer>> transformer = context.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK_TRANSFORMER).get(transformerKey);
		return transformer.isPresent() ? transformer.get().value().transformBlock(context, transformer.get()) : InteractionResult.PASS;
	}

	//shovel douse is still an item ability in 26.3 (axe, hoe and other shovel abilities are now block transformers, see useOn)
	@Override public boolean canPerformAction(ItemInstance stack, net.neoforged.neoforge.common.ItemAbility itemAbility)
	{
		return this.useInteractionToolTypes.contains(UseInteractionType.SHOVEL) && itemAbility == ItemAbilities.SHOVEL_DOUSE;
	}

	public static class Properties extends Item.Properties
	{
		private int maxUseDuration;
		private ItemUseAnimation itemUseAnimation;
		@Nullable private AbilitySelector abilitySelector;
		private List<AerialHellItem.UseInteractionType> useInteractionTypes;
		public Properties() {super(); this.maxUseDuration = 0; this.itemUseAnimation = ItemUseAnimation.NONE; this.useInteractionTypes = new ArrayList<>();}

		public AerialHellItem.Properties humanoidArmor(AerialHellArmorMaterial material, ArmorType type) {return this.humanoidArmor(material, type, new AttributeEntryList());}
		public AerialHellItem.Properties humanoidArmor(AerialHellArmorMaterial material, ArmorType type, AttributeEntry attributeEntry) {return this.humanoidArmor(material, type, new AttributeEntryList().add(attributeEntry));}
		public AerialHellItem.Properties humanoidArmor(AerialHellArmorMaterial material, ArmorType type, AttributeEntryList additionalAttributes)
		{
			return (AerialHellItem.Properties) this.durability(type.getDurability(material.durability())).attributes(material.createAttributes(type, additionalAttributes)).enchantable(material.enchantmentValue()).component(DataComponents.EQUIPPABLE, Equippable.builder(type.getSlot()).setEquipSound(material.equipSound()).setAsset(material.assetId()).build()).repairable(material.repairIngredient());
		}

		public AerialHellItem.Properties tool(AerialHellToolMaterial material, TagKey<Block> minesEfficiently, float attackDamage, float attackSpeed, AttributeEntryList additionalAttributes, float disableBlockingSeconds)
		{
			return material.applyToolProperties(this, minesEfficiently, attackDamage, attackSpeed, additionalAttributes, disableBlockingSeconds);
		}

		public AerialHellItem.Properties pickaxe(AerialHellToolMaterial material, float attackDamage, float attackSpeed) {return this.pickaxe(material, attackDamage, attackSpeed, new AttributeEntryList());}
		public AerialHellItem.Properties pickaxe(AerialHellToolMaterial material, float attackDamage, float attackSpeed, AttributeEntry attributeEntry) {return this.pickaxe(material, attackDamage, attackSpeed, new AttributeEntryList().add(attributeEntry));}
		public AerialHellItem.Properties pickaxe(AerialHellToolMaterial material, float attackDamage, float attackSpeed, AttributeEntryList additionalAttributes)
		{
			return this.tool(material, BlockTags.MINEABLE_WITH_PICKAXE, attackDamage, attackSpeed, additionalAttributes, 0.0F);
		}

		public AerialHellItem.Properties axe(AerialHellToolMaterial material, float attackDamage, float attackSpeed) {return this.axe(material, attackDamage, attackSpeed, new AttributeEntryList());}
		public AerialHellItem.Properties axe(AerialHellToolMaterial material, float attackDamage, float attackSpeed, AttributeEntry attributeEntry) {return this.axe(material, attackDamage, attackSpeed, new AttributeEntryList().add(attributeEntry));}
		public AerialHellItem.Properties axe(AerialHellToolMaterial material, float attackDamage, float attackSpeed, AttributeEntryList additionalAttributes)
		{
			return this.tool(material, BlockTags.MINEABLE_WITH_AXE, attackDamage, attackSpeed, additionalAttributes, 5.0F);
		}

		public AerialHellItem.Properties hoe(AerialHellToolMaterial material, float attackDamage, float attackSpeed) {return this.hoe(material, attackDamage, attackSpeed, new AttributeEntryList());}
		public AerialHellItem.Properties hoe(AerialHellToolMaterial material, float attackDamage, float attackSpeed, AttributeEntry attributeEntry) {return this.hoe(material, attackDamage, attackSpeed, new AttributeEntryList().add(attributeEntry));}
		public AerialHellItem.Properties hoe(AerialHellToolMaterial material, float attackDamage, float attackSpeed, AttributeEntryList additionalAttributes)
		{
			return this.tool(material, BlockTags.MINEABLE_WITH_HOE, attackDamage, attackSpeed, additionalAttributes, 0.0F);
		}

		public AerialHellItem.Properties shovel(AerialHellToolMaterial material, float attackDamage, float attackSpeed) {return this.shovel(material, attackDamage, attackSpeed, new AttributeEntryList());}
		public AerialHellItem.Properties shovel(AerialHellToolMaterial material, float attackDamage, float attackSpeed, AttributeEntry attributeEntry) {return this.shovel(material, attackDamage, attackSpeed, new AttributeEntryList().add(attributeEntry));}
		public AerialHellItem.Properties shovel(AerialHellToolMaterial material, float attackDamage, float attackSpeed, AttributeEntryList additionalAttributes)
		{
			return this.tool(material, BlockTags.MINEABLE_WITH_SHOVEL, attackDamage, attackSpeed, additionalAttributes, 0.0F);
		}

		public AerialHellItem.Properties sword(AerialHellToolMaterial material, float attackDamage, float attackSpeed) {return this.sword(material, attackDamage, attackSpeed, new AttributeEntryList());}
		public AerialHellItem.Properties sword(AerialHellToolMaterial material, float attackDamage, float attackSpeed, AttributeEntry attributeEntry) {return this.sword(material, attackDamage, attackSpeed, new AttributeEntryList().add(attributeEntry));}
		public AerialHellItem.Properties sword(AerialHellToolMaterial material, float attackDamage, float attackSpeed, AttributeEntryList additionalAttributes)
		{
			return material.applySwordProperties(this, attackDamage, attackSpeed, additionalAttributes);
		}

		public AerialHellItem.Properties spear(AerialHellToolMaterial material) {return material.applySpearProperties(this, new AttributeEntryList());}
		public AerialHellItem.Properties spear(AerialHellToolMaterial material, AttributeEntryList additionalAttributes) {return material.applySpearProperties(this, additionalAttributes);}

		public AerialHellItem.Properties maxUseDuration(int useDuration) {this.maxUseDuration = useDuration; return this;}

		public AerialHellItem.Properties useAnimation(ItemUseAnimation itemUseAnimation) {this.itemUseAnimation = itemUseAnimation; return this;}

		public AerialHellItem.Properties abilitySelector(AbilitySelector abilitySelector) {this.abilitySelector = abilitySelector; return this;}

		public AerialHellItem.Properties useInteraction(AerialHellItem.UseInteractionType useInteractionType) {this.useInteractionTypes = new ArrayList<>(); this.useInteractionTypes.add(useInteractionType); return this;}

		public AerialHellItem.Properties useInteractions(AerialHellItem.UseInteractionType... useInteractionTypes) {this.useInteractionTypes = new ArrayList<>(List.of(useInteractionTypes)); return this;}

		@Override public AerialHellItem.Properties setId(ResourceKey<Item> id) {return (AerialHellItem.Properties) super.setId(id);}

		@Override public AerialHellItem.Properties rarity(Rarity rarity) {return (AerialHellItem.Properties) super.rarity(rarity);}

		@Override public AerialHellItem.Properties durability(int maxDamage) {return (AerialHellItem.Properties) super.durability(maxDamage);}

		@Override public AerialHellItem.Properties fireResistant() {return (AerialHellItem.Properties) super.fireResistant();}

		@Override public AerialHellItem.Properties trimMaterial(ResourceKey<TrimMaterial> material) {return (AerialHellItem.Properties) super.trimMaterial(material);}

		@Override public AerialHellItem.Properties stacksTo(int max) {return (AerialHellItem.Properties) super.stacksTo(max);}

		@Override public <T> AerialHellItem.Properties component(DataComponentType<T> type, T value) {return (AerialHellItem.Properties) super.component(type, value);}

		@Override public AerialHellItem.Properties enchantable(int value) {return (AerialHellItem.Properties) super.enchantable(value);}
	}

	//"tool types" that can be used with right click
	public enum UseInteractionType {AXE, HOE, SHOVEL}
}
