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

package snw.kookbc.impl;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;
import snw.jkook.Core;
import snw.jkook.HttpAPI;
import snw.jkook.Unsafe;
import snw.jkook.command.CommandManager;
import snw.jkook.command.ConsoleCommandSender;
import snw.jkook.entity.User;
import snw.jkook.event.EventManager;
import snw.jkook.plugin.PluginManager;
import snw.jkook.scheduler.Scheduler;
import snw.jkook.util.Validate;
import snw.kookbc.SharedConstants;
import snw.kookbc.impl.command.CommandManagerImpl;
import snw.kookbc.impl.command.ConsoleCommandSenderImpl;
import snw.kookbc.impl.command.cloud.CloudCommandManagerImpl;
import snw.kookbc.impl.event.EventManagerImpl;
import snw.kookbc.impl.plugin.SimplePluginManager;
import snw.kookbc.impl.scheduler.SchedulerImpl;

import java.util.Optional;

public class CoreImpl implements Core {
    private boolean init = false;
    private SchedulerImpl scheduler;
    private CommandManagerImpl commandManager;
    private EventManagerImpl eventManager;
    private UnsafeImpl unsafe;
    private final Logger logger;
    private KBCClient client;
    private HttpAPI httpApi;
    private SimplePluginManager pluginManager;
    private User botUser;
    private ConsoleCommandSender ccs;

    // Note for hardcore developers:
    // Use this instead of CoreImpl(Logger) constructor, unless you want the logging output.
    public CoreImpl() {
        this(NOPLogger.NOP_LOGGER);
    }

    public CoreImpl(Logger logger) {
        this.logger = logger;
    }

    @Override
    public HttpAPI getHttpAPI() {
        return httpApi;
    }

    @Override
    public String getAPIVersion() {
        return SharedConstants.SPEC_VERSION;
    }

    @Override
    public String getImplementationName() {
        return SharedConstants.IMPL_NAME;
    }

    @Override
    public String getImplementationVersion() {
        return SharedConstants.IMPL_VERSION;
    }

    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public EventManager getEventManager() {
        return eventManager;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public ConsoleCommandSender getConsoleCommandSender() {
        return ccs;
    }

    @Override
    public CommandManager getCommandManager() {
        return commandManager;
    }

    @Override
    public PluginManager getPluginManager() {
        return pluginManager;
    }

    @Override
    public User getUser() {
        return botUser;
    }

    @Override
    public void setUser(User user) throws IllegalStateException {
        if (this.botUser != null) {
            throw new IllegalStateException("A user has already bound to this core implementation.");
        }
        this.botUser = user;
    }

    @Override
    public Unsafe getUnsafe() {
        return unsafe;
    }

    @Override
    public void shutdown() {
        // If we don't use another thread for calling the real shutdown method,
        //  the client won't be terminated if you called this in a scheduler task.
        new Thread(client::shutdown, "Shutdown Thread").start();
    }

    // Just a friendly way to get the client instance.
    @SuppressWarnings("unused")
    public KBCClient getClient() {
        return client;
    }

    protected synchronized void init(KBCClient client) {
        this.init(client, null, null, null, null, null, null);
    }

    // pass null to an argument if you don't need to replace it using your version.
    protected synchronized void init(
            KBCClient client,
            @Nullable SimplePluginManager simplePluginManager,
            @Nullable HttpAPIImpl httpApiImpl,
            @Nullable SchedulerImpl schedulerImpl,
            @Nullable EventManagerImpl eventManagerImpl,
            @Nullable CommandManagerImpl commandManagerImpl,
            @Nullable UnsafeImpl unsafeImpl
    ) {
        Validate.isFalse(this.init, "This core implementation has already initialized.");
        Validate.notNull(client);
        this.client = client;
        this.pluginManager = Optional.ofNullable(simplePluginManager).orElseGet(() -> new SimplePluginManager(client));
        this.client.loadAllPlugins();
        this.httpApi = Optional.ofNullable(httpApiImpl).orElseGet(() -> new HttpAPIImpl(client));
        this.scheduler = Optional.ofNullable(schedulerImpl).orElseGet(() -> new SchedulerImpl(client));
        this.eventManager = Optional.ofNullable(eventManagerImpl).orElseGet(() -> new EventManagerImpl(client));
        /*Cloud*/
        this.commandManager = Optional.ofNullable(commandManagerImpl).orElseGet(() -> new CloudCommandManagerImpl(client));
        this.unsafe = Optional.ofNullable(unsafeImpl).orElseGet(() -> new UnsafeImpl(client));
        this.ccs = ConsoleCommandSenderImpl.get(client.getInternalPlugin());
        this.init = true;
    }
}
