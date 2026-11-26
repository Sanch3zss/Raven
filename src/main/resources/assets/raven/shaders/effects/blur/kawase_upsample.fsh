#version 330 core

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D uTexture;
uniform vec2 uHalfTexelSize;
uniform float uOffset;

void main() {
    fragColor = (
        texture(uTexture, texCoord + vec2(-uHalfTexelSize.x * 2.0, 0.0) * uOffset) +
        texture(uTexture, texCoord + vec2(-uHalfTexelSize.x, uHalfTexelSize.y) * uOffset) * 2.0 +
        texture(uTexture, texCoord + vec2(0.0, uHalfTexelSize.y * 2.0) * uOffset) +
        texture(uTexture, texCoord + uHalfTexelSize * uOffset) * 2.0 +
        texture(uTexture, texCoord + vec2(uHalfTexelSize.x * 2.0, 0.0) * uOffset) +
        texture(uTexture, texCoord + vec2(uHalfTexelSize.x, -uHalfTexelSize.y) * uOffset) * 2.0 +
        texture(uTexture, texCoord + vec2(0.0, -uHalfTexelSize.y * 2.0) * uOffset) +
        texture(uTexture, texCoord - uHalfTexelSize * uOffset) * 2.0
    ) / 12.0;
    fragColor.a = 1.0;
}
