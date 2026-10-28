package com.miranoserqu.miras_space;

import com.miranoserqu.miras_space.client.ClientLevel;
import com.miranoserqu.miras_space.event.EventBus;
import com.miranoserqu.miras_space.server.ServerLevel;

public class Game {

    private EventBus serverBus;
    private EventBus clientBus;

    public Game(){
        this.serverBus = new EventBus();
        this.clientBus = new EventBus();
    }

    public EventBus getServerBus() {
        return serverBus;
    }

    public EventBus getClientBus() {
        return clientBus;
    }
}
