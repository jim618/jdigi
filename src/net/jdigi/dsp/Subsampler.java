package net.jdigi.dsp;

public class Subsampler extends RealFIRFilter {
	public Subsampler(int factor) {
		super(factor);
		implement(new LowPassFilterDesign(32, 1.0 / (double) factor));
	}

	public double[] subsample(double data[], int length) {
		return filter(data, length);
	}
}
