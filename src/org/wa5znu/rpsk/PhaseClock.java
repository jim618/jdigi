// Copyright (c) 2004 Leigh L. Klotz, Jr. <leigh@wa5nzu.org>        
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

public class PhaseClock extends Canvas
{
  RPSK parent;
  final static int LENGTH = 20;
  final static double dLENGTH = (double)LENGTH;
  final static int WIDTH = ((int)(Math.round(Math.sqrt((dLENGTH*dLENGTH*5)))));
  final static int HEIGHT = WIDTH;
  Image offScreenImage; 
  Graphics offScreenGraphics; 
  final static int MAX_PHASES=8;
  int p = 0;

  PhaseClock(RPSK parent) {
    super();
    this.parent=parent;
    setSize(WIDTH, HEIGHT);
  }
 

  // You must call this
  public void init() {
    setVisible(true);
    offScreenImage = createImage(WIDTH, HEIGHT);
    if (! isDisplayable()) throw new RuntimeException("component is not displayable");
    offScreenGraphics = offScreenImage.getGraphics();
    offScreenGraphics.setColor(Color.black);
    offScreenGraphics.fillRect(0, 0, WIDTH, HEIGHT);
    offScreenGraphics.setColor(Color.white);
    drawCircle(offScreenGraphics);
  }

  public synchronized void paint(Graphics screen) {
    if (offScreenImage == null) {
      System.err.println("paint; offScreenImage null");
      offScreenImage = createImage(WIDTH, HEIGHT);
      offScreenGraphics = offScreenImage.getGraphics();
    }
    screen.drawImage(offScreenImage, 0, 0, this);
  }

  boolean handlePhase(Connection connection) throws IOException {
    InputStream stream = connection.inputStream;
    return handlePhaseInternal(stream, false);
  }

  boolean handlePhaseHighlight(Connection connection) throws IOException {
    InputStream stream = connection.inputStream;
    return handlePhaseInternal(stream, true);
  }

  private boolean handlePhaseInternal(InputStream stream, boolean dcd) throws IOException {
    int phase;
    synchronized(stream) {	// why sync here? it's too late if someone else is reading it...
      phase = stream.read();
    }
    if (phase < 0) return false;
    int fromx = WIDTH/2;
    int fromy = HEIGHT/2;
    if (p == 0) {
      offScreenGraphics.setColor(Color.black);
      offScreenGraphics.fillRect(0, 0, WIDTH, HEIGHT);
      offScreenGraphics.setColor(Color.white);
      drawCircle(offScreenGraphics);
    }
    double angle = (phase / 256.0) * Math.PI*2;
    angle -= Math.PI/2.0;
    int tox = (int)(fromx+LENGTH*Math.cos(angle));
    int toy = (int)(fromy+LENGTH*Math.sin(angle));
    offScreenGraphics.setColor(dcd ? Color.green : Color.yellow);
    offScreenGraphics.drawLine(fromx, fromy, tox, toy);
    if (p++ == MAX_PHASES) {
      repaint();
      p = 0;
    }
    return true;
  }

  private void drawCircle(Graphics offScreenGraphics) {
    offScreenGraphics.drawOval((WIDTH/2-LENGTH), (HEIGHT/2-LENGTH), LENGTH*2, LENGTH*2);
  }

  public void update(Graphics screen) {
    paint(screen);
  }
}
