/*************************************************************************
 *  Copyright (C) 2007 Robert Sedgewick  and Kevin Wayne. All rights reserved.
 *  Used by permission of Kevin Wayne December 16, 2007
 *  under dual license GPL and Berkeley.
 *************************************************************************/
package org.wa5znu.znuradio.dsp;


public final class Complex {
    private final double re;
    private final double im;

    public double Re() {
        return re;
    }

    public double Im() {
        return im;
    }

    public Complex(double real, double imag) {
        re = real;
        im = imag;
    }

    public String toString() {
        if(im == 0.0)
	    return Double.toString(re);
        if(re == 0.0)
            return im + "i";
        if(im < 0.0)
            return re + " - " + (-im) + "j";
        else
            return re + " + " + im + "j";
    }

    public double norm() {
	return (re * re + im * im);
    }

    public double abs() {
        return Math.hypot(re, im);
    }

    public double phase() {
        return Math.atan2(im, re);
    }

    public Complex plus(Complex b) {
        double real = re + b.re;
        double imag = im + b.im;
        return new Complex(real, imag);
    }

    public Complex minus(Complex b) {
        double real = re - b.re;
        double imag = im - b.im;
        return new Complex(real, imag);
    }

    public Complex times(Complex b) {
        double bre = b.re;
        double bim = b.im;
        double real = re * bre - im * bim;
        double imag = re * bim + im * bre;
        return new Complex(real, imag);
    }

    public Complex times(double alpha) {
        return new Complex(alpha * re, alpha * im);
    }

    public Complex conjugate() {
        return new Complex(re, -im);
    }


    // Z = (complex conjugate of X) * Y
    // Z1 = x1 + jy1, or Z1 = |Z1|exp(jP1)
    // Z2 = x2 + jy2, or Z2 = |Z2|exp(jP2)
    // Z = (x1 - jy1) * (x2 + jy2)
    // or Z = |Z1|*|Z2| exp (j (P2 - P1))
    // So, |Z| = |X| * |y|  and  arg(z) = arg(y) - arg(x)
    public Complex conjugateTimes (Complex y) {
	if (true) {
	    double real = re * y.re + im * y.im;
	    double imag = re * y.im - im * y.re;
	    return new Complex(real, imag);
	} else {
	    return conjugate().times(y);
	}
    }

    public Complex reciprocal() {
        double n = norm();
        return new Complex(re / n, -im / n);
    }

    public Complex divide(Complex b) {
        return times(b.reciprocal());
    }

}
