package org.apache;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.protocol.ws.IWebSocketSettings;
import org.apache.wicket.protocol.ws.api.WebSocketBehavior;
import org.apache.wicket.protocol.ws.api.WebSocketPushBroadcaster;
import org.apache.wicket.protocol.ws.api.event.WebSocketPushPayload;
import org.apache.wicket.protocol.ws.api.message.ClosedMessage;
import org.apache.wicket.protocol.ws.api.message.ConnectedMessage;
import org.apache.wicket.protocol.ws.api.message.IWebSocketPushMessage;
import org.apache.wicket.protocol.ws.api.registry.IWebSocketConnectionRegistry;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.jquery.ui.calendar.Calendar;
import com.googlecode.wicket.jquery.ui.calendar.CalendarEvent;
import com.googlecode.wicket.jquery.ui.calendar.CalendarModel;
import com.googlecode.wicket.jquery.ui.form.button.Button;
import com.googlecode.wicket.jquery.ui.panel.JQueryFeedbackPanel;

public class HomePage extends WebPage {
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(HomePage.class);
	private final List<CalendarEvent> events;

	private final Button btn = new Button("button");
	private final AbstractDefaultAjaxBehavior aab = new AbstractDefaultAjaxBehavior() {
		private static final long serialVersionUID = 1L;

		@Override
		protected void respond(AjaxRequestTarget target) {
			IWebSocketConnectionRegistry reg = IWebSocketSettings.Holder.get(getApplication()).getConnectionRegistry();
			new WebSocketPushBroadcaster(reg).broadcastAll(getApplication(), new IWebSocketPushMessage() {});
		}
	};

	@Override
	public void onEvent(IEvent<?> event) {
		super.onEvent(event);

		if (event.getPayload() instanceof WebSocketPushPayload) {
			WebSocketPushPayload wsEvent = (WebSocketPushPayload) event.getPayload();
			wsEvent.getHandler().add(btn.setVisible(true));
			wsEvent.getHandler().appendJavaScript("console.log('javascript executed');");
		}
	}

	public HomePage(final PageParameters parameters) {
		super(parameters);

		add(new Label("version", getApplication().getFrameworkSettings().getVersion()));
		add(new Button("button0"));

		add(aab, new WebSocketBehavior() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConnect(ConnectedMessage message) {
				super.onConnect(message);
				log.info(String.format("WebSocketBehavior::onConnect [session: %s, key: %s]", message.getSessionId(), message.getKey()));
			}

			@Override
			protected void onClose(ClosedMessage message) {
				super.onClose(message);
				log.info(String.format("WebSocketBehavior::onClose [session: %s, key: %s]", message.getSessionId(), message.getKey()));
			}
		});

		add(btn.setOutputMarkupPlaceholderTag(true).setVisible(false));
		
		events = new ArrayList<CalendarEvent>();
		java.util.Calendar cs = java.util.Calendar.getInstance();
		cs.set(2014, 8, 20, 10, 0);
		java.util.Calendar ce = (java.util.Calendar)cs.clone();
		ce.add(java.util.Calendar.HOUR_OF_DAY, 1);
		for (int i = 0; i < 10; ++i) {
			CalendarEvent e = new CalendarEvent(i, "Event " + i, cs.getTime(), ce.getTime());
			e.setAllDay(false);
			events.add(e);
			cs.add(java.util.Calendar.HOUR_OF_DAY, 25);
			ce.add(java.util.Calendar.HOUR_OF_DAY, 25);
		}

		// FeedbackPanel //
		final FeedbackPanel feedback = new JQueryFeedbackPanel("feedback");
		add(feedback.setOutputMarkupId(true));

		// Calendar //
		add(new Calendar("calendar", this.newCalendarModel(), new Options("theme", true).set("header", "{ left: 'title', right: 'month,agendaWeek,agendaDay, today, prev,next' }")) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isSelectable() {
				return true;
			}

			@Override
			public boolean isDayClickEnabled() {
				return true;
			}

			@Override
			public boolean isEventClickEnabled() {
				return true;
			}

			@Override
			public boolean isEventDropEnabled() {
				return true;
			}

			@Override
			public boolean isEventResizeEnabled() {
				return true;
			}
			
			@Override
			public void onEventDrop(AjaxRequestTarget target, int eventId, long delta, boolean allDay) {
				if (eventId > 3 && eventId < 7) {
					warn(String.format("Editing of event with ID %s is denied", eventId));
					target.add(feedback);
					return;
				}
				CalendarEvent event = events.get(eventId);

				if (event != null) {
					event.setStart(event.getStart() != null ? new Date(event.getStart().getTime() + delta) : null);
					event.setEnd(event.getEnd() != null ? new Date(event.getEnd().getTime() + delta) : null);
					event.setAllDay(allDay);

					info(String.format("%s changed to %s", event.getTitle(), event.getStart()));
					target.add(feedback);
				}
			}

			@Override
			public void onEventResize(AjaxRequestTarget target, int eventId, long delta) {
				if (eventId > 3 && eventId < 7) {
					warn(String.format("Editing of event with ID %s is denied", eventId));
					target.add(feedback);
					return;
				}
				CalendarEvent event = events.get(eventId);

				if (event != null) {
					Date date = event.getEnd() == null ? event.getStart() : event.getEnd();
					event.setEnd(new Date(date.getTime() + delta));

					this.info(String.format("%s now ends the %s", event.getTitle(), event.getEnd()));
					target.add(feedback);
				}
			}
		});
	}

	private CalendarModel newCalendarModel() {
		return new CalendarModel() {

			private static final long serialVersionUID = 1L;

			@Override
			protected List<CalendarEvent> load() {
				return events;
			}
		};
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(OnDomReadyHeaderItem.forScript("window.setTimeout(function() {" + aab.getCallbackScript() + "} , 500);"));
	}
}
