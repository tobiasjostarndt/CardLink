package com.appdinx.cardlink.egk;

import org.greenrobot.eventbus.Subscribe;

import java.util.logging.Level;
import java.util.logging.Logger;

import de.gematik.ti.cardreader.provider.api.events.card.CardAbsentEvent;

import com.appdinx.cardlink.CardLinkCardHandler;
import com.appdinx.cardlink.artemis.ClientManager;
import com.appdinx.cardlink.util.DeviceUtils;

public class CardAbsentEventSubscriber {

    private static final Logger log = Logger.getLogger(CardAbsentEventSubscriber.class.getName());

    private final CardLinkCardHandler context;

    public CardAbsentEventSubscriber(CardLinkCardHandler context) {
        this.context = context;
    }

    @Subscribe
    public void onCardAbsentEvent(CardAbsentEvent event) {
        try {
            ClientManager clientManager = ClientManager.getInstance();
            clientManager.sendCardRemovedEvent(DeviceUtils.getDeviceId(context));
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error communicating with the server", e);
        }

    }
}
