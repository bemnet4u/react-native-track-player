//
//  MediaInfoController.swift
//  SwiftAudio
//
//  Created by Jørgen Henrichsen on 15/03/2018.
//

import Foundation
import MediaPlayer


public protocol NowPlayingInfoKeyValue {
    func getKey() -> String
    func getValue() -> Any?
}

public class NowPlayingInfoController {
    
    let infoCenter: MPNowPlayingInfoCenter
    
    private var _info: [String: Any] = [:]
    
    var info: [String: Any] {
        return _info
    }
    
    /**
     Create a new NowPlayingInfoController.
     
     - parameter infoCenter: The MPNowPlayingInfoCenter to use. Default is `MPNowPlayingInfoCenter.default()`
     */
    public init(infoCenter: MPNowPlayingInfoCenter = MPNowPlayingInfoCenter.default()) {
        self.infoCenter = infoCenter
        self._info = [:]
    }
    
    /**
     This updates a set of values in the now playing info.
     
     - Warning: This will reset the now playing info completely! Use this function when starting playback of a new item.
     */
    public func set(keyValues: [NowPlayingInfoKeyValue]) {
        self._info = [:]
        keyValues.forEach { (keyValue) in
            // https://stackoverflow.com/questions/26787070/exc-bad-access-when-updating-swift-dictionary-after-using-it-for-evaluate-nsexpr?rq=1
            let stupidHack = self._info
            self._info[keyValue.getKey()] = keyValue.getValue()
        }
    }
    
    /**
     This updates a single value in the now playing info.
     */
    public func set(keyValue: NowPlayingInfoKeyValue) {
        if let value = keyValue.getValue() {
            let stupidHack = self._info
            _info[keyValue.getKey()] = value
            self.infoCenter.nowPlayingInfo = _info
        }
    }
    
}
