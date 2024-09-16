// WebSocketClientManager.m
#import "WebSocketClientManager.h"

@implementation WebSocketClientManager

- (instancetype)init {
    self = [super init];
    if (self) {
        self.webSocketClient = [[WebSocketClient alloc] init];
        self.webSocketClient.delegate = self;
    }
    return self;
}

- (void)connectToURL:(NSURL *)url {
    [self.webSocketClient connectToURL:url];
}

- (void)send:(NSString *)message {
    [self.webSocketClient send:message];
}

- (void)disconnect {
    [self.webSocketClient disconnect];
}

#pragma mark - WebSocketClientDelegate

- (void)webSocketClientDidConnect {
    self.isConnected = YES;
}

- (void)webSocketClientDidDisconnectWithReason:(NSString *)reason {
    self.isConnected = NO;
}

- (void)webSocketClientDidReceiveMessage:(NSString *)message {
    NSLog(@"Received message: %@", message);
    // Handle received message
}

- (void)webSocketClientDidReceiveError:(NSError *)error {
    NSLog(@"Error: %@", error.localizedDescription);
    // Handle error
}

@end
