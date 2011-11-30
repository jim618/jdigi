/*
 * This file is part of znudigi.
 * Released under GNU GENERAL PUBLIC LICENSE Version 2
 * See file COPYING.
 * Copyright (C) 2007-2008 Leigh L. Klotz, Jr. <Leigh@WA5ZNU.org>
 */

package org.wa5znu.znuradio.receiver;

import org.wa5znu.znuradio.modems.bpsk.BPSKDemodulator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;


public class ModemThread extends Thread
    implements WaveHandler
{
  Demodulator demodulator;
  double waveDataIn[]=null;
  int dataLengthIn;
  int dataFrameIn;

  private static final Object lock = new Object();

  public ModemThread(Demodulator demodulator) {
    this.demodulator = demodulator;
  }

  public void setFrequency(double f) {
    demodulator.setFrequency(f);
  }

  public double getFrequency() {
    return demodulator.getFrequency();
  }

  public void showNextStage() {
    if (demodulator instanceof FrequencyHandler)
      ((FrequencyHandler)demodulator).showNextStage();
  }

  public void run() {
    while (true) {
      double waveData[]=null;
      int dataLength=0;
      int dataFrame=0;
      synchronized(lock) {
	try {
	  lock.wait();
	  dataFrame = dataFrameIn;
	  waveData = waveDataIn;
	  dataLength = dataLengthIn;
	  // Dropping the lock before processing casues us to drop frames, which we don't want to do in a Modem.
	  // Dropping the lock after we process serializes all demodulators.
	  // We really need to queue the frames.
	  if (waveData != null)
	    demodulator.handleWave(dataFrame, waveData, dataLength);
	} catch(InterruptedException exp) { }
      }
    }
  }

  // Accept the incoming sound samples
  public void handleWave(int frame, double data[], int length) {
    synchronized(lock) {
      dataFrameIn = frame;
      waveDataIn = data;
      dataLengthIn = length;
      lock.notifyAll();
    }
  }

}
