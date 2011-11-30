/*
 * This file is part of znudigi.
 * Released under GNU GENERAL PUBLIC LICENSE Version 2
 * See file COPYING.
 * Copyright (C) 2007-2008 Leigh L. Klotz, Jr. <Leigh@WA5ZNU.org>
 * Portions derived from 
 * fldigi Copyright (C) 2006 Dave Freese, W1HKJ
 * gmfsk Copyright (C) 2001, 2002, 2003 Tomi Manninen (oh2bns@sral.fi)
 */


package org.wa5znu.znuradio.dsp;

// From fldigi by Dave Freese W1HKJ
public abstract class Window {

  // Window functions
  private static double rect(double x)
  {
    return 1.0;
  }

  private static double blackman(double x)
  {
    return (0.42 - 0.50 * Math.cos(2 * Math.PI * x) + 0.08 * Math.cos(4 * Math.PI * x));
  }

  private static double hamming(double x)
  {
    return 0.54 - 0.46 * Math.cos(2 * Math.PI * x);
  }

  private static double hanning(double x)
  {
    return 0.5 - 0.5 * Math.cos(2 * Math.PI * x);
  }

  // Rectangular - no pre filtering of data array
  public static void rectWindow(double array[], int n) {
    for (int i = 0; i < n; i++)
      array[i] = 1.0;
  }
    
  // Hamming - used by gmfsk
  public static void hammingWindow(double array[]) {
    int n = array.length;
    double pwr = 0.0;
    for (int i = 0; i < n; i++) {
      array[i] = hamming((double)i/(double)n);
      pwr += array[i] * array[i];
    }
    pwr = Math.sqrt((double)n/pwr);
    for (int i = 0; i < n; i++)
      array[i] *= pwr;
  }
    
  // Hanning - used by winpsk
  public static void hanningWindow(double array[]) {
    int n = array.length;
    double pwr = 0.0;
    for (int i = 0; i < n; i++) {
      array[i] = hanning((double)i/(double)n);
      pwr += array[i] * array[i];
    }
    pwr = Math.sqrt((double)n/pwr);
    for (int i = 0; i < n; i++)
      array[i] *= pwr;
  }

  // Best lobe suppression - least in band ripple
  public static void blackmanWindow(double array[]) {
    int n = array.length;
    double pwr = 0.0;
    for (int i = 0; i < n; i++) {
      array[i] = blackman((double)i/(double)n);
      pwr += array[i] * array[i];
    }
    pwr = Math.sqrt((double)n/pwr);
    for (int i = 0; i < n; i++)
      array[i] *= pwr;
  }

  // Simple about effective as Hamming or Hanning
  public static void triangularWindow(double array[]) {
    int n = array.length;
    double pwr = 0.0;
    for (int i = 0; i < n; i++) array[i] = 1.0;
    for (int i = 0; i < n / 4; i++) {
      array[i] = 4.0 * (double)i / (double)n;
      array[n-i] = array[i];
    }
    for (int i = 0; i < n; i++)
      pwr += array[i] * array[i];
    pwr = Math.sqrt((double)n/pwr);
    for (int i = 0; i < n; i++)
      array[i] *= pwr;
  }
}
