@interface WebSocketClientManager : NSObject

@property (nonatomic, strong) WebSocketClient *webSocketClient;
@property (nonatomic, assign) BOOL isConnected;
@property (nonatomic, strong) NSString *cardSessionId;
@property (nonatomic, strong) NSMutableSet<AnyCancellable *> *cancellables;
@property (nonatomic, assign) NSInteger sendAPDUMessageCount;

- (void)connectToURL:(NSString *)url;
- (void)sendJSONObject:(id)jsonObject onSuccess:(void (^)(void))onSuccess;
- (void)sendMessage:(NSString *)message onSuccess:(void (^)(void))onSuccess;

@end