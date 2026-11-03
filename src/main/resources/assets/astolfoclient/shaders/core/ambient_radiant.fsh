#version 150
layout(std140) uniform EffectData {
    mat4 uModelViewMat;
    mat4 uProjMat;
    float Time;
    vec3 Accent;
    vec4 ColorModulator;
    vec4 uDepthRange;
};

// Полярное сияние / Aurora Borealis. Тёмное звёздное небо + плывущие занавески сияния.
// Все цвета занавесок и атмосферы полностью производны от accent.

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

// Занавеска авроры: волнистая полоса с резкой нижней кромкой и мягким верхним затуханием.
// Внутри — высокочастотные вертикальные «штрихи» (ribbons) и медленный FBM-патчинг.
float auroraCurtain(vec3 d, float t, float baseY, float phase, float speed,
                    float bottomFalloff, float topFalloff) {
    if (d.y < baseY - 0.5 || d.y > baseY + 0.8) return 0.0;

    float az = atan(d.z, d.x);

    // Многочастотная форма нижней кромки занавески.
    float curtainY = baseY
                   + sin(az * 1.3 + t * speed + phase) * 0.16
                   + sin(az * 2.6 + t * speed * 0.6 + phase * 1.3) * 0.06
                   + sin(az * 5.2 - t * speed * 0.35 + phase * 2.1) * 0.025;

    float distFromCurtain = d.y - curtainY;

    // Асимметричный спад: резкий низ (как горящая основа), мягкое затухание вверх.
    float intensity;
    if (distFromCurtain < 0.0) {
        intensity = exp(distFromCurtain * bottomFalloff);
    } else {
        intensity = exp(-distFromCurtain * topFalloff);
    }

    // Вертикальные «штрихи» внутри занавески.
    float ribbonHi = sin(az * 32.0 + t * 1.6 + phase * 5.0) * 0.4 + 0.6;
    float ribbonLo = sin(az * 8.0  + t * 0.5 + phase * 2.0) * 0.5 + 0.5;
    float ribbons = ribbonHi * (0.40 + 0.60 * ribbonLo);
    intensity *= 0.35 + 0.75 * ribbons;

    // Медленные шумовые «патчи» — где-то ярче, где-то тусклее.
    float patches = fbm(vec3(az * 2.5, t * 0.18 + phase, 0.0), 3);
    intensity *= 0.50 + 0.70 * patches;

    return max(0.0, intensity);
}

// Чистые звёзды без хроматической перегрузки.
vec3 starfield(vec3 d, float t, vec3 tint) {
    vec3 result = vec3(0.0);

    // Слой A — редкие крупные.
    {
        vec3 p = d * 80.0;
        vec3 i = floor(p);
        float h = hash13(i);
        if (h > 0.987) {
            vec3 f = fract(p) - 0.5;
            float dist2 = dot(f, f);
            float core = exp(-dist2 * 800.0);
            float halo = exp(-dist2 *  90.0) * 0.30;
            float bright = (h - 0.987) / 0.013;
            float twinkle = 0.4 + 0.6 * sin(t * 2.0 + h * 113.0);
            // Лёгкий цветовой тинт по hash (тёплый/холодный белый).
            float tw = hash13(i + vec3(7.7));
            vec3 col = mix(vec3(0.92, 0.95, 1.00), vec3(1.00, 0.93, 0.85), tw);
            col = mix(col, tint, 0.10);
            result += col * (core + halo) * bright * twinkle;
        }
    }

    // Слой B — плотный мелкий.
    {
        vec3 p = d * 200.0;
        vec3 i = floor(p);
        float h = hash13(i + vec3(1.0));
        if (h > 0.991) {
            vec3 f = fract(p) - 0.5;
            float dist2 = dot(f, f);
            float core = exp(-dist2 * 1500.0);
            float bright = (h - 0.991) / 0.009;
            float twinkle = 0.5 + 0.5 * sin(t * 1.5 + h * 91.0);
            result += vec3(0.95, 0.95, 1.00) * core * bright * twinkle * 0.7;
        }
    }

    return result;
}

void main() {
    vec3 d = normalize(Direction);
    float t = Time;

    vec3 accent = max(Accent, vec3(0.05));

    // ===== Палитра — всё derived from accent =====
    vec3 cMain   = accent;
    vec3 cAlt    = clamp(accent.gbr * 1.10, vec3(0.05), vec3(1.0));     // ротация каналов
    vec3 cWarm   = clamp(accent + vec3(0.20, -0.05, -0.10), vec3(0.05), vec3(1.0));
    vec3 cBright = mix(accent, vec3(1.0), 0.55);
    vec3 cComp   = clamp(vec3(1.0) - accent * 0.75, vec3(0.05), vec3(1.0));

    // ===== Глубокое ночное небо (с тонким accent-влиянием в градиенте) =====
    vec3 nightZenith  = mix(vec3(0.003, 0.003, 0.012), accent * 0.04, 0.40);
    vec3 nightHigh    = mix(vec3(0.018, 0.020, 0.055), accent * 0.10, 0.50);
    vec3 nightHorizon = mix(vec3(0.040, 0.040, 0.085), accent * 0.22, 0.65);
    vec3 nightLow     = vec3(0.004, 0.004, 0.020);

    vec3 sky = nightZenith;
    sky = mix(sky, nightHigh,    smoothstep(1.00, 0.45, d.y));
    sky = mix(sky, nightHorizon, smoothstep(0.45, 0.00, d.y));
    sky = mix(sky, nightLow,     smoothstep(0.00, -0.60, d.y));

    // Узкое тёплое сияние у горизонта.
    float horizonGlow = exp(-d.y * d.y * 80.0);
    sky += accent * horizonGlow * 0.10;

    // Очень мягкий accent-туман над горизонтом, имитирует низкую дымку.
    float lowMist = exp(-d.y * d.y * 25.0) * smoothstep(-0.10, 0.30, d.y);
    sky += accent * lowMist * 0.07;

    vec3 color = sky;

    // ===== Звёзды =====
    color += starfield(d, t, accent);

    // ===== Полярные занавески — все цвета от accent =====
    vec3 col1 = cMain;                    // основная — pure accent
    vec3 col2 = mix(cAlt, cBright, 0.40); // вторичная — повёрнутая+светлая
    vec3 col3 = cWarm;                    // тёплый акцент
    vec3 col4 = mix(cMain, cComp, 0.30);  // лёгкий уход к комплименту

    float a1 = auroraCurtain(d, t, 0.18,  0.0, 0.32, 8.0, 3.2);
    float a2 = auroraCurtain(d, t, 0.10,  1.7, 0.42, 7.0, 2.8);
    float a3 = auroraCurtain(d, t, 0.32,  3.4, 0.25, 9.0, 3.6);
    float a4 = auroraCurtain(d, t, 0.05, -2.1, 0.20, 6.0, 2.5);

    color += col1 * a1 * 1.65;
    color += col2 * a2 * 1.30;
    color += col3 * a3 * 1.05;
    color += col4 * a4 * 0.85;

    // Яркие "горящие" основания занавесок — самая сильная характеристика авроры.
    float bases = pow(a1, 2.0) * 1.0
                + pow(a2, 2.0) * 0.8
                + pow(a3, 2.0) * 0.6
                + pow(a4, 2.0) * 0.5;
    color += cBright * bases * 0.45;

    // ===== Тонировка =====
    color = color / (1.0 + color * 0.42);
    color = pow(color, vec3(0.92));

    OutColor = vec4(color, 1.0) * ColorModulator;
}
