package driver;

import java.awt.event.ComponentEvent;

import javax.swing.JPanel;

/**
 * DisplayPanel is a Panel derivative useful for displaying a cobweb simulation.
 * It uses an offscreen image to buffer drawing, for flicker-free performance at
 * the cost of memory and perhaps a little speed. Use of DisplayPanel is not
 * required in Cobweb, but it does automate display handling. Future
 * enhancement: implement a ScrollingDisplayPanel for large simulations.
 */
public class DisplayPanel extends JPanel {

	public DisplayPanel(cobweb.UIInterface ui, int borderW, int borderH) {
		theUI = ui;
		borderWidth = borderW;
		borderHeight = borderH;
		addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override
			public void componentResized(java.awt.event.ComponentEvent evt) {
				super.componentResized(evt);
				DisplayPanel.this.setupOffscreen();
			}

			@Override
			public void componentShown(ComponentEvent arg0) {
				super.componentShown(arg0);
				DisplayPanel.this.setupOffscreen();
			}

		});

	}

	public void setUI(cobweb.UIInterface ui) {
		theUI = ui;
		setupOffscreen();
		addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override
			public void componentResized(java.awt.event.ComponentEvent evt) {
				super.componentResized(evt);
				DisplayPanel.this.setupOffscreen();
			}

			@Override
			public void componentShown(ComponentEvent arg0) {
				super.componentShown(arg0);
				DisplayPanel.this.setupOffscreen();
			}
		});

	}

	@Override
	public void paint(java.awt.Graphics g) {
		update(g);
	}

	@Override
	public void update(java.awt.Graphics g) {
		if (offscreenImage == null || tileWidth == 0 || mapWidth != getWidthInTiles()
				|| mapHeight != getHeightInTiles() || tileHeight == 0) {
			setupOffscreen();
		}
		theUI.draw(offscreenGraphics, tileWidth, tileHeight);
		offscreenGraphics.setColor(java.awt.Color.white);
		offscreenGraphics.fillRect(0, -fontHeight, getSize().width - 2
				* borderWidth, fontHeight);
		offscreenGraphics.setColor(java.awt.Color.black);
		offscreenGraphics.drawString((new Long(theUI.getTime())).toString(), 0,
				fontDrawHeight);
		g.drawImage(offscreenImage, 0, 0, null);
	}


	void setupOffscreen() {
		offscreenGraphics = null;
		offscreenImage = null;
		java.awt.Dimension size = getSize();
		if (size.width <= 0 || size.height <= 0) {
			return;
		}
		offscreenImage = createImage(size.width, size.height);
		offscreenGraphics = offscreenImage.getGraphics();
		offscreenGraphics.setColor(java.awt.Color.white);
		offscreenGraphics.fillRect(0, 0, size.width, size.height);
		java.awt.FontMetrics m = offscreenGraphics.getFontMetrics();
		fontDrawHeight = -1 * (m.getMaxDescent() + m.getLeading());
		fontHeight = m.getMaxAscent() + m.getMaxDescent() + m.getLeading();
		offscreenGraphics.translate(borderWidth, borderHeight + fontHeight);
		size.width -= 2 * borderWidth;
		size.height -= (2 * borderHeight) + fontHeight;

		mapWidth = theUI.getWidth();
		mapHeight = theUI.getHeight();
		if (mapWidth != 0) {
			tileWidth = size.width / mapWidth;
		}
		if (mapHeight != 0) {
			tileHeight = size.height / mapHeight;
		}
	}

	/*
	 * the following information needed to convert the mouse coords into the
	 * grid coordinates
	 */
	public int getWidthInTiles() {
		return theUI.getWidth();
	}

	public int getHeightInTiles() {
		return theUI.getHeight();
	}

	public int getTileW() {
		return tileWidth;
	}

	public int getTileH() {
		return tileHeight;
	}

	public int getfontDH() {
		return fontDrawHeight;
	}

	public int getfontHeight() {
		return fontHeight;
	}

	public int getBorderHeight() {
		return borderHeight;
	}

	public int getBorderWidth() {
		return borderWidth;
	}

	private java.awt.Image offscreenImage;

	private java.awt.Graphics offscreenGraphics;

	private int tileWidth = 0;

	private int tileHeight = 0;

	private int fontDrawHeight = 0;

	private int fontHeight = 0;

	private int borderWidth = 0;

	private int borderHeight = 0;

	private int mapWidth = 0;

	private int mapHeight = 0;

	cobweb.UIInterface theUI;

	public static final long serialVersionUID = 0x09FE6158DCF2CA3BL;
}
