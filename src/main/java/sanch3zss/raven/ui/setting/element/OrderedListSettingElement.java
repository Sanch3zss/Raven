package sanch3zss.raven.ui.setting.element;

import java.util.*;
import java.util.function.*;

import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.client.InputEvent;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.animation.HighlightAnimation;
import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.component.control.ToggleSwitch;
import sanch3zss.raven.ui.component.layout.OrderedListRow;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.layout.ModuleFrame;
import sanch3zss.raven.util.math.interpolation.Easings;

public class OrderedListSettingElement<E extends Enum<E>> extends ModuleFrame {
    public static final float ROW_GAP = 6.0f;
    public static final float ROW_HEIGHT = 20.0f;
    public static final float PADDING = 10.0f;
    public static final float ICON_SIZE = 12.0f;
    public static final float SWITCH_WIDTH = 22.0f;
    public static final float SWITCH_HEIGHT = 15.0f;
    public static final int fontSize = 12;

    public final Supplier<List<E>> orderSupplier;
    public final Supplier<Set<E>> selectedSupplier;
    public final Function<E, Translation> nameFunction;
    public final Consumer<E> toggleConsumer;
    public final BiConsumer<Integer, Integer> moveConsumer;
    public Supplier<Boolean> visibilitySupplier;
    public boolean dragging;
    public E draggedValue;
    public float dragOffset;
    public float dragMouseY;
    public final MsdfFont font = Fonts.INTER_BOLD.get();
    public final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    public final Map<E, OrderedListRow<E>> rowCache = new HashMap<>();
    public final List<OrderedListRow<E>> rows = new ArrayList<>();
    public final List<E> currentOrder = new ArrayList<>();
    public final WidgetBounds bounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public int draggedIndex = -1;

    public OrderedListSettingElement(Supplier<List<E>> supplier, Supplier<Set<E>> supplier2, Function<E, Translation> function, Consumer<E> consumer, BiConsumer<Integer, Integer> biConsumer) {
        this.orderSupplier = supplier;
        this.selectedSupplier = supplier2;
        this.nameFunction = function;
        this.toggleConsumer = consumer;
        this.moveConsumer = biConsumer;
    }

    @Override
    public void highlight() {
        this.highlightAnimation.trigger();
    }

    @Override
    public void clearHighlight() {
        this.highlightAnimation.clearHighlight();
    }

    @Override
    public void draw(DrawCtx drawCtx, float f, float f2) {
        rebuildRows();
        ThemePalette palette = drawCtx.theme().palette();
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        float fY = y();
        float height = computeHeight();
        this.bounds.withPosition(x(), fY).withSize(f, height);
        for (int i = 0; i < this.rows.size(); i++) {
            OrderedListRow<E> orderedListRow = this.rows.get(i);
            float f3 = fY + (i * 26.0f);
            float fClamp = f3;
            if (this.dragging && orderedListRow.value() == this.draggedValue) {
                fClamp = clamp(this.dragMouseY, fY, (fY + height) - ROW_HEIGHT);
            }
            orderedListRow.rowRect().withPosition(x(), f3).withSize(f, ROW_HEIGHT);
            orderedListRow.grabRect().withPosition(x(), f3).withSize(24.0f, ROW_HEIGHT);
            float fX = x();
            boolean lifted = this.dragging && orderedListRow.value() == this.draggedValue;
            if (lifted) {
                MenuStyle.drawPopup(drawCtx, x() - 4.0f, fClamp - 2.0f, f + 8.0f, ROW_HEIGHT + 4.0f, 6.0f);
            } else if (i > 0) {
                drawCtx.hairline(fX, f3 - (26.0f - ROW_HEIGHT) / 2.0f, f,
                        MenuStyle.outline(colorStack));
            }
            MenuStyle.drawDragHandle(drawCtx, fX + 1.0f, (fClamp + PADDING) - ROW_GAP + ICON_SIZE / 2.0f,
                    colorStack.computeColor(palette.text().tone(500).argb()));
            drawCtx.text(this.font, this.nameFunction.apply(orderedListRow.value()).effective(), fontSize, fX + ICON_SIZE + 4.0f, (fClamp + PADDING) - (this.font.getHeight(ICON_SIZE) / 2.0f), colorStack.interpolate(colorStack.computeColor(palette.text().tone(100).argb()), colorStack.computeColor(palette.text().tone(100).argb()), orderedListRow.toggleAnimation()));
            float fX2 = (x() + f) - SWITCH_WIDTH;
            float f4 = (fClamp + PADDING) - 7.5f;
            orderedListRow.toggleSwitch().setPosition(fX2, f4);
            orderedListRow.toggleSwitch().setClickableArea(x(), f4, f, SWITCH_HEIGHT);
            orderedListRow.toggleSwitch().render(drawCtx);
        }
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean zHandleInput = super.handleInput(inputEventContext, z);
        if (zHandleInput || !visible()) {
            return zHandleInput;
        }
        InputEvent inputEvent = inputEventContext.inputEvent();
        if (inputEvent instanceof MouseButtonInput mouseButtonInput) {
            if (mouseButtonInput.button() != 0) {
                return false;
            }
            MouseButtonAction action = mouseButtonInput.action();
            if (action.press() && !z) {
                for (int i = 0; i < this.rows.size(); i++) {
                    OrderedListRow<E> orderedListRow = this.rows.get(i);
                    WidgetBounds grabRect = orderedListRow.grabRect();
                    if (inputEventContext.inArea(grabRect.x(), grabRect.y(), grabRect.width(), grabRect.height())) {
                        startDrag(inputEventContext.logicalMousePosition().y(), i, orderedListRow.value());
                        return true;
                    }
                }
            }
            if (action.release() && this.dragging) {
                endDrag();
                return true;
            }
        }
        if ((inputEventContext.inputEvent() instanceof CursorMoveInput) && this.dragging) {
            updateDrag(inputEventContext.logicalMousePosition().y());
            return true;
        }
        if (this.dragging) {
            return false;
        }
        boolean z2 = z;
        for (OrderedListRow<E> row : this.rows) {
            if (row.toggleSwitch().handleInput(inputEventContext, z2)) {
                z2 = true;
            }
        }
        return z2;
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.visibilitySupplier != null) {
            visible(Boolean.TRUE.equals(this.visibilitySupplier.get()));
        }
        if (visible()) {
            rebuildRows();
            for (OrderedListRow<E> orderedListRow : this.rows) {
                orderedListRow.toggleSwitch().animation(weightedEngine);
                orderedListRow.toggleAnimation().state(this.selectedSupplier.get().contains(orderedListRow.value()));
                orderedListRow.toggleAnimation().animate(weightedEngine);
            }
            this.highlightAnimation.animate(weightedEngine);
            super.animation(weightedEngine);
        }
    }

    @Override
    public void handleClose() {
        endDrag();
        super.handleClose();
    }

    @Override
    public float height() {
        rebuildRows();
        return computeHeight();
    }

    public void rebuildRows() {
        List<E> list = this.orderSupplier.get();
        if (list == null) {
            return;
        }
        if (!this.currentOrder.equals(list)) {
            this.currentOrder.clear();
            this.currentOrder.addAll(list);
        }
        this.rowCache.keySet().retainAll(new HashSet<>(this.currentOrder));
        this.rows.clear();
        for (E item : this.currentOrder) {
            this.rows.add(this.rowCache.computeIfAbsent(item, this::createRow));
        }
    }

    public OrderedListRow<E> createRow(E e) {
        ToggleSwitch toggleSwitch = new ToggleSwitch(SWITCH_WIDTH, SWITCH_HEIGHT, () -> this.selectedSupplier.get().contains(e));
        ToggleAnimator animator = new ToggleAnimator(220, Easings.EASE_IN_OUT_CUBIC);
        animator.force(this.selectedSupplier.get().contains(e));
        OrderedListRow<E> orderedListRow = new OrderedListRow<>(e, toggleSwitch, animator);
        toggleSwitch.runnable(() -> {
            boolean z = !this.selectedSupplier.get().contains(e);
            this.toggleConsumer.accept(e);
            orderedListRow.toggleAnimation().state(z);
        });
        return orderedListRow;
    }

    public float computeHeight() {
        int size = this.rows.size();
        if (size == 0) {
            return 0.0f;
        }
        return (ROW_HEIGHT * size) + (ROW_GAP * Math.max(0, size - 1));
    }

    public void startDrag(float f, int i, E e) {
        this.dragging = true;
        this.draggedIndex = i;
        this.draggedValue = e;
        this.dragOffset = f - (this.bounds.y() + (i * 26.0f));
        this.dragMouseY = f - this.dragOffset;
    }

    public void updateDrag(float f) {
        float fY = this.bounds.y();
        float fY2 = (this.bounds.y() + this.bounds.height()) - ROW_HEIGHT;
        if (fY2 < fY) {
            fY2 = fY;
        }
        this.dragMouseY = clamp(f - this.dragOffset, fY, fY2);
        int targetIndex = indexFromY(f);
        if (targetIndex == this.draggedIndex || targetIndex < 0 || targetIndex >= this.rows.size()) {
            return;
        }
        moveRow(this.draggedIndex, targetIndex);
        this.moveConsumer.accept(this.draggedIndex, targetIndex);
        this.draggedIndex = targetIndex;
    }

    public int indexFromY(float f) {
        float fY = f - this.bounds.y();
        if (fY <= 0.0f) {
            return 0;
        }
        return Math.max(0, Math.min(this.rows.size() - 1, (int) Math.floor(fY / 26.0f)));
    }

    public void moveRow(int i, int i2) {
        if (i == i2 || i < 0 || i2 < 0 || i >= this.rows.size() || i2 >= this.rows.size()) {
            return;
        }
        this.rows.add(i2, this.rows.remove(i));
        this.currentOrder.add(i2, this.currentOrder.remove(i));
    }

    public void endDrag() {
        this.dragging = false;
        this.draggedIndex = -1;
        this.draggedValue = null;
    }

    public float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }

    public OrderedListSettingElement<E> visibleSupplier(Supplier<Boolean> supplier) {
        this.visibilitySupplier = supplier;
        return this;
    }
}
