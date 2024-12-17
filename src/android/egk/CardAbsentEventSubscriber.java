package com.appdinx.cardlink.egk;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;

import java.util.logging.Level;
import java.util.logging.Logger;

import de.gematik.ti.cardreader.provider.api.events.card.CardAbsentEvent;
import com.appdinx.cardlink.artemis.ClientManager;
import com.appdinx.cardlink.activities.CardInsertionActivity;
import com.appdinx.cardlink.util.DeviceUtils;

public class CardAbsentEventSubscriber {

    private static final Logger log = Logger.getLogger(CardAbsentEventSubscriber.class.getName());

    private final CardInsertionActivity context;

    public CardAbsentEventSubscriber(CardInsertionActivity context) {
        this.context = context;
    }

    @Subscribe
    public void onCardAbsentEvent(CardAbsentEvent event) {
        context.setStatusText("Kann Karte nicht an Server senden");
        try {
            ClientManager clientManager = ClientManager.getInstance();
            clientManager.sendCardRemovedEvent(DeviceUtils.getDeviceId(context));
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error communicating with the server", e);
        }

    }
}
