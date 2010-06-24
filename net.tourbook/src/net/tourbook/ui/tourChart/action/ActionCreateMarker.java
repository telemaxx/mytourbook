/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.tourChart.action;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartLabel;
import net.tourbook.chart.ChartXSlider;
import net.tourbook.chart.IChartContextProvider;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.tour.DialogMarker;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

public class ActionCreateMarker extends Action {

	/**
	 * 
	 */
	private final IChartContextProvider	_chartContextProvider;
	private boolean						_isLeftSlider;

	private IMarkerReceiver				_markerReceiver;

	public ActionCreateMarker(	final IChartContextProvider chartContextProvider,
								final String text,
								final boolean isLeftSlider) {

		super(text);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__edit_tour_marker_new));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__edit_tour_marker_new_disabled));

		_chartContextProvider = chartContextProvider;
		_isLeftSlider = isLeftSlider;
	}

	/**
	 * Creates a new marker
	 * 
	 * @param tourData
	 * @return
	 */
	private TourMarker createTourMarker(final TourData tourData) {

		final ChartXSlider leftSlider = _chartContextProvider.getLeftSlider();
		final ChartXSlider rightSlider = _chartContextProvider.getRightSlider();

		final ChartXSlider slider = rightSlider == null ? //
				leftSlider
				: _isLeftSlider ? leftSlider : rightSlider;

		if (slider == null || tourData.timeSerie == null) {
			return null;
		}

		final int serieIndex = slider.getValuesIndex();
		final int[] distSerie = tourData.getMetricDistanceSerie();

		// create a new marker
		final TourMarker tourMarker = new TourMarker(tourData, ChartLabel.MARKER_TYPE_CUSTOM);
		tourMarker.setSerieIndex(serieIndex);
		tourMarker.setTime(tourData.timeSerie[serieIndex]);
		tourMarker.setLabel(Messages.TourData_Label_new_marker);
		tourMarker.setVisualPosition(ChartLabel.VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED);

		if (distSerie != null) {
			tourMarker.setDistance(distSerie[serieIndex]);
		}

		return tourMarker;
	}

	@Override
	public void run() {

		final Chart chart = _chartContextProvider.getChart();

		TourData tourData = null;
		final Object tourId = chart.getChartDataModel().getCustomData(TourManager.CUSTOM_DATA_TOUR_ID);
		if (tourId instanceof Long) {
			tourData = TourManager.getInstance().getTourData((Long) tourId);
		}

		if (tourData == null) {
			return;
		}

		final TourMarker newTourMarker = createTourMarker(tourData);
		if (newTourMarker == null) {
			return;
		}

		if (_markerReceiver != null) {
			_markerReceiver.addTourMarker(newTourMarker);

			// the marker dialog will not be opened
			return;
		}

		final DialogMarker markerDialog = new DialogMarker(Display.getCurrent().getActiveShell(), tourData, null);

		markerDialog.create();
		markerDialog.addTourMarker(newTourMarker);

		if (markerDialog.open() == Window.OK) {
			TourManager.saveModifiedTour(tourData);
		}
	}

	public void setMarkerReceiver(final IMarkerReceiver markerReceiver) {
		_markerReceiver = markerReceiver;
	}
}
