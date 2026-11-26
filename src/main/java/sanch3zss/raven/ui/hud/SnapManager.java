package sanch3zss.raven.ui.hud;


import sanch3zss.raven.render.engine.DrawEngine;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.shape.SnapGuideLine;
import sanch3zss.raven.ui.hud.Draggable;
import sanch3zss.raven.util.math.interpolation.Easings;

public class SnapManager {
    static final float aG = 8.0f;
    static final float aH = 1.0f;
    public final ToggleAnimator gridAnimation = new ToggleAnimator(150, Easings.EASE_OUT_CUBIC);
    public final List<SnapGuideLine> activeLines = new ArrayList();
    public boolean snapping = false;
    public float aI = -1.0f;
    public float aJ = -1.0f;

    public SnapResult calculateSnap(Draggable draggable2, List<Draggable> list, float f, float f2, float f3, float f4) {
        this.activeLines.clear();
        float fWidth = draggable2.scaledWidth();
        float fHeight = draggable2.scaledHeight();
        float f5 = f + fWidth;
        float f6 = f + (fWidth / 2.0f);
        float f7 = f2 + fHeight;
        float f8 = f2 + (fHeight / 2.0f);
        float f9 = f;
        float f10 = f2;
        boolean z = false;
        boolean z2 = false;
        float f11 = f3 / 2.0f;
        float f12 = f4 / 2.0f;
        if (Math.abs(f6 - f11) < aG) {
            f9 = f11 - (fWidth / 2.0f);
            z = true;
            this.activeLines.add(new SnapGuideLine(f11, false, 0.0f, f4));
        }
        if (Math.abs(f8 - f12) < aG) {
            f10 = f12 - (fHeight / 2.0f);
            z2 = true;
            this.activeLines.add(new SnapGuideLine(f12, true, 0.0f, f3));
        }
        for (Draggable draggable : list) {
            if (draggable != draggable2 && draggable.isVisible()) {
                float x = draggable.getX();
                float x2 = draggable.getX() + draggable.scaledWidth();
                float x3 = draggable.getX() + (draggable.scaledWidth() / 2.0f);
                float y = draggable.getY();
                float y2 = draggable.getY() + draggable.scaledHeight();
                float y3 = draggable.getY() + (draggable.scaledHeight() / 2.0f);
                if (!z) {
                    if (Math.abs(f - x) < aG) {
                        f9 = x;
                        z = true;
                        this.activeLines.add(new SnapGuideLine(x, false, Math.min(f2, y) - 10.0f, Math.max(f7, y2) + 10.0f));
                    } else if (Math.abs(f5 - x2) < aG) {
                        f9 = x2 - fWidth;
                        z = true;
                        this.activeLines.add(new SnapGuideLine(x2, false, Math.min(f2, y) - 10.0f, Math.max(f7, y2) + 10.0f));
                    } else if (Math.abs(f6 - x3) < aG) {
                        f9 = x3 - (fWidth / 2.0f);
                        z = true;
                        this.activeLines.add(new SnapGuideLine(x3, false, Math.min(f2, y) - 10.0f, Math.max(f7, y2) + 10.0f));
                    } else if (Math.abs(f - x2) < aG) {
                        f9 = x2;
                        z = true;
                        this.activeLines.add(new SnapGuideLine(x2, false, Math.min(f2, y) - 10.0f, Math.max(f7, y2) + 10.0f));
                    } else if (Math.abs(f5 - x) < aG) {
                        f9 = x - fWidth;
                        z = true;
                        this.activeLines.add(new SnapGuideLine(x, false, Math.min(f2, y) - 10.0f, Math.max(f7, y2) + 10.0f));
                    }
                }
                if (!z2) {
                    if (Math.abs(f2 - y) < aG) {
                        f10 = y;
                        z2 = true;
                        this.activeLines.add(new SnapGuideLine(y, true, Math.min(f, x) - 10.0f, Math.max(f5, x2) + 10.0f));
                    } else if (Math.abs(f7 - y2) < aG) {
                        f10 = y2 - fHeight;
                        z2 = true;
                        this.activeLines.add(new SnapGuideLine(y2, true, Math.min(f, x) - 10.0f, Math.max(f5, x2) + 10.0f));
                    } else if (Math.abs(f8 - y3) < aG) {
                        f10 = y3 - (fHeight / 2.0f);
                        z2 = true;
                        this.activeLines.add(new SnapGuideLine(y3, true, Math.min(f, x) - 10.0f, Math.max(f5, x2) + 10.0f));
                    } else if (Math.abs(f2 - y2) < aG) {
                        f10 = y2;
                        z2 = true;
                        this.activeLines.add(new SnapGuideLine(y2, true, Math.min(f, x) - 10.0f, Math.max(f5, x2) + 10.0f));
                    } else if (Math.abs(f7 - y) < aG) {
                        f10 = y - fHeight;
                        z2 = true;
                        this.activeLines.add(new SnapGuideLine(y, true, Math.min(f, x) - 10.0f, Math.max(f5, x2) + 10.0f));
                    }
                }
            }
        }
        this.snapping = z || z2;
        this.aI = z ? f9 : -1.0f;
        this.aJ = z2 ? f10 : -1.0f;
        return new SnapResult(f9, f10, z, z2);
    }

    public void animate(WeightedEngine weightedEngine, boolean z) {
        this.gridAnimation.state(z && this.snapping).animate(weightedEngine);
    }

    public void render(DragRenderContext dragRenderContext) {
        float fSmoothAnimation = this.gridAnimation.smoothAnimation();
        if (fSmoothAnimation <= 0.0f || this.activeLines.isEmpty()) {
            return;
        }
        DrawEngine drawEngine = dragRenderContext.drawEngine();
        PoseStack matrixStack = dragRenderContext.matrixStack();
        int i = (((int) (255.0f * fSmoothAnimation)) << 24) | 16777215;
        for (SnapGuideLine snapGuideLine : this.activeLines) {
            if (snapGuideLine.horizontal) {
                drawEngine.rectangle(matrixStack.last().pose(), snapGuideLine.start, snapGuideLine.position, snapGuideLine.end - snapGuideLine.start, aH, i);
            } else {
                drawEngine.rectangle(matrixStack.last().pose(), snapGuideLine.position, snapGuideLine.start, aH, snapGuideLine.end - snapGuideLine.start, i);
            }
        }
    }

    public void clear() {
        this.activeLines.clear();
        this.snapping = false;
    }

    public ToggleAnimator getGridAnimation() {
        return this.gridAnimation;
    }

    public List<SnapGuideLine> getActiveLines() {
        return this.activeLines;
    }

    public boolean isSnapping() {
        return this.snapping;
    }

    public float getLastSnapX() {
        return this.aI;
    }

    public float getLastSnapY() {
        return this.aJ;
    }
}
