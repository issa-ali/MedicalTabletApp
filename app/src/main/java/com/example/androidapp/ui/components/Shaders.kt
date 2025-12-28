package com.example.androidapp.ui.components

import org.intellij.lang.annotations.Language

object Shaders {
    @Language("AGSL")
    val NEURAL_FLUX = """
        uniform float2 iResolution;
        uniform float iTime;
        
        float hash(float n) { return fract(sin(n) * 43758.5453123); }
        
        float noise(float3 x) {
            float3 p = floor(x);
            float3 f = fract(x);
            f = f * f * (3.0 - 2.0 * f);
            float n = p.x + p.y * 57.0 + 113.0 * p.z;
            return lerp(lerp(lerp(hash(n + 0.0), hash(n + 1.0), f.x),
                           lerp(hash(n + 57.0), hash(n + 58.0), f.x), f.y),
                       lerp(lerp(hash(n + 113.0), hash(n + 114.0), f.x),
                           lerp(hash(n + 170.0), hash(n + 171.0), f.x), f.y), f.z);
        }

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / iResolution.xy;
            float3 p = float3(uv * 3.0, iTime * 0.2);
            
            float n = noise(p);
            float n2 = noise(p * 2.0 + 10.0);
            
            float3 color = float3(0.02, 0.05, 0.1); // Deep Space
            color += float3(0.0, 0.4, 0.5) * pow(n, 4.0);
            color += float3(0.3, 0.1, 0.5) * pow(n2, 5.0);
            
            return half4(color, 1.0);
        }
    """.trimIndent()

    @Language("AGSL")
    val CINEMATIC_POST_PROCESS = """
        uniform float2 iResolution;
        uniform float iTime;
        uniform shader contents;
        
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / iResolution.xy;
            
            // 1. Chromatic Aberration (Infinite Depth)
            float dist = distance(uv, float2(0.5));
            float amount = 0.005 * dist;
            
            half4 r = contents.eval(fragCoord + float2(amount, 0.0));
            half4 g = contents.eval(fragCoord);
            half4 b = contents.eval(fragCoord - float2(amount, 0.0));
            
            half4 color = half4(r.r, g.g, b.b, g.a);
            
            // 2. Anamorphic Vignette
            float vignette = 1.0 - smoothstep(0.4, 1.2, dist);
            color *= vignette;
            
            // 3. Subtle Scanlines
            float scanline = sin(uv.y * 800.0) * 0.02;
            color -= scanline;
            
            return color;
        }
    """.trimIndent()
    
    @Language("AGSL")
    val SCANNING_LASER = """
        uniform float2 iResolution;
        uniform float iTime;
        uniform half4 iColor;
        
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / iResolution.xy;
            float scanPos = fract(iTime * 0.5);
            float dist = abs(uv.y - scanPos);
            
            float line = smoothstep(0.015, 0.0, dist);
            float glow = smoothstep(0.12, 0.0, dist) * 0.4;
            
            return iColor * (line + glow);
        }
    """.trimIndent()
}
