@objc(CardLink) class CardLink: CDVPlugin {
    
    // Verwende lazy var, um die Instanz beim ersten Zugriff zu erstellen
    private lazy var webSocketClientManager = WebSocketClientManager()
    private lazy var cardReaderManager = CardReaderManager()
    private lazy var isCodeCorrect = false
    private lazy var canNumber = ""
    
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
             webSocketClientManager.cardSessionId = "APPDINX_\(UUID().uuidString)"
        
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

    @objc(verifyCode:)
    func verifyCode(command: CDVInvokedUrlCommand) {
        var pluginResult: CDVPluginResult? = nil

        if let code = command.arguments.first as? String, !code.isEmpty {
            let payloadDict = ["smsCode": code]

            do {
                let jsonData = try JSONSerialization.data(withJSONObject: payloadDict, options: [])
                let base64Encoded = jsonData.base64EncodedString()

                let confirmSMSCodeMessage = """
                [
                    {
                        "type": "confirmSMSCode",
                        "payload": "\(base64Encoded)"
                    },
                    "\(webSocketClientManager.cardSessionId!)"
                ]
                """

                webSocketClientManager.send(confirmSMSCodeMessage) {
                    print("SUCCESSFULLY SENT CONFIRMSMSCODE MESSAGE")

                    // Hier wird der Observer hinzugefügt, um die Antwort zu empfangen
                    NotificationCenter.default.addObserver(
                        self,
                        selector: #selector(self.handleConfirmSMSCodeResponse(_:)),
                        name: .confirmSMSCodeResponse,
                        object: nil
                    )
                }

                pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "true")
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

    @objc(isSMSCodeCorrect:)
    func isSMSCodeCorrect(command: CDVInvokedUrlCommand) {
        var pluginResult: CDVPluginResult? = nil

        if self.isCodeCorrect {
            pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "true")
        } else {
            pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "false")
        }
        
        // Das Ergebnis an den Cordova-Callback zurückgeben
        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }

    @objc(setCanNumber:)
    func setCanNumber(command: CDVInvokedUrlCommand) {
        var pluginResult: CDVPluginResult? = nil

        if let canNumber = command.arguments.first as? String, !canNumber.isEmpty {
            self.canNumber = canNumber;
            pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "true")
        } else {
            pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR)
        }
        
        // Das Ergebnis an den Cordova-Callback zurückgeben
        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }

    @objc(startReadCard:)
    func startReadCard(command: CDVInvokedUrlCommand) {
        var pluginResult: CDVPluginResult? = nil

        Task {
            do {
                _ = try await cardReaderManager.scanCard(canNumber: canNumber, cardSessionId: webSocketClientManager.cardSessionId!)
                pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "true")
            } catch {
                print("[ERROR] Failed to scan card: \(error)")
                pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR)
            }
        }
        
        // Das Ergebnis an den Cordova-Callback zurückgeben
        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }

    // Diese Methode wird aufgerufen, wenn eine Benachrichtigung empfangen wird
    @objc func handleConfirmSMSCodeResponse(_ notification: Notification) {
        if let info = notification.object as? [String: String],
        let payload = info["payload"] {
            handleVerificationResponse(payload)
        }
    }

    @objc func handleVerificationResponse(_ payload: String) {
        if let data = Data(base64Encoded: payload),
           let payloadString = String(data: data, encoding: .utf8),
           let payloadJson = try? JSONSerialization.jsonObject(with: Data(payloadString.utf8), options: []) as? [String: Any],
           let verificationResult = payloadJson["result"] as? String {
            if verificationResult == "SUCCESS" {
                isCodeCorrect = true
            } else if verificationResult == "FAILURE" {
                isCodeCorrect = false
            }
        }
    }
}