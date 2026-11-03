package xyz.angames.astolfoclient.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.gui.ClickGuiScreen;
import xyz.angames.astolfoclient.client.util.DiscordAvatarManager;

@Environment(value=EnvType.CLIENT)
public class DiscordRpcManager {
    private static final String CLIENT_ID = "1439691920963145779";
    public static String discordUsername = "Connecting...";
    private volatile boolean running = true;
    private Thread rpcThread;
    private IPCChannel ipcChannel;
    private long startTimestamp = 0L;

    public void start() {
        this.startTimestamp = System.currentTimeMillis() / 1000L;
        this.running = true;
        this.rpcThread = new Thread(() -> {
            while (this.running) {
                try {
                    if (this.ipcChannel == null) {
                        this.ipcChannel = this.connectIPC();
                        if (this.ipcChannel != null) {
                            this.sendHandshake();
                            this.readResponse();
                            this.update();
                        }
                    }
                    if (this.ipcChannel != null) {
                        this.update();
                    }
                }
                catch (Exception e) {
                    this.closeChannel();
                }
                try {
                    Thread.sleep(5000L);
                }
                catch (InterruptedException ignored) {
                    break;
                }
            }
        }, "discord-rpc-thread");
        this.rpcThread.setDaemon(true);
        this.rpcThread.start();
    }

    public void stop() {
        this.running = false;
        if (this.rpcThread != null && this.rpcThread.isAlive()) {
            this.rpcThread.interrupt();
        }
        this.closeChannel();
    }

    private synchronized void closeChannel() {
        if (this.ipcChannel != null) {
            try {
                this.writeFrame(2, "{}");
            }
            catch (Exception exception) {
                // empty catch block
            }
            try {
                this.ipcChannel.close();
            }
            catch (Exception exception) {
                // empty catch block
            }
            this.ipcChannel = null;
        }
    }

    private IPCChannel connectIPC() {
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        for (int i = 0; i < 10; ++i) {
            try {
                String[] envDirs;
                if (isWindows) {
                    String pipePath = "\\\\.\\pipe\\discord-ipc-" + i;
                    return new WindowsPipeChannel(pipePath);
                }
                for (String dir : envDirs = new String[]{System.getenv("XDG_RUNTIME_DIR"), System.getenv("TMPDIR"), System.getenv("TMP"), System.getenv("TEMP"), "/tmp"}) {
                    File socketFile;
                    if (dir == null || dir.trim().isEmpty() || !(socketFile = new File(dir, "discord-ipc-" + i)).exists()) continue;
                    return new UnixDomainChannel(socketFile.getAbsolutePath());
                }
                continue;
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return null;
    }

    private void sendHandshake() throws IOException {
        JsonObject obj = new JsonObject();
        obj.addProperty("v", (Number)1);
        obj.addProperty("client_id", CLIENT_ID);
        this.writeFrame(0, obj.toString());
    }

    private void readResponse() {
        try {
            JsonObject data;
            String jsonStr = this.readFrame();
            if (jsonStr == null || jsonStr.isEmpty()) {
                return;
            }
            JsonObject root = JsonParser.parseString((String)jsonStr).getAsJsonObject();
            if (root.has("evt") && "READY".equalsIgnoreCase(root.get("evt").getAsString()) && root.has("data") && root.get("data").isJsonObject() && (data = root.getAsJsonObject("data")).has("user") && data.get("user").isJsonObject()) {
                JsonObject user = data.getAsJsonObject("user");
                String username = user.has("username") ? user.get("username").getAsString() : "User";
                String userId = user.has("id") ? user.get("id").getAsString() : "";
                String avatar = user.has("avatar") && !user.get("avatar").isJsonNull() ? user.get("avatar").getAsString() : "";
                discordUsername = username;
                ClickGuiScreen.setDiscordUser(username, userId, avatar);
                DiscordAvatarManager.update(username, userId, avatar);
                System.out.println("[Discord] Logged in as: " + username);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public synchronized void update() {
        if (this.ipcChannel == null) {
            return;
        }
        try {
            JsonObject activity = new JsonObject();
            activity.addProperty("state", "Version: " + AstolfoclientClient.getInstance().getVersion());
            activity.addProperty("details", "User: " + discordUsername);
            JsonObject timestamps = new JsonObject();
            timestamps.addProperty("start", (Number)this.startTimestamp);
            activity.add("timestamps", (JsonElement)timestamps);
            JsonObject assets = new JsonObject();
            assets.addProperty("large_image", "logo2");
            assets.addProperty("large_text", AstolfoclientClient.getInstance().getClientName());
            activity.add("assets", (JsonElement)assets);
            JsonObject argsObj = new JsonObject();
            argsObj.addProperty("pid", (Number)((int)ProcessHandle.current().pid()));
            argsObj.add("activity", (JsonElement)activity);
            JsonObject root = new JsonObject();
            root.addProperty("cmd", "SET_ACTIVITY");
            root.add("args", (JsonElement)argsObj);
            root.addProperty("nonce", UUID.randomUUID().toString());
            this.writeFrame(1, root.toString());
        }
        catch (Exception e) {
            this.closeChannel();
        }
    }

    private void writeFrame(int op, String json) throws IOException {
        if (this.ipcChannel == null) {
            return;
        }
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(8 + bytes.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(op);
        buf.putInt(bytes.length);
        buf.put(bytes);
        this.ipcChannel.write(buf.array());
    }

    private String readFrame() throws IOException {
        if (this.ipcChannel == null) {
            return null;
        }
        byte[] header = new byte[8];
        this.ipcChannel.readFully(header);
        ByteBuffer hBuf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        int op = hBuf.getInt();
        int len = hBuf.getInt();
        if (len <= 0 || len > 65536) {
            return null;
        }
        byte[] body = new byte[len];
        this.ipcChannel.readFully(body);
        return new String(body, StandardCharsets.UTF_8);
    }

    @Environment(value=EnvType.CLIENT)
    private static interface IPCChannel
    extends Closeable {
        public void write(byte[] var1) throws IOException;

        public void readFully(byte[] var1) throws IOException;
    }

    @Environment(value=EnvType.CLIENT)
    private static class WindowsPipeChannel
    implements IPCChannel {
        private final RandomAccessFile file;

        public WindowsPipeChannel(String pipePath) throws IOException {
            this.file = new RandomAccessFile(pipePath, "rw");
        }

        @Override
        public void write(byte[] data) throws IOException {
            this.file.write(data);
        }

        @Override
        public void readFully(byte[] dst) throws IOException {
            this.file.readFully(dst);
        }

        @Override
        public void close() throws IOException {
            this.file.close();
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class UnixDomainChannel
    implements IPCChannel {
        private final SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);

        public UnixDomainChannel(String socketPath) throws IOException {
            this.channel.connect(UnixDomainSocketAddress.of(socketPath));
        }

        @Override
        public void write(byte[] data) throws IOException {
            ByteBuffer buf = ByteBuffer.wrap(data);
            while (buf.hasRemaining()) {
                this.channel.write(buf);
            }
        }

        @Override
        public void readFully(byte[] dst) throws IOException {
            ByteBuffer buf = ByteBuffer.wrap(dst);
            while (buf.hasRemaining()) {
                int read = this.channel.read(buf);
                if (read >= 0) continue;
                throw new IOException("End of stream reached");
            }
        }

        @Override
        public void close() throws IOException {
            this.channel.close();
        }
    }
}

