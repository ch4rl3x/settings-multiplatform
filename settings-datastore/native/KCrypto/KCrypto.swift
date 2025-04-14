import CryptoKit
import Foundation
import Security

@objc public class KCrypto : NSObject {
    @objc(sha256:) public class func sha256(string: String) -> Data {
        return SHA256
            .hash(data: string.data(using: .ascii)!)
            .data
    }

    // Must be 32 characters!
    let cipher = SymmetricKey(size: .bits256)

    init() {
        // Set attributes
        let attributes: [String: Any] = [
            kSecClass as String: kSecAttrKeyClassSymmetric,
            kSecAttrAccount as String: username,
            kSecValueData as String: password,
        ]
        // Add user
        if SecItemAdd(attributes as CFDictionary, nil) == noErr {
            print("User saved successfully in the keychain")
        } else {
            print("Something went wrong trying to save the user in the keychain")
        }

    }

    func encrypt(message: String) throws -> Data {
        guard let messageData = message.data(using: .utf8)
        else { throw CustomError("Konnte message nicht zu data umwandeln") }
        guard let encrypted = try AES.GCM.seal(messageData, using: cipher).combined else { throw CustomError("konnte nicht combinen")}
        return encrypted
    }

    func decrypt(data: Data) throws -> String {

        let box = try AES.GCM.SealedBox(combined: data)
        let decryptedData = try AES.GCM.open(box, using: cipher)

        guard let text = String(data: decryptedData, encoding: .utf8) else { throw CustomError("Konnte data nicht in String formatieren") }
        return text
    }


}

extension Digest {
    var bytes: [UInt8] { Array(makeIterator()) }
    var data: Data { Data(bytes) }
}