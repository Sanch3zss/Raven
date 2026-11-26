package sanch3zss.raven.mixin.accessor;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
    @Accessor("age")
    int raven$getAge();

    @Accessor("xo")
    double raven$getPreviousX();

    @Accessor("yo")
    double raven$getPreviousY();

    @Accessor("zo")
    double raven$getPreviousZ();

    @Accessor("x")
    double raven$getX();

    @Accessor("y")
    double raven$getY();

    @Accessor("z")
    double raven$getZ();
}
