#version 330 core

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D GlowSampler;

void main() {
    fragColor = vec4(texture(GlowSampler, texCoord).rgb * 0.85, 1.0);
}
