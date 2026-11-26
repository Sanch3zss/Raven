package sanch3zss.raven.render.animation;

public class AnimationStack2 {
    public final AnimationStack stack = new AnimationStack();

    public void begin() {
        this.stack.begin();
    }

    public float animationMultiplier() {
        return this.stack.animation();
    }

    public void push() {
        this.stack.push();
    }

    public void animation(ToggleAnimator animator) {
        this.stack.animation(animator.smoothAnimation());
    }

    public void pop() {
        this.stack.pop();
    }

    public void end() {
        this.stack.end();
    }
}
