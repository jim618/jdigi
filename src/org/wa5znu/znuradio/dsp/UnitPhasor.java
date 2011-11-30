/*
 * This file is part of znudigi.
 * Released under GNU GENERAL PUBLIC LICENSE Version 2
 * See file COPYING.
 * Copyright (C) 2007-2008 Leigh L. Klotz, Jr. <Leigh@WA5ZNU.org>
 */

package org.wa5znu.znuradio.dsp;

public class UnitPhasor {
  private int samplerate;
  private double frequency;
  private double delta;
  private double phase;

  public UnitPhasor(int samplerate) {
    this.samplerate = samplerate;
    phase = 0.0;
  }

  public void setFrequency(double f) {
    if(f <= 0.0) {
      return;
    } else if (f == frequency) {
      return;
    } else {
      frequency = f;
      delta = 2.0 * Math.PI * f / samplerate;
      return;
    }
  }

  public double getFrequency() {
    return frequency;
  }

  public double getPhase() {
    double answer = phase;
    phase += delta;
    if (phase > Math.PI) 
      phase -= 2*Math.PI;
    return answer;
  }

  public Complex scale(double d) {
    double phase = getPhase();
    double I = d * Math.cos(phase);
    double Q = d * Math.sin(phase);
    return new Complex(I, Q);
  }

}
