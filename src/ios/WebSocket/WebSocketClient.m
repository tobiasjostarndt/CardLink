//
//  WebSocketClient.m
//  WebSocket
//
//  Created by Beatriz Correia on 26/04/2024.
//

#import <Foundation/Foundation.h>
#import <Combine/Combine.h>
#import <Starscream/Starscream.h>

@interface WebSocketClient : NSObject <WebSocketDelegate>

@property (nonatomic, strong) WebSocket *socket;
@property (nonatomic, assign) BOOL isConnected;
@property (nonatomic, strong) PassthroughSubject<NSNumber *, Never *> *connectionStatusPublisher;
@property (nonatomic, strong) PassthroughSubject<NSString *, Never *> *messagePublisher;

- (void)connectToWebSocketUrl:(NSString *)webSocketUrl;
- (void)send:(id)value onSuccess:(void (^)(void))onSuccess;
- (void)handleError:(NSError *)error;

@end

@implementation WebSocketClient

- (instancetype)init {
    self = [super init];
    if (self) {
        _connectionStatusPublisher = [[PassthroughSubject alloc] init];
        _messagePublisher = [[PassthroughSubject alloc] init];
        _isConnected = NO;
    }
    return self;
}

- (void)connectToWebSocketUrl:(NSString *)webSocketUrl {
    NSURLRequest *request = [NSURLRequest requestWithURL:[NSURL URLWithString:webSocketUrl] cachePolicy:NSURLRequestUseProtocolCachePolicy timeoutInterval:10];
    self.socket = [[WebSocket alloc] initWithRequest:request];
    self.socket.delegate = self;
    [self.socket connect];
    [self.socket writeString:@"test"];
    NSLog(@"testingg");
}

- (void)didReceiveEvent:(WebSocketEvent *)event client:(id<WebSocketClient>)client {
    switch (event.type) {
        case WebSocketEventTypeConnected: {
            NSDictionary *headers = event.headers;
            self.isConnected = YES;
            [self.connectionStatusPublisher send:@(self.isConnected)];
            NSLog(@"-- Websocket is connected: %@", headers);
            break;
        }
        case WebSocketEventTypeDisconnected: {
            NSString *reason = event.reason;
            NSInteger code = event.code;
            self.isConnected = NO;
            [self.connectionStatusPublisher send:@(self.isConnected)];
            NSLog(@"-- Websocket is disconnected: %@ with code: %ld", reason, (long)code);
            break;
        }
        case WebSocketEventTypeText: {
            NSString *string = event.text;
            NSLog(@"-- Received text: %@", string);
            [self.messagePublisher send:string];
            break;
        }
        case WebSocketEventTypeBinary: {
            NSData *data = event.data;
            NSLog(@"-- Received data: %lu", (unsigned long)data.length);
            break;
        }
        case WebSocketEventTypePing:
            NSLog(@"-- Ping");
            break;
        case WebSocketEventTypePong:
            NSLog(@"-- Pong");
            break;
        case WebSocketEventTypeViabilityChanged:
            NSLog(@"-- Viability changed");
            break;
        case WebSocketEventTypeReconnectSuggested:
            NSLog(@"-- Reconnect suggested");
            break;
        case WebSocketEventTypeCancelled:
            self.isConnected = NO;
            [self.connectionStatusPublisher send:@(self.isConnected)];
            NSLog(@"-- Cancelled");
            break;
        case WebSocketEventTypeError: {
            NSError *error = event.error;
            self.isConnected = NO;
            [self.connectionStatusPublisher send:@(self.isConnected)];
            NSLog(@"-- Error");
            [self handleError:error];
            break;
        }
        case WebSocketEventTypePeerClosed:
            NSLog(@"-- Peer closed");
            break;
    }
}

- (void)handleError:(NSError *)error {
    if ([error isKindOfClass:[WSError class]]) {
        WSError *wsError = (WSError *)error;
        NSLog(@"websocket encountered an error: %@", wsError.message);
    } else {
        NSLog(@"websocket encountered an error: %@", error.localizedDescription);
    }
}

- (void)send:(id)value onSuccess:(void (^)(void))onSuccess {
    if (![NSJSONSerialization isValidJSONObject:value]) {
        NSLog(@"[WEBSOCKET] Value is not a valid JSON object.\n %@", value);
        return;
    }
    
    NSError *error = nil;
    NSData *data = [NSJSONSerialization dataWithJSONObject:value options:0 error:&error];
    
    if (error) {
        NSLog(@"[WEBSOCKET] Error serializing JSON:\n%@", error);
    } else {
        [self.socket writeData:data completion:^{
            onSuccess();
        }];
    }
}

@end
