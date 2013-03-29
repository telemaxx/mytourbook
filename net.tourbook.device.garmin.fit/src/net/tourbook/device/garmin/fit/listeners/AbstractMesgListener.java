package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.device.garmin.fit.FitActivityContext;
import net.tourbook.device.garmin.fit.FitActivityContextData;

import com.garmin.fit.Mesg;

/**
 * The super class for all message listeners, provides access to the {@link FitActivityContext}.
 * 
 * @author Marcin Kuthan <marcin.kuthan@gmail.com>
 */
public abstract class AbstractMesgListener {

	private static final Integer		DEFAULT_MESSAGE_INDEX	= Integer.valueOf(0);

	protected final FitActivityContext	context;

	public AbstractMesgListener(final FitActivityContext context) {
		this.context = context;
	}

	protected FitActivityContextData getContextData() {
		return context.getContextData();
	}

	protected Integer getLapMessageIndex(final Mesg mesg) {
		return mesg.getFieldIntegerValue(254);
	}

	protected Integer getMessageIndex(final Mesg mesg) {
		final Integer messageIndex = mesg.getFieldIntegerValue(254);
		return messageIndex != null ? messageIndex : DEFAULT_MESSAGE_INDEX;
	}

	protected TimeData getTimeData() {
		return getContextData().getCurrentTimeData();
	}

	protected TourData getTourData() {
		return getContextData().getCurrentTourData();
	}

	protected TourMarker getTourMarker() {
		return getContextData().getCurrentTourMarker();
	}

}
