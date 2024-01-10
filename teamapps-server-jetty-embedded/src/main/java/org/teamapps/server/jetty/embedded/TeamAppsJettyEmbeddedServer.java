/*-
 * ========================LICENSE_START=================================
 * TeamApps
 * ---
 * Copyright (C) 2014 - 2023 TeamApps.org
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package org.teamapps.server.jetty.embedded;

import jakarta.servlet.ServletContextListener;
import org.eclipse.jetty.ee10.webapp.Configuration;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.ee10.webapp.WebXmlConfiguration;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.teamapps.client.ClientCodeExtractor;
import org.teamapps.config.TeamAppsConfiguration;
import org.teamapps.core.TeamAppsCore;
import org.teamapps.util.threading.CompletableFutureChainSequentialExecutorFactory;
import org.teamapps.ux.servlet.TeamAppsServletContextListener;
import org.teamapps.webcontroller.WebController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class TeamAppsJettyEmbeddedServer {

	public static final int DEFAULT_PORT = 8080;

	private final TeamAppsCore teamAppsCore;
	private final File webAppDirectory;
	private final List<ServletContextListener> customServletContextListeners = new ArrayList<>();

	private final Server server;
	private final WebAppContext webapp;

	public TeamAppsJettyEmbeddedServer(WebController webController, File webAppDirectory, TeamAppsConfiguration config, int port, boolean globalGzipCompression) {
		teamAppsCore = new TeamAppsCore(config, new CompletableFutureChainSequentialExecutorFactory(config.getMaxNumberOfSessionExecutorThreads()), webController);
		this.webAppDirectory = webAppDirectory;

		server = new Server(port);
		webapp = new WebAppContext();
		webapp.setClassLoader(TeamAppsJettyEmbeddedServer.class.getClassLoader());
		webapp.setConfigurations(new Configuration[]{new WebXmlConfiguration()});
		webapp.getServerClassMatcher().exclude("org.eclipse.jetty.");
		webapp.setContextPath("/");
		webapp.addEventListener(new TeamAppsServletContextListener(teamAppsCore));
		webapp.setBaseResourceAsPath(webAppDirectory.getAbsoluteFile().toPath());
		webapp.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
		// The following will not actually set the secure flag on the cookie if the session is started without encryption.
		// Use getWebapp().getSessionHandler().getSessionCookieConfig().setSecure(true) if you want to force secure cookies.
		webapp.getSessionHandler().setSecureRequestOnly(true);
		if (globalGzipCompression) {
			GzipHandler gzipHandler = new GzipHandler();
			gzipHandler.setInflateBufferSize(32 * 1024);
			server.setHandler(gzipHandler); // GZIP handler set at the server level will apply compression to everything, including resources and dynamic content
			gzipHandler.setHandler(webapp);
		} else {
			server.setHandler(webapp);
		}
		JakartaWebSocketServletContainerInitializer.configure(webapp, null);
	}

	public Server getServer() {
		return server;
	}

	public WebAppContext getWebapp() {
		return webapp;
	}

	public TeamAppsCore getTeamAppsCore() {
		return teamAppsCore;
	}

	public void addServletContextListener(ServletContextListener servletContextListener) {
		if (server.isRunning()) {
			throw new IllegalStateException("ServletContextListeners need to be registered before the server is started!");
		}
		this.customServletContextListeners.add(servletContextListener);
	}

	public void start() throws Exception {
		ClientCodeExtractor.initializeWebserverDirectory(webAppDirectory);
		customServletContextListeners.forEach(webapp::addEventListener);
		server.start();
		server.join();
	}

	public void stop() throws Exception {
		server.stop();
	}

	public static class Builder {

		private WebController webController;
		private File webAppDirectory;
		private TeamAppsConfiguration config = new TeamAppsConfiguration();
		private int port = DEFAULT_PORT;
		private boolean globalGzipCompression;

		public Builder setWebController(WebController webController) {
			this.webController = webController;
			return this;
		}

		public Builder setWebAppDirectory(File webAppDirectory) {
			this.webAppDirectory = webAppDirectory;
			return this;
		}

		public Builder setConfig(TeamAppsConfiguration config) {
			this.config = config;
			return this;
		}

		public Builder setPort(int port) {
			this.port = port;
			return this;
		}

		public Builder setGlobalGzipCompression(boolean globalGzipCompression) {
			this.globalGzipCompression = globalGzipCompression;
			return this;
		}

		public TeamAppsJettyEmbeddedServer build() throws IOException {
			if (webAppDirectory == null) {
				webAppDirectory = Files.createTempDirectory("teamapps").toFile();
			}
			return new TeamAppsJettyEmbeddedServer(webController, webAppDirectory, config, port, globalGzipCompression);
		}
	}

}
