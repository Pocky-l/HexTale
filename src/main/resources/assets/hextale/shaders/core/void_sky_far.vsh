#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

// The Void's sky drawn over a shader pack's finished picture: the same box as void_sky.vsh, pushed onto the far plane
// (depth 0 - the depth is reversed), so with a "greater or equal" test it only lands where the world left the sky open.

layout(location = 0) in vec3 Position;

layout(location = 0) out vec3 direction;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    gl_Position.z = 0.0;
    direction = Position;
}
