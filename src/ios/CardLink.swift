@objc(CardLink) class CardLink: CDVPlugin {
    
    // Verwende lazy var, um die Instanz beim ersten Zugriff zu erstellen
    private lazy var webSocketClientManager = WebSocketClientManager()
    
    @objc(establishWSS:)
    func establishWSS(command: CDVInvokedUrlCommand) {
        var pluginResult: CDVPluginResult? = nil
        
        // Überprüfen, ob der erste Parameter eine gültige URL ist
        if let wssURL = command.arguments.first as? String, !wssURL.isEmpty {
            // Verbindung mit der URL herstellen
            self.webSocketClientManager.connect(to: wssURL)
            pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "true")
        } else {
            pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR)
        }
        
        // Das Ergebnis an den Cordova-Callback zurückgeben
        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }

    @objc(isConnected:)
    func isConnected(command: CDVInvokedUrlCommand) {
        var pluginResult: CDVPluginResult? = nil

        if self.webSocketClientManager.isConnected {
            pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "true")
        } else {
            pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "false")
        }
        
        // Das Ergebnis an den Cordova-Callback zurückgeben
        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }
}
