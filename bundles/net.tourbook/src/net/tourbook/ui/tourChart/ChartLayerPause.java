/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/

package net.tourbook.ui.tourChart;

import net.tourbook.chart.Chart;
import net.tourbook.chart.GraphDrawingData;
import net.tourbook.chart.IChartLayer;
import net.tourbook.chart.IChartOverlay;
import net.tourbook.photo.ILoadCallBack;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class ChartLayerPause implements IChartLayer, IChartOverlay {

   private int              LABEL_OFFSET;
   private int              PAUSE_POINT_SIZE;

   private TourChart        _tourChart;
   private ChartPauseConfig _cpc;

   private int              _devXPause;
   private int              _devYPause;

   public class LoadImageCallback implements ILoadCallBack {

      @Override
      public void callBackImageIsLoaded(final boolean isImageLoaded) {

         if (isImageLoaded == false) {
            return;
         }

         // run in UI thread
         Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {

               // ensure chart is still displayed
               if (_tourChart.getShell().isDisposed()) {
                  return;
               }

               // paint image
               _tourChart.redrawLayer();
            }
         });
      }
   }

   public ChartLayerPause(final TourChart tourChart) {

      _tourChart = tourChart;
   }

   /**
    * Adjust label to the requested position
    *
    * @param chartLabel
    * @param devYTop
    * @param devYBottom
    * @param labelWidth
    * @param labelHeight
    */
   private void adjustLabelPosition(final ChartLabel chartLabel,
                                    final int devYTop,
                                    final int devYBottom,
                                    final int labelWidth,
                                    final int labelHeight) {

      final int pausePointSize2 = PAUSE_POINT_SIZE / 2 + 0;

      //LABEL_POS_HORIZONTAL_ABOVE_GRAPH_CENTERED:
      _devXPause -= labelWidth / 2;
      _devYPause -= labelHeight + LABEL_OFFSET + pausePointSize2;
   }

   /**
    * This paints the pause(s) for the current graph configuration.
    */
   @Override
   public void draw(final GC gc, final GraphDrawingData drawingData, final Chart chart, final PixelConverter pc) {

      final Device display = gc.getDevice();

      PAUSE_POINT_SIZE = pc.convertVerticalDLUsToPixels(2);
      LABEL_OFFSET = pc.convertVerticalDLUsToPixels(2);

      final int pausePointSize2 = PAUSE_POINT_SIZE / 2;

      final int devYTop = drawingData.getDevYTop();
      final int devYBottom = drawingData.getDevYBottom();
      final long devVirtualGraphImageOffset = chart.getXXDevViewPortLeftBorder();
      final int devGraphHeight = drawingData.devGraphHeight;
      final long devVirtualGraphWidth = drawingData.devVirtualGraphWidth;
      final int devVisibleChartWidth = drawingData.getChartDrawingData().devVisibleChartWidth;
      final boolean isGraphZoomed = devVirtualGraphWidth != devVisibleChartWidth;

      final float graphYBottom = drawingData.getGraphYBottom();
      final float[] yValues = drawingData.getYData().getHighValuesFloat()[0];
      final double scaleX = drawingData.getScaleX();
      final double scaleY = drawingData.getScaleY();

      final Color colorDefault = new Color(display, new RGB(0x60, 0x60, 0x60));
      final Color colorDevice = new Color(display, new RGB(0xff, 0x0, 0x80));
      final Color colorHidden = new Color(display, new RGB(0x24, 0x9C, 0xFF));

      final ValueOverlapChecker overlapChecker = new ValueOverlapChecker(2);

      gc.setClipping(0, devYTop, gc.getClipping().width, devGraphHeight);

      /*
       * Draw pause point and label
       */
      for (final ChartLabel chartLabel : _cpc.chartLabels) {

         final float yValue = yValues[chartLabel.serieIndex];
         final int devYGraph = (int) ((yValue - graphYBottom) * scaleY) - 0;

         final double virtualXPos = chartLabel.graphX * scaleX;
         _devXPause = (int) (virtualXPos - devVirtualGraphImageOffset);
         _devYPause = devYBottom - devYGraph;

         final Point labelExtend = gc.textExtent(chartLabel.pauseDuration);

         /*
          * Get pause point top/left position
          */
         final int devXPauseTopLeft = _devXPause - pausePointSize2;
         final int devYPauseTopLeft = _devYPause - pausePointSize2;

         /*
          * Draw pause point
          */
         gc.setBackground(colorDefault);

         // draw pause point
         gc.fillRectangle(devXPauseTopLeft, devYPauseTopLeft, PAUSE_POINT_SIZE, PAUSE_POINT_SIZE);

         /*
          * Draw pause label
          */

         gc.setForeground(colorDefault);

         final int labelWidth = labelExtend.x;
         final int labelHeight = labelExtend.y;

         adjustLabelPosition(chartLabel, devYTop, devYBottom, labelWidth, labelHeight);

         // add an additional offset which is defined for all pauses in the pause properties slideout
         _devXPause += chartLabel.labelXOffset;
         _devYPause -= chartLabel.labelYOffset;

         /*
          * label is horizontal
          */

         // don't draw the pause to the left of the chart
         if (devVirtualGraphImageOffset == 0 && _devXPause < 0) {
            _devXPause = 0;
         }

         // don't draw the pause to the right of the chart
         final double devPauseRightPos = isGraphZoomed
               ? virtualXPos + labelWidth
               : _devXPause + labelWidth;
         if (devPauseRightPos > devVirtualGraphWidth) {
            _devXPause = (int) (devVirtualGraphWidth - labelWidth - devVirtualGraphImageOffset - 2);
         }

         // force label to be not below the bottom
         if (_devYPause + labelHeight > devYBottom) {
            _devYPause = devYBottom - labelHeight;
         }

         // force label to be not above the top
         if (_devYPause < devYTop) {
            _devYPause = devYTop;
         }

         final String pauseDurationText = chartLabel.pauseDuration;
         final Point textExtent = gc.textExtent(pauseDurationText);
         final int textWidth = textExtent.x;
         final int textHeight = textExtent.y;
         final int borderWidth = 5;
         final int borderWidth2 = 2 * borderWidth;
         final int borderHeight = 0;
         final int borderHeight2 = 2 * borderHeight;
         final int textHeightWithBorder = textHeight + borderHeight2;

         /*
          * Ensure the value text do not overlap, if possible :-)
          */
         final Rectangle textRect = new Rectangle(//
               _devXPause,
               _devYPause,
               textWidth + borderWidth2,
               textHeightWithBorder);

         final Rectangle validRect = overlapChecker.getValidRect(
               textRect,
               true,
               textHeightWithBorder,
               pauseDurationText);

         // don't draw over the graph borders
         if (validRect != null && validRect.y > devYTop && validRect.y + textHeight < devYBottom) {

            // keep current valid rectangle
            overlapChecker.setupNext(validRect, true);

            gc.setAlpha(0xff);

            // draw label
            gc.drawText(pauseDurationText, validRect.x, validRect.y, true);
         }
      }

      colorDefault.dispose();
      colorDevice.dispose();
      colorHidden.dispose();

      gc.setClipping((Rectangle) null);
   }

   /**
    * This is painting the hovered pause.
    * <p>
    * {@inheritDoc}
    */
   @Override
   public void drawOverlay(final GC gc, final GraphDrawingData graphDrawingData) {
      return;
   }

   public void setChartPauseConfig(final ChartPauseConfig chartPauseConfig) {
      _cpc = chartPauseConfig;
   }
}
