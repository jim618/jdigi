/*
 * This file is part of znudigi.
 * Released under GNU GENERAL PUBLIC LICENSE Version 2
 * See file COPYING.
 * Copyright (C) 2007-2008 Leigh L. Klotz, Jr. <Leigh@WA5ZNU.org>
 */

package org.wa5znu.znuradio.dsp;

abstract public class FilterDesign {
  int order;
  int taps;
  double a[];

  public int getOrder() {
    return order;
  }

  public int getTaps() {
    return taps;
  }

  public double[] getA() {
    return a;
  }

  public void setOrder(int order) {
    this.order=order;
    taps = (order % 2 == 0) ? order + 1 : order;
    if(a == null || a.length != taps) {
      a = new double[taps];
    }
  }

}
