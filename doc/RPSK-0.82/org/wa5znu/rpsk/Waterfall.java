// Copyright (c) 2004, 2007 Leigh L. Klotz, Jr. <leigh@wa5nzu.org>        
// Color table from fldigi Copyright 2006 Dave Freese W1HKJ
// Tic code from gMFSK by Tomi Manninen, used under explicit license.
// 
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//                                        
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//                                        
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package org.wa5znu.rpsk;

import java.awt.*;
// import java.awt.image.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseMotionAdapter;


import java.io.File;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import java.io.EOFException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.IOException;

public class Waterfall extends Canvas
{
  static class Palette {
    int r, g, b;
    Palette(int r, int g, int b) { 
      this.r=r; this.b=b; this.g=g;
    }
  }

  static final int MAXWIDTH=1024;
  static final int WIDTH = 1024;
  static final int WATERFALL_HEIGHT = 128;
  static final int RULER_HEIGHT = 20;
  final Color FREQUENCY_COLOR = Color.green;
  final Color HIGHLIGHT_FREQUENCY_COLOR = Color.magenta;
  final Color FREQUENCY_SCALE_COLOR = Color.black;
  byte[] bytes = new byte[MAXWIDTH];
  Palette[] palette;
  Color[] colorTable = new Color[256];
  int frequencyInTicks = -1;
  int highlightFrequencyInTicks = -1;
  WaterfallMouseListener waterfallMouseListener;
  WaterfallMouseMotionListener waterfallMouseMotionListener;
  static final int ACK_INTERVAL = 25;
  int ackCounter = ACK_INTERVAL;
  static final double hertzPerTick = (4000.0 / 1024.0);
  double startFrequency = 0.0;
  double bandwidthInHertz;
  int halfBandwidthInTicks;
  RPSK parent; 
  boolean lsb = false;
  double carrierfreq = 0;
  Connection connection;
  Image offScreenImage; 
  Graphics offScreenGraphics; 
  byte[] dataBufferBytes;
  final static boolean bwPalette=false;

  Waterfall(RPSK parent, Connection connection) {
    super();
    this.parent = parent;
    this.connection = connection;
    setSize(WIDTH, WATERFALL_HEIGHT + RULER_HEIGHT);
    waterfallMouseMotionListener = new WaterfallMouseMotionListener(this);
    waterfallMouseListener = new WaterfallMouseListener(this);
    addMouseMotionListener(waterfallMouseMotionListener);
    addMouseListener(waterfallMouseListener);
    setBandwidthInHz(31.25);
  }
 

  // you must call init
  public void init() {
    setVisible(true);
    insureImageBuffer();
    if (! isDisplayable()) throw new RuntimeException("component is not displayable");
    initColorTable();
  }

  void readPalette() {
    palette = new Palette[9];
    palette[0] = new Palette(0,0,0);
    palette[1] = new Palette(0,0,62);
    palette[2] = new Palette(0,0,126);
    palette[3] = new Palette(0,0,214);
    palette[4] = new Palette(145, 142, 96);
    palette[5] = new Palette(181, 184, 48);
    palette[6] = new Palette(223, 226, 105);
    palette[7] = new Palette(254, 254, 4);
    palette[8] = new Palette(255, 58, 0);
  }

  void initColorTable() {
    if (bwPalette) {
      for (int i = 0; i < 256; i++) {
	int di = (int)(Math.sqrt((double)i / 256.0)*256);
	colorTable[i] = new Color(di, di, di);
      }
	    
    } else {
      readPalette();
      for (int n = 0; n < 8; n++) {
	for (int i = 0; i < 32; i++) {
	  int r = palette[n].r + (int)(1.0 * i * (palette[n+1].r - palette[n].r) / 32.0);
	  int g = palette[n].g + (int)(1.0 * i * (palette[n+1].g - palette[n].g) / 32.0);
	  int b = palette[n].b + (int)(1.0 * i * (palette[n+1].b - palette[n].b) / 32.0);
	  colorTable[i + 32*n] = new Color(r, g, b);
	}
      }
    }
  }

  void insureImageBuffer() {
    if (offScreenImage == null) {
      offScreenImage = createImage(WIDTH, WATERFALL_HEIGHT);
      offScreenGraphics = offScreenImage.getGraphics();
      offScreenGraphics.setColor(Color.BLACK);
      offScreenGraphics.fillRect(0, 0, WIDTH, WATERFALL_HEIGHT);
    }
  }

  public synchronized void paint(Graphics screen) {
    if (offScreenImage == null) {
      insureImageBuffer();
    }
    screen.drawImage(offScreenImage, 0, RULER_HEIGHT, this);
    if (highlightFrequencyInTicks > 0 && highlightFrequencyInTicks != frequencyInTicks) {
      screen.setColor(HIGHLIGHT_FREQUENCY_COLOR);      
      screen.drawLine(highlightFrequencyInTicks - halfBandwidthInTicks, RULER_HEIGHT, highlightFrequencyInTicks - halfBandwidthInTicks, WATERFALL_HEIGHT+RULER_HEIGHT);
      screen.drawLine(highlightFrequencyInTicks + halfBandwidthInTicks, RULER_HEIGHT, highlightFrequencyInTicks + halfBandwidthInTicks, WATERFALL_HEIGHT+RULER_HEIGHT);
    }
    if (frequencyInTicks >= 0) {
      //screen.setColor(FREQUENCY_COLOR);
      //screen.fillRect(frequencyInTicks, 0, 1, 1);
      screen.setColor(FREQUENCY_COLOR);      
      screen.drawLine(frequencyInTicks - halfBandwidthInTicks, RULER_HEIGHT, frequencyInTicks - halfBandwidthInTicks, WATERFALL_HEIGHT+RULER_HEIGHT);
      screen.drawLine(frequencyInTicks + halfBandwidthInTicks, RULER_HEIGHT, frequencyInTicks + halfBandwidthInTicks, WATERFALL_HEIGHT+RULER_HEIGHT);
    }
    {
      drawFrequencyScale(screen);
    }
  }

  void setBandwidthInHz(double hz) {
    bandwidthInHertz = hz;
    halfBandwidthInTicks = ((int)((((bandwidthInHertz / hertzPerTick))/2.0)+0.5));
  }


  // 3 bytes scaled as mHz
  boolean handleBandwidth() throws IOException {
    int milliHz = 0;
    InputStream stream = connection.inputStream;
    synchronized(stream) {
      boolean eof = false;
      double hz = 0.0;
      int byte1 = stream.read();
      if (byte1 < 0) return false;
      int byte2 = stream.read();
      if (byte2 < 0) return false;
      int byte3 = stream.read();
      if (byte3 < 0) return false;
      milliHz = ((byte1 * 65536) + (byte2 * 256) + byte3);
    }
    setBandwidthInHz(milliHz / 1000.0);
    repaint();
    return true;
  }

  boolean handleSpectrum() throws IOException {
    boolean eof=false;
    double doubleValue = 0;
    int intValue = 0;
    int z=0;
    int len = 0;
    InputStream stream = connection.inputStream;
    synchronized (stream) {
      int len1 = stream.read();
      if (len1 < 0) return false;
      int len2 = stream.read();
      if (len2 < 0) return false;
      len = len1 * 256 + len2;
      int start = 0;
      int togo = len;
      while (togo > 0) {
	int bytesRead = -1;
	bytesRead = stream.read(bytes, start, togo);
	// bytesRead = stream.read(dataBufferBytes, start, togo);
	if (bytesRead < 0) {
	  eof = true;
	  break;
	}
	start += bytesRead;
	togo -= bytesRead;
      }
    }
    if (eof) return false;
    scroll();
    for (int x = 0; x < len; x++) {
      int b = bytes[x];
      plot(x, 0, b<0 ? b+256 : b);
    }
    if (frequencyInTicks >= 0) {
      //offScreenGraphics.setColor(FREQUENCY_COLOR);
      //offScreenGraphics.fillRect(frequencyInTicks, 0, 1, 1);
    }
    if (--ackCounter <= 0) {
      ackCounter = ACK_INTERVAL;
      connection.sendAck();
    }
    repaint();
    return true;
  }

  boolean handleSpectrumCompressed() throws IOException {
    boolean eof=false;
    double doubleValue = 0;
    int intValue = 0;
    int z=0;
    int len = 0;
    InputStream stream = connection.inputStream;
    synchronized (stream) {
      int len1 = stream.read();
      if (len1 < 0) return false;
      int len2 = stream.read();
      if (len2 < 0) return false;
      len = len1 * 256 + len2;
      int start = 0;
      int togo = len;
      while (togo > 0) {
 	int bytesRead = -1;
 	bytesRead = stream.read(bytes, start, togo);
 	// bytesRead = stream.read(dataBufferBytes, start, togo);
 	if (bytesRead < 0) {
 	  eof = true;
 	  break;
 	}
 	start += bytesRead;
 	togo -= bytesRead;
      }
    }
    if (eof) return false;
    scroll();
    for (int x = 0; x < len; x++) {
      int b = bytes[x];
      if (b<0) b+=256;
      int n1 =  1 * (b & 0xf0);
      int n2 = 16 * (b & 0x0f);
      plot(x*2+0, 0, n1);
      plot(x*2+1, 0, n2);
    }
    if (frequencyInTicks >= 0) {
      //offScreenGraphics.setColor(FREQUENCY_COLOR);
      //offScreenGraphics.fillRect(frequencyInTicks, 0, 1, 1);
    }
    if (--ackCounter <= 0) {
      ackCounter = ACK_INTERVAL;
      connection.sendAck();
    }
    repaint();
    return true;
  }
 


  private void scroll() {
    offScreenGraphics.copyArea(0, 0, WIDTH, WATERFALL_HEIGHT-1, 0, 1);
  }

  private void plot(int x, int y, int color) {
    offScreenGraphics.setColor(colorTable[color]);
    offScreenGraphics.fillRect(x, y, 1, 1);
  }

  public void update(Graphics screen) {
    paint(screen);
  }

  // Called when the server changes audio frequencey
  void setFrequency(int frequencyInTicks) {
    this.frequencyInTicks = frequencyInTicks;
  }

  // Called when the mouse moves into the waterfall but isn't clicked
  public void setHighlightFrequency(int frequencyInTicks) {
    highlightFrequencyInTicks = frequencyInTicks;
  }

  // Called when the mouse exits the waterfall but isn't clicked
  public void clearHighlightFrequency() {
    setHighlightFrequency(-1);
  }
  

  class WaterfallMouseMotionListener extends MouseMotionAdapter {
    Waterfall waterfall;
    WaterfallMouseMotionListener(Waterfall waterfall) {
      super();
      this.waterfall = waterfall;
    }

    //  public void mouseDragged(MouseEvent event) {
    //    System.err.println("mouseDragged event " + event);
    //  }

    public void mouseMoved(MouseEvent event) {
      waterfall.setHighlightFrequency(event.getX());
    }

  }

  class WaterfallMouseListener extends MouseAdapter {
    Waterfall waterfall;
    WaterfallMouseListener(Waterfall waterfall) {
      super();
      this.waterfall = waterfall;
    }

    public void mouseClicked(MouseEvent event) {
      connection.requestSetFrequency(event.getX());
    }

    //  public void mousePressed(MouseEvent event) {
    //    System.err.println("mousePressed event " + event);
    //  }
    //
    //  public void mouseReleased(MouseEvent event) {
    //    System.err.println("mouseReleased event " + event);
    //  }
    //
    //  public void mouseDragged(MouseEvent event) {
    //    System.err.println("mouseDragged event " + event);
    //  }
    //    public void mouseEntered(MouseEvent event) {
    //      System.err.println("mouseEntered event " + event);
    //      waterfall.setHighlightFrequency(event.getX());
    //    }

    public void mouseExited(MouseEvent event) {
      waterfall.clearHighlightFrequency();
    }
  }

  // This method was adapted from gMFSK-0.6 Beta 4 and used 
  // used on the MIT/X11 license granted for this purpose
  // by Tomi Manninen <oh2bns@sral.fi>
  void drawFrequencyScale(Graphics screen) {
    screen.setColor(FREQUENCY_SCALE_COLOR);
    Tic tics = buildTics(screen);
    while (tics != null) {
      if (tics.major) {
	screen.drawLine(tics.x, RULER_HEIGHT - 9, tics.x, RULER_HEIGHT - 1);
	screen.drawString(tics.str, tics.strx, tics.strh - 5);
      } else {
	screen.drawLine(tics.x, RULER_HEIGHT - 5, tics.x, RULER_HEIGHT - 1);
      }
      tics = tics.next;
    }
  }

  // This method was adapted from gMFSK-0.6 Beta 4 and used 
  // used on the MIT/X11 license granted for this purpose
  // by Tomi Manninen <oh2bns@sral.fi>
  private Tic buildTics(Graphics screen) {
    Tic list, p;
    double f, realFreq;
    int i, ifreq, width;
    FontMetrics fontMetrics = screen.getFontMetrics(); 
    int fontHeight = fontMetrics.getHeight();

    list = null;

    width = getWidth();
    f = startFrequency;

    for (i = 0; i < width; i++) {
      if (lsb)
	realFreq = f - carrierfreq;
      else
	realFreq = f + carrierfreq;

      realFreq = Math.abs(realFreq);
      ifreq = (int)(100 * Math.floor(realFreq / 100.0 + 0.5));

      if (ifreq < realFreq || ifreq >= realFreq + hertzPerTick) {
	f += hertzPerTick;
	continue;
      }

      p = new Tic();
      p.major = false;
      p.freq = ifreq;
      p.x = i;

      if ((ifreq % 500) == 0) {
	Font font;

	int khz = ifreq / 1000;
	int hz  = ifreq % 1000;

	if (khz > 9)
	  p.str= khz + "." + hz;
	else
	  p.str= "" + ifreq;
	p.strw = fontMetrics.stringWidth(p.str);
	p.strh = fontHeight;
	p.strx = clamp(i - p.strw / 2, 0, width - p.strw);

	p.major = true;
      }

      f += hertzPerTick;

      p.next = list;
      list = p;
    }
    return list;
  }

  // This method was adapted from gMFSK-0.6 Beta 4 and used 
  // used on the MIT/X11 license granted for this purpose
  // by Tomi Manninen <oh2bns@sral.fi>
  private int clamp(int x,int low,int high) {
    return (((x)>(high))?(high):(((x)<(low))?(low):(x)));
  }

  // This method was adapted from gMFSK-0.6 Beta 4 and used 
  // used on the MIT/X11 license granted for this purpose
  // by Tomi Manninen <oh2bns@sral.fi>
  static class Tic {
    boolean major;
    double freq;
    String str;
    int strw;
    int strh;
    int strx;
    int x;
    Tic next;
  }

}




