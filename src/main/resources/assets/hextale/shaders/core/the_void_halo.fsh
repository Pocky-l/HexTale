#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:globals.glsl>

// The glow around the Void, on the free cells next to it (additive). Each cell knows which of its eight neighbours
// burn; the distance to the nearest of them gives a crisp line on the edge and a soft glow that fades within a cell,
// so the glow follows every step of the outline without seams between cells.

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec3 viewPos;
layout(location = 3) in vec2 localUv;
layout(location = 4) flat in ivec4 voidData;
layout(location = 5) flat in ivec2 cellCore;
layout(location = 6) flat in ivec2 freshSize;

layout(location = 0) out vec4 fragColor;

float hash11(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

void main() {
    int on = voidData.r;
    float open = float(voidData.g) / 255.0;
    float seed = float(voidData.b & 127);
    float cell = float(cellCore.x) / 1000.0;
    float t = GameTime * 1200.0;

    vec2 c = fract(localUv / cell);
    float d = 10.0;
    if ((on & 1) != 0) d = min(d, 1.0 - c.x);
    if ((on & 2) != 0) d = min(d, 1.0 - c.y);
    if ((on & 4) != 0) d = min(d, c.x);
    if ((on & 8) != 0) d = min(d, c.y);
    if ((on & 16) != 0) d = min(d, length(vec2(1.0) - c));
    if ((on & 32) != 0) d = min(d, length(vec2(c.x, 1.0 - c.y)));
    if ((on & 64) != 0) d = min(d, length(c));
    if ((on & 128) != 0) d = min(d, length(vec2(1.0 - c.x, c.y)));

    float flicker = 0.85 + 0.15 * hash11(floor(t * 12.0) + seed);
    float line = smoothstep(0.09, 0.0, d);
    float glow = exp(-d * 3.6) * (1.0 - smoothstep(0.55, 1.0, d));
    vec3 col = vec3(1.0, 0.16, 0.86) * (glow * 0.75 + line * 1.1) + vec3(1.0, 0.82, 1.0) * line * 0.5;
    // A tearing breach burns brighter.
    col *= flicker * (1.0 + 1.5 * (1.0 - open));

    float fog = total_fog_value(sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd,
            FogRenderDistanceStart, FogRenderDistanceEnd);
    fragColor = vec4(col * (1.0 - fog), 1.0);
}
