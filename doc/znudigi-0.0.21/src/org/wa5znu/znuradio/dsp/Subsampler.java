/*
 * This file is part of znudigi.
 * Released under GNU GENERAL PUBLIC LICENSE Version 2
 * See file COPYING.
 * Copyright (C) 2007-2008 Leigh L. Klotz, Jr. <Leigh@WA5ZNU.org>
 */

package org.wa5znu.znuradio.dsp;

public class Subsampler extends RealFIRFilter {
  public Subsampler(int factor) {
    super(factor);
    implement(new LowPassFilterDesign(32, 1.0 / (double)factor));
  }

  public double[] subsample(double data[], int length) {
    return filter(data, length);
  }
}
