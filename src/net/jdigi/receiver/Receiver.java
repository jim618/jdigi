package net.jdigi.receiver;

import net.jdigi.audio.AudioInputThread;
import net.jdigi.audio.AudioUtils;
import net.jdigi.modems.bpsk.BPSKDemodulator;

public class Receiver {
	private AudioInputThread audioInputThread = null;
	private ModemThread modemThread = null;

	private SpectrumThread spectrumThread = null;

	private static final int REQUIRED_SAMPLE_RATE = 8000;
	private static final int sampleRates[] = { REQUIRED_SAMPLE_RATE, 48000 };

	private Controller controller;

	public Receiver(Controller controller) {
		this.controller = controller;

		// find the audio input
		initAudioIn();

		// start the demodulation thread
		BPSKDemodulator demodulator = new BPSKDemodulator(getSampleRate(), controller);
		modemThread = new ModemThread(demodulator);
		modemThread.setPriority(10);
		modemThread.start();
	}

	public int getSampleRate() {
		return REQUIRED_SAMPLE_RATE;
	}

	public void initAudioIn() {
		int deviceCount = AudioUtils.getMixerCount();
		System.out.println("Receiver/initAudioIn - List of sound devices available :");
		for (int deviceNo = 0; deviceNo < deviceCount; deviceNo++) {
			System.out.println("\tDevice number : " + deviceNo + ", " + AudioUtils.getMixerName(deviceNo));
		}

		if (startReceiver(AudioUtils.getMixerName(1))) {
			System.out.println("Receiver/initAudioIn - Using device " + 1);
		}
	}

	public boolean startReceiver(String soundDevice) {
		if (audioInputThread != null) {
			audioInputThread.stopAudio();
		}
		boolean failed = true;
		for (int n = 0; n < sampleRates.length; n++) {
			int sampleRate = sampleRates[n];
			audioInputThread = new AudioInputThread(REQUIRED_SAMPLE_RATE, sampleRate);
			if (audioInputThread.startAudio(soundDevice, this)) {
				failed = false;
				audioInputThread.setPriority(10);
				spectrumThread = new SpectrumThread(controller);
				spectrumThread.setPriority(1);
				spectrumThread.start();
				break;
			}
		}
		return (!failed);
	}

	public void handleWave(int frame, double data[], int length) {
		// System.out.println("Receiver/handleWave - saw frame = " + frame +
		// " with length = " + length);
		// for (int i = 0 ; i < 32 ; i++) {
		// System.out.print("|" + data[i]);
		// }
		// System.out.println("\n");

		if (length == -1) {
			System.err.println("\n[Missed sample " + frame + "]");
			return;
		}
		if (modemThread != null) {
			modemThread.handleWave(frame, data, length);
		}
		if (spectrumThread != null) {
			spectrumThread.handleWave(frame, data, length);
		}
	}

	public ModemThread getModemThread() {
		return modemThread;
	}

	public SpectrumThread getSpectrumThread() {
		return spectrumThread;
	}

}
