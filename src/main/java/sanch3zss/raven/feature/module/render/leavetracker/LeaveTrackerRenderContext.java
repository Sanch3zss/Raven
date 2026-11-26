package sanch3zss.raven.feature.module.render.leavetracker;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import sanch3zss.raven.mixin.accessor.RenderSetupAccessor;
import sanch3zss.raven.mixin.accessor.RenderSetupTextureBindingAccessor;
import sanch3zss.raven.mixin.accessor.RenderTypeAccessor;

public enum LeaveTrackerRenderContext {
    INSTANCE;

    private boolean active;

    public void submit(Runnable action) {
        boolean previous = this.active;
        this.active = true;
        try {
            action.run();
        } finally {
            this.active = previous;
        }
    }

    public RenderType renderType(RenderType original) {
        if (!this.active || original.isOutline()) {
            return original;
        }
        Object binding = ((RenderSetupAccessor) (Object) ((RenderTypeAccessor) original).raven$state())
                .raven$textures().get("Sampler0");
        return binding instanceof RenderSetupTextureBindingAccessor texture
                ? RenderTypes.entityTranslucent(texture.raven$location()) : original;
    }

    public int color(int original) {
        if (!this.active) {
            return original;
        }
        return ghostColor(original);
    }

    public int ghostColor(int original) {
        int alpha = Math.round((original >>> 24) * 0.48f);
        return alpha << 24 | original & 0x00FFFFFF;
    }

    public boolean isActive() {
        return this.active;
    }
}
