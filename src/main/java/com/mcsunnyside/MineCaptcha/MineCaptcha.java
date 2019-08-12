package com.mcsunnyside.MineCaptcha;

import com.mcsunnyside.MineCaptcha.Database.Database;
import com.mcsunnyside.MineCaptcha.Database.DatabaseHelper;
import com.mcsunnyside.MineCaptcha.Database.MySQLCore;
import lombok.Getter;
import lombok.SneakyThrows;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.YamlConfiguration;
import org.bstats.bungeecord.Metrics;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.SQLException;

@Getter
public class MineCaptcha extends Plugin {
    private File configFile;
    private Configuration config;
    private Database database;
    @Override
    @SneakyThrows
    public void onEnable() {
        //noinspection ResultOfMethodCallIgnored
        getDataFolder().mkdirs();
        configFile = new File(getDataFolder(),"config.yml");
        if(!configFile.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, configFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.getProvider(YamlConfiguration.class).load(configFile);
        if(!setupDatabase()){
            getLogger().warning("Failed setup the database, aborting...");
        }
        getProxy().getPluginManager().registerListener(this,new BungeeListener(this));
        /* Setup bStats */
        try{
            new Metrics(this);
        }catch (Throwable th){
            th.printStackTrace();
        }
    }

    @Override
    @SneakyThrows
    public void onDisable() {
    }

    private boolean setupDatabase()  {
            MySQLCore dbCore;
            String user = getConfig().getString("database.user");
            String pass = getConfig().getString("database.pass");
            String host = getConfig().getString("database.host");
            int port = getConfig().getInt("database.port");
            String database = getConfig().getString("database.database");
            boolean useSSL = getConfig().getBoolean("database.usessl");
            dbCore = new MySQLCore(host, user, pass, database, port, useSSL);
            try {
                this.database = new Database(dbCore);
                DatabaseHelper.createInfoTable(this.database,this);
            }catch (Database.ConnectionException | SQLException e){
                e.printStackTrace();
                return false;
            }
            return true;
    }
}
