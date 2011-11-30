/*
 * This file is part of znudigi.
 * Released under GNU GENERAL PUBLIC LICENSE Version 2
 * See file COPYING.
 * Copyright (C) 2007-2008 Leigh L. Klotz, Jr. <Leigh@WA5ZNU.org>
 */

package org.wa5znu.znuradio.receiver;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.wa5znu.znuradio.audio.AudioInputThread;
import org.wa5znu.znuradio.dsp.Complex;
import org.wa5znu.znuradio.modems.bpsk.BPSKDemodulator;

public class Receiver implements ReceiverHandler, WaveHandler, Controller {

  private AudioInputThread audioInputThread;
  private Controller controller;
  private ConcurrentLinkedQueue<ModemThread> modemThreads = new ConcurrentLinkedQueue<ModemThread>();
  private SpectrumThread spectrumThread;

  private static final int REQUIRED_SAMPLE_RATE = 8000;
  private static final int sampleRates[] = {
    REQUIRED_SAMPLE_RATE, 48000
  };

  public Receiver(Controller controller) {
    this.controller=controller;
  }

  public int getSampleRate() {
    return REQUIRED_SAMPLE_RATE;
  }

  public void setSampleRate(int rate) {
    controller.setSampleRate(rate);
  }

  public boolean startReceiver(String soundDevice) {
    if(audioInputThread != null) {
      audioInputThread.stopAudio();
    }
    boolean failed = true;
    for (int n = 0; n < sampleRates.length; n++) {
      int sampleRate = sampleRates[n];
      audioInputThread = new AudioInputThread(REQUIRED_SAMPLE_RATE, sampleRate);
      if(audioInputThread.startAudio(soundDevice, this)) {
	failed=false;
	setSampleRate(REQUIRED_SAMPLE_RATE);
	audioInputThread.setPriority(10);
	{
	  if (spectrumThread != null) throw new RuntimeException("SpectrumThread!=null");
	  spectrumThread = new SpectrumThread(this);
	  spectrumThread.setPriority(1);
	  spectrumThread.start();
	}
	break;
      }
    }
    // You must call addDemodulator(x,y) yourself
    return (!failed);
  }

  public Demodulator addDemodulator(ReceiverHandler receiverHandler, Controller controller) {
    Demodulator demodulator = new BPSKDemodulator(getSampleRate(), receiverHandler, controller);
    ModemThread modemThread = new ModemThread(demodulator);
    modemThread.setPriority(10);
    modemThreads.add(modemThread);
    modemThread.start();
    return demodulator;
  }

  public void handleWave(int frame, double data[], int length)  {
    if(length == -1) {
      System.err.println("\n[Missed sample " + frame + "]");
      return;
    }
    for (ModemThread modemThread : modemThreads) {
      modemThread.handleWave(frame, data, length);
    }
    if (spectrumThread != null) {
      spectrumThread.handleWave(frame, data, length);
    }
  }

  public void handleSpectrum(int frame, double data[], int length)  {
    controller.handleSpectrum(frame, data, length);
  }

  public void setFrequency(double f) {
    setFrequency(f, false);
  }

  public void setFrequency(double f, boolean userClick) {
    for (ModemThread modemThread : modemThreads) {
      modemThread.setFrequency(f);
    }
  }

  public void handleStage(int frame, double data[], int length) {
    controller.handleStage(frame, data, length);
  }

  public void handleStage(int frame, Complex data[], int length) {
    controller.handleStage(frame, data, length);
  }

  public void handlePhase(int frame, double phi, boolean dcd) {
    controller.handlePhase(frame, phi, dcd);
  }

  public void handleText(int frame, String text) {
    controller.handleText(frame, text);
  }

  public void showNextStage() {
    for (ModemThread modemThread : modemThreads) {
      modemThread.showNextStage();
    }
  }

}
