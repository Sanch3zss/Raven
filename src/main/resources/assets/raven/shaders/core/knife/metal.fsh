#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <raven:viewmodel_light.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec3 ambientBase;
in vec3 ambientSky;
in vec3 ambientBlock;
in vec4 overlayColor;
in vec2 texCoord0;
in vec3 viewPosition;
in vec3 viewNormal;

out vec4 fragColor;

float luminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

float hue(vec3 color) {
    float high = max(color.r, max(color.g, color.b));
    float low = min(color.r, min(color.g, color.b));
    float range = max(high - low, 0.0001);
    float h;
    if (high == color.r) {
        h = mod((color.g - color.b) / range, 6.0);
    } else if (high == color.g) {
        h = (color.b - color.r) / range + 2.0;
    } else {
        h = (color.r - color.g) / range + 4.0;
    }
    return h / 6.0;
}

vec3 shadingNormal(vec3 view) {
    vec3 normal = normalize(viewNormal) * (gl_FrontFacing ? 1.0 : -1.0);
    return normalize(normal + view * max(0.15 - dot(normal, view), 0.0));
}

vec4 similarTap(vec2 uv, vec3 center) {
    vec4 tap = texture(Sampler0, uv);
    float weight = tap.a * (1.0 - smoothstep(0.06, 0.2, distance(tap.rgb, center)));
    return vec4(tap.rgb * weight, weight);
}

vec4 averaged(vec2 uv) {
    vec2 pixel = 1.0 / vec2(textureSize(Sampler0, 0));
    vec4 center = texture(Sampler0, uv);
    vec4 sum = vec4(center.rgb * center.a, center.a) * 2.0;
    sum += similarTap(uv + vec2(pixel.x, 0.0), center.rgb);
    sum += similarTap(uv - vec2(pixel.x, 0.0), center.rgb);
    sum += similarTap(uv + vec2(0.0, pixel.y), center.rgb);
    sum += similarTap(uv - vec2(0.0, pixel.y), center.rgb);
    return vec4(sum.rgb / max(sum.a, 0.001), sum.a / 6.0);
}

void main() {
    vec4 texel = texture(Sampler0, texCoord0);
    vec3 tint = averaged(texCoord0).rgb;
    vec3 view = normalize(-viewPosition);
    vec3 normal = shadingNormal(view);
    vec3 smoothNormal = normalize(viewNormal);
    float grazing = smoothstep(0.0, 0.35, abs(dot(smoothNormal, view)));
    if (texel.a < ALPHA_CUTOUT) {
        discard;
    }

    vec3 albedo = pow(texel.rgb, vec3(2.2));
#ifdef ATLAS
    vec4 detail = texture(Sampler0, texCoord0 + vec2(0.5, 0.0));
    float metal = detail.g;
    float roughness = max(detail.r, 0.05);
    float saturation = 0.0;
    float warm = 0.0;
    float colored = 0.0;
    float soft = 0.0;
#else
    float brightest = max(tint.r, max(tint.g, tint.b));
    float saturation = (brightest - min(tint.r, min(tint.g, tint.b))) / max(brightest, 0.001);
    float lum = luminance(tint);
    float neutral = 1.0 - smoothstep(METAL_SATURATION * 0.4, METAL_SATURATION * 1.2, saturation);
    float metal = smoothstep(METAL_DARK - 0.05, METAL_DARK + 0.25, lum) * neutral;
    float tone = hue(tint);
    float redDistance = min(tone, 1.0 - tone);
    float warm = smoothstep(0.15, 0.5, saturation) * smoothstep(0.0, 0.035, redDistance)
            * (1.0 - smoothstep(0.08, 0.2, redDistance))
            * (1.0 - smoothstep(0.7, 0.95, lum)) * (1.0 - metal);
    float colored = smoothstep(0.35, 0.8, saturation) * (1.0 - warm) * (1.0 - metal);
    float soft = (1.0 - metal) * (1.0 - warm) * (1.0 - colored);

    float dielectricRoughness = mix(0.62, 0.45, warm);
    dielectricRoughness = mix(dielectricRoughness, 0.35, colored);
    dielectricRoughness = mix(dielectricRoughness, 0.75, soft * smoothstep(0.05, 0.35, saturation));
    float roughness = mix(dielectricRoughness, max(BASE_ROUGHNESS, 0.2), metal);
#endif

    ViewmodelLight light = viewmodel_light();
    float nv = clamp(dot(normal, view), 0.0001, 1.0);
    roughness = viewmodel_specular_aa(roughness, smoothNormal);
    float coatRoughness = viewmodel_specular_aa(0.04, smoothNormal);

    vec3 f0 = mix(vec3(0.02), min(albedo * METAL_GAIN, vec3(1.0)), metal);
    vec3 sun = light.color * light.visibility;
    vec3 envSpecular = viewmodel_env_specular(f0, roughness, nv);
    vec3 irradiance = viewmodel_ambient(light, normal, ambientBase, ambientSky, ambientBlock);
    vec3 reflected = viewmodel_reflection(normal, view, roughness);

    vec3 diffuse = albedo * (1.0 - metal) * (sun * max(dot(normal, light.direction), 0.0) + irradiance * (1.0 - envSpecular));
    vec3 specular = sun * viewmodel_direct_specular(f0, max(roughness, 0.06), normal, view, light.direction)
            + viewmodel_environment(light, reflected, roughness, ambientBase, ambientSky, ambientBlock) * envSpecular;
    vec3 color = diffuse + specular * mix(0.3, 1.0, grazing);

    float sheen = soft * smoothstep(0.05, 0.3, saturation) * pow(1.0 - nv, 3.0);
    color += albedo * irradiance * sheen * 0.5;

    float coat = warm * 0.3 + colored * 0.25;
    vec3 coatFresnel = viewmodel_env_specular(vec3(0.04), coatRoughness, nv) * coat * grazing;
    vec3 coatLight = viewmodel_environment(light, reflect(-view, normal), coatRoughness, ambientBase, ambientSky, ambientBlock)
            + sun * viewmodel_direct_specular(vec3(1.0), 0.06, normal, view, light.direction);
    color = color * (1.0 - coatFresnel) + coatLight * coatFresnel;

    color = viewmodel_tonemap(color);

    vec4 result = vec4(color, texel.a) * ColorModulator;
    result.rgb = mix(overlayColor.rgb, result.rgb, overlayColor.a);
    fragColor = apply_fog(result, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
