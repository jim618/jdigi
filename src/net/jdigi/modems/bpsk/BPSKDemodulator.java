package net.jdigi.modems.bpsk;

import net.jdigi.dsp.Complex;
import net.jdigi.dsp.IQFIRFilter;
import net.jdigi.dsp.LowPassFilterDesign;
import net.jdigi.dsp.Mixer;
import net.jdigi.receiver.Controller;

public class BPSKDemodulator {
	// length of symbol in samples
	// 256 samples at 8Khz yields 31.25 Hz
	// It ought to be calculated the other way around.

	// int symbolLength = 256; // BPSK31
	// int symbolLength = 128; // BPSK63
	// int symbolLength = 64; // BPSK125
	// int symbolLength = 32; // BPSK250
	private int symbolLength = 16; // BPSK500

	private IQFIRFilter filter1;
	private IQFIRFilter filter2;
	private Mixer mixer;

	/**
	 * clock within symbol time window indicating where data is to be sampled
	 */
	private double bitClock;
	private int bits = 0;
	private int decodeShiftRegister = 0;
	private int symbolShiftRegister = 0;

	/**
	 * whether to decode the symbols into output characters
	 */
	private boolean decode = false;

	private double synchronisationBuffer[] = new double[16];
	private Complex previousSymbol;
	private double phase = 0;
	private Complex quality;
	private double metric = 0.0;

	/**
	 * threshold for squelch
	 */
	private double squelch = 10.0;

	/**
	 * is squelch on ?
	 */
	private boolean squelchOn = true;

	private StringBuffer demodulatedTextStringBuffer;

	Controller controller = null;

	public BPSKDemodulator(int sampleRate, Controller controller) {
		this.controller = controller;

		mixer = new Mixer(sampleRate);
		// low-pass filter fc=1/symbolLen order=64 (sin(x)/x with blackman
		// window)
		// decimation by symbolLen/16
		{
			filter1 = new IQFIRFilter(symbolLength / 16);
			filter1.implement(new LowPassFilterDesign(64, 1.0 / symbolLength));
		}
		// low-pass filter fc=1/16 order=64 (sin(x)/x with blackman window)
		// no decimation.
		{
			filter2 = new IQFIRFilter(1);
			filter2.implement(new LowPassFilterDesign(64, 1.0 / 16.0));
		}

		bitClock = 0.0;
		previousSymbol = new Complex(1.0, 0.0);
		quality = new Complex(0.0, 0.0);
		decodeShiftRegister = 0;
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
		demodulatedTextStringBuffer = new StringBuffer();

		// mix wavedata with phasor rotating at set frequency
		Complex result[] = mixer.mixIQ(waveData, length);

		// filter the wave data
		Complex result1[] = filter1.filter(result, length);
		int nunberOfSymbols = length / (symbolLength / 16);
		Complex result2[] = filter2.filter(result1, nunberOfSymbols);

		// work out position of bit in window
		for (int sample = 0; sample < nunberOfSymbols; sample++) {
			Complex z = result2[sample];
			double zmag = z.abs();
			double sum = 0.0;
			double ampsum = 0.0;
			int idx = (int) bitClock;

			// decaying average - insert current sample magnitude
			synchronisationBuffer[idx] = 0.8 * synchronisationBuffer[idx] + 0.2 * zmag;

			// added correction as per PocketDigi
			// vastly improved performance with synchronous interference !!
			for (int i = 0; i < 8; i++) {
				sum += (synchronisationBuffer[i] - synchronisationBuffer[i + 8]);
				ampsum += (synchronisationBuffer[i] + synchronisationBuffer[i + 8]);
			}
			sum = (ampsum == 0.0 ? 0.0 : (sum / ampsum));
			bitClock -= (sum / 5.0);

			// time minor ticks
			bitClock += 1;

			// ensure bitClock is between 0 and 16
			if (bitClock < 0) {
				bitClock += 16.0;
			}
			if (bitClock >= 16.0) {
				bitClock -= 16.0;

				// process symbol on each major time tick
				receiveSymbol(z);
			}
		}

		String demodulatedText = demodulatedTextStringBuffer.toString();
		controller.handleText(frame, demodulatedText);
	}

	private void receiveSymbol(Complex symbol) {
		// The following is a clever way of calculating
		// prevsymbol.phase()-symbol.phase().
		// It's not clear it's any better than just doing
		// prevPhase-symbol.phase() though.
		phase = (previousSymbol.conjugateTimes(symbol)).phase();
		previousSymbol = symbol;

		// ensure phase is between 0 and 2 pi radians
		if (phase < 0) {
			phase += 2 * Math.PI;
		}

		// work out if phase inversion has occurred
		bits = (((int) (phase / Math.PI + 0.5)) & 1) << 1;

		// simple low pass filter for quality of signal
		quality = new Complex(0.02 * Math.cos(2 * phase) + 0.98 * quality.Re(), 0.02 * Math.sin(2 * phase) + 0.98 * quality.Im());
		metric = 100.0 * quality.norm();

		decodeShiftRegister = (decodeShiftRegister << 2) | bits;

		switch (decodeShiftRegister) {
		case 0xAAAAAAAA: /* decode is on for preamble - 16 inversions */
			decode = true;
			quality = new Complex(1.0, 0.0);
			break;

		case 0: /* decode is off for postamble */
			decode = false;
			quality = new Complex(0.0, 0.0);
			break;

		default:
			decode = (!squelchOn || metric > squelch);
		}

		if (decode) {
			receiveBit(bits == 0 ? 1 : 0);
		}
	}

	/**
	 * receive a single bit no change of phase is indicated a '1' data bit
	 * change of phase is indicated by a '0' data
	 * 
	 * @param bit
	 */
	private void receiveBit(int bit) {
		symbolShiftRegister = (symbolShiftRegister << 1) | bit;

		// check to see if last two bits indicate an end of symbol space has
		// been received
		if ((symbolShiftRegister & 3) == 0) {
			// work out the received character
			int receivedCharacter = PSKVaricode.psk_varicode_decode(symbolShiftRegister >> 2);
			if (receivedCharacter != -1) {
				putReceivedCharacter(receivedCharacter);
			}
			symbolShiftRegister = 0;
		}
	}

	/**
	 * receive a single character
	 * 
	 * @param receivedCharacter
	 */
	private void putReceivedCharacter(int receivedCharacter) {
		// Handle backspace and delete by removing last char if possible
		// If too late, pass it through and let the next level handle it.
		if (receivedCharacter == 8 || receivedCharacter == 127) {
			int len = demodulatedTextStringBuffer.length();
			if (len > 0) {
				demodulatedTextStringBuffer.setLength(len - 1);
			} else {
				// too late
				demodulatedTextStringBuffer.append((char) receivedCharacter);
			}
		} else {
			demodulatedTextStringBuffer.append((char) receivedCharacter);
		}
	}
}
