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




public class LoginDialog extends Dialog {
  public LoginDialog(final RPSK parent) {
    super(parent, "TX Authorization Password", true);
    final TextField passwordField = new TextField(32);
    final TextField callsignField = new TextField(32);
    final LoginDialog loginDialog = this;
    setLayout(new GridLayout(3, 1));
    {
      Panel p = new Panel(new FlowLayout());
      ActionListener callsignHandler = (new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    passwordField.requestFocus();	  
	  }
	});
      callsignField.addActionListener(callsignHandler);
      p.add(new Label("Call"));
      p.add(callsignField);
      callsignField.setVisible(true);
      add(p);
    }

    ActionListener passwordHandler = (new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  parent.login(callsignField.getText(), passwordField.getText());
	  loginDialog.setVisible(false);
	}
      });
    {
      Panel p = new Panel(new FlowLayout());
      passwordField.addActionListener(passwordHandler);
      passwordField.setEchoChar('*');
      p.add(new Label("Password"));
      p.add(passwordField, BorderLayout.WEST);
      passwordField.setVisible(true);
      add(p);
    }
    
    {
      Panel p = new Panel(new FlowLayout());
      addWindowListener(new WindowAdapter() {
	  public void windowClosing(WindowEvent evt) {
	    loginDialog.setVisible(false);
	    loginDialog.dispose();
	  }
	});
      {
	Button b = new Button("OK");
	b.addActionListener(passwordHandler);
	p.add(b);
      }
      {
	Button b = new Button("Cancel");
	b.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	      loginDialog.setVisible(false);
	      loginDialog.dispose();
	    }
	  });
	p.add(b);
      }
      add(p);
    }
    pack();
  }
}
