package org.apache;

import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.protocol.ws.IWebSocketSettings;
import org.apache.wicket.protocol.ws.api.WebSocketBehavior;
import org.apache.wicket.protocol.ws.api.WebSocketPushBroadcaster;
import org.apache.wicket.protocol.ws.api.event.WebSocketPushPayload;
import org.apache.wicket.protocol.ws.api.message.ClosedMessage;
import org.apache.wicket.protocol.ws.api.message.ConnectedMessage;
import org.apache.wicket.protocol.ws.api.message.IWebSocketPushMessage;
import org.apache.wicket.protocol.ws.api.registry.IWebSocketConnectionRegistry;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.googlecode.wicket.jquery.ui.form.button.Button;

public class HomePage extends WebPage
{
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(HomePage.class);

	private final Button btn = new Button("button");
	private final AbstractDefaultAjaxBehavior aab = new AbstractDefaultAjaxBehavior() {
		private static final long serialVersionUID = 1L;

		@Override
		protected void respond(AjaxRequestTarget target)
		{
			IWebSocketConnectionRegistry reg = IWebSocketSettings.Holder.get(getApplication()).getConnectionRegistry();
			new WebSocketPushBroadcaster(reg).broadcastAll(getApplication(), new IWebSocketPushMessage() {
			});
		}
	};

	@Override
	public void onEvent(IEvent<?> event)
	{
		super.onEvent(event);

		if (event.getPayload() instanceof WebSocketPushPayload)
		{
			WebSocketPushPayload wsEvent = (WebSocketPushPayload) event.getPayload();
			wsEvent.getHandler().add(btn.setVisible(true));
			wsEvent.getHandler().appendJavaScript("console.log('javascript executed');");
		}
	}

	public HomePage(final PageParameters parameters)
	{
		super(parameters);

		add(new Label("version", getApplication().getFrameworkSettings().getVersion()));

		add(aab, new WebSocketBehavior() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConnect(ConnectedMessage message)
			{
				super.onConnect(message);
				log.info(String.format("WebSocketBehavior::onConnect [session: %s, key: %s]", message.getSessionId(), message.getKey()));
			}

			@Override
			protected void onClose(ClosedMessage message)
			{
				super.onClose(message);
				log.info(String.format("WebSocketBehavior::onClose [session: %s, key: %s]", message.getSessionId(), message.getKey()));
			}
		});

		add(btn.setOutputMarkupPlaceholderTag(true).setVisible(false));
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(OnDomReadyHeaderItem.forScript("window.setTimeout(function() {" + aab.getCallbackScript() + "} , 500);"));
	}
}
