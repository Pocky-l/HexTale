#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

// The Void's sky drawn over a shader pack's finished picture: the same box as void_sky.vsh, pushed onto the far plane
// (depth 1), so with a "less or equal" test it only lands where the world left the sky open.

in vec3 Position;

out vec3 direction;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    gl_Position.z = gl_Position.w;
    direction = Position;
}
