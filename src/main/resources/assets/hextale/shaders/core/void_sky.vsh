#version 150

// The Void's sky: a box round the camera; the fragment shader paints by the direction it looks along.

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 direction;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    direction = Position;
}
