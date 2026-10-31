#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

// The Void's sky: a box round the camera; the fragment shader paints by the direction it looks along.

in vec3 Position;

out vec3 direction;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    direction = Position;
}
