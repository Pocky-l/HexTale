#version 150

#moj_import <fog.glsl>

// The space behind a Hex Vault's door. UV0 = the point in the door's plane (blocks, middle = 0).
// Color: r = how far the door is open, g = how full the vault is.

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

out float vertexDistance;
out vec3 viewPos;
out vec2 localUv;
out vec2 openFilled;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexDistance = fog_distance(Position, FogShape);
    viewPos = Position;
    localUv = UV0;
    openFilled = Color.rg;
}
