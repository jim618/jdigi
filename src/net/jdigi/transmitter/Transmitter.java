package net.jdigi.transmitter;

import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;

import net.jdigi.modems.bpsk.BPSKModulator;

public class Transmitter extends Thread {

	private final BPSKModulator bpskModulator;
	
	private String textToTransmit;
	
	public Transmitter() {
		super();
		bpskModulator = new BPSKModulator();
		textToTransmit = null;
		start();
	}
	
	public void transmit(String textToTransmit) {
		this.textToTransmit = textToTransmit;
	}
	
	public void setFrequency(double frequency) {
		bpskModulator.setFrequency(frequency);
	}
	
	@Override
	public void run() {
		while (true) {
			if (textToTransmit != null) {
				String text = textToTransmit;
				textToTransmit = null;
				try {
					bpskModulator.convertStringToSymbols(text);
				} catch (LineUnavailableException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
