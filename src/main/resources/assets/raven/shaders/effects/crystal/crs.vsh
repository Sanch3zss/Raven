#version 330 core

layout(location = 0) in vec3 Position;
layout(location = 1) in vec3 Data1;
layout(location = 2) in vec4 Data2;

uniform int Mode;
uniform mat4 Mvp;
uniform mat4 ModelViewMat;

out vec3 smoothNormal;
out vec3 viewPosition;
out float vertexAlpha;
out vec2 texCoord;
out vec4 vertexColor;

void main() {
    gl_Position = Mvp * vec4(Position, 1.0);
    if (Mode == 0) {
        vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
        smoothNormal = normalize(mat3(ModelViewMat) * Data1);
        viewPosition = viewPos.xyz;
        vertexAlpha = Data2.x;
    } else {
        texCoord = Data1.xy;
        vertexColor = Data2;
    }
}
