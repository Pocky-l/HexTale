#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

// The Void: a slab of square cells (body pass) and the glow on the free cells around them (halo pass).
// UV0 = the point in the breach's own plane (blocks, centre = 0); on a side wall (along the edge, z share -1..1).
// Color: r = neighbour mask (body: sides that are open, halo: neighbours that burn; bits +x, +y, -x, -y, then the
// corners +x+y, -x+y, -x-y, +x-y), g = how far the breach is open, b = seed 0-127 (+128 on a side wall),
// a = seams (4 side bits) | region shade << 4 (0 = a dead cell).
// UV1 = cell size, core brightness (thousandths). UV2 = fresh flash (thousandths), size (hundredths of a block).

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV1;
layout(location = 4) in ivec2 UV2;
layout(location = 5) in vec3 Normal;

layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
layout(location = 2) out vec3 viewPos;
layout(location = 3) out vec2 localUv;
layout(location = 4) flat out ivec4 voidData;
layout(location = 5) flat out ivec2 cellCore;
layout(location = 6) flat out ivec2 freshSize;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    viewPos = Position;
    localUv = UV0;
    voidData = ivec4(round(Color * 255.0));
    cellCore = UV1;
    freshSize = UV2;
}
