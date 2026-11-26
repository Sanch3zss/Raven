#version 330 core

in vec2 texCoord;
out vec4 fragColor;

uniform float Time;
uniform vec4 FirstColor;
uniform vec4 SecondColor;
uniform float Opacity;

const vec3 MOD3 = vec3(0.1031, 0.11369, 0.13787);

vec3 hash33(vec3 p) {
    p = fract(p * MOD3);
    p += dot(p, p.yxz + 19.19);
    return -1.0 + 2.0 * fract(vec3(
        (p.x + p.y) * p.z,
        (p.x + p.z) * p.y,
        (p.y + p.z) * p.x
    ));
}

float simplexNoise(vec3 p) {
    const float K1 = 0.333333333;
    const float K2 = 0.166666667;

    vec3 i = floor(p + (p.x + p.y + p.z) * K1);
    vec3 d0 = p - (i - (i.x + i.y + i.z) * K2);

    vec3 e = step(vec3(0.0), d0 - d0.yzx);
    vec3 i1 = e * (1.0 - e.zxy);
    vec3 i2 = 1.0 - e.zxy * (1.0 - e);

    vec3 d1 = d0 - (i1 - K2);
    vec3 d2 = d0 - (i2 - 2.0 * K2);
    vec3 d3 = d0 - (1.0 - 3.0 * K2);

    vec4 h = max(0.6 - vec4(dot(d0, d0), dot(d1, d1),
                             dot(d2, d2), dot(d3, d3)), 0.0);
    vec4 n = h * h * h * h * vec4(
        dot(d0, hash33(i)), dot(d1, hash33(i + i1)),
        dot(d2, hash33(i + i2)), dot(d3, hash33(i + 1.0))
    );
    return dot(vec4(31.316), n);
}

void main() {
    vec2 uv = (texCoord - 0.5) * 2.0;
    float radius = length(uv);
    float footprint = max(length(dFdx(uv)), length(dFdy(uv)));
    float detail = 1.0 - smoothstep(0.025, 0.10, footprint);
    float angle = atan(uv.y, uv.x);
    float am = abs(sin(angle) - 0.5) * 0.25;

    float m1 = clamp(0.1 / max(smoothstep(0.0, 1.75, radius), 0.0001), 0.0, 1.0);
    float m2 = clamp(0.1 / max(1.0 - smoothstep(0.0, 0.42, radius), 0.0001), 0.0, 1.0);
    float s1 = simplexNoise(vec3(uv * 2.0, 1.0 + Time * 0.525))
               * max(1.0 - radius * 1.75, 0.0) * detail + 0.9;
    float s2 = simplexNoise(vec3(uv, 15.0 + Time * 0.525))
               * max(radius, 0.025) * detail + 1.25;
    float s3 = simplexNoise(vec3(vec2(am, am * 100.0 + Time * 3.0) * 0.15,
                                 30.0 + Time * 0.525))
               * max(radius, 0.25) * detail + 1.5;
    s3 *= smoothstep(0.0, 0.3345, radius);
    float sh = smoothstep(0.15, 0.35, radius);
    float originalGlow = max(m1 * m1 * m2 * s1 * s2 * s3 * (1.0 - radius) * sh, 0.0);

    float alpha = clamp(pow(originalGlow, 0.85) * 0.88 * Opacity, 0.0, 1.0);
    if (alpha < 0.003) discard;

    float gradient = 0.5 + 0.5 * cos(angle - Time * 0.38);
    vec4 color = mix(FirstColor, SecondColor, gradient);
    float brightness = min(0.85 + originalGlow * 0.55, 1.55);
    fragColor = vec4(color.rgb * brightness, alpha * color.a);
}
