//
//  ViewState.swift
//  Egk
//
//  Created by Beatriz on 15/05/2024.
//

import Foundation

enum ViewState<Value, Failure> {
    case idle
    case loading(Value?)
    case value(Value)
    case error(Failure)

    var isIdle: Bool {
        if case .idle = self {
            return true
        }
        return false
    }
    
    var isLoading: Bool {
        if case .loading = self {
            return true
        }
        return false
    }

    var value: Value? {
        switch self {
        case let .value(value):
            return value
        case let .loading(value):
            return value
        default:
            return nil
        }
    }

    var error: Failure? {
        if case let .error(error) = self {
            return error
        }
        return nil
    }
}
