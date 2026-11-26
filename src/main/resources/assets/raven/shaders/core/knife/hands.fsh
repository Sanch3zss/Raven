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

vec3 detailNormal(vec3 surface, vec3 encoded) {
    vec2 uv = texCoord0 * vec2(2.0, 1.0);
    vec3 dp1 = dFdx(viewPosition);
    vec3 dp2 = dFdy(viewPosition);
    vec2 duv1 = dFdx(uv);
    vec2 duv2 = dFdy(uv);
    vec3 dp2perp = cross(dp2, surface);
    vec3 dp1perp = cross(surface, dp1);
    vec3 tangent = dp2perp * duv1.x + dp1perp * duv2.x;
    vec3 bitangent = dp2perp * duv1.y + dp1perp * duv2.y;
    float scale = inversesqrt(max(max(dot(tangent, tangent), dot(bitangent, bitangent)), 1.0e-12));
    vec3 local = encoded * 2.0 - 1.0;
    local.xy *= DETAIL;
    vec3 mapped = mat3(tangent * scale, -bitangent * scale, surface) * local;
    return dot(mapped, mapped) < 1.0e-8 ? surface : normalize(mapped);
}

void main() {
    vec4 texel = texture(Sampler0, texCoord0);
    if (texel.a < 0.1) {
        discard;
    }
    vec4 detail = texture(Sampler0, texCoord0 + vec2(0.5, 0.0));
    vec3 view = normalize(-viewPosition);
    vec3 smoothNormal = normalize(viewNormal) * (gl_FrontFacing ? 1.0 : -1.0);
    vec3 normal = detailNormal(smoothNormal, detail.rgb);
    normal = normalize(normal + view * max(0.1 - dot(normal, view), 0.0));
    ViewmodelLight light = viewmodel_light();

    vec3 albedo = pow(texel.rgb, vec3(2.2));
    float roughness = viewmodel_specular_aa(clamp(detail.a * ROUGHNESS, 0.08, 1.0), smoothNormal);
    vec3 f0 = vec3(0.04 * SPECULAR);
    float nv = clamp(dot(normal, view), 0.0001, 1.0);

    vec3 sun = light.color * light.visibility;
    float wrapped = max((dot(normal, light.direction) + WRAP) / (1.0 + WRAP), 0.0);
    float terminator = smoothstep(0.0, 0.35, wrapped) * (1.0 - smoothstep(0.35, 0.8, wrapped));
    vec3 directDiffuse = sun * (wrapped + SUBSURFACE * vec3(1.0, 0.35, 0.22) * terminator);
    vec3 directSpecular = sun * viewmodel_direct_specular(f0, max(roughness, 0.08), normal, view, light.direction);

    vec3 envSpecular = viewmodel_env_specular(f0, roughness, nv);
    vec3 irradiance = viewmodel_ambient(light, normal, ambientBase, ambientSky, ambientBlock);
    vec3 reflected = viewmodel_reflection(normal, view, roughness);
    vec3 indirectSpecular = viewmodel_environment(light, reflected, roughness, ambientBase, ambientSky, ambientBlock) * envSpecular;

    float rim = pow(1.0 - nv, 3.0);
    vec3 sheen = mix(albedo, vec3(viewmodel_luminance(albedo)), 0.5) * irradiance * rim * SHEEN;

    vec3 color = albedo * (directDiffuse + irradiance * (1.0 - envSpecular)) + directSpecular + indirectSpecular + sheen;
    color *= gl_FrontFacing ? 1.0 : 0.3;
    color = viewmodel_tonemap(color);

    vec4 result = vec4(color, texel.a) * ColorModulator;
    result.rgb = mix(overlayColor.rgb, result.rgb, overlayColor.a);
    fragColor = apply_fog(result, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
