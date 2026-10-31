#version 150

// The Void's sky: a box round the camera; the fragment shader paints by the direction it looks along. The box is
// pinned onto the far plane (depth 1 here), so its size never clips it against the render distance.

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 direction;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    gl_Position.z = gl_Position.w;
    direction = Position;
}
