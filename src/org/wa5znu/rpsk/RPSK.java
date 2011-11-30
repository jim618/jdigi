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
import java.awt.event.ItemListener;
import java.awt.ItemSelectable;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import java.io.EOFException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.Socket;
import java.io.IOException;
import java.text.DecimalFormat;

public class RPSK extends Frame {
  boolean isApplet = true;
  Connection connection;
  Waterfall waterfall;
  // TextField freqDisplay;
  Label freqDisplay;
  PhaseClock phaseClock;
  TX tx;
  RX rx;
  int frequency;
  boolean lsb = true;

  OutputTextArea outputText;
  InputTextArea inputTextArea;
  InputTextLine inputTextLine;
  Panel topPanel;
  Panel centerPanel;
  Panel bottomPanel;
  Panel buttonRowPanel;
  Choice modeChoiceBox;

  DecimalFormat mhzFormat = new DecimalFormat("#####.######");

  static final String [] modeNames = {
    "MFSK16",
    "MFSK8",
    "RTTY",
    "THROB1",
    "THROB2",
    "THROB4",
    "BPSK31",
    "QPSK31",
    "PSK63",
    "MT63",
    "FELDHELL",
    "FMHELL"
  };

  final static int WIDTH=1025;
  final static int HEIGHT=640;
  final static int OUTPUT_TEXT_ROWS = 22;
  final static int OUTPUT_TEXT_COLUMNS = 80;
  final static int INPUT_TEXT_ROWS = 7;
  final static int INPUT_TEXT_COLUMNS = 80;
  final static int SPECTRUM_WIDTH = 1024;
  final static int MINISCOPE_SKIP_AMOUNT = 8;
    
  RPSK(String host, int port, boolean isApplet, boolean wantsCompression) {
    super("RPSK by WA5ZNU");
    this.isApplet = isApplet;
    connection = new Connection(this, host, port, wantsCompression);
    setBackground(java.awt.Color.white);
    setLayout(new BorderLayout());
    setResizable(true);
    setVisible(true);
    init();
    setVisible(true);
    connection.requestSetSpectrumWidth(SPECTRUM_WIDTH);
    connection.requestSetMiniscopeSkipAmount(MINISCOPE_SKIP_AMOUNT);
    rx = new RX(connection, this);
    tx = new TX(connection, inputTextArea, inputTextLine);
    // setSize(WIDTH, HEIGHT);
    pack();
  }

  public static void main (String [] args) {
    boolean wantsCompression=false;
    int port=3125;
    String host="localhost";
    // usage:: [hostname] [port] [useCompression]
    if (args.length >=1) {
      host = args[0];
    }
    if (args.length >= 2) {
      port = Integer.parseInt(args[1]);
    } 
    if (args.length == 3) {
      wantsCompression = "true".equals(args[2]);
    }
    RPSK rpsk = new RPSK(host, port, false, wantsCompression);
  }
 
  public void init() {

    // Exit on close
    {
      addWindowListener(new WindowAdapter() {
	  public void windowClosing(WindowEvent evt) {
	    if (isApplet) {
	      dispose(); 
	    } else {
	      System.exit(0);
	    }
	  }
	});
    }

    // Top Panel: Output, Input, Typein
    {
      topPanel = new Panel(new BorderLayout());
      {
	// No matter what I do, this TextArea is wider than the specified
	// number of columns, yet its getColumns reports the original value.
	outputText = new OutputTextArea(OUTPUT_TEXT_ROWS, OUTPUT_TEXT_COLUMNS);
	topPanel.add(outputText, BorderLayout.NORTH);
	outputText.init();
	outputText.setVisible(true);
      }
      {
	inputTextArea = new InputTextArea(INPUT_TEXT_ROWS, INPUT_TEXT_COLUMNS);
	topPanel.add(inputTextArea, BorderLayout.CENTER);
	inputTextArea.setVisible(true);
      }
      {
	inputTextLine = new InputTextLine(INPUT_TEXT_COLUMNS);
	topPanel.add(inputTextLine, BorderLayout.SOUTH);
	inputTextLine.setVisible(true);
      }
      add(topPanel, BorderLayout.NORTH);
    }
    
    // Center Panel: Menus and Buttons, Phase
    {
      String qsoName = "QSO 1";
      centerPanel = new Panel(new FlowLayout());
      add(centerPanel);
      {
	{
	  buttonRowPanel = new Panel(new FlowLayout());
	  {
	    Button b = new Button("Login");
	    b.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		  requirePassword();
		}
	      });
	    buttonRowPanel.add(b, BorderLayout.EAST);
	  }
	  {
	    Button b = new Button("Logout");
	    b.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		  logout();
		}
	      });
	    buttonRowPanel.add(b, BorderLayout.EAST);
	  }
	  {
	    Choice b = new Choice();
	    b.add(qsoName);
	    b.addItemListener(new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
		  Choice c = ((Choice)(e.getItemSelectable()));
		  System.err.println("QSO " + c.getItem(c.getSelectedIndex()));
		}
	      });
	    buttonRowPanel.add(b);
	  }
	  {
	    // freqDisplay = new TextField(7);
	    // freqDisplay.setBackground(Color.white);
	    // freqDisplay.setEditable(false);
	    freqDisplay = new Label();
	    buttonRowPanel.add(freqDisplay, BorderLayout.EAST);
	    freqDisplay.setVisible(true);
	  }
	  {
	    Button b = new Button("TX");
	    b.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		  System.err.println("TX Button");
		  if (requirePassword()) 
		    connection.requestPushButton("txbutton");
		}
	      });
	    buttonRowPanel.add(b, BorderLayout.EAST);
	  }
	  
	  {
	    Button b = new Button("RX");
	    b.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		  System.err.println("RX Button");
		  if (requirePassword()) 
		    connection.requestPushButton("rxbutton");
		}
	      });
	    buttonRowPanel.add(b, BorderLayout.EAST);
	  }
	  {
	    Button b = new Button("ABORT TX!");
	    b.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		  System.err.println("ABORT TX!");
		  if (requirePassword())
		    connection.requestPushButton("abortbutton");
		}
	      });
	    buttonRowPanel.add(b, BorderLayout.EAST);
	  }
	  {
	    modeChoiceBox = new Choice();
	    for (int i = 0; i < modeNames.length; i++) {
	      modeChoiceBox.add(modeNames[i]);
	    }
	    modeChoiceBox.addItemListener(new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
		  Choice c = ((Choice)(e.getItemSelectable()));
		  connection.requestSetMode(c.getItem(c.getSelectedIndex()));
		}
	      });
	    buttonRowPanel.add(modeChoiceBox, BorderLayout.EAST);
	  }
	  {
	    Button b = new Button("Clear RX");
	    b.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		  outputText.setText("");
		}
	      });
	    buttonRowPanel.add(b, BorderLayout.EAST);
	  }
	  {
	    Button b = new Button("Clear TX");
	    b.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    // TODO: should also send to server
		  inputTextArea.clear();
		}
	      });
	    buttonRowPanel.add(b, BorderLayout.EAST);
	  }
	  {
	    Checkbox b = new Checkbox("AFC", true);
	    b.addItemListener(new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
		  lsb = ((Checkbox)(e.getItemSelectable())).getState();
		  System.err.println("AFC=" + lsb);
		  // FIXME: toggles and gets out of sync!
		  connection.requestPushButton("afcbutton");
		}
	      });
	    buttonRowPanel.add(b, BorderLayout.EAST);
	  }
	  {
	    Checkbox b = new Checkbox("SQL", true);
	    b.addItemListener(new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
		  lsb = ((Checkbox)(e.getItemSelectable())).getState();
		  System.err.println("SQL=" + lsb);
		  // FIXME: toggles and gets out of sync!
		  connection.requestPushButton("squelchbutton");
		}
	      });
	    buttonRowPanel.add(b, BorderLayout.EAST);
	  }
	  {
	    Checkbox b = new Checkbox("LSB", false);
	    b.addItemListener(new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
		  lsb = ((Checkbox)(e.getItemSelectable())).getState();
		  System.err.println("LSB=" + lsb);
		  // FIXME: toggles and gets out of sync!
		  connection.requestPushButton("reversebutton");
		}
	      });
	    buttonRowPanel.add(b, BorderLayout.EAST);
	  }
	}
	buttonRowPanel.setVisible(true);
	{
	  phaseClock = new PhaseClock(this);
	  buttonRowPanel.add(phaseClock, BorderLayout.EAST);
	  phaseClock.setVisible(true);
	}
	centerPanel.add(buttonRowPanel, BorderLayout.SOUTH);
      }
      centerPanel.setVisible(true);
    }


    // Bottom panel: Waterfall
    {
      bottomPanel = new Panel(new FlowLayout(FlowLayout.CENTER));
      add(bottomPanel, BorderLayout.SOUTH);
      {
	waterfall = new Waterfall(this, connection);
	bottomPanel.add(waterfall);
	waterfall.init();
	waterfall.setVisible(true);
      }
      bottomPanel.setVisible(true);
    }

    inputTextLine.requestFocus();
    phaseClock.init();
  }

  void login(String callsign, String password) {
    connection.login(callsign, password);
  }

  void logout() {
    connection.logout();
  }

  public synchronized void paint(Graphics screen) {
  }

  /**
   * 
   * @return true if we have a password, false if we pop up a dialog box asking for one.
   */
  boolean requirePassword() {
    if (connection.password != null)
      return true;
    Dialog dialog = new LoginDialog(this);
    dialog.setVisible(true);
    return false;
  }

  String formatMHz(double mhz) {
    return mhzFormat.format(mhz);
  }

  boolean carrierFrequencyKnown() {
    return waterfall.carrierfreq != 0;
  }

  double carrierFrequencyMHz(int offsetTicks) {
    double mhz = waterfall.carrierfreq / 1e6;
    return (lsb ? (mhz - offsetAsHz(offsetTicks)/1e6) : (mhz + offsetAsHz(offsetTicks)/1e6));
  }
  double carrierFrequencyMHz() {
    return carrierFrequencyMHz(frequency);
  }

  int offsetAsHz(int offsetTicks) {
    return ((int)(offsetTicks * Waterfall.hertzPerTick + 0.5));
  }
  int offsetAsHz() {
    return offsetAsHz(frequency);
  }



}

