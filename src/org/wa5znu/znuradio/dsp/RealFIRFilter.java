/*
 * This file is part of znudigi.
 * Released under GNU GENERAL PUBLIC LICENSE Version 2
 * See file COPYING.
 * Copyright (C) 2007-2008 Leigh L. Klotz, Jr. <Leigh@WA5ZNU.org>
 */

package org.wa5znu.znuradio.dsp;

public class RealFIRFilter extends BaseFIRFilter {

  double x[];
  int counter = 0;

  public RealFIRFilter(int decimation) {
    super(decimation);
  }

  public void implement(FilterDesign design) {
    super.implement(design);
    if (x == null || x.length != taps) {
      x = new double[taps];
      for(int k = 0; k < taps; k++) {
	x[k] = 0.0;
      }
    }
  }

  public double[] filter(double io[], int nSamples) {
    int j = 0;
    int taps = a.length;
    for (int i = 0; i < nSamples; i++) {
      x[0] = io[i];
      if (++counter == decimation) {
	counter = 0;
	double y = 0.0;
	for (int k = 0; k < taps; k++) {
	  y += x[k] * a[k];
	}
	io[j++] = y;
      }
      for(int k = taps - 1; k > 0; k--) {
	x[k] = x[k - 1];
      }
    }
    return io;
  }

}
