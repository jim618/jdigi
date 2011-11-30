/*
 * This file is part of znudigi.
 * Released under GNU GENERAL PUBLIC LICENSE Version 2
 * See file COPYING.
 * Copyright (C) 2007-2008 Leigh L. Klotz, Jr. <Leigh@WA5ZNU.org>
 */

package org.wa5znu.znuradio.dsp;

abstract public class BaseFIRFilter {
  int decimation;
  int taps;
  int order;
  double[] a;

  public BaseFIRFilter(int decimation) {
    this.decimation=decimation;
  }

  public void implement(FilterDesign design) {
    order=design.getOrder();
    taps = design.getTaps();
    a = design.getA();
  }
}
