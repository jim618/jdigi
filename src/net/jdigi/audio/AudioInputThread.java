package net.jdigi.audio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.TargetDataLine;

import net.jdigi.dsp.Subsampler;
import net.jdigi.receiver.Receiver;

public class AudioInputThread extends Thread {
	private TargetDataLine targetDataLine;
	private int frameSize;
	private boolean stopRequested = true;
	private Receiver receiver = null;
	private int deviceSampleRate;
	private byte frameBytes[];
	private Subsampler subsampler;
	private double data[];
	private int frame = 0;
	private ByteBuffer frameByteBuffer;
	private static final int OUTPUT_SAMPLES = 2048;

	public AudioInputThread(int outputSampleRate, int deviceSampleRate) {
		this.deviceSampleRate = deviceSampleRate;
		int oversampleRate = deviceSampleRate / outputSampleRate;
		if (outputSampleRate > 1) {
			subsampler = new Subsampler(oversampleRate);
		} else if (oversampleRate < 1) {
			throw new RuntimeException(
					"AudioInputThread cannot interpolate: outputSampleRate="
							+ outputSampleRate + " deviceSampleRate="
							+ deviceSampleRate);
		}
		frameSize = OUTPUT_SAMPLES * oversampleRate;
		frameBytes = new byte[frameSize * 2];
		frameByteBuffer = ByteBuffer.wrap(frameBytes).order(
				ByteOrder.LITTLE_ENDIAN);
		data = new double[OUTPUT_SAMPLES * oversampleRate];
	}

	public void start() {
		stopRequested = false;
		targetDataLine.start();
		super.start();
	}

	public void run() {
		int nInputSamples = data.length;
		while (!stopRequested) {
			frameByteBuffer.clear();
			int bytesRead = targetDataLine.read(frameBytes, 0,
					frameBytes.length);
			if (bytesRead == frameBytes.length) {
				// convert two bytes to 16-bit signed value and scale to +/- 1.0
				double data[] = new double[nInputSamples];
				for (int n = 0; n < nInputSamples; n++) {
					data[n] = (double) (frameByteBuffer.getShort()) / 32768.0;
				}
				if (subsampler != null)
					data = subsampler.subsample(data, nInputSamples);
				
				//System.out.println("AudioInputThread/run ");
				//for (int i = 0 ; i < 32 ; i++) {
				//	System.out.print("|" + data[i]);
				//}
				//System.out.println("\n");
				
				receiver.handleWave(frame, data, OUTPUT_SAMPLES);
			}
			frame++;
		}
	}

	public boolean startAudio(String deviceName, Receiver receiver) {
		stopAudio();
		// Linear PCM encoding at specified deviceSampleRate and the following
		// parameters:
		int sampleBits = 16;
		int channels = 1;
		boolean signed = true;
		boolean bigEndian = false;
		AudioFormat audioFormat = new AudioFormat(deviceSampleRate, sampleBits,
				channels, signed, bigEndian);
		targetDataLine = AudioUtils.getTargetDataLine(deviceName, audioFormat);

		if (targetDataLine == null) {
			return false;
		}
		this.receiver = receiver;
		start();
		return true;
	}

	public void stopAudio() {
		if (stopRequested || targetDataLine == null) {
			return;
		}
		stopRequested = true;
		interrupt();
		try {
			join();
		} catch (InterruptedException exp) {
		}
		targetDataLine.stop();
		targetDataLine.close();
		targetDataLine = null;
	}
}
