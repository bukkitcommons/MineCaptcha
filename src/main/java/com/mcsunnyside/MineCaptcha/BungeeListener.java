package com.mcsunnyside.MineCaptcha;

import com.mcsunnyside.MineCaptcha.Database.DatabaseHelper;
import com.mcsunnyside.MineCaptcha.Database.PlayerQueryResult;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import java.sql.SQLException;


public class BungeeListener implements Listener {
    private MineCaptcha plugin;
    private String MSG_userVerifyFailed;
    private String MSG_ipVerifyFailed;
    private String MSG_usernameNotMatched;
    private String MSG_sqlFailed;
    private int maxJoinPlayers;
    private int lastJoinedPlayers;
    private int currentJoinedPlayers;
    private long lastResetTime;

    public BungeeListener (MineCaptcha plugin){
        this.plugin = plugin;
        this.MSG_userVerifyFailed = plugin.getConfig().getString("messages.userVerifyFailed");
        this.MSG_ipVerifyFailed = plugin.getConfig().getString("messages.ipVerifyFailed");
        this.MSG_usernameNotMatched = plugin.getConfig().getString("messages.usernameNotMatched");
        this.MSG_sqlFailed = plugin.getConfig().getString("messages.MSG_sqlFailed");
        this.maxJoinPlayers = plugin.getConfig().getInt("max-join-players");
    }
    //使用最低优先级，这样我们可以更早的处理事件
    //然后尽快取消和掐断链接，缓解服务器压力
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(LoginEvent e){
        if(e.isCancelled()) //忽略已经取消的事件，因为已经取消了就肯定会被踢了..
            return;
        currentJoinedPlayers++;
        if(System.currentTimeMillis() - lastResetTime > 600000){
            lastJoinedPlayers = currentJoinedPlayers;
            currentJoinedPlayers = 0;
            lastResetTime = System.currentTimeMillis();
        }
        if(lastJoinedPlayers < maxJoinPlayers)
            return;
        PlayerQueryResult result;
        String username = e.getConnection().getName();
        BaseComponent[] kickMsg;
        try {
            result = DatabaseHelper.queryPlayer(plugin.getDatabase(), plugin, e.getConnection().getName());
        }catch (SQLException sqle){
            sqle.printStackTrace();
            kickMsg = TextComponent.fromLegacyText(this.MSG_sqlFailed.replaceAll("%username%",username ));
            e.setCancelled(true); //取消事件，让其他有判断的插件跳过检查，减轻服务器压力
            e.setCancelReason(kickMsg);
            e.getConnection().disconnect(kickMsg); //强制断开链接，而不要等到事件处理完毕，节约资源
            return;
        }
        if(!result.isSuccess()){
            kickMsg =TextComponent.fromLegacyText(this.MSG_userVerifyFailed.replaceAll("%username%",username));
            e.setCancelled(true);
            e.setCancelReason(kickMsg);
            e.getConnection().disconnect(kickMsg);
            return;
        }
        if(!result.getUsername().equals(e.getConnection().getName())){
            kickMsg =TextComponent.fromLegacyText(this.MSG_usernameNotMatched.replaceAll("%username%",username));
            e.setCancelled(true);
            e.setCancelReason(kickMsg);
            e.getConnection().disconnect(kickMsg);
            return;
        }
        if(plugin.getConfig().getBoolean("ip-strict-mode")){
            if(!result.getIpAddress().equals(e.getConnection().getAddress().getAddress().getHostAddress())){
                kickMsg = TextComponent.fromLegacyText(this.MSG_ipVerifyFailed.replaceAll("%username%",username));
                e.setCancelled(true);
                e.setCancelReason(kickMsg);
                e.getConnection().disconnect(kickMsg);
            }
        }

    }
}
