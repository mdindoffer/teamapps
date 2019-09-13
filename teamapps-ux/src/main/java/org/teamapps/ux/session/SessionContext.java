/*-
 * ========================LICENSE_START=================================
 * TeamApps
 * ---
 * Copyright (C) 2014 - 2019 TeamApps.org
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
package org.teamapps.ux.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.common.format.Color;
import org.teamapps.dto.UiCommand;
import org.teamapps.dto.UiRootPanel;
import org.teamapps.event.Event;
import org.teamapps.icons.api.Icon;
import org.teamapps.icons.api.IconTheme;
import org.teamapps.server.CommandDispatcher;
import org.teamapps.server.UxServerContext;
import org.teamapps.uisession.QualifiedUiSessionId;
import org.teamapps.util.UiUtil;
import org.teamapps.ux.component.Component;
import org.teamapps.ux.component.animation.EntranceAnimation;
import org.teamapps.ux.component.animation.ExitAnimation;
import org.teamapps.ux.component.notification.Notification;
import org.teamapps.ux.component.notification.NotificationPosition;
import org.teamapps.ux.component.popup.Popup;
import org.teamapps.ux.component.rootpanel.RootPanel;
import org.teamapps.ux.component.template.Template;
import org.teamapps.ux.component.template.TemplateReference;
import org.teamapps.ux.component.window.Window;
import org.teamapps.ux.i18n.MultiResourceBundle;
import org.teamapps.ux.i18n.UTF8Control;
import org.teamapps.ux.json.UxJacksonSerializationTemplate;
import org.teamapps.ux.resource.Resource;
import org.teamapps.ux.task.ObservableProgress;
import org.teamapps.ux.task.Progress;
import org.teamapps.ux.task.ProgressReportingRunnable;

import java.io.File;
import java.io.InputStream;
import java.text.MessageFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SessionContext {

	private static final Logger LOGGER = LoggerFactory.getLogger(SessionContext.class);
	private static final int LOCK_TIMEOUT = 60_000;
	private static final Function<Locale, ResourceBundle> DEFAULT_RESOURCE_BUNDLE_PROVIDER = locale -> ResourceBundle.getBundle("org.teamapps.ux.i18n.DefaultCaptions", locale, new UTF8Control());

	private final Event<Void> onDestroyed = new Event<>();

	private final ReentrantLock lock = new ReentrantLock();

	private boolean isValid = true;
	private long lastClientEventTimeStamp;

	private final QualifiedUiSessionId sessionId;
	private final ClientInfo clientInfo;
	private final CommandDispatcher commandDispatcher;
	private final UxServerContext serverContext;
	private final UxJacksonSerializationTemplate uxJacksonSerializationTemplate;
	private final HashMap<String, Component> componentsById = new HashMap<>();
	private IconTheme iconTheme;
	private ClientSessionResourceProvider sessionResourceProvider;

	private Function<Locale, ResourceBundle> customMessageBundleProvider = DEFAULT_RESOURCE_BUNDLE_PROVIDER;
	private ResourceBundle messagesBundle;

	private Map<String, Template> registeredTemplates = new ConcurrentHashMap<>();
	private SessionConfiguration sessionConfiguration = SessionConfiguration.createDefault();

	public SessionContext(QualifiedUiSessionId sessionId, ClientInfo clientInfo, CommandDispatcher commandDispatcher, UxServerContext serverContext, IconTheme iconTheme,
	                      ObjectMapper jacksonObjectMapper) {
		this.sessionId = sessionId;
		this.clientInfo = clientInfo;
		this.commandDispatcher = commandDispatcher;
		this.serverContext = serverContext;
		this.iconTheme = iconTheme;
		this.sessionResourceProvider = new ClientSessionResourceProvider(sessionId);
		this.uxJacksonSerializationTemplate = new UxJacksonSerializationTemplate(jacksonObjectMapper, this);
		this.lastClientEventTimeStamp = System.currentTimeMillis();
		updateMessageBundle();
	}

	public void setCustomMessageBundleProvider(Function<Locale, ResourceBundle> provider) {
		this.customMessageBundleProvider = provider;
		updateMessageBundle();
	}

	private void updateMessageBundle() {
		if (customMessageBundleProvider != null) {
			ResourceBundle customResourceBundle = customMessageBundleProvider.apply(sessionConfiguration.getLanguageLocale());
			ResourceBundle defaultResourceBundle = DEFAULT_RESOURCE_BUNDLE_PROVIDER.apply(sessionConfiguration.getLanguageLocale());
			this.messagesBundle = new MultiResourceBundle(customResourceBundle, defaultResourceBundle);
		} else {
			this.messagesBundle = DEFAULT_RESOURCE_BUNDLE_PROVIDER.apply(sessionConfiguration.getLanguageLocale());
		}
	}

	public Locale getLocale() {
		return sessionConfiguration.getLanguageLocale();
	}

	public ResourceBundle getMessageBundle() {
		return messagesBundle;
	}

	public String getLocalized(String key, Object... parameters) {
		String value = messagesBundle.getString(key);
		if (parameters != null) {
			return MessageFormat.format(value, parameters);
		}
		return value;
	}

	public long getLastClientEventTimestamp() {
		return lastClientEventTimeStamp;
	}

	public void setLastClientEventTimestamp(long timestamp) {
		lastClientEventTimeStamp = timestamp;
	}

	public boolean isOpen() {
		return isValid;
	}

	public void destroy() {
		isValid = false;
		commandDispatcher.close();
		runWithContext(() -> {
			onDestroyed.fire(null);
		});
	}

	public Event<Void> onDestroyed() {
		return onDestroyed;
	}

	public <RESULT> void queueCommand(UiCommand<RESULT> command, Consumer<RESULT> resultCallback) {
		if (CurrentSessionContext.get() != this) {
			String errorMessage = "Trying to queue a command for foreign/null SessionContext (CurrentSessionContext.get() != this)."
					+ " Please use SessionContext.runWithContext(Runnable). NOTE: The command will not get queued!";
			LOGGER.error(errorMessage);
			throw new IllegalStateException(errorMessage);
		}
		Consumer<RESULT> wrappedCallback = resultCallback != null ? result -> this.runWithContext(() -> resultCallback.accept(result)) : null;
		commandDispatcher.queueCommand(command, wrappedCallback);
	}

	public <RESULT> void queueCommand(UiCommand<RESULT> command) {
		this.queueCommand(command, null);
	}

	public ClientInfo getClientInfo() {
		return clientInfo;
	}

	public void flushCommands() {
		uxJacksonSerializationTemplate.doWithUxJacksonSerializers(commandDispatcher::flushCommands);
	}

	public IconTheme getIconTheme() {
		return iconTheme;
	}

	public void setIconTheme(IconTheme theme) {
		this.iconTheme = theme;
	}

	public String createFileLink(File file) {
		return sessionResourceProvider.createFileLink(file);
	}

	public String createResourceLink(Resource resource, String uniqueIdentifier) {
		return sessionResourceProvider.createResourceLink(resource, uniqueIdentifier);
	}

	public Resource getBinaryResource(int resourceId) {
		return sessionResourceProvider.getBinaryResource(resourceId);
	}

	public File getUploadedFileByUuid(String uuid) {
		return this.serverContext.getUploadedFileByUuid(uuid);
	}

	public TemplateReference registerTemplate(String id, Template template) {
		registeredTemplates.put(id, template);
		queueCommand(new UiRootPanel.RegisterTemplateCommand(id, template.createUiTemplate()));
		return new TemplateReference(template);
	}

	public void registerTemplates(Map<String, Template> templates) {
		registeredTemplates.putAll(templates);
		queueCommand(new UiRootPanel.RegisterTemplatesCommand(templates.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().createUiTemplate()))));
	}

	public Template getTemplate(String id) {
		return registeredTemplates.get(id);
	}

	private void lock(long timeoutMillis) {
		try {
			boolean locked = lock.tryLock(timeoutMillis, TimeUnit.MILLISECONDS);
			if (!locked) {
				String errorMessage = "Could not acquire lock for SessionContext " + sessionId + " after " + timeoutMillis + "ms.";
				LOGGER.error(errorMessage);
				throw new IllegalStateException(errorMessage);
			}
		} catch (InterruptedException e) {
			String errorMessage = "Could not acquire lock for SessionContext " + sessionId + ". Thread was interrupted while waiting for the lock!";
			LOGGER.error(errorMessage);
			throw new IllegalStateException(errorMessage);
		}
	}

	private void unlock() {
		lock.unlock();
	}

	/**
	 * Does the following:
	 * <ol>
	 * <li>Releases the current SessionContext (A) lock (if present)</li>
	 * <li>Acquires the lock for this SessionContext (B)</li>
	 * <li>Sets this SessionContext (B) as the current context (CurrentSessionContext).</li>
	 * <li>Executes the specified Runnable.</li>
	 * <li>Sets back the last SessionContext (A) as current context.</li>
	 * <li>Releases the lock for this SessionContext (B)</li>
	 * <li>Reacquires the lock for the last (A)</li>
	 * </ol>
	 *
	 * @param runnable the code to be executed.
	 */
	public void runWithContext(Runnable runnable) {
		if (CurrentSessionContext.getOrNull() == this) {
			// Fast lane! This context is already bound to this thread. Just execute the runnable.
			runnable.run();
		} else {
			if (CurrentSessionContext.getOrNull() != null) {
				// unlock the previously locked sessionContext (WITHOUT POPPING IT!)
				CurrentSessionContext.getOrNull().unlock();
			}
			try {
				long startTime = System.currentTimeMillis();
				lock(LOCK_TIMEOUT);
				try {
					CurrentSessionContext.pushContext(this);
					try {
						runnable.run();
					} catch (Exception e) {
						LOGGER.error("Exception while executing runnable! Context: " + sessionId.toString(), e);
						throw e;
					} finally {
						CurrentSessionContext.popContext();
					}
				} finally {
					unlock();
					long endTime = System.currentTimeMillis();
					if (endTime - startTime > LOCK_TIMEOUT) {
						String message = "The execution within the session context {} took dangerously long (longer than the session's lock timeout). This might have caused other threads to fail "
								+ "acquiring the session's lock. Stacktrace follows:";
						LOGGER.warn(message, sessionId, new RuntimeException(message));
					}
				}
			} finally {
				if (CurrentSessionContext.getOrNull() != null) {
					// relock the previously locked sessionContext
					CurrentSessionContext.getOrNull().lock(Long.MAX_VALUE); // we WANT to wait here.
				}
			}
			this.flushCommands();
		}
	}

	public SessionConfiguration getConfiguration() {
		return sessionConfiguration;
	}

	public void setConfiguration(SessionConfiguration config) {
		this.sessionConfiguration = config;
		updateMessageBundle();
		queueCommand(new UiRootPanel.SetConfigCommand(config.createUiConfiguration()));
	}

	public void showPopupAtCurrentMousePosition(Popup popup) {
		queueCommand(new UiRootPanel.ShowPopupAtCurrentMousePositionCommand(popup.createUiComponentReference()));
	}

	public void showPopup(Popup popup) {
		queueCommand(new UiRootPanel.ShowPopupCommand(popup.createUiComponentReference()));
	}

	public ObservableProgress executeBackgroundTask(Icon icon, String taskName, boolean cancelable, ProgressReportingRunnable runnable) {
		return executeBackgroundTask(icon, taskName, cancelable, runnable, ForkJoinPool.commonPool());
	}

	public ObservableProgress executeBackgroundTask(Icon icon, String taskName, boolean cancelable, ProgressReportingRunnable runnable, Executor executor) {
		Progress progress = new Progress(icon, taskName, cancelable);
		executor.execute(() -> {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			progress.start();
			try {
				runnable.run(progress);
			} catch (Exception e) {
				LOGGER.error("Error in background task", e);
				progress.markFailed(this.getLocalized("dict.error"), e);
				return;
			}
			progress.markCompleted();
		});
		return progress;
	}

	public Locale getLanguageLocale() {
		return getConfiguration().getLanguageLocale();
	}

	public ZoneId getTimeZone() {
		return getConfiguration().getTimeZone();
	}

	public String resolveIcon(Icon icon) {
		if (icon == null) {
			return null;
		}
		return icon.getQualifiedIconId(getIconTheme());
	}

	public void registerComponent(Component component) {
		componentsById.put(component.getId(), component);
	}

	public void unregisterComponent(Component component) {
		componentsById.remove(component.getId());
	}

	public Component getComponent(String componentId) {
		return componentsById.get(componentId);
	}

	public String createResourceLink(Supplier<InputStream> inputStreamSupplier, long length) {
		return createResourceLink(inputStreamSupplier, length, null);
	}

	public String createResourceLink(Supplier<InputStream> inputStreamSupplier, long length, String resourceName) {
		return createResourceLink(inputStreamSupplier, length, resourceName, null);
	}

	public String createResourceLink(Supplier<InputStream> inputStreamSupplier, long length, String resourceName, String uniqueIdentifier) {
		return createResourceLink(new Resource() {
			@Override
			public InputStream getInputStream() {
				return inputStreamSupplier.get();
			}

			@Override
			public long getLength() {
				return length;
			}

			@Override
			public Date getLastModified() {
				return new Date();
			}

			@Override
			public Date getExpires() {
				return new Date(System.currentTimeMillis() + 600000000L);
			}

			@Override
			public String getName() {
				return resourceName;
			}

			@Override
			public String getMimeType() {
				return null;
			}
		}, uniqueIdentifier);
	}

	public String createResourceLink(Resource resource) {
		return createResourceLink(resource, null);
	}

	public void showWindow(Window window, int animationDuration) {
		queueCommand(new UiRootPanel.ShowWindowCommand(window.createUiComponentReference(), animationDuration));
	}

	public void closeWindow(Window window, int animationDuration) {
		queueCommand(new UiRootPanel.CloseWindowCommand(window.getId(), animationDuration));
	}

	public void closeWindow(String windowId, int animationDuration) {
		queueCommand(new UiRootPanel.CloseWindowCommand(windowId, animationDuration));
	}

	public void downloadFile(String fileUrl, String downloadFileName) {
		queueCommand(new UiRootPanel.DownloadFileCommand(fileUrl, downloadFileName));
	}

	public void registerBackgroundImage(String id, String image, String blurredImage) {
		queueCommand(new UiRootPanel.RegisterBackgroundImageCommand(id, image, blurredImage));
	}

	public void setBackgroundImage(String id, int animationDuration) {
		queueCommand(new UiRootPanel.SetBackgroundImageCommand(id, animationDuration));
	}

	public void setBackgroundColor(Color color, int animationDuration) {
		queueCommand(new UiRootPanel.SetBackgroundColorCommand(UiUtil.createUiColor(color), animationDuration));
	}

	public void exitFullScreen() {
		queueCommand(new UiRootPanel.ExitFullScreenCommand());
	}

	public void addRootComponent(String containerElementId, RootPanel rootPanel) {
		queueCommand(new UiRootPanel.BuildRootPanelCommand(containerElementId, rootPanel.createUiComponentReference()));
	}

	public void addClientToken(String token) {
		queueCommand(new UiRootPanel.AddClientTokenCommand(token));
	}

	public void removeClientToken(String token) {
		queueCommand(new UiRootPanel.RemoveClientTokenCommand(token));
	}

	public void clearClientTokens() {
		queueCommand(new UiRootPanel.ClearClientTokensCommand());
	}

	public void showNotification(Notification notification) {
		queueCommand(new UiRootPanel.ShowNotificationCommand(notification.createUiNotification()));
	}

	public void showNotification(Icon icon, String caption) {
		Notification notification = Notification.createWithIconAndCaption(icon, caption);
		notification.setPosition(NotificationPosition.TOP_RIGHT);
		notification.setEntranceAnimation(EntranceAnimation.SLIDE_IN_UP);
		notification.setExitAnimation(ExitAnimation.SLIDE_OUT_UP);
		notification.setDismissable(true);
		notification.setShowProgressBar(false);
		notification.setDisplayTimeInMillis(5000);
		showNotification(notification);
	}

	public void showNotification(Icon icon, String caption, String description, boolean dismissable, int displayTimeInMillis, boolean showProgress) {
		Notification notification = Notification.createWithIconAndTextAndDescription(icon, caption, description);
		notification.setPosition(NotificationPosition.TOP_RIGHT);
		notification.setEntranceAnimation(EntranceAnimation.SLIDE_IN_LEFT);
		notification.setExitAnimation(ExitAnimation.FADE_OUT_UP);
		notification.setDismissable(dismissable);
		notification.setDisplayTimeInMillis(displayTimeInMillis);
		notification.setShowProgressBar(showProgress);
		showNotification(notification);
	}

}
