//
//  CardReaderManager.swift
//  NFCApp
//
//  Created by Beatriz on 08/05/2024.
//

import Foundation
import CardReaderAccess
import CardReaderProviderApi
import Combine
import GemCommonsKit
import HealthCardAccess
import HealthCardControl
import NFCCardReaderProvider
import CoreNFC

public class CardReaderManager {
    
    private let nfcScannerController: NFCScannerController
    
    public init() {
        self.nfcScannerController = NFCScannerController()
    }
    
    public func scanCard(canNumber: String, cardSessionId: String) async throws -> Data {
        let cardData = await nfcScannerController.readEgkData(can: canNumber, cardSessionId: cardSessionId)
        
        guard let cardData = cardData else {
            throw NFCHealthCardSessionError.couldNotInitializeSession
        }
        
        return cardData
    }
}
