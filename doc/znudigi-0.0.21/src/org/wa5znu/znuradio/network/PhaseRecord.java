/*
 * This file is part of znudigi.
 * Released under GNU GENERAL PUBLIC LICENSE Version 2
 * See file COPYING.
 * Copyright (C) 2007-2008 Leigh L. Klotz, Jr. <Leigh@WA5ZNU.org>
 */

package org.wa5znu.znuradio.network;

import org.wa5znu.znuradio.receiver.PhaseHandler;

public class PhaseRecord implements PhaseHandler, Runnable {
  public final static int MAX_PHASES = 12;
  public final static int MIN_PHASES = 6;
  ClientConnection client;
  int[] phases = new int[MAX_PHASES];
  boolean[] dcds = new boolean[MAX_PHASES];
  int p = 0;

  public PhaseRecord(ClientConnection client)  {
    this.client=client;
  }

  public synchronized void handlePhase(int frame, double phase, boolean dcd) {
    if (p == phases.length) {
      System.out.println("[Overflew phase]");
      p=0;	// don't wrap, just give up on old data.
    }
    phases[p]=(int)((((phase) / (2 * Math.PI))*256)+0.5);
    dcds[p]=dcd;
    p++;
  }

  public synchronized void run() {
    if (p<MIN_PHASES) return;
    synchronized(client.getWriteLock()) {
      for (int i = 0; i < p; i++) {
	client.writeCommand(dcds[i] ? 'P' : 'p');
	client.writeClient((byte)phases[i]);
      }
      p=0;
    }
  }
}
