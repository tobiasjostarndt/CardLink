#import <Cordova/CDV.h>
#import "WebSocket/WebSocketClientManager.m"

@interface CardLink : CDVPlugin {
}

@property (nonatomic, strong) WebSocketClientManager *webSocketClientManager;

- (void)establishWSS:(CDVInvokedUrlCommand*)command;

@end

@implementation CardLink

- (void)establishWSS:(CDVInvokedUrlCommand*)command{
    CDVPluginResult* pluginResult = nil;
    NSString* wssURL = [command.arguments objectAtIndex:0];

    if (wssURL != nil && [wssURL length] > 0) {
        [self.webSocketClientManager connectTo:url];
    
        if (self.webSocketClientManager.isConnected) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:"true"];
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        }
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    }

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end


- (BOOL)connectToWebSocket:(NSString *)url {
    self.webSocketUrl = url;
    [self.webSocketClientManager connectTo:url];
    
    if (self.webSocketClientManager.isConnected) {
        self.isShowingPopup = NO;
        return YES;
    } else {
        return NO;
    }
}