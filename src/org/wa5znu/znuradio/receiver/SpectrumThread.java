/*
 * This file is part of znudigi.
 * Released under GNU GENERAL PUBLIC LICENSE Version 2
 * See file COPYING.
 * Copyright (C) 2007-2008 Leigh L. Klotz, Jr. <Leigh@WA5ZNU.org>
 */

package org.wa5znu.znuradio.receiver;

import org.wa5znu.znuradio.dsp.Complex;
import org.wa5znu.znuradio.dsp.FFT;
import org.wa5znu.znuradio.dsp.Window;

public class SpectrumThread extends Thread
    implements WaveHandler
{

  double waveDataIn[]=null;
  int dataLengthIn=0;
  int dataFrameIn=0;
  double window[];
  SpectrumHandler spectrumHandler;
  private final Object lock = new Object();
  volatile boolean busy=false;
  double alcMax = 3.0;
  double alcMin = -2.0;

  public SpectrumThread(SpectrumHandler spectrumHandler) {
    this.spectrumHandler = spectrumHandler;
  }

  public void run() {
    Complex x[]=null;
    double powerSpectrum[]=null;
    while(true) {
      int dataLength=0;
      int dataFrame=0;
      double waveData[]=null;
      synchronized(lock) {
	try {
	  lock.wait();
	  dataLength=dataLengthIn;
	  dataFrame=dataFrameIn;
	  waveData=waveDataIn;
	} catch(InterruptedException e) { }
      }

      if (waveData != null) {
	busy = true;
	// Set the window function and convolve the real signal with it
	// and produce a Complex signal with a zero imaginary component.
	if (window == null || window.length != dataLength) {
	  window = new double[dataLength];
	  Window.hammingWindow(window);
	}

	if (x==null || x.length != dataLength) {
	  x = new Complex[dataLength];
	}

	for(int n = 0; n < dataLength; n++) {
	  x[n] = new Complex(waveData[n] * window[n], 0.0);
	}

	// Perform the Complex FFT
	Complex X[] = FFT.fft(x);

	int outputLength = dataLength/2;

	// Calculate the log power spectrum of the Complex FFT
	if (powerSpectrum == null || powerSpectrum.length != outputLength) 
	  powerSpectrum = new double[outputLength];

	for(int n = 0; n < outputLength; n++) {
	  double d = X[n].norm();
	  powerSpectrum[n] = (d == 0.0) ? alcMin : Math.log10(d)/2.0;
	}

	double max = alcMax;
	double min = alcMin;

	// Normalize spectrum to max-min and truncate to [0,1]
	max -= min;
	for(int n = 0; n < outputLength; n++) {
	  powerSpectrum[n] -= min;
	  powerSpectrum[n] /= max;
	  if (powerSpectrum[n]<0) powerSpectrum[n]=0;
	  else if (powerSpectrum[n]>1) powerSpectrum[n]=1;
	}

	// Dispatch spectrum data
	spectrumHandler.handleSpectrum(dataFrame, powerSpectrum, outputLength);
	busy=false;
      }
    }
  }

  // Accept the incoming data
  public void handleWave(int frame, double data[], int length) {
    // Just drop samples of the wave data if it's not ready.
    if (busy) {
      System.out.println("[Skipping wave frame " +frame + " Spectrum]");
    } else {
      synchronized(lock) {
	waveDataIn = data;
	dataLengthIn = length;
	dataFrameIn = frame;
	lock.notifyAll();
      }
    }
  }
}
