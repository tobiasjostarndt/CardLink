//
//  WebSocketClientManager.h
//  WebSocket
//

#import <Foundation/Foundation.h>
#import "WebSocketClient.h"

@interface WebSocketClientManager : NSObject <WebSocketClientDelegate>

@property (nonatomic, strong) WebSocketClient *webSocketClient;
@property (nonatomic, assign) BOOL isConnected;
@property (nonatomic, strong) NSString *cardSessionId;
@property (nonatomic, assign) NSInteger sendAPDUMessageCount;

- (void)connectToURL:(NSString *)url;
- (void)sendJSONObject:(id)jsonObject onSuccess:(void (^)(void))onSuccess;
- (void)sendMessage:(NSString *)message onSuccess:(void (^)(void))onSuccess;

@end

@interface MockWebSocketClientManager : WebSocketClientManager

@end
