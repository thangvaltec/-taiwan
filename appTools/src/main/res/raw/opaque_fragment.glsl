varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;

void main() {
    highp vec4 originColor = texture2D(inputImageTexture, textureCoordinate);
    gl_FragColor = vec4(originColor.rgb, 1);
}