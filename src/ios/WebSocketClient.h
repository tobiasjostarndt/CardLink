@interface WebSocketClient : NSObject <WebSocketDelegate>

@property (nonatomic, strong) WebSocket *socket;
@property (nonatomic, assign) BOOL isConnected;
@property (nonatomic, strong) PassthroughSubject<NSNumber *, Never *> *connectionStatusPublisher;
@property (nonatomic, strong) PassthroughSubject<NSString *, Never *> *messagePublisher;

- (void)connectToWebSocketUrl:(NSString *)webSocketUrl;
- (void)send:(id)value onSuccess:(void (^)(void))onSuccess;
- (void)handleError:(NSError *)error;

@end