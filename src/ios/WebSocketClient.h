// WebSocketClient.h
#import <Foundation/Foundation.h>

@protocol WebSocketClientDelegate <NSObject>
- (void)webSocketClientDidConnect;
- (void)webSocketClientDidDisconnectWithReason:(NSString *)reason;
- (void)webSocketClientDidReceiveMessage:(NSString *)message;
- (void)webSocketClientDidReceiveError:(NSError *)error;
@end

@interface WebSocketClient : NSObject

@property (nonatomic, weak) id<WebSocketClientDelegate> delegate;
@property (nonatomic, assign, readonly) BOOL isConnected;

- (instancetype)init;
- (void)connectToURL:(NSURL *)url;
- (void)send:(NSString *)message;
- (void)disconnect;

@end
