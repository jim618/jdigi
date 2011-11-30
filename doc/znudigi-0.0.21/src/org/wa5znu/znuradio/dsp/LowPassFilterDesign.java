/*
 * This file is part of znudigi.
 * Released under GNU GENERAL PUBLIC LICENSE Version 2
 * See file COPYING.
 * Copyright (C) 2007-2008 Leigh L. Klotz, Jr. <Leigh@WA5ZNU.org>
 */

package org.wa5znu.znuradio.dsp;

public class LowPassFilterDesign extends FilterDesign {
  public LowPassFilterDesign(int order, double fc) {
    setOrder(order);
    designLowPass(fc);
  }

  /**
   * Design a lowpass Blackman-windowed sinc filter of m taps with specified cutoff frequency..
   * m is rounded up to odd if necessary.
   * @see http://www.dspguide.com/ch16/2.htm "Windowed-Sinc Filters" by Steven W. Smith, Ph.D.
   */
  public void designLowPass(double fc) {
    double normalize = 0;
    int m2 = (taps - 1) / 2;

    // sin(x-tau)/(x-tau)	
    a[0] = 0.0;
    for (int i = 1; i < taps; i++) {
      int j = (i-m2);
      if (j != 0)
	a[i] = Math.sin(2 * Math.PI * fc * j) / j;
      else
	a[i] = 2 * Math.PI * fc;
    }

    // blackman window
    for (int i = 0; i < taps; i++)
      a[i] = a[i] * (0.42 - 0.5 * Math.cos(2*Math.PI*i/taps) + 0.08 * Math.cos(4*Math.PI*i/taps));

    // calculation normalization factor
    for (int i = 0; i < taps; i++)
      normalize += a[i];
    normalize = Math.abs(normalize);

    // normalize the filter
    for (int i = 0; i < taps; i++)
      a[i] /= normalize;
  }

}
