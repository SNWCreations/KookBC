package snw.kookbc.impl.event.internal;

import snw.jkook.event.EventHandler;
import snw.jkook.event.Listener;
import snw.jkook.event.channel.ChannelInfoUpdateEvent;
import snw.jkook.event.channel.ChannelMessageUpdateEvent;
import snw.jkook.event.guild.GuildInfoUpdateEvent;
import snw.jkook.event.guild.GuildUpdateEmojiEvent;
import snw.jkook.event.guild.GuildUserNickNameUpdateEvent;
import snw.jkook.event.pm.PrivateMessageUpdateEvent;
import snw.jkook.event.role.RoleInfoUpdateEvent;
import snw.jkook.event.user.UserInfoUpdateEvent;

public class InternalListener implements Listener {

    @EventHandler(internal = true)
    public void event(ChannelInfoUpdateEvent event) {
    }

    @EventHandler(internal = true)
    public void event(ChannelMessageUpdateEvent event) {
    }

    @EventHandler(internal = true)
    public void event(GuildUpdateEmojiEvent event) {
    }

    @EventHandler(internal = true)
    public void event(GuildUserNickNameUpdateEvent event) {
    }

    @EventHandler(internal = true)
    public void event(GuildInfoUpdateEvent event) {
    }

    @EventHandler(internal = true)
    public void event(RoleInfoUpdateEvent event) {
    }

    @EventHandler
    public void event(PrivateMessageUpdateEvent event) {
    }

    @EventHandler
    public void event(UserInfoUpdateEvent event) {
    }

}
