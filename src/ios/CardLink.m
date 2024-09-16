#import "CardLink.h"

@implementation CardLink

- (void)establishWSS:(CDVInvokedUrlCommand*)command{
    CDVPluginResult* pluginResult = nil;
    NSString* wssURL = [command.arguments objectAtIndex:0];

    if (wssURL != nil && [wssURL length] > 0) {
        [self.webSocketClientManager connectTo:url];
    
        if (self.webSocketClientManager.isConnected) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"true"];
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        }
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    }

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end