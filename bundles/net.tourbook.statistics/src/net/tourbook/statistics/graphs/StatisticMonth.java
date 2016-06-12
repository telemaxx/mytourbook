/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.statistics.graphs;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import net.tourbook.chart.BarChartMinMaxKeeper;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartStatisticSegments;
import net.tourbook.chart.ChartToolTipInfo;
import net.tourbook.chart.ChartType;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.common.UI;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.statistic.StatisticContext;
import net.tourbook.statistic.TourbookStatistic;
import net.tourbook.statistics.Messages;
import net.tourbook.statistics.StatisticServices;
import net.tourbook.ui.TourTypeFilter;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;

public abstract class StatisticMonth extends TourbookStatistic {

	private TourPerson					_activePerson;
	private TourTypeFilter				_activeTourTypeFilter;

	private int							_currentYear;
	private int							_numberOfYears;

	private Chart						_chart;
	private final BarChartMinMaxKeeper	_minMaxKeeper	= new BarChartMinMaxKeeper();

	private boolean						_isSynchScaleEnabled;

	private DateFormat					_dateFormatter	= DateFormat.getDateInstance(DateFormat.FULL);

	private TourData_Month				_tourMonthData;
	private StatisticContext			_statContext;

	private int							_barOrderStart;

	private long[][]					_resortedTypeIds;
	private float[][]					_resortedAltitudeLow;
	private float[][]					_resortedAltitudeHigh;
	private float[][]					_resortedDistanceLow;
	private float[][]					_resortedDistanceHigh;
	private float[][]					_resortedTimeLow;
	private float[][]					_resortedTimeHigh;

	public boolean canTourBeVisible() {
		return false;
	}

	ChartStatisticSegments createChartSegments(final TourData_Month tourMonthData) {

		/*
		 * create segments for each year
		 */
		final int monthCounter = tourMonthData.altitudeHigh[0].length;
		final double segmentStart[] = new double[_numberOfYears];
		final double segmentEnd[] = new double[_numberOfYears];
		final String[] segmentTitle = new String[_numberOfYears];

		final int oldestYear = _currentYear - _numberOfYears + 1;

		// get start/end and title for each segment
		for (int monthIndex = 0; monthIndex < monthCounter; monthIndex++) {

			final int yearIndex = monthIndex / 12;

			if (monthIndex % 12 == 0) {

				// first month in a year
				segmentStart[yearIndex] = monthIndex;
				segmentTitle[yearIndex] = Integer.toString(oldestYear + yearIndex);

			} else if (monthIndex % 12 == 11) {

				// last month in a year
				segmentEnd[yearIndex] = monthIndex;
			}
		}

		final ChartStatisticSegments monthSegments = new ChartStatisticSegments();
		monthSegments.segmentStartValue = segmentStart;
		monthSegments.segmentEndValue = segmentEnd;
		monthSegments.segmentTitle = segmentTitle;

		return monthSegments;
	}

	private double[] createMonthData(final TourData_Month tourMonthData) {

		/*
		 * create segments for each year
		 */
		final int monthCounter = tourMonthData.altitudeHigh[0].length;
		final double[] allMonths = new double[monthCounter];

		// get start/end and title for each segment
		for (int monthIndex = 0; monthIndex < monthCounter; monthIndex++) {
			allMonths[monthIndex] = monthIndex;
		}

		return allMonths;
	}

	@Override
	public void createStatisticUI(	final Composite parent,
									final IViewSite viewSite,
									final IPostSelectionProvider postSelectionProvider) {

		// create chart
		_chart = new Chart(parent, SWT.BORDER | SWT.FLAT);
		_chart.setShowZoomActions(true);
		_chart.setToolBarManager(viewSite.getActionBars().getToolBarManager(), false);
	}

	private ChartToolTipInfo createToolTipInfo(final int serieIndex, final int valueIndex) {

		final int oldestYear = _currentYear - _numberOfYears + 1;

		final Calendar calendar = GregorianCalendar.getInstance();

		calendar.set(oldestYear, 0, 1);
		calendar.add(Calendar.MONTH, valueIndex);

		//
		final StringBuffer monthStringBuffer = new StringBuffer();
		final FieldPosition monthPosition = new FieldPosition(DateFormat.MONTH_FIELD);

		final Date date = new Date();
		date.setTime(calendar.getTimeInMillis());
		_dateFormatter.format(date, monthStringBuffer, monthPosition);

		final Integer recordingTime = _tourMonthData.recordingTime[serieIndex][valueIndex];
		final Integer drivingTime = _tourMonthData.drivingTime[serieIndex][valueIndex];
		final int breakTime = recordingTime - drivingTime;

		/*
		 * tool tip: title
		 */
		final StringBuilder titleString = new StringBuilder();

		final String tourTypeName = StatisticServices.getTourTypeName(serieIndex, _activeTourTypeFilter);
		if (tourTypeName != null && tourTypeName.length() > 0) {
			titleString.append(tourTypeName);
		}

		final String toolTipTitle = String.format(Messages.tourtime_info_date_month, //
				titleString.toString(),
				monthStringBuffer.substring(monthPosition.getBeginIndex(), monthPosition.getEndIndex()),
				calendar.get(Calendar.YEAR)
		//
				)
				.toString();

		/*
		 * tool tip: label
		 */
		final StringBuilder toolTipFormat = new StringBuilder();
		toolTipFormat.append(Messages.tourtime_info_distance_tour);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_altitude);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_recording_time);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_driving_time);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_break_time);

		final String toolTipLabel = String.format(toolTipFormat.toString(), //
				//
				_tourMonthData.distanceHigh[serieIndex][valueIndex] / 1000,
				UI.UNIT_LABEL_DISTANCE,
				//
				(int) _tourMonthData.altitudeHigh[serieIndex][valueIndex],
				UI.UNIT_LABEL_ALTITUDE,
				//
				recordingTime / 3600,
				(recordingTime % 3600) / 60,
				//
				drivingTime / 3600,
				(drivingTime % 3600) / 60,
				//
				breakTime / 3600,
				(breakTime % 3600) / 60
		//
				)
				.toString();

		/*
		 * create tool tip info
		 */

		final ChartToolTipInfo toolTipInfo = new ChartToolTipInfo();
		toolTipInfo.setTitle(toolTipTitle);
		toolTipInfo.setLabel(toolTipLabel);
//		toolTipInfo.setLabel(toolTipFormat.toString());

		return toolTipInfo;
	}

	void createXDataMonths(final ChartDataModel chartDataModel) {

		// set the x-axis
		final ChartDataXSerie xData = new ChartDataXSerie(createMonthData(_tourMonthData));
		xData.setAxisUnit(ChartDataXSerie.X_AXIS_UNIT_MONTH);
		xData.setChartSegments(createChartSegments(_tourMonthData));

		chartDataModel.setXData(xData);
	}

	void createYDataAltitude(final ChartDataModel chartDataModel) {

		// altitude

		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartType.BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				_resortedAltitudeLow,
				_resortedAltitudeHigh);

		yData.setYTitle(Messages.LABEL_GRAPH_ALTITUDE);
		yData.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		yData.setShowYSlider(true);

		StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE);
		StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE, _activeTourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, _resortedTypeIds, _activeTourTypeFilter);

		chartDataModel.addYData(yData);
	}

	void createYDataDistance(final ChartDataModel chartDataModel) {

		// distance

		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartType.BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				_resortedDistanceLow,
				_resortedDistanceHigh);

		yData.setYTitle(Messages.LABEL_GRAPH_DISTANCE);
		yData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		yData.setValueDivisor(1000);
		yData.setShowYSlider(true);

		StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_DISTANCE);
		StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_DISTANCE, _activeTourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, _resortedTypeIds, _activeTourTypeFilter);

		chartDataModel.addYData(yData);
	}

	void createYDataTourTime(final ChartDataModel chartDataModel) {

		// duration

		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartType.BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				_resortedTimeLow,
				_resortedTimeHigh);

		yData.setYTitle(Messages.LABEL_GRAPH_TIME);
		yData.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
		yData.setShowYSlider(true);

		StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_TIME);
		StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_TIME, _activeTourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, _resortedTypeIds, _activeTourTypeFilter);

		chartDataModel.addYData(yData);
	}

	protected abstract String getBarOrderingStateKey();

	abstract ChartDataModel getChartDataModel();

	@Override
	public void preferencesHasChanged() {

		updateStatistic();
	}

	/**
	 * Resort statistic bars according to the sequence start
	 * 
	 * @param statContext
	 */
	private void reorderStatData() {

		final int barLength = _tourMonthData.altitudeHigh.length;

		_resortedTypeIds = new long[barLength][];

		_resortedAltitudeLow = new float[barLength][];
		_resortedAltitudeHigh = new float[barLength][];
		_resortedDistanceLow = new float[barLength][];
		_resortedDistanceHigh = new float[barLength][];
		_resortedTimeLow = new float[barLength][];
		_resortedTimeHigh = new float[barLength][];

		if (_statContext.outBarNames == null) {

			// there are no data available, create dummy data that the UI do not fail

			_resortedTypeIds = new long[1][1];

			_resortedAltitudeLow = new float[1][1];
			_resortedAltitudeHigh = new float[1][1];
			_resortedDistanceLow = new float[1][1];
			_resortedDistanceHigh = new float[1][1];
			_resortedTimeLow = new float[1][1];
			_resortedTimeHigh = new float[1][1];

			return;
		}

		int resortedIndex = 0;

		final long[][] typeIds = _tourMonthData.typeIds;

		final float[][] altitudeLowValues = _tourMonthData.altitudeLow;
		final float[][] altitudeHighValues = _tourMonthData.altitudeHigh;
		final float[][] distanceLowValues = _tourMonthData.distanceLow;
		final float[][] distanceHighValues = _tourMonthData.distanceHigh;
		final float[][] timeLowValues = _tourMonthData.getDurationTimeLowFloat();
		final float[][] timeHighValues = _tourMonthData.getDurationTimeHighFloat();

		if (_barOrderStart >= barLength) {

			final int barOrderStart = _barOrderStart % barLength;

			// set types starting from the sequence start
			for (int serieIndex = barOrderStart; serieIndex >= 0; serieIndex--) {

				_resortedTypeIds[resortedIndex] = typeIds[serieIndex];

				_resortedAltitudeLow[resortedIndex] = altitudeLowValues[serieIndex];
				_resortedAltitudeHigh[resortedIndex] = altitudeHighValues[serieIndex];
				_resortedDistanceLow[resortedIndex] = distanceLowValues[serieIndex];
				_resortedDistanceHigh[resortedIndex] = distanceHighValues[serieIndex];
				_resortedTimeLow[resortedIndex] = timeLowValues[serieIndex];
				_resortedTimeHigh[resortedIndex] = timeHighValues[serieIndex];

				resortedIndex++;
			}

			// set types starting from the last
			for (int serieIndex = barLength - 1; resortedIndex < barLength; serieIndex--) {

				_resortedTypeIds[resortedIndex] = typeIds[serieIndex];

				_resortedAltitudeLow[resortedIndex] = altitudeLowValues[serieIndex];
				_resortedAltitudeHigh[resortedIndex] = altitudeHighValues[serieIndex];
				_resortedDistanceLow[resortedIndex] = distanceLowValues[serieIndex];
				_resortedDistanceHigh[resortedIndex] = distanceHighValues[serieIndex];
				_resortedTimeLow[resortedIndex] = timeLowValues[serieIndex];
				_resortedTimeHigh[resortedIndex] = timeHighValues[serieIndex];

				resortedIndex++;
			}

		} else {

			final int barOrderStart = _barOrderStart;

			// set types starting from the sequence start
			for (int serieIndex = barOrderStart; serieIndex < barLength; serieIndex++) {

				_resortedTypeIds[resortedIndex] = typeIds[serieIndex];

				_resortedAltitudeLow[resortedIndex] = altitudeLowValues[serieIndex];
				_resortedAltitudeHigh[resortedIndex] = altitudeHighValues[serieIndex];
				_resortedDistanceLow[resortedIndex] = distanceLowValues[serieIndex];
				_resortedDistanceHigh[resortedIndex] = distanceHighValues[serieIndex];
				_resortedTimeLow[resortedIndex] = timeLowValues[serieIndex];
				_resortedTimeHigh[resortedIndex] = timeHighValues[serieIndex];

				resortedIndex++;
			}

			// set types starting from 0
			for (int serieIndex = 0; resortedIndex < barLength; serieIndex++) {

				_resortedTypeIds[resortedIndex] = typeIds[serieIndex];

				_resortedAltitudeLow[resortedIndex] = altitudeLowValues[serieIndex];
				_resortedAltitudeHigh[resortedIndex] = altitudeHighValues[serieIndex];
				_resortedDistanceLow[resortedIndex] = distanceLowValues[serieIndex];
				_resortedDistanceHigh[resortedIndex] = distanceHighValues[serieIndex];
				_resortedTimeLow[resortedIndex] = timeLowValues[serieIndex];
				_resortedTimeHigh[resortedIndex] = timeHighValues[serieIndex];

				resortedIndex++;
			}
		}
	}

	@Override
	public void restoreStateEarly(final IDialogSettings state) {

		_barOrderStart = Util.getStateInt(state, getBarOrderingStateKey(), 0);
	}

	@Override
	public void saveState(final IDialogSettings state) {

		state.put(getBarOrderingStateKey(), _barOrderStart);
	}

	@Override
	public void setBarVerticalOrder(final int selectedIndex) {

		// selected index can be -1 when tour type combobox is empty
		_barOrderStart = selectedIndex < 0 ? 0 : selectedIndex;

		final ArrayList<TourType> tourTypes = TourDatabase.getActiveTourTypes();

		if (tourTypes == null || tourTypes.size() == 0) {
			return;
		}

		reorderStatData();

		updateStatistic();
	}

	private void setChartProviders(final ChartDataModel chartModel) {

		// set tool tip info
		chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {
			@Override
			public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {
				return createToolTipInfo(serieIndex, valueIndex);
			}
		});
	}

	@Override
	public void setSynchScale(final boolean isSynchScaleEnabled) {

		if (!isSynchScaleEnabled) {

			// reset when it's disabled

			_minMaxKeeper.resetMinMax();
		}

		_isSynchScaleEnabled = isSynchScaleEnabled;
	}

	/**
	 * Set bar names into the statistic context. The names will be displayed in a combobox in the
	 * statistics toolbar.
	 * 
	 * @param statContext
	 * @param usedTourTypeIds
	 */
	private void setupBars_20_BarNames(final StatisticContext statContext, final long[] usedTourTypeIds) {

		int numUsedTypes = 0;

		// get number of used tour types, a used tour type is != -1
		for (final long tourTypeId : usedTourTypeIds) {
			if (tourTypeId != -1) {
				numUsedTypes++;
			}
		}

		final ArrayList<TourType> tourTypes = TourDatabase.getActiveTourTypes();

		if (tourTypes == null || tourTypes.size() == 0 || numUsedTypes == 0) {

			statContext.outIsUpdateBarNames = true;
			statContext.outBarNames = null;

			return;
		}

		int barIndex = 0;

		// create bar names 2 times
		final String[] barNames = new String[numUsedTypes * 2];

		for (int inverseIndex = 0; inverseIndex < 2; inverseIndex++) {

			for (int typeIndex = 0; typeIndex < tourTypes.size(); typeIndex++) {

				final TourType tourType = tourTypes.get(typeIndex);

				final long tourTypeId = tourType.getTypeId();

				/*
				 * Check if this type is used
				 */
				boolean isTourTypeUsed = false;
				for (final long usedType : usedTourTypeIds) {

					if (usedType == tourTypeId) {
						isTourTypeUsed = true;
						break;
					}
				}

				if (isTourTypeUsed) {

					String barName;

					if (inverseIndex == 0) {
						barName = tourType.getName();
					} else {
						barName = tourType.getName() + UI.SPACE + Messages.Statistic_Label_Invers;
					}

					barNames[barIndex++] = barName;
				}
			}
		}

		// set state what the statistic container should do
		statContext.outIsUpdateBarNames = true;
		statContext.outBarNames = barNames;
		statContext.outVerticalBarIndex = _barOrderStart;
	}

	private void updateStatistic() {

		updateStatistic(new StatisticContext(_activePerson, _activeTourTypeFilter, _currentYear, _numberOfYears));
	}

	@Override
	public void updateStatistic(final StatisticContext statContext) {

		_statContext = statContext;

		// this statistic supports bar reordering
		statContext.outIsBarReorderingSupported = true;

		_activePerson = statContext.appPerson;
		_activeTourTypeFilter = statContext.appTourTypeFilter;
		_currentYear = statContext.statYoungestYear;
		_numberOfYears = statContext.statNumberOfYears;

		_tourMonthData = DataProvider_Tour_Month.getInstance().getMonthData(
				statContext.appPerson,
				statContext.appTourTypeFilter,
				statContext.statYoungestYear,
				statContext.statNumberOfYears,
				isDataDirtyWithReset() || statContext.isRefreshData);

		setupBars_20_BarNames(statContext, _tourMonthData.usedTourTypeIds);
		reorderStatData();

		// reset min/max values
		if (_isSynchScaleEnabled == false && statContext.isRefreshData) {
			_minMaxKeeper.resetMinMax();
		}

		final ChartDataModel chartDataModel = getChartDataModel();

		setChartProviders(chartDataModel);

		if (_isSynchScaleEnabled) {
			_minMaxKeeper.setMinMaxValues(chartDataModel);
		}

		StatisticServices.updateChartProperties(_chart);

		// show the fDataModel in the chart
		_chart.updateChart(chartDataModel, true);
	}

	@Override
	public void updateToolBar() {
		_chart.fillToolbar(true);
	}
}
