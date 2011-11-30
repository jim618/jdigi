package net.jdigi.dsp;

abstract public class BaseFIRFilter {
	int decimation;
	int taps;
	int order;
	double[] a;

	public BaseFIRFilter(int decimation) {
		this.decimation = decimation;
	}

	public void implement(FilterDesign design) {
		order = design.getOrder();
		taps = design.getTaps();
		a = design.getA();
	}
}
