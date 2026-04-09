package com.dp.toms_extensions.data.condition;

import com.dp.toms_extensions.TomsSimpleStorageExtensions;
import com.dp.toms_extensions.config.ModConfigData;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

public class AllowTrimSlabRecipesCondition implements ICondition {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(TomsSimpleStorageExtensions.MOD_ID, "allow_trim_slab_recipes");

    public static final AllowTrimSlabRecipesCondition INSTANCE = new AllowTrimSlabRecipesCondition();

    private AllowTrimSlabRecipesCondition() {
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        return ModConfigData.VALUES.allowTrimSlabRecipes.get();
    }

    public static class Serializer implements IConditionSerializer<AllowTrimSlabRecipesCondition> {
        public static final Serializer INSTANCE = new Serializer();

        private Serializer() {
        }

        @Override
        public ResourceLocation getID() {
            return ID;
        }

        @Override
        public AllowTrimSlabRecipesCondition read(JsonObject json) {
            return AllowTrimSlabRecipesCondition.INSTANCE;
        }

        @Override
        public void write(JsonObject json, AllowTrimSlabRecipesCondition value) {
        }
    }
}