package me.kiriyaga.nami.mixin;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.entity.player.PlayerModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(GameOptions.class)
public interface GameOptionsAccessor {

    @Accessor("fov")
    SimpleOption<Integer> getFov();

    @Accessor("gamma")
    SimpleOption<Double> getGamma();

    @Accessor("enabledPlayerModelParts")
    Set<PlayerModelPart> getPlayerModelParts();

    @Accessor("enabledPlayerModelParts")
    void setPlayerModelParts(Set<PlayerModelPart> parts);
}
