//
//  ScannerEnvironmentExtensions.swift
//  Egk
//
//  Created by Beatriz on 15/05/2024.
//

import SwiftUI

struct ScanControllerKey: EnvironmentKey {
    static let defaultValue: ScannerController = NFCScannerController()
}

extension EnvironmentValues {
    var scannerController: ScannerController {
        get { self[ScanControllerKey.self] }
        set { self[ScanControllerKey.self] = newValue }
    }
}
