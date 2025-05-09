//
//  WebSocketClientManager.swift
//  WebSocket
//
//  Created by Beatriz on 23/05/2024.
//

import Combine
import Foundation

public class WebSocketClientManager: ObservableObject {
    @Published public var webSocketClient: WebSocketClient?
    @Published public var isConnected = false
    @Published public var cardSessionId: String?
    public var cancellables = Set<AnyCancellable>()
    private var sendAPDUMessageCount = 0

    public init() {
        self.webSocketClient = WebSocketClient()
        subscribeToConnectionStatus()
        subscribeToWebSocketMessages()
    }

    public func connect(to url: String) {
        webSocketClient?.connect(to: url)
    }

    public func send(_ jsonObject: Any, onSuccess: @escaping () -> Void) {
        webSocketClient?.send(jsonObject, onSuccess: onSuccess)
    }

    public func send(_ message: String, onSuccess: @escaping () -> Void) {
        webSocketClient?.socket.write(string: message)
        onSuccess()
    }

    private func subscribeToConnectionStatus() {
        webSocketClient?.connectionStatusPublisher
            .sink { [weak self] isConnected in
                self?.isConnected = isConnected
            }
            .store(in: &cancellables)
    }

    private func subscribeToWebSocketMessages() {
        webSocketClient?.messagePublisher
            .sink { [weak self] message in
                self?.handleWebSocketMessage(message)
            }
            .store(in: &cancellables)
    }

    private func handleWebSocketMessage(_ message: String) {
        print("Handling WebSocket message: \(message)")
        guard let data = message.data(using: .utf8) else {
            print("Failed to convert message to data.")
            return
        }

        do {
            if let jsonArray = try JSONSerialization.jsonObject(
                with: data,
                options: []
            ) as? [Any],
                jsonArray.count > 2,
                let messageDict = jsonArray[0] as? [String: Any],
                let messageType = messageDict["type"] as? String,
                let correlationId: String? = {
                    if jsonArray[2] is NSNull {
                        return ""
                    } else {
                        return jsonArray[2] as? String
                    }
                }()
            {

                switch messageType {

                case "confirmSMSCodeResponse":
                    if let payload = messageDict["payload"] as? String {
                        print(
                            "Received confirmSMSCodeResponse message with payload: \(payload)"
                        )
                        NotificationCenter.default.post(
                            name: .confirmSMSCodeResponse,
                            object: ["payload": payload]
                        )
                    }

                case "sendAPDU":
                    if let payload = messageDict["payload"] as? String {
                        if let correlationId = correlationId {
                            print(
                                "Received sendAPDU message with payload: \(payload), correlationId: \(correlationId)"
                            )
                        } else {
                            print(
                                "Received sendAPDU message with payload: \(payload), correlationId: nil"
                            )
                        }
                        NotificationCenter.default.post(
                            name: .receivedSendAPDU,
                            object: [
                                "payload": payload,
                                "correlationId": correlationId,
                            ]
                        )
                    } else {
                        print(
                            "Received sendAPDU message but payload is missing or invalid."
                        )
                    }

                case "eRezeptTokensFromAVS":
                    if let correlationId = correlationId {
                        print(
                            "Received eRezeptTokensFromAVS message with correlationId: \(correlationId)"
                        )
                    } else {
                        print(
                            "Received eRezeptTokensFromAVS message with no correlationId"
                        )
                    }
                    if let payload = messageDict["payload"] as? String {
                        NotificationCenter.default.post(
                            name: .receivedERezeptTokensFromAVS,
                            object: [
                                "payload": payload,
                                "correlationId": correlationId,
                            ]
                        )
                    }

                case "eRezeptBundlesFromAVS":
                    if let correlationId = correlationId {
                        print(
                            "Received eRezeptBundlesFromAVS message with correlationId: \(correlationId)"
                        )
                    } else {
                        print(
                            "Received eRezeptBundlesFromAVS message with no correlationId"
                        )
                    }
                    if let payload = messageDict["payload"] as? String {
                        NotificationCenter.default.post(
                            name: .receivedERezeptBundlesFromAVS,
                            object: [
                                "payload": payload,
                                "correlationId": correlationId,
                            ]
                        )
                    }
                    
                case "receiveTasklistError":
                    print("Received receiveTasklistError message with correlationId: \(correlationId)")
                    if let payload = messageDict["payload"] as? String {
                        NotificationCenter.default.post(name: .receivedTasklistError, object: ["payload": payload, "correlationId": correlationId])
                    }

                default:
                    print("Other message type: \(messageType)")
                }
            }
        } catch {
            print("Failed to parse JSON with error: \(error)")
        }
    }
}

extension Notification.Name {
    public static let confirmSMSCodeResponse = Notification.Name(
        "confirmSMSCodeResponse"
    )
    public static let receivedSendAPDU = Notification.Name("receivedSendAPDU")
    public static let receivedERezeptTokensFromAVS = Notification.Name(
        "receivedERezeptTokensFromAVS"
    )
    public static let receivedERezeptBundlesFromAVS = Notification.Name(
        "receivedERezeptBundlesFromAVS"
    )
    static let receivedTasklistError = Notification.Name("receivedTasklistError")
}

public class MockWebSocketClientManager: WebSocketClientManager {
    public override init() {
        super.init()
        self.isConnected = false
    }

    public override func connect(to url: String) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            self.isConnected = true
        }
    }
}
