#version 330 core

in vec2 texCoord;

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;
uniform int LensCount;
uniform float Aspect;
uniform float Strength;
uniform int RespectDepth;
uniform vec4 Lens0;
uniform vec4 Lens1;
uniform vec4 Lens2;
uniform vec4 Lens3;
uniform vec4 Lens4;
uniform vec4 Lens5;
uniform vec4 Lens6;
uniform vec4 Lens7;
uniform vec4 Lens8;
uniform vec4 Lens9;
uniform vec4 Lens10;
uniform vec4 Lens11;
uniform vec4 Lens12;
uniform vec4 Lens13;
uniform vec4 Lens14;
uniform vec4 Lens15;
uniform vec4 Lens16;
uniform vec4 Lens17;

out vec4 fragColor;

const int MAX_LENSES = 18;
const float PULL = 0.85;
const float CHROMA = 0.03;

void main() {
    vec4 lens[MAX_LENSES] = vec4[MAX_LENSES](
        Lens0, Lens1, Lens2, Lens3, Lens4, Lens5,
        Lens6, Lens7, Lens8, Lens9, Lens10, Lens11,
        Lens12, Lens13, Lens14, Lens15, Lens16, Lens17
    );
    float sceneDepth = texture(DepthSampler, texCoord).r;
    vec2 distortion = vec2(0.0);
    float accumulatedProfile = 0.0;
    float strongestProfile = 0.0;

    for (int i = 0; i < MAX_LENSES; i++) {
        if (i >= LensCount) break;
        vec4 current = lens[i];
        if (current.w <= 0.0) continue;

        vec2 delta = (texCoord - current.xy) * vec2(Aspect, 1.0);
        float distanceToCenter = length(delta);
        float normalizedDistance = distanceToCenter / current.w;
        if (normalizedDistance >= 1.0 || distanceToCenter < 0.00001) continue;
        if (RespectDepth != 0 && sceneDepth > current.z + 0.0005) continue;

        float profile = normalizedDistance * (1.0 - normalizedDistance)
            * (1.0 - normalizedDistance) * 6.75;
        vec2 direction = delta / distanceToCenter;
        distortion += direction / vec2(Aspect, 1.0) * (profile * current.w);
        accumulatedProfile += profile;
        strongestProfile = max(strongestProfile, profile);
    }

    vec2 offset = -distortion / max(accumulatedProfile, 0.00001)
        * strongestProfile * PULL * Strength;

    if (dot(offset, offset) < 0.000000000001) {
        fragColor = texture(SceneSampler, texCoord);
        return;
    }

    vec2 redUv = clamp(texCoord + offset * (1.0 - CHROMA), vec2(0.0), vec2(1.0));
    vec2 greenUv = clamp(texCoord + offset, vec2(0.0), vec2(1.0));
    vec2 blueUv = clamp(texCoord + offset * (1.0 + CHROMA), vec2(0.0), vec2(1.0));
    vec4 source = texture(SceneSampler, greenUv);
    fragColor = vec4(
        texture(SceneSampler, redUv).r,
        source.g,
        texture(SceneSampler, blueUv).b,
        source.a
    );
}
