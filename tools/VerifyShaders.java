import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import static org.lwjgl.util.shaderc.Shaderc.*;

/** Compiles preprocessed shader sources without requiring a graphics context. */
public class VerifyShaders {
    static Path resources;
    static ZipFile minecraft;
    static String expand(String source) throws Exception {
        var pattern = Pattern.compile("#moj_import <([^>]+)>");
        var matches = pattern.matcher(source);
        var expanded = new StringBuffer();
        while (matches.find()) {
            String[] id = matches.group(1).split(":", 2);
            String path = "assets/" + id[0] + "/shaders/include/" + id[1];
            String content;
            if (id[0].equals("minecraft")) {
                var entry = minecraft.getEntry(path);
                if (entry == null) throw new IllegalArgumentException("Missing include " + path);
                try (var stream = minecraft.getInputStream(entry)) { content = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8); }
            } else content = Files.readString(resources.resolve(path));
            content = content.replaceAll("(?m)^#version[^\\r\\n]*", "");
            matches.appendReplacement(expanded, Matcher.quoteReplacement(expand(content)));
        }
        matches.appendTail(expanded);
        return expanded.toString();
    }
    public static void main(String[] args) throws Exception {
        resources = Path.of(args[0]);
        long compiler = shaderc_compiler_initialize();
        if (compiler == 0) throw new IllegalStateException("Cannot initialize shader compiler");
        int errors = 0, count = 0;
        try (var archive = new ZipFile(args[1]); var mre = Files.list(resources.resolve("assets/mre/shaders/core"));
             var client = Files.list(resources.resolve("assets/astolfoclient/shaders/core"))) {
            minecraft = archive;
            var paths=java.util.stream.Stream.concat(java.util.stream.Stream.concat(mre,client),
                    java.util.stream.Stream.of(resources.resolve("assets/astolfoclient/shaders/post/depth_blur.fsh")))
                    .filter(p -> p.toString().endsWith(".vsh") || p.toString().endsWith(".fsh"))
                    .filter(p -> p.getFileName().toString().contains("_gui.") || Files.exists(p.resolveSibling(p.getFileName().toString().replaceAll("\\.(vsh|fsh)$", ".json"))));
            for (Path path : paths.sorted().toList()) {
                String source = expand(Files.readString(path));
                if(path.toString().endsWith(".vsh")) {
                    var descriptor=path.resolveSibling(path.getFileName().toString().replace(".vsh",".json"));
                    if(Files.exists(descriptor)&&Files.readString(descriptor).matches("(?s).*\"screen\"\\s*:\\s*true.*")) {
                        var inputs=Pattern.compile("(?m)^\\s*in\\s+\\w+\\s+(\\w+)\\s*;").matcher(source);
                        while(inputs.find())if(!Set.of("Position","UV0","Color").contains(inputs.group(1)))
                            throw new IllegalStateException(path+": vertex attribute "+inputs.group(1)+" is not supplied by the effect mesh");
                    }
                }
                {
                    long options = shaderc_compile_options_initialize();
                    long result = 0;
                    try {
                        shaderc_compile_options_set_auto_bind_uniforms(options, true);
                        shaderc_compile_options_set_auto_map_locations(options, true);
                        shaderc_compile_options_set_target_env(options, shaderc_target_env_opengl, shaderc_env_version_opengl_4_5);
                        int kind = path.toString().endsWith(".vsh") ? shaderc_vertex_shader : shaderc_fragment_shader;
                        // OpenGL SPIR-V requires GLSL 330; shipped OpenGL sources remain GLSL 150.
                        String compileSource = source.replace("#version 150", "#version 330");
                        result = shaderc_compile_into_spv(compiler, compileSource, kind, path.getFileName().toString(), "main", options);
                        if (result == 0 || shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) {
                            errors++;
                            System.err.println(path.getFileName() + " [OpenGL]: " + (result == 0 ? "No result" : shaderc_result_get_error_message(result)));
                        } else {
                            count++;
                            System.out.println("PASS " + path.getFileName() + " [OpenGL]");
                        }
                    } finally {
                        if (result != 0) shaderc_result_release(result);
                        shaderc_compile_options_release(options);
                    }
                }
            }
        } finally { shaderc_compiler_release(compiler); }
        System.out.println(count + " shader compilations passed, " + errors + " failed.");
        if (errors != 0) System.exit(1);
    }
}
