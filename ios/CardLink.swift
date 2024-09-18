@objc(CardLink) class CardLink: CDVPlugin {

    @objc(establishWSS:)
    func establishWSS(command: CDVInvokedUrlCommand) {
        var pluginResult: CDVPluginResult? = nil
        
        // Überprüfen, ob der erste Parameter eine gültige URL ist
        if let wssURL = command.arguments.first as? String, !wssURL.isEmpty {
            // Verbindung mit der URL herstellen
            //self.webSocketClientManager.connectToURL(wssURL)
            
            // Überprüfen, ob die Verbindung erfolgreich hergestellt wurde
            //if self.webSocketClientManager.isConnected {
                //pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "true")
            //} else {
                //pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR)
            //}

            pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: wssURL)
        } else {
            pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR)
        }

        // Das Ergebnis an den Cordova-Callback zurückgeben
        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }
    
    // WebSocketClientManager-Instanz bereitstellen
    private var webSocketClientManager = WebSocketClientManager()
}
