#version 330 core

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D DepthSampler;
uniform sampler2D SceneSampler;
uniform vec4 Sources[32];
uniform vec3 Colors[32];
uniform int SourceCount;
uniform float Aspect;

void main() {
    float sceneDepth = texture(DepthSampler, texCoord).r;
    vec3 sceneColor = texture(SceneSampler, texCoord).rgb;
    vec3 glow = vec3(0.0);
    for (int i = 0; i < 32; i++) {
        if (i >= SourceCount) break;
        vec4 source = Sources[i];
        if (sceneDepth > source.z + 0.003) continue;
        vec2 delta = (texCoord - source.xy) * vec2(Aspect, 1.0);
        float distanceToCenter = length(delta) / source.w;
        if (distanceToCenter >= 1.0) continue;
        float profile = pow(1.0 - distanceToCenter, 2.0);
        float similarity = dot(normalize(sceneColor + vec3(0.001)),
                normalize(Colors[i] + vec3(0.001)));
        float extraction = smoothstep(0.2, 0.75, similarity)
                * smoothstep(0.02, 0.25, max(max(sceneColor.r, sceneColor.g), sceneColor.b));
        glow += Colors[i] * profile * extraction;
    }
    fragColor = vec4(min(glow, vec3(1.0)), 1.0);
}
