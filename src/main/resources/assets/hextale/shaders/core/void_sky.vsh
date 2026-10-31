#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

// The Void's sky: a box round the camera; the fragment shader paints by the direction it looks along. The box is
// pinned onto the far plane (depth 0 here), so its size never clips it against the render distance.

layout(location = 0) in vec3 Position;

layout(location = 0) out vec3 direction;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    gl_Position.z = 0.0;
    direction = Position;
}
