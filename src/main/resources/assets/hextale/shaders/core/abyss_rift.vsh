#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

// An abyss rift: the opening and the shards of torn space around it.
// The rift is a 3D body. UV0 = x, y of the point in the rift's own frame (blocks, centre = 0), Color.a = its z
// (128 = 0, +-127 = +-Z_RANGE). Normal = the smooth skin normal in the rift's frame. Color: g = how far the
// rift is open, b = random phase 0-127 (+128 on a shard).

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;
layout(location = 3) in vec3 Normal;

layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
layout(location = 2) out vec3 viewPos;
layout(location = 3) out vec3 localPos;
layout(location = 4) out vec4 riftData;
layout(location = 5) out vec3 localNormal;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    viewPos = Position;
    localPos = vec3(UV0, (Color.a * 255.0 - 128.0) / 127.0 * 2.0);
    riftData = Color;
    localNormal = Normal;
}
