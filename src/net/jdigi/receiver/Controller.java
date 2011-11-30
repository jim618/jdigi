/*
 * This file is part of znudigi.
 * Released under GNU GENERAL PUBLIC LICENSE Version 2
 * See file COPYING.
 * Copyright (C) 2007-2008 Leigh L. Klotz, Jr. <Leigh@WA5ZNU.org>
 */

package net.jdigi.receiver;

public interface Controller {
	public void setFrequency(double f);

	public void setFrequency(double f, boolean userClick);

	public void setSampleRate(int f);

	public abstract void handleSpectrum(int frame, double data[], int length);

	public abstract void handleText(int frame, String s);

}
