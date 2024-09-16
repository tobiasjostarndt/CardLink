// WebSocketClient.m
#import "WebSocketClient.h"

@interface WebSocketClient ()

@property (nonatomic, strong) NSURLSessionWebSocketTask *webSocketTask;
@property (nonatomic, strong) NSURLSession *urlSession;
@property (nonatomic, assign) BOOL isConnected;

@end

@implementation WebSocketClient

- (instancetype)init {
    self = [super init];
    if (self) {
        _urlSession = [NSURLSession sharedSession];
    }
    return self;
}

- (void)connectToURL:(NSURL *)url {
    NSURLRequest *request = [NSURLRequest requestWithURL:url];
    self.webSocketTask = [self.urlSession webSocketTaskWithRequest:request];
    
    // Start the connection
    [self.webSocketTask resume];
    
    // Attempt to receive a message to confirm the connection
    [self receiveMessage];
    
    // Send a ping to check if the connection is alive
    [self.webSocketTask sendMessage:[NSURLSessionWebSocketTaskMessage messageWithString:@"ping"] completionHandler:^(NSError * _Nullable error) {
        if (error) {
            self.isConnected = NO;
            [self.delegate webSocketClientDidReceiveError:error];
        } else {
            self.isConnected = YES;
            [self.delegate webSocketClientDidConnect];
        }
    }];
}


- (void)receiveMessage {
    [self.webSocketTask receiveMessageWithCompletionHandler:^(NSURLSessionWebSocketTaskMessage * _Nullable message, NSError * _Nullable error) {
        if (error) {
            [self.delegate webSocketClientDidReceiveError:error];
            return;
        }
        
        if (message.type == NSURLSessionWebSocketTaskMessageTypeText) {
            NSString *text = message.text;
            [self.delegate webSocketClientDidReceiveMessage:text];
        }
        
        [self receiveMessage];  // Keep receiving messages
    }];
}

- (void)send:(NSString *)message {
    NSURLSessionWebSocketTaskMessage *webSocketMessage = [NSURLSessionWebSocketTaskMessage messageWithString:message];
    [self.webSocketTask sendMessage:webSocketMessage completionHandler:^(NSError * _Nullable error) {
        if (error) {
            [self.delegate webSocketClientDidReceiveError:error];
        }
    }];
}

- (void)disconnect {
    [self.webSocketTask cancelWithCloseCode:NSURLSessionWebSocketCloseCodeNormal];
    self.isConnected = NO;
    [self.delegate webSocketClientDidDisconnectWithReason:@"Disconnected"];
}

@end
