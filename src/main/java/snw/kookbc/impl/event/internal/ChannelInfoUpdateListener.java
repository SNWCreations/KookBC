package snw.kookbc.impl.event.internal;

import snw.jkook.event.EventHandler;
import snw.jkook.event.Listener;
import snw.jkook.event.channel.ChannelInfoUpdateEvent;

public class ChannelInfoUpdateListener implements Listener {

    @EventHandler(internal = true)
    public void event(ChannelInfoUpdateEvent event) {
        // An empty listener that updates the channel's information as it should
    }

}
