#version 150
layout(std140) uniform EffectData {
    mat4 uModelViewMat;
    mat4 uProjMat;
    float Time;
    vec3 Accent;
    vec4 ColorModulator;
    vec4 uDepthRange;
};


in vec3 Direction;
in vec4 vColor;





out vec4 OutColor;

float hash13(vec3 p) {
    p = fract(p * vec3(443.8975, 397.2973, 491.1871));
    p += dot(p, p.yxz + 19.19);
    return fract((p.x + p.y) * p.z);
}

float vnoise(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(mix(hash13(i + vec3(0.0, 0.0, 0.0)), hash13(i + vec3(1.0, 0.0, 0.0)), f.x),
            mix(hash13(i + vec3(0.0, 1.0, 0.0)), hash13(i + vec3(1.0, 1.0, 0.0)), f.x), f.y),
        mix(mix(hash13(i + vec3(0.0, 0.0, 1.0)), hash13(i + vec3(1.0, 0.0, 1.0)), f.x),
            mix(hash13(i + vec3(0.0, 1.0, 1.0)), hash13(i + vec3(1.0, 1.0, 1.0)), f.x), f.y),
        f.z);
}

float fbm(vec3 p, int oct) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 6; i++) {
        if (i >= oct) break;
        v += a * vnoise(p);
        p = p * 2.07 + vec3(1.7, 9.2, 4.3);
        a *= 0.5;
    }
    return v;
}

// Высокодетальные облака на горизонтальной плоскости altitude.
// Луч пересекает плоскость в точке d * (altitude / d.y).
float cloudShape(vec3 d, float altitude, vec2 wind, float scale, float low, float high, int oct) {
    // Гладкое затухание к горизонту вместо жёсткого if-обрезания (раньше это давало
    // видимую горизонтальную «полоску среза» при d.y == 0.012).
    if (d.y < 0.001) return 0.0;
    vec3 p = d * (altitude / max(d.y, 0.001));
    p.xz += wind;
    float n = fbm(vec3(p.x * scale, p.z * scale, altitude * 0.5), oct);
    float result = smoothstep(low, high, n);
    // Smooth fade-out у горизонта: облака исчезают плавно в полосе d.y ∈ [0.001 .. 0.08].
    result *= smoothstep(0.001, 0.08, d.y);
    return result;
}

void main() {
    vec3 d = normalize(Direction);
    float t = Time;

    // Солнце у горизонта с тонким покачиванием — застывший момент заката.
    vec3 sunDir = normalize(vec3(0.55 + sin(t * 0.04) * 0.04, 0.07, 0.80 + cos(t * 0.04) * 0.04));
    float toSun = max(0.0, dot(d, sunDir));

    vec3 accent = max(Accent, vec3(0.05));

    // ===== Палитра неба =====
    vec3 skyZenith   = vec3(0.045, 0.025, 0.165);  // глубокий фиолет в зените
    vec3 skyHigh     = vec3(0.420, 0.180, 0.420);  // маджента
    vec3 skyMid      = vec3(0.950, 0.400, 0.350);  // розово-коралловый
    vec3 skyLow      = vec3(1.000, 0.620, 0.250);  // оранжевый
    vec3 skyEmber    = vec3(0.450, 0.050, 0.100);  // тлеющие угли под горизонтом

    vec3 sky;
    if (d.y > 0.55) {
        sky = mix(skyHigh, skyZenith, smoothstep(0.55, 1.0, d.y));
    } else if (d.y > 0.18) {
        sky = mix(skyMid, skyHigh, smoothstep(0.18, 0.55, d.y));
    } else if (d.y > 0.0) {
        sky = mix(skyLow, skyMid, smoothstep(0.0, 0.18, d.y));
    } else {
        sky = mix(skyEmber, skyLow, smoothstep(-0.45, 0.0, d.y));
    }

    // Лёгкое тонирование акцентом, чтобы цветовая настройка влияла.
    sky = mix(sky, sky * mix(vec3(1.0), accent * 1.6, 0.55), 0.30);

    // Тёплое гало вокруг солнца.
    sky += vec3(1.00, 0.65, 0.25) * pow(toSun, 8.0) * 0.55;
    sky += vec3(1.00, 0.85, 0.40) * pow(toSun, 24.0) * 0.85;

    // Мягкое атмосферное "мерцание" воздуха.
    float shimmer = fbm(vec3(d.xz * 4.0, t * 0.6), 3) * 0.04;
    sky += vec3(1.0, 0.9, 0.7) * shimmer * (1.0 - smoothstep(0.0, 0.4, abs(d.y)));

    // ===== Облака — три слоя на разных "высотах" =====
    vec2 wind = vec2(t * 0.55, t * 0.32);
    float c1 = cloudShape(d, 1.20, wind,        0.55, 0.42, 0.78, 5);  // дальние, перистые
    float c2 = cloudShape(d, 0.55, wind * 0.85, 1.10, 0.45, 0.82, 5);  // средние
    float c3 = cloudShape(d, 0.30, wind * 0.65, 2.00, 0.50, 0.85, 4);  // близкие, кучевые

    // Послойный композит (каждый слой накрывает следующий).
    float cloudCover = c1;
    cloudCover = cloudCover + c2 * (1.0 - cloudCover);
    cloudCover = cloudCover + c3 * (1.0 - cloudCover);

    // Плотность для затухания неба за облаками.
    float cloudDepth = clamp(c1 + c2 * 0.85 + c3 * 0.65, 0.0, 1.0);

    // Освещение облаков от солнца: лицевые стороны золотятся, тыльные тёмно-сиреневые.
    float sunFacing = pow(clamp(dot(normalize(d * 0.85 + sunDir * 0.15), sunDir), 0.0, 1.0), 2.5);
    float backlight = pow(toSun, 4.5);
    vec3 cloudShadow = mix(vec3(0.18, 0.10, 0.22), vec3(0.45, 0.25, 0.35), 0.5 + 0.5 * d.y);
    vec3 cloudLit    = mix(vec3(1.00, 0.78, 0.45), vec3(1.00, 0.55, 0.30), 0.4);
    vec3 cloudFire   = vec3(1.00, 0.92, 0.65);  // огненный backlight по краям
    vec3 cloudColor  = mix(cloudShadow, cloudLit, sunFacing);
    cloudColor = mix(cloudColor, cloudFire, backlight * 0.55);

    // Серебряная кромка облаков (silver lining) — при подсветке солнцем сзади.
    float edgeMask = clamp(c2 - c3, 0.0, 1.0);
    edgeMask *= smoothstep(0.0, 0.4, c2) * (1.0 - smoothstep(0.6, 0.95, c2));
    cloudColor += cloudFire * edgeMask * backlight * 0.9;

    // Композит облаков поверх неба.
    vec3 color = mix(sky, cloudColor, cloudDepth);

    // ===== Солнечный диск + корона =====
    float sunAng = acos(clamp(toSun, -1.0, 1.0));
    float sunDisc = smoothstep(0.028, 0.022, sunAng);
    float sunCorona = exp(-sunAng * 25.0) * 0.40;
    float sunWideHalo = exp(-sunAng * 4.0) * 0.18;
    color += vec3(1.00, 0.95, 0.72) * sunDisc * 4.5 * (1.0 - cloudDepth * 0.85);
    color += vec3(1.00, 0.65, 0.30) * sunCorona;
    color += vec3(1.00, 0.55, 0.25) * sunWideHalo * (1.0 - cloudDepth * 0.5);

    // ===== God rays — лучи через облака =====
    {
        float rays = 0.0;
        for (int i = 1; i < 10; i++) {
            float u = float(i) / 10.0;
            vec3 s = normalize(mix(d, sunDir, u));
            float dens = cloudShape(s, 0.85, wind * 0.9, 0.95, 0.40, 0.80, 3);
            rays += (1.0 - dens) * (1.0 - u);
        }
        rays /= 10.0;
        float rayMask = pow(toSun, 5.0);
        color += vec3(1.00, 0.72, 0.38) * rays * rayMask * 0.85;
    }

    // ===== Lens flare — несколько ярких бликов на линии вид→солнце (упрощённо) =====
    {
        // Используем "проекцию" вектора d на плоскость, перпендикулярную sunDir.
        vec3 sunPerp = d - sunDir * dot(d, sunDir);
        float perpLen = length(sunPerp);
        if (perpLen > 0.001) {
            // Бл блики появляются в трёх позициях вдоль линии "от солнца".
            for (int i = 0; i < 3; i++) {
                float fp = -0.15 + float(i) * 0.18;
                vec3 flarePos = normalize(sunDir + sunPerp / max(perpLen, 0.0001) * fp * 0.3);
                float fd = max(0.0, 1.0 - dot(d, flarePos));
                float flare = exp(-fd * 600.0) * (0.4 - float(i) * 0.10);
                vec3 flareCol = (i == 0) ? vec3(1.0, 0.7, 0.4)
                              : (i == 1) ? vec3(0.6, 0.9, 1.0)
                                         : vec3(1.0, 0.5, 0.7);
                color += flareCol * flare * (1.0 - cloudDepth * 0.7) * pow(toSun, 2.0);
            }
        }
    }

    // ===== Лёгкие звёзды в верхней половине неба =====
    if (d.y > 0.5) {
        float starFade = smoothstep(0.50, 0.90, d.y) * (1.0 - cloudDepth * 0.85);
        vec3 sp = d * 200.0;
        vec3 si = floor(sp);
        float sh = hash13(si);
        if (sh > 0.991) {
            vec3 sf = fract(sp) - 0.5;
            float disc = 1.0 - smoothstep(0.0, 0.05, length(sf));
            float twinkle = 0.4 + 0.6 * sin(t * 2.8 + sh * 67.0);
            color += vec3(1.0, 0.95, 0.85) * disc * twinkle * 0.65 * starFade;
        }
    }

    // ===== Мягкая дымка/haze над горизонтом (без жёсткого силуэта) =====
    {
        float hazeBand = exp(-d.y * d.y * 80.0);
        float hazeNoise = fbm(vec3(d.xz * 5.0 + wind * 0.05, 0.0), 3);
        vec3 hazeCol = mix(skyEmber, skyLow, 0.6);
        color = mix(color, color * 0.85 + hazeCol * 0.25, hazeBand * (0.40 + 0.30 * hazeNoise));
    }

    // Тонировка: сильный Reinhard + лёгкая гамма для кинематографичности.
    color = color / (1.0 + color * 0.45);
    color = pow(color, vec3(0.92));

    OutColor = vec4(color, 1.0) * ColorModulator;
}
