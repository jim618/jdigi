package net.jdigi.modems.spatula;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

//import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;

public class SpatulaModulator {
	private static final int NUMBER_OF_CHANNELS = 1;
	private static final int BITS_PER_SAMPLE = 16;
	private static final int BIT_SET_MULTIPLIER = 4;
	private static final int BITS_PER_WORD = 8;
	private static final int SAMPLE_RATE = 8000; // 44100 if you want a really
													// nice, clean sin wave, but
													// then you must change
													// FFT_SIZE to at least
													// 16384 too
	private static final int BYTES_PER_SAMPLE = 2;
	private static final int BASE_FREQUENCY = 110;
	private static final int FRAME_SIZE = 32;
	private static final int SAMPLES_PER_CHARACTER = SAMPLE_RATE / BASE_FREQUENCY * 2;
	private static final double BASE_AMPLITUDE = 4095;
	private static final int FFT_SIZE = 4096; // 16384 if you use 44100 as the
												// sample rate. FFT happens
												// faster with smaller sizes.

	public void modulate(String string) throws LineUnavailableException, IOException {

		byte[] buffer = new byte[FRAME_SIZE * SAMPLES_PER_CHARACTER * BYTES_PER_SAMPLE];
		ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
		ShortBuffer shortBuffer = byteBuffer.asShortBuffer();

		for (int i = 0; i < string.length() && i < FRAME_SIZE; i++) {
			byte byteToModulate = (byte) string.charAt(i);

			modulateByte(shortBuffer, byteToModulate, i % 2 == 0);
		}

		playByteArray(buffer);

		//demodulateSampleBuffer(shortBuffer);
	}

	private void modulateByte(ShortBuffer shortBuffer, byte byteToModulate, boolean even) {
		for (int sampleCount = 0; sampleCount < SAMPLES_PER_CHARACTER; sampleCount++) {
			double time = (double) sampleCount / (double) SAMPLE_RATE;
			double sampleValue = 0;
			int oneBits = 0;

			// sum the signals for each bit
			for (int bitNumber = 0; bitNumber < BITS_PER_WORD; bitNumber++) {
				boolean bitSet = ((byteToModulate & (byte) (Math.pow(2, bitNumber))) != 0);
				if (bitSet) {
					oneBits++;
				}
				sampleValue += Math.sin(2 * Math.PI * BASE_FREQUENCY * (bitNumber + 1) * time) * BASE_AMPLITUDE
						* (bitSet ? BIT_SET_MULTIPLIER : 1);
			}

			// add in the parity bit
			boolean setParity = ((even && (oneBits % 2 != 0)) || (!even && (oneBits % 2 == 0)));
			sampleValue += Math.sin(2 * Math.PI * BASE_FREQUENCY * (BITS_PER_WORD + 1) * time) * BASE_AMPLITUDE
					* (setParity ? BIT_SET_MULTIPLIER : 1);

			// average the signals
			sampleValue /= (BITS_PER_WORD + 1);
			shortBuffer.put((short) sampleValue);
		}
	}

	private void playByteArray(byte[] buffer) throws LineUnavailableException, IOException {
		InputStream is = new ByteArrayInputStream(buffer);
		AudioFormat audioFormat = new AudioFormat(SAMPLE_RATE, BITS_PER_SAMPLE, NUMBER_OF_CHANNELS, true, true);
		AudioInputStream ais = new AudioInputStream(is, audioFormat, buffer.length / audioFormat.getFrameSize());
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

	// private void demodulateSampleBuffer(ShortBuffer shortBuffer) {
	// DemodulatedCharacter lastChar = null;
	// for (int i = 0; i < shortBuffer.capacity(); i += SAMPLES_PER_CHARACTER /
	// 2)
	// {
	// DemodulatedCharacter nextChar = demodulateCharacter(shortBuffer, i,
	// SAMPLES_PER_CHARACTER / 2);
	// if (!nextChar.equals(lastChar))
	// {
	// lastChar = nextChar;
	// System.out.print(nextChar);
	// }
	// }
	// System.out.println();
	// }

	// private DemodulatedCharacter demodulateCharacter(ShortBuffer shortBuffer,
	// int offset, int length) {
	// float[] floatArray = new float[FFT_SIZE * 2];
	// for (int i = offset; i < shortBuffer.capacity() && i < offset + length;
	// i++)
	// {
	// floatArray[i - offset] = shortBuffer.get(i);
	// }
	//
	// FloatFFT_1D fft = new FloatFFT_1D(FFT_SIZE);
	// fft.realForward(floatArray);
	//
	// int multiplier = (int) (BASE_FREQUENCY / ((float) SAMPLE_RATE / (float)
	// FFT_SIZE));
	//
	// long maxPower = findMaxPower(floatArray, multiplier);
	//
	// int value = 0;
	// for (int i = 0; i < BITS_PER_WORD; i++)
	// {
	// int index = (i + 1) * multiplier;
	// long power = computePowerAtIndex(floatArray, index);
	// if (power > (maxPower / (BIT_SET_MULTIPLIER / 2)))
	// {
	// value += Math.pow(2, i);
	// }
	// }
	//
	// DemodulatedCharacter character = new DemodulatedCharacter();
	// character.setData((char) value);
	//
	// long parityPower = computePowerAtIndex(floatArray, (BITS_PER_WORD + 1) *
	// multiplier);
	// if (parityPower > (maxPower / (BIT_SET_MULTIPLIER / 2)))
	// {
	// character.setParity(true);
	// }
	//
	// return character;
	//
	// }

	private long findMaxPower(float[] floatArray, int multiplier) {
		long maxPower = 0;
		for (int i = 0; i < BITS_PER_WORD; i++) {
			int index = (i + 1) * multiplier;
			long power = computePowerAtIndex(floatArray, index);
			if (power > maxPower) {
				maxPower = power;
			}
		}
		return maxPower;
	}

	private long computePowerAtIndex(float[] floatArray, int index) {
		return (long) Math.sqrt(Math.pow(floatArray[index * 2], 2) + Math.pow(floatArray[index * 2 + 1], 2));
	}

	private static class DemodulatedCharacter {
		private char data;
		private boolean parity;

		public char getData() {
			return data;
		}

		public void setData(char data) {
			this.data = data;
		}

		public boolean isParity() {
			return parity;
		}

		public void setParity(boolean parity) {
			this.parity = parity;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + data;
			result = prime * result + (parity ? 1231 : 1237);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DemodulatedCharacter other = (DemodulatedCharacter) obj;
			if (data != other.data)
				return false;
			if (parity != other.parity)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return String.valueOf(getData());
		}
	}

	/**
	 * @param args
	 * @throws LineUnavailableException
	 * @throws IOException
	 */
	public static void main(String[] args) throws LineUnavailableException, IOException {

		new SpatulaModulator().modulate("The quick brown fox jumps over the lazy dog");
	}
}