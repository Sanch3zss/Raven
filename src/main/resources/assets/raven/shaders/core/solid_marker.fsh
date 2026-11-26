#version 330

uniform sampler2D Sampler0;

in vec2 texCoord;
in vec4 tintColor;

out vec4 fragColor;

void main() {
    float coverage = texture(Sampler0, texCoord).a;
    if (coverage < 0.80) {
        discard;
    }
    fragColor = vec4(tintColor.rgb, tintColor.a * smoothstep(0.80, 0.98, coverage));
}
