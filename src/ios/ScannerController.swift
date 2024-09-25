//
//  ScannerController.swift
//  Egk
//
//  Created by Beatriz on 15/05/2024.
//

import Combine
import Foundation
import Helper
import NFCCardReaderProvider
import CardReaderProviderApi
import SwiftUI

protocol ScannerController {
    var state: Published<ViewState<Bool, Error>>.Publisher { get }

    func readEgkData(can: String, cardSessionId: String) async -> Data?
    func dismissError() async
}

class NFCScannerViewModel: ObservableObject {
    @Environment(\.scannerController) var scannerController: ScannerController
    @Published var state: ViewState<Bool, Error> = .idle
    @Published var cardSessionId: String? = nil

    private var disposables = Set<AnyCancellable>()

    init(state: ViewState<Bool, Error> = .idle) {
        self.state = state
        scannerController.state
            .dropFirst()
            .sink { [weak self] viewState in
                self?.state = viewState

                guard !viewState.isLoading, !viewState.isIdle else { return }

                CommandLogger.commands = []
            }
            .store(in: &disposables)
    }

    func readEgkData(can: String, cardSessionId: String) async {
        let cardData = await scannerController.readEgkData(can: can, cardSessionId: cardSessionId)
        Task { @MainActor in
            if let cardData = cardData {
                self.state = .value(true)
                self.cardSessionId = cardSessionId
                print("Card Data: \(cardData)")
                print("Card Session ID: \(String(describing: cardSessionId))")
            } else {
                self.state = .error(NFCHealthCardSessionError.couldNotInitializeSession)
            }
        }
    }

    func dismissError() async {
        await scannerController.dismissError()
    }
}
