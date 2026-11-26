#version 330 core

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;
uniform mat4 InverseViewProjection;
uniform mat4 PreviousViewProjection;
uniform vec3 CameraDelta;
uniform vec3 Grade;
uniform vec2 ScreenSize;
uniform float MotionStrength;
uniform int ZeroToOne;
uniform vec4 FogStartColor;
uniform vec4 FogEndColor;
uniform float FogDistance;
uniform float Time;

float bayer2(vec2 point) {
    point = floor(point);
    return fract(point.x * 0.5 + point.y * point.y * 0.75);
}

float bayer8(vec2 point) {
    return (bayer2(point * 0.25) * 0.25 + bayer2(point * 0.5)) * 0.25
            + bayer2(point);
}

vec3 skyRay(vec2 uv) {
    vec4 clip = vec4(uv * 2.0 - 1.0,
            ZeroToOne != 0 ? 0.0001 : -0.9998, 1.0);
    vec4 point = InverseViewProjection * clip;
    return normalize(point.xyz / point.w);
}

vec3 motionBlur(vec3 source, float depth, bool sky, float dither) {
    vec4 currentClip = vec4(texCoord * 2.0 - 1.0,
            ZeroToOne != 0 ? depth : depth * 2.0 - 1.0, 1.0);
    vec4 previous;
    if (sky) {
        previous = PreviousViewProjection * vec4(skyRay(texCoord), 0.0);
    } else {
        vec4 position = InverseViewProjection * currentClip;
        if (abs(position.w) < 0.00001) return source;
        position /= position.w;
        previous = PreviousViewProjection * vec4(position.xyz + CameraDelta, 1.0);
    }
    if (previous.w <= 0.00001) return source;
    vec2 velocity = currentClip.xy - previous.xy / previous.w;
    velocity = velocity / (1.0 + length(velocity)) * MotionStrength * 0.18;
    float pixelLength = length(velocity * ScreenSize);
    if (pixelLength < 0.4) return source;
    velocity *= min(1.0, 64.0 / pixelLength);

    vec2 coordinate = texCoord - velocity * (1.5 + dither);
    vec2 doublePixel = 2.0 / ScreenSize;
    vec3 sum = vec3(0.0);
    float weight = 0.0;
    for (int i = 0; i < 13; i++) {
        vec2 sampleUv = clamp(coordinate, doublePixel, 1.0 - doublePixel);
        float sampleDepth = texture(DepthSampler, sampleUv).r;
        float sameSurface = sky == (sampleDepth < 0.00001) ? 1.0 : 0.0;
        sum += texture(SceneSampler, sampleUv).rgb * sameSurface;
        weight += sameSurface;
        coordinate += velocity / 3.0;
    }
    return weight > 0.0 ? sum / weight : source;
}

vec3 fog(vec3 color, float depth, bool sky, float dither) {
    vec3 direction;
    float amount;
    if (sky) {
        direction = skyRay(texCoord);
        float horizon = 1.0 - smoothstep(-0.05, 0.45, direction.y);
        float cover = 1.0 - smoothstep(24.0, 160.0, FogDistance);
        amount = max(horizon, cover);
    } else {
        vec4 position = InverseViewProjection * vec4(texCoord * 2.0 - 1.0,
                ZeroToOne != 0 ? depth : depth * 2.0 - 1.0, 1.0);
        if (abs(position.w) < 0.00001) return color;
        vec3 offset = position.xyz / position.w;
        float range = length(offset);
        direction = offset / max(range, 0.0001);
        amount = smoothstep(FogDistance * 0.06, FogDistance, range);
    }
    float azimuth = atan(direction.z, direction.x);
    float blend = 0.5 + direction.y * 1.6
            + 0.28 * sin(azimuth * 2.0 + Time * 0.3)
            + 0.12 * sin(azimuth * 3.0 - Time * 0.17);
    blend = smoothstep(0.0, 1.0, blend);
    vec3 tint = sqrt(mix(FogStartColor.rgb * FogStartColor.rgb, FogEndColor.rgb * FogEndColor.rgb, blend));
    amount *= mix(FogStartColor.a, FogEndColor.a, blend);
    return mix(color, tint + (dither - 0.5) / 255.0, amount);
}

void main() {
    float depth = texture(DepthSampler, texCoord).r;
    vec4 source = texture(SceneSampler, texCoord);
    vec3 color = source.rgb;
    bool sky = depth < 0.00001;
    float dither = bayer8(gl_FragCoord.xy);

    if (MotionStrength > 0.0) {
        color = motionBlur(color, depth, sky, dither);
    }
    if (FogDistance > 0.0) {
        color = fog(color, depth, sky, dither);
    }

    float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
    color = mix(vec3(luma), color, Grade.x);
    color = (color - 0.5) * Grade.y + 0.5;
    color += vec3(0.055, 0.014, -0.048) * Grade.z;
    fragColor = vec4(clamp(color, 0.0, 1.0), source.a);
}
