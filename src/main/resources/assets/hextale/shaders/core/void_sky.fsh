#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>

// The sky of the Void. Overhead a funnel of membrane turns, slower than anything should: three layers of translucent
// veils spiral in toward a hot violet eye, each layer at its own pace and the nearest against the others, so they
// overlap like the folds of something swallowing. The membrane is violet and veined, lilac currents stream inward
// across it, and every veil is lit pink on the edge that faces the eye and throws a soft shade on the sheet behind.
// Toward the horizon everything sinks into the zone's haze (the fog colour), where three layers of far ribs, curled
// horns and crystal-tipped spires stand in drifting mist. Below, the abyss: plum depths, a second slower funnel of
// glowing tissue round a magenta heart, motes rising out of it. The hearts beat like the breaches do: two beats, a long
// rest.
//
// GameTime runs 0..1 over 24000 ticks (20 minutes) and wraps; every motion here turns or slides a whole number of
// periods per cycle, so the wrap never shows. Noise round the poles is periodic in the azimuth, so there is no seam.

in vec3 direction;

out vec4 fragColor;

const float TAU = 6.2831853;

const vec3 CHITIN_SHADOW = vec3(0.105, 0.080, 0.070);
const vec3 HUNGER = vec3(0.878, 0.251, 0.784);
const vec3 HUNGER_CORE = vec3(1.000, 0.700, 0.940);
const vec3 MEMBRANE = vec3(0.541, 0.361, 0.722);
const vec3 LILAC = vec3(0.706, 0.549, 0.847);
const vec3 FLESH = vec3(0.172, 0.039, 0.149);
const vec3 FLESH_DEEP = vec3(0.078, 0.016, 0.059);

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec3 hash33(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.xxy + p.yxx) * p.zyx);
}

// Index of a whole number in 0..n-1; plain mod() of a whole number can come out as n when the division rounds down.
float wrapIndex(float i, float n) {
    return floor(mod(i + 0.5, n));
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash12(i), hash12(i + vec2(1.0, 0.0)), u.x), mix(hash12(i + vec2(0.0, 1.0)), hash12(i + vec2(1.0)), u.x), u.y);
}

// Value noise that repeats every `period` cells along x (the azimuth).
float pnoise(vec2 p, float period) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float x0 = wrapIndex(i.x, period);
    float x1 = wrapIndex(i.x + 1.0, period);
    return mix(mix(hash12(vec2(x0, i.y)), hash12(vec2(x1, i.y)), u.x),
            mix(hash12(vec2(x0, i.y + 1.0)), hash12(vec2(x1, i.y + 1.0)), u.x), u.y);
}

float pfbm(vec2 p, float period, int octaves) {
    float sum = 0.0;
    float amp = 0.5;
    float norm = 0.0;
    for (int i = 0; i < 5; i++) {
        if (i >= octaves) {
            break;
        }
        sum += amp * pnoise(p, period);
        norm += amp;
        p = p * 2.0 + vec2(0.0, 17.31);
        period *= 2.0;
        amp *= 0.5;
    }
    return sum / norm;
}

float heartbeat(float phase) {
    return exp(-pow((phase - 0.08) / 0.045, 2.0)) + 0.65 * exp(-pow((phase - 0.27) / 0.06, 2.0));
}

// Polar frame round a pole: u = azimuth in turns, r = 0 at the pole .. 1 at the horizon, lr = log r (log-polar is
// conformal, so (u * TAU, lr) is a square grid everywhere), du/dlr = their screen-space footprints for antialiasing.
struct Polar {
    float u;
    float r;
    float lr;
    float du;
    float dlr;
};

Polar polar(vec3 d, float toward) {
    Polar p;
    p.u = atan(d.z, d.x) / TAU;
    p.r = acos(clamp(d.y * toward, -1.0, 1.0)) / 1.5707963;
    p.lr = log(p.r + 0.002);
    p.du = min(fwidth(p.u), fwidth(fract(p.u + 0.5)));
    p.dlr = fwidth(p.lr);
    return p;
}

// One layer of membrane veils: n folds on a logarithmic spiral, each with its own reach, width and taper. A veil is a
// soft translucent sheet: brighter where it bellies, streaked along its length, lit on the edge that faces the pole.
// `light` is the light from the eye there (0..1). Returns colour and opacity; `near` is how close the point lies to a
// veil (for the shade it throws on the sheet behind).
vec4 veils(Polar p, float n, float twist, float turn, float width, vec2 span, float seed, float light, out float near) {
    float phi = (p.u - turn) * n + twist * p.lr;
    float x = fract(phi + 0.5) - 0.5;
    float id = wrapIndex(floor(phi + 0.5), n);
    float h1 = hash12(vec2(id, seed));
    float h2 = hash12(vec2(id, seed + 3.1));
    float h3 = hash12(vec2(id, seed + 7.7));
    float r0 = span.x + (span.y - span.x) * 0.3 * h1;
    float r1 = span.y - (span.y - span.x) * 0.35 * h2;
    float t = clamp((p.r - r0) / (r1 - r0), 0.0, 1.0);
    float taper = sqrt(smoothstep(0.0, 0.3, t) * smoothstep(1.0, 0.45, t));
    float wobble = 1.0 + 0.25 * sin(p.lr * 3.3 + h3 * TAU) + 0.1 * sin(p.lr * 9.0 + h1 * TAU);
    float w = width * (0.75 + 0.5 * h3) * wobble * taper;
    float nx = clamp(x / max(w, 1e-4), -1.0, 1.0);
    float inner = -nx * sign(twist);
    float sheet = smoothstep(1.0, 0.35, abs(nx)) * step(1e-4, w);
    near = (1.0 - smoothstep(w, w + 0.2, abs(x))) * smoothstep(0.0, 0.3, taper);

    // Streaks along the veil, in units of its half-width.
    float along = p.lr * n / (TAU * width);
    float streak = noise(vec2(along * 1.5, nx * 4.0) + id * 13.0) * 0.6 + noise(vec2(along * 4.0, nx * 9.0) + id * 5.0) * 0.4;
    vec3 col = mix(MEMBRANE * 0.42, mix(LILAC, HUNGER, 0.25) * 0.7, sqrt(1.0 - nx * nx) * (0.5 + 0.5 * streak));
    col = mix(col, HUNGER * 0.85, light * 0.6);
    float rim = smoothstep(0.45, 0.95, inner) * smoothstep(1.0, 0.85, abs(nx));
    col += mix(LILAC * 0.35, HUNGER_CORE * 0.8, light) * rim;
    return vec4(col, sheet * (0.45 + 0.25 * streak));
}

// The membrane behind the veils of a funnel: violet depth, light from the eye, veins and currents streaming in.
vec3 membrane(Polar p, float turn, float beat, float bright) {
    float r = p.r;
    vec3 col = mix(MEMBRANE * 0.42, FLESH * 0.9, smoothstep(0.08, 0.75, r));
    // Currents: thin lilac streaks along a looser spiral, flowing inward.
    float phiS = (p.u - turn) * 4.0 + 0.9 * p.lr;
    float streak = pfbm(vec2(phiS * 14.0, p.lr * 1.3), 56.0, 3);
    streak = pow(smoothstep(0.45, 0.85, streak), 2.0);
    col += LILAC * streak * 0.22 * smoothstep(0.95, 0.25, r) * smoothstep(0.0, 0.08, r);
    // Veins: ridged noise on the log-polar grid, magenta, brighter toward the eye.
    vec2 q = vec2(p.u - turn / 3.0, p.lr / TAU) * 24.0;
    float vein = 1.0 - abs(2.0 * pfbm(q, 24.0, 3) - 1.0);
    vein = smoothstep(0.8, 0.98, vein);
    col += HUNGER * vein * 0.1 * smoothstep(0.7, 0.1, r) * (0.7 + 0.3 * beat);
    // Folds of the sheet, soft light and shade.
    float fold = pfbm(vec2((p.u + turn / 3.0) * 10.0, p.lr * 1.6), 10.0, 4);
    col *= 0.7 + 0.6 * fold;
    // Light from the eye.
    col += HUNGER * exp(-r / (0.16 + 0.03 * beat)) * (0.55 + 0.25 * beat) * bright;
    col += HUNGER_CORE * exp(-r * r / (0.003 + 0.0015 * beat)) * (0.8 + 0.3 * beat) * bright;
    return col;
}

// Far shapes in the haze: a ridge line, arches and curled horns of rib, crystal-tipped spires. Returns coverage and
// writes the glow of crystal tips.
float farShapes(float u, float up, float layer, out float tip) {
    tip = 0.0;
    float cells = 7.0 + layer * 4.0;
    float scale = 1.0 - layer * 0.28;
    float ridge = -0.03 + 0.09 * scale * pfbm(vec2(u * 9.0, layer * 13.0), 9.0, 4);
    float cover = smoothstep(0.004, -0.004, up - ridge);

    float cu = u * cells;
    float cell = wrapIndex(floor(cu), cells);
    float x = fract(cu);
    float h = hash12(vec2(cell, layer * 7.3 + 1.0));
    float h2 = hash12(vec2(cell, layer * 7.3 + 2.0));
    float h3 = hash12(vec2(cell, layer * 7.3 + 3.0));
    // Width of a cell in radians, to measure shapes in the same units as `up`.
    float cellRad = TAU / cells;
    if (h < 0.62) {
        // An arch or a curled horn: part of an ellipse rising from the haze, thick at the root, thin at the top.
        vec2 radii = vec2((0.22 + 0.2 * h2) * cellRad, (0.09 + 0.16 * h3) * scale);
        vec2 c = vec2(0.5 + (h2 - 0.5) * 0.3, -0.03);
        vec2 q = vec2((x - c.x) * cellRad, up - c.y);
        q.x += q.y * (h3 - 0.5) * 1.2;
        float k0 = length(q / radii);
        float k1 = length(q / (radii * radii));
        float dist = k0 * (k0 - 1.0) / max(k1, 1e-5);
        float angle = atan(q.y, q.x) / 3.1415927;
        float horn = step(0.31, h);
        // A horn keeps only one leg and ends in a point; an arch keeps both.
        float keep = horn > 0.5 ? (h2 > 0.5 ? smoothstep(0.08, 0.32, angle) : smoothstep(0.92, 0.68, angle)) : 1.0;
        float thick = (0.022 + 0.012 * h3) * scale * mix(1.0, 0.45, smoothstep(0.0, radii.y, q.y)) * keep;
        cover = max(cover, smoothstep(thick + 0.002, thick - 0.002, abs(dist)) * step(0.0, q.y + 0.03));
    } else {
        // A spire: a thin needle, sometimes with a crystal at the tip.
        float height = (0.07 + 0.17 * h2) * scale;
        float base = (0.012 + 0.01 * h3) * scale;
        float dx = (x - 0.5 + (h3 - 0.5) * 0.4) * cellRad;
        float lean = (h2 - 0.5) * 0.25 * up;
        float t = clamp(up / height, 0.0, 1.0);
        float halfWidth = base * pow(1.0 - t, 1.4);
        cover = max(cover, smoothstep(halfWidth + 0.0015, halfWidth - 0.0015, abs(dx - lean)) * step(up, height));
        if (h3 > 0.45) {
            vec2 at = vec2(dx - (h2 - 0.5) * 0.25 * height, up - height);
            tip = exp(-dot(at, at) / (0.00002 * scale + 0.00001));
        }
    }
    return cover;
}

// Soft round motes on a grid over the sphere of directions; `rise` slides them up, a whole number of cells per cycle.
float motes(vec3 d, float density, float size, float rise, float twinkle) {
    vec3 p = d * density;
    p.y -= rise;
    vec3 cell = floor(p);
    vec3 key = vec3(cell.x, wrapIndex(cell.y, 240.0), cell.z);
    vec3 h = hash33(key + 0.5);
    if (h.x < 0.86) {
        return 0.0;
    }
    vec3 at = cell + 0.25 + 0.5 * hash33(key + 7.3);
    vec3 off = p - at;
    off -= d * dot(off, d);
    float k = exp(-dot(off, off) / (size * size));
    return k * (0.55 + 0.45 * sin(TAU * (twinkle + h.y)));
}

void main() {
    vec3 d = normalize(direction);
    float g = GameTime;
    float up = d.y;
    vec3 haze = FogColor.rgb;
    float beat = heartbeat(fract(g * 632.0));

    vec3 col = haze;

    if (up > -0.15) {
        // The funnel overhead.
        Polar p = polar(d, 1.0);
        vec3 sky = membrane(p, g * 3.0, beat, 1.0);
        float near;
        vec4 veil = veils(p, 7.0, 2.6, g * 2.0, 0.24, vec2(0.02, 0.6), 1.0, exp(-p.r / 0.12), near);
        sky *= 1.0 - 0.3 * near;
        sky = mix(sky, veil.rgb, veil.a);
        veil = veils(p, 5.0, 1.75, g, 0.28, vec2(0.06, 0.9), 2.0, exp(-p.r / 0.16), near);
        sky *= 1.0 - 0.35 * near;
        sky = mix(sky, veil.rgb, veil.a);
        veil = veils(p, 3.0, -1.15, -g, 0.22, vec2(0.18, 1.05), 3.0, exp(-p.r / 0.2) * 0.8, near);
        sky *= 1.0 - 0.35 * near;
        sky = mix(sky, veil.rgb, veil.a * 0.8);
        // The eye shines through every veil.
        sky += HUNGER_CORE * exp(-p.r * p.r / (0.0012 + 0.0008 * beat)) * 0.6;

        // Into the haze toward the horizon: a pale band of lit mist, then the fog colour itself.
        sky *= mix(vec3(1.0), clamp(haze / vec3(0.42, 0.35, 0.52), 0.4, 1.6), 0.45);
        float mistUp = pfbm(vec2((p.u + g * 2.0) * 16.0, p.lr * 6.0), 16.0, 4);
        vec3 band = mix(haze, LILAC * 0.5 + haze * 0.5, 0.12 + 0.18 * mistUp);
        sky = mix(sky, band, smoothstep(0.62, 0.97, p.r + (mistUp - 0.5) * 0.12));
        sky = mix(sky, haze, smoothstep(0.92, 1.0, p.r));
        col = sky;
    }

    // Three layers of far shapes standing in the haze, the nearest darkest, mist drifting between them.
    if (up > -0.12 && up < 0.32) {
        float u = atan(d.z, d.x) / TAU;
        for (int i = 0; i < 3; i++) {
            float layer = float(i);
            float tip;
            float cover = farShapes(u + layer * 0.137, up, layer, tip);
            vec3 shape = mix(haze, haze * 0.38 + CHITIN_SHADOW * 0.5, 0.3 + 0.3 * (2.0 - layer));
            // Lit from below by the abyss.
            shape += HUNGER * 0.08 * smoothstep(0.04, -0.06, up);
            col = mix(col, shape, cover * smoothstep(-0.12, -0.04, up));
            col += mix(LILAC, HUNGER, 0.5) * tip * (0.5 - layer * 0.1) * (0.8 + 0.2 * beat);
            float mist = pfbm(vec2((u + g * (1.0 + layer)) * 24.0, up * 22.0 + layer * 5.0), 24.0, 4);
            col = mix(col, mix(haze, LILAC * 0.6 + haze * 0.4, 0.2), smoothstep(0.42, 0.75, mist) * 0.55 * smoothstep(0.2, 0.0, abs(up - 0.01)));
        }
    }

    if (up < 0.02) {
        // The abyss: the haze thins into plum depths, then a slower funnel of glowing tissue round the heart.
        Polar p = polar(d, -1.0);
        float abeat = heartbeat(fract(g * 632.0 + 0.21));
        vec3 abyss = mix(FLESH * 1.25, FLESH_DEEP, smoothstep(0.95, 0.55, p.r));
        float phi = (p.u + g) * 6.0 - 1.4 * p.lr;
        float tissue = pow(0.5 + 0.5 * cos(TAU * phi), 3.0);
        float drift = pfbm(vec2((p.u + g) * 12.0, p.lr * 2.0), 12.0, 4);
        abyss += HUNGER * tissue * drift * 0.35 * smoothstep(0.9, 0.2, p.r);
        vec2 q = vec2(p.u - g, p.lr / TAU) * 30.0;
        float vein = smoothstep(0.82, 0.98, 1.0 - abs(2.0 * pfbm(q, 30.0, 3) - 1.0));
        abyss += HUNGER * vein * 0.1 * smoothstep(0.7, 0.1, p.r);
        abyss += HUNGER * exp(-p.r / (0.22 + 0.04 * abeat)) * (0.5 + 0.25 * abeat);
        abyss += HUNGER_CORE * exp(-p.r * p.r / (0.006 + 0.003 * abeat)) * 0.7;
        // Rising motes.
        abyss += mix(LILAC, HUNGER_CORE, 0.4) * motes(d, 70.0, 0.09, g * 240.0, g * 300.0) * 0.6;
        float into = smoothstep(0.0, 0.45, -up);
        col = mix(col, abyss, into);
    }

    // A few motes drifting across the vault, turning with it.
    float a = TAU * g * 2.0;
    vec3 turned = vec3(d.x * cos(a) - d.z * sin(a), d.y, d.x * sin(a) + d.z * cos(a));
    col += LILAC * motes(turned, 55.0, 0.07, 0.0, g * 200.0) * 0.35 * smoothstep(0.05, 0.3, up);

    // Dither against banding in the dark gradients.
    col += (hash12(gl_FragCoord.xy) - 0.5) / 255.0;
    fragColor = vec4(max(col, 0.0), 1.0);
}
