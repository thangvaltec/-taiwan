precision mediump float;
//varying vec4 varyingPointColor;
uniform vec4 pointColor;

void main() {
  gl_FragColor = pointColor;
}