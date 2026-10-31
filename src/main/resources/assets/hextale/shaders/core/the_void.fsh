#version 150

#moj_import <fog.glsl>

uniform float GameTime;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

// The cells of the Void. Their faces look into a throat that sinks behind the breach from whichever side it is seen:
// dark umber coils wind round and down it, glassy lilac ribbons glow deeper in, and at the bottom a magenta heart beats
// behind a net of black roots. Every cell is shaded by its region (rectangles of the breach differ a little, a dead
// cell is black), seams smoulder between regions, the outline burns. Side walls of the slab are pure burning edge.

in float vertexDistance;
in vec3 viewPos;
in vec2 localUv;
flat in ivec4 voidData;
flat in ivec2 cellCore;
flat in ivec2 freshSize;

out vec4 fragColor;

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

// A slow heartbeat: two beats, then a long rest (seconds).
float heartbeat(float t) {
    float phase = fract(t / 1.9);
    return exp(-pow((phase - 0.08) / 0.045, 2.0)) + 0.65 * exp(-pow((phase - 0.27) / 0.06, 2.0));
}

// A held breach's colour: the same light, cooled to a cold violet.
vec3 cooled(vec3 c) {
    float l = dot(c, vec3(0.38, 0.27, 0.35));
    return vec3(l * 0.62, l * 0.58, l * 1.45);
}

// The throat seen at plane point uv along the ray (in the breach's frame). R = half the breach's size.
vec3 throat(vec2 uv, vec3 ray, float t, float seed, float core, float R) {
    float slope = 1.0 / max(abs(ray.z), 0.22);
    float beat = heartbeat(t + seed * 0.13);

    // The bottom: a magenta heart behind black roots.
    vec2 bottom = (uv + ray.xy * 7.0 * slope) / R;
    float rb = length(bottom);
    vec3 col = vec3(0.06, 0.01, 0.09) + vec3(0.22, 0.04, 0.26) * fbm(bottom * 3.0 + vec2(t * 0.03, seed));
    float spread = 0.07 + 0.2 * core;
    float glow = exp(-rb * rb / (spread * (1.0 + 0.25 * beat)));
    vec3 heart = mix(vec3(0.75, 0.10, 0.62), vec3(1.0, 0.78, 0.96), smoothstep(0.45, 1.0, glow));
    col += heart * glow * (0.45 + 1.3 * core) * (0.8 + 0.45 * beat);
    vec2 vp = rotate(bottom, t * 0.02) * 1.8;
    float roots = 1.0 - smoothstep(0.0, 0.06, abs(fbm(vp + seed) - 0.5));
    roots = max(roots, 1.0 - smoothstep(0.0, 0.035, abs(fbm(vp * 2.3 + seed + 4.0) - 0.5)));
    roots *= smoothstep(1.6, 0.12, rb) * smoothstep(0.0, 0.14, rb);
    col = mix(col, vec3(0.025, 0.018, 0.01), roots * 0.9);

    // Ribbons, deepest first, each laid over what lies behind it: glassy lilac deep down, dark umber coils near.
    for (int k = 5; k >= 0; k--) {
        float fk = float(k);
        float depth = 0.25 + fk * fk * 0.22;
        vec2 q = (uv + ray.xy * depth * slope) / R;
        float r = length(q);
        float a = atan(q.y, q.x);
        float arms = 2.0 + mod(fk + seed, 3.0);
        float dir = mod(fk, 2.0) < 0.5 ? 1.0 : -0.6;
        float s = sin(a * arms + log(r + 0.04) * (2.4 + 0.3 * fk) * dir - t * (0.16 + 0.04 * fk) * dir + seed * 1.7 + fk * 2.1);
        // The throat narrows with depth: each ring of ribbons sits a little further in.
        float ring = 0.95 / (1.0 + depth * 0.45);
        float inRing = smoothstep(ring * 0.25, ring * 0.7, r) * (1.0 - smoothstep(ring * 1.3, ring * 2.4, r));
        float band = smoothstep(0.3, 0.7, s);
        float alpha = band * inRing * 0.9;
        float deep = fk / 5.0;
        // Lit along the edge that faces the heart, dark where it turns away.
        float lit = smoothstep(0.78, 1.0, s);
        vec3 coil = mix(vec3(0.07, 0.055, 0.035), vec3(0.20, 0.16, 0.10), fbm(q * 6.0 + fk));
        vec3 glass = mix(vec3(0.42, 0.08, 0.52), vec3(0.88, 0.42, 0.95), 0.4 + 0.6 * fbm(q * 4.0 - fk));
        vec3 ribbon = mix(coil, glass, smoothstep(0.15, 0.7, deep));
        ribbon += mix(vec3(0.55, 0.32, 0.5), vec3(1.0, 0.62, 1.0), deep) * lit * (0.5 + 0.4 * core);
        ribbon += heart * exp(-r * r / (0.08 + 0.2 * core)) * 0.75 * deep * (0.8 + 0.4 * beat);
        col = mix(col, ribbon, alpha);
    }

    // Pale motes sinking slowly towards the heart.
    vec2 sp = uv + ray.xy * 1.1 * slope;
    sp = rotate(sp, t * 0.06 / (length(sp) / R + 0.4));
    vec2 g = sp / 0.16;
    float h = hash12(floor(g) + seed);
    vec2 f = abs(fract(g) - 0.5);
    float speck = step(0.95, h) * step(max(f.x, f.y), 0.11) * (0.55 + 0.45 * sin(t * (0.8 + 1.5 * h) + h * 40.0));
    col += mix(vec3(0.85, 0.45, 0.9), vec3(1.0, 0.9, 1.0), step(0.985, h)) * speck * 0.7;

    // The breach darkens towards its rim: the light lives only deep inside.
    return col * mix(0.45, 1.0, smoothstep(1.25, 0.3, length(uv) / R));
}

// A gate seen from inside the Void: our world through it, soft as if through warm water - a daylit sky over a green
// smear of land, a low sun, slow clouds and gold dust drifting in front. The only warm light in the realm.
vec3 home(vec2 uv, vec3 ray, float t, float seed, float R) {
    float slope = 1.0 / max(abs(ray.z), 0.22);
    // The world beyond sits far behind the plane: it shifts with the view, the dust in front barely does.
    vec2 far = (uv + ray.xy * 9.0 * slope) / R;
    float horizon = -0.18 + 0.05 * sin(far.x * 1.7 + seed);
    float above = far.y - horizon;
    vec3 sky = mix(vec3(0.95, 0.82, 0.60), vec3(0.55, 0.76, 0.92), smoothstep(0.0, 0.9, above));
    float clouds = smoothstep(0.55, 0.8, fbm(vec2(far.x * 1.3 + t * 0.012, far.y * 3.0) + seed));
    sky = mix(sky, vec3(1.0, 0.96, 0.9), clouds * 0.55 * smoothstep(0.05, 0.4, above));
    vec2 sun = far - vec2(0.35 + 0.1 * sin(seed), horizon + 0.22);
    sky += vec3(1.0, 0.85, 0.55) * exp(-dot(sun, sun) * 9.0) * 0.8;
    float hills = horizon - 0.05 * fbm(vec2(far.x * 2.0, seed));
    vec3 land = mix(vec3(0.33, 0.52, 0.27), vec3(0.50, 0.69, 0.41), fbm(far * 3.0 + seed) * 0.8);
    land = mix(land, vec3(0.86, 0.78, 0.58), smoothstep(-0.02, 0.0, far.y - hills) * 0.4);
    vec3 col = mix(land, sky, smoothstep(-0.01, 0.01, far.y - hills));
    // Seen through the Void's skin: a little hazed with lilac at the edges.
    col = mix(col, vec3(0.78, 0.70, 0.86), smoothstep(0.55, 1.25, length(uv) / R) * 0.55);
    // Gold dust, close to the plane.
    vec2 sp = uv + ray.xy * 0.6 * slope + vec2(0.0, t * 0.02);
    vec2 g = sp / 0.18;
    float h = hash12(floor(g) + seed);
    vec2 f = abs(fract(g) - 0.5);
    float speck = step(0.93, h) * step(max(f.x, f.y), 0.12) * (0.5 + 0.5 * sin(t * (0.6 + h) + h * 30.0));
    col += vec3(1.0, 0.86, 0.5) * speck * 0.6;
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
    // A gate inside the Void carries its core as a negative number.
    bool gate = cellCore.y < 0;
    float core = float(gate ? -cellCore.y - 1 : cellCore.y) / 1000.0;
    float fresh = float(freshSize.x) / 1000.0;
    // The size's low 11 bits; above them how far a held breach has calmed (0-15).
    float R = float(freshSize.y & 2047) / 200.0;
    float calm = float(freshSize.y >> 11) / 15.0;
    float t = GameTime * 1200.0;
    float beat = heartbeat(t + seed * 0.13);
    float pulse = 0.88 + 0.12 * beat;
    float tearing = 1.0 + 1.2 * (1.0 - open);

    vec3 col;
    if (sideWall) {
        float hot = 1.0 - abs(localUv.y);
        col = mix(vec3(0.62, 0.05, 0.45), vec3(1.0, 0.62, 0.92), hot * hot) * 1.2 * pulse * tearing;
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

        col = gate ? home(localUv, ray, t, seed, max(R, 0.5)) : throat(localUv, ray, t, seed, core, max(R, 0.5));

        // Regions: the rectangles the breach is made of differ in shade; a dead cell is black.
        float shade = float(shadeLevel) / 15.0;
        if (shadeLevel == 0) {
            col = col * 0.06 + vec3(0.01, 0.0, 0.015);
        } else {
            col *= 0.72 + 0.45 * shade;
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

        // Black just inside the edge, so the burning outline stands against nothing.
        col *= mix(0.12, 1.0, smoothstep(0.0, 1.5, d));
        col += vec3(0.6, 0.06, 0.45) * (smoothstep(0.06, 0.0, seam) * 0.8 + exp(-seam * 7.0) * 0.12) * pulse;

        float band = smoothstep(0.2, 0.0, d);
        float hot = smoothstep(0.07, 0.0, d);
        vec3 rim = mix(vec3(0.68, 0.05, 0.5), vec3(1.0, 0.7, 0.95), hot) * 1.3 * pulse * tearing;
        if (gate) {
            rim = cooled(rim);
        }
        col = mix(col, rim, band);
    }

    // A cell that has only just broken through is still black.
    col *= 1.0 - fresh * 0.85;
    // Held by its anchors, the breach cools from magenta to a cold violet (a gate keeps its warm view, only the rim cools).
    if (!gate) {
        col = mix(col, cooled(col), calm);
    }

    fragColor = linear_fog(vec4(col, 1.0), vertexDistance, FogStart, FogEnd, FogColor);
}
