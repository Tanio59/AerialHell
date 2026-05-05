package fr.factionbedrock.aerialhell.Recipe;

import com.mojang.serialization.MapCodec;
import fr.factionbedrock.aerialhell.Item.TornPageItem;
import fr.factionbedrock.aerialhell.Registry.AerialHellItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;

public class BookPageRecipe extends CustomRecipe
{
    public static final RecipeSerializer<BookPageRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(new BookPageRecipe()),
            StreamCodec.<RegistryFriendlyByteBuf, BookPageRecipe>of(
                    (buf, recipe) -> {},
                    buf -> new BookPageRecipe()
            )
    );

    public BookPageRecipe()
    {
        super();
    }

    @Override
    public boolean matches(CraftingInput input, Level level)
    {
        boolean hasBook     = false;
        boolean hasTornPage = false;

        for (int i = 0; i < input.size(); i++)
        {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(AerialHellItems.AERIAL_HELL_BOOK.get())) hasBook     = true;
            if (stack.getItem() instanceof TornPageItem)           hasTornPage = true;
        }

        return hasBook && hasTornPage;
    }

    @Override
    public ItemStack assemble(CraftingInput input)
    {
        ItemStack    book     = ItemStack.EMPTY;
        TornPageItem tornPage = null;

        for (int i = 0; i < input.size(); i++)
        {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(AerialHellItems.AERIAL_HELL_BOOK.get())) book     = stack.copy();
            if (stack.getItem() instanceof TornPageItem tp)        tornPage = tp;
        }

        if (book.isEmpty() || tornPage == null) return ItemStack.EMPTY;

        CompoundTag tag   = book.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag     pages = tag.contains("inserted_pages")
                ? tag.getList("inserted_pages").orElse(new ListTag())
                : new ListTag();

        String  pageId          = tornPage.getPageId();
        boolean alreadyInserted = pages.stream().anyMatch(t -> t instanceof StringTag st && st.value().equals(pageId));

        if (alreadyInserted) return ItemStack.EMPTY;

        pages.add(StringTag.valueOf(pageId));
        tag.put("inserted_pages", pages);
        book.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        return book;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer()
    {
        return SERIALIZER;
    }
}