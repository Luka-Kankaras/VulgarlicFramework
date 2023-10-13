#version 450

layout(set = 1, binding = 0) uniform sampler2D texSampler;

layout(location = 0) in vec4 entityFragColor;
layout(location = 1) in vec2 entityTexCoords;

layout(location = 0) out vec4 finalColor;

void main() {
    finalColor = texture(texSampler, entityTexCoords);
    //    finalColor = entityFragColor;
}