#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:globals.glsl>

// The abyss inside a rift. The rift is a closed 3D body; every fragment looks along its view ray INTO a 3D field of
// violet mist and specks that lives in the rift's own frame (the ray is turned into that frame with the screen
// derivatives of the world and local positions), so the depth is right from every side and overlapping pieces show the
// very same field - no seams. The field spirals into the rift's axis for ever. The torn edge crawls (animated noise
// eats into the body), glows hot near the cut with a cold fringe; grazing views light the skin. Shards show the same
// abyss with lit edges and are never eaten.

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec3 viewPos;
layout(location = 3) in vec3 localPos;
layout(location = 4) in vec4 riftData;

layout(location = 0) out vec4 fragColor;

const float TAU = 6.2831853;
// Angular cells of the spiral: the noise wraps around after this many cells, so the funnel has no seam.
const float SPOKES = 9.0;

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

// Value noise whose x wraps every `period` cells (period <= 0: no wrap).
float noise(vec2 p, float period) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    vec2 i1 = i + vec2(1.0, 0.0);
    if (period > 0.0) {
        i.x = mod(i.x, period);
        i1.x = mod(i1.x, period);
    }
    float a = hash12(i);
    float b = hash12(i1);
    float c = hash12(vec2(i.x, i.y + 1.0));
    float d = hash12(vec2(i1.x, i1.y + 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p, float period) {
    float sum = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
        sum += amp * noise(p, period);
        // Exactly 2: the wrapped axis must stay a whole number of cells.
        p = p * 2.0 + vec2(0.0, 1.7);
        period *= 2.0;
        amp *= 0.5;
    }
    return sum;
}

float plainFbm(vec2 p) {
    float sum = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
        sum += amp * noise(p, 0.0);
        p = mat2(1.6, 1.2, -1.2, 1.6) * p;
        amp *= 0.5;
    }
    return sum;
}

// Deep indigo -> violet -> magenta -> pale lilac.
vec3 abyssRamp(float x) {
    vec3 c = mix(vec3(0.020, 0.006, 0.050), vec3(0.20, 0.05, 0.42), smoothstep(0.0, 0.45, x));
    c = mix(c, vec3(0.62, 0.14, 0.72), smoothstep(0.40, 0.80, x));
    return mix(c, vec3(1.00, 0.78, 1.00), smoothstep(0.82, 1.0, x));
}

// One layer of the abyss at depth `depth` (blocks behind the plane), seen at plane point p.
vec3 layer(vec3 point, float depth, float t, float seed, float k) {
    vec2 p = point.xy;
    float r = length(p);
    float a = atan(p.y, p.x) / TAU;
    // Log-polar spiral: the mist turns and slides inward forever.
    float twist = 0.55 + 0.25 * k;
    vec2 q = vec2(a * SPOKES + log(r + 0.08) * twist + seed * 3.7,
            log(r + 0.08) * 1.9 + point.z * 0.9 + t * (0.22 + 0.07 * k) + seed * 11.0 + k * 5.3);
    float mist = fbm(q, SPOKES);
    float wisps = smoothstep(0.42, 0.88, mist);
    float fade = exp(-depth * 0.16);
    vec3 col = abyssRamp(mist * (0.75 + 0.18 * fade)) * wisps * (0.55 + 0.45 * fade);

    // Specks: tiny points that drift in with the mist and blink.
    vec2 g = vec2(a * SPOKES * 3.0, log(r + 0.08) * 6.0 + t * (0.5 + 0.2 * k)) + k * 17.0 + seed * 5.0;
    vec2 cell = floor(g);
    cell.x = mod(cell.x, SPOKES * 3.0);
    vec2 f = fract(g) - 0.5;
    float h = hash12(cell + k * 31.0);
    vec2 o = vec2(hash12(cell + 7.1), hash12(cell + 3.9)) - 0.5;
    float star = smoothstep(0.16, 0.0, length(f - o * 0.6)) * step(0.86, h);
    star *= 0.55 + 0.45 * sin(t * (2.0 + h * 5.0) + h * 40.0);
    vec3 starCol = h > 0.97 ? vec3(0.45, 0.95, 1.0) : vec3(1.0, 0.82, 1.0);
    col += starCol * star * fade * 0.9;
    return col;
}

void main() {
    float edge = riftData.r;
    float open = riftData.g;
    bool shard = riftData.b > 0.5;
    float seed = mod(riftData.b * 255.0, 128.0);
    float t = GameTime * 1200.0;

    // Turn the view ray into the rift's frame: the rigid map local -> world is fixed by two screen derivatives and
    // their cross product (a rotation keeps cross products).
    vec3 dp1 = dFdx(viewPos);
    vec3 dp2 = dFdy(viewPos);
    vec3 dl1 = dFdx(localPos);
    vec3 dl2 = dFdy(localPos);
    mat3 world = mat3(dp1, dp2, cross(dp1, dp2));
    mat3 local = mat3(dl1, dl2, cross(dl1, dl2));
    vec3 view = normalize(viewPos);
    vec3 ray = vec3(0.0, 0.0, -1.0);
    vec3 normal = vec3(0.0, 0.0, 1.0);
    if (abs(determinant(world)) > 1e-30) {
        ray = normalize(local * (inverse(world) * view));
        normal = normalize(cross(dl1, dl2));
    }
    float grazing = 1.0 - abs(dot(normal, ray));
    vec2 planeUv = localPos.xy;

    // The torn edge crawls: noise eats into the opening, faster while it tears open.
    float crawl = plainFbm(planeUv * 2.6 + vec2(t * 0.35, -t * 0.27) + seed * 13.0);
    // The body stays closed (cutting the edge away would open a slit between its two skins); the crawl only moves the
    // burning band along the seam.
    float threshold = shard ? -1.0 : 0.12 * crawl - 0.02;

    vec3 col = vec3(0.010, 0.003, 0.026);
    const float DEPTHS[5] = float[](0.35, 1.0, 2.3, 4.8, 10.0);
    for (int i = 0; i < 5; i++) {
        float depth = DEPTHS[i];
        vec3 p = localPos + ray * depth;
        col += layer(p, depth, t, seed, float(i)) * (i == 0 ? 0.45 : 1.0);
    }
    // A faint glow at the bottom of the abyss, far behind everything.
    vec3 far = localPos + ray * 18.0;
    col += vec3(0.55, 0.12, 0.65) * exp(-dot(far.xy, far.xy) * 0.35) * (0.35 + 0.15 * sin(t * 0.9 + seed));
    // Near the edge the opening is shallow and lit by the rim; deep inside it is darker.
    col *= mix(1.0, 0.72, smoothstep(0.35, 1.0, edge));

    float flicker = 0.85 + 0.15 * noise(vec2(t * 6.0, seed * 7.0), 0.0);
    float tearing = 1.0 + 2.2 * (1.0 - open);
    if (shard) {
        float lit = 1.0 - smoothstep(0.0, 0.32, edge);
        col += vec3(0.78, 0.22, 0.95) * lit * lit * 1.3 * flicker;
        col += vec3(1.0, 0.9, 1.0) * smoothstep(0.08, 0.0, edge) * 0.8;
    } else {
        float rim = 1.0 - smoothstep(threshold, threshold + 0.15, edge);
        float hot = 1.0 - smoothstep(threshold, threshold + 0.03, edge);
        float fringe = exp(-pow((edge - threshold - 0.09) / 0.03, 2.0));
        vec3 rimCol = mix(vec3(0.45, 0.07, 0.70), vec3(1.0, 0.45, 0.95), rim * rim);
        col = mix(col, col * 0.4, rim);
        col += rimCol * rim * rim * 1.5 * flicker * tearing;
        col += vec3(0.25, 0.85, 0.95) * fringe * 0.32 * flicker;
        // The skin of the body catches light where it is seen at a glance.
        col += vec3(0.62, 0.16, 0.85) * pow(grazing, 4.0) * 0.3 * flicker;
        col += vec3(1.0, 0.94, 1.0) * hot * (0.9 + 0.6 * (1.0 - open));
    }

    fragColor = apply_fog(vec4(col, 1.0), sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart,
            FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
