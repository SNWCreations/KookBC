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
import snw.kookbc.impl.event.EventManagerImpl;
import snw.kookbc.impl.plugin.SimplePluginManager;
import snw.kookbc.impl.scheduler.SchedulerImpl;

import java.io.IOException;
import java.util.Properties;

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

    // Note for hardcore developers:
    // Use this instead of CoreImpl(Logger) constructor, unless you want the logging output.
    public CoreImpl() {
        this.logger = NOPLogger.NOP_LOGGER;
    }

    public CoreImpl(Logger logger) {
        this.logger = logger;
        getLogger().info("Starting {} version {}", getImplementationName(), getImplementationVersion());
        getLogger().info("This VM is running {} version {} (Implementing API version {})", getImplementationName(), getImplementationVersion(), getAPIVersion());
        Properties gitProperties = new Properties();
        try {
            gitProperties.load(CoreImpl.class.getClassLoader().getResourceAsStream("kookbc_git_data.properties"));
            getLogger().info("Compiled from Git commit {}, build at {}", gitProperties.get("git.commit.id.full"), gitProperties.get("git.build.time"));
        } catch (NullPointerException | IOException e) {
            getLogger().warn("Unable to read Git commit information. {}", e.getMessage());
        }
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
        return ConsoleCommandSenderImpl.INSTANCE;
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

    void init(KBCClient client, HttpAPIImpl impl) {
        Validate.isFalse(init, "This core implementation has already initialized.");
        Validate.notNull(client);
        Validate.notNull(impl);
        this.client = client;
        this.httpApi = impl;
        this.pluginManager = new SimplePluginManager(client);
        this.client.loadAllPlugins();
        this.unsafe = new UnsafeImpl(client);
        this.scheduler = new SchedulerImpl(client);
        this.eventManager = new EventManagerImpl(client);
        this.commandManager = new CommandManagerImpl(client);
        this.init = true;
    }
}
