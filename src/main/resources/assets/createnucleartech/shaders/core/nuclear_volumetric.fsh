#version 150

uniform mat4 InvViewProjMat;
uniform vec3 ExplosionCenter;
uniform float ExplosionTime;
uniform float ExplosionPower;
uniform vec2 ScreenSize;
uniform sampler2D DepthTex;

out vec4 fragColor;

// ---- Noise ----
float hash31(vec3 p) {
    p = fract(p * vec3(443.897, 397.297, 491.187));
    p += dot(p.zxy, p.yxz + 19.19);
    return fract(p.x * p.y * p.z);
}

float valueNoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash31(i);
    float b = hash31(i + vec3(1,0,0));
    float c = hash31(i + vec3(0,1,0));
    float d = hash31(i + vec3(1,1,0));
    float e = hash31(i + vec3(0,0,1));
    float f2 = hash31(i + vec3(1,0,1));
    float g = hash31(i + vec3(0,1,1));
    float h = hash31(i + vec3(1,1,1));
    return mix(mix(mix(a,b,f.x), mix(c,d,f.x), f.y),
               mix(mix(e,f2,f.x), mix(g,h,f.x), f.y), f.z);
}

float fbm3(vec3 p) {
    float val = 0.0;
    float a = 0.5;
    for (int i = 0; i < 2; i++) { val += valueNoise(p) * a; p *= 2.0; a *= 0.5; }
    return val;
}

// ---- AABB intersection ----
bool intersectAABB(vec3 o, vec3 d, vec3 bmin, vec3 bmax, out float tN, out float tF) {
    vec3 invD = 1.0 / d;
    vec3 t0 = (bmin - o) * invD;
    vec3 t1 = (bmax - o) * invD;
    vec3 mn = min(t0, t1);
    vec3 mx = max(t0, t1);
    tN = max(max(mn.x, mn.y), mn.z);
    tF = min(min(mx.x, mx.y), mx.z);
    tN = max(tN, 0.0);
    return tN <= tF;
}

// ---- Density functions ----
// Noise args are divided by s so patterns scale with cloud size,
// preventing aliasing at larger step sizes and keeping the visual look consistent.

float fireballDensity(vec3 p, float t, float s) {
    if (t > 4.0) return 0.0;
    float r = (5.0 + t * 28.0) * s;
    float rise = t * 35.0 * s;
    vec3 c = ExplosionCenter + vec3(0.0, rise, 0.0);
    float dist = length(p - c);
    float d = smoothstep(r, r * 0.15, dist);
    d *= max(0.0, 1.0 - t / 4.0);
    d *= (0.7 + 0.3 * valueNoise((p * 0.08 + t * 0.5) / s));
    return d;
}

float stemDensity(vec3 p, float t, float s) {
    if (t < 0.4) return 0.0;
    float stemH = min((t - 0.4) * 18.0, 120.0) * s;
    if (stemH < 1.0) return 0.0;
    float baseW = 8.0 * s;
    float topW = 14.0 * s;
    vec3 rel = p - ExplosionCenter;
    float hFrac = clamp(rel.y / stemH, 0.0, 1.0);
    if (rel.y < -1.0 || rel.y > stemH + 2.0) return 0.0;
    float w = mix(baseW, topW, hFrac);
    float distA = length(rel.xz);
    float d = smoothstep(w, w * 0.05, distA);
    float n = fbm3((p * 0.06 + vec3(0.0, -t * 1.5, 0.0)) / s);
    d *= (0.5 + 0.5 * n);
    d *= max(0.0, 1.0 - smoothstep(8.0, 22.0, t));
    return d * 0.85;
}

float capDensity(vec3 p, float t, float s) {
    if (t < 1.5) return 0.0;
    float ct = t - 1.5;
    float capH = ExplosionCenter.y + min(120.0 * s, (35.0 + ct * 10.0) * s);
    float majR = min(55.0 * s, (6.0 + ct * 6.0) * s);
    float minR = min(majR * 0.45, (4.0 + ct * 2.0) * s);
    vec3 rel = p - vec3(ExplosionCenter.x, capH, ExplosionCenter.z);
    float distA = length(rel.xz);
    // Torus
    float td = length(vec2(distA - majR, rel.y)) - minR;
    float d = smoothstep(4.0 * s, -1.5, td);
    // Top dome
    float domeR = majR * 0.6;
    float domeH = minR * 1.2;
    if (domeH > 0.5) {
        vec3 dr = vec3(rel.x, (rel.y - domeH) * (domeR / max(domeH, 0.1)), rel.z);
        d = max(d, smoothstep(domeR, domeR * 0.15, length(dr)));
    }
    float n = fbm3((p * 0.035 + vec3(t * 0.2, -t * 0.4, t * 0.15)) / s);
    d *= (0.4 + 0.6 * n);
    d *= max(0.0, 1.0 - smoothstep(10.0, 25.0, t));
    return d;
}

float shockwaveDensity(vec3 p, float t, float s) {
    if (t > 6.0) return 0.0;
    float ringR = t * 50.0 * s;
    float wallW = 12.0 * s;
    float wallH = 18.0 * s;
    vec3 rel = p - ExplosionCenter;
    if (rel.y < -2.0 || rel.y > wallH) return 0.0;
    float dist = length(rel.xz);
    float rd = abs(dist - ringR);
    float d = smoothstep(wallW, 0.0, rd);
    d *= max(0.0, 1.0 - rel.y / wallH);
    d *= max(0.0, 1.0 - smoothstep(3.0, 6.0, t));
    d *= (0.6 + 0.4 * valueNoise((p * 0.08 + t) / s));
    return d * 0.7;
}

// ---- Blast clouds - volumetric 3D clouds pushed outward by the explosion ----
float blastCloudDensity(vec3 p, float t, float s) {
    if (t < 1.5 || t > 55.0) return 0.0;
    float ct = t - 1.5;

    vec3 rel = p - ExplosionCenter;
    float distA = length(rel.xz);
    float d = 0.0;

    // Fast condensation dome (Wilson cloud) - rapidly expanding white hemisphere
    if (ct < 18.0) {
        float domeR = (8.0 + ct * 3.0) * s;
        float domeW = (6.0 + ct * 0.5) * s;
        float domeH = (8.0 + ct * 2.0) * s;
        float ringDist = abs(distA - domeR);
        if (rel.y >= -1.0 && rel.y <= domeH) {
            float dd = smoothstep(domeW, 0.0, ringDist);
            // Hemisphere: strong at ground, fading up
            dd *= smoothstep(-1.0, 4.0 * s, rel.y) * smoothstep(domeH, domeH * 0.15, rel.y);
            dd *= max(0.0, 1.0 - ct / 18.0);
            float n = fbm3((p * 0.04 + vec3(ct * 0.6, 0.0, ct * 0.4)) / s);
            dd *= (0.45 + 0.55 * n);
            d += dd * 0.55;
        }
    }

    // Slow debris/dust clouds - chunkier, persist much longer
    if (ct > 3.0) {
        float dct = ct - 3.0;
        float cloudR = (4.0 + dct * 1.0) * s;
        float cloudW = (8.0 + dct * 0.5) * s;
        float cloudBase = 3.0 * s;
        float cloudTop = (12.0 + dct * 0.8) * s;
        float ringDist = abs(distA - cloudR);
        if (rel.y >= -2.0 && rel.y <= cloudTop) {
            float dd = smoothstep(cloudW, 0.0, ringDist);
            // Vertical bell shape
            float midH = mix(cloudBase, cloudTop, 0.35);
            float vertSigma = (cloudTop - cloudBase) * 0.35;
            float vd = (rel.y - midH) / max(vertSigma, 1.0);
            dd *= exp(-0.5 * vd * vd);
            dd *= max(0.0, 1.0 - smoothstep(28.0, 52.0, t));
            // Heavy FBM for chunky volumetric cloud look
            float n = fbm3((p * 0.03 + vec3(dct * 0.12, -dct * 0.04, dct * 0.09)) / s);
            dd *= n * 1.6;
            d += dd * 0.65;
        }
    }

    // High-altitude debris ring - thin cloud layer pushed upward
    if (ct > 5.0 && ct < 40.0) {
        float hct = ct - 5.0;
        float highR = (8.0 + hct * 0.8) * s;
        float highW = (5.0 + hct * 0.3) * s;
        float highH = (40.0 + hct * 1.5) * s;
        float highThick = 5.0 * s;
        float ringDist = abs(distA - highR);
        if (rel.y >= highH - highThick && rel.y <= highH + highThick) {
            float dd = smoothstep(highW, 0.0, ringDist);
            float vFade = 1.0 - abs(rel.y - highH) / highThick;
            dd *= vFade * vFade;
            dd *= max(0.0, 1.0 - smoothstep(20.0, 40.0, hct));
            float n = fbm3((p * 0.04 + vec3(hct * 0.08, hct * 0.03, -hct * 0.06)) / s);
            dd *= (0.3 + 0.7 * n);
            d += dd * 0.4;
        }
    }

    return clamp(d, 0.0, 1.0);
}

void main() {
    float t = ExplosionTime;
    if (t < 0.0 || t > 34.0) discard;

    // Logarithmic visual scale - keeps rendering manageable at high power
    // power=50 -> s=1.0 (original), power=1000 -> s~4.0 (huge but renderable)
    float rawScale = ExplosionPower / 50.0;
    float s = min(3.35, 1.0 + log2(max(rawScale, 1.0)) * 0.58);

    // Reconstruct ray via near/far plane unprojection
    vec2 ndc = gl_FragCoord.xy / ScreenSize * 2.0 - 1.0;
    vec4 nearP = InvViewProjMat * vec4(ndc, -1.0, 1.0);
    nearP.xyz /= nearP.w;
    vec4 farP = InvViewProjMat * vec4(ndc, 1.0, 1.0);
    farP.xyz /= farP.w;
    vec3 rayOrigin = nearP.xyz;
    vec3 rayDir = normalize(farP.xyz - nearP.xyz);

    // Sample scene depth buffer - stop raymarching at solid geometry
    vec2 uv = gl_FragCoord.xy / ScreenSize;
    float sceneDepthRaw = texture(DepthTex, uv).r;
    // Reconstruct world-space position of the closest surface
    float sceneNDCz = sceneDepthRaw * 2.0 - 1.0;
    vec4 sceneWorldPos = InvViewProjMat * vec4(ndc, sceneNDCz, 1.0);
    sceneWorldPos.xyz /= sceneWorldPos.w;
    // Distance along the ray to the scene surface
    float sceneRayDist = dot(sceneWorldPos.xyz - rayOrigin, rayDir);

    // Dynamic AABB - tight bounds around active cloud structures only
    // Shockwave (only during its lifetime, t < 6.5)
    float shockR = t < 6.5 ? t * 52.0 * s + 15.0 : 0.0;

    // Blast cloud extents (per cloud type for tight bounds)
    float ct_aabb = max(t - 1.5, 0.0);
    float domeExtent = ct_aabb < 18.0 ? (16.0 + ct_aabb * 3.5) * s : 0.0;
    float debrisExtent = t > 4.5 ? (14.0 + (t - 4.5) * 1.5) * s : 0.0;
    float highExtent = t > 6.5 ? (15.0 + (t - 6.5) * 1.1) * s : 0.0;
    if (t > 42.0) highExtent = 0.0;
    float cloudR = max(max(domeExtent, debrisExtent), highExtent);

    float maxR = max(max(70.0 * s, shockR), cloudR);
    float maxH = 130.0 * s;
    vec3 bmin = ExplosionCenter + vec3(-maxR, -5.0, -maxR);
    vec3 bmax = ExplosionCenter + vec3(maxR, maxH, maxR);

    float tN, tF;
    if (!intersectAABB(rayOrigin, rayDir, bmin, bmax, tN, tF)) discard;

    // Clamp ray to not go past solid geometry
    tF = min(tF, sceneRayDist);
    if (tN >= tF) discard;

    // Raymarch - reduced step count keeps the mushroom readable without turning every frame into a furnace.
    float scaledStepMax = 4.0 * max(s, 1.0);
    float stepSz = min((tF - tN) / 26.0, scaledStepMax);
    vec4 acc = vec4(0.0);

    for (int i = 0; i < 32; i++) {
        if (acc.a > 0.95) break;
        float tc = tN + (float(i) + 0.5) * stepSz;
        if (tc > tF) break;
        vec3 pos = rayOrigin + rayDir * tc;

        float fb = fireballDensity(pos, t, s);
        float st = stemDensity(pos, t, s);
        float cp = capDensity(pos, t, s);
        float sw = shockwaveDensity(pos, t, s);
        float bc = blastCloudDensity(pos, t, s);
        float total = fb + st + cp + sw + bc;
        if (total < 0.001) continue;

        // Temperature: hot = 1, cool = 0
        float temp = clamp(fb + st * max(0.0, 0.7 - t * 0.08) + cp * max(0.0, 0.3 - t * 0.025), 0.0, 1.0);

        // Color
        vec3 hotCol = mix(vec3(1.0, 0.5, 0.1), vec3(1.0, 0.9, 0.5), temp);
        vec3 coolCol = vec3(0.18, 0.14, 0.11) + valueNoise(pos * 0.04 / s) * vec3(0.06, 0.04, 0.02);
        vec3 swCol = vec3(0.82, 0.85, 0.88);
        // Blast cloud colors: white-grey condensation -> brown-grey debris over time
        vec3 cloudCol = mix(
            vec3(0.85, 0.87, 0.90),
            vec3(0.35, 0.28, 0.22),
            clamp(t * 0.04, 0.0, 0.7)
        );
        cloudCol += valueNoise(pos * 0.03 / s) * vec3(0.08, 0.06, 0.04);

        float w = 0.001;
        vec3 col = vec3(0.0);
        float hotW = fb + st * max(0.0, 0.5 - t * 0.05);
        col += hotCol * hotW; w += hotW;
        float coolW = st * min(1.0, t * 0.15) + cp;
        col += coolCol * coolW; w += coolW;
        col += swCol * sw; w += sw;
        col += cloudCol * bc; w += bc;
        col /= w;

        // Emissive glow
        col += vec3(0.4, 0.15, 0.03) * temp * 0.5;

        // Absorption
        float alpha = total * stepSz * 0.17;
        alpha = min(alpha, 0.3);

        // Front-to-back composite
        acc.rgb += col * alpha * (1.0 - acc.a);
        acc.a += alpha * (1.0 - acc.a);
    }

    if (acc.a < 0.002) discard;
    fragColor = acc;
}
