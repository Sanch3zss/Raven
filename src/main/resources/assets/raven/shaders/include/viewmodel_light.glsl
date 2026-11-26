#version 330

uniform sampler2D Sampler3;

struct ViewmodelLight {
    vec3 direction;
    vec3 color;
    float visibility;
    vec3 sky;
    vec3 glow;
    float glowStrength;
    vec3 up;
};

float viewmodel_luminance(vec3 color) {
    return dot(color, vec3(0.2125, 0.7154, 0.0721));
}

ViewmodelLight viewmodel_light() {
    vec4 direction = texelFetch(Sampler3, ivec2(0, 0), 0);
    vec4 color = texelFetch(Sampler3, ivec2(1, 0), 0);
    vec4 sky = texelFetch(Sampler3, ivec2(2, 0), 0);
    vec4 glow = texelFetch(Sampler3, ivec2(3, 0), 0);
    ViewmodelLight light;
    light.direction = normalize(mat3(ModelViewMat) * (direction.xyz * 2.0 - 1.0));
    light.visibility = direction.a;
    light.color = color.rgb * color.rgb * 4.0;
    light.sky = pow(sky.rgb, vec3(2.2));
    light.glow = pow(glow.rgb, vec3(2.2)) * sky.a;
    light.glowStrength = glow.a;
    light.up = normalize(ModelViewMat[1].xyz);
    return light;
}

vec3 viewmodel_ambient(ViewmodelLight light, vec3 direction, vec3 base, vec3 sky, vec3 block) {
    float hemisphere = dot(direction, light.up) * 0.5 + 0.5;
    vec3 tint = min(light.sky / max(viewmodel_luminance(light.sky), 0.02), vec3(2.0));
    float sunFill = 1.0 - 0.45 * light.visibility * smoothstep(0.0, 0.6, viewmodel_luminance(light.color));
    vec3 fromSky = sky * mix(vec3(0.3), 1.45 * mix(vec3(1.0), tint, 0.35), hemisphere) * sunFill;
    vec3 fromBlocks = block * mix(0.8, 1.1, hemisphere);
    return base + fromSky + fromBlocks;
}

vec3 viewmodel_environment(ViewmodelLight light, vec3 direction, float roughness, vec3 base, vec3 sky, vec3 block) {
    vec3 world = direction * mat3(ModelViewMat);
    vec3 radiance = viewmodel_ambient(light, direction, base, sky, block);
    float sharp = 1.0 - roughness;
    float horizon = 1.0 - smoothstep(0.0, 0.25 + roughness * 0.6, abs(world.y - 0.05));
    vec3 fog = pow(FogColor.rgb, vec3(2.2));
    vec3 fogTint = min(fog / max(viewmodel_luminance(fog), 0.02), vec3(2.0));
    radiance *= mix(vec3(1.0), fogTint, horizon * 0.3) * (1.0 + horizon * 0.35 * sharp);
    float azimuth = atan(world.z, world.x);
    float variation = 0.3 * sin(azimuth * 2.0 + 0.6) + 0.15 * sin(azimuth * 5.0 - 1.1) * sharp
            + 0.08 * sin(azimuth * 9.0 + 2.3) * sharp * sharp;
    radiance *= 1.0 + variation * sharp * (1.0 - smoothstep(0.4, 0.95, abs(world.y)));
    vec3 lightWorld = light.direction * mat3(ModelViewMat);
    vec2 towardLight = normalize(lightWorld.xz + vec2(0.0001));
    float facing = max(dot(normalize(world.xz + vec2(0.0001)), towardLight), 0.0);
    float skyOpen = clamp(viewmodel_luminance(sky) * 2.0, 0.0, 1.0);
    radiance += light.glow * light.glowStrength * pow(facing, mix(6.0, 1.5, roughness)) * horizon * skyOpen * 1.5;
    return radiance;
}

float viewmodel_specular_aa(float roughness, vec3 smoothNormal) {
    vec3 across = dFdx(smoothNormal);
    vec3 down = dFdy(smoothNormal);
    return max(roughness, pow(clamp(max(dot(across, across), dot(down, down)), 0.0, 1.0), 0.333));
}

vec3 viewmodel_reflection(vec3 normal, vec3 view, float roughness) {
    float a = roughness * roughness;
    float f = clamp(1.0 - a, 0.0, 1.0);
    return normalize(mix(normal, reflect(-view, normal), f * (sqrt(f) + a)));
}

vec3 viewmodel_direct_specular(vec3 f0, float roughness, vec3 normal, vec3 view, vec3 lightDirection) {
    vec3 halfway = normalize(lightDirection + view);
    float nl = max(dot(normal, lightDirection), 0.0);
    float nv = max(dot(normal, view), 0.0);
    float nh = dot(normal, halfway);
    float a = roughness * roughness;
    float d = a / (nh * nh * (a * a - 1.0) + 1.0);
    float k = (roughness + 1.0) * (roughness + 1.0) * 0.125;
    float visibility = 1.0 / (4.0 * (nl * (1.0 - k) + k) * (nv * (1.0 - k) + k));
    vec3 fresnel = f0 + (1.0 - f0) * pow(max(1.0e-6, 1.0 - max(dot(lightDirection, halfway), 0.0)), 5.0);
    return fresnel * d * d * visibility * nl;
}

vec3 viewmodel_env_specular(vec3 f0, float roughness, float nv) {
    vec4 r = roughness * vec4(-1.0, -0.0275, -0.572, 0.022) + vec4(1.0, 0.0425, 1.04, -0.04);
    float a004 = min(r.x * r.x, exp2(-9.28 * nv)) * r.x + r.y;
    vec2 brdf = vec2(-1.04, 1.04) * a004 + r.zw;
    vec3 single = f0 * brdf.x + brdf.y;
    float ess = brdf.x + brdf.y;
    vec3 average = f0 + (1.0 - f0) / 21.0;
    return single + single * average / (1.0 - average * (1.0 - ess)) * (1.0 - ess);
}

vec3 viewmodel_tonemap(vec3 color) {
    color = max(color, vec3(0.0));
    float peak = max(color.r, max(color.g, color.b));
    if (peak > 0.6) {
        color *= (0.6 + 0.4 * (1.0 - exp(-(peak - 0.6) / 0.4))) / peak;
    }
    return pow(color, vec3(1.0 / 2.2));
}
