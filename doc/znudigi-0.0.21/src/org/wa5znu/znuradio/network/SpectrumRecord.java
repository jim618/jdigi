/*
 * This file is part of znudigi.
 * Released under GNU GENERAL PUBLIC LICENSE Version 2
 * See file COPYING.
 * Copyright (C) 2007-2008 Leigh L. Klotz, Jr. <Leigh@WA5ZNU.org>
 */

package org.wa5znu.znuradio.network;
import org.wa5znu.znuradio.receiver.*;

public class SpectrumRecord implements SpectrumHandler, FrequencyHandler, Runnable {
  ClientConnection client;
  byte[] byteDataIn;
  double frequency=-1;
  int sampleRate=-1;
  boolean clientWantsSpectrumNybbles=false;
  private Object lock = new Object();

  public SpectrumRecord(ClientConnection client, boolean clientWantsSpectrumNybbles) {
    super();
    this.client=client;
    this.clientWantsSpectrumNybbles = clientWantsSpectrumNybbles;
  }

  public void handleSpectrum(int frame, double data[], int length)  {
    if (clientWantsSpectrumNybbles) {
      int nyblen = length/2;
      synchronized(lock) {
	if (byteDataIn == null || byteDataIn.length != nyblen) 
	  byteDataIn = new byte[nyblen];
	for (int i = 0; i < nyblen; i++) {
	  double d1 = data[i*2];
	  double d2 = data[i*2+1];
	  int b1 = ((int)(d1*15))*16;
	  int b2 = ((int)(d2*15));
	  byteDataIn[i] = (byte)(b1+b2);
	}
      }
    } else {
      synchronized(lock) {
	if (byteDataIn == null || byteDataIn.length != length) 
	  byteDataIn = new byte[length];
	for (int i = 0; i < length; i++) {
	  double d = data[i];
	  byteDataIn[i] = (byte)(d * 255);
	}
      }
    }
  }



  public void run() {
    byte[] byteData=null;
    synchronized(lock) {
      if (byteDataIn != null) {
	byteData = byteDataIn;
      }
    }
    if (byteData != null) {
      synchronized(client.getWriteLock()) {
	if (clientWantsSpectrumNybbles) {
	  client.writeCommand('s');
	  client.writeClient(byteData);
	  client.flushClient();
	} else {
	  client.writeCommand('S');
	  client.writeClient(byteData);
	  client.flushClient();
	}
      }
    } else {
      synchronized(this) {
	if (frequency>0) {
	  double hertzPerTick = (4000.0 / 1024.0); // crock
	  int ticks = (int)(frequency / hertzPerTick);
	  synchronized(client.getWriteLock()) {
	    client.writeCommand('F');
	    client.writeShort((short)ticks);
	  }
	} else if (sampleRate > 0) {
	  // ok
	}
      }
    }
  }

  public void setFrequency(double f) {
    setFrequency(f, false);
  }

  public void setFrequency(double f, boolean userClick) {
    frequency=f;
  }

  public void setSampleRate(int r) {
    sampleRate=r;
  }

  public void showNextStage() {
  }

}
