@objc(CardLink) class CardLink: CDVPlugin {
    
    // Verwende lazy var, um die Instanz beim ersten Zugriff zu erstellen
    private lazy var webSocketClientManager = WebSocketClientManager()
    private lazy var cardReaderManager = CardReaderManager()
    private lazy var isCodeCorrect = false
    private lazy var canNumber = ""
    private lazy var correlationId = ""
    private lazy var cardScanned = false
    private lazy var eRezeptTokensFromAVS = ""
    private lazy var eRezeptBundlesFromAVS = ""
    
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
                NotificationCenter.default.addObserver(
                    self,
                    selector: #selector(self.handleEgkDataReceived(_:)),
                    name: .egkDataReceived,
                    object: nil
                )

                NotificationCenter.default.addObserver(
                    self,
                    selector: #selector(self.handleReceivedFirstSendAPDU(_:)),
                    name: .receivedFirstSendAPDU,
                    object: nil
                )

                NotificationCenter.default.addObserver(
                    self,
                    selector: #selector(self.handleReceivedSecondSendAPDU(_:)),
                    name: .receivedSecondSendAPDU,
                    object: nil
                )

                NotificationCenter.default.addObserver(
                    self,
                    selector: #selector(self.handleReceivedFirstSendAPDUResponse(_:)),
                    name: .receivedFirstSendAPDUResponse,
                    object: nil
                )

                NotificationCenter.default.addObserver(
                    self,
                    selector: #selector(self.handleReceivedSecondSendAPDUResponse(_:)),
                    name: .receivedSecondSendAPDUResponse,
                    object: nil
                )
                
                NotificationCenter.default.addObserver(
                    self,
                    selector: #selector(self.handleReceivedERezeptTokensFromAVS(_:)),
                    name: .receivedERezeptTokensFromAVS,
                    object: nil
                )
                
                NotificationCenter.default.addObserver(
                    self,
                    selector: #selector(self.handleReceivedERezeptBundlesFromAVS(_:)),
                    name: .receivedERezeptBundlesFromAVS,
                    object: nil
                )

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
    
    @objc(isCardScanned:)
    func isCardScanned(command: CDVInvokedUrlCommand) {
        var pluginResult: CDVPluginResult? = nil

        if self.cardScanned {
            pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "true")
        } else {
            pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "false")
        }
        
        // Das Ergebnis an den Cordova-Callback zurückgeben
        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }
    
    @objc(getERezeptTokensFromAVS:)
    func getERezeptTokensFromAVS(command: CDVInvokedUrlCommand) {
        var pluginResult: CDVPluginResult? = nil

        if self.cardScanned && self.eRezeptTokensFromAVS != "" {
            pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: self.eRezeptTokensFromAVS)
        } else {
            pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR)
        }
        
        // Das Ergebnis an den Cordova-Callback zurückgeben
        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }
    
    @objc(getERezeptBundlesFromAVS:)
    func getERezeptBundlesFromAVS(command: CDVInvokedUrlCommand) {
        var pluginResult: CDVPluginResult? = nil

        if self.cardScanned && self.eRezeptBundlesFromAVS != "" {
            pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: self.eRezeptBundlesFromAVS)
        } else {
            pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR)
        }
        
        // Das Ergebnis an den Cordova-Callback zurückgeben
        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }

    // Diese Methode wird aufgerufen, wenn eine Benachrichtigung empfangen wird
    @objc func handleConfirmSMSCodeResponse(_ notification: Notification) {
        if let info = notification.object as? [String: String],
        let payload = info["payload"] {
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

    @objc func handleEgkDataReceived(_ notification: Notification){
        if let cardData = notification.object as? Data {
            let base64Encoded = cardData.base64EncodedString()
            
            let newUUID = "APPDINX_\(UUID().uuidString)"
            let registerEgkMessage = """
            [
                {
                    "type": "registerEGK",
                    "payload": "\(base64Encoded)"
                },
                "\(webSocketClientManager.cardSessionId!)",
                "\(newUUID)"
            ]
            """
            
            print(registerEgkMessage)
            webSocketClientManager.send(registerEgkMessage) {
                print("SUCCESSFULLY SENT REGISTEREGK MESSAGE")
            }
        }
    }

    @objc func handleReceivedFirstSendAPDU(_ notification: Notification){
        if let info = notification.object as? [String: String],
            let payload = info["payload"],
            let receivedCorrelationId = info["correlationId"] {
            self.correlationId = receivedCorrelationId
            print("Payload from sendAPDU message: \(payload)")
            print("CorrelationID from sendAPDU message: \(self.correlationId)")
            
            if let data = Data(base64Encoded: payload),
                let payloadString = String(data: data, encoding: .utf8) {
                if let payloadJson = try? JSONSerialization.jsonObject(with: Data(payloadString.utf8), options: []) as? [String: Any],
                    let cardSessionId = payloadJson["cardSessionId"] as? String,
                    let apdu = payloadJson["apdu"] as? String {
                    
                    let apduCommand: [String: Any] = ["payload": apdu]
                    NotificationCenter.default.post(name: .sendFirstSendAPDUCommandReceived, object: apduCommand)
                } else {
                    print("Failed to parse the payload JSON.")
                }
            } else {
                print("Failed to decode the base64 payload.")
            }
        } else {
            print("Failed to extract payload and correlationId from notification.")
        }
    }

    @objc func handleReceivedSecondSendAPDU(_ notification: Notification){
        if let info = notification.object as? [String: String],
            let payload = info["payload"],
            let receivedCorrelationId = info["correlationId"] {
            self.correlationId = receivedCorrelationId
            
            if let data = Data(base64Encoded: payload),
                let payloadString = String(data: data, encoding: .utf8) {
                if let payloadJson = try? JSONSerialization.jsonObject(with: Data(payloadString.utf8), options: []) as? [String: Any],
                    let cardSessionId = payloadJson["cardSessionId"] as? String,
                    let apdu = payloadJson["apdu"] as? String {
                    
                    let apduCommand: [String: Any] = ["payload": apdu]
                    NotificationCenter.default.post(name: .sendSecondSendAPDUCommandReceived, object: apduCommand)
                } else {
                    print("Failed to parse the payload JSON.")
                }
            } else {
                print("Failed to decode the base64 payload.")
            }
        } else {
            print("Failed to extract payload and correlationId from notification.")
        }
    }

    @objc func handleReceivedFirstSendAPDUResponse(_ notification: Notification){
        if let firstSendAPDUResponseData = notification.object as? Data {
            let base64Encoded = firstSendAPDUResponseData.base64EncodedString()
            let sendApduResponseMessage = """
            [
                {
                    "type": "sendAPDUResponse",
                    "payload": "\(base64Encoded)"
                },
                "\(webSocketClientManager.cardSessionId!)",
                "\(self.correlationId)"
            ]
            """
            
            print(sendApduResponseMessage)
            webSocketClientManager.send(sendApduResponseMessage) {
                print("SUCCESSFULLY SENT SENDAPDURESPONSE MESSAGE")
            }
        }
    }
    
    @objc func handleReceivedSecondSendAPDUResponse(_ notification: Notification){
        if let secondSendAPDUResponseData = notification.object as? Data {
            let base64Encoded = secondSendAPDUResponseData.base64EncodedString()
            let sendApduResponseMessage = """
                        [
                            {
                                "type": "sendAPDUResponse",
                                "payload": "\(base64Encoded)"
                            },
                            "\(webSocketClientManager.cardSessionId!)",
                            "\(self.correlationId)"
                        ]
                        """
            
            print(sendApduResponseMessage)
            webSocketClientManager.send(sendApduResponseMessage) {
                print("SUCCESSFULLY SENT SENDAPDURESPONSE MESSAGE")
                self.cardScanned = true
            }
        }
    }
    
    @objc func handleReceivedERezeptTokensFromAVS(_ notification: Notification){
        if let objectReceived = notification.object as? [String: Any],
           let base64Payload = objectReceived["payload"] as? String {
            
            if let data = Data(base64Encoded: base64Payload),
               let jsonString = String(data: data, encoding: .utf8),
               let jsonData = jsonString.data(using: .utf8) {
                self.eRezeptTokensFromAVS = jsonString
                /*do {
                    if let jsonObject = try JSONSerialization.jsonObject(with: jsonData, options: []) as? [String: Any],
                       let tokens = jsonObject["tokens"] as? String {
                        print("Tokens content: \(tokens)")
                        prescriptions = TokensParser.parseXmlPrescriptions(xmlString: tokens)
                        
                    } else {
                        print("Failed to extract tokens")
                    }
                } catch {
                    print("Error parsing JSON: \(error)")
                }*/
            } else {
                print("Failed to convert jsonString to Data")
            }
        }
    }
    
    @objc func handleReceivedERezeptBundlesFromAVS(_ notification: Notification){
        if let objectReceived = notification.object as? [String: Any],
           let base64Payload = objectReceived["payload"] as? String {
            
            if let data = Data(base64Encoded: base64Payload),
               let jsonString = String(data: data, encoding: .utf8),
               let jsonData = jsonString.data(using: .utf8) {
                self.eRezeptBundlesFromAVS = jsonString
                /*do {
                    if let jsonObject = try JSONSerialization.jsonObject(with: jsonData, options: []) as? [String: Any],
                       let bundlesArray = jsonObject["bundles"] as? [String] {
                        
                        for bundleString in bundlesArray {
                            
                            if let bundle = parseXmlBundle(xmlString: bundleString) {
                                bundles.append(bundle)
                                
                                print("Medication Name: \(bundle.medicationName ?? "")")
                                print("PZN: \(bundle.medicationCode ?? "")")
                                print("Prescription ID: \(bundle.prescriptionId ?? "")")
                                print("---")
                            } else {
                                print("Error parsing XML bundle: \(bundleString)")
                            }
                        }
                        
                    } else {
                        print("Failed to extract bundles")
                    }
                } catch {
                    print("Error parsing JSON: \(error)")
                }*/
            } else {
                print("Failed to convert jsonString to Data")
            }
        }
    }
}
