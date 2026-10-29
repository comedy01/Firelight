#ifndef FIRELIGHT_FLICKER_GLSL
#define FIRELIGHT_FLICKER_GLSL

#ifdef FIRELIGHT_GLOBALS

float firelight_factor(vec3 pos, float blockLevel) {
    int count = int(min(FirelightInfo.z, 64.0));
    if (FirelightInfo.w != 1234.5 || count <= 0 || blockLevel < 0.5) {
        return 1.0;
    }
    float weights = 0.0;
    float sum = 0.0;
    float best = 0.0;
    for (int i = 0; i < count; i++) {
        vec4 source = FirelightSources[i];
        float level = floor(source.w / 8.0);
        vec3 offset = abs(pos - source.xyz);
        float reach = level - (offset.x + offset.y + offset.z);
        if (reach <= 0.0) {
            continue;
        }
        float weight = reach * reach;
        weights += weight;
        sum += weight * (source.w - level * 8.0 - 4.0);
        best = max(best, reach);
    }
    if (weights == 0.0) {
        return 1.0;
    }
    float share = clamp((best + 3.5 - blockLevel) * 0.5, 0.0, 1.0);
    return max(0.0, 1.0 + sum / weights * min(1.0, best / blockLevel) * share);
}

#if __VERSION__ >= 130
vec4 firelight_sample(sampler2D lightMap, ivec2 uv, vec3 pos) {
    vec2 coord = vec2(uv);
    coord.x *= firelight_factor(pos, coord.x / 16.0);
#ifdef FIRELIGHT_HALF_TEXEL
    return texture(lightMap, clamp((coord / 256.0) + 0.5 / 16.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
#else
    return texture(lightMap, clamp(coord / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
#endif
}
#endif

vec2 firelight_coord(vec2 coord, vec3 pos) {
    coord.x *= firelight_factor(pos, coord.x * 16.0);
    return coord;
}

#else

#if __VERSION__ >= 130
vec4 firelight_sample(sampler2D lightMap, ivec2 uv, vec3 pos) {
#ifdef FIRELIGHT_HALF_TEXEL
    return texture(lightMap, clamp((vec2(uv) / 256.0) + 0.5 / 16.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
#else
    return texture(lightMap, clamp(vec2(uv) / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
#endif
}
#endif

vec2 firelight_coord(vec2 coord, vec3 pos) {
    return coord;
}

#endif

#endif
