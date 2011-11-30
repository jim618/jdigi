/*
 * This file is part of znudigi.
 * Released under GNU GENERAL PUBLIC LICENSE Version 2
 * See file COPYING.
 * Copyright (C) 2007-2008 Leigh L. Klotz, Jr. <Leigh@WA5ZNU.org>
 */

package org.wa5znu.znuradio.dsp;

public class IQFIRFilter extends BaseFIRFilter {

  double xI[];
  double xQ[];
  int counter=0;

  public IQFIRFilter(int decimation) {
    super(decimation);
  }

  public void implement(FilterDesign design) {
    super.implement(design);
    if (xI == null || xI.length != taps) {
      xI = new double[taps];
      xQ = new double[taps];
      for(int k = 0; k < taps; k++) {
	xI[k] = 0.0;
	xQ[k] = 0.0;
      }
    }
  }

  public Complex[] filter(Complex io[], int nSamples) {
    int j = 0;
    int taps = a.length;
    for (int i = 0; i < nSamples; i++) {
      xI[0] = io[i].Re();
      xQ[0] = io[i].Im();
      if (++counter == decimation) {
	counter = 0;
	double yI = 0.0;
	double yQ = 0.0;
	for (int k = 0; k < taps; k++) {
	  yI += xI[k]*(a[k]);
	  yQ += xQ[k]*(a[k]);
	}
	io[j++] = new Complex(yI, yQ);
      }
      for(int k = taps - 1; k > 0; k--) {
	xI[k] = xI[k - 1];
	xQ[k] = xQ[k - 1];
      }
    }
    return io;
  }

}
