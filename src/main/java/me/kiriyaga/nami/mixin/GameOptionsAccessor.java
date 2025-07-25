package me.kiriyaga.nami.mixin;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameOptions.class)
public interface GameOptionsAccessor {

    @Accessor("fov")
    SimpleOption<Integer> getFov();

    @Accessor("gamma")
    SimpleOption<Double> getGamma();
}
