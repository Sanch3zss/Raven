#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;
#ifdef ATLAS_GRID
uniform sampler2D Sampler1;
#endif

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec2 surfaceUv;
in vec2 flowUv;

out vec4 fragColor;

const float TAU = 6.2831853;

vec3 gradient(float position) {
    return texture(Sampler0, vec2(1.0 - abs(fract(position) * 2.0 - 1.0), 0.5)).rgb;
}

void main() {
#ifdef ATLAS_GRID
    vec2 grid = vec2(textureSize(Sampler1, 0)) / 16.0;
#else
    vec2 grid = vec2(GRID_X, GRID_Y) / 16.0;
#endif
    float cycle = fract(TextureMat[3][1]);
    vec3 color;
    float intensity;

#ifdef PIXEL
    vec2 cell = floor(surfaceUv * grid * 16.0);
    float diagonal = (cell.x + cell.y) / 16.0;
    float wave = fract(diagonal * 0.5 - cycle * 4.0);
    float head = exp(-pow((wave - 0.3) * 7.0, 2.0));
    float tail = exp(-pow((wave - 0.75) * 10.0, 2.0)) * 0.45;
    intensity = 0.12 + (head + tail) * 0.9;
    color = gradient(diagonal * 0.25 + cycle * 2.0);
#else
    vec2 p = surfaceUv * grid;
    vec2 q = p + 0.12 * vec2(sin(p.y * 6.0 + TAU * cycle * 3.0), cos(p.x * 5.0 - TAU * cycle * 2.0));
    float silk = pow(0.5 + 0.5 * sin((q.x + q.y) * 4.5 - TAU * cycle * 5.0), 4.0);
    float ripple = pow(0.5 + 0.5 * sin((q.x - q.y * 0.6) * 6.0 + TAU * cycle * 4.0), 6.0);
    float sweep = fract((p.x * 0.8 + p.y) * 0.35 - cycle * 2.0);
    float streak = exp(-pow((sweep - 0.5) * 8.0, 2.0));
    intensity = 0.15 + silk * 0.6 + ripple * 0.3 + streak * 0.6;
    color = gradient(q.x * 0.5 + q.y * 0.3 + cycle);
#endif

    float fade = (1.0 - total_fog_value(sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart,
            FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd)) * GlintAlpha;
    vec3 light = color * intensity * STRENGTH;
    light /= max(1.0, max(light.r, max(light.g, light.b)));
    fragColor = vec4(sqrt(light * fade), 1.0);
}
