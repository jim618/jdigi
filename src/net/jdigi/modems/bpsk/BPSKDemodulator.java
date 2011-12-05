package net.jdigi.modems.bpsk;

import net.jdigi.dsp.Complex;
import net.jdigi.dsp.IQFIRFilter;
import net.jdigi.dsp.LowPassFilterDesign;
import net.jdigi.dsp.Mixer;
import net.jdigi.receiver.Controller;

public class BPSKDemodulator {
	// 256 samples at 8Khz yields 31.25 Hz
	// It ought to be calculated the other way around.
//	int symbolLen = 256;  // BPSK31
//	int symbolLen = 128;  // BPSK63
	int symbolLen = 64;   // BPSK125
	//int symbolLen = 32;   // BPSK250
	int sampleRate;
	double bandwidth;
	IQFIRFilter filter1;
	IQFIRFilter filter2;
	Mixer mixer;
	double bitclk;
	int bits = 0;
	int dcdshreg = 0;
	int shreg = 0;
	boolean dcd = false;
	double syncbuf[] = new double[16];
	double phaseacc = 0;
	Complex prevsymbol;
	double phase = 0;
	Complex quality;
	double metric = 0.0;
	double squelch = 10.0;
	boolean squelchon = true;
	StringBuffer sb;
	boolean afcon = true;
	int sigSearchCount = 0;
	final static int SEARCH_RANGE = 200;
	final static double SN_THRESHOLD = 2.0;
	final static int AFCDECAY = 8;

	double freqerr = 0.0;
	Controller controller = null;

	public BPSKDemodulator(int sampleRate, Controller controller) {
		this.controller = controller;
		this.sampleRate = sampleRate;
		bandwidth = sampleRate / (double) symbolLen;
		mixer = new Mixer(sampleRate);
		// low-pass filter fc=1/symbolLen order=64 (sin(x)/x with blackman
		// window)
		// decimation by symbolLen/16
		{
			filter1 = new IQFIRFilter(symbolLen / 16);
			filter1.implement(new LowPassFilterDesign(64, 1.0 / symbolLen));
		}
		// low-pass filter fc=1/16 order=64 (sin(x)/x with blackman window)
		// no decimation.
		{
			filter2 = new IQFIRFilter(1);
			filter2.implement(new LowPassFilterDesign(64, 1.0 / 16.0));
		}
		bitclk = 0.0;
		prevsymbol = new Complex(1.0, 0.0);
		quality = new Complex(0.0, 0.0);
		phaseacc = 0;
		dcdshreg = 0;
		phase = 0;
	}

	public void setFrequency(double f) {
		mixer.setFrequency(f);
	}

	public double getFrequency() {
		return mixer.getFrequency();
	}

	public void handleWave(int frame, double waveData[], int length) {
		// System.out.println("BPSKDemodulator/handleWave called for frame = " +
		// frame);
		sb = new StringBuffer();
		Complex result[] = mixer.mixIQ(waveData, length);

		Complex result1[] = filter1.filter(result, length);
		int nsyms = length / (symbolLen / 16);
		Complex result2[] = filter2.filter(result1, nsyms);
		for (int samp = 0; samp < nsyms; samp++) {
			Complex z = result2[samp];
			double zmag = z.abs();
			double sum = 0.0;
			double ampsum = 0.0;
			int idx = (int) bitclk;

			syncbuf[idx] = 0.8 * syncbuf[idx] + 0.2 * zmag;
			for (int i = 0; i < 8; i++) {
				sum += (syncbuf[i] - syncbuf[i + 8]);
				ampsum += (syncbuf[i] + syncbuf[i + 8]);
			}
			// added correction as per PocketDigi
			// vastly improved performance with synchronous interference !!
			sum = (ampsum == 0.0 ? 0.0 : (sum / ampsum));

			bitclk -= (sum / 5.0);
			bitclk += 1;
			if (bitclk < 0)
				bitclk += 16.0;
			if (bitclk >= 16.0) {
				bitclk -= 16.0;
				rx_symbol(z);
				afc();
			}
		}

		if (sigSearchCount > 0)
			findsignal();

		String demodulatedText = sb.toString();
		controller.handleText(frame, demodulatedText);
		// System.out.println("BPSKDemodulator/handleWave - I saw text : " +
		// demodulatedText);
	}

	private void rx_symbol(Complex symbol) {
		// The following is a clever way of calculating
		// prevsymbol.phase()-symbol.phase().
		// It's not clear it's any better than just doing
		// prevPhase-symbol.phase() though.
		phase = (prevsymbol.conjugateTimes(symbol)).phase();
		prevsymbol = symbol;

		if (phase < 0)
			phase += 2 * Math.PI;
		bits = (((int) (phase / Math.PI + 0.5)) & 1) << 1;

		// simple low pass filter for quality of signal
		quality = new Complex(0.02 * Math.cos(2 * phase) + 0.98 * quality.Re(), 0.02 * Math.sin(2 * phase) + 0.98 * quality.Im());
		metric = 100.0 * quality.norm();

		dcdshreg = (dcdshreg << 2) | bits;

		switch (dcdshreg) {
		case 0xAAAAAAAA: /* DCD on by preamble */
			dcd = true;
			quality = new Complex(1.0, 0.0);
			break;

		case 0: /* DCD off by postamble */
			dcd = false;
			quality = new Complex(0.0, 0.0);
			break;

		default:
			dcd = (!squelchon || metric > squelch);
		}

		if (dcd) {
			rx_bit(bits == 0 ? 1 : 0);
		}

	}

	private void rx_bit(int bit) {
		shreg = (shreg << 1) | bit;
		if ((shreg & 3) == 0) {
			int c = PSKVaricode.psk_varicode_decode(shreg >> 2);
			if (c != -1) {
				put_rx_char(c);
			}
			shreg = 0;
		}
	}

	private void put_rx_char(int c) {
		// Handle backspace and delete by removing last char if possible
		// If too late, pass it through and let the next level handle it.
		if (c == 8 || c == 127) {
			int len = sb.length();
			if (len > 0) {
				sb.setLength(len - 1);
			} else {
				sb.append((char) c); // too late
			}
		} else {
			sb.append((char) c);
		}
	}

	private void afc() {
		if (!afcon)
			return;
		if (sigSearchCount > 0)
			findsignal();
		else if (dcd)
			phaseafc();
	}

	private void findsignal() {
	}

	private void phaseafc() {
		double error = (phase - bits * Math.PI / 2.0);
		if (error < Math.PI / 2.0)
			error += 2.0 * Math.PI;
		if (error > Math.PI / 2)
			error -= 2.0 * Math.PI;
		double scale = ((sampleRate / (symbolLen * 2.0 * Math.PI) / 16.0));
		error *= scale;
		if (Math.abs(error) < bandwidth) {
			freqerr = decayavg(freqerr, error, AFCDECAY);
			// controller.setFrequency(getFrequency() - freqerr);
		}
	}

	private double decayavg(double average, double input, double weight) {
		return input * (1.0 / weight) + average * (1.0 - (1.0 / weight));
	}
}
