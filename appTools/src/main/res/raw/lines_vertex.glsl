attribute vec4 position;
//attribute vec4 pointColor;
uniform float radius;

//varying vec4 varyingPointColor;

void main() {
//  varyingPointColor = pointColor;
  gl_PointSize = 2.0*radius;
  gl_Position = position;
}