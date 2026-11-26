#version 330 core

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D uTexture;
uniform vec2 uHalfTexelSize;
uniform float uOffset;

void main() {
    fragColor = (
        texture(uTexture, texCoord) * 4.0 +
        texture(uTexture, texCoord - uHalfTexelSize.xy * uOffset) +
        texture(uTexture, texCoord + uHalfTexelSize.xy * uOffset) +
        texture(uTexture, texCoord + vec2(uHalfTexelSize.x, -uHalfTexelSize.y) * uOffset) +
        texture(uTexture, texCoord - vec2(uHalfTexelSize.x, -uHalfTexelSize.y) * uOffset)
    ) / 8.0;
    fragColor.a = 1.0;
}
