#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:globals.glsl>

// The sky of the Void. Overhead a vortex turns, slower than anything should: dark umber ribbons of chitin spiralling
// in toward a dim violet eye, cold light between them, and round the eye faint rings of rune-script, as in the
// hex-space beyond the Arcane. Toward the horizon everything sinks into the zone's haze (the fog colour), with the
// silhouettes of far ribs and spires standing in it; below, the abyss glows magenta, brighter the straighter one looks
// down. Motes drift across all of it. The heart at the eye beats like the breaches do: two beats, a long rest.

layout(location = 0) in vec3 direction;

layout(location = 0) out vec4 fragColor;

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
    for (int i = 0; i < 5; i++) {
        sum += amp * noise(p);
        p = mat2(1.6, 1.2, -1.2, 1.6) * p;
        amp *= 0.5;
    }
    return sum;
}

float heartbeat(float t) {
    float phase = fract(t / 1.9);
    return exp(-pow((phase - 0.08) / 0.045, 2.0)) + 0.65 * exp(-pow((phase - 0.27) / 0.06, 2.0));
}

void main() {
    vec3 d = normalize(direction);
    float t = GameTime * 1200.0;
    float up = d.y;
    float azimuth = atan(d.z, d.x);
    vec3 haze = FogColor.rgb;

    // The vault: deep plum at the zenith, the haze at the horizon.
    vec3 col = mix(haze, vec3(0.06, 0.03, 0.075), smoothstep(0.02, 0.85, up));

    if (up > 0.0) {
        // The vortex, in polar coordinates round the zenith.
        float r = acos(clamp(up, -1.0, 1.0)) / 1.5707963;
        float a = azimuth;
        float spin = t * 0.004;
        float spiral = sin(a * 5.0 + log(r + 0.03) * 6.5 - spin * 6.2832 + fbm(vec2(a * 2.0, r * 6.0)) * 1.4);
        float ribbon = smoothstep(0.62, 0.9, spiral) * smoothstep(0.55, 0.75, fbm(vec2(a * 6.0, r * 18.0 - spin * 4.0)) + 0.3)
                * smoothstep(1.05, 0.25, r) * smoothstep(0.03, 0.12, r);
        vec3 umber = mix(vec3(0.05, 0.035, 0.03), vec3(0.15, 0.11, 0.065), fbm(vec2(a * 4.0 - spin * 3.0, r * 12.0)));
        // Lit along the edge that faces the eye.
        float lit = smoothstep(0.82, 1.0, spiral) * smoothstep(0.9, 0.2, r);
        vec3 between = mix(vec3(0.10, 0.05, 0.15), vec3(0.27, 0.14, 0.38), fbm(vec2(a * 3.0 + spin * 2.0, r * 8.0)));
        vec3 vortex = mix(between, umber, ribbon) + vec3(0.62, 0.46, 0.28) * lit * 0.22;
        col = mix(col, vortex, smoothstep(0.05, 0.35, up));

        // The eye and its heart.
        float beat = heartbeat(t);
        float eye = exp(-r * r / (0.004 + 0.0015 * beat));
        float glow = exp(-r * r / (0.03 + 0.01 * beat));
        col += vec3(0.80, 0.20, 0.68) * glow * (0.25 + 0.2 * beat) + vec3(1.0, 0.78, 0.95) * eye * 0.5;

        // Rings of rune-script round the eye, turning against the vortex.
        for (int k = 0; k < 3; k++) {
            float fk = float(k);
            float rr = 0.14 + fk * 0.09;
            float ring = smoothstep(0.006, 0.0, abs(r - rr));
            float dashes = step(0.45, fract((a / 6.2832 + spin * (0.6 - fk * 0.2)) * (24.0 + fk * 12.0)));
            float glyph = step(0.8, hash12(vec2(floor((a / 6.2832 + spin * (0.6 - fk * 0.2)) * (24.0 + fk * 12.0)), fk)));
            col += vec3(0.62, 0.42, 0.95) * ring * mix(dashes * 0.35, 0.8, glyph) * 0.35 * smoothstep(0.5, 0.15, r);
        }
    }

    // Far ribs and spires standing in the haze at the horizon.
    float skyline = fbm(vec2(azimuth * 3.0, 0.5)) * 0.18 + fbm(vec2(azimuth * 9.0, 4.0)) * 0.06 - 0.05;
    float spires = step(0.86, noise(vec2(azimuth * 40.0, 1.0))) * 0.22 * fbm(vec2(azimuth * 40.0, 2.0));
    float shape = smoothstep(0.012, 0.0, up - max(skyline, spires));
    vec3 silhouette = mix(haze * 0.55, vec3(0.16, 0.10, 0.12), 0.35);
    col = mix(col, silhouette, shape * smoothstep(-0.12, 0.02, up));

    // Below: the abyss glows, brightest straight down, breathing with the heart.
    if (up < 0.0) {
        float down = -up;
        float beat = heartbeat(t + 0.4);
        vec3 abyss = mix(haze, vec3(0.30, 0.07, 0.24), smoothstep(0.05, 0.9, down));
        abyss += vec3(0.85, 0.25, 0.70) * pow(down, 14.0) * (0.35 + 0.18 * beat);
        float swirl = fbm(vec2(azimuth * 2.0 + t * 0.002, down * 5.0 - t * 0.01));
        abyss *= 0.8 + 0.35 * swirl;
        col = mix(col, abyss, smoothstep(0.0, 0.25, down));
    }

    // Motes drifting across the whole sky.
    vec2 cell = vec2(azimuth * 30.0 + t * 0.01, up * 30.0 + t * 0.004);
    float h = hash12(floor(cell));
    vec2 f = abs(fract(cell) - 0.5);
    float mote = step(0.985, h) * step(max(f.x, f.y), 0.08) * (0.5 + 0.5 * sin(t * (0.5 + h) + h * 40.0));
    col += vec3(0.85, 0.70, 1.0) * mote * 0.5 * (1.0 - abs(up));

    fragColor = vec4(col, 1.0);
}
