#version 330

#moj_import <minecraft:globals.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;
in vec2 texCoord;
in vec4 tintColor;
in float gradientPhase;

out vec4 fragColor;

void main() {
    vec4 textureColor = texture(Sampler0, texCoord);
    if (textureColor.a < ALPHA_CUTOUT) {
        discard;
    }
    vec2 screenUv = gl_FragCoord.xy / ScreenSize;
    vec2 halfTexel = 0.5 / vec2(textureSize(Sampler2, 0));
    vec2 blurUv = clamp(screenUv, halfTexel, vec2(1.0) - halfTexel);
    vec3 blurred = texture(Sampler2, blurUv).rgb;
    vec4 glassTint = tintColor;
#ifdef BLOCK_TINT
    vec4 firstTint = vec4(BLOCK_TINT_RED, BLOCK_TINT_GREEN, BLOCK_TINT_BLUE, BLOCK_TINT_ALPHA);
    vec4 secondTint = vec4(BLOCK_TINT_RED2, BLOCK_TINT_GREEN2, BLOCK_TINT_BLUE2, BLOCK_TINT_ALPHA2);
#ifdef KNIFE_TINT
    float spatialPhase = (screenUv.x - 0.5) * 3.5 - (screenUv.y - 0.5) * 0.7;
#else
    vec2 texturePixel = texCoord * vec2(textureSize(Sampler0, 0));
    float spatialPhase = texturePixel.x * 0.055 + texturePixel.y * 0.035;
#endif
#ifdef KNIFE_TINT
    float phase = gradientPhase * 6.2831853 + spatialPhase;
#else
    float phase = (gradientPhase + spatialPhase) * 6.2831853;
#endif
    glassTint *= mix(firstTint, secondTint, 0.5 - 0.5 * cos(phase));
#endif
#ifdef KNIFE_TINT
    fragColor = vec4(mix(blurred, textureColor.rgb, TEXTURE_MIX * 0.45) * glassTint.rgb, textureColor.a);
#else
    fragColor = vec4(mix(blurred, textureColor.rgb, TEXTURE_MIX), textureColor.a) * glassTint;
#endif
}
