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
    var receivedCommandFromSendAPDU: String? = nil

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
        discoveryMessage: NSLocalizedString(
            "\n Karte flach hinlegen und das Smartphone direkt darauf platzieren. \n",
            comment: ""
        ),
        connectMessage: NSLocalizedString("connectMessage", comment: ""),
        secureChannelMessage: NSLocalizedString(
            "\n Kartenlesung, bitte stillhalten \n",
            comment: ""
        ),
        noCardMessage: NSLocalizedString("noCardMessage", comment: ""),
        multipleCardsMessage: NSLocalizedString(
            "multipleCardsMessage",
            comment: ""
        ),
        unsupportedCardMessage: NSLocalizedString(
            "unsupportedCardMessage",
            comment: ""
        ),
        connectionErrorMessage: NSLocalizedString(
            "connectionErrorMessage",
            comment: ""
        ),
        readingRecieps25: NSLocalizedString("Rezepte werden gesucht: 25 % ...", comment: ""),
        readingRecieps50: NSLocalizedString("Rezepte werden gesucht: 50 % ...", comment: ""),
        readingRecieps75: NSLocalizedString("Rezepte werden gesucht: 75 % ...", comment: ""),
        readingRecieps100: NSLocalizedString("Rezepte werden gesucht: 100 % ...", comment: "")
    )

    deinit {
        if let observer = observer {
            NotificationCenter.default.removeObserver(observer)
        }
    }

    func readEgkData(can: String, cardSessionId: String) async -> Data? {
        if case .loading = await pState { return nil }
        await MainActor.run {
            self.pState = .loading(nil)
        }

        guard
            let nfcHealthCardSession = NFCHealthCardSession(
                messages: messages,
                can: can,
                operation: { session in
                    let cEgkAutCVCE256Length = 0x00DE

                    var resultDict = [String: String]()
                    
                    session.updateAlert(message: self.messages.readingRecieps25)

                    func transmitAndStoreResult(
                        command: HealthCardCommand,
                        tagName: String
                    ) async throws {
                        let response = try await command.transmitAsync(
                            to: session.card
                        )
                        if let base64String = response.data?
                            .base64EncodedString()
                        {
                            resultDict[tagName] = base64String
                        }
                    }

                    let observer = NotificationCenter.default.addObserver(
                        forName: .sendAPDUCommandReceived,
                        object: nil,
                        queue: nil
                    ) { [weak self] notification in
                        guard let self = self else { return }
                        Task {
                            if let receivedObject = notification.object
                                as? [String: Any],
                                let payload = receivedObject["payload"]
                                    as? String
                            {
                                self.receivedCommandFromSendAPDU = payload
                            } else {
                                print("Not the expected String object")
                            }
                        }
                    }
                    
                    let observer2 = NotificationCenter.default.addObserver(
                        forName: .receivedERezeptBundlesFromAVS,
                        object: nil,
                        queue: nil
                    ) { [weak self] notification in
                        guard let self = self else { return }
                        Task {
                            if let receivedObject = notification.object
                                as? [String: Any],
                                let payload = receivedObject["payload"]
                                    as? String
                            {
                                session.updateAlert(message: self.messages.readingRecieps100)
                                session.invalidateSession(with: nil)
                            } else {
                                print("Not the expected String object")
                            }
                        }
                    }

                    resultDict["cardSessionId"] = cardSessionId
                    resultDict["client"] = "COM"

                    do {
                        _ = try await transmitAndStoreResult(
                            command: try HealthCardCommand.Read.readFileCommand(
                                with: EgkFileSystem.EF.gdo.sfid!,
                                ne: cEgkAutCVCE256Length,
                                offset: 0
                            ),
                            tagName: "gdo"
                        )
                        
                        session.updateAlert(message: self.messages.readingRecieps50)
                    } catch {
                        session.invalidateSession(with: "Failed to detect card")
                        throw error
                    }
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
                    let selectEsignCommand = HealthCardCommand.Select
                        .selectFile(with: eSign.aid)
                    _ = try await selectEsignCommand.transmitAsync(
                        to: session.card
                    )

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
                    
                    session.updateAlert(message: self.messages.readingRecieps75)
                    print("JETZT SIND WIR HIER")

                    let jsonData = try JSONSerialization.data(
                        withJSONObject: resultDict,
                        options: []
                    )
                    if let jsonString = String(data: jsonData, encoding: .utf8)
                    {
                        print(jsonString)
                    }

                    DispatchQueue.main.async {
                        NotificationCenter.default.post(
                            name: .egkDataReceived,
                            object: jsonData
                        )
                    }

                    let (apduStream, apduContinuation) =
                        self.makeCommandAPDUStream(
                            name: .sendAPDUCommandReceived
                        )

                    let stopObserver = NotificationCenter.default.addObserver(
                        forName: .stopAPDUStream,
                        object: nil,
                        queue: nil
                    ) { _ in
                        apduContinuation.finish()
                        print("stopAPDUStream")
                    }

                    for await sendAPDUCommand in apduStream {
                        let command = [UInt8](sendAPDUCommand)
                        do {
                            print("WAS GEHT HIER?")
                            let sendAPDUCommandReadyToSend =
                                try unwrapCommandApdu(apduMessage: command)
                            let response = try await session.card
                                .currentCardChannel.transmitAsync(
                                    command: sendAPDUCommandReadyToSend,
                                    writeTimeout: 3000,
                                    readTimeout: 3000
                                )

                            var resultDict3 = [String: String]()
                            var finalString: String = ""

                            if let data = response.data {
                                let hexString = data.map {
                                    String(format: "%02x", $0)
                                }.joined()
                                let swHex = String(format: "%04X", response.sw)
                                finalString = hexString + swHex
                            } else {
                                finalString = String(
                                    format: "%04X",
                                    response.sw
                                )
                            }

                            let dataWithHex = hexStringToData(finalString)
                            resultDict3["response"] = dataWithHex?
                                .base64EncodedString()

                            let jsonData2 = try JSONSerialization.data(
                                withJSONObject: resultDict3,
                                options: []
                            )

                            DispatchQueue.main.async {
                                NotificationCenter.default.post(
                                    name: .receivedSendAPDUResponse,
                                    object: jsonData2
                                )
                            }
                        } catch {
                            print("NOT WORKING :( \(error)")
                            break
                        }
                    }
                    
                    try? await Task.sleep(nanoseconds: 500_000_000)
                    NotificationCenter.default.removeObserver(stopObserver)

                    return jsonData
                }
            )
        else {
            Task { @MainActor in
                self.pState = .error(
                    NFCHealthCardSessionError.couldNotInitializeSession
                )
            }
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
                    self.pState = .error(
                        NFCScannerController.Error.unsupportedTag
                    )
                default:
                    self.pState = .error(
                        NFCScannerController.Error.cardError(error)
                    )
                }
            }
            nfcHealthCardSession.invalidateSession(
                with: error.localizedDescription
            )
            return nil
        } catch {
            Task { @MainActor in self.pState = .error(error) }
            nfcHealthCardSession.invalidateSession(
                with: error.localizedDescription
            )
            return nil
        }
    }

    func makeCommandAPDUStream(name: Notification.Name) -> (
        stream: AsyncStream<Data>, continuation: AsyncStream<Data>.Continuation
    ) {
        var continuation: AsyncStream<Data>.Continuation!

        let stream = AsyncStream<Data> { c in
            continuation = c

            let observer = NotificationCenter.default.addObserver(
                forName: name,
                object: nil,
                queue: nil
            ) { notification in
                if let receivedObject = notification.object as? [String: Any],
                    let payload = receivedObject["payload"] as? String,
                    let data = Data(base64Encoded: payload)
                {
                    c.yield(data)
                }
            }

            c.onTermination = { @Sendable _ in
                NotificationCenter.default.removeObserver(observer)
            }
        }
        return (stream, continuation)
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

    print(
        "unwrapCommandApdu: raw = \(apduMessage.map { String(format: "%02X", $0) }.joined())"
    )

    let apduData = Message.getAPDUData(body: apduMessage)
    print(
        "APDU data extracted: \(apduData.map { String(format: "%02X", $0) }.joined())"
    )

    if apduMessage.count == 7 && apduMessage[4] == 0x00 {
        let le = (Int(apduMessage[5]) << 8) | Int(apduMessage[6])
        print("Extended-length APDU with le only: le = \(le)")
        return try APDU.Command(
            cla: apduMessage[0],
            ins: apduMessage[1],
            p1: apduMessage[2],
            p2: apduMessage[3],
            ne: le == 0 ? APDU.expectedLengthWildcardExtended : le
        )
    }

    if apduMessage.count == 4 {
        print("APDU (header only)")
        return try APDU.Command(
            cla: apduMessage[0],
            ins: apduMessage[1],
            p1: apduMessage[2],
            p2: apduMessage[3]
        )
    }

    if apduMessage.count == 5 {
        let ne = Message.expectedLength(apdu: apduMessage)
        print("Expected length = \(ne)")
        return try APDU.Command(
            cla: apduMessage[0],
            ins: apduMessage[1],
            p1: apduMessage[2],
            p2: apduMessage[3],
            ne: ne
        )
    }

    if !apduData.isEmpty {
        let isGeneralAuthenticate =
            (apduMessage[0] == 0x00 && apduMessage[1] == 0x88)
        let declaredLength = Message.getAPDULength(apduData: apduMessage)
        let actualLength = apduData.count
        let ne: Int? =
            (isGeneralAuthenticate && (apduMessage.count - 6) == declaredLength)
            ? Int(apduMessage.last ?? 0)
            : nil

        print(
            "Declared Lc: \(declaredLength), Actual: \(actualLength), ne: \(ne?.description ?? "nil")"
        )

        return try APDU.Command(
            cla: apduMessage[0],
            ins: apduMessage[1],
            p1: apduMessage[2],
            p2: apduMessage[3],
            data: Data(apduData),
            ne: ne
        )
    }

    print("Unknown format. Defaulting to ne = 65536")
    let ne = APDU.expectedLengthWildcardExtended
    return try APDU.Command(
        cla: apduMessage[0],
        ins: apduMessage[1],
        p1: apduMessage[2],
        p2: apduMessage[3],
        ne: ne
    )
}

extension Notification.Name {
    public static let sendAPDUCommandReceived = Notification.Name(
        "sendAPDUCommandReceived"
    )
    public static let receivedSendAPDUResponse = Notification.Name(
        "receivedSendAPDUResponse"
    )
    public static let egkDataReceived = Notification.Name("egkDataReceived")
    public static let stopAPDUStream = Notification.Name("stopAPDUStream")

}
