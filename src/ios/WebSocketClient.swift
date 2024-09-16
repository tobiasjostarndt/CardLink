//
//  WebSocketClient.swift
//  WebSocket
//
//  Created by Beatriz Correia on 26/04/2024.
//

import Foundation
import Starscream
import Combine

public class WebSocketClient: WebSocketDelegate {
    
    var socket: WebSocket!
    var isConnected = false
    
    public let connectionStatusPublisher = PassthroughSubject<Bool, Never>()
    public let messagePublisher = PassthroughSubject<String, Never>()
    
    public init() {}
    
    public func connect(to webSocketUrl: String) {
        var request = URLRequest(url: URL(string: webSocketUrl)!)
        request.timeoutInterval = 10
        self.socket = WebSocket(request: request)
        self.socket.delegate = self
        self.socket.connect()
        self.socket.write(string: "test")
        print("testingg")
    }
    
    public func didReceive(event: Starscream.WebSocketEvent, client: Starscream.WebSocketClient) {
        switch event {
        case .connected(let headers):
            isConnected = true
            connectionStatusPublisher.send(isConnected)
            print("-- Websocket is connected: \(headers)")
        case .disconnected(let reason, let code):
            isConnected = false
            connectionStatusPublisher.send(isConnected)
            print("-- Websocket is disconnected: \(reason) with code: \(code)")
        case .text(let string):
            print("-- Received text: \(string)")
            messagePublisher.send(string)
        case .binary(let data):
            print("-- Received data: \(data.count)")
        case .ping(_):
            print("-- Ping")
            break
        case .pong(_):
            print("-- Pong")
            break
        case .viabilityChanged(_):
            print("-- Viability changed")
            break
        case .reconnectSuggested(_):
            print("-- Reconnect suggested")
            break
        case .cancelled:
            isConnected = false
            connectionStatusPublisher.send(isConnected)
            print("-- Cancelled")
            isConnected = false
        case .error(let error):
            isConnected = false
            connectionStatusPublisher.send(isConnected)
            print("-- Error")
            handleError(error)
        case .peerClosed:
            print("-- Peer closed")
            break
        }
    }
    
    public func handleError(_ error: Error?) {
        if let e = error as? WSError {
            print("websocket encountered an error: \(e.message)")
        } else if let e = error {
            print("websocket encountered an error: \(e.localizedDescription)")
        } else {
            print("websocket encountered an error")
        }
    }
    
    public func send(_ value: Any, onSuccess: @escaping ()-> Void) {
        guard JSONSerialization.isValidJSONObject(value) else {
            print("[WEBSOCKET] Value is not a valid JSON object.\n \(value)")
            return
        }
        
        do {
            let data = try JSONSerialization.data(withJSONObject: value, options: [])
            socket.write(data: data) {
                onSuccess()
            }
        } catch let error {
            print("[WEBSOCKET] Error serializing JSON:\n\(error)")
        }
    }
}
