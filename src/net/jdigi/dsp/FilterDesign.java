package net.jdigi.dsp;

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
		this.order = order;
		taps = (order % 2 == 0) ? order + 1 : order;
		if (a == null || a.length != taps) {
			a = new double[taps];
		}
	}

}
