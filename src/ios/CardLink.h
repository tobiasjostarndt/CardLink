#import <Cordova/CDV.h>
#import "WebSocketClientManager.m"

@interface CardLink : CDVPlugin {
}

@property (nonatomic, strong) WebSocketClientManager *webSocketClientManager;

- (void)establishWSS:(CDVInvokedUrlCommand*)command;

@end