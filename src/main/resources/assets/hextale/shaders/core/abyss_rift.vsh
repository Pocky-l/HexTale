#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

// An abyss rift: the opening and the shards of torn space around it.
// UV0 = position on the rift's own plane in blocks (centre = 0, 0). Color: r = depth into the opening
// (0 on the torn edge, 1 deep inside), g = how far the rift is open, b = 0 for the opening, 1 for a shard,
// a = the rift's own random phase.

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;

layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
layout(location = 2) out vec3 viewPos;
layout(location = 3) out vec2 planeUv;
layout(location = 4) out vec4 riftData;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    viewPos = Position;
    planeUv = UV0;
    riftData = Color;
}
