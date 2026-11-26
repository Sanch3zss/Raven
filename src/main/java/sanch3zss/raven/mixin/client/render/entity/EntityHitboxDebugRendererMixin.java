package sanch3zss.raven.mixin.client.render.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import sanch3zss.raven.feature.module.render.CustomHitBoxModule;

@Mixin(EntityHitboxDebugRenderer.class)
public abstract class EntityHitboxDebugRendererMixin {
    @ModifyExpressionValue(method = "showHitboxes", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/gizmos/GizmoStyle;stroke(I)Lnet/minecraft/gizmos/GizmoStyle;", ordinal = 0))
    private GizmoStyle raven$customHitBoxStyle(GizmoStyle original) {
        CustomHitBoxModule module = CustomHitBoxModule.active();
        if (module == null) return original;
        int outline = module.outlineColor();
        return module.fill.isValue()
                ? GizmoStyle.strokeAndFill(outline, original.strokeWidth(), module.fillColor())
                : GizmoStyle.stroke(outline, original.strokeWidth());
    }

    @ModifyArg(method = "showHitboxes", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/gizmos/Gizmos;point(Lnet/minecraft/world/phys/Vec3;IF)Lnet/minecraft/gizmos/GizmoProperties;"), index = 1)
    private int raven$customHitBoxPoint(int original) {
        CustomHitBoxModule module = CustomHitBoxModule.active();
        return module == null ? original : module.outlineColor();
    }

    @WrapOperation(method = "showHitboxes", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/gizmos/Gizmos;arrow(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;I)Lnet/minecraft/gizmos/GizmoProperties;",
            ordinal = 0))
    private GizmoProperties raven$customHitBoxDirection(Vec3 start, Vec3 end, int color,
                                                        Operation<GizmoProperties> original) {
        CustomHitBoxModule module = CustomHitBoxModule.active();
        if (module == null) return original.call(start, end, color);
        return module.direction.isValue() ? original.call(start, end, module.outlineColor()) : null;
    }

    @WrapOperation(method = "showHitboxes", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/gizmos/Gizmos;cuboid(Lnet/minecraft/world/phys/AABB;Lnet/minecraft/gizmos/GizmoStyle;)Lnet/minecraft/gizmos/GizmoProperties;",
            ordinal = 2))
    private GizmoProperties raven$customHitBoxEyeLine(AABB box, GizmoStyle style,
                                                      Operation<GizmoProperties> original) {
        CustomHitBoxModule module = CustomHitBoxModule.active();
        return module == null || module.eyeLine.isValue() ? original.call(box, style) : null;
    }
}
