#import <Cordova/CDV.h>
#import "WebSocketClientManager.h"

@interface CardLink : CDVPlugin {
}

@property (nonatomic, strong) WebSocketClientManager *webSocketClientManager;

- (void)establishWSS:(CDVInvokedUrlCommand*)command;

@end
