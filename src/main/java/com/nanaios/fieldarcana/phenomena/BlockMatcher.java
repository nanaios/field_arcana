package com.nanaios.fieldarcana.phenomena;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** {@code "minecraft:sand"} or {@code "#minecraft:logs"}. */
public record BlockMatcher(Optional<TagKey<Block>> tag, Optional<Block> block) {
    public static final Codec<BlockMatcher> CODEC = Codec.STRING.comapFlatMap(BlockMatcher::parse, BlockMatcher::toString);

    private static DataResult<BlockMatcher> parse(String text) {
        boolean isTag = text.startsWith("#");
        ResourceLocation id = ResourceLocation.tryParse(isTag ? text.substring(1) : text);
        if (id == null) {
            return DataResult.error(() -> "Invalid block or tag: " + text);
        }
        if (isTag) {
            return DataResult.success(new BlockMatcher(Optional.of(TagKey.create(Registries.BLOCK, id)), Optional.empty()));
        }
        return BuiltInRegistries.BLOCK.getOptional(id)
                .map(b -> DataResult.success(new BlockMatcher(Optional.empty(), Optional.of(b))))
                .orElseGet(() -> DataResult.error(() -> "Unknown block: " + id));
    }

    public boolean test(BlockState state) {
        return tag.map(state::is).orElseGet(() -> block.map(state::is).orElse(false));
    }

    @Override
    public String toString() {
        return tag.map(t -> "#" + t.location()).orElseGet(() -> block.map(b -> BuiltInRegistries.BLOCK.getKey(b).toString()).orElse("?"));
    }
}
