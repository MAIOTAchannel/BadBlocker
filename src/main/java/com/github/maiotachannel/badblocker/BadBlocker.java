package com.github.maiotachannel.badblocker;

import com.google.inject.Inject;
import com.velocitypowered.api.event.player.PlayerClientBrandEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

@Plugin(
        id = "bad_blocker",
        name = "BadBlocker",
        version = "1.0-SNAPSHOT",
        description = "This is a plugin that detects the use of VPNs, unauthorized mods, and unauthorized clients and kicks the player!",
        authors = {"MAIOTA"}
)
public class BadBlocker{

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public BadBlocker(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory){
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        logger.info(dataDirectory.toString());
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getEventManager().register(this, new Event(server,dataDirectory));
    }

}

class Event{

    ProxyServer server;
    Path dataDir;
    @Inject
    public Event(ProxyServer server, @DataDirectory Path dataDir) {
        this.server = server;
        this.dataDir = dataDir;
        try {
            System.out.println(loadFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<Object,Object> loadFile() throws IOException {

        File loadLocation = new File(dataDir.toFile(), "config.yml");

        if (!loadLocation.isFile()) {
            // If this location does not exist, create it and write the default server from the Velocity.toml config file
            Map<Object,Object> map = new HashMap<Object,Object>() {
                {
                    put("mods", new ArrayList<>(Arrays.asList("minecraft","tfc","forge")));
                }
            };
            saveFile(map);
            return map;
        }

        Map<Object,Object> server = getFileContents(loadLocation);
        return server;
    }

    private Map<Object,Object> getFileContents(File file) throws FileNotFoundException {
        try(BufferedReader br = new BufferedReader(new FileReader(file))) {
            return new  Yaml().loadAs(br, Map.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveFile(Map<Object,Object> server) throws IOException {
        File dataDirectoryFile = this.dataDir.toFile();

        // Make sure the directory exists
        if (!dataDirectoryFile.exists())
            dataDirectoryFile.mkdir(); // TODO ensure it succeeds

        // Create a reference to the file location
        File saveLocation = new File(dataDir.toFile(), "config.yml");

        // Check if file already exists, if not, create it
        if (saveLocation.createNewFile()) {
            // PersistentServer.logger.info("File created: " + saveLocation.getName());
        } else {
            // PersistentServer.logger.info("File already exists.");
        }

        // Write to file
        try {
            FileWriter myWriter = new FileWriter(saveLocation);
            new Yaml().dump(server,myWriter);
            myWriter.close();
            // PersistentServer.logger.info("Successfully wrote to the file.");
        } catch (IOException e) {
            // PersistentServer.logger.info("An error occurred.");
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        if (event.getPlayer().getRemoteAddress().getHostName().equals("127.0.0.1")){return;}
        ReadJson reader = new ReadJson();
        String json1,json2;
        try {
            json1 = reader.readJsonFromUrl("http://ip-api.com/json/" + event.getPlayer().getRemoteAddress().getAddress().getHostAddress() + "?fields=proxy");
            json2 = reader.readJsonFromUrl("https://check.getipintel.net/check.php?ip=" + event.getPlayer().getRemoteAddress().getAddress().getHostAddress() + "&contact=maiotanetwork@gmail.com&flags=m");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (json1.substring(9,14).equals("true") || Float.parseFloat(json2.substring(0,1)) > 0.95 ){
            event.getPlayer().disconnect(Component.text("Your IP has been detected as a VPN. If you have any problems, please contact the server administrator."));
        }
    }

    @Subscribe
    public void onPlayerClientBrand(PlayerClientBrandEvent event){
        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("plugins\\bad_blocker\\config.yml");
        HashMap yamlMap = (HashMap) yaml.load(inputStream);
        if(event.getBrand().contains("forge"))
            System.out.println(yamlMap);
            System.out.println(event.getPlayer().getModInfo().get().getMods().toString());
    }
}
