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

    @objc(isConnectedWSS:)
    func isConnectedWSS(command: CDVInvokedUrlCommand) {
        var pluginResult: CDVPluginResult? = nil

        if self.webSocketClientManager.isConnected {
            pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "true")
        } else {
            pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "false")
        }
        
        // Das Ergebnis an den Cordova-Callback zurückgeben
        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }

    @objc(sendRequestSMSCodeMessage:)
    func sendRequestSMSCodeMessage(command: CDVInvokedUrlCommand) {
        var pluginResult: CDVPluginResult? = nil

        if let phoneNumber = command.arguments.first as? String, !phoneNumber.isEmpty {
             webSocketClientManager.cardSessionId = UUID().uuidString
        
            let payloadDict: [String: String] = [
                "senderId": "cardlink",
                "textTemplate": "Bitte geben Sie in der CardLink App folgenden Code ein: {0}",
                "phoneNumber": phoneNumber,
                "textReassignmentTemplate": "Ihre Gesundheitskarte {0} wurde der Telefonnummer {1} neu zugeordnet. Wenn Sie diese Telefonnummer kennen, ist alles in Ordnung. Wenn Ihre Karte gestohlen wurde, lassen Sie diese bitte von Ihrer Versicherung sperren."
            ]
            
            do {
                let jsonData = try JSONSerialization.data(withJSONObject: payloadDict, options: [])
                let base64Encoded = jsonData.base64EncodedString()
                
                let requestSMSCodeMessage = """
                [
                    {
                        "type": "requestSMSCode",
                        "payload": "\(base64Encoded)"
                    },
                    "\(webSocketClientManager.cardSessionId!)"
                ]
                """
                
                webSocketClientManager.send(requestSMSCodeMessage) {
                    print("SUCCESSFULLY SENT REQUESTSMSCODE MESSAGE")
                    pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "true")
                }
            } catch {
                print("Failed to serialize JSON: \(error)")
                pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR)
            }
        } else {
            pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR)
        }
        
        // Das Ergebnis an den Cordova-Callback zurückgeben
        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }
}
