attribute vec4 position;

uniform mat4 mvpMatrix;
uniform float radius;

void main() {
  gl_PointSize = 2.0 * radius;
  gl_Position = mvpMatrix * position;
}