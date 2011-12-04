package net.jdigi.ui;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;

import net.jdigi.receiver.Controller;

public class Waterfall extends Canvas {
	private static final long serialVersionUID = 536699012446305L;

	static class Palette {
		int r, g, b;

		Palette(int r, int g, int b) {
			this.r = r;
			this.b = b;
			this.g = g;
		}
	}

	private static final int WIDTH = 1024;
	private static final double MAXIMUM_FREQUENCY = 4000.0;
	public static final double FREQUENCY_TO_PIXELS_SCALE_FACTOR = MAXIMUM_FREQUENCY / WIDTH;
	private static final int WATERFALL_HEIGHT = 128;
	private static final int RULER_HEIGHT = 20;
	private final Color FREQUENCY_COLOR = Color.green;
	private final Color HIGHLIGHT_FREQUENCY_COLOR = Color.magenta;
	private final Color FREQUENCY_SCALE_COLOR = Color.black;
	private Palette[] palette;
	private Color[] colorTable = new Color[256];
	private int signalFrequencyInHertz = -1;
	private int highlightFrequencyInHertz = -1;
	private WaterfallMouseListener waterfallMouseListener;
	private WaterfallMouseMotionListener waterfallMouseMotionListener;
	private double startFrequency = 0.0;
	private double bandwidthInHertz;
	private int halfBandwidthInPixels;
	private Controller controller = null;
	private boolean lsb = false;
	public double carrierfreq = 0;
	private Image offScreenImage;
	private Graphics offScreenGraphics;
	private final static boolean bwPalette = false;

	Waterfall(Controller controller) {
		super();
		this.controller = controller;
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
		if (!isDisplayable()) {
			throw new RuntimeException("component is not displayable");
		}
		initColorTable();
	}

	void readPalette() {
		palette = new Palette[9];
		palette[0] = new Palette(0, 0, 0);
		palette[1] = new Palette(0, 0, 62);
		palette[2] = new Palette(0, 0, 126);
		palette[3] = new Palette(0, 0, 214);
		palette[4] = new Palette(145, 142, 96);
		palette[5] = new Palette(181, 184, 48);
		palette[6] = new Palette(223, 226, 105);
		palette[7] = new Palette(254, 254, 4);
		palette[8] = new Palette(255, 58, 0);
	}

	void initColorTable() {
		if (bwPalette) {
			for (int i = 0; i < 256; i++) {
				int di = (int) (Math.sqrt((double) i / 256.0) * 256);
				colorTable[i] = new Color(di, di, di);
			}

		} else {
			readPalette();
			for (int n = 0; n < 8; n++) {
				for (int i = 0; i < 32; i++) {
					int r = palette[n].r + (int) (1.0 * i * (palette[n + 1].r - palette[n].r) / 32.0);
					int g = palette[n].g + (int) (1.0 * i * (palette[n + 1].g - palette[n].g) / 32.0);
					int b = palette[n].b + (int) (1.0 * i * (palette[n + 1].b - palette[n].b) / 32.0);
					colorTable[i + 32 * n] = new Color(r, g, b);
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
		screen.drawImage(offScreenImage, 0, RULER_HEIGHT, this);
		if (highlightFrequencyInHertz > 0 && highlightFrequencyInHertz != signalFrequencyInHertz) {
			screen.setColor(HIGHLIGHT_FREQUENCY_COLOR);
			int leftBandwidthEdge = (int) (highlightFrequencyInHertz / FREQUENCY_TO_PIXELS_SCALE_FACTOR - halfBandwidthInPixels);
			int rightBandwidthEdge = (int) (highlightFrequencyInHertz / FREQUENCY_TO_PIXELS_SCALE_FACTOR + halfBandwidthInPixels);
			screen.drawLine(leftBandwidthEdge, RULER_HEIGHT, leftBandwidthEdge, WATERFALL_HEIGHT + RULER_HEIGHT);
			screen.drawLine(rightBandwidthEdge, RULER_HEIGHT, rightBandwidthEdge, WATERFALL_HEIGHT + RULER_HEIGHT);
		}
		if (signalFrequencyInHertz >= 0) {
			screen.setColor(FREQUENCY_COLOR);
			int leftBandwidthEdge = (int) (signalFrequencyInHertz / FREQUENCY_TO_PIXELS_SCALE_FACTOR - halfBandwidthInPixels);
			int rightBandwidthEdge = (int) (signalFrequencyInHertz / FREQUENCY_TO_PIXELS_SCALE_FACTOR + halfBandwidthInPixels);

			screen.drawLine(leftBandwidthEdge, RULER_HEIGHT, leftBandwidthEdge, WATERFALL_HEIGHT + RULER_HEIGHT);
			screen.drawLine(rightBandwidthEdge, RULER_HEIGHT, rightBandwidthEdge, WATERFALL_HEIGHT + RULER_HEIGHT);
		}

		drawFrequencyScale(screen);
	}

	void setBandwidthInHz(double hz) {
		bandwidthInHertz = hz;
		halfBandwidthInPixels = (int) (((bandwidthInHertz / FREQUENCY_TO_PIXELS_SCALE_FACTOR) / 2.0) + 0.5);
	}

	boolean handleSpectrum(int frame, double[] data, int length) throws IOException {
		scroll();

		// System.out.println("Waterfall/handleSpectrum ");
		// for (int i = 0 ; i < 64 ; i++) {
		// System.out.print("|" + data[i]);
		// }
		// System.out.println("\n");

		for (int x = 0; x < length; x++) {
			int b = (int) (data[x] * 255);
			plot(x, 0, b < 0 ? b + 256 : b);
		}
		invalidate();
		validate();
		repaint();
		return true;
	}

	private void scroll() {
		if (offScreenGraphics != null) {
			offScreenGraphics.copyArea(0, 0, WIDTH, WATERFALL_HEIGHT - 1, 0, 1);
		}
	}

	private void plot(int x, int y, int color) {
		if (offScreenGraphics != null) {
			offScreenGraphics.setColor(colorTable[color]);
			offScreenGraphics.fillRect(x, y, 1, 1);
		}
	}

	/**
	 * Change audio frequency
	 */
	public void setReceiveFrequency(int frequencyInHertz) {
		signalFrequencyInHertz = frequencyInHertz;
	}

	// Called when the mouse moves into the waterfall but isn't clicked
	public void setHighlightFrequency(int frequencyInHertz) {
		highlightFrequencyInHertz = frequencyInHertz;
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

		public void mouseMoved(MouseEvent event) {
			waterfall.setHighlightFrequency((int) (event.getX() * FREQUENCY_TO_PIXELS_SCALE_FACTOR));
		}

	}

	class WaterfallMouseListener extends MouseAdapter {
		Waterfall waterfall;

		WaterfallMouseListener(Waterfall waterfall) {
			super();
			this.waterfall = waterfall;
		}

		public void mouseClicked(MouseEvent event) {
			if (event.isShiftDown()) {
				controller.setTransmitterFrequency((double) (event.getX() * FREQUENCY_TO_PIXELS_SCALE_FACTOR));
			} else {
				controller.setReceiverFrequency((double) (event.getX() * FREQUENCY_TO_PIXELS_SCALE_FACTOR));
			}
		}

		public void mouseExited(MouseEvent event) {
			waterfall.clearHighlightFrequency();
		}
	}

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
			ifreq = (int) (100 * Math.floor(realFreq / 100.0 + 0.5));

			if (ifreq < realFreq || ifreq >= realFreq + FREQUENCY_TO_PIXELS_SCALE_FACTOR) {
				f += FREQUENCY_TO_PIXELS_SCALE_FACTOR;
				continue;
			}

			p = new Tic();
			p.major = false;
			p.freq = ifreq;
			p.x = i;

			if ((ifreq % 500) == 0) {

				int khz = ifreq / 1000;
				int hz = ifreq % 1000;

				if (khz > 9)
					p.str = khz + "." + hz;
				else
					p.str = "" + ifreq;
				p.strw = fontMetrics.stringWidth(p.str);
				p.strh = fontHeight;
				p.strx = clamp(i - p.strw / 2, 0, width - p.strw);

				p.major = true;
			}

			f += FREQUENCY_TO_PIXELS_SCALE_FACTOR;

			p.next = list;
			list = p;
		}
		return list;
	}

	private int clamp(int x, int low, int high) {
		return (((x) > (high)) ? (high) : (((x) < (low)) ? (low) : (x)));
	}

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
