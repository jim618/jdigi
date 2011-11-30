package net.jdigi.receiver;

import net.jdigi.modems.bpsk.BPSKDemodulator;

public class ModemThread extends Thread {
	BPSKDemodulator demodulator;
	double waveDataIn[] = null;
	int dataLengthIn;
	int dataFrameIn;

	private static final Object lock = new Object();

	public ModemThread(BPSKDemodulator demodulator) {
		this.demodulator = demodulator;
	}

	public void setFrequency(double f) {
		demodulator.setFrequency(f);
	}

	public double getFrequency() {
		return demodulator.getFrequency();
	}

	public void run() {
		while (true) {
			double waveData[] = null;
			int dataLength = 0;
			int dataFrame = 0;
			synchronized (lock) {
				try {
					lock.wait();
					dataFrame = dataFrameIn;
					waveData = waveDataIn;
					dataLength = dataLengthIn;
					// Dropping the lock before processing causes us to drop
					// frames, which we don't want to do in a Modem.
					// Dropping the lock after we process serializes all
					// demodulators.
					// We really need to queue the frames.
					if (waveData != null) {
						demodulator.handleWave(dataFrame, waveData, dataLength);
					}
				} catch (InterruptedException exp) {
				}
			}
		}
	}

	// Accept the incoming sound samples
	public void handleWave(int frame, double data[], int length) {
		synchronized (lock) {
			dataFrameIn = frame;
			waveDataIn = data;
			dataLengthIn = length;
			lock.notifyAll();
		}
	}

}
