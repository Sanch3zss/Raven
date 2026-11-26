#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:sample_lightmap.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec3 ambientBase;
out vec3 ambientSky;
out vec3 ambientBlock;
out vec4 overlayColor;
out vec2 texCoord0;
out vec3 viewPosition;
out vec3 viewNormal;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    vec3 base = pow(sample_lightmap(Sampler2, ivec2(0)).rgb, vec3(2.2));
    vec3 total = pow(sample_lightmap(Sampler2, UV2).rgb, vec3(2.2));
    vec3 sky = max(pow(sample_lightmap(Sampler2, ivec2(0, UV2.y)).rgb, vec3(2.2)) - base, vec3(0.0));
    vec3 block = max(pow(sample_lightmap(Sampler2, ivec2(UV2.x, 0)).rgb, vec3(2.2)) - base, vec3(0.0));
    float share = dot(max(total - base, vec3(0.0)), vec3(0.2125, 0.7154, 0.0721))
            / max(dot(sky + block, vec3(0.2125, 0.7154, 0.0721)), 0.0001);
    ambientBase = base;
    ambientSky = sky * min(share, 1.0);
    ambientBlock = block * min(share, 1.0);
    overlayColor = texelFetch(Sampler1, UV1, 0);
    texCoord0 = UV0;
    viewPosition = (ModelViewMat * vec4(Position, 1.0)).xyz;
    viewNormal = mat3(ModelViewMat) * Normal;
}
