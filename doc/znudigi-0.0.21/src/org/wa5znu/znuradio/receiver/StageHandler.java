/*
 * This file is part of znudigi.
 * Released under GNU GENERAL PUBLIC LICENSE Version 2
 * See file COPYING.
 * Copyright (C) 2007-2008 Leigh L. Klotz, Jr. <Leigh@WA5ZNU.org>
 */

package org.wa5znu.znuradio.receiver;

import org.wa5znu.znuradio.dsp.Complex;

public interface StageHandler {
  public abstract void handleStage(int frame, double data[], int length);
  public abstract void handleStage(int frame, Complex data[], int length);
}
