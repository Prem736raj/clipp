#version 100

attribute vec4 aFramePosition;
uniform mat4 uOverlayTransformationMatrix;
varying vec2 vVideoTexSamplingCoord;
varying vec2 vOverlayTexSamplingCoord;

vec2 getTexSamplingCoord(vec2 ndcPosition) {
  return vec2(ndcPosition.x * 0.5 + 0.5, ndcPosition.y * 0.5 + 0.5);
}

void main() {
  gl_Position = aFramePosition;
  vVideoTexSamplingCoord = getTexSamplingCoord(aFramePosition.xy);
  vec4 overlayPosition = uOverlayTransformationMatrix * aFramePosition;
  vOverlayTexSamplingCoord = getTexSamplingCoord(overlayPosition.xy);
}
