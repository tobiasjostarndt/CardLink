//
//  WebSocketClientManager.m
//  WebSocket
//
//  Created by Beatriz on 23/05/2024.
//

#import <Foundation/Foundation.h>
#import <Combine/Combine.h>
#import "WebSocketClient.m"

#import "WebSocketClientManager.h"

@implementation WebSocketClientManager

- (instancetype)init {
    self = [super init];
    
    if (self) {
        _webSocketClient = [[WebSocketClient alloc] init];
        _isConnected = NO;
        _sendAPDUMessageCount = 0;
        _cancellables = [NSMutableSet set];
        [self subscribeToConnectionStatus];
        [self subscribeToWebSocketMessages];
    }

    return self;
}

- (void)connectToURL:(NSString *)url {
    [self.webSocketClient connectToWebSocketUrl:url];
}

- (void)sendJSONObject:(id)jsonObject onSuccess:(void (^)(void))onSuccess {
    [self.webSocketClient send:jsonObject onSuccess:onSuccess];
}

- (void)sendMessage:(NSString *)message onSuccess:(void (^)(void))onSuccess {
    [self.webSocketClient.socket writeString:message];
    onSuccess();
}

- (void)subscribeToConnectionStatus {
    [self.webSocketClient.connectionStatusPublisher
        sinkWithWeakTarget:self weakTargetAction:@selector(updateConnectionStatus:)]
        .storeIn:self.cancellables];
}

- (void)updateConnectionStatus:(NSNumber *)isConnected {
    self.isConnected = isConnected.boolValue;
}

- (void)subscribeToWebSocketMessages {
    [self.webSocketClient.messagePublisher
        sinkWithWeakTarget:self weakTargetAction:@selector(handleWebSocketMessage:)]
        .storeIn:self.cancellables];
}

- (void)handleWebSocketMessage:(NSString *)message {
    NSLog(@"Handling WebSocket message: %@", message);
    NSData *data = [message dataUsingEncoding:NSUTF8StringEncoding];
    
    if (!data) {
        NSLog(@"Failed to convert message to data.");
        return;
    }
    
    NSError *error = nil;
    NSArray *jsonArray = [NSJSONSerialization JSONObjectWithData:data options:0 error:&error];
    
    if (error || ![jsonArray isKindOfClass:[NSArray class]] || jsonArray.count <= 2) {
        NSLog(@"Failed to parse JSON with error: %@", error);
        return;
    }
    
    NSDictionary *messageDict = jsonArray[0];
    NSString *messageType = messageDict[@"type"];
    NSString *correlationId = (jsonArray[2] == [NSNull null]) ? @"" : jsonArray[2];
    
    if ([messageType isEqualToString:@"confirmSMSCodeResponse"]) {
        NSString *payload = messageDict[@"payload"];
        NSLog(@"Received confirmSMSCodeResponse message with payload: %@", payload);
        [[NSNotificationCenter defaultCenter] postNotificationName:@"confirmSMSCodeResponse" object:@{@"payload": payload}];
    } else if ([messageType isEqualToString:@"sendAPDU"]) {
        NSString *payload = messageDict[@"payload"];
        NSLog(@"Received sendAPDU message with payload: %@ and correlationId: %@", payload, correlationId);
        
        self.sendAPDUMessageCount += 1;
        
        if (self.sendAPDUMessageCount == 1) {
            [[NSNotificationCenter defaultCenter] postNotificationName:@"receivedFirstSendAPDU" object:@{@"payload": payload, @"correlationId": correlationId}];
        } else if (self.sendAPDUMessageCount == 2) {
            [[NSNotificationCenter defaultCenter] postNotificationName:@"receivedSecondSendAPDU" object:@{@"payload": payload, @"correlationId": correlationId}];
        }
    } else if ([messageType isEqualToString:@"eRezeptTokensFromAVS"]) {
        NSLog(@"Received eRezeptTokensFromAVS message with correlationId: %@", correlationId);
        NSString *payload = messageDict[@"payload"];
        [[NSNotificationCenter defaultCenter] postNotificationName:@"receivedERezeptTokensFromAVS" object:@{@"payload": payload, @"correlationId": correlationId}];
    } else if ([messageType isEqualToString:@"eRezeptBundlesFromAVS"]) {
        NSLog(@"Received eRezeptBundlesFromAVS message with correlationId: %@", correlationId);
        NSString *payload = messageDict[@"payload"];
        [[NSNotificationCenter defaultCenter] postNotificationName:@"receivedERezeptBundlesFromAVS" object:@{@"payload": payload, @"correlationId": correlationId}];
    } else {
        NSLog(@"Other message type: %@", messageType);
    }
}

@end

@implementation MockWebSocketClientManager

- (instancetype)init {
    self = [super init];
    if (self) {
        self.isConnected = NO;
    }
    return self;
}

- (void)connectToURL:(NSString *)url {
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(1 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        self.isConnected = YES;
    });
}

@end
