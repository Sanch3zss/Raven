#version 330 core

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV;

uniform mat4 Mvp;

out vec2 texCoord;

void main() {
    texCoord = UV;
    gl_Position = Mvp * vec4(Position, 1.0);
}
