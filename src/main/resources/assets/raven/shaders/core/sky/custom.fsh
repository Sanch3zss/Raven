#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec3 direction;

out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * vec3(443.897, 441.423, 437.195));
    p += dot(p, p.yzx + 19.19);
    return fract((p.x + p.y) * p.z);
}

float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float noise(vec2 p) {
    vec2 cell = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(cell);
    float b = hash(cell + vec2(1.0, 0.0));
    float c = hash(cell + vec2(0.0, 1.0));
    float d = hash(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

vec3 gradient(float position) {
    return texture(Sampler0, vec2(clamp(position, 0.002, 0.998), 0.5)).rgb;
}

vec3 stars(vec3 dir, float time) {
    vec3 point = dir * 150.0;
    vec3 cell = floor(point);
    float seed = hash(cell);
    if (seed < 0.975) return vec3(0.0);
    vec3 center = normalize(cell + 0.2 + 0.6 * vec3(hash(cell + 1.3), hash(cell + 2.7), hash(cell + 4.1))) * 150.0;
    float size = 0.18 + 0.22 * hash(cell + 6.9);
    float glow = smoothstep(size, 0.0, length(point - center));
    float twinkle = 0.75 + 0.25 * sin(time * (1.0 + seed * 2.0) + seed * 60.0);
    return vec3(glow * twinkle);
}

void main() {
    vec3 dir = normalize(direction);
    if (dir.y < 0.0) {
        fragColor = vec4(FogColor.rgb, 1.0);
        return;
    }
    float time = ModelOffset.x;
    float night = clamp(ModelOffset.y, 0.0, 1.0);
    float up = dir.y;

    vec3 sky = ColorModulator.rgb;
    if (night > 0.001) {
        vec2 plane = dir.xz / (up + 0.35);
        vec2 drift = vec2(time * 0.006, time * 0.004);
        vec2 warp = vec2(noise(plane * 1.6 + drift), noise(plane * 1.6 - drift + 5.2));
        float cloud = noise(plane * 2.2 + warp * 1.4 + drift) * 0.65 + noise(plane * 5.0 - warp + drift * 2.0) * 0.35;
        float nebula = smoothstep(0.3, 0.8, cloud);
        float shimmer = 0.5 + 0.5 * sin(cloud * 5.0 + warp.x * 3.0 + time * 0.12);
        vec3 space = mix(gradient(0.0) * 0.1, gradient(shimmer) * 0.85, nebula);
        space += stars(dir, time);
        sky = mix(sky, space, night);
    }

    vec3 color = mix(sky, FogColor.rgb, exp(-up * 8.0));
    fragColor = vec4(color, 1.0);
}
