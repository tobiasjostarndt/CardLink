//
//  NFCScannerController.swift
//  Egk
//
//  Created by Beatriz on 15/05/2024.
//

import CardReaderProviderApi
import Combine
import CoreNFC
import Foundation
import GemCommonsKit
import HealthCardAccess
import HealthCardControl
import Helper
import NFCCardReaderProvider

public class NFCScannerController: ScannerController {
    var observer: NSObjectProtocol?
    var receivedCommandFromFirstSendAPDU: String?
    var receivedCommandFromSecondSendAPDU: String?
    
    public enum Error: Swift.Error, LocalizedError {
        case cardError(NFCHealthCardSessionError)
        case invalidCanOrPinFormat
        case unsupportedTag
        case wrongCAN
        
        public var errorDescription: String? {
            switch self {
            case let .cardError(error):
                return error.localizedDescription
            case .invalidCanOrPinFormat:
                return "Invalid CAN or PIN format"
            case .unsupportedTag:
                return "UnsupportedTag"
            case .wrongCAN:
                return "WRONG CAN"
            }
        }
    }
    
    @MainActor
    @Published
    private var pState: ViewState<Bool, Swift.Error> = .idle
    var state: Published<ViewState<Bool, Swift.Error>>.Publisher {
        $pState
    }
    
    var cancellable: AnyCancellable?
    
    @MainActor
    func dismissError() async {
        if pState.error != nil {
            pState = .idle
        }
    }
    
    let messages = NFCHealthCardSession<Data>.Messages(
        discoveryMessage: NSLocalizedString("Warten auf die Platzierung der Karte", comment: ""),
        connectMessage: NSLocalizedString("connectMessage", comment: ""),
        secureChannelMessage: NSLocalizedString("Sichere Sitzung eingerichtet", comment: ""),
        noCardMessage: NSLocalizedString("noCardMessage", comment: ""),
        multipleCardsMessage: NSLocalizedString("multipleCardsMessage", comment: ""),
        unsupportedCardMessage: NSLocalizedString("unsupportedCardMessage", comment: ""),
        connectionErrorMessage: NSLocalizedString("connectionErrorMessage", comment: "")
    )
    
    deinit {
        if let observer = observer {
            NotificationCenter.default.removeObserver(observer)
        }
    }
    
    // TO BE REFACTORED
    func readEgkData(can: String, cardSessionId: String) async -> Data? {
        if case .loading = await pState { return nil }
        await MainActor.run {
            self.pState = .loading(nil)
        }
        
        guard let nfcHealthCardSession = NFCHealthCardSession(messages: messages, can: can, operation: { session in
            let cEgkAutCVCE256Length = 0x00DE
            
            var resultDict = [String: String]()
            
            func transmitAndStoreResult(command: HealthCardCommand, tagName: String) async throws {
                let response = try await command.transmitAsync(to: session.card)
                if let base64String = response.data?.base64EncodedString() {
                    resultDict[tagName] = base64String
                }
            }
            
            let observer = NotificationCenter.default.addObserver(forName: .sendFirstSendAPDUCommandReceived, object: nil, queue: nil) { [weak self] notification in
                guard let self = self else { return }
                Task {
                    if let receivedObject = notification.object as? [String: Any], let payload = receivedObject["payload"] as? String {
                        self.receivedCommandFromFirstSendAPDU = payload
                    } else {
                        print("Not the expected String object")
                    }
                }
            }
            
            let observer2 = NotificationCenter.default.addObserver(forName: .sendSecondSendAPDUCommandReceived, object: nil, queue: nil) { [weak self] notification in
                guard let self = self else { return }
                Task {
                    if let receivedObject = notification.object as? [String: Any], let payload = receivedObject["payload"] as? String {
                        self.receivedCommandFromSecondSendAPDU = payload
                    } else {
                        print("Not the expected String object")
                    }
                }
            }
            
            resultDict["cardSessionId"] = cardSessionId
            resultDict["client"] = "COM"
            
            try await transmitAndStoreResult(
                command: try HealthCardCommand.Read.readFileCommand(
                    with: EgkFileSystem.EF.gdo.sfid!,
                    ne: cEgkAutCVCE256Length,
                    offset: 0
                ),
                tagName: "gdo"
            )
            try await transmitAndStoreResult(
                command: try HealthCardCommand.Read.readFileCommand(
                    with: EgkFileSystem.EF.atr.sfid!,
                    ne: cEgkAutCVCE256Length,
                    offset: 0
                ),
                tagName: "atr"
            )
            try await transmitAndStoreResult(
                command: try HealthCardCommand.Read.readFileCommand(
                    with: EgkFileSystem.EF.version2.sfid!,
                    ne: cEgkAutCVCE256Length,
                    offset: 0
                ),
                tagName: "cardVersion"
            )
            try await transmitAndStoreResult(
                command: try HealthCardCommand.Read.readFileCommand(
                    with: EgkFileSystem.EF.cEgkAutCVCE256.sfid!,
                    ne: cEgkAutCVCE256Length,
                    offset: 0
                ),
                tagName: "cvcAuth"
            )
            try await transmitAndStoreResult(
                command: try HealthCardCommand.Read.readFileCommand(
                    with: EgkFileSystem.EF.cCaEgkCsE256.sfid!,
                    ne: cEgkAutCVCE256Length,
                    offset: 0
                ),
                tagName: "cvcCA"
            )
            
            let eSign = EgkFileSystem.DF.ESIGN
            let selectEsignCommand = HealthCardCommand.Select.selectFile(with: eSign.aid)
            _ = try await selectEsignCommand.transmitAsync(to: session.card)
            
            try await transmitAndStoreResult(
                command: try HealthCardCommand.Read.readFileCommand(
                    with: EgkFileSystem.EF.esignCChAutR2048.sfid!,
                    ne: 0x076C - 1,
                    offset: 0
                ),
                tagName: "x509AuthRSA"
            )
            try await transmitAndStoreResult(
                command: try HealthCardCommand.Read.readFileCommand(
                    with: EgkFileSystem.EF.esignCChAutE256.sfid!,
                    ne: 0x076C - 1,
                    offset: 0
                ),
                tagName: "x509AuthECC"
            )
            
            let jsonData = try JSONSerialization.data(withJSONObject: resultDict, options: [])
            if let jsonString = String(data: jsonData, encoding: .utf8) {
                print(jsonString)
            }
            
            DispatchQueue.main.async {
                NotificationCenter.default.post(name: .egkDataReceived, object: jsonData)
            }
            
            if let firstSendAPDUCommand = await self.waitForCommandAPDUNotification(name: .sendFirstSendAPDUCommandReceived) {
                let firstCommand = [UInt8](firstSendAPDUCommand)
                do {
                    let firstSendAPDUCommandReadyToSend = try unwrapCommandApdu(apduMessage: firstCommand)
                    let response = try await session.card.currentCardChannel.transmitAsync(command: firstSendAPDUCommandReadyToSend, writeTimeout: 3000, readTimeout: 3000)
                    var responseToFirstAPDUCommand = ["response": encodeResponseToBase64(sw: response.sw)]
                    
                    let jsonData2 = try JSONSerialization.data(withJSONObject: responseToFirstAPDUCommand, options: [])
                    
                    DispatchQueue.main.async {
                        NotificationCenter.default.post(name: .receivedFirstSendAPDUResponse, object: jsonData2)
                    }
                } catch {
                    print("NOT WORKING :( \(error)")
                    throw error
                }
            }
            
            if let secondSendAPDUCommand = await self.waitForCommandAPDUNotification(name: .sendSecondSendAPDUCommandReceived) {
                let secondCommand = [UInt8](secondSendAPDUCommand)
                do {
                    let secondSendAPDUCommandReadyToSend = try unwrapCommandApdu(apduMessage: secondCommand)
                    let response = try await session.card.currentCardChannel.transmitAsync(command: secondSendAPDUCommandReadyToSend, writeTimeout: 3000, readTimeout: 3000)
                    
                    var resultDict3 = [String: String]()
                    var finalString = ""
                    if let data = response.data {
                        let hexString = data.map { String(format: "%02x", $0) }.joined()
                        finalString = hexString + "9000" // append SW
                    } else {
                        print("response.data is nil")
                    }
                    let dataWithHex = hexStringToData(finalString)
                    resultDict3["response"] = dataWithHex?.base64EncodedString()
                    
                    let jsonData2 = try JSONSerialization.data(withJSONObject: resultDict3, options: [])
                    
                    DispatchQueue.main.async {
                        NotificationCenter.default.post(name: .receivedSecondSendAPDUResponse, object: jsonData2)
                    }
                } catch {
                    print("NOT WORKING :( \(error)")
                    throw error
                }
            }
            
            session.updateAlert(message: NSLocalizedString("Success", comment: ""))
            return jsonData
        })
        else {
            Task { @MainActor in self.pState = .error(NFCHealthCardSessionError.couldNotInitializeSession) }
            return nil
        }
        
        do {
            let readEgkData = try await nfcHealthCardSession.executeOperation()
            return readEgkData
        } catch let error as NFCHealthCardSessionError {
            Task { @MainActor in
                switch error {
                case .wrongCAN:
                    self.pState = .error(NFCScannerController.Error.wrongCAN)
                case .unsupportedTag:
                    self.pState = .error(NFCScannerController.Error.unsupportedTag)
                default:
                    self.pState = .error(NFCScannerController.Error.cardError(error))
                }
            }
            nfcHealthCardSession.invalidateSession(with: error.localizedDescription)
            return nil
        } catch {
            Task { @MainActor in self.pState = .error(error) }
            nfcHealthCardSession.invalidateSession(with: error.localizedDescription)
            return nil
        }
    }

    func waitForCommandAPDUNotification(name: Notification.Name) async -> Data? {
        return await withCheckedContinuation { continuation in
            var observer: NSObjectProtocol?
            observer = NotificationCenter.default.addObserver(forName: name, object: nil, queue: nil) { notification in
                if let receivedObject = notification.object as? [String: Any], let payload = receivedObject["payload"] as? String, let data = Data(base64Encoded: payload) {
                    continuation.resume(returning: data)
                } else {
                    continuation.resume(returning: nil)
                }
                if let observer = observer {
                    NotificationCenter.default.removeObserver(observer)
                }
            }
        }
    }
}

func hexStringToData(_ hexString: String) -> Data? {
    var data = Data()
    var hex = hexString
    
    if hex.count % 2 != 0 {
        hex = "0" + hex
    }
    
    for i in stride(from: 0, to: hex.count, by: 2) {
        let startIndex = hex.index(hex.startIndex, offsetBy: i)
        let endIndex = hex.index(startIndex, offsetBy: 2)
        let byteString = hex[startIndex..<endIndex]
        if let num = UInt8(byteString, radix: 16) {
            data.append(num)
        } else {
            return nil
        }
    }
    return data
}

func encodeResponseToBase64(sw: UInt16) -> String? {
    var swBigEndian = sw.bigEndian
    let swData = withUnsafeBytes(of: &swBigEndian) { Data($0) }
    let base64Encoded = swData.base64EncodedString()
    
    return base64Encoded
}

public func unwrapCommandApdu(apduMessage: [UInt8]) throws -> APDU.Command {
    let apduData = Message.getAPDUData(body: apduMessage)
    let commandApdu: APDU.Command
    if !apduData.isEmpty {
        let ne: Int? = (apduMessage[0] == 0x00 && apduMessage[1] == 0x88 && (apduMessage.count - 6) == Message.getAPDULength(apduData: apduMessage)) ? Int(apduMessage[apduMessage.count - 1]) : nil
        commandApdu = try APDU.Command(cla: apduMessage[0], ins: apduMessage[1], p1: apduMessage[2], p2: apduMessage[3], data: Data(apduData), ne: ne)
    } else if apduMessage.count == 4 {
        commandApdu = try APDU.Command(cla: apduMessage[0], ins: apduMessage[1], p1: apduMessage[2], p2: apduMessage[3])
    } else {
        let ne: Int = apduMessage.count == 5 ? Message.expectedLength(apdu: apduMessage) : APDU.expectedLengthWildcardExtended
        commandApdu = try APDU.Command(cla: apduMessage[0], ins: apduMessage[1], p1: apduMessage[2], p2: apduMessage[3], ne: ne)
    }
    return commandApdu
}

extension Notification.Name {
    static let sendFirstSendAPDUCommandReceived = Notification.Name("sendFirstSendAPDUCommandReceived")
    static let sendSecondSendAPDUCommandReceived = Notification.Name("sendSecondSendAPDUCommandReceived")
    static let egkDataReceived = Notification.Name("egkDataReceived")
    static let receivedFirstSendAPDUResponse = Notification.Name("receivedFirstSendAPDUResponse")
    static let receivedSecondSendAPDUResponse = Notification.Name("receivedSecondSendAPDUResponse")
}
