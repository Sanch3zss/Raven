package sanch3zss.raven.render.shape;

import java.util.*;

import sanch3zss.raven.render.data.ScissorRegion;

public class ScissorStack {
    public static final int maxSize = 16;
    public final Deque<ScissorRegion> stack = new ArrayDeque(maxSize);

    public void push(int i, int i2, int i3, int i4, float f, float f2, float f3, float f4) {
        if (this.stack.size() >= maxSize) {
            throw new IllegalStateException("Stack overflow");
        }
        ScissorRegion parent = this.stack.peek();
        if (parent != null) {
            int right = Math.min(i + Math.max(0, i3), parent.x + parent.width);
            int top = Math.min(i2 + Math.max(0, i4), parent.y + parent.height);
            i = Math.max(i, parent.x);
            i2 = Math.max(i2, parent.y);
            i3 = Math.max(0, right - i);
            i4 = Math.max(0, top - i2);
        }
        ScissorRegion scissorRegion = new ScissorRegion(i, i2, i3, i4, f, f2, f3, f4);
        this.stack.push(scissorRegion);
        scissorRegion.apply();
    }

    public void pop() {
        if (this.stack.isEmpty()) {
            throw new IllegalStateException("Stack underflow");
        }
        this.stack.pop();
        currentScissor().apply();
    }

    public void end() {
        this.stack.pop();
        if (!this.stack.isEmpty()) {
            throw new IllegalStateException("Stack overflow");
        }
    }

    public ScissorRegion currentScissor() {
        ScissorRegion peek = this.stack.peek();
        if (peek == null) {
            throw new IllegalStateException("Stack underflow");
        }
        return peek;
    }
}
