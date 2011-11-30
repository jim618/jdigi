/*
 * This file is part of znudigi.
 * Released under GNU GENERAL PUBLIC LICENSE Version 2
 * See file COPYING.
 * Copyright (C) 2007-2008 Leigh L. Klotz, Jr. <Leigh@WA5ZNU.org>
 */

package org.wa5znu.znuradio.dsp;

public class Mixer {
  UnitPhasor phasor;

  public Mixer(int samplerate) {
    phasor = new UnitPhasor(samplerate);
  }

  public void setFrequency(double f) {
    phasor.setFrequency(f);
  }

  public double getFrequency() {
    return phasor.getFrequency();
  }

  public Complex[] mixIQ(double data[], int length) {
    Complex[] result = new Complex[length];
    for(int n = 0; n < length; n++) {
      result[n] = phasor.scale(data[n]);
    }
    return result;
  }
}
