package com.miranoserqu.miras_space;

import com.miranoserqu.miras_space.client.ClientLevel;
import com.miranoserqu.miras_space.event.EventBus;
import com.miranoserqu.miras_space.game.Level;
import com.miranoserqu.miras_space.server.ServerLevel;

public class Game {

    private final EventBus serverBus;
    private final EventBus clientBus;
    private Level localClientLevel;
    private Level localServerLevel;

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

    public void registerClient(Level localClientLevel){
        this.localClientLevel = localClientLevel;
        this.clientBus.register(localClientLevel);
    }

    public void registerServer(Level localServerLevel){
        this.localServerLevel = localServerLevel;
        this.serverBus.register(localServerLevel);
    }

    public Level getLocalClientLevel(){
        return this.localClientLevel;
    }

    public Level getLocalServerLevel(){
        return this.localServerLevel;
    }

}
