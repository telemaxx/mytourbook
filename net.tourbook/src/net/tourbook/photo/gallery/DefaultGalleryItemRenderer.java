/*******************************************************************************
 * Copyright (c) 2006-2007 Nicolas Richeton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors :
 *    Nicolas Richeton (nicolas.richeton@gmail.com) - initial API and implementation
 *    Richard Michalsky - bugs 195415,  195443
 *******************************************************************************/
package net.tourbook.photo.gallery;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

/**
 * <p>
 * Default item renderer used by the Gallery widget. Supports single line text, image, drop shadows
 * and decorators.
 * </p>
 * <p>
 * Decorator images can be set with {@link GalleryMTItem#setData(String, Object)} by using the
 * following keys :
 * </p>
 * <ul>
 * <li>org.eclipse.nebula.widget.gallery.bottomLeftOverlay</li>
 * <li>org.eclipse.nebula.widget.gallery.bottomRightOverlay</li>
 * <li>org.eclipse.nebula.widget.gallery.topLeftOverlay</li>
 * <li>org.eclipse.nebula.widget.gallery.topRightOverlay</li>
 * </ul>
 * <p>
 * Supported types are org.eclipse.swt.Image for one single decorator and org.eclipse.swt.Image[]
 * for multiple decorators.
 * </p>
 * <p>
 * NOTE: THIS WIDGET AND ITS API ARE STILL UNDER DEVELOPMENT.
 * </p>
 * 
 * @author Nicolas Richeton (nicolas.richeton@gmail.com)
 * @contributor Richard Michalsky (bugs 195415, 195443)
 * @contributor Peter Centgraf (bugs 212071, 212073)
 */
public class DefaultGalleryItemRenderer extends AbstractGalleryItemRenderer {

	/**
	 * Stores colors used in drop shadows
	 */
	protected ArrayList	dropShadowsColors			= new ArrayList();

	// Renderer parameters
	boolean				dropShadows					= false;
	int					dropShadowsSize				= 0;
	int					dropShadowsAlphaStep		= 20;

	Color				foregroundColor;
	Color				backgroundColor;
	Color				selectionForegroundColor;
	Color				selectionBackgroundColor;

	boolean				showLabels					= true;
	boolean				showRoundedSelectionCorners	= true;

	int					selectionRadius				= 15;

	// Vars used during drawing (optimization)
	private boolean		_drawBackground				= false;
	private Color		_drawBackgroundColor		= null;
	private Image		_drawImage					= null;
	private Color		_drawForegroundColor		= null;

	public DefaultGalleryItemRenderer() {

		// Set defaults
		foregroundColor = Display.getDefault().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
		backgroundColor = Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND);

		selectionForegroundColor = Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
		selectionBackgroundColor = Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION);

		// Create drop shadows
		createColors();
	}

	private void createColors() {
		if (dropShadowsSize > 0) {
			final int step = 125 / dropShadowsSize;
			// Create new colors
			for (int i = dropShadowsSize - 1; i >= 0; i--) {
				final int value = 255 - i * step;
				final Color c = new Color(Display.getDefault(), value, value, value);
				dropShadowsColors.add(c);
			}
		}
	}

	@Override
	public void dispose() {
		freeDropShadowsColors();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.nebula.widgets.gallery.AbstractGalleryItemRenderer#draw(org
	 * .eclipse.swt.graphics.GC, org.eclipse.nebula.widgets.gallery.GalleryItem, int, int, int, int,
	 * int)
	 */
	@Override
	public void draw(	final GC gc,
						final GalleryMTItem item,
						final int index,
						final int x,
						final int y,
						final int width,
						final int height) {
		_drawImage = item.getImage();
		_drawForegroundColor = getForeground(item);

		// Set up the GC
		gc.setFont(getFont(item));

		// Create some room for the label.
		int useableHeight = height;
		int fontHeight = 0;
		if (item.getText() != null && !EMPTY_STRING.equals(item.getText()) && this.showLabels) {
			fontHeight = gc.getFontMetrics().getHeight();
			useableHeight -= fontHeight + 2;
		}

		int imageWidth = 0;
		int imageHeight = 0;
		int xShift = 0;
		int yShift = 0;
		Point size = null;

		if (_drawImage != null) {
			final Rectangle itemImageBounds = _drawImage.getBounds();
			imageWidth = itemImageBounds.width;
			imageHeight = itemImageBounds.height;

			size = RendererHelper.getBestSize(
					imageWidth,
					imageHeight,
					width - 8 - 2 * this.dropShadowsSize,
					useableHeight - 8 - 2 * this.dropShadowsSize);

			xShift = RendererHelper.getShift(width, size.x);
			yShift = RendererHelper.getShift(useableHeight, size.y);

			if (dropShadows) {
				Color c = null;
				for (int i = this.dropShadowsSize - 1; i >= 0; i--) {
					c = (Color) dropShadowsColors.get(i);
					gc.setForeground(c);

					gc.drawLine(x + width + i - xShift - 1, y + dropShadowsSize + yShift, x + width + i - xShift - 1, y
							+ useableHeight
							+ i
							- yShift);
					gc.drawLine(x + xShift + dropShadowsSize, y + useableHeight + i - yShift - 1, x
							+ width
							+ i
							- xShift, y - 1 + useableHeight + i - yShift);
				}
			}
		}

		// Draw background (rounded rectangles)

		// Checks if background has to be drawn
		_drawBackground = selected;
		_drawBackgroundColor = null;
		if (!_drawBackground && item.getBackground(true) != null) {
			_drawBackgroundColor = getBackground(item);

			if (!RendererHelper.isColorsEquals(_drawBackgroundColor, galleryBackgroundColor)) {
				_drawBackground = true;
			}
		}

		if (_drawBackground) {
			// Set colors
			if (selected) {
				gc.setBackground(selectionBackgroundColor);
				gc.setForeground(selectionBackgroundColor);
			} else if (_drawBackgroundColor != null) {
				gc.setBackground(_drawBackgroundColor);
			}

			// Draw
			if (showRoundedSelectionCorners) {
				gc.fillRoundRectangle(x, y, width, useableHeight, selectionRadius, selectionRadius);
			} else {
				gc.fillRectangle(x, y, width, height);
			}

			if (item.getText() != null && !EMPTY_STRING.equals(item.getText()) && showLabels) {
				gc.fillRoundRectangle(x, y + height - fontHeight, width, fontHeight, selectionRadius, selectionRadius);
			}
		}

		// Draw image
		if (_drawImage != null && size != null) {
			if (size.x > 0 && size.y > 0) {
				gc.drawImage(_drawImage, 0, 0, imageWidth, imageHeight, x + xShift, y + yShift, size.x, size.y);
				drawAllOverlays(gc, item, x, y, size, xShift, yShift);
			}

		}

		// Draw label
		if (item.getText() != null && !EMPTY_STRING.equals(item.getText()) && showLabels) {
			// Set colors
			if (selected) {
				// Selected : use selection colors.
				gc.setForeground(selectionForegroundColor);
				gc.setBackground(selectionBackgroundColor);
			} else {
				// Not selected, use item values or defaults.

				// Background
				if (_drawBackgroundColor != null) {
					gc.setBackground(_drawBackgroundColor);
				} else {
					gc.setBackground(backgroundColor);
				}

				// Foreground
				if (_drawForegroundColor != null) {
					gc.setForeground(_drawForegroundColor);
				} else {
					gc.setForeground(foregroundColor);
				}
			}

			// Create label
			final String text = RendererHelper.createLabel(item.getText(), gc, width - 10);

			// Center text
			final int textWidth = gc.textExtent(text).x;
			final int textxShift = RendererHelper.getShift(width, textWidth);

			// Draw
			gc.drawText(text, x + textxShift, y + height - fontHeight, true);
		}
	}

	private void freeDropShadowsColors() {
		// Free colors :
		{
			final Iterator i = this.dropShadowsColors.iterator();
			while (i.hasNext()) {
				final Color c = (Color) i.next();
				if (c != null && !c.isDisposed()) {
					c.dispose();
				}
			}
		}
	}

	public Color getBackgroundColor() {
		return backgroundColor;
	}

	public int getDropShadowsSize() {
		return dropShadowsSize;
	}

	/**
	 * Returns the font used for drawing all item labels or <tt>null</tt> if system font is used.
	 * 
	 * @return the font
	 * @see {@link GalleryMT#getFont()} for setting font for a specific GalleryItem.
	 */
	public Font getFont() {
		if (gallery != null) {
			return gallery.getFont();
		}
		return null;
	}

	public Color getForegroundColor() {
		return foregroundColor;
	}

	public Color getSelectionBackgroundColor() {
		return selectionBackgroundColor;
	}

	public Color getSelectionForegroundColor() {
		return selectionForegroundColor;
	}

	public boolean isDropShadows() {
		return dropShadows;
	}

	/**
	 * Returns current label state : enabled or disabled
	 * 
	 * @return true if labels are enabled.
	 * @see DefaultGalleryItemRenderer#setShowLabels(boolean)
	 */
	public boolean isShowLabels() {
		return showLabels;
	}

	public boolean isShowRoundedSelectionCorners() {
		return this.showRoundedSelectionCorners;
	}

	public void setBackgroundColor(final Color backgroundColor) {
		this.backgroundColor = backgroundColor;
	}

	public void setDropShadows(final boolean dropShadows) {
		this.dropShadows = dropShadows;
	}

	public void setDropShadowsSize(final int dropShadowsSize) {
		this.dropShadowsSize = dropShadowsSize;
		this.dropShadowsAlphaStep = (dropShadowsSize == 0) ? 0 : (200 / dropShadowsSize);

		freeDropShadowsColors();
		createColors();
		// TODO: force redraw

	}

	/**
	 * Set the font for drawing all item labels or <tt>null</tt> to use system font.
	 * 
	 * @param font
	 *            the font to set
	 * @see {@link GalleryMT#setFont(Font)} for setting font for a specific GalleryItem.
	 */
	public void setFont(final Font font) {
		if (gallery != null) {
			gallery.setFont(font);
		}
	}

	public void setForegroundColor(final Color foregroundColor) {
		this.foregroundColor = foregroundColor;
	}

	public void setSelectionBackgroundColor(final Color selectionBackgroundColor) {
		this.selectionBackgroundColor = selectionBackgroundColor;
	}

	public void setSelectionForegroundColor(final Color selectionForegroundColor) {
		this.selectionForegroundColor = selectionForegroundColor;
	}

	/**
	 * Enables / disables labels at the bottom of each item.
	 * 
	 * @param showLabels
	 * @see DefaultGalleryItemRenderer#isShowLabels()
	 */
	public void setShowLabels(final boolean showLabels) {
		this.showLabels = showLabels;
	}

	public void setShowRoundedSelectionCorners(final boolean showRoundedSelectionCorners) {
		this.showRoundedSelectionCorners = showRoundedSelectionCorners;
	}
}