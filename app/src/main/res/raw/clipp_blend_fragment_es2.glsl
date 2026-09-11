#version 100

precision mediump float;
uniform sampler2D uVideoTexSampler;
uniform sampler2D uOverlayTexSampler;
uniform float uOverlayAlphaScale;
uniform int uBlendMode;
varying vec2 vVideoTexSamplingCoord;
varying vec2 vOverlayTexSamplingCoord;

vec3 blendColor(vec3 base, vec3 overlay) {
  if (uBlendMode == 1) {
    return base * overlay;
  } else if (uBlendMode == 2) {
    return 1.0 - (1.0 - base) * (1.0 - overlay);
  } else if (uBlendMode == 3) {
    return mix(
      2.0 * base * overlay,
      1.0 - 2.0 * (1.0 - base) * (1.0 - overlay),
      step(0.5, base)
    );
  } else if (uBlendMode == 4) {
    return (1.0 - overlay) * base * base + overlay * (1.0 - (1.0 - base) * (1.0 - base));
  } else if (uBlendMode == 5) {
    return mix(
      2.0 * base * overlay,
      1.0 - 2.0 * (1.0 - base) * (1.0 - overlay),
      step(0.5, overlay)
    );
  } else if (uBlendMode == 6) {
    return abs(base - overlay);
  } else if (uBlendMode == 7) {
    return min(base + overlay, vec3(1.0));
  }
  return overlay;
}

void main() {
  vec4 base = texture2D(uVideoTexSampler, vVideoTexSamplingCoord);
  vec2 overlayUv = vOverlayTexSamplingCoord;
  float inside = step(0.0, overlayUv.x) * step(overlayUv.x, 1.0) *
      step(0.0, overlayUv.y) * step(overlayUv.y, 1.0);
  float alpha = inside * uOverlayAlphaScale;
  if (alpha <= 0.0001) {
    gl_FragColor = base;
    return;
  }

  vec4 overlay = texture2D(uOverlayTexSampler, clamp(overlayUv, 0.0, 1.0));
  float sourceAlpha = clamp(overlay.a * alpha, 0.0, 1.0);
  vec3 blended = blendColor(base.rgb, overlay.rgb);
  gl_FragColor = vec4(mix(base.rgb, blended, sourceAlpha), base.a);
}
