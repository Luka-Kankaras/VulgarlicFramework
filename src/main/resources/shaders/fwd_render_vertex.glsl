#version 450

layout(location = 0) in vec3 entityPos;
layout(location = 1) in vec2 entityTexCoords;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec2 texCoords;

layout(set = 0, binding = 0) uniform ProjectionMatrix {
    mat4 projectionMatrix;
};

layout(set = 0, binding = 1) uniform ViewMatrix {
    mat4 viewMatrix;
};

layout(set = 0, binding = 2) uniform ModelMatrix {
    mat4 modelMatrix;
};

void main() {
    gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(entityPos, 1);
    fragColor = vec4(1, 0, 0, 1);
    texCoords = entityTexCoords;
}