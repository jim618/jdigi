/*
 * This file is part of znudigi.
 * Released under GNU GENERAL PUBLIC LICENSE Version 2
 * See file COPYING.
 * Copyright (C) 2007-2008 Leigh L. Klotz, Jr. <Leigh@WA5ZNU.org>
 */

package org.wa5znu.znuradio.receiver;

public interface WaveHandler {

  public abstract void handleWave(int frame, double data[], int length);
}
