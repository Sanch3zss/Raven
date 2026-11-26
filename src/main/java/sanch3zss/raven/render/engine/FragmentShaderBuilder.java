package sanch3zss.raven.render.engine;

import java.nio.charset.StandardCharsets;

import org.lwjgl.opengl.GL33;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.resource.*;
import sanch3zss.raven.event.ClientListener;
import sanch3zss.raven.event.client.ClientInitEvent;
import sanch3zss.raven.render.shader.ShaderProgram;

public class FragmentShaderBuilder implements ClientListener {
    public static final String SHADER_HEADER = "#version 330 core\n\n#define COLOR_MODE 0\n#define TEXTURE_MODE 1\n#define ROUNDED_RECTANGLE_MODE 2\n#define ROUNDED_TEXTURE_MODE 3\n#define BLUR 4\n#define CHECKER_MODE 5\n#define CIRCLE_MODE 6\n#define OUTER_MASK 7\n#define ALPHA_MASK 8\n#define MSDF_FONT 9\n#define RADIAL_ROUNDED_RECTANGLE_MODE 10\n\nin vec2 meshPosition;\nin vec2 meshSize;\nin vec2 texCoord;\nin vec4 radius;\nin vec4 color;\nin vec4 outlineColor;\nin float thickness;\nin float softness;\n\nflat in int texIndex;\nflat in int drawMode;\nflat in int maskIndex;\n\nout vec4 fragColor;\n\n";
    public static final String SHADER_FUNCTIONS = "uniform float squircle;\nuniform float alphaClip;\n\nfloat sdShape(in vec2 point, in vec2 halfSize, in vec4 radii, bool smoothCorners) {\n    float maxRadius = max(min(halfSize.x, halfSize.y), 0.0);\n    radii = clamp(radii, vec4(0.0), vec4(maxRadius));\n    radii.xy = (point.x > 0.0) ? radii.xy : radii.zw;\n    float cornerRadius = (point.y > 0.0) ? radii.x : radii.y;\n    if (smoothCorners && squircle > 0.5 && cornerRadius > 0.0) {\n        float smoothRadius = min(maxRadius, cornerRadius * 1.8);\n        float power = clamp(-0.6931 / log(max(1.0 - 0.2929 * cornerRadius / smoothRadius, 0.0001)), 2.0, 64.0);\n        vec2 sq = abs(point) - halfSize + smoothRadius;\n        vec2 corner = max(sq, 0.0) / smoothRadius;\n        float superellipse = pow(pow(corner.x, power) + pow(corner.y, power), 1.0 / power) * smoothRadius;\n        return min(max(sq.x, sq.y), 0.0) + superellipse - smoothRadius;\n    }\n    vec2 q = abs(point) - halfSize + cornerRadius;\n    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - cornerRadius;\n}\n\nfloat sdRoundedBox(in vec2 point, in vec2 halfSize, in vec4 radii) {\n    return sdShape(point, halfSize, radii, true);\n}\n\nfloat sdCircularBox(in vec2 point, in vec2 halfSize, in vec4 radii) {\n    return sdShape(point, halfSize, radii, false);\n}\n\nfloat roundedCoverage(float distanceToEdge) {\n    float antialias = max(fwidth(distanceToEdge), 0.75);\n    return 1.0 - smoothstep(-antialias, antialias, distanceToEdge);\n}\n\nfloat median(vec3 color) {\n    return max(min(color.r, color.g), min(max(color.r, color.g), color.b));\n}\nvec4 sampleTex(int id, vec2 uv);\nivec2 sampleTexSize(int id);\n\n";
    public static final String SHADOW_CASE = "        case SHADOW_MODE: {\n            float glowRadius = max(softness, 0.5);\n            float shadowExtent = max(thickness, 0.0);\n            vec2 local = gl_FragCoord.xy - meshPosition;\n            vec2 panelSize = max(meshSize - vec2(shadowExtent * 2.0), vec2(1.0));\n            vec2 panelCenter = meshSize * 0.5;\n            float dist = sdCircularBox(local - panelCenter, panelSize * 0.5, radius);\n            float glow = 1.0 - smoothstep(-glowRadius, glowRadius, dist);\n            fragColor = vec4(color.rgb, color.a * glow);\n            break;\n        }\n";
    public static final String HORIZONTAL_GRADIENT_CASE = "        case HORIZONTAL_GRADIENT_MODE: {\n            vec2 center = gl_FragCoord.xy - meshPosition - (meshSize / 2.0);\n            float dist = sdRoundedBox(center, meshSize / 2.0, radius);\n            float alpha = 1.0 - smoothstep(-1.0, 1.0, dist);\n            float blend = smoothstep(thickness, softness, texCoord.x);\n            vec4 gradientColor = mix(color, outlineColor, blend);\n            fragColor = vec4(gradientColor.rgb, gradientColor.a * alpha);\n            break;\n        }\n";
    public static final String GLASS_PANEL_CASE = "        case GLASS_PANEL_MODE: {\n            vec2 local = gl_FragCoord.xy - meshPosition;\n            vec2 halfSize = meshSize * 0.5;\n            float dist = sdRoundedBox(local - halfSize, halfSize, radius);\n            float panelCoverage = roundedCoverage(dist);\n\n            float panelAlpha = clamp(color.a, 0.0, 1.0);\n            float tintStrength = clamp(texCoord.y, 0.0, 1.0);\n            vec3 blurred = sampleTex(texIndex, gl_FragCoord.xy / resolution).rgb;\n            vec3 panelColor = mix(blurred, color.rgb, tintStrength);\n            fragColor = vec4(panelColor, panelAlpha * panelCoverage);\n            break;\n        }\n";
    public static final String GLASS_RING_CASE = "        case GLASS_RING_MODE: {\n            vec2 halfSize = meshSize * 0.5;\n            vec2 center = gl_FragCoord.xy - meshPosition - halfSize;\n            float distanceFromCenter = length(center);\n            float outerRadius = radius.x;\n            float innerRadius = max(outerRadius - thickness, 0.0);\n            float dist = max(distanceFromCenter - outerRadius, innerRadius - distanceFromCenter);\n            float panelCoverage = roundedCoverage(dist);\n\n            float panelAlpha = clamp(color.a, 0.0, 1.0);\n            float tintStrength = clamp(texCoord.y, 0.0, 1.0);\n            vec3 blurred = sampleTex(texIndex, gl_FragCoord.xy / resolution).rgb;\n            vec3 panelColor = mix(blurred, color.rgb, tintStrength);\n            fragColor = vec4(panelColor, panelAlpha * panelCoverage);\n            break;\n        }\n";
    public static final String CHEVRON_CASE = """
        case CHEVRON_MODE: {
            vec2 p = gl_FragCoord.xy - meshPosition;
            vec2 size = meshSize;
            float halfWidth = thickness * 0.5;
            vec2 top = vec2(halfWidth, size.y - halfWidth);
            vec2 tip = vec2(size.x - halfWidth, size.y * 0.5);
            vec2 bottom = vec2(halfWidth, halfWidth);
            if (softness > 2.5) {
                top = vec2(halfWidth, size.y * 0.5);
                tip = vec2(size.x * 0.38, halfWidth);
                bottom = vec2(size.x - halfWidth, size.y - halfWidth);
            } else if (softness > 0.5) {
                p = p.yx;
                size = size.yx;
                if (softness > 1.5) p.x = size.x - p.x;
                top = vec2(halfWidth, size.y - halfWidth);
                tip = vec2(size.x - halfWidth, size.y * 0.5);
                bottom = vec2(halfWidth, halfWidth);
            }
            vec2 upper = tip - top;
            vec2 lower = bottom - tip;
            float upperDist = length(p - top - upper * clamp(dot(p - top, upper) / dot(upper, upper), 0.0, 1.0));
            float lowerDist = length(p - tip - lower * clamp(dot(p - tip, lower) / dot(lower, lower), 0.0, 1.0));
            float dist = min(upperDist, lowerDist) - halfWidth;
            float antialias = max(fwidth(dist), 0.85);
            float alpha = 1.0 - smoothstep(-antialias, antialias, dist);
            fragColor = vec4(color.rgb, color.a * alpha);
            break;
        }
""";
    public static final String COLOR_WELL_CASE = """
        case COLOR_WELL_MODE: {
            vec2 halfSize = meshSize * 0.5;
            vec2 p = gl_FragCoord.xy - meshPosition - halfSize;
            float len = length(p);
            float outer = min(halfSize.x, halfSize.y);
            float ring = outer * texCoord.x;
            float inner = outer - ring - outer * texCoord.y;
            float aa = max(fwidth(len), 0.75);
            float ringAlpha = (1.0 - smoothstep(outer - aa, outer, len)) * smoothstep(outer - ring - aa, outer - ring, len);
            float innerAlpha = 1.0 - smoothstep(inner - aa, inner, len);
            float hue = fract(atan(p.x, p.y) / 6.2831853 + 0.5);
            vec3 rainbow = clamp(abs(mod(hue * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);
            rainbow = mix(vec3(1.0), rainbow, 0.8);
            float ringA = outlineColor.a * ringAlpha;
            float innerA = color.a * innerAlpha;
            float alpha = ringA + innerA;
            if (alpha <= 0.001) discard;
            fragColor = vec4((rainbow * ringA + color.rgb * innerA) / alpha, min(alpha, 1.0));
            break;
        }
""";
    public static final String SATURATION_VALUE_CASE = """
        case SATURATION_VALUE_MODE: {
            vec2 local = gl_FragCoord.xy - meshPosition;
            vec2 halfSize = meshSize * 0.5;
            float coverage = roundedCoverage(sdRoundedBox(local - halfSize, halfSize, radius));
            vec2 sv = clamp(local / max(meshSize, vec2(1.0)), 0.0, 1.0);
            vec3 rgb = mix(vec3(1.0), color.rgb, sv.x) * sv.y;
            fragColor = vec4(rgb, color.a * coverage);
            break;
        }
""";
    public static final String SKIN_FACE_CASE = """
        case SKIN_FACE_MODE: {
            vec4 face = sampleTex(texIndex, texCoord);
            vec4 hat = sampleTex(texIndex, texCoord + vec2(thickness, 0.0));
            vec2 halfSize = meshSize * 0.5;
            float coverage = roundedCoverage(sdRoundedBox(gl_FragCoord.xy - meshPosition - halfSize, halfSize, radius));
            vec3 rgb = mix(face.rgb, hat.rgb, hat.a);
            fragColor = vec4(rgb * color.rgb, max(face.a, hat.a) * color.a * coverage);
            break;
        }
""";
    public static final String GLYPH_TEXTURE_CASE = "        case GLYPH_TEXTURE_MODE: {\n            float coverage = sampleTex(texIndex, texCoord).r;\n            if (coverage <= 0.001) discard;\n            fragColor = vec4(color.rgb, color.a * coverage);\n            break;\n        }\n";
    private static final String SMOOTH_ROUNDED_RECTANGLE_CASE = """
        case ROUNDED_RECTANGLE_MODE:
        {
            vec2 halfSize = meshSize * 0.5;
            vec2 point = gl_FragCoord.xy - meshPosition - halfSize;
            float outerDistance = sdRoundedBox(point, halfSize, radius);
            float outerCoverage = roundedCoverage(outerDistance);

            if (thickness > 0.001) {
                float gradient = max(length(vec2(dFdx(outerDistance), dFdy(outerDistance))), 0.5);
                float innerDistance = outerDistance / gradient + thickness;
                float innerCoverage = 1.0 - smoothstep(-0.75, 0.75, innerDistance);
                vec4 composed = mix(outlineColor, color, innerCoverage);
                fragColor = vec4(composed.rgb, composed.a * outerCoverage);
            } else {
                fragColor = vec4(color.rgb, color.a * outerCoverage);
            }
            break;
        }
""".indent(4);
    private static final String ORIGINAL_BLUR_CASE = "        case BLUR:\n        {\n            vec2 pos = gl_FragCoord.xy;\n            vec2 blurredPos = pos / resolution;\n            vec4 textureColor = sampleTex(texIndex, blurredPos);\n            vec3 blurredColor = textureColor.rgb;\n\n            vec2 center = pos - meshPosition - (meshSize / 2.0);\n            float dist = sdRoundedBox(center, meshSize / 2.0, radius);\n            float alpha = 1.0 - smoothstep(-softness, softness, dist);\n\n            fragColor = vec4(blurredColor.rgb * color.rgb, color.a * alpha);\n            break;\n        }\n";
    private static final String KAWASE_BLUR_CASE = "        case BLUR:\n        {\n            vec2 pos = gl_FragCoord.xy;\n            vec2 blurredPos = pos / resolution;\n            vec4 textureColor = sampleTex(texIndex, blurredPos);\n\n            vec2 center = pos - meshPosition - (meshSize / 2.0);\n            float dist = sdRoundedBox(center, meshSize / 2.0, radius);\n            float antialias = max(fwidth(dist), 0.7);\n            float alpha = 1.0 - smoothstep(-antialias, antialias, dist);\n\n            fragColor = vec4(textureColor.rgb * color.rgb, color.a * alpha);\n            break;\n        }\n";
    private static final String ORIGINAL_SDF_ANTIALIAS = "float smoothedAlpha = 1.0 - smoothstep(-1.0, thickness > 0. ? 1. : softness + 1., dist);\n            float smoothedborderAlpha = (1.0 - smoothstep(-softness, softness, dist));\n            float borderAlpha = 1.0 - smoothstep(thickness - 2.0, thickness, abs(dist));";
    private static final String DERIVATIVE_SDF_ANTIALIAS = "float antialias = max(fwidth(dist), 0.7);\n            float smoothedAlpha = 1.0 - smoothstep(-antialias, thickness > 0.0 ? antialias : softness + antialias, dist);\n            float edgeSoftness = max(softness, antialias);\n            float smoothedborderAlpha = 1.0 - smoothstep(-edgeSoftness, edgeSoftness, dist);\n            float borderAlpha = thickness > 0.0\n                ? 1.0 - smoothstep(max(thickness - antialias * 2.0, 0.0), max(thickness, antialias), abs(dist))\n                : 0.0;";
    public static final String SHADER_MAIN = "void main() {\n    switch (drawMode) {\n        case COLOR_MODE:\n        {\n            fragColor = color;\n            break;\n        }\n        case TEXTURE_MODE:\n        {\n            vec4 textureColor = sampleTex(texIndex, texCoord);\n\n            if (textureColor.a == 0.0) {\n                discard;\n            }\n\n            fragColor = textureColor * color;\n            break;\n        }\n        case ROUNDED_RECTANGLE_MODE:\n        {\n            vec2 center = gl_FragCoord.xy - meshPosition - (meshSize / 2.0);\n            float dist = sdRoundedBox(center, meshSize / 2.0, radius);\n            float smoothedAlpha = 1.0 - smoothstep(-1.0, thickness > 0. ? 1. : softness + 1., dist);\n            float smoothedborderAlpha = (1.0 - smoothstep(-softness, softness, dist));\n            float borderAlpha = 1.0 - smoothstep(thickness - 2.0, thickness, abs(dist));\n\n            if (smoothedAlpha < 0.49 && thickness > 0.) {\n                fragColor = vec4(outlineColor.rgb, outlineColor.a * smoothedborderAlpha);\n            } else {\n                vec4 basicColor = vec4(color.rgb, color.a * smoothedAlpha);\n\n                fragColor = mix(vec4(color.rgb, 0.), mix(basicColor, thickness > 0. ? outlineColor : basicColor,\n                outlineColor.a *  borderAlpha), smoothedAlpha);\n            }\n            break;\n        }\n        case ROUNDED_TEXTURE_MODE:\n        {\n            vec4 textureColor = sampleTex(texIndex, texCoord);\n            vec4 textureColorMultipliedByInputColor = textureColor * color;\n\n            vec2 center = gl_FragCoord.xy - meshPosition - (meshSize / 2.0);\n            float dist = sdRoundedBox(center, meshSize / 2.0, radius);\n            float alpha = 1.0 - smoothstep(-1.0, 1.0, dist);\n\n            fragColor = vec4(textureColorMultipliedByInputColor.rgb, textureColorMultipliedByInputColor.a * alpha);\n            break;\n        }\n        case BLUR:\n        {\n            vec2 pos = gl_FragCoord.xy;\n            vec2 blurredPos = pos / resolution;\n            vec4 textureColor = sampleTex(texIndex, blurredPos);\n            vec3 blurredColor = textureColor.rgb;\n\n            vec2 center = pos - meshPosition - (meshSize / 2.0);\n            float dist = sdRoundedBox(center, meshSize / 2.0, radius);\n            float alpha = 1.0 - smoothstep(-softness, softness, dist);\n\n            fragColor = vec4(blurredColor.rgb * color.rgb, color.a * alpha);\n            break;\n        }\n        case CHECKER_MODE: {\n            vec2 local = gl_FragCoord.xy - meshPosition;\n\n            float rows = 2.0;\n\n            float cellSize = min(meshSize.x, meshSize.y) / rows;\n\n            ivec2 cell = ivec2(floor(local / cellSize));\n\n            bool isWhite = (cell.x + cell.y) % 2 == 0;\n\n            vec3 checkerColor = isWhite ? color.rgb : outlineColor.rgb;\n            float alpha = isWhite ? color.a : outlineColor.a;\n\n            vec2 center = local - (meshSize * 0.5);\n            float dist = sdRoundedBox(center, meshSize * 0.5, radius);\n            float mask = 1.0 - smoothstep(-softness, softness, dist);\n\n            fragColor = vec4(checkerColor, alpha * mask);\n            break;\n        }\n        case CIRCLE_MODE:{\n            vec2 center = gl_FragCoord.xy - meshPosition - (meshSize / 2.0);\n            float len = length(center);\n            float dist = len - radius.x;\n            if (radius.y > 0.0) {\n                dist = max(dist, radius.y - len);\n            }\n            float smoothedAlpha = 1.0 - smoothstep(-1.0, thickness > 0. ? 1. : softness + 1., dist);\n            float smoothedborderAlpha = (1.0 - smoothstep(-softness, softness, dist));\n            float borderAlpha = 1.0 - smoothstep(thickness - 2.0, thickness, abs(dist));\n\n            float arcAlpha = 1.0;\n            float span = texCoord.y - texCoord.x;\n            if (span < 6.2831) {\n                span = mod(span, 6.28318530718);\n                float local = mod(atan(center.y, center.x) - texCoord.x, 6.28318530718);\n                float edge = local <= span ? -min(local, span - local) : min(local - span, 6.28318530718 - local);\n                arcAlpha = clamp(0.5 - edge * max(len, 1.0), 0.0, 1.0);\n                if (arcAlpha <= 0.0) discard;\n            }\n\n            if (smoothedAlpha < 0.49 && thickness > 0.) {\n                fragColor = vec4(outlineColor.rgb, outlineColor.a * smoothedborderAlpha * arcAlpha);\n            } else {\n                vec4 basicColor = vec4(color.rgb, color.a * smoothedAlpha);\n\n                fragColor = mix(vec4(color.rgb, 0.), mix(basicColor, thickness > 0. ? outlineColor : basicColor,\n                outlineColor.a *  borderAlpha), smoothedAlpha);\n                fragColor.a *= arcAlpha;\n            }\n            break;\n        }\n        case OUTER_MASK: {\n            vec4 original = sampleTex(texIndex, texCoord);\n            float mask = sampleTex(maskIndex, texCoord).a;\n\n            vec3 rgb = original.rgb * (1.0 - mask);\n            float a  = original.a   * (1.0 - mask);\n\n            fragColor = vec4(rgb, a);\n            break;\n        }\n        case ALPHA_MASK: {\n            vec4 textureColor = sampleTex(texIndex, texCoord);\n            textureColor.a *= color.a;\n            fragColor = textureColor;\n            break;\n        }\n        case MSDF_FONT: {\n            float dist = median(sampleTex(texIndex, texCoord).rgb) - 0.5 + thickness;\n            vec2 h = vec2(dFdx(texCoord.x), dFdy(texCoord.y)) * vec2(sampleTexSize(texIndex));\n            float pixels = 10 * inversesqrt(h.x * h.x + h.y * h.y);\n            float alpha = smoothstep(-softness, softness, dist * pixels);\n            vec4 outColor = vec4(color.rgb, color.a * alpha);\n            fragColor = outColor;\n            break;\n        }\n        case RADIAL_ROUNDED_RECTANGLE_MODE: {\n            vec2 center = gl_FragCoord.xy - meshPosition - (meshSize / 2.0);\n            float dist = sdRoundedBox(center, meshSize / 2.0, radius);\n            float alpha = 1.0 - smoothstep(-1.0, 1.0, dist);\n            vec2 normalized = center / (meshSize / 2.0);\n            float radial = clamp(length(normalized), 0.0, 1.0);\n            vec4 gradientColor = mix(color, outlineColor, radial);\n\n            fragColor = vec4(gradientColor.rgb, gradientColor.a * alpha);\n            break;\n        }\n    }\n}\n";

    public FragmentShaderBuilder() {
        Raven.INSTANCE.eventDispatcher().register(ClientInitEvent.class, event -> {
            int iMax = Math.min(32, Math.max(1, GL33.glGetInteger(34930)));
            Raven.INSTANCE.drawEngine(new DrawEngine(new ShaderProgram(new ByteArrayResource(buildSource(iMax).getBytes(StandardCharsets.UTF_8)), new ResourceRouter("/", ClasspathResource::new).route("shaders/core/ui.vsh")), iMax));
            Raven.INSTANCE.windowController().init();
            Raven.INSTANCE.widgetStack().init();
        });
    }

    public static String buildSource(int i) {
        String header = SHADER_HEADER.replace("#define RADIAL_ROUNDED_RECTANGLE_MODE 10", "#define RADIAL_ROUNDED_RECTANGLE_MODE 10\n#define SHADOW_MODE 11\n#define HORIZONTAL_GRADIENT_MODE 12\n#define GLASS_PANEL_MODE 13\n#define GLASS_RING_MODE 15\n#define GLYPH_TEXTURE_MODE 16\n#define CHEVRON_MODE 17\n#define COLOR_WELL_MODE 18\n#define SATURATION_VALUE_MODE 19\n#define SKIN_FACE_MODE 20");
        StringBuilder sb = new StringBuilder(header);
        sb.append("uniform sampler2D textureSampler[").append(i).append("];\n").append("uniform vec2 resolution;\n\n").append(SHADER_FUNCTIONS);
        appendSampleTex(sb, i);
        appendSampleTexSize(sb, i);
        String shaderMain = replaceCase(SHADER_MAIN,
                        "        case ROUNDED_RECTANGLE_MODE:",
                        "        case ROUNDED_TEXTURE_MODE:",
                        SMOOTH_ROUNDED_RECTANGLE_CASE)
                .replace(ORIGINAL_BLUR_CASE, KAWASE_BLUR_CASE)
                .replace(ORIGINAL_SDF_ANTIALIAS, DERIVATIVE_SDF_ANTIALIAS);
        if (shaderMain.equals(SHADER_MAIN)) {
            throw new IllegalStateException("Unable to replace blur fragment shader");
        }
        if (shaderMain.contains(ORIGINAL_SDF_ANTIALIAS)) {
            throw new IllegalStateException("Unable to replace SDF antialiasing");
        }
        if (!shaderMain.contains("float outerDistance = sdRoundedBox")) {
            throw new IllegalStateException("Unable to replace rounded rectangle fragment shader");
        }
        int insertionPoint = shaderMain.lastIndexOf("    }\n}\n");
        if (insertionPoint < 0) {
            throw new IllegalStateException("Unable to extend core fragment shader");
        }
        sb.append(shaderMain, 0, insertionPoint).append(SHADOW_CASE).append(HORIZONTAL_GRADIENT_CASE)
                .append(GLASS_PANEL_CASE)
                .append(GLASS_RING_CASE)
                .append(GLYPH_TEXTURE_CASE)
                .append(CHEVRON_CASE)
                .append(COLOR_WELL_CASE)
                .append(SATURATION_VALUE_CASE)
                .append(SKIN_FACE_CASE)
                .append("    }\n    if (fragColor.a < color.a * alphaClip) discard;\n}\n");
        return sb.toString();
    }

    private static String replaceCase(String source, String startMarker, String endMarker,
                                      String replacement) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start + startMarker.length());
        if (start < 0 || end < 0 || end <= start) {
            throw new IllegalStateException("Unable to locate fragment shader case: " + startMarker);
        }
        return source.substring(0, start) + replacement + source.substring(end);
    }

    public static void appendSampleTex(StringBuilder sb, int i) {
        sb.append("vec4 sampleTex(int id, vec2 uv) {\n");
        for (int i2 = 0; i2 < i; i2++) {
            sb.append("    if (id == ").append(i2).append(") return texture(textureSampler[").append(i2).append("], uv);\n");
        }
        sb.append("    return vec4(1.0, 0.0, 1.0, 1.0);\n").append("}\n\n");
    }

    public static void appendSampleTexSize(StringBuilder sb, int i) {
        sb.append("ivec2 sampleTexSize(int id) {\n");
        for (int i2 = 0; i2 < i; i2++) {
            sb.append("    if (id == ").append(i2).append(") return textureSize(textureSampler[").append(i2).append("], 0);\n");
        }
        sb.append("    return ivec2(1, 1);\n").append("}\n\n");
    }
}
