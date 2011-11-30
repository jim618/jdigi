/*
 * This file is part of znudigi.
 * Released under GNU GENERAL PUBLIC LICENSE Version 2
 * See file COPYING.
 * Copyright (C) 2007-2008 Leigh L. Klotz, Jr. <Leigh@WA5ZNU.org>
 */

package org.wa5znu.znuradio.audio;

import org.wa5znu.znuradio.dsp.Subsampler;
import org.wa5znu.znuradio.receiver.WaveHandler;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioInputThread extends Thread
{
  private static TargetDataLine targetDataLine;
  private static int frameSize;
  private static boolean stopRequested=true;
  private static WaveHandler waveHandler;
  private int deviceSampleRate;
  private int outputSampleRate;
  private byte frameBytes[];
  private Subsampler subsampler;
  private double data[];
  private int frame = 0;
  private ByteBuffer frameByteBuffer;
  private static final int OUTPUT_SAMPLES=2048; // Crock: Fixed at 2048 samples 

  public AudioInputThread(int outputSampleRate, int deviceSampleRate) {
    this.deviceSampleRate=deviceSampleRate;
    this.outputSampleRate=outputSampleRate;
    int oversampleRate = deviceSampleRate/outputSampleRate;
    if (outputSampleRate>1) {
      subsampler = new Subsampler(oversampleRate);
    }
    else if (oversampleRate<1) {
      throw new RuntimeException("AudioInputThread cannot interpolate: outputSampleRate="+outputSampleRate+" deviceSampleRate="+deviceSampleRate);
    }
    frameSize = OUTPUT_SAMPLES * oversampleRate;
    frameBytes = new byte[frameSize*2];
    frameByteBuffer = ByteBuffer.wrap(frameBytes).order(ByteOrder.LITTLE_ENDIAN);
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
      // TODO: Ought to check for timeout or missed samples
      frameByteBuffer.clear();
      int bytesRead = targetDataLine.read(frameBytes, 0, frameBytes.length);
      if (bytesRead == frameBytes.length) {
	// convert two bytes to 16-bit signed value and scale to +/- 1.0
	double data[] = new double[nInputSamples];
	for (int n = 0; n < nInputSamples; n++) {
	  data[n] = (double)(frameByteBuffer.getShort()) / 32768.0;	    
	}
	if (subsampler != null)
	  data = subsampler.subsample(data, nInputSamples);
	
	//System.out.println("AudioInputThread/run ");
	//for (int i = 0 ; i < 32 ; i++) {
	//	System.out.print("|" + data[i]);
	//}
	//System.out.println("\n");

	// TODO: Handle interpolation and fractional subsampling
	waveHandler.handleWave(frame, data, OUTPUT_SAMPLES);
      }
      frame++;
    }
  }

  public boolean startAudio(String deviceName, WaveHandler waveHandler) {
    stopAudio();
    // Linear PCM encoding at specified deviceSampleRate and the following parameters:
    {
      int sampleBits = 16;
      int channels = 1;
      boolean signed = true;
      boolean bigEndian = false;
      AudioFormat audioFormat = new AudioFormat(deviceSampleRate, sampleBits, channels, signed, bigEndian);
      targetDataLine = AudioUtils.getTargetDataLine(deviceName, audioFormat);
    }
    if (targetDataLine == null)  {
      return false;
    }
    this.waveHandler = waveHandler;
    start();
    return true;
  }

  public void stopAudio() {
    if (stopRequested || targetDataLine == null) return;
    stopRequested = true;
    interrupt();
    try {
      join();
    } catch (InterruptedException exp) { }
    targetDataLine.stop();
    targetDataLine.close();
    targetDataLine=null;
  }

}
