#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:globals.glsl>

// The cells of the Void. Their faces look into a tunnel that sinks behind the breach from whichever side it is seen:
// ribbons of glassy violet spiralling round and down towards a pink-white glow, dark veins across the glow, cold specks
// drifting in. Every cell is shaded by its region (rectangles of the breach differ a little, a dead cell is nearly
// black), seams burn faintly between regions, the outline burns hot. Side walls of the slab are pure burning edge.

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec3 viewPos;
layout(location = 3) in vec2 localUv;
layout(location = 4) flat in ivec4 voidData;
layout(location = 5) flat in ivec2 cellCore;
layout(location = 6) flat in ivec2 freshSize;

layout(location = 0) out vec4 fragColor;

const float TAU = 6.2831853;

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash12(i), hash12(i + vec2(1.0, 0.0)), u.x), mix(hash12(i + vec2(0.0, 1.0)), hash12(i + vec2(1.0)), u.x), u.y);
}

float fbm(vec2 p) {
    float sum = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
        sum += amp * noise(p);
        p = mat2(1.6, 1.2, -1.2, 1.6) * p;
        amp *= 0.5;
    }
    return sum;
}

vec2 rotate(vec2 p, float a) {
    float c = cos(a);
    float s = sin(a);
    return vec2(c * p.x - s * p.y, s * p.x + c * p.y);
}

// The tunnel seen at plane point uv along the ray (in the breach's frame). R = half the breach's size.
vec3 tunnel(vec2 uv, vec3 ray, float t, float seed, float core, float R) {
    float slope = 1.0 / max(abs(ray.z), 0.22);

    // The bottom: a glow with dark veins branching across it.
    vec2 bottom = (uv + ray.xy * 7.0 * slope) / R;
    float rb = length(bottom);
    vec3 col = vec3(0.07, 0.015, 0.14) + vec3(0.12, 0.03, 0.2) * fbm(bottom * 3.0 + vec2(t * 0.05, seed));
    float glow = exp(-rb * rb / (0.06 + 0.22 * core));
    vec3 glowCol = mix(vec3(0.95, 0.30, 0.90), vec3(1.0, 0.92, 1.0), smoothstep(0.35, 1.0, glow));
    col += glowCol * glow * (0.55 + 1.5 * core);
    vec2 vp = rotate(bottom, t * 0.03) * 2.2;
    float vein = 1.0 - smoothstep(0.0, 0.045, abs(fbm(vp + seed) - 0.5));
    vein *= smoothstep(1.4, 0.15, rb) * smoothstep(0.02, 0.18, rb);
    col *= 1.0 - vein * 0.75;

    // Ribbons, deepest first, each laid over what lies behind it.
    for (int k = 5; k >= 0; k--) {
        float fk = float(k);
        float depth = 0.25 + fk * fk * 0.22;
        vec2 q = (uv + ray.xy * depth * slope) / R;
        float r = length(q);
        float a = atan(q.y, q.x);
        float arms = 2.0 + mod(fk + seed, 3.0);
        float dir = mod(fk, 2.0) < 0.5 ? 1.0 : -0.6;
        float s = sin(a * arms + log(r + 0.04) * (2.4 + 0.3 * fk) * dir - t * (0.35 + 0.08 * fk) * dir + seed * 1.7 + fk * 2.1);
        // The tunnel narrows with depth: each ring of ribbons sits a little further in.
        float ring = 0.95 / (1.0 + depth * 0.45);
        float inRing = smoothstep(ring * 0.25, ring * 0.7, r) * (1.0 - smoothstep(ring * 1.3, ring * 2.4, r));
        float band = smoothstep(0.35, 0.75, s);
        float alpha = band * inRing * 0.85;
        // Glassy: lit along the inner edge (towards the glow), dark violet away from it, lighter the deeper.
        float lit = smoothstep(0.75, 1.0, s) * (0.6 + 0.4 * core);
        float deep = 1.0 - fk / 5.0;
        vec3 ribbon = mix(vec3(0.16, 0.035, 0.28), vec3(0.62, 0.22, 0.78), 0.35 + 0.45 * deep);
        ribbon += vec3(0.95, 0.62, 1.0) * lit * (0.35 + 0.65 * deep);
        ribbon += glowCol * exp(-r * r / (0.05 + 0.2 * core)) * 0.6 * deep;
        col = mix(col, ribbon, alpha);
    }

    // Cold specks drifting round and in.
    vec2 sp = (uv + ray.xy * 1.1 * slope);
    sp = rotate(sp, t * 0.12 / (length(sp) / R + 0.4));
    vec2 g = sp / 0.16;
    vec2 cellId = floor(g);
    float h = hash12(cellId + seed);
    vec2 f = abs(fract(g) - 0.5);
    float speck = step(0.93, h) * step(max(f.x, f.y), 0.13) * (0.5 + 0.5 * sin(t * (2.0 + 5.0 * h) + h * 40.0));
    col += mix(vec3(0.45, 0.9, 1.0), vec3(1.0, 0.85, 1.0), step(0.97, h)) * speck;
    return col;
}

void main() {
    int mask = voidData.r;
    float open = float(voidData.g) / 255.0;
    bool sideWall = voidData.b >= 128;
    float seed = float(voidData.b & 127);
    int seams = voidData.a & 15;
    int shadeLevel = voidData.a >> 4;
    float cell = float(cellCore.x) / 1000.0;
    float core = float(cellCore.y) / 1000.0;
    float fresh = float(freshSize.x) / 1000.0;
    float R = float(freshSize.y) / 200.0;
    float t = GameTime * 1200.0;
    float flicker = 0.85 + 0.15 * noise(vec2(t * 9.0, seed * 7.0));
    float tearing = 1.0 + 1.5 * (1.0 - open);

    vec3 col;
    if (sideWall) {
        float hot = 1.0 - abs(localUv.y);
        col = mix(vec3(0.95, 0.16, 0.82), vec3(1.0, 0.85, 1.0), hot * hot) * (1.25 + 0.25 * sin(t * 3.0 + localUv.x * 6.0)) * flicker * tearing;
    } else {
        // The view ray in the breach's frame: the plane's axes in the world from the screen derivatives.
        vec3 dp1 = dFdx(viewPos);
        vec3 dp2 = dFdy(viewPos);
        vec2 du1 = dFdx(localUv);
        vec2 du2 = dFdy(localUv);
        float det = du1.x * du2.y - du1.y * du2.x;
        vec3 ray = vec3(0.0, 0.0, -1.0);
        if (abs(det) > 1e-12) {
            vec3 T = (dp1 * du2.y - dp2 * du1.y) / det;
            vec3 B = (dp2 * du1.x - dp1 * du2.x) / det;
            vec3 N = cross(T, B);
            if (dot(N, N) > 1e-12 && dot(T, T) > 1e-12 && dot(B, B) > 1e-12) {
                vec3 view = normalize(viewPos);
                ray = vec3(dot(view, normalize(T)), dot(view, normalize(B)), dot(view, normalize(N)));
            }
        }

        // Now and then a band of the picture slips sideways: the Void does not hold still.
        vec2 uv = localUv;
        float slip = hash12(vec2(floor(uv.y / cell * 0.5), floor(t * 5.0) + seed));
        uv.x += step(0.975, slip) * (hash12(vec2(slip, seed)) - 0.5) * cell * 3.0;

        col = tunnel(uv, ray, t, seed, core, max(R, 0.5));

        // Regions: the rectangles the breach is made of differ in shade; a dead cell is nearly black.
        float shade = float(shadeLevel) / 15.0;
        if (shadeLevel == 0) {
            col = col * 0.12 + vec3(0.02, 0.0, 0.04);
        } else {
            col = mix(col, col * vec3(0.75, 0.9, 1.35), shade * 0.6) * (0.7 + 0.55 * shade);
        }

        vec2 c = fract(localUv / cell);
        // Distance (in cells) to the open sides and corners, and to the seams with other regions.
        float d = 10.0;
        if ((mask & 1) != 0) d = min(d, 1.0 - c.x);
        if ((mask & 2) != 0) d = min(d, 1.0 - c.y);
        if ((mask & 4) != 0) d = min(d, c.x);
        if ((mask & 8) != 0) d = min(d, c.y);
        if ((mask & 16) != 0) d = min(d, length(vec2(1.0) - c));
        if ((mask & 32) != 0) d = min(d, length(vec2(c.x, 1.0 - c.y)));
        if ((mask & 64) != 0) d = min(d, length(c));
        if ((mask & 128) != 0) d = min(d, length(vec2(1.0 - c.x, c.y)));
        float seam = 10.0;
        if ((seams & 1) != 0) seam = min(seam, 1.0 - c.x);
        if ((seams & 2) != 0) seam = min(seam, 1.0 - c.y);
        if ((seams & 4) != 0) seam = min(seam, c.x);
        if ((seams & 8) != 0) seam = min(seam, c.y);

        // Darker just inside the edge, so the burning outline stands out.
        col *= mix(0.35, 1.0, smoothstep(0.0, 1.3, d));
        col += vec3(0.85, 0.16, 0.8) * (smoothstep(0.07, 0.0, seam) * 0.9 + exp(-seam * 6.0) * 0.18) * flicker;

        float band = smoothstep(0.22, 0.0, d);
        float hot = smoothstep(0.08, 0.0, d);
        vec3 rim = mix(vec3(0.95, 0.14, 0.82), vec3(1.0, 0.86, 1.0), hot) * 1.35 * flicker * tearing;
        col = mix(col, rim, band);
    }

    // A cell that has just broken through flashes white.
    col = mix(col, vec3(1.0, 0.9, 1.0) * 1.4, fresh * 0.85);

    fragColor = apply_fog(vec4(col, 1.0), sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart,
            FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
