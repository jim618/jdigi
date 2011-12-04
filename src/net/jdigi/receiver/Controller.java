/*
 * This file is part of znudigi.
 * Released under GNU GENERAL PUBLIC LICENSE Version 2
 * See file COPYING.
 * Copyright (C) 2007-2008 Leigh L. Klotz, Jr. <Leigh@WA5ZNU.org>
 */

package net.jdigi.receiver;

public interface Controller {
	public void setReceiverFrequency(double frequency);

	public void setTransmitterFrequency(double frequency);

	public void setSampleRate(int frameRate);

	public abstract void handleSpectrum(int frame, double data[], int length);

	public abstract void handleText(int frame, String text);

}
