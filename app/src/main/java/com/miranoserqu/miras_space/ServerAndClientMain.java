package com.miranoserqu.miras_space;

import java.io.IOException;

public class ServerAndClientMain {

    public static void main(String[] args) throws IOException, InterruptedException {
        ServerMain.main(args);
        ClientMain.main(args);
    }

}
