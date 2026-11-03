package xyz.angames.astolfoclient.client.mixin;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(value=EnvType.CLIENT)
@Mixin(value={Util.OS.class})
public class UtilOperatingSystemMixin {
    @ModifyVariable(method={"openFile(Ljava/io/File;)V"}, at=@At(value="HEAD"), argsOnly=true)
    private File modifyOpenParam(File file) {
        return file;
    }

    @ModifyVariable(method={"openPath(Ljava/nio/file/Path;)V"}, at=@At(value="HEAD"), argsOnly=true)
    private Path modifyOpenParamPath(Path path) {
        return path;
    }

    @ModifyVariable(method={"openUri(Ljava/net/URI;)V"}, at=@At(value="HEAD"), argsOnly=true)
    private URI modifyOpenParamURI(URI uri) {
        return uri;
    }

    @ModifyVariable(method={"openUri(Ljava/lang/String;)V"}, at=@At(value="HEAD"), argsOnly=true)
    private String modifyOpenParamString(String str) {
        return str;
    }
}

