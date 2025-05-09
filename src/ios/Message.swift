//
//  Message.swift
//  Egk
//
//  Created by Beatriz on 05/06/2024.
//

import Foundation

public class Message {
    private var raw: [UInt8]

    public init(raw: [UInt8]) {
        self.raw = raw
    }

    public func getRaw() -> [UInt8] {
        return self.raw
    }

    public func setRaw(_ raw: [UInt8]) {
        self.raw = raw
    }

    public func getMessageType() -> UInt8 {
        return raw[0]
    }

    public func getSrcOrDesAddr() -> [UInt8] {
        return [raw[1], raw[2]]
    }

    public func getSeq() -> [UInt8] {
        return [raw[3], raw[4]]
    }

    public func getLength() -> Int {
        return (Int(raw[6]) << 24) | (Int(raw[7]) << 16) | (Int(raw[8]) << 8)
            | Int(raw[9])
    }

    public func getBody() -> [UInt8] {
        let length = getLength()
        guard raw.count >= 10 + length else { return [] }
        return Array(raw[10..<10 + length])
    }

    public func getCLA() -> UInt8 {
        return getBody()[0]
    }

    public func getIns() -> UInt8 {
        return getBody()[1]
    }

    public func getP1() -> UInt8 {
        return getBody()[2]
    }

    public func getP2() -> UInt8 {
        return getBody()[3]
    }

    public func getAPDULength() -> Int {
        return Message.getAPDULength(apduData: getBody())
    }

    public static func getAPDULength(apduData: [UInt8]) -> Int {
        if apduData[4] != 0 && apduData.count > 5 {
            return Int(apduData[4] & 0xff)
        } else if apduData.count == 5 {
            return 0
        } else if apduData.count > 6 {
            let body = apduData
            return Int(
                UInt32(
                    bigEndian: Data([0x00, 0x00, body[5], body[6]])
                        .withUnsafeBytes { $0.load(as: UInt32.self) }
                )
            )
        } else {
            return 0
        }
    }

    public func getAPDUData() -> [UInt8] {
        let body = getBody()
        return Message.getAPDUData(body: body)
    }

    public static func getAPDUData(body: [UInt8]) -> [UInt8] {
        if body.count < 5 {
            print("getAPDUData: body too short (<5)")
            return []
        }

        if body.count == 7 && body[4] == 0x00 {
            print("getAPDUData: Detected 7-byte APDU with extended Le only")
            return []
        }

        let lc = Int(body[4] & 0xff)
        let apduLength = getAPDULength(apduData: body)

        if lc != 0 && body.count >= 5 + apduLength {
            let data = Array(body[5..<5 + apduLength])
            print(
                "getAPDUData: Short-length data (Lc=\(lc)), extracted \(data.count) bytes"
            )
            return data
        } else if apduLength > 0 && body.count >= 7 + apduLength {
            let data = Array(body[7..<7 + apduLength])
            print(
                "getAPDUData: Extended-length data, extracted \(data.count) bytes"
            )
            return data
        } else {
            print(
                "getAPDUData: Data length mismatch (Lc: \(lc), apduLength: \(apduLength), body.count: \(body.count))"
            )
            return []
        }
    }

    public func getExpectedLength() -> Int {
        return Message.expectedLength(apdu: getBody())
    }

    public static func expectedLength(apdu: [UInt8]) -> Int {
        let l1 = apdu[4] & 0xff
        if apdu.count == 5 {
            return (l1 == 0) ? 256 : Int(l1)
        }
        if l1 != 0 {
            if apdu.count == 4 + 1 + Int(l1) {
                return 0
            } else if apdu.count == 4 + 2 + Int(l1) {
                let l2 = apdu[apdu.count - 1] & 0xff
                return (l2 == 0) ? 256 : Int(l2)
            } else {
                return 0
            }
        }
        guard apdu.count > 6 else { return 0 }
        let l2 = Int(apdu[5]) << 8 | Int(apdu[6])
        if apdu.count == 7 {
            return (l2 == 0) ? 65536 : l2
        }
        if apdu.count == 4 + 5 + l2 {
            let leOfs = apdu.count - 2
            let l3 = Int(apdu[leOfs]) << 8 | Int(apdu[leOfs + 1])
            return (l3 == 0) ? 65536 : l3
        } else {
            return 0
        }
    }

    public func toString() -> String {
        return raw.map { String(format: "%02hhX", $0) }.joined()
    }
}
