package net.jdigi.modems.bpsk;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * modulator for BPSK31
 * 
 * Read: http://87.194.135.226/ivarc/articles.php?article_id=48
 * 
 * @author jim
 * 
 */
public class BPSKModulator {
	private static final int NUMBER_OF_CHANNELS = 1;
	private static final int BITS_PER_SAMPLE = 16;
	private static final int SAMPLE_RATE = 8000;
	private static final int BYTES_PER_SAMPLE = 2;

	// frequency of symbol output
	//private static final double //BASE_FREQUENCY = 31.25; // Hz - BPSK31
	//private static final double BASE_FREQUENCY = 62.5; // Hz - BPSK63
	//private static final double BASE_FREQUENCY = 125; // Hz - BPSK125
	private static final double BASE_FREQUENCY = 250; // Hz - BPSK250

	// carrier wave frequency
	private static final double DEFAULT_CARRIER_WAVE_FREQUENCY = 1000; // Hz
    private double carrierWaveFrequency;
	
	private static final int SAMPLES_PER_SYMBOL = (int) (SAMPLE_RATE / BASE_FREQUENCY);

	// mean amplitude output
	private static final double BASE_AMPLITUDE = 16383; // 8191;

	// suffix indicating end of tx
	private static final String CHARACTER_GAP = "00";

	private static final char ZERO_SYMBOL = '0'; // phase reversal
	private static final char ONE_SYMBOL = '1'; // no phase reversal

	private char previousSymbol;
	private char currentSymbol;
	private char nextSymbol;

	// if true output phase zero, if false output phase 180
	private boolean outputPhaseZero;

	private static final int NUMBER_OF_REVERSALS_IN_PREAMBLE = 96;
	private static final int NUMBER_OF_REVERSALS_IN_POSTAMBLE = 32;

	public BPSKModulator() {
		carrierWaveFrequency = DEFAULT_CARRIER_WAVE_FREQUENCY;
		initialise();
	}

	public void initialise() {
		// initially output phase zero
		outputPhaseZero = true;

		// previous is zero so that amplitude is modulated up at beginning of
		// overall tx
		previousSymbol = ZERO_SYMBOL;
		currentSymbol = ZERO_SYMBOL;
		nextSymbol = ZERO_SYMBOL;
	}

	public void convertStringToSymbols(String stringToSend) throws LineUnavailableException, IOException {
		StringBuffer symbols = new StringBuffer();

		// add preamble - one is deducted because of the pre/current/ post
		// structure
		for (int i = 0; i < NUMBER_OF_REVERSALS_IN_PREAMBLE - 1; i++) {
			symbols.append(ZERO_SYMBOL);
		}

		// convert the string into PSK31 symbols (1 and 0 but as Strings)
		if (stringToSend != null) {
			for (int i = 0; i < stringToSend.length(); i++) {
				symbols.append(PSKVaricode.psk_varicode_encode(stringToSend.charAt(i)));
				symbols.append(CHARACTER_GAP);
			}
		}

		// add postamble
		for (int i = 0; i < NUMBER_OF_REVERSALS_IN_POSTAMBLE; i++) {
			symbols.append(ZERO_SYMBOL);
		}

		System.out.println("BPSKModulator#convertStringToSymbols - input string '" + stringToSend + "' is '" + symbols.toString()
				+ "'");
		byte[] buffer = new byte[symbols.toString().length() * SAMPLES_PER_SYMBOL * BYTES_PER_SAMPLE];
		ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
		ShortBuffer shortBuffer = byteBuffer.asShortBuffer();

		initialise();

		for (int i = 0; i < symbols.toString().length(); i++) {
			previousSymbol = currentSymbol == ZERO_SYMBOL ? ZERO_SYMBOL : ONE_SYMBOL;
			currentSymbol = nextSymbol == ZERO_SYMBOL ? ZERO_SYMBOL : ONE_SYMBOL;
			nextSymbol = symbols.toString().charAt(i);

			modulateSymbol(shortBuffer, previousSymbol, currentSymbol, nextSymbol);
		}

		playByteArray(buffer);
	}

	/**
	 * output a single symbol when symbolToModulate is ZERO, phase is shifted
	 * 180 degrees when symbolToModulate is ONE, phase is not shifted
	 * 
	 * @param shortBuffer
	 * @param currentSymbol
	 *            , boolean nextSymbol
	 */
	private void modulateSymbol(ShortBuffer shortBuffer, char previousSymbol, char currentSymbol, char nextSymbol) {
		// if this symbol is a one then there is no reversal and hence no cosine
		// shaping of the phase change
		if (currentSymbol == ONE_SYMBOL) {
			// do not phase invert
			// do not change outputPhaseZero;
		} else {
			outputPhaseZero = !outputPhaseZero;
		}

		boolean rampUp = false;
		boolean rampDown = false;
		
		if (currentSymbol == ZERO_SYMBOL) {
			// current symbol is a phase invert so need to ramp up
			rampUp = true;
		}
		
		if (nextSymbol == ZERO_SYMBOL) {
			// next symbol is a phase invert so need to ramp down
			rampDown = true;
		}
		outputSymbol(shortBuffer, outputPhaseZero, rampUp, rampDown);
	}

	void outputSymbol(ShortBuffer shortBuffer, boolean outputPhaseZero, boolean rampUp, boolean rampDown) {
		double primaryScaleFactor =  2 * Math.PI * carrierWaveFrequency / BASE_FREQUENCY / (double) SAMPLES_PER_SYMBOL;
		for (int sampleCount = 0; sampleCount < SAMPLES_PER_SYMBOL; sampleCount++) {
			// time is 0.00 at beginning of symbol output,TIME_PER_SYMBOL at end of symbol output

			// work out value of output due to carrier wave
			double time = (double) sampleCount / (double) SAMPLES_PER_SYMBOL;
			double carrierFrequencySampleValue = Math.sin(sampleCount * primaryScaleFactor);
			if (!outputPhaseZero) {
				carrierFrequencySampleValue = 0 - carrierFrequencySampleValue;
			}
			double sampleValue = carrierFrequencySampleValue * BASE_AMPLITUDE;

			// modulate for ramp up/ ramp down
			if (rampUp && (time < 0.25)) {
				// ramp up sine wise - 0 at time zero, 1 at time TIME_PER_SYMBOL
				// 
				double radians = time * Math.PI * 2;
				double scaleFactor = Math.sin(radians);
				sampleValue = sampleValue * scaleFactor;
			}
			if (rampDown && (time > 0.75)) {
				// ramp down cosine wise - 1 at time TIME_PER_SYMBOL / 2, 0 at
				// TIME_PER_SYMBOL
				double radians = (time - 0.75) * Math.PI * 2;
				double scaleFactor = Math.cos(radians);
				sampleValue = sampleValue * scaleFactor;
			}

			shortBuffer.put((short) sampleValue);
		}
	}

	private void playByteArray(byte[] buffer) throws LineUnavailableException, IOException {
		InputStream is = new ByteArrayInputStream(buffer);
		AudioFormat audioFormat = new AudioFormat(SAMPLE_RATE, BITS_PER_SAMPLE, NUMBER_OF_CHANNELS, true, true);
		AudioInputStream ais = new AudioInputStream(is, audioFormat, buffer.length / audioFormat.getFrameSize());
		
		//writeAudio(ais);
		
		DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
		SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);

		sourceDataLine.open();
		sourceDataLine.start();

		byte[] playBuffer = new byte[buffer.length];
		int bytesRead;
		while ((bytesRead = ais.read(playBuffer, 0, playBuffer.length)) != -1) {
			sourceDataLine.write(playBuffer, 0, bytesRead);
		}
		sourceDataLine.drain();
		sourceDataLine.stop();
		sourceDataLine.close();
	}

	private void writeAudio(AudioInputStream audioInputStream) {
		try {
			AudioSystem.write(audioInputStream, AudioFileFormat.Type.AU, new File("sample.au"));
		} catch (Exception e) {
			e.printStackTrace();
		}// end catch
	}
	
	public void setFrequency(double frequency) {
		carrierWaveFrequency = frequency;
	}

	/**
	 * @param args
	 * @throws LineUnavailableException
	 * @throws IOException
	 */
	public static void main(String[] args) throws LineUnavailableException, IOException {

		new BPSKModulator().convertStringToSymbols("xb:1KAcxZPRc495U8ZQxnY1ZnhRE46HvA6nUc?a=4.025&l=Bitcoin Booksy1234z");
	}
}