package com.appdinx.cardlink.artemis;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Broker {
    static String brokerUrl; // Replace with your broker URL
    static String brokerName;
    static Pattern brokerNamePattern = Pattern.compile(".*//([^:]*).*");

    public static void setBrokerUrl(String brokerUrl) {
        Broker.brokerUrl = brokerUrl;

        Matcher m = brokerNamePattern.matcher(getBrokerUrl());
        if(m.matches()) {
            brokerName = m.group(1);
        }

    }

    public static String getBrokerUrl() {
        return brokerUrl;
    }

    public static String getBrokerName() {
        return brokerName;
    }
}
