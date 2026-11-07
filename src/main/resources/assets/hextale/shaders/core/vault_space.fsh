#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:globals.glsl>

// The space behind a Hex Vault's door, seen as if it went on far behind the frame. A dark blue depth with slow clouds of
// teal and rose, a lattice of warm hexagons far inside with light running along it, three layers of stars that shift against
// each other as the viewer moves, and a warm glow from the frame's inside edge. The fuller the vault, the more the lattice
// burns. With the door shut only its edge shows, through the gap round the petals.

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec3 viewPos;
layout(location = 3) in vec2 localUv;
layout(location = 4) in vec2 openFilled;

layout(location = 0) out vec4 fragColor;

const float APOTHEM = 1.6875;

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

// The point seen on a plane this far behind the door.
vec2 behind(vec2 uv, vec3 ray, float depth) {
    return uv + ray.xy / max(abs(ray.z), 0.3) * depth;
}

// Distance to the nearest edge of a pointy hexagon grid, 0 on the edge, and the cell's id.
vec3 hexGrid(vec2 p) {
    const vec2 s = vec2(1.0, 1.7320508);
    vec2 a = mod(p, s) - s * 0.5;
    vec2 b = mod(p - s * 0.5, s) - s * 0.5;
    vec2 g = dot(a, a) < dot(b, b) ? a : b;
    vec2 q = abs(g);
    float edge = 0.5 - max(dot(q, s * 0.5), q.x);
    return vec3(edge, p - g);
}

float stars(vec2 p, float scale, float t) {
    vec2 cell = floor(p * scale);
    vec2 inside = fract(p * scale) - 0.5;
    float seed = hash12(cell);
    if (seed < 0.86) {
        return 0.0;
    }
    vec2 at = vec2(hash12(cell + 7.1), hash12(cell + 13.7)) - 0.5;
    float twinkle = 0.65 + 0.35 * sin(t * (0.6 + seed * 2.0) + seed * 40.0);
    return smoothstep(0.16, 0.0, length(inside - at * 0.7)) * twinkle;
}

void main() {
    float open = openFilled.x;
    float filled = openFilled.y;
    float t = GameTime * 1200.0;

    // The view ray in the door's frame: the plane's axes in the world from the screen derivatives.
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

    // Clouds on two depths.
    vec2 far = behind(localUv, ray, 7.0);
    float cloud = fbm(far * 0.42 + vec2(t * 0.012, -t * 0.008));
    float wisp = fbm(behind(localUv, ray, 3.5) * 0.8 - vec2(t * 0.02, t * 0.011) + cloud * 1.6);
    vec3 col = vec3(0.012, 0.016, 0.055);
    col = mix(col, vec3(0.02, 0.17, 0.26), smoothstep(0.34, 0.74, cloud));
    col = mix(col, vec3(0.34, 0.09, 0.27), smoothstep(0.5, 0.82, wisp) * 0.8);

    // The lattice, with light running outwards along it.
    vec2 mid = behind(localUv, ray, 4.5);
    vec3 hex = hexGrid(mid * 0.9);
    float line = smoothstep(0.045, 0.0, hex.x);
    float wave = 0.5 + 0.5 * sin(length(hex.yz) * 1.7 - t * 0.7);
    vec3 gold = vec3(1.0, 0.72, 0.36);
    col += gold * line * (0.08 + 0.3 * wave * wave) * (0.35 + 0.65 * filled);
    // A cell lit now and then.
    float blink = hash12(floor(hex.yz * 4.0));
    float lit = smoothstep(0.93, 1.0, sin(t * (0.2 + blink * 0.3) + blink * 30.0));
    col += gold * 0.1 * lit * smoothstep(0.0, 0.3, hex.x);

    // Stars.
    col += vec3(0.75, 0.88, 1.0) * stars(behind(localUv, ray, 9.0), 3.2, t) * 0.8;
    col += vec3(1.0, 0.9, 0.72) * stars(behind(localUv, ray, 5.5), 2.1, t + 3.0);
    col += vec3(0.8, 0.95, 1.0) * stars(behind(localUv, ray, 2.5), 1.3, t + 7.0) * 1.2;

    // Shut, the depth goes dark behind the petals.
    col *= mix(0.25, 1.0, smoothstep(0.0, 0.6, open));

    // Warm light off the frame's inside edge; the middle stays deep. It burns through the gap round a shut door too.
    vec2 q = abs(localUv);
    float side = max(q.y, q.x * 0.8660254 + q.y * 0.5) / APOTHEM;
    float rim = pow(smoothstep(0.55, 1.0, side), 3.0);
    col += vec3(1.0, 0.63, 0.28) * rim * (0.55 + 0.3 * sin(t * 0.8));

    fragColor = apply_fog(vec4(col, 1.0), sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart,
            FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
