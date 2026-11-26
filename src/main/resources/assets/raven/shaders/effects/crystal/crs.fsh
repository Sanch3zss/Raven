#version 330 core

in vec3 smoothNormal;
in vec3 viewPosition;
in float vertexAlpha;
in vec2 texCoord;
in vec4 vertexColor;

uniform int Mode;
uniform sampler2D Sampler0;
uniform vec3 BaseColor;
uniform float Time;
uniform float Roundness;
uniform float HighlightStrength;
uniform float GlowIntensity;
uniform float GlowRadius;
uniform float PulseSpeed;
uniform float BloomStrength;

out vec4 fragColor;

float alphaAt(vec2 offset) {
    return texture(Sampler0, texCoord + offset).a;
}

void surfacePass() {
    vec3 roundedNormal = normalize(smoothNormal);
    vec3 flatNormal = normalize(cross(dFdx(viewPosition), dFdy(viewPosition)));
    if (dot(flatNormal, roundedNormal) < 0.0) flatNormal = -flatNormal;
    vec3 normal = normalize(mix(flatNormal, roundedNormal, Roundness));
    vec3 viewDirection = normalize(-viewPosition);
    vec3 lightDirection = normalize(vec3(-0.38, 0.62, 0.69));
    float diffuse = clamp(dot(normal, lightDirection) * 0.5 + 0.5, 0.0, 1.0);
    float facing = clamp(dot(normal, viewDirection), 0.0, 1.0);
    float fresnel = pow(1.0 - abs(dot(normal, viewDirection)), 2.4);
    vec3 color = BaseColor * (0.80 + diffuse * 0.34 + facing * 0.08);
    float facet = smoothstep(0.06, 0.62, dot(flatNormal, lightDirection));
    float rounded = smoothstep(-0.20, 0.72, dot(roundedNormal, lightDirection));
    color = mix(color, vec3(1.0), clamp(facet * (0.28 + rounded * 0.24) * HighlightStrength, 0.0, 0.52));
    color += BaseColor * fresnel * 0.15;
    color *= 0.985 + 0.025 * (0.5 + 0.5 * sin(Time * 0.45 + roundedNormal.x * 2.2 + roundedNormal.y * 1.4));
    fragColor = vec4(color, vertexAlpha);
}

void glowPass() {
    vec4 source = texture(Sampler0, texCoord);
    float nearStep = 0.0105 * GlowRadius;
    float wideStep = 0.0275 * GlowRadius;
    float nearBlur = (alphaAt(vec2(nearStep, 0)) + alphaAt(vec2(-nearStep, 0))
        + alphaAt(vec2(0, nearStep)) + alphaAt(vec2(0, -nearStep))
        + alphaAt(vec2(nearStep)) + alphaAt(vec2(-nearStep, nearStep))
        + alphaAt(vec2(nearStep, -nearStep)) + alphaAt(vec2(-nearStep))) * 0.125;
    float wideBlur = (alphaAt(vec2(wideStep, 0)) + alphaAt(vec2(-wideStep, 0))
        + alphaAt(vec2(0, wideStep)) + alphaAt(vec2(0, -wideStep))) * 0.25;
    float core = pow(source.a, 0.72);
    float halo = max(nearBlur * 0.82, wideBlur * 0.56);
    float pulse = 0.95 + 0.05 * sin(Time * PulseSpeed);
    float alpha = clamp((core * 0.58 + halo * BloomStrength) * GlowIntensity * vertexColor.a * pulse, 0.0, 1.0);
    if (alpha < 0.002) discard;
    fragColor = vec4(vertexColor.rgb * (1.0 + core * 0.24 + halo * 0.30), alpha);
}

void main() {
    if (Mode == 0) surfacePass();
    else glowPass();
}
