/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 - 2023 KookBC contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package snw.kookbc.impl.event;

import snw.jkook.event.EventHandler;
import snw.jkook.event.Listener;
import snw.jkook.event.user.UserJoinVoiceChannelEvent;
import snw.jkook.event.user.UserLeaveVoiceChannelEvent;
import snw.kookbc.impl.entity.UserImpl;

// The internal event listener for processing things related to events before the Bot-defined listeners got call.
// This class won't work in other JKook implementations.
public class InternalEventListener implements Listener {

    @EventHandler(internal = true)
    public void onUserJoinChannel(UserJoinVoiceChannelEvent event) {
        ((UserImpl) event.getUser()).setJoinedChannel(event.getChannel());
    }

    @EventHandler(internal = true)
    public void onUserLeaveChannel(UserLeaveVoiceChannelEvent event) {
        ((UserImpl) event.getUser()).setJoinedChannel(null);
    }

}
