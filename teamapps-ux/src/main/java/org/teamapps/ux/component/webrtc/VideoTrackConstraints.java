package org.teamapps.ux.component.webrtc;

import org.teamapps.dto.UiVideoTrackConstraints;

import java.util.Objects;

public class VideoTrackConstraints {

	private String deviceId = null;
	private int width = 800;
	private int height = 600;
	private VideoFacingMode facingMode = VideoFacingMode.USER;
	private int frameRate = 20;

	public VideoTrackConstraints() {
	}

	public VideoTrackConstraints(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public VideoTrackConstraints(int width, int height, VideoFacingMode facingMode, int frameRate) {
		this.width = width;
		this.height = height;
		this.facingMode = facingMode;
		this.frameRate = frameRate;
	}

	public UiVideoTrackConstraints createUiVideoTrackConstraints() {
		UiVideoTrackConstraints ui = new UiVideoTrackConstraints();
		ui.setHeight(height);
		ui.setWidth(width);
		ui.setFacingMode(facingMode.toUiVideoFacingMode());
		ui.setFrameRate(frameRate);
		ui.setDeviceId(deviceId);
		return ui;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public VideoFacingMode getFacingMode() {
		return facingMode;
	}

	public void setFacingMode(VideoFacingMode facingMode) {
		this.facingMode = facingMode;
	}

	public int getFrameRate() {
		return frameRate;
	}

	public void setFrameRate(int frameRate) {
		this.frameRate = frameRate;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		VideoTrackConstraints that = (VideoTrackConstraints) o;
		return width == that.width &&
				height == that.height &&
				frameRate == that.frameRate &&
				Objects.equals(deviceId, that.deviceId) &&
				facingMode == that.facingMode;
	}

	@Override
	public int hashCode() {
		return Objects.hash(deviceId, width, height, facingMode, frameRate);
	}
}
