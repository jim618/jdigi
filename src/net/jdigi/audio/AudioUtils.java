package net.jdigi.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

public abstract class AudioUtils {
	public static TargetDataLine getTargetDataLine(String mixerName,
			AudioFormat audioFormat) {
		TargetDataLine targetDataLine = null;
		System.out.println("[Trying " + mixerName + " " + audioFormat + "]");
		try {
			Mixer.Info mixerInfo = null;
			if (mixerName != null) {
				mixerInfo = getMixer(mixerName);
			}
			if (mixerInfo != null) {
				Mixer mixer = AudioSystem.getMixer(mixerInfo);
				DataLine.Info info = new DataLine.Info(TargetDataLine.class,
						audioFormat);
				if (AudioSystem.isLineSupported(info)) {
					targetDataLine = (TargetDataLine) mixer.getLine(info);
					targetDataLine.open(audioFormat, targetDataLine
							.getBufferSize());
				}
			}
		} catch (LineUnavailableException e) {
			targetDataLine = null;
			System.out.println("[Unavailable: " + mixerName + " "
					+ e.getMessage() + "]");
			// e.printStackTrace();
		} catch (IllegalArgumentException e) {
			targetDataLine = null;
			System.out.println("[Unavailable: " + mixerName + " "
					+ e.getMessage() + "]");
			// e.printStackTrace();
		}
		return targetDataLine;
	}

	public static String getMixerName(int i) {
		Mixer.Info a[] = AudioSystem.getMixerInfo();
		if (i < a.length) {
			return a[i].getName();
		} else {
			return null;
		}
	}

	public static int getMixerCount() {
		Mixer.Info a[] = AudioSystem.getMixerInfo();
		return a.length;
	}

	private static Mixer.Info getMixer(String name) {
		Mixer.Info a[] = AudioSystem.getMixerInfo();
		for (int i = 0; i < a.length; i++) {
			if (a[i].getName().equals(name)) {
				return a[i];
			}
		}
		return null;
	}
}
