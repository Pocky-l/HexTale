#version 150

#moj_import <fog.glsl>

uniform float GameTime;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

// The glow around the Void, on the free cells next to it (additive). Each cell knows which of its eight neighbours
// burn; the distance to the nearest of them gives a crisp line on the edge and a soft glow that fades within a cell,
// so the glow follows every step of the outline without seams between cells.

in float vertexDistance;
in vec3 viewPos;
in vec2 localUv;
flat in ivec4 voidData;
flat in ivec2 cellCore;
flat in ivec2 freshSize;

out vec4 fragColor;

// The same slow heartbeat as the breach (seconds).
float heartbeat(float t) {
    float phase = fract(t / 1.9);
    return exp(-pow((phase - 0.08) / 0.045, 2.0)) + 0.65 * exp(-pow((phase - 0.27) / 0.06, 2.0));
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

    float pulse = 0.82 + 0.25 * heartbeat(t + seed * 0.13);
    float line = smoothstep(0.08, 0.0, d);
    float glow = exp(-d * 4.2) * (1.0 - smoothstep(0.5, 1.0, d));
    vec3 col = vec3(0.72, 0.06, 0.55) * (glow * 0.6 + line * 1.0) + vec3(1.0, 0.7, 0.95) * line * 0.35;
    // A breach still breaking through burns brighter.
    col *= pulse * (1.0 + 1.2 * (1.0 - open));

    fragColor = vec4(col * linear_fog_fade(vertexDistance, FogStart, FogEnd), 1.0);
}
