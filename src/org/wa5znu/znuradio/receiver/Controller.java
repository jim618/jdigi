/*
 * This file is part of znudigi.
 * Released under GNU GENERAL PUBLIC LICENSE Version 2
 * See file COPYING.
 * Copyright (C) 2007-2008 Leigh L. Klotz, Jr. <Leigh@WA5ZNU.org>
 */

package org.wa5znu.znuradio.receiver;

public interface Controller extends ReceiverHandler {
  public void setFrequency(double f);
  public void setFrequency(double f, boolean userClick);
  public void setSampleRate(int f);
  public void showNextStage();
}
