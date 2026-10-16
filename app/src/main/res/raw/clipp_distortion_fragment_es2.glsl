#version 100

precision mediump float;
uniform sampler2D uTexSampler;
uniform int uMode;
uniform float uIntensity;
uniform float uTime;
uniform float uStartMs;
uniform float uEndMs;
varying vec2 vTexSamplingCoord;

vec2 safeUv(vec2 uv) {
  return clamp(uv, vec2(0.001), vec2(0.999));
}

vec4 sampleFrame(vec2 uv) {
  return texture2D(uTexSampler, safeUv(uv));
}

float hash21(vec2 value) {
  return fract(sin(dot(value, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
  float timeMs = uTime * 1000.0;
  float active = step(uStartMs, timeMs) * (1.0 - step(uEndMs, timeMs));
  float intensity = clamp(uIntensity * active, 0.0, 1.0);
  vec2 uv = vTexSamplingCoord;

  if (active <= 0.0 || intensity <= 0.0001) {
    gl_FragColor = sampleFrame(uv);
    return;
  }

  vec4 base = sampleFrame(uv);
  vec4 color = base;

  if (uMode == 1) {
    float shift = (0.008 + 0.045 * intensity) * (0.5 + 0.5 * sin(uTime * 11.0));
    vec4 red = sampleFrame(uv + vec2(shift, 0.0));
    vec4 blue = sampleFrame(uv - vec2(shift, 0.0));
    color = vec4(red.r, base.g, blue.b, base.a);
  } else if (uMode == 2) {
    float slice = floor(uv.y * 28.0);
    float jitter = (hash21(vec2(slice, floor(uTime * 12.0))) - 0.5) * 0.22 * intensity;
    color = sampleFrame(uv + vec2(jitter, 0.0));
    float scan = step(0.92, fract(uv.y * 80.0 + uTime * 2.0));
    color.rgb = mix(color.rgb, vec3(1.0), scan * 0.22 * intensity);
  } else if (uMode == 3) {
    uv.x += sin(uv.y * 74.0 + uTime * 8.0) * 0.018 * intensity;
    color = sampleFrame(uv);
    float scan = step(0.94, fract(uv.y * 150.0));
    color.rgb *= 1.0 - scan * 0.55 * intensity;
    color.rgb += vec3(0.02, 0.08, 0.12) * intensity;
  } else if (uMode == 4) {
    vec2 block = floor(uv * vec2(14.0, 9.0));
    float randomShift = hash21(block + floor(uTime * 5.0));
    uv.x += (randomShift - 0.5) * 0.16 * intensity;
    uv.y += (hash21(block.yx + 4.0) - 0.5) * 0.03 * intensity;
    color = sampleFrame(uv);
  } else if (uMode == 5) {
    vec2 p = uv * 2.0 - 1.0;
    float radius = length(p);
    p *= 1.0 + 0.7 * intensity * radius * radius;
    color = sampleFrame(p * 0.5 + 0.5);
  } else if (uMode == 6) {
    uv.x += sin(uv.y * 42.0 + uTime * 5.0) * 0.065 * intensity;
    uv.y += sin(uv.x * 31.0 + uTime * 4.0) * 0.022 * intensity;
    color = sampleFrame(uv);
  } else if (uMode == 7) {
    float cells = mix(180.0, 12.0, intensity);
    color = sampleFrame((floor(uv * cells) + 0.5) / cells);
  } else if (uMode == 8) {
    vec2 p = uv * 2.0 - 1.0;
    float radius = length(p);
    float angle = atan(p.y, p.x);
    float segments = mix(2.0, 8.0, intensity);
    float sector = 6.2831853 / segments;
    angle = mod(angle, sector);
    angle = abs(angle - sector * 0.5);
    color = sampleFrame(vec2(cos(angle), sin(angle)) * radius * 0.5 + 0.5);
  } else if (uMode == 9) {
    vec4 sum = vec4(0.0);
    for (int index = 0; index < 5; index++) {
      float offset = (float(index) - 2.0) * 0.018 * intensity;
      sum += sampleFrame(uv + vec2(offset, 0.0));
    }
    color = sum / 5.0;
  } else if (uMode == 10) {
    vec2 center = vec2(0.5);
    vec4 sum = vec4(0.0);
    for (int index = 0; index < 5; index++) {
      float progress = float(index) / 4.0;
      sum += sampleFrame(mix(center, uv, 1.0 - progress * 0.08 * intensity));
    }
    color = sum / 5.0;
  } else if (uMode == 11) {
    float edge = smoothstep(0.28, 0.02, abs(uv.y - 0.5));
    vec4 sum = base;
    for (int index = 1; index < 4; index++) {
      float offset = float(index) * 0.012 * intensity;
      sum += sampleFrame(uv + vec2(offset, 0.0)) * edge;
      sum += sampleFrame(uv - vec2(offset, 0.0)) * edge;
    }
    color = sum / (1.0 + 6.0 * edge);
  } else if (uMode == 12) {
    float shift = 0.018 * intensity;
    vec4 red = sampleFrame(uv + vec2(shift, shift));
    vec4 blue = sampleFrame(uv - vec2(shift, shift));
    color = vec4(red.r, base.g, blue.b, base.a);
  } else if (uMode == 13) {
    vec4 red = sampleFrame(uv + vec2(0.012 * intensity, 0.0));
    vec4 blue = sampleFrame(uv - vec2(0.012 * intensity, 0.0));
    vec3 neon = vec3(red.r, base.g, blue.b) * 1.25;
    color = vec4(mix(base.rgb, neon, 0.75 * intensity), base.a);
  }

  gl_FragColor = vec4(clamp(color.rgb, 0.0, 1.0), color.a);
}
