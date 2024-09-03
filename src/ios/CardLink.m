#import "CardLink.h"
#import ‹Cordova/CDVAvailability.h>

@implementation CardLink

- (void) pluginInitialize
｝

- (void) cardlink: (CDVInvokedUrlCommand *) command {
    NSString* arg0 = [command arguments objectAtIndex:0];

    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString: arg0];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

@end